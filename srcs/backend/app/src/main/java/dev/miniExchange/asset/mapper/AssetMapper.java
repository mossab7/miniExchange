package dev.miniExchange.asset.mapper;

import dev.miniExchange.asset.dto.AssetResponse;
import dev.miniExchange.asset.entity.Asset;
import dev.miniExchange.asset.dto.CreateAssetRequest;
import dev.miniExchange.asset.dto.UpdateAssetRequest;

import org.springframework.stereotype.Component;

@Component
public class AssetMapper {

    public AssetResponse toResponse(Asset asset) {
        return new AssetResponse(
                asset.getSymbol(),
                asset.getName(),
                asset.getDecimalPlaces(),
                asset.isActive()
        );
    }

    public Asset toEntity(CreateAssetRequest request) {
        return new Asset(
                request.symbol(),
                request.name(),
                request.decimalPlaces(),
                request.isActive()
        );
    }

    public Asset toEntity(UpdateAssetRequest request) {
        return new Asset(
                request.symbol(),
                request.name(),
                request.decimalPlaces(),
                request.isActive()
        );
    }

}
