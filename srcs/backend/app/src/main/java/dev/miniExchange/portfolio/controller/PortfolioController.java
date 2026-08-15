package dev.miniExchange.portfolio.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import dev.miniExchange.portfolio.service.PortfolioService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import dev.miniExchange.portfolio.mapper.PortfolioMapper;
import dev.miniExchange.portfolio.dto.PortfolioResponse;

import dev.miniExchange.portfolio.dto.UpdateBalanceRequest;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import dev.miniExchange.portfolio.entity.Portfolio;
import java.util.UUID;

@RestController
@RequestMapping("/portfolios")
public class PortfolioController {
    private final PortfolioService portfolioService;
    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }
    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> getPortfolio(@PathVariable UUID portfolioId) {
        Portfolio portfolio = this.portfolioService.getPortfolioByUserId();
        return ResponseEntity.ok(PortfolioMapper.toResponse(portfolio));
    }

    @GetMapping("")
    // this not a good practice to update balance directly, but for a learning  project let's do it for now, in a real production project we should have a transaction service to handle this
    @PostMapping("/{portfolioId}/balance")
    public ResponseEntity<Void> updateBalance(@PathVariable UUID portfolioId, @RequestBody @Valid UpdateBalanceRequest request) {
        portfolioService.updateBalance(portfolioId, request);
        return ResponseEntity.ok().build();
    }
}
