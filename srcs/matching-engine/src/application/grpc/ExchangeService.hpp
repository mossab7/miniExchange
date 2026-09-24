#ifndef MATCHING_ENGINE_GRPC_EXCHANGE_SERVICE_HPP
#define MATCHING_ENGINE_GRPC_EXCHANGE_SERVICE_HPP

#include <grpcpp/grpcpp.h>

#include <miniExchange.grpc.pb.h>

#include "engine/matchingEngine/matchingEngine.hpp"

class ExchangeService final : public miniExchange::MiniExchangeService::Service
{
public:
    explicit ExchangeService(MatchingEngine& matchingEngine);

    grpc::Status AddInstrument(grpc::ServerContext* context,
                               const miniExchange::AddInstrumentRequest* request,
                               miniExchange::AddInstrumentResponse* response) override;

    grpc::Status SubmitOrder(grpc::ServerContext* context,
                             const miniExchange::SubmitOrderRequest* request,
                             miniExchange::SubmitOrderResponse* response) override;

    grpc::Status CancelOrder(grpc::ServerContext* context,
                             const miniExchange::CancelOrderRequest* request,
                             miniExchange::CancelOrderResponse* response) override;

private:
    MatchingEngine& matchingEngine_;
};

#endif // MATCHING_ENGINE_GRPC_EXCHANGE_SERVICE_HPP
