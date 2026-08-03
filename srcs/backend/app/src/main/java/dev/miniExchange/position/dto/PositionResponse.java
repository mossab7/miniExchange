package dev.miniExchange.position.dto;

import java.math.BigInteger;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PositionResponse(
    @NotBlank
    Long id,
    @NotBlank
    @Size(min = 1, max = 4)
    String symbol,
    @NotBlank
    BigInteger quantity) {
    
}
