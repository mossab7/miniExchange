package dev.miniExchange.infra.grpc;

import dev.miniExchange.infra.grpc.generated.CancelOrderResponse;
import dev.miniExchange.infra.grpc.generated.SubmitOrderResponse;
import dev.miniExchange.infra.grpc.generated.AddInstrumentResponse;
import dev.miniExchange.order.entity.Order;
import dev.miniExchange.asset.entity.Asset;


public interface MatchingEngineClient {

    SubmitOrderResponse submitOrder(Order order);
    CancelOrderResponse cancelOrder(Order order);
    AddInstrumentResponse addInstrument(Asset asset);
}
