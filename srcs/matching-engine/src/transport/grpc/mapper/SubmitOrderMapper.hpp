#ifndef MATCHING_ENGINE_TRANSPORT_GRPC_MAPPER_SUBMIT_ORDER_MAPPER_HPP
#define MATCHING_ENGINE_TRANSPORT_GRPC_MAPPER_SUBMIT_ORDER_MAPPER_HPP

#include <generated/proto/exchange.grpc.pb.h>

#include <application/dto/SubmitOrderRequest.hpp>
#include <application/dto/SubmitOrderResponse.hpp>

class SubmitOrderMapper final
{
    public:
    SubmitOrderRequest fromGrpcRequest(const exchange::OrderRequest& request) const;
    void toGrpcResponse(const SubmitOrderResponse& dto, exchange::OrderResponse* response) const;
};

#endif // MATCHING_ENGINE_TRANSPORT_GRPC_MAPPER_SUBMIT_ORDER_MAPPER_HPP