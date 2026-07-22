#ifndef EXCHANGE_GRPC_SERVER_HPP
#define EXCHANGE_GRPC_SERVER_HPP

#include <generated/proto/exchange.grpc.pb.h>

class ExchangeGrpcServer final : public ExchangeGrpcService::Service
{
    
    public:
        ExchangeGrpcServer();
        ~ExchangeGrpcServer();
        grpc::Status PlaceOrder(grpc::ServerContext* context, const PlaceOrderRequest* request, PlaceOrderResponse* response) override;
        grpc::Status CancelOrder(grpc::ServerContext* context, const CancelOrderRequest* request, CancelOrderResponse* response) override;
        grpc::Status GetOrderBook(grpc::ServerContext* context, const GetOrderBookRequest* request, GetOrderBookResponse* response) override;
};


#endif // EXCHANGE_GRPC_SERVER_HPP