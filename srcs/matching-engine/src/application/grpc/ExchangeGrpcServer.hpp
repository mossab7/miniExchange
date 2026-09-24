#ifndef MATCHING_ENGINE_GRPC_SERVER_HPP
#define MATCHING_ENGINE_GRPC_SERVER_HPP

#include <grpcpp/grpcpp.h>

#include <memory>
#include <string>

#include "ExchangeService.hpp"
#include "engine/matchingEngine/matchingEngine.hpp"

class ExchangeGrpcServer final
{
public:
    ExchangeGrpcServer(MatchingEngine& matchingEngine, std::string address);
    ~ExchangeGrpcServer();

    ExchangeGrpcServer(const ExchangeGrpcServer&) = delete;
    ExchangeGrpcServer& operator=(const ExchangeGrpcServer&) = delete;

    void start();
    void wait();
    void shutdown();

private:
    MatchingEngine& matchingEngine_;
    std::string address_;
    ExchangeService service_;
    std::unique_ptr<grpc::Server> server_;
};

#endif // MATCHING_ENGINE_GRPC_SERVER_HPP
