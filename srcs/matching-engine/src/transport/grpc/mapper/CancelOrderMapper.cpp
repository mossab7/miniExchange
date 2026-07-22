#include <transport/grpc/mapper/CancelOrderMapper.hpp>

CancelOrderRequest CancelOrderMapper::fromGrpcRequest(const exchange::CancelOrderRequest& request) const
{
    CancelOrderRequest dto;
    dto.orderId = request.orderid();
    return dto;
}

void CancelOrderMapper::toGrpcResponse(const CancelOrderResponse& dto, exchange::CancelOrderResponse* response) const
{
    response->set_success(dto.success);
    response->set_message(dto.message);
}