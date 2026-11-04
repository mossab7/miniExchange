#include "engine/OrderBook/OrderBook.hpp"
#include "engine/matchingEngine/matchingEngine.hpp"

#include <algorithm>
#include <atomic>
#include <cstdint>
#include <exception>
#include <iostream>
#include <random>
#include <sstream>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>

namespace
{
struct TestFailure : std::runtime_error
{
    using std::runtime_error::runtime_error;
};

void require(bool condition, const std::string& message)
{
    if (!condition)
    {
        throw TestFailure(message);
    }
}

template <typename Actual, typename Expected>
void requireEqual(const Actual& actual, const Expected& expected, const std::string& message)
{
    if (!(actual == expected))
    {
        std::ostringstream output;
        output << message << " (actual=" << actual << ", expected=" << expected << ')';
        throw TestFailure(output.str());
    }
}

SubmitOrderRequest order(uint64_t id, Side side, uint64_t price = 100, uint64_t quantity = 1,
                         uint64_t instrument = 1)
{
    return {id, instrument, price, quantity, side};
}

struct RestingView
{
    uint64_t id;
    uint64_t quantity;

    bool operator==(const RestingView&) const = default;
};

std::vector<RestingView> level(OrderBook& book, Side side, uint64_t price)
{
    const SubmitOrderRequest probe{UINT64_C(999999999), 1, price, 1, side};
    Order probeOrder(probe);
    const PriceLevel* priceLevel = book.getPriceLevel(&probeOrder);
    std::vector<RestingView> result;
    for (const Order* current = priceLevel == nullptr ? nullptr : priceLevel->getOrders();
         current != nullptr; current = current->next)
    {
        result.push_back({current->getId(), current->getQuantity()});
    }
    return result;
}

void requireLevel(OrderBook& book, Side side, uint64_t price,
                  const std::vector<RestingView>& expected)
{
    require(level(book, side, price) == expected, "resting order sequence differs at price level");
}

void test_basic_book_behavior()
{
    OrderBook book;
    require(book.submitOrder(order(1, Side::BUYER, 99, 5)), "buy should be accepted");
    require(book.submitOrder(order(2, Side::SELLER, 101, 4)), "sell should be accepted");
    require(book.getTrades().empty(), "non-crossing orders must not trade");
    require(book.containsOrder(1) && book.containsOrder(2), "non-crossing orders must rest");
    requireEqual(book.getBestBid()->getPrice(), UINT64_C(99), "best bid");
    requireEqual(book.getBestAskPrice(), UINT64_C(101), "best ask");

    require(book.submitOrder(order(3, Side::SELLER, 98, 7)), "crossing sell should be accepted");
    requireEqual(book.getTrades().size(), std::size_t{1}, "one trade expected");
    const Trade& trade = book.getTrades().front();
    requireEqual(trade.buyOrderId, UINT64_C(1), "trade buy order");
    requireEqual(trade.sellOrderId, UINT64_C(3), "trade sell order");
    requireEqual(trade.price, UINT64_C(99), "trade price is resting price");
    requireEqual(trade.quantity, UINT64_C(5), "trade quantity");
    require(!book.containsOrder(1), "fully filled buy must leave the book");
    require(book.containsOrder(3), "partially filled incoming sell must rest");
    requireLevel(book, Side::SELLER, 98, {{3, 2}});
    requireLevel(book, Side::SELLER, 101, {{2, 4}});
}

void test_price_time_priority_and_multiple_levels()
{
    OrderBook book;
    require(book.submitOrder(order(10, Side::SELLER, 103, 2)), "ask 103");
    require(book.submitOrder(order(11, Side::SELLER, 101, 2)), "ask 101 first");
    require(book.submitOrder(order(12, Side::SELLER, 101, 3)), "ask 101 second");
    require(book.submitOrder(order(13, Side::SELLER, 102, 2)), "ask 102");

    require(book.submitOrder(order(20, Side::BUYER, 103, 8)), "multi-level buy");
    const auto& trades = book.getTrades();
    requireEqual(trades.size(), std::size_t{4}, "all eligible price levels should execute");
    require(trades[0].sellOrderId == 11 && trades[0].price == 101 && trades[0].quantity == 2,
            "best price and FIFO order 1");
    require(trades[1].sellOrderId == 12 && trades[1].price == 101 && trades[1].quantity == 3,
            "best price and FIFO order 2");
    require(trades[2].sellOrderId == 13 && trades[2].price == 102 && trades[2].quantity == 2,
            "next price level order");
    require(trades[3].sellOrderId == 10 && trades[3].price == 103 && trades[3].quantity == 1,
            "last price level partial fill");
    require(!book.containsOrder(11) && !book.containsOrder(12) && !book.containsOrder(13),
            "fully filled asks must be removed");
    requireLevel(book, Side::SELLER, 103, {{10, 1}});

    OrderBook bids;
    require(bids.submitOrder(order(30, Side::BUYER, 99, 2)), "bid 99");
    require(bids.submitOrder(order(31, Side::BUYER, 101, 2)), "bid 101");
    require(bids.submitOrder(order(32, Side::BUYER, 101, 3)), "bid 101 FIFO");
    require(bids.submitOrder(order(33, Side::SELLER, 99, 6)), "sell consumes bids");
    requireEqual(bids.getTrades().size(), std::size_t{3}, "sell should consume three bid orders");
    require(bids.getTrades()[0].buyOrderId == 31 && bids.getTrades()[1].buyOrderId == 32 &&
                bids.getTrades()[2].buyOrderId == 30,
            "bids use best-price then FIFO priority");
    requireLevel(bids, Side::BUYER, 99, {{30, 1}});
}

void test_validation_and_cancellation()
{
    OrderBook book;
    std::vector<SubmitOrderRequest> invalid{
        order(0, Side::BUYER), order(1, Side::BUYER, 0), order(2, Side::BUYER, 100, 0),
        SubmitOrderRequest{3, 1, 100, 1, static_cast<Side>(99)}};
    for (const auto& request : invalid)
    {
        require(!book.submitOrder(request), "invalid order should be rejected");
        requireEqual(book.getLastRejectedOrderId(), request.id, "rejected order id");
        requireEqual(book.getLastRejectionReason(), std::string{"invalid or duplicate order"},
                     "rejection reason");
    }
    require(book.submitOrder(order(40, Side::BUYER, UINT64_MAX, UINT64_MAX)),
            "maximum non-zero price and quantity should be accepted");
    require(!book.submitOrder(order(40, Side::BUYER, 1, 1)), "duplicate order id should be rejected");
    require(book.cancelOrder(40), "resting order should be cancellable");
    require(!book.cancelOrder(40), "unknown cancellation should fail");
    require(book.getTrades().empty(), "cancellation must not create trades");
}

void test_matching_engine_dispatch_and_lifecycle()
{
    MatchingEngine engine(3, 512);
    for (uint64_t instrument = 1; instrument <= 6; ++instrument)
    {
        require(engine.addInstrument(instrument), "instrument should register");
        require(engine.workerForInstrument(instrument) == ((instrument - 1) % 3),
                "instruments should be assigned round-robin");
    }
    std::string reason;
    require(!engine.addInstrument(1, &reason) && reason == "instrument is already registered",
            "duplicate instrument error");
    require(!engine.submitOrder(order(50, Side::BUYER, 100, 1, 99), &reason) &&
                reason == "instrument is not configured",
            "unknown instrument error");

    require(engine.submitOrder(order(51, Side::SELLER, 100, 5)), "engine sell");
    require(engine.submitOrder(order(52, Side::BUYER, 110, 3)), "engine buy");
    const auto trades = engine.getTrades(1);
    requireEqual(trades.size(), std::size_t{1}, "engine should return the latest trades");
    require(trades.front().buyOrderId == 52 && trades.front().sellOrderId == 51 &&
                trades.front().price == 100 && trades.front().quantity == 3,
            "engine trade result");
    require(!engine.submitOrder(order(51, Side::BUYER), &reason) &&
                reason == "order is already active or being processed",
            "engine duplicate active id error");
    require(engine.cancelOrder(51), "partially filled order should be cancellable");
    require(!engine.cancelOrder(51, &reason) && reason == "order was not found",
            "filled/cancelled order should not cancel twice");

    engine.shutdown();
    require(!engine.isRunning(), "shutdown state");
    require(!engine.submitOrder(order(53, Side::BUYER), &reason) &&
                reason == "matching engine is stopped",
            "submission after shutdown");
}

struct ModelOrder
{
    uint64_t id;
    Side side;
    uint64_t price;
    uint64_t quantity;
};

struct ModelTrade
{
    uint64_t buyId;
    uint64_t sellId;
    uint64_t price;
    uint64_t quantity;
};

class ReferenceBook
{
public:
    std::vector<ModelTrade> submit(ModelOrder incoming)
    {
        std::vector<ModelTrade> trades;
        while (incoming.quantity > 0)
        {
            auto candidate = bestOpposite(incoming);
            if (candidate == orders_.end() ||
                (incoming.side == Side::BUYER && incoming.price < candidate->price) ||
                (incoming.side == Side::SELLER && incoming.price > candidate->price))
            {
                break;
            }
            const uint64_t fill = std::min(incoming.quantity, candidate->quantity);
            trades.push_back({incoming.side == Side::BUYER ? incoming.id : candidate->id,
                              incoming.side == Side::SELLER ? incoming.id : candidate->id,
                              candidate->price, fill});
            incoming.quantity -= fill;
            candidate->quantity -= fill;
            if (candidate->quantity == 0)
            {
                orders_.erase(candidate);
            }
        }
        if (incoming.quantity > 0)
        {
            orders_.push_back(incoming);
        }
        return trades;
    }

    std::vector<ModelOrder> at(Side side, uint64_t price) const
    {
        std::vector<ModelOrder> result;
        for (const auto& item : orders_)
        {
            if (item.side == side && item.price == price)
            {
                result.push_back(item);
            }
        }
        return result;
    }

private:
    std::vector<ModelOrder>::iterator bestOpposite(const ModelOrder& incoming)
    {
        auto result = orders_.end();
        for (auto it = orders_.begin(); it != orders_.end(); ++it)
        {
            if (it->side == incoming.side)
            {
                continue;
            }
            if (result == orders_.end() ||
                (incoming.side == Side::BUYER && it->price < result->price) ||
                (incoming.side == Side::SELLER && it->price > result->price))
            {
                result = it;
            }
        }
        return result;
    }

    std::vector<ModelOrder> orders_;
};

void test_randomized_reference_oracle()
{
    constexpr uint64_t seed = 0x5eed1234;
    std::mt19937_64 random(seed);
    std::uniform_int_distribution<uint64_t> price(95, 105);
    std::uniform_int_distribution<uint64_t> quantity(1, 7);
    std::bernoulli_distribution side(0.5);
    OrderBook book;
    ReferenceBook reference;
    std::vector<uint64_t> observedPrices;
    for (uint64_t id = 1; id <= 500; ++id)
    {
        const ModelOrder generated{id, side(random) ? Side::BUYER : Side::SELLER,
                                   price(random), quantity(random)};
        observedPrices.push_back(generated.price);
        const auto expectedTrades = reference.submit(generated);
        require(book.submitOrder(order(generated.id, generated.side, generated.price,
                                       generated.quantity)),
                "random valid submission should be accepted");
        const auto& actualTrades = book.getTrades();
        requireEqual(actualTrades.size(), expectedTrades.size(),
                     "random trade count (seed=0x5eed1234)");
        for (std::size_t index = 0; index < expectedTrades.size(); ++index)
        {
            require(actualTrades[index].buyOrderId == expectedTrades[index].buyId &&
                        actualTrades[index].sellOrderId == expectedTrades[index].sellId &&
                        actualTrades[index].price == expectedTrades[index].price &&
                        actualTrades[index].quantity == expectedTrades[index].quantity,
                    "random trade differs from reference (seed=0x5eed1234, id=" +
                        std::to_string(id) + ")");
        }
        for (const uint64_t candidatePrice : observedPrices)
        {
            for (const Side candidateSide : {Side::BUYER, Side::SELLER})
            {
                const auto expected = reference.at(candidateSide, candidatePrice);
                const auto actual = level(book, candidateSide, candidatePrice);
                require(actual.size() == expected.size(),
                        "random resting order count differs (seed=0x5eed1234)");
                for (std::size_t index = 0; index < expected.size(); ++index)
                {
                    require(actual[index].id == expected[index].id &&
                                actual[index].quantity == expected[index].quantity,
                            "random resting order differs (seed=0x5eed1234, id=" +
                                std::to_string(id) + ")");
                }
            }
        }
    }
}

void test_concurrent_routing()
{
    MatchingEngine engine(3, 512);
    require(engine.addInstrument(42), "instrument should register");
    constexpr std::size_t threadCount = 4;
    constexpr std::size_t ordersPerThread = 50;
    std::atomic<std::size_t> accepted{0};
    std::vector<std::thread> submitters;
    for (std::size_t threadId = 0; threadId < threadCount; ++threadId)
    {
        submitters.emplace_back([&, threadId]() {
            for (std::size_t index = 0; index < ordersPerThread; ++index)
            {
                if (engine.submitOrder(order(1000 + threadId * ordersPerThread + index,
                                             Side::BUYER, 90, 1, 42)))
                {
                    accepted.fetch_add(1, std::memory_order_relaxed);
                }
            }
        });
    }
    for (auto& submitter : submitters)
    {
        submitter.join();
    }
    requireEqual(accepted.load(), threadCount * ordersPerThread, "concurrent accepted orders");
    for (std::size_t index = 0; index < threadCount * ordersPerThread; ++index)
    {
        require(engine.cancelOrder(1000 + index), "concurrent resting order cancellation");
    }
}

void run(const char* name, void (*test)())
{
    try
    {
        test();
        std::cout << "PASS " << name << '\n';
    }
    catch (const std::exception& exception)
    {
        std::cerr << "FAIL " << name << ": " << exception.what() << '\n';
        throw;
    }
}
} // namespace

int main()
{
    run("basic_book_behavior", test_basic_book_behavior);
    run("price_time_priority_and_multiple_levels", test_price_time_priority_and_multiple_levels);
    run("validation_and_cancellation", test_validation_and_cancellation);
    run("matching_engine_dispatch_and_lifecycle", test_matching_engine_dispatch_and_lifecycle);
    run("randomized_reference_oracle", test_randomized_reference_oracle);
    run("concurrent_routing", test_concurrent_routing);
    return 0;
}
