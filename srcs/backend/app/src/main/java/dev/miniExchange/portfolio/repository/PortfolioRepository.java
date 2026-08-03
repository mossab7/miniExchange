package dev.miniExchange.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.miniExchange.portfolio.entity.Portfolio;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
}
