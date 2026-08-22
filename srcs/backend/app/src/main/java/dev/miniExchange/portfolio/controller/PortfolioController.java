package dev.miniExchange.portfolio.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;

import dev.miniExchange.portfolio.service.PortfolioService;
import dev.miniExchange.portfolio.mapper.PortfolioMapper;
import dev.miniExchange.portfolio.dto.PortfolioResponse;
import dev.miniExchange.portfolio.dto.CreatePortfolioRequest;
import dev.miniExchange.portfolio.dto.UpdateBalanceRequest;
import dev.miniExchange.portfolio.entity.Portfolio;

import java.util.UUID;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {
    private final PortfolioService portfolioService;
    private final PortfolioMapper portfolioMapper;

    public PortfolioController(PortfolioService portfolioService, PortfolioMapper portfolioMapper) {
        this.portfolioService = portfolioService;
        this.portfolioMapper = portfolioMapper;
    }

    @GetMapping("/me")
    public ResponseEntity<PortfolioResponse> getMyPortfolio() {
        Portfolio portfolio = this.portfolioService.getPortfolioByUserId();
        return ResponseEntity.ok(this.portfolioMapper.toResponse(portfolio));
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> getPortfolio(@PathVariable UUID portfolioId) {
        Portfolio portfolio = this.portfolioService.getPortfolioByUuid(portfolioId);
        return ResponseEntity.ok(this.portfolioMapper.toResponse(portfolio));
    }

    @PostMapping("/{portfolioId}/balance")
    public ResponseEntity<Void> updateBalance(@PathVariable UUID portfolioId, @RequestBody @Valid UpdateBalanceRequest request) {
        portfolioService.updateBalance(portfolioId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(@RequestBody @Valid CreatePortfolioRequest request) {
        Portfolio portfolio = portfolioService.createPortfolio(request);
        return ResponseEntity.ok(this.portfolioMapper.toResponse(portfolio));
    }
}
