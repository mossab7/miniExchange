package dev.miniExchange.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.miniExchange.order.entity.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.LongAccumulator;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByUuid(UUID uuid);
    Optional<Order> findById(Long id);

    List<Order> findByPortfolioUserId(Long portfolioId);

}
