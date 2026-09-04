package dev.miniExchange.trade.event;

import java.util.UUID;
import java.math.BigInteger;
import java.time.Instant;
public record TradeEvent(
    UUID tradeUuid,
    UUID sellerOrderId,
    UUID buyerOrderId,
    String assetSymbol,
    BigInteger quantity,
    BigInteger price,
    Instant time
) {
    
}
