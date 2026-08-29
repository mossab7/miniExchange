package dev.miniExchange.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.miniExchange.trade.entity.Trade;

import java.util.Optional;
import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    Optional<Trade> findByUuid(UUID uuid);
}
