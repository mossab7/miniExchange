package dev.miniExchange.portfolio.service;

import dev.miniExchange.order.service.OrderService;
import dev.miniExchange.portfolio.position.service.PositionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;
import dev.miniExchange.portfolio.repository.PortfolioRepository;

import dev.miniExchange.portfolio.entity.Portfolio;
import dev.miniExchange.user.entity.User;
import dev.miniExchange.security.user.CurrentUser;
import dev.miniExchange.amountConvertor.AmountConverterService;
import dev.miniExchange.portfolio.dto.CreatePortfolioRequest;
import dev.miniExchange.portfolio.dto.UpdateBalanceRequest;
import dev.miniExchange.asset.service.AssetService;
import dev.miniExchange.asset.entity.Asset;
import dev.miniExchange.common.exceptions.ResourceNotFoundException;
import dev.miniExchange.trade.command.ProcessTradeCommand;
import java.math.BigInteger;
import java.util.UUID;

@Service
public class PortfolioService {
    private final OrderService orderService;
    private final PositionService positionService;
    private final PortfolioRepository portfolioRepository;
    private final CurrentUser currentUser;
    private final AmountConverterService amountConverterService;
    private final AssetService assetService;

    public PortfolioService(PortfolioRepository portfolioRepository,
            CurrentUser currentUser,
            AmountConverterService amountConverterService,
            AssetService assetService, PositionService positionService, OrderService orderService) {
        this.portfolioRepository = portfolioRepository;
        this.currentUser = currentUser;
        this.amountConverterService = amountConverterService;
        this.assetService = assetService;
        this.positionService = positionService;
        this.orderService = orderService;
    }

    @Transactional
    public void updateBalance(UUID portfolioId, UpdateBalanceRequest request) {
        Portfolio portfolio = portfolioRepository.findByUuid(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId));

        // Ownership check
        if (!portfolio.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Portfolio", portfolioId);
        }

        Asset asset = assetService.getBySymbol(request.currency());
        BigInteger amountInSmallestUnit = amountConverterService.toSmallestUnit(request.amount(),
                asset.getDecimalPlaces());
        portfolio.deposit(amountInSmallestUnit);
        portfolioRepository.save(portfolio);
    }

    public Portfolio getPortfolioByUserIdAndName(String portfolioName) {
        Long userId = currentUser.getId();
        return portfolioRepository.findByUser_IdAndName(userId, portfolioName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Portfolio for userId:" + userId + "and name:" + portfolioName));
    }

    public Portfolio getPortfolioByUuid(UUID portfolioUuid) {
        Portfolio portfolio = portfolioRepository.findByUuid(portfolioUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioUuid));
        if (!portfolio.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Portfolio", portfolioUuid);
        }
        return portfolio;
    }

    public BigInteger getAvailableBalanceByPortfolioId(UUID portfolioId) {
        Portfolio portfolio = portfolioRepository.findByUuid(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId));
        return portfolio.getAvailableBalance();
    }

    @Transactional
    public Portfolio createDefaultPortfolio(User user) {
        Portfolio portfolio = new Portfolio(user);
        portfolio.setName("default portfolio");
        portfolio.setDescription("default portfolio");
        return portfolioRepository.save(portfolio);
    }

    @Transactional
    public Portfolio createPortfolio(CreatePortfolioRequest request) {
        Portfolio portfolio = new Portfolio(currentUser.get().getUser(), request.name(), request.description());
        return portfolioRepository.save(portfolio);
    }

    public Portfolio getReference(@NonNull Long portfolioId) {
        return portfolioRepository.getReferenceById(portfolioId);
    }

    @Transactional
    public void processTrade(ProcessTradeCommand command) {
        Portfolio sellerPortfolio = portfolioRepository.findById(command.sellerPortfolioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", command.sellerPortfolioId()));
        Portfolio buyerPortfolio = portfolioRepository.findById(command.buyerPortfolioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", command.buyerPortfolioId()));

        // Compute the actual trade value: price * quantity / 10^baseDecimalPlaces
        // This mirrors the total-cost formula used at order creation time.
        int baseDecimals = assetService.getBySymbol(command.assetSymbol()).getDecimalPlaces();
        BigInteger tradeValue = command.price()
                .multiply(command.amount())
                .divide(BigInteger.TEN.pow(baseDecimals));

        buyerPortfolio.deductLockedBalance(tradeValue);
        sellerPortfolio.deposit(tradeValue);
        positionService.applyTrade(command);
        orderService.applyTrade(command);
    }

    public Portfolio getReferenceByOrderId(Long id) {
        return getReference(orderService.getOrderPortfolioId(id));
    }
}
