package dev.miniExchange.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import dev.miniExchange.order.entity.Side;
import java.util.UUID;

public record CreateOrderRequest(
    @NotNull
    UUID portfolioId,
    @NotBlank
    String market,
    @NotBlank
    String price,
    @NotBlank
    String quantity,
    @NotNull
    Side side
) {
}
