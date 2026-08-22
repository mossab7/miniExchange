package dev.miniExchange.market;

import dev.miniExchange.asset.entity.Asset;


public class Market {
    private final Asset baseAsset;
    private final Asset quoteAsset;
    public Market(Asset baseAsset, Asset quoteAsset) {
        this.baseAsset = baseAsset;
        this.quoteAsset = quoteAsset;
    }
    public Asset getBaseAsset() {
        return baseAsset;
    }
    public Asset getQuoteAsset() {
        return quoteAsset;
    }
    public int getBaseAssetDecimalPlaces() {
        return baseAsset.getDecimalPlaces();
    }
    public int getQuoteAssetDecimalPlaces() {
        return quoteAsset.getDecimalPlaces();
    }
}
