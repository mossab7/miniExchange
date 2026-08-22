package dev.miniExchange.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.miniExchange.portfolio.entity.Portfolio;


import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    Optional<Portfolio> findByUserId(Long userId);
    Optional<Portfolio> findByUuid(UUID uuid);
}
