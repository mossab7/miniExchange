#ifndef MATCHING_ENGINE_HPP
#define MATCHING_ENGINE_HPP

#include <cstddef>
#include <cstdint>
#include <future>
#include <memory>
#include <mutex>
#include <optional>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#include "../Trade/Trade.hpp"
#include "../Trade/TradePublisher.hpp"
#include "submitOrderRequest.hpp"

class MatchingEngine final
{
public:
    static constexpr std::size_t kDefaultQueueCapacity = 1024;

    explicit MatchingEngine(std::size_t workerCount,
                            std::size_t queueCapacity = kDefaultQueueCapacity,
                            std::shared_ptr<TradePublisher> tradePublisher = nullptr);
    explicit MatchingEngine(const std::vector<uint64_t>& instrumentIds,
                            std::size_t workerCount = 1,
                            std::size_t queueCapacity = kDefaultQueueCapacity,
                            std::shared_ptr<TradePublisher> tradePublisher = nullptr);
    ~MatchingEngine();

    MatchingEngine(const MatchingEngine&) = delete;
    MatchingEngine& operator=(const MatchingEngine&) = delete;

    bool addInstrument(uint64_t instrumentId, std::string* reason = nullptr);
    bool hasInstrument(uint64_t instrumentId) const;
    std::optional<std::size_t> workerForInstrument(uint64_t instrumentId) const;

    // Requests are processed synchronously from the caller's point of view. The
    // matching operation itself always runs on the owning worker thread.
    bool submitOrder(const SubmitOrderRequest& request, std::string* reason = nullptr);
    bool cancelOrder(uint64_t orderId, std::string* reason = nullptr);

    // A copy is returned because the actual order book remains owned by a worker.
    std::vector<Trade> getTrades(uint64_t instrumentId) const;

    std::size_t workerCount() const noexcept;
    bool isRunning() const noexcept;
    void shutdown() noexcept;

private:
    class Worker;

    struct WorkerResult
    {
        bool success{false};
        bool resting{false};
        std::string reason;
        std::vector<Trade> trades;
        std::vector<uint64_t> filledOrderIds;
    };

    enum class CommandType
    {
        RegisterInstrument,
        SubmitOrder,
        CancelOrder,
        ReadTrades
    };

    struct WorkItem
    {
        CommandType type{CommandType::SubmitOrder};
        uint64_t instrumentId{0};
        uint64_t orderId{0};
        SubmitOrderRequest request{};
        std::promise<WorkerResult> completion;
    };

    struct ActiveOrder
    {
        std::size_t workerId{0};
        uint64_t instrumentId{0};
    };

    static void setReason(std::string* reason, const std::string& value);
    bool enqueueAndWait(std::size_t workerId, WorkItem item,
                        WorkerResult& result, std::string* reason) const;

    const std::size_t queueCapacity_;
    std::shared_ptr<TradePublisher> tradePublisher_;
    std::vector<std::unique_ptr<Worker>> workers_;
    mutable std::mutex routingMutex_;
    std::unordered_map<uint64_t, std::size_t> instrumentWorkers_;
    std::unordered_map<uint64_t, ActiveOrder> activeOrders_;
    std::unordered_set<uint64_t> pendingOrderIds_;
    std::size_t nextWorker_{0};
    bool stopped_{false};
};

#endif // MATCHING_ENGINE_HPP
