package dev.miniExchange.asset.dto;

import java.util.UUID;

public record AssetResponse(
    String symbol,
    String name,
    int decimalPlaces,
    boolean isActive,
    UUID assetUuid
) {
}
