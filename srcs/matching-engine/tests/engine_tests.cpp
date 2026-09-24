#include "engine/OrderBook/OrderBook.hpp"
#include "engine/matchingEngine/matchingEngine.hpp"

#ifdef NDEBUG
#undef NDEBUG
#endif

#include <atomic>
#include <cassert>
#include <cstddef>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>

namespace
{
SubmitOrderRequest order(uint64_t id, uint64_t instrument, Side side,
                         uint64_t price = 100, uint64_t quantity = 1)
{
    return {id, instrument, price, quantity, side};
}

void test_round_robin_assignment()
{
    MatchingEngine engine(3, 64);
    for (uint64_t instrument = 1; instrument <= 6; ++instrument)
    {
        assert(engine.addInstrument(instrument));
        assert(engine.workerForInstrument(instrument) == ((instrument - 1) % 3));
    }

    assert(engine.workerForInstrument(1) == engine.workerForInstrument(1));
    assert(engine.workerForInstrument(1) == 0);
    assert(engine.workerForInstrument(4) == 0);
    assert(engine.workerForInstrument(2) == 1);
    assert(engine.workerForInstrument(5) == 1);
    assert(engine.workerForInstrument(3) == 2);
    assert(engine.workerForInstrument(6) == 2);

    std::string reason;
    assert(!engine.addInstrument(1, &reason));
    assert(reason == "instrument is already registered");
    assert(!engine.submitOrder(order(1, 99, Side::BUYER), &reason));
    assert(reason == "instrument is not configured");
}

void test_single_worker_and_validation()
{
    MatchingEngine engine(1);
    assert(engine.addInstrument(7));
    assert(engine.workerForInstrument(7) == 0);

    std::string reason;
    assert(!engine.submitOrder(order(0, 7, Side::BUYER), &reason));
    assert(reason == "invalid or duplicate order");
    assert(!engine.addInstrument(7, &reason));
    assert(reason == "instrument is already registered");

    bool threw = false;
    try
    {
        MatchingEngine invalid(0);
        (void)invalid;
    }
    catch (const std::invalid_argument&)
    {
        threw = true;
    }
    assert(threw);

    threw = false;
    try
    {
        MatchingEngine invalidQueue(1, 0);
        (void)invalidQueue;
    }
    catch (const std::invalid_argument&)
    {
        threw = true;
    }
    assert(threw);
}

void test_matching_and_cancellation()
{
    MatchingEngine engine(2);
    assert(engine.addInstrument(1));

    assert(engine.submitOrder(order(10, 1, Side::SELLER, 100, 5)));
    assert(engine.submitOrder(order(11, 1, Side::BUYER, 110, 3)));
    const auto trades = engine.getTrades(1);
    assert(trades.size() == 1);
    assert(trades.front().buyOrderId == 11);
    assert(trades.front().sellOrderId == 10);
    assert(trades.front().price == 100);
    assert(trades.front().quantity == 3);

    assert(engine.submitOrder(order(12, 1, Side::BUYER, 100, 2)));
    std::string reason;
    assert(!engine.cancelOrder(10, &reason));
    assert(reason == "order was not found");
    assert(!engine.cancelOrder(10, &reason));
    assert(reason == "order was not found");
    assert(!engine.cancelOrder(11, &reason));
}

void test_concurrent_routing()
{
    MatchingEngine engine(3, 8);
    assert(engine.addInstrument(42));

    constexpr std::size_t threadCount = 4;
    constexpr std::size_t ordersPerThread = 50;
    std::atomic<std::size_t> accepted{0};
    std::vector<std::thread> submitters;
    submitters.reserve(threadCount);
    for (std::size_t threadId = 0; threadId < threadCount; ++threadId)
    {
        submitters.emplace_back([&, threadId]() {
            for (std::size_t index = 0; index < ordersPerThread; ++index)
            {
                const uint64_t id = 1000 + threadId * ordersPerThread + index;
                if (engine.submitOrder(order(id, 42, Side::BUYER, 90)))
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
    assert(accepted == threadCount * ordersPerThread);

    for (std::size_t index = 0; index < threadCount * ordersPerThread; ++index)
    {
        assert(engine.cancelOrder(1000 + index));
    }
}

void test_order_book_matching_is_preserved()
{
    OrderBook book;
    assert(book.submitOrder(order(1, 1, Side::SELLER, 100, 5)));
    assert(book.submitOrder(order(2, 1, Side::BUYER, 100, 2)));
    assert(book.getTrades().size() == 1);
    assert(book.getTrades().front().quantity == 2);
    assert(book.containsOrder(1));
    assert(!book.containsOrder(2));
}
} // namespace

int main()
{
    test_round_robin_assignment();
    test_single_worker_and_validation();
    test_matching_and_cancellation();
    test_concurrent_routing();
    test_order_book_matching_is_preserved();
    return 0;
}
