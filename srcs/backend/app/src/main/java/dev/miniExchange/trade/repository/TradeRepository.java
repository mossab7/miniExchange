package dev.miniExchange.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.miniExchange.trade.entity.Trade;

public interface TradeRepository extends JpaRepository<Trade, Long> {
}
