package dev.miniExchange.trade.command;

import java.math.BigInteger;
import java.util.UUID;

public record ProcessTradeCommand(
    BigInteger amount,
    BigInteger price,
    String assetSymbol,
    Long sellerPortfolioId,
    Long buyerPortfolioId,
    UUID buyerOrderId,
    UUID sellerOrderId
) {
    
}
