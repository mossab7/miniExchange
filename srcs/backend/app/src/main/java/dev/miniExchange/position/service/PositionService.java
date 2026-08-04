package dev.miniExchange.position.service;

import org.springframework.stereotype.Service;
import dev.miniExchange.position.repository.PositionRepository;
import dev.miniExchange.position.entity.Position;
import dev.miniExchange.position.dto.PositionResponse;
import dev.miniExchange.exception.notfound.positionNotFoundException;
import dev.miniExchange.security.user.CurrentUser;
import dev.miniExchange.position.mapper.PositionMapper;


import java.util.List;
import java.util.stream.Collectors;

@Service
public class PositionService {
    private final PositionRepository positionRepository;
    private final CurrentUser currentUser;
    private final PositionMapper positionMapper;

    public PositionService(PositionRepository positionRepository, CurrentUser currentUser, PositionMapper positionMapper) {
        this.positionRepository = positionRepository;
        this.currentUser = currentUser;
        this.positionMapper = positionMapper;
    }

    public PositionResponse getPosition(String symbol) {
        
        Position position = positionRepository.findByPortfolioUserIdAndAssetSymbol(currentUser.getId(), symbol)
                .orElseThrow(() -> new positionNotFoundException(symbol));
        return positionMapper.toResponse(position);
 
    }
    
    public List<PositionResponse> getAllPositions() {
        Long userId = currentUser.getId();
        return positionRepository.findByPortfolioUserId(userId).stream()
                .map(positionMapper::toResponse)
                .collect(Collectors.toList());
    }

}
