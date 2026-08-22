package dev.miniExchange.portfolio.position.dto;

import java.math.BigInteger;
import java.util.UUID;

public record PositionResponse(
    Long id,
    UUID uuid,
    String symbol,
    BigInteger quantity
) {
}
