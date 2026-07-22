#include <transport/grpc/mapper/GetOrderMapper.hpp>

GetOrderRequest GetOrderMapper::fromGrpcRequest(const exchange::GetOrderRequest& request) const
{
    GetOrderRequest dto;
    dto.orderId = request.orderid();
    return dto;
}

void GetOrderMapper::toGrpcResponse(const GetOrderResponse& dto, exchange::GetOrderResponse* response) const
{
    response->set_orderid(dto.orderId);
    response->set_symbol(dto.symbol);
}