package dev.miniExchange.position.mapper;

import dev.miniExchange.position.entity.Position;
import dev.miniExchange.position.dto.PositionResponse;

public class PositionMapper {

    static public PositionResponse toResponse(Position position) {
        return new PositionResponse(position.getId(), position.getAsset().getSymbol(), position.getQuantity());
    }

    
}
