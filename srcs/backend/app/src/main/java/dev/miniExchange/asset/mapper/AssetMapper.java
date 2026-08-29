package dev.miniExchange.asset.mapper;

import org.springframework.lang.NonNull;

import dev.miniExchange.asset.dto.AssetResponse;
import dev.miniExchange.asset.entity.Asset;
import dev.miniExchange.asset.dto.CreateAssetRequest;
import dev.miniExchange.asset.dto.UpdateAssetRequest;


public class AssetMapper {

    static public AssetResponse toResponse(Asset asset) {
        return new AssetResponse(
                asset.getSymbol(),
                asset.getName(),
                asset.getDecimalPlaces(),
                asset.isActive(),
                asset.getUuid()
        );
    }

    static public  @NonNull Asset toEntity(CreateAssetRequest request) {
        return new Asset(
                request.symbol(),
                request.name(),
                request.decimalPlaces(),
                request.isActive()
        );
    }

    static public @NonNull Asset toEntity(UpdateAssetRequest request) {
        return new Asset(
                request.symbol(),
                request.name(),
                request.decimalPlaces(),
                request.isActive()
        );
    }

}
