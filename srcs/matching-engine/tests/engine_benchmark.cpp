#include "engine/OrderBook/OrderBook.hpp"

#include <algorithm>
#include <atomic>
#include <chrono>
#include <cstdint>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <limits>
#include <random>
#include <set>
#include <stdexcept>
#include <string>
#include <string_view>
#include <vector>

namespace
{
using Clock = std::chrono::steady_clock;

std::atomic<uint64_t> benchmarkSink{0};

enum class OperationType
{
    Submit,
    Cancel
};

struct Operation
{
    OperationType type;
    SubmitOrderRequest request{};
    uint64_t orderId{0};
    bool expectedSuccess{true};
};

struct ScenarioDefinition
{
    std::string name;
    std::string operation;
    std::vector<SubmitOrderRequest> setup;
    std::vector<Operation> operations;
    std::size_t expectedTrades{0};
    std::size_t expectedFailedOperations{0};
    bool expectCancellation{false};
    std::size_t priceLevels{0};
    std::string priceDistribution{"cyclic"};
};

struct Result
{
    std::string scenario;
    std::size_t setupOrders{0};
    std::size_t initialBookSize{0};
    std::size_t measuredSubmissions{0};
    std::size_t measuredOperations{0};
    std::size_t successfulOperations{0};
    std::size_t failedOperations{0};
    std::size_t trades{0};
    uint64_t executedQuantity{0};
    std::size_t finalBookSize{0};
    std::size_t repetitions{0};
    uint64_t seed{0};
    std::size_t priceLevels{0};
    std::size_t latencySamples{0};
    double elapsedMilliseconds{0.0};
    double attemptedOperationsPerSecond{0.0};
    double operationsPerSecond{0.0};
    double submissionsPerSecond{0.0};
    double tradesPerSecond{0.0};
    double medianOperationsPerSecond{0.0};
    double minimumOperationsPerSecond{0.0};
    double maximumOperationsPerSecond{0.0};
    double p50LatencyNanoseconds{0.0};
    double p90LatencyNanoseconds{0.0};
    double p95LatencyNanoseconds{0.0};
    double p99LatencyNanoseconds{0.0};
    double p999LatencyNanoseconds{0.0};
    double maximumLatencyNanoseconds{0.0};
};

void fail(const std::string& message)
{
    throw std::runtime_error(message);
}

void expect(bool condition, const std::string& message)
{
    if (!condition)
    {
        fail(message);
    }
}

SubmitOrderRequest makeOrder(uint64_t id, Side side, uint64_t price, uint64_t quantity)
{
    return {id, 1, price, quantity, side};
}

void addSubmit(ScenarioDefinition& scenario, const SubmitOrderRequest& request)
{
    scenario.operations.push_back({OperationType::Submit, request, 0, true});
}

ScenarioDefinition buildScenario(std::string_view name, std::size_t count, uint64_t seed,
                                 std::size_t priceLevels, std::string_view priceDistribution)
{
    ScenarioDefinition scenario{std::string{name}, "submit", {}, {}, 0, 0, false,
                                std::max<std::size_t>(1, priceLevels), std::string{priceDistribution}};
    std::mt19937_64 random(seed);
    if (name == "no-match-insertion")
    {
        for (std::size_t index = 0; index < count; ++index)
        {
            const uint64_t level = scenario.priceDistribution == "uniform"
                                       ? random() % scenario.priceLevels
                                       : index % scenario.priceLevels;
            addSubmit(scenario, makeOrder(index + 1, Side::BUYER, 90 + level, 1));
        }
    }
    else if (name == "immediate-matching")
    {
        for (std::size_t index = 0; index < count; ++index)
        {
            scenario.setup.push_back(makeOrder(index + 1, Side::SELLER, 100, 1));
            addSubmit(scenario, makeOrder(count + index + 1, Side::BUYER, 100, 1));
        }
        scenario.expectedTrades = count;
    }
    else if (name == "partial-fills")
    {
        for (std::size_t index = 0; index < count; ++index)
        {
            scenario.setup.push_back(makeOrder(index + 1, Side::SELLER, 100, 2));
            addSubmit(scenario, makeOrder(count + index + 1, Side::BUYER, 100, 1));
        }
        scenario.expectedTrades = count;
    }
    else if (name == "multi-order-matching")
    {
        for (std::size_t index = 0; index < count; ++index)
        {
            scenario.setup.push_back(makeOrder(2 * index + 1, Side::SELLER, 100, 1));
            scenario.setup.push_back(makeOrder(2 * index + 2, Side::SELLER, 100, 1));
            addSubmit(scenario, makeOrder(2 * count + index + 1, Side::BUYER, 100, 2));
        }
        scenario.expectedTrades = count * 2;
    }
    else if (name == "price-level-traversal")
    {
        for (std::size_t index = 0; index < count; ++index)
        {
            scenario.setup.push_back(makeOrder(index + 1, Side::SELLER, 100 + index, 1));
        }
        addSubmit(scenario, makeOrder(count + 1, Side::BUYER, 100 + count, count));
        scenario.expectedTrades = count;
    }
    else if (name == "cancellation")
    {
        scenario.operation = "cancel";
        for (std::size_t index = 0; index < count; ++index)
        {
            scenario.setup.push_back(makeOrder(index + 1, Side::BUYER, 90 + index % 3, 1));
            scenario.operations.push_back({OperationType::Cancel, {}, index + 1, true});
        }
        scenario.expectCancellation = true;
    }
    else if (name == "cancellation-mixed")
    {
        scenario.operation = "cancel";
        const std::size_t activeOrders = (count + 1) / 2;
        for (std::size_t index = 0; index < activeOrders; ++index)
        {
            scenario.setup.push_back(makeOrder(index + 1, Side::BUYER, 90 + index % 3, 1));
        }
        for (std::size_t index = 0; index < count; ++index)
        {
            if (index % 2 == 0)
            {
                scenario.operations.push_back({OperationType::Cancel, {}, index / 2 + 1, true});
            }
            else
            {
                scenario.operations.push_back(
                    {OperationType::Cancel, {}, UINT64_C(1000000000000) + index, false});
                ++scenario.expectedFailedOperations;
            }
        }
        scenario.expectCancellation = true;
    }
    else if (name == "mixed-workload")
    {
        scenario.operation = "mixed";
        std::mt19937_64 random(seed);
        uint64_t nextId = 1;
        std::vector<uint64_t> cancellable;
        for (std::size_t index = 0; index < count; ++index)
        {
            switch (index % 4)
            {
            case 0:
            {
                const uint64_t id = nextId++;
                cancellable.push_back(id);
                addSubmit(scenario, makeOrder(id, Side::BUYER, 80 + random() % 5, 1));
                break;
            }
            case 1:
            {
                const uint64_t id = nextId++;
                addSubmit(scenario, makeOrder(id, Side::SELLER, 120 + random() % 5, 1));
                break;
            }
            case 2:
            {
                const uint64_t id = nextId++;
                addSubmit(scenario, makeOrder(id, Side::BUYER, 125 + random() % 5, 1));
                scenario.expectedTrades++;
                break;
            }
            case 3:
                scenario.operations.push_back({OperationType::Cancel, {}, cancellable.front(), true});
                cancellable.erase(cancellable.begin());
                scenario.expectCancellation = true;
                break;
            }
        }
    }
    else
    {
        fail("unknown scenario: " + std::string{name});
    }
    return scenario;
}

void setupBook(OrderBook& book, const ScenarioDefinition& scenario)
{
    for (const auto& request : scenario.setup)
    {
        expect(book.submitOrder(request), "scenario setup order was rejected");
    }
}

std::vector<uint64_t> knownOrderIds(const ScenarioDefinition& scenario)
{
    std::set<uint64_t> ids;
    for (const auto& request : scenario.setup)
    {
        ids.insert(request.id);
    }
    for (const auto& operation : scenario.operations)
    {
        if (operation.type == OperationType::Submit)
        {
            ids.insert(operation.request.id);
        }
        else
        {
            ids.insert(operation.orderId);
        }
    }
    return {ids.begin(), ids.end()};
}

std::size_t bookSize(const OrderBook& book, const std::vector<uint64_t>& ids)
{
    std::size_t active = 0;
    for (const uint64_t id : ids)
    {
        active += book.containsOrder(id) ? 1 : 0;
    }
    return active;
}

bool apply(OrderBook& book, const Operation& operation)
{
    return operation.type == OperationType::Submit
               ? book.submitOrder(operation.request)
               : book.cancelOrder(operation.orderId);
}

void validateScenario(const OrderBook& book, const ScenarioDefinition& scenario,
                      std::size_t trades, uint64_t quantity, std::size_t cancellations,
                      std::size_t failedOperations)
{
    expect(trades == scenario.expectedTrades,
           scenario.name + ": unexpected trade count (expected=" +
               std::to_string(scenario.expectedTrades) + ", actual=" + std::to_string(trades) + ')');
    expect(failedOperations == scenario.expectedFailedOperations,
           scenario.name + ": unexpected failed-operation count");
    if (scenario.name == "no-match-insertion")
    {
        expect(trades == 0 && quantity == 0, "no-match scenario executed a trade");
    }
    if (scenario.name == "immediate-matching")
    {
        expect(quantity == scenario.expectedTrades, "immediate matching quantity mismatch");
        for (const auto& request : scenario.setup)
        {
            expect(!book.containsOrder(request.id), "fully consumed setup order remained active");
        }
    }
    if (scenario.name == "partial-fills")
    {
        expect(quantity == scenario.expectedTrades, "partial fill quantity mismatch");
        bool hasPartiallyFilledOrder = false;
        for (const auto& request : scenario.setup)
        {
            hasPartiallyFilledOrder = hasPartiallyFilledOrder || book.containsOrder(request.id);
        }
        expect(hasPartiallyFilledOrder, "partial-fill scenario left no resting liquidity");
    }
    if (scenario.expectCancellation)
    {
        expect(cancellations > 0, "cancellation scenario did not cancel any order");
    }
    if (scenario.name == "mixed-workload")
    {
        expect(cancellations > 0 && trades > 0, "mixed workload did not exercise all operations");
    }
}

double percentile(std::vector<uint64_t> values, double fraction)
{
    if (values.empty())
    {
        return 0.0;
    }
    const std::size_t index = std::min(
        values.size() - 1, static_cast<std::size_t>(fraction * static_cast<double>(values.size())));
    std::nth_element(values.begin(), values.begin() + index, values.end());
    return static_cast<double>(values[index]);
}

Result runScenario(std::string_view name, std::size_t count, std::size_t repetitions,
                   std::size_t warmup, uint64_t seed, std::size_t priceLevels,
                   std::string_view priceDistribution, bool measureLatency)
{
    const ScenarioDefinition definition =
        buildScenario(name, count, seed, priceLevels, priceDistribution);
    const std::vector<uint64_t> knownIds = knownOrderIds(definition);
    for (std::size_t iteration = 0; iteration < warmup; ++iteration)
    {
        OrderBook book;
        setupBook(book, definition);
        for (const auto& operation : definition.operations)
        {
            expect(apply(book, operation) == operation.expectedSuccess,
                   "warmup operation result differed from the generated workload");
        }
    }

    std::vector<double> throughputs;
    std::vector<uint64_t> latencySamples;
    throughputs.reserve(repetitions);
    std::size_t totalTrades = 0;
    uint64_t totalQuantity = 0;
    std::size_t totalSuccessfulOperations = 0;
    std::size_t totalSuccessfulSubmissions = 0;
    std::size_t totalFailedOperations = 0;
    std::size_t totalInitialBookSize = 0;
    std::size_t totalFinalBookSize = 0;
    double totalElapsedMilliseconds = 0.0;

    for (std::size_t iteration = 0; iteration < repetitions; ++iteration)
    {
        OrderBook book;
        setupBook(book, definition);
        const std::size_t initialBookSize = bookSize(book, knownIds);
        std::size_t trades = 0;
        uint64_t quantity = 0;
        std::size_t cancellations = 0;
        std::size_t successfulOperations = 0;
        std::size_t successfulSubmissions = 0;
        std::size_t failedOperations = 0;
        const auto start = Clock::now();
        for (const auto& operation : definition.operations)
        {
            const auto operationStart = measureLatency ? Clock::now() : Clock::time_point{};
            const bool succeeded = apply(book, operation);
            if (succeeded)
            {
                ++successfulOperations;
                if (operation.type == OperationType::Submit)
                {
                    ++successfulSubmissions;
                    benchmarkSink.fetch_add(1, std::memory_order_relaxed);
                    const auto& operationTrades = book.getExecutions();
                    trades += operationTrades.size();
                    for (const auto& trade : operationTrades)
                    {
                        quantity += trade.quantity;
                        benchmarkSink.fetch_xor(trade.quantity + trade.price,
                                                std::memory_order_relaxed);
                    }
                }
            }
            else
            {
                ++failedOperations;
            }
            if (succeeded != operation.expectedSuccess)
            {
                fail("generated operation result differed from the engine");
            }
            if (succeeded && operation.type == OperationType::Cancel)
            {
                ++cancellations;
                benchmarkSink.fetch_add(1, std::memory_order_relaxed);
            }
            if (measureLatency)
            {
                latencySamples.push_back(static_cast<uint64_t>(
                    std::chrono::duration_cast<std::chrono::nanoseconds>(Clock::now() - operationStart)
                        .count()));
            }
        }
        const double elapsedMilliseconds =
            std::chrono::duration<double, std::milli>(Clock::now() - start).count();
        const double elapsedSeconds = elapsedMilliseconds / 1000.0;
        const double operationsPerSecond = elapsedSeconds == 0.0
                                               ? std::numeric_limits<double>::infinity()
                                               : successfulOperations / elapsedSeconds;
        throughputs.push_back(operationsPerSecond);
        totalElapsedMilliseconds += elapsedMilliseconds;
        totalTrades += trades;
        totalQuantity += quantity;
        totalSuccessfulOperations += successfulOperations;
        totalSuccessfulSubmissions += successfulSubmissions;
        totalFailedOperations += failedOperations;
        totalInitialBookSize += initialBookSize;
        totalFinalBookSize += bookSize(book, knownIds);
        validateScenario(book, definition, trades, quantity, cancellations, failedOperations);
    }

    std::sort(throughputs.begin(), throughputs.end());
    const double averageElapsedSeconds = totalElapsedMilliseconds / 1000.0 / repetitions;
    const double averageAttemptedOperations = static_cast<double>(definition.operations.size());
    const double averageSuccessfulOperations =
        static_cast<double>(totalSuccessfulOperations) / repetitions;
    const double averageSubmissions = static_cast<double>(totalSuccessfulSubmissions) / repetitions;
    const double averageTrades = static_cast<double>(totalTrades) / repetitions;
    Result result;
    result.scenario = definition.name;
    result.setupOrders = definition.setup.size();
    result.initialBookSize = totalInitialBookSize / repetitions;
    result.measuredSubmissions = static_cast<std::size_t>(averageSubmissions);
    result.measuredOperations = definition.operations.size();
    result.successfulOperations = static_cast<std::size_t>(averageSuccessfulOperations);
    result.failedOperations = totalFailedOperations / repetitions;
    result.trades = static_cast<std::size_t>(averageTrades);
    result.executedQuantity = totalQuantity / repetitions;
    result.finalBookSize = totalFinalBookSize / repetitions;
    result.repetitions = repetitions;
    result.seed = seed;
    result.priceLevels = definition.priceLevels;
    result.latencySamples = latencySamples.size();
    result.elapsedMilliseconds = totalElapsedMilliseconds / repetitions;
    result.attemptedOperationsPerSecond = averageAttemptedOperations / averageElapsedSeconds;
    result.operationsPerSecond = averageSuccessfulOperations / averageElapsedSeconds;
    result.submissionsPerSecond = averageSubmissions / averageElapsedSeconds;
    result.tradesPerSecond = averageTrades / averageElapsedSeconds;
    result.medianOperationsPerSecond = throughputs[throughputs.size() / 2];
    result.minimumOperationsPerSecond = throughputs.front();
    result.maximumOperationsPerSecond = throughputs.back();
    result.p50LatencyNanoseconds = percentile(latencySamples, 0.50);
    result.p90LatencyNanoseconds = percentile(latencySamples, 0.90);
    result.p95LatencyNanoseconds = percentile(latencySamples, 0.95);
    result.p99LatencyNanoseconds = percentile(latencySamples, 0.99);
    result.p999LatencyNanoseconds = percentile(latencySamples, 0.999);
    result.maximumLatencyNanoseconds = latencySamples.empty()
                                           ? 0.0
                                           : static_cast<double>(*std::max_element(
                                                 latencySamples.begin(), latencySamples.end()));
    return result;
}

std::vector<std::string> requestedScenarios(const std::string& requested)
{
    if (requested != "all")
    {
        return {requested};
    }
    return {"no-match-insertion", "immediate-matching", "partial-fills",
            "multi-order-matching", "price-level-traversal", "cancellation", "cancellation-mixed",
            "mixed-workload"};
}

void writeCsv(const std::string& path, const std::vector<Result>& results)
{
    std::ofstream output(path);
    if (!output)
    {
        fail("cannot open CSV output: " + path);
    }
    output << "scenario,setup_orders,initial_book_size,measured_submissions,measured_operations,"
              "successful_operations,failed_operations,trades,executed_quantity,final_book_size,"
              "repetitions,seed,price_levels,latency_samples,elapsed_ms,attempted_operations_per_second,"
              "operations_per_second,submissions_per_second,trades_per_second,median_operations_per_second,"
              "min_operations_per_second,max_operations_per_second,p50_latency_ns,p90_latency_ns,"
              "p95_latency_ns,p99_latency_ns,p99_9_latency_ns,max_latency_ns,correctness_failures\n";
    output << std::setprecision(17);
    for (const auto& result : results)
    {
        output << result.scenario << ',' << result.setupOrders << ',' << result.initialBookSize << ','
               << result.measuredSubmissions << ',' << result.measuredOperations << ','
               << result.successfulOperations << ',' << result.failedOperations << ',' << result.trades
               << ',' << result.executedQuantity << ',' << result.finalBookSize << ',' << result.repetitions
               << ',' << result.seed << ',' << result.priceLevels << ',' << result.latencySamples << ','
               << result.elapsedMilliseconds << ',' << result.attemptedOperationsPerSecond << ','
               << result.operationsPerSecond << ',' << result.submissionsPerSecond << ','
               << result.tradesPerSecond << ',' << result.medianOperationsPerSecond << ','
               << result.minimumOperationsPerSecond << ',' << result.maximumOperationsPerSecond << ','
               << result.p50LatencyNanoseconds << ',' << result.p90LatencyNanoseconds << ','
               << result.p95LatencyNanoseconds << ',' << result.p99LatencyNanoseconds << ','
               << result.p999LatencyNanoseconds << ',' << result.maximumLatencyNanoseconds << ',' << 0 << '\n';
    }
}

void printUsage(const char* executable)
{
    std::cout << "Usage: " << executable
              << " [--scenario all|name] [--orders N] [--iterations N] [--warmup N]"
                 " [--seed N] [--price-levels N] [--price-distribution cyclic|uniform]"
                 " [--latency] [--csv PATH]\n";
}
} // namespace

int main(int argc, char** argv)
{
    try
    {
        std::string scenario = "all";
        std::size_t orders = 10000;
        std::size_t iterations = 7;
        std::size_t warmup = 1;
        uint64_t seed = 0xC0FFEE;
        std::size_t priceLevels = 100;
        std::string priceDistribution = "cyclic";
        bool latency = false;
        std::string csvPath;
        for (int index = 1; index < argc; ++index)
        {
            const std::string argument = argv[index];
            auto value = [&](const char* option) -> std::string {
                if (index + 1 >= argc)
                {
                    fail(std::string("missing value for ") + option);
                }
                return argv[++index];
            };
            if (argument == "--scenario") scenario = value("--scenario");
            else if (argument == "--orders") orders = std::stoull(value("--orders"));
            else if (argument == "--iterations") iterations = std::stoull(value("--iterations"));
            else if (argument == "--warmup") warmup = std::stoull(value("--warmup"));
            else if (argument == "--seed") seed = std::stoull(value("--seed"));
            else if (argument == "--price-levels") priceLevels = std::stoull(value("--price-levels"));
            else if (argument == "--price-distribution") priceDistribution = value("--price-distribution");
            else if (argument == "--latency") latency = true;
            else if (argument == "--csv") csvPath = value("--csv");
            else if (argument == "--help" || argument == "-h")
            {
                printUsage(argv[0]);
                return 0;
            }
            else
            {
                fail("unknown argument: " + argument);
            }
        }
        expect(orders > 0 && iterations > 0 && priceLevels > 0,
               "orders, iterations, and price-levels must be greater than zero");
        expect(priceDistribution == "cyclic" || priceDistribution == "uniform",
               "price-distribution must be cyclic or uniform");
        const auto scenarios = requestedScenarios(scenario);
        std::vector<Result> results;
        results.reserve(scenarios.size());
        std::cout << "compiler=" << __VERSION__ << " build="
#ifdef NDEBUG
                  << "Release\n";
#else
                  << "non-Release (compile with -DCMAKE_BUILD_TYPE=Release for performance)\n";
#endif
        std::cout << "orders=" << orders << " iterations=" << iterations << " warmup=" << warmup
                  << " seed=" << seed << " price_levels=" << priceLevels
                  << " price_distribution=" << priceDistribution
                  << " latency=" << (latency ? "enabled" : "disabled") << '\n';
        for (const auto& selectedScenario : scenarios)
        {
            Result result = runScenario(selectedScenario, orders, iterations, warmup, seed,
                                        priceLevels, priceDistribution, latency);
            results.push_back(result);
            std::cout << std::fixed << std::setprecision(2) << result.scenario
                      << ": elapsed_ms=" << result.elapsedMilliseconds
                      << " operations/s=" << result.operationsPerSecond
                      << " submissions/s=" << result.submissionsPerSecond
                      << " attempted_operations/s=" << result.attemptedOperationsPerSecond
                      << " trades/s=" << result.tradesPerSecond << " trades=" << result.trades
                      << " executed_quantity=" << result.executedQuantity
                      << " successful_operations=" << result.successfulOperations
                      << " failed_operations=" << result.failedOperations
                      << " initial_book=" << result.initialBookSize
                      << " final_book=" << result.finalBookSize
                      << " repetitions=" << result.repetitions << '\n';
            if (latency)
            {
                std::cout << "  latency_ns: p50=" << result.p50LatencyNanoseconds
                          << " p90=" << result.p90LatencyNanoseconds
                          << " p95=" << result.p95LatencyNanoseconds
                          << " p99=" << result.p99LatencyNanoseconds
                          << " p99.9=" << result.p999LatencyNanoseconds
                          << " max=" << result.maximumLatencyNanoseconds
                          << " samples=" << result.latencySamples << '\n';
            }
        }
        if (!csvPath.empty())
        {
            writeCsv(csvPath, results);
            std::cout << "csv=" << csvPath << '\n';
        }
        return 0;
    }
    catch (const std::exception& exception)
    {
        std::cerr << "benchmark failed: " << exception.what() << '\n';
        return 1;
    }
}
