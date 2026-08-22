package dev.miniExchange.order.dto;

import java.time.Instant;


public record OrderResponse(
    Long id,
    String symbol,
    String quoteSymbol,
    String price,
    String quantity,
    String side,
    String status,
    Instant createdAt
) {
}