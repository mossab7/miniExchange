package dev.miniExchange.portfolio.position.dto;

import java.math.BigInteger;

public record CreatePositionCommand(
    Long portfolioId,
    Long assetId,
    BigInteger quantity
) {
}
