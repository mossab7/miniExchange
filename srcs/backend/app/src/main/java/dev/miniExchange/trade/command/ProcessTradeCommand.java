package dev.miniExchange.trade.command;

import java.math.BigInteger;

public record ProcessTradeCommand(
    BigInteger amount,
    BigInteger price,
    String assetSymbol,
    Long sellerPortfolioId,
    Long buyerPortfolioId,
    Long buyerOrderId,
    Long sellerOrderId
) {
    
}
