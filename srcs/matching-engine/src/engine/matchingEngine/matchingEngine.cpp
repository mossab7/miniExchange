#include "matchingEngine.hpp"

#include "../OrderBook/OrderBook.hpp"

#include <condition_variable>
#include <deque>
#include <stdexcept>
#include <thread>
#include <utility>

class MatchingEngine::Worker final
{
public:
    explicit Worker(std::size_t queueCapacity)
        : queueCapacity_(queueCapacity), thread_(&Worker::run, this)
    {
    }

    ~Worker()
    {
        stop();
    }

    Worker(const Worker&) = delete;
    Worker& operator=(const Worker&) = delete;

    bool enqueue(WorkItem item)
    {
        {
            std::lock_guard lock(queueMutex_);
            if (stopping_ || queue_.size() >= queueCapacity_)
            {
                return false;
            }
            queue_.emplace_back(std::move(item));
        }
        queueReady_.notify_one();
        return true;
    }

    void stop() noexcept
    {
        {
            std::lock_guard lock(queueMutex_);
            if (stopping_)
            {
                return;
            }
            stopping_ = true;
        }

        queueReady_.notify_one();
        if (thread_.joinable())
        {
            thread_.join();
        }
    }

private:
    void run() noexcept
    {
        for (;;)
        {
            WorkItem item;
            {
                std::unique_lock lock(queueMutex_);
                queueReady_.wait(lock, [this]() { return stopping_ || !queue_.empty(); });
                if (queue_.empty())
                {
                    return;
                }
                item = std::move(queue_.front());
                queue_.pop_front();
            }

            try
            {
                item.completion.set_value(process(item));
            }
            catch (const std::exception& exception)
            {
                WorkerResult result;
                result.reason = exception.what();
                item.completion.set_value(std::move(result));
            }
            catch (...)
            {
                WorkerResult result;
                result.reason = "worker command failed";
                item.completion.set_value(std::move(result));
            }
        }
    }

    WorkerResult process(const WorkItem& item)
    {
        switch (item.type)
        {
        case CommandType::RegisterInstrument:
            if (books_.contains(item.instrumentId))
            {
                return {false, false, "instrument is already registered", {}, {}};
            }
            books_.emplace(item.instrumentId, std::make_unique<OrderBook>());
            return {true, false, {}, {}, {}};

        case CommandType::SubmitOrder:
        {
            const auto book = books_.find(item.instrumentId);
            if (book == books_.end())
            {
                return {false, false, "instrument is not configured", {}, {}};
            }

            if (!book->second->submitOrder(item.request))
            {
                return {false, false, book->second->getLastRejectionReason(), {}, {}};
            }

            return {true,
                    book->second->containsOrder(item.request.id),
                    {},
                    book->second->getTrades(),
                    book->second->getLastFilledOrderIds()};
        }

        case CommandType::CancelOrder:
        {
            const auto book = books_.find(item.instrumentId);
            if (book == books_.end())
            {
                return {false, false, "instrument is not configured", {}, {}};
            }
            if (!book->second->cancelOrder(item.orderId))
            {
                return {false, false, "order was not found", {}, {}};
            }
            return {true, false, {}, {}, {}};
        }

        case CommandType::ReadTrades:
        {
            const auto book = books_.find(item.instrumentId);
            if (book == books_.end())
            {
                return {false, false, "instrument is not configured", {}, {}};
            }
            return {true, false, {}, book->second->getTrades(), {}};
        }
        }

        return {false, false, "unknown worker command", {}, {}};
    }

    const std::size_t queueCapacity_;
    std::mutex queueMutex_;
    std::condition_variable queueReady_;
    std::deque<WorkItem> queue_;
    bool stopping_{false};
    std::thread thread_;
    std::unordered_map<uint64_t, std::unique_ptr<OrderBook>> books_;
};

void MatchingEngine::setReason(std::string* reason, const std::string& value)
{
    if (reason != nullptr)
    {
        *reason = value;
    }
}

MatchingEngine::MatchingEngine(std::size_t workerCount, std::size_t queueCapacity,
                               std::shared_ptr<TradePublisher> tradePublisher)
    : queueCapacity_(queueCapacity), tradePublisher_(std::move(tradePublisher))
{
    if (workerCount == 0)
    {
        throw std::invalid_argument("worker count must be greater than zero");
    }
    if (queueCapacity == 0)
    {
        throw std::invalid_argument("worker queue capacity must be greater than zero");
    }

    workers_.reserve(workerCount);
    for (std::size_t id = 0; id < workerCount; ++id)
    {
        workers_.push_back(std::make_unique<Worker>(queueCapacity_));
    }
}

MatchingEngine::MatchingEngine(const std::vector<uint64_t>& instrumentIds,
                               std::size_t workerCount,
                               std::size_t queueCapacity,
                               std::shared_ptr<TradePublisher> tradePublisher)
    : MatchingEngine(workerCount, queueCapacity, std::move(tradePublisher))
{
    for (const uint64_t instrumentId : instrumentIds)
    {
        std::string reason;
        if (!addInstrument(instrumentId, &reason))
        {
            throw std::invalid_argument(reason.empty() ? "failed to register instrument" : reason);
        }
    }
}

MatchingEngine::~MatchingEngine()
{
    shutdown();
}

bool MatchingEngine::addInstrument(uint64_t instrumentId, std::string* reason)
{
    if (instrumentId == 0)
    {
        setReason(reason, "instrument id must be greater than zero");
        return false;
    }

    std::lock_guard lock(routingMutex_);
    if (stopped_)
    {
        setReason(reason, "matching engine is stopped");
        return false;
    }
    if (instrumentWorkers_.contains(instrumentId))
    {
        setReason(reason, "instrument is already registered");
        return false;
    }

    WorkItem item;
    item.type = CommandType::RegisterInstrument;
    item.instrumentId = instrumentId;
    WorkerResult result;
    if (!enqueueAndWait(nextWorker_, std::move(item), result, reason))
    {
        return false;
    }
    if (!result.success)
    {
        setReason(reason, result.reason);
        return false;
    }

    instrumentWorkers_.emplace(instrumentId, nextWorker_);
    nextWorker_ = (nextWorker_ + 1) % workers_.size();
    return true;
}

bool MatchingEngine::hasInstrument(uint64_t instrumentId) const
{
    std::lock_guard lock(routingMutex_);
    return instrumentWorkers_.contains(instrumentId);
}

std::optional<std::size_t> MatchingEngine::workerForInstrument(uint64_t instrumentId) const
{
    std::lock_guard lock(routingMutex_);
    const auto it = instrumentWorkers_.find(instrumentId);
    if (it == instrumentWorkers_.end())
    {
        return std::nullopt;
    }
    return it->second;
}

bool MatchingEngine::enqueueAndWait(std::size_t workerId, WorkItem item,
                                    WorkerResult& result, std::string* reason) const
{
    if (workerId >= workers_.size())
    {
        setReason(reason, "worker id is out of range");
        return false;
    }

    std::future<WorkerResult> completion = item.completion.get_future();
    if (!workers_[workerId]->enqueue(std::move(item)))
    {
        setReason(reason, "worker queue is full or stopped");
        return false;
    }

    result = completion.get();
    if (!result.success && reason != nullptr)
    {
        *reason = result.reason;
    }
    return true;
}

bool MatchingEngine::submitOrder(const SubmitOrderRequest& request, std::string* reason)
{
    std::size_t workerId = 0;
    {
        std::lock_guard lock(routingMutex_);
        if (stopped_)
        {
            setReason(reason, "matching engine is stopped");
            return false;
        }

        const auto instrument = instrumentWorkers_.find(request.instrumentId);
        if (instrument == instrumentWorkers_.end())
        {
            setReason(reason, "instrument is not configured");
            return false;
        }
        if (pendingOrderIds_.contains(request.id) || activeOrders_.contains(request.id))
        {
            setReason(reason, "order is already active or being processed");
            return false;
        }
        pendingOrderIds_.insert(request.id);
        activeOrders_[request.id] = ActiveOrder{instrument->second, request.instrumentId};
        workerId = instrument->second;
    }

    WorkItem item;
    item.type = CommandType::SubmitOrder;
    item.instrumentId = request.instrumentId;
    item.request = request;
    WorkerResult result;
    const bool enqueued = enqueueAndWait(workerId, std::move(item), result, reason);

    if (!enqueued || !result.success)
    {
        std::lock_guard lock(routingMutex_);
        pendingOrderIds_.erase(request.id);
        activeOrders_.erase(request.id);
        return false;
    }
    if (tradePublisher_ != nullptr)
    {
        try
        {
            for (const Trade& trade : result.trades)
            {
                tradePublisher_->publish(trade, request.instrumentId);
            }
        }
        catch (const std::exception& exception)
        {
            setReason(reason, exception.what());
            std::lock_guard lock(routingMutex_);
            pendingOrderIds_.erase(request.id);
            activeOrders_.erase(request.id);
            for (const uint64_t filledOrderId : result.filledOrderIds)
            {
                activeOrders_.erase(filledOrderId);
            }
            return false;
        }
    }
    std::lock_guard lock(routingMutex_);
    pendingOrderIds_.erase(request.id);
    for (const uint64_t filledOrderId : result.filledOrderIds)
    {
        activeOrders_.erase(filledOrderId);
    }
    if (result.resting)
    {
        activeOrders_[request.id] = ActiveOrder{workerId, request.instrumentId};
    }
    else
    {
        activeOrders_.erase(request.id);
    }
    return true;
}

bool MatchingEngine::cancelOrder(uint64_t orderId, std::string* reason)
{
    std::size_t workerId = 0;
    uint64_t instrumentId = 0;
    {
        std::lock_guard lock(routingMutex_);
        if (stopped_)
        {
            setReason(reason, "matching engine is stopped");
            return false;
        }

        const auto active = activeOrders_.find(orderId);
        if (active == activeOrders_.end())
        {
            setReason(reason, "order was not found");
            return false;
        }
        workerId = active->second.workerId;
        instrumentId = active->second.instrumentId;
    }

    WorkItem item;
    item.type = CommandType::CancelOrder;
    item.instrumentId = instrumentId;
    item.orderId = orderId;
    WorkerResult result;
    const bool enqueued = enqueueAndWait(workerId, std::move(item), result, reason);

    std::lock_guard lock(routingMutex_);
    if (enqueued && result.success)
    {
        activeOrders_.erase(orderId);
        return true;
    }
    return false;
}

std::vector<Trade> MatchingEngine::getTrades(uint64_t instrumentId) const
{
    std::size_t workerId = 0;
    {
        std::lock_guard lock(routingMutex_);
        const auto instrument = instrumentWorkers_.find(instrumentId);
        if (instrument == instrumentWorkers_.end())
        {
            return {};
        }
        workerId = instrument->second;
    }

    WorkItem item;
    item.type = CommandType::ReadTrades;
    item.instrumentId = instrumentId;
    WorkerResult result;
    if (!enqueueAndWait(workerId, std::move(item), result, nullptr) || !result.success)
    {
        return {};
    }
    return std::move(result.trades);
}

std::size_t MatchingEngine::workerCount() const noexcept
{
    return workers_.size();
}

bool MatchingEngine::isRunning() const noexcept
{
    std::lock_guard lock(routingMutex_);
    return !stopped_;
}

void MatchingEngine::shutdown() noexcept
{
    {
        std::lock_guard lock(routingMutex_);
        if (stopped_)
        {
            return;
        }
        stopped_ = true;
    }

    for (auto& worker : workers_)
    {
        worker->stop();
    }
}
