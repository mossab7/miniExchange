package dev.miniExchange.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
public record CreateAssetRequest(
    
    @NotBlank
    @Size(min = 1, max = 4)
    String symbol,
    @NotBlank
    String name,
    @Min(0)
    @Max(18)
    int decimalPlaces,
    boolean isActive
) {
}
