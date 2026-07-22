#ifndef MATCHING_ENGINE_TRANSPORT_GRPC_MAPPER_CANCEL_ORDER_MAPPER_HPP
#define MATCHING_ENGINE_TRANSPORT_GRPC_MAPPER_CANCEL_ORDER_MAPPER_HPP

#include <generated/proto/exchange.grpc.pb.h>

#include <application/dto/CancelOrderRequest.hpp>
#include <application/dto/CancelOrderResponse.hpp>

class CancelOrderMapper final
{
    public:
    CancelOrderRequest fromGrpcRequest(const exchange::CancelOrderRequest& request) const;
    void toGrpcResponse(const CancelOrderResponse& dto, exchange::CancelOrderResponse* response) const;
};

#endif // MATCHING_ENGINE_TRANSPORT_GRPC_MAPPER_CANCEL_ORDER_MAPPER_HPP