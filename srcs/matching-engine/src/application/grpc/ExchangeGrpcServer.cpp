#include "ExchangeGrpcServer.hpp"

#include <stdexcept>
#include <utility>

ExchangeGrpcServer::ExchangeGrpcServer(MatchingEngine& matchingEngine, std::string address)
    : matchingEngine_(matchingEngine),
      address_(std::move(address)),
      service_(matchingEngine_)
{
}

ExchangeGrpcServer::~ExchangeGrpcServer()
{
    shutdown();
}

void ExchangeGrpcServer::start()
{
    if (server_ != nullptr)
    {
        throw std::logic_error("gRPC server is already running");
    }

    grpc::ServerBuilder builder;
    builder.AddListeningPort(address_, grpc::InsecureServerCredentials());
    builder.RegisterService(&service_);
    server_ = builder.BuildAndStart();

    if (server_ == nullptr)
    {
        throw std::runtime_error("failed to start gRPC server on " + address_);
    }
}

void ExchangeGrpcServer::wait()
{
    if (server_ == nullptr)
    {
        throw std::logic_error("gRPC server has not been started");
    }
    server_->Wait();
}

void ExchangeGrpcServer::shutdown()
{
    if (server_ != nullptr)
    {
        server_->Shutdown();
    }
}
