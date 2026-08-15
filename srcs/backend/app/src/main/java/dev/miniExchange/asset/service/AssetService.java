package dev.miniExchange.asset.service;

import org.springframework.stereotype.Service;
import dev.miniExchange.asset.dto.CreateAssetRequest;
import dev.miniExchange.asset.dto.UpdateAssetRequest;
import dev.miniExchange.asset.entity.Asset;
import dev.miniExchange.asset.exceptions.AssetAlreadyExistsException;
import dev.miniExchange.asset.exceptions.AssetNotFoundException;
import dev.miniExchange.asset.repository.AssetRepository;
import dev.miniExchange.asset.mapper.AssetMapper;
import java.util.List;
import java.util.UUID;

@Service
public class AssetService {

    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }
    public Asset create(CreateAssetRequest request) {
        if (assetRepository.existsBySymbol(request.symbol())) {
            throw new AssetAlreadyExistsException(request.symbol());
        }
        Asset asset = AssetMapper.toEntity(request);
        assetRepository.save(asset);
        return asset;
    }

    public Asset getBySymbol(String symbol) {
        Asset asset = assetRepository.findBySymbol(symbol)
                .orElseThrow(() -> new AssetNotFoundException(symbol));
        return asset;
    }

    public List<Asset> getAll() {
        return assetRepository.findAll();   
    }

    public Asset update(String symbol, UpdateAssetRequest request) {
        if (!assetRepository.existsBySymbol(symbol)) {
            throw new AssetNotFoundException(symbol);
        }
        Asset updatedAsset = AssetMapper.toEntity(request);
        assetRepository.save(updatedAsset);
        return updatedAsset;
    }
    public Asset getByUuid(UUID uuid) {
        Asset asset = assetRepository.findByUuid(uuid)
                .orElseThrow(() -> new AssetNotFoundException(uuid.toString()));
        return asset;
    }
}
