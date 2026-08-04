package dev.miniExchange.position.mapper;

import org.springframework.stereotype.Component;
import dev.miniExchange.position.entity.Position;
import dev.miniExchange.position.dto.PositionResponse;

@Component
public class PositionMapper {

    public PositionResponse toResponse(Position position) {
        return new PositionResponse(position.getId(), position.getAsset().getSymbol(), position.getQuantity());
    }

    
}
