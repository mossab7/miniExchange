package dev.miniExchange.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PortfolioResponse(
    UUID portfolioId,
    UUID userId,
    String name,
    String description,
    BigDecimal availableBalance,
    BigDecimal lockedBalance,
    BigDecimal totalBalance,
    Instant createdAt,
    Instant updatedAt
) {
}