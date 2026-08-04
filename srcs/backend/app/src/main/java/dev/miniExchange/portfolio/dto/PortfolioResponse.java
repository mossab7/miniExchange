package dev.miniExchange.portfolio.dto;

import java.math.BigInteger;
import java.util.UUID;

public record PortfolioResponse(
    UUID userId,
    UUID portfolioId,
    BigInteger balance
) {
}