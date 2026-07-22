#ifndef MATCHING_ENGINE_TRANSPORT_GRPC_MAPPER_GET_ORDER_MAPPER_HPP
#define MATCHING_ENGINE_TRANSPORT_GRPC_MAPPER_GET_ORDER_MAPPER_HPP

#include <generated/proto/exchange.grpc.pb.h>

#include <application/dto/GetOrderRequest.hpp>
#include <application/dto/GetOrderResponse.hpp>

class GetOrderMapper final
{
    public:
    GetOrderRequest fromGrpcRequest(const exchange::GetOrderRequest& request) const;
    void toGrpcResponse(const GetOrderResponse& dto, exchange::GetOrderResponse* response) const;
};

#endif // MATCHING_ENGINE_TRANSPORT_GRPC_MAPPER_GET_ORDER_MAPPER_HPP