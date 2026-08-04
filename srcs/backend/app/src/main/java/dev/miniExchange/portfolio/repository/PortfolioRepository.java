package dev.miniExchange.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.miniExchange.portfolio.entity.Portfolio;
import java.util.Optional;
import java.math.BigInteger;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    Optional<Portfolio> findByUserId(Long userId);
}
