package dev.miniExchange.portfolio.position.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.miniExchange.portfolio.position.entity.Position;
import dev.miniExchange.portfolio.position.exceptions.PositionAlreadyExistException;
import dev.miniExchange.portfolio.position.exceptions.PositionNotFoundException;
import dev.miniExchange.portfolio.position.repository.PositionRepository;
import dev.miniExchange.security.user.CurrentUser;
import dev.miniExchange.portfolio.position.dto.CreatePositionCommand;
import dev.miniExchange.portfolio.repository.PortfolioRepository;
import dev.miniExchange.portfolio.entity.Portfolio;
import dev.miniExchange.asset.service.AssetService;
import dev.miniExchange.asset.entity.Asset;

import java.util.List;
import java.math.BigInteger;
import java.util.UUID;

@Service
public class PositionService {
    private final PositionRepository positionRepository;
    private final CurrentUser currentUser;
    private final PortfolioRepository portfolioRepository;
    private final AssetService assetService;

    public PositionService(PositionRepository positionRepository, CurrentUser currentUser,
            PortfolioRepository portfolioRepository, AssetService assetService) {
        this.positionRepository = positionRepository;
        this.currentUser = currentUser;
        this.portfolioRepository = portfolioRepository;
        this.assetService = assetService;
    }

    public Position getPosition(String symbol) {
        return positionRepository.findByPortfolio_User_IdAndAsset_Symbol(currentUser.getId(), symbol)
                .orElseThrow(() -> new PositionNotFoundException(symbol));
    }

    public List<Position> getAllPositions() {
        Long userId = currentUser.getId();
        return positionRepository.findAllByPortfolio_User_Id(userId);
    }

    public List<Position> getAllPositions(Long portfolioId) {
        return positionRepository.findAllByPortfolio_Id(portfolioId);
    }

    public Position getPosition(Long portfolioId, String assetSymbol) {
        return positionRepository.findByPortfolio_IdAndAsset_Symbol(portfolioId, assetSymbol)
                .orElseThrow(() -> new PositionNotFoundException(assetSymbol));
    }

    public BigInteger getQuantity(UUID portfolioUuid, String assetSymbol) {
        Position position = positionRepository.findByPortfolio_UuidAndAsset_Symbol(portfolioUuid, assetSymbol)
                .orElseThrow(() -> new PositionNotFoundException(assetSymbol));
        return position.getQuantity();
    }

    @Transactional
    public Position createPosition(CreatePositionCommand command) {
        if (positionRepository.existsByAsset_IdAndPortfolio_Id(command.assetId(), command.portfolioId())) {
            throw new PositionAlreadyExistException();
        }
        Asset asset = assetService.getReference(command.assetId());
        Portfolio portfolio = portfolioRepository.getReferenceById(command.portfolioId());
        Position position = new Position(asset, portfolio, command.quantity());
        return positionRepository.save(position);
    }
}
