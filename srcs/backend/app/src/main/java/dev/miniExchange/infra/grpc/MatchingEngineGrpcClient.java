package dev.miniExchange.infra.grpc;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import dev.miniExchange.infra.grpc.generated.AddInstrumentResponse;
import dev.miniExchange.infra.grpc.generated.CancelOrderResponse;
import dev.miniExchange.infra.grpc.generated.SubmitOrderResponse;
import dev.miniExchange.infra.grpc.generated.MiniExchangeServiceGrpc;
import dev.miniExchange.order.entity.Order;
import dev.miniExchange.asset.entity.Asset;
import dev.miniExchange.infra.grpc.mapper.MatchingEngineMapper;


@Component
@Profile("!test")
public class MatchingEngineGrpcClient implements MatchingEngineClient {

   private final MiniExchangeServiceGrpc.MiniExchangeServiceBlockingStub stub;

    public MatchingEngineGrpcClient(MiniExchangeServiceGrpc.MiniExchangeServiceBlockingStub stub) {
        this.stub = stub;
    }

    @Override
    public SubmitOrderResponse submitOrder(Order order) {
        return stub.submitOrder(MatchingEngineMapper.toSubmitOrderRequest(order));
    }

    @Override
    public CancelOrderResponse cancelOrder(Order order) {
        return stub.cancelOrder(MatchingEngineMapper.toCancelOrderRequest(order));
    }

    @Override
    public AddInstrumentResponse addInstrument(Asset asset) {
        return stub.addInstrument(MatchingEngineMapper.toAddInstrumentRequest(asset));
    }

}
