package dev.miniExchange.market;

import org.springframework.stereotype.Service;
import dev.miniExchange.asset.repository.AssetRepository;
import dev.miniExchange.asset.entity.Asset;

@Service
public class MarketService {

    private final AssetRepository assetRepository;

    public MarketService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public Market getMarket(String marketSymbol) {
        String[] symbols = marketSymbol.split("/");
        if (symbols.length != 2) {
            throw new IllegalArgumentException("Invalid market symbol format. Expected format: BASE/QUOTE");
        }
        String baseSymbol = symbols[0];
        String quoteSymbol = symbols[1];

        Asset baseAsset = assetRepository.findBySymbol(baseSymbol)
                .orElseThrow(() -> new RuntimeException("Base asset not found"));
        Asset quoteAsset = assetRepository.findBySymbol(quoteSymbol)
                .orElseThrow(() -> new RuntimeException("Quote asset not found"));

        return new Market(baseAsset, quoteAsset);
    }
    
}
