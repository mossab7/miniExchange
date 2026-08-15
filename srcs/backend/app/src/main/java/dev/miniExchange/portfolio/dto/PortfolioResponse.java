package dev.miniExchange.portfolio.dto;

import java.math.BigInteger;
import java.util.UUID;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

public record PortfolioResponse(
    @NotBlank
    UUID portfolioId,
    @NotBlank
    UUID userId,
    @NotBlank
    BigInteger balance,
    @NotBlank
    Instant createdAt,
    @NotBlank
    Instant updatedAt
) {
}