package dev.miniExchange.asset.service;

import org.springframework.stereotype.Service;
import dev.miniExchange.asset.dto.CreateAssetRequest;
import dev.miniExchange.asset.dto.UpdateAssetRequest;
import dev.miniExchange.asset.dto.AssetResponse;
import dev.miniExchange.asset.entity.Asset;
import dev.miniExchange.asset.repository.AssetRepository;
import dev.miniExchange.asset.mapper.AssetMapper;

import dev.miniExchange.exception.notfound.assetNotFoundException;
import dev.miniExchange.exception.conflict.assetAlreadyExistsException;


import java.util.List;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetMapper assetMapper;

    public AssetService(AssetRepository assetRepository, AssetMapper assetMapper) {
        this.assetRepository = assetRepository;
        this.assetMapper = assetMapper;
    }
    public AssetResponse create(CreateAssetRequest request) {
        if (assetRepository.existsBySymbol(request.symbol())) {
            throw new assetAlreadyExistsException(request.symbol());
        }
        Asset asset = assetMapper.toEntity(request);
        assetRepository.save(asset);
        return assetMapper.toResponse(asset);
    }

    public AssetResponse getBySymbol(String symbol) {
        Asset asset = assetRepository.findBySymbol(symbol)
                .orElseThrow(() -> new assetNotFoundException(symbol));
        return assetMapper.toResponse(asset);
    }

    public List<AssetResponse> getAll() {
        List<Asset> assets = assetRepository.findAll();
        return assets.stream().map(assetMapper::toResponse).toList();
    }

    public AssetResponse update(String symbol, UpdateAssetRequest request) {
        if (!assetRepository.existsBySymbol(symbol)) {
            throw new assetNotFoundException(symbol);
        }
        Asset updatedAsset = assetMapper.toEntity(request);
        assetRepository.save(updatedAsset);
        return assetMapper.toResponse(updatedAsset);
    }
}
