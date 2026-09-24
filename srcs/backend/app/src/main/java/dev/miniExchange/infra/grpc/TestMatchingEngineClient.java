package dev.miniExchange.infra.grpc;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import dev.miniExchange.asset.entity.Asset;
import dev.miniExchange.infra.grpc.generated.AddInstrumentResponse;
import dev.miniExchange.infra.grpc.generated.CancelOrderResponse;
import dev.miniExchange.infra.grpc.generated.SubmitOrderResponse;
import dev.miniExchange.order.entity.Order;

/** Keeps persistence-focused tests independent from a running engine process. */
@Component
@Profile("test")
public class TestMatchingEngineClient implements MatchingEngineClient {
    @Override
    public SubmitOrderResponse submitOrder(Order order) {
        return SubmitOrderResponse.newBuilder().setOrderId(order.getId() == null ? 0 : order.getId()).setSuccess(true).build();
    }

    @Override
    public CancelOrderResponse cancelOrder(Order order) {
        return CancelOrderResponse.newBuilder().setOrderId(order.getId() == null ? 0 : order.getId()).setSuccess(true).build();
    }

    @Override
    public AddInstrumentResponse addInstrument(Asset asset) {
        return AddInstrumentResponse.newBuilder().setInstrumentId(asset.getId() == null ? 0 : asset.getId()).setSuccess(true).build();
    }
}
