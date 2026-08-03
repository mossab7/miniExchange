package dev.miniExchange.position.service;

import org.springframework.stereotype.Service;
import dev.miniExchange.position.repository.PositionRepository;
import dev.miniExchange.position.entity.Position;
import dev.miniExchange.position.dto.PositionResponse;
import dev.miniExchange.exception.notfound.positionNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PositionService {
    private final PositionRepository positionRepository;

    public PositionService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    public PositionResponse getPosition(Long assetId, Long portfolioId) {
        Position position = positionRepository.findByAssetIdAndPortfolioId(assetId, portfolioId)
                .orElseThrow(() -> new positionNotFoundException(assetId, portfolioId));
        return new PositionResponse(position.getId(), position.getAssetSymbol(), position.getQuantity());
    }
    
    public List<PositionResponse> getAllPositions() {
        return positionRepository.findAll().stream()
                .map(position -> new PositionResponse(position.getId(), position.getAssetSymbol(), position.getQuantity()))
                .collect(Collectors.toList());
    }
}
