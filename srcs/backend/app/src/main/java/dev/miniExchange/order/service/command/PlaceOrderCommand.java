package dev.miniExchange.order.service.command;

import dev.miniExchange.order.entity.Side;
import dev.miniExchange.market.Market;
import java.math.BigInteger;
import java.util.UUID;

public record PlaceOrderCommand(
    UUID portfolioId,
    Market market,
    Side side,
    BigInteger price,
    BigInteger quantity
) {
    
}
