package dev.miniExchange.portfolio.position.mapper;

import dev.miniExchange.portfolio.position.dto.PositionResponse;
import dev.miniExchange.portfolio.position.entity.Position;

public class PositionMapper {

    public static PositionResponse toResponse(Position position) {
        if (position == null) {
            return null;
        }
        return new PositionResponse(
                position.getId(),
                position.getUuid(),
                position.getAssetSymbol(),
                position.getQuantity()
        );
    }
}
