package dev.miniExchange.trade.event;

import java.util.UUID;
import java.math.BigInteger;
import java.time.Instant;
public record TradeEvent(
    UUID tradeUuid,
    Long sellerOrderId,
    Long buyerOderId,
    String assetSymbol,
    BigInteger quantity,
    BigInteger price,
    Instant time
) {
    
}
