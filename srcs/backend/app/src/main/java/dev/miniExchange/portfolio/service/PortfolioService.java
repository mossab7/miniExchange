package dev.miniExchange.portfolio.service;

import org.springframework.stereotype.Service;
import dev.miniExchange.portfolio.repository.PortfolioRepository;
import dev.miniExchange.portfolio.entity.Portfolio;

import java.math.BigInteger;

@Service
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;

    public PortfolioService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    public void updateBalance(Long userId, BigInteger balance) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found for userId: " + userId));
        portfolio.updateBalance(balance);
    }
    public Portfolio getPortfolioByUserId(Long userId) {
        return portfolioRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Portfolio not found for userId: " + userId));
    }
}
