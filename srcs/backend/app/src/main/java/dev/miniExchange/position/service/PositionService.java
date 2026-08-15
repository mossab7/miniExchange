package dev.miniExchange.position.service;

import org.springframework.stereotype.Service;
import dev.miniExchange.position.repository.PositionRepository;
import dev.miniExchange.position.entity.Position;
import dev.miniExchange.position.exceptions.PositionNotFoundException;
import dev.miniExchange.security.user.CurrentUser;


import java.util.List;
import java.util.stream.Collectors;
import java.math.BigInteger;
import java.util.UUID;

@Service
public class PositionService {
    private final PositionRepository positionRepository;
    private final CurrentUser currentUser;

    public PositionService(PositionRepository positionRepository, CurrentUser currentUser) {
        this.positionRepository = positionRepository;
        this.currentUser = currentUser;
    }

    public Position getPosition(String symbol) {
        
        Position position = positionRepository.findByPortfolioUserIdAndAssetSymbol(currentUser.getId(), symbol)
                .orElseThrow(() -> new PositionNotFoundException(symbol));
        return position;
 
    }
    
    public List<Position> getAllPositions() {
        Long userId = currentUser.getId();
        return positionRepository.findByPortfolioUserId(userId).stream()
                .collect(Collectors.toList());
    }

    public Position getPositionByPortfolioIdAndAssetSymbol(Long portfolioId, String assetSymbol) {
        return positionRepository.findByPortfolioIdAndAssetSymbol(portfolioId, assetSymbol)
                .orElseThrow(() -> new PositionNotFoundException(assetSymbol));
    }
    public BigInteger getQuantityByPortfolioIdAndAssetSymbol(UUID portfolioId, String assetSymbol) {
        Position position = positionRepository.findByPortfolioIdAndAssetSymbol(portfolioId, assetSymbol)
                .orElseThrow(() -> new PositionNotFoundException(assetSymbol));
        return position.getQuantity();
    }
}
