#include <transport/grpc/mapper/SubmitOrderMapper.hpp>

SubmitOrderRequest SubmitOrderMapper::fromGrpcRequest(const exchange::OrderRequest& request) const
{
    SubmitOrderRequest dto;
    dto.orderId = "";
    dto.userId = request.clientid();
    dto.side = request.side() == exchange::BUY ? "BUY" : "SELL";
    dto.symbol = request.symbol();
    dto.price = request.price();
    dto.quantity = static_cast<int>(request.quantity());

    return dto;
}

void SubmitOrderMapper::toGrpcResponse(const SubmitOrderResponse& dto, exchange::OrderResponse* response) const
{
    response->set_accepted(dto.success);
    response->set_orderid(dto.orderId);
    response->set_message(dto.message);
}