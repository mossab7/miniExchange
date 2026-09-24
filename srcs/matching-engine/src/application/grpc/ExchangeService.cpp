#include "ExchangeService.hpp"

#include <exception>
#include <string>

namespace
{
grpc::Status statusForEngineFailure(const std::string& reason)
{
    if (reason == "instrument is not configured")
    {
        return {grpc::StatusCode::NOT_FOUND, reason};
    }
    if (reason == "instrument is already registered" ||
        reason == "order is already active or being processed" ||
        reason == "invalid or duplicate order")
    {
        return {grpc::StatusCode::ALREADY_EXISTS, reason};
    }
    if (reason.find("queue is full") != std::string::npos)
    {
        return {grpc::StatusCode::RESOURCE_EXHAUSTED, reason};
    }
    return {grpc::StatusCode::INVALID_ARGUMENT,
            reason.empty() ? "matching-engine request was rejected" : reason};
}
} // namespace

ExchangeService::ExchangeService(MatchingEngine& matchingEngine)
    : matchingEngine_(matchingEngine)
{
}

grpc::Status ExchangeService::AddInstrument(
    grpc::ServerContext* context,
    const miniExchange::AddInstrumentRequest* request,
    miniExchange::AddInstrumentResponse* response)
{
    (void)context;
    if (request == nullptr || response == nullptr || request->instrumentid() == 0)
    {
        return {grpc::StatusCode::INVALID_ARGUMENT, "instrument id must be greater than zero"};
    }

    std::string reason;
    if (!matchingEngine_.addInstrument(request->instrumentid(), &reason))
    {
        return statusForEngineFailure(reason);
    }

    response->set_instrumentid(request->instrumentid());
    response->set_success(true);
    return grpc::Status::OK;
}

grpc::Status ExchangeService::SubmitOrder(
    grpc::ServerContext* context,
    const miniExchange::SubmitOrderRequest* request,
    miniExchange::SubmitOrderResponse* response)
{
    (void)context;
    if (request == nullptr || response == nullptr)
    {
        return {grpc::StatusCode::INVALID_ARGUMENT, "request and response are required"};
    }
    if (request->orderid() == 0 || request->instrumentid() == 0 ||
        request->price() == 0 || request->quantity() == 0 ||
        !miniExchange::Side_IsValid(request->side()))
    {
        return {grpc::StatusCode::INVALID_ARGUMENT, "invalid order fields"};
    }

    const SubmitOrderRequest engineRequest{
        request->orderid(),
        request->instrumentid(),
        request->price(),
        request->quantity(),
        request->side() == miniExchange::BUY ? Side::BUYER : Side::SELLER};

    std::string reason;
    try
    {
        if (!matchingEngine_.submitOrder(engineRequest, &reason))
        {
            return statusForEngineFailure(reason);
        }
    }
    catch (const std::exception& exception)
    {
        return {grpc::StatusCode::INTERNAL, exception.what()};
    }

    response->set_orderid(request->orderid());
    response->set_success(true);
    return grpc::Status::OK;
}

grpc::Status ExchangeService::CancelOrder(
    grpc::ServerContext* context,
    const miniExchange::CancelOrderRequest* request,
    miniExchange::CancelOrderResponse* response)
{
    (void)context;
    if (request == nullptr || response == nullptr || request->orderid() == 0)
    {
        return {grpc::StatusCode::INVALID_ARGUMENT, "order id must be greater than zero"};
    }

    std::string reason;
    if (!matchingEngine_.cancelOrder(request->orderid(), &reason))
    {
        return statusForEngineFailure(reason);
    }

    response->set_orderid(request->orderid());
    response->set_success(true);
    return grpc::Status::OK;
}
