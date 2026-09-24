#include "application/grpc/ExchangeGrpcServer.hpp"

#include <atomic>
#include <chrono>
#include <csignal>
#include <cstdlib>
#include <iostream>
#include <limits>
#include <memory>
#include <string>
#include <thread>

namespace
{
std::atomic_bool stopRequested{false};

void requestShutdown(int signal)
{
    if (signal == SIGINT || signal == SIGTERM)
    {
        stopRequested.store(true, std::memory_order_relaxed);
    }
}
} // namespace

int main(int argc, char** argv)
{
    const std::string address = argc > 1 ? argv[1] : "0.0.0.0:50051";
    std::size_t workerCount = 1;
    if (argc > 2)
    {
        try
        {
            const unsigned long long parsed = std::stoull(argv[2]);
            if (parsed == 0 || parsed > std::numeric_limits<std::size_t>::max())
            {
                throw std::invalid_argument("worker count must be greater than zero");
            }
            workerCount = static_cast<std::size_t>(parsed);
        }
        catch (const std::exception& exception)
        {
            std::cerr << "invalid worker count: " << exception.what() << '\n';
            return EXIT_FAILURE;
        }
    }

    std::shared_ptr<TradePublisher> tradePublisher = createTradePublisherFromEnvironment();
    MatchingEngine matchingEngine(workerCount, MatchingEngine::kDefaultQueueCapacity,
                                  std::move(tradePublisher));
    ExchangeGrpcServer server(matchingEngine, address);

    std::signal(SIGINT, requestShutdown);
    std::signal(SIGTERM, requestShutdown);

    try
    {
        server.start();
        std::thread serverThread([&server]() { server.wait(); });

        std::cout << "matching engine gRPC server listening on " << address << '\n';
        while (!stopRequested.load(std::memory_order_relaxed))
        {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }

        server.shutdown();
        serverThread.join();
    }
    catch (const std::exception& exception)
    {
        std::cerr << "failed to run matching engine server: " << exception.what() << '\n';
        return EXIT_FAILURE;
    }

    return EXIT_SUCCESS;
}
