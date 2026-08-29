package dev.miniExchange.portfolio.position.dto;

import java.math.BigInteger;
import org.springframework.lang.NonNull;

public record CreatePositionCommand(
    @NonNull Long portfolioId,
    @NonNull Long assetId,
    @NonNull BigInteger quantity
) {
}
