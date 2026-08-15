package dev.miniExchange.order.mapper;

import dev.miniExchange.order.dto.OrderResponse;
import dev.miniExchange.order.entity.Order;

public class OrderMapper {

    public static OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }
        return new OrderResponse(
                order.getId(),
                order.getAsset() != null ? order.getAsset().getSymbol() : "UNKNOWN",
                order.getQuoteAsset() != null ? order.getQuoteAsset().getSymbol() : "UNKNOWN",
                order.getPrice().toString(),
                order.getQuantity().toString(),
                order.getSide().name(),
                order.getStatus().name(),
                order.getCreatedAt()
        );
    }
    
}
