package dev.miniExchange.trade.event;

import java.util.UUID;
import java.math.BigInteger;
import java.time.Instant;
public record TradeEvent(
    UUID tradeUuid,
    long engineNamespace,
    long executionId,
    UUID sellerOrderId,
    UUID buyerOrderId,
    String assetSymbol,
    BigInteger quantity,
    BigInteger price,
    Instant time
) {
    public TradeEvent(UUID tradeUuid, UUID sellerOrderId, UUID buyerOrderId, String assetSymbol,
                      BigInteger quantity, BigInteger price, Instant time) {
        this(tradeUuid, 0, 0, sellerOrderId, buyerOrderId, assetSymbol, quantity, price, time);
    }
}
