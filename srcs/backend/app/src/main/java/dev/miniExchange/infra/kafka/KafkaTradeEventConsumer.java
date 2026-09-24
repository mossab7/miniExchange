package dev.miniExchange.infra.kafka;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import dev.miniExchange.asset.service.AssetService;
import dev.miniExchange.infra.grpc.generated.TradeEvent;
import dev.miniExchange.order.entity.Order;
import dev.miniExchange.order.service.OrderService;
import dev.miniExchange.trade.service.TradeService;

@Component
@Profile("!test")
public class KafkaTradeEventConsumer {
    private final AssetService assetService;
    private final OrderService orderService;
    private final TradeService tradeService;

    public KafkaTradeEventConsumer(AssetService assetService,
                                   OrderService orderService,
                                   TradeService tradeService) {
        this.assetService = assetService;
        this.orderService = orderService;
        this.tradeService = tradeService;
    }

    @KafkaListener(topics = "${trade-events.topic:trade-events}",
                   groupId = "${trade-events.consumer-group:mini-exchange-trades}",
                   containerFactory = "tradeEventKafkaListenerContainerFactory")
    public void consume(byte[] payload) throws Exception {
        TradeEvent event = TradeEvent.parseFrom(payload);
        Order sellOrder = orderService.getByIdSystem(toId(event.getSellOrderId()));
        Order buyOrder = orderService.getByIdSystem(toId(event.getBuyOrderId()));

        dev.miniExchange.trade.event.TradeEvent domainEvent = new dev.miniExchange.trade.event.TradeEvent(
                UUID.fromString(event.getTradeId()),
                sellOrder.getUuid(),
                buyOrder.getUuid(),
                assetService.getByIdSystem(toId(event.getInstrumentId())).getSymbol(),
                BigInteger.valueOf(event.getQuantity()),
                BigInteger.valueOf(event.getPrice()),
                Instant.ofEpochMilli(event.getTimestampEpochMillis()));
        tradeService.executeTrade(domainEvent);
    }

    private static long toId(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Kafka trade event contains an invalid identifier");
        }
        return value;
    }
}
