package dev.miniExchange.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.miniExchange.portfolio.entity.Portfolio;


import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    List<Portfolio> findAllByUserId(Long userId);
    Optional<Portfolio> findByUser_IdAndName(Long userId, String portfolioName);
    Optional<Portfolio> findByUuid(UUID uuid);
    @Query("""
            SELECT o.portfolio
            FROM ExchangeOrder o
            WHERE o.uuid = :orderUuid
            """)
    Optional<Portfolio> findByOrderUuid(@Param("orderUuid") UUID orderUuid);
}
