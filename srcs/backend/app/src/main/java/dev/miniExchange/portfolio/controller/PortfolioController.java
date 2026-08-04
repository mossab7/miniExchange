package dev.miniExchange.portfolio.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import dev.miniExchange.portfolio.service.PortfolioService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import dev.miniExchange.security.user.CurrentUser;
import dev.miniExchange.portfolio.entity.Portfolio;
import dev.miniExchange.portfolio.dto.PortfolioResponse;
import dev.miniExchange.portfolio.Mapper.PortfolioMapper;

@RestController
@RequestMapping("/portfolios")
public class PortfolioController {
    private final PortfolioService portfolioService;
    private final CurrentUser currentUser;
    private final PortfolioMapper portfolioMapper;
    public PortfolioController(PortfolioService portfolioService, CurrentUser currentUser, PortfolioMapper portfolioMapper) {
        this.portfolioService = portfolioService;
        this.currentUser = currentUser;
        this.portfolioMapper = portfolioMapper;
    }
    @GetMapping
    public ResponseEntity<PortfolioResponse> getPortfolio() {
        Portfolio portfolio = this.portfolioService.getPortfolioByUserId(currentUser.getId());
        return ResponseEntity.ok(portfolioMapper.toResponse(portfolio));
    }
    // this not a good practice to update balance directly, but for a learning  project let's do it for now, in a real production project we should have a transaction service to handle this
    @PostMapping("/balance/{balance}")
    public ResponseEntity<Void> updateBalance(@PathVariable java.math.BigInteger balance) {
        portfolioService.updateBalance(currentUser.getId(), balance);
        return ResponseEntity.ok().build();
    }
}
