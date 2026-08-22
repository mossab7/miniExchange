package dev.miniExchange.market;

import org.springframework.stereotype.Service;
import dev.miniExchange.asset.repository.AssetRepository;
import dev.miniExchange.asset.entity.Asset;
import dev.miniExchange.asset.exceptions.AssetNotFoundException;
import dev.miniExchange.common.exceptions.ValidationException;

@Service
public class MarketService {

    private final AssetRepository assetRepository;

    public MarketService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public Market getMarket(String marketSymbol) {
        if (marketSymbol == null || !marketSymbol.contains("/")) {
            throw new ValidationException("Invalid market symbol format. Expected format: BASE/QUOTE");
        }
        String[] symbols = marketSymbol.split("/");
        if (symbols.length != 2 || symbols[0].isBlank() || symbols[1].isBlank()) {
            throw new ValidationException("Invalid market symbol format. Expected format: BASE/QUOTE");
        }
        String baseSymbol = symbols[0].trim();
        String quoteSymbol = symbols[1].trim();

        Asset baseAsset = assetRepository.findBySymbol(baseSymbol)
                .orElseThrow(() -> new AssetNotFoundException(baseSymbol));
        Asset quoteAsset = assetRepository.findBySymbol(quoteSymbol)
                .orElseThrow(() -> new AssetNotFoundException(quoteSymbol));

        return new Market(baseAsset, quoteAsset);
    }
}
