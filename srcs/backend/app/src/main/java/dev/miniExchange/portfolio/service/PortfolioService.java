package dev.miniExchange.portfolio.service;

import org.springframework.stereotype.Service;
import dev.miniExchange.portfolio.repository.PortfolioRepository;
import dev.miniExchange.portfolio.entity.Portfolio;


import dev.miniExchange.position.service.PositionService;
import dev.miniExchange.security.user.CurrentUser;

import java.math.BigInteger;

import dev.miniExchange.position.entity.Position;

import dev.miniExchange.amountConvertor.AmountConverterService;

import dev.miniExchange.portfolio.dto.UpdateBalanceRequest;
import dev.miniExchange.asset.service.AssetService;
import dev.miniExchange.asset.entity.Asset;
import java.util.UUID;

@Service
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final PositionService positionService;
    private final CurrentUser currentUser;
    private final AmountConverterService amountConverterService;
    private final AssetService assetService;
    public PortfolioService(PortfolioRepository portfolioRepository, PositionService positionService,
                             CurrentUser currentUser, AmountConverterService amountConverterService, AssetService assetService) {
        this.portfolioRepository = portfolioRepository;
        this.positionService = positionService;
        this.currentUser = currentUser;
        this.amountConverterService = amountConverterService;
        this.assetService = assetService;
    }
    
    public void updateBalance(UUID portfolioId, UpdateBalanceRequest request) {
        Portfolio portfolio = portfolioRepository.findByUuid(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found for ID: " + portfolioId));
        Asset asset = assetService.getBySymbol(request.currency());
        if (asset == null) {
            throw new RuntimeException("Asset not found for symbol: " + request.currency());
        }

        BigInteger newBalance = amountConverterService.toSmallestUnit(request.amount(), asset.getDecimalPlaces());
        portfolio.updateBalance(newBalance);
        portfolioRepository.save(portfolio);
    }
    public Portfolio getPortfolioByUserId() {
        Long userId = currentUser.get().getId();
        Portfolio portfolio =  portfolioRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Portfolio not found for userId: " + userId));
        return portfolio;
    }

    public BigInteger getBalanceByPortfolioId(UUID portfolioId) {
        Portfolio portfolio = portfolioRepository.findByUuid(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found for ID: " + portfolioId));
        return portfolio.getBalance();
    }

    public Position getPositionByPortfolioIdAndAssetSymbol(Long portfolioId, String assetSymbol) {
        return positionService.getPositionByPortfolioIdAndAssetSymbol(portfolioId, assetSymbol);
    }
}
