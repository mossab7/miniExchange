package dev.miniExchange.asset.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Asset create(CreateAssetRequest request) {
        if (assetRepository.existsBySymbol(request.symbol())) {
            throw new AssetAlreadyExistsException(request.symbol());
        }
        Asset asset = AssetMapper.toEntity(request);
        return assetRepository.save(asset);
    }

    public Asset getBySymbol(String symbol) {
        return assetRepository.findBySymbol(symbol)
                .orElseThrow(() -> new AssetNotFoundException(symbol));
    }

    public List<Asset> getAll() {
        return assetRepository.findAll();
    }

    @Transactional
    public Asset update(String symbol, UpdateAssetRequest request) {
        Asset asset = assetRepository.findBySymbol(symbol)
                .orElseThrow(() -> new AssetNotFoundException(symbol));

        asset.setName(request.name());
        asset.setDecimalPlaces(request.decimalPlaces());
        asset.setActive(request.isActive());

        return assetRepository.save(asset);
    }

    public Asset getByUuid(UUID uuid) {
        return assetRepository.findByUuid(uuid)
                .orElseThrow(() -> new AssetNotFoundException(uuid.toString()));
    }

    public Asset getReference(Long assetId) {
        return assetRepository.getReferenceById(assetId);
    }
}
