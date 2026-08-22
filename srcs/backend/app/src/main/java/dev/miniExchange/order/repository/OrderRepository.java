package dev.miniExchange.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import dev.miniExchange.order.entity.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUuid(UUID uuid);

    List<Order> findByPortfolio_User_Id(Long userId);

    List<Order> findByPortfolioId(Long portfolioId);

    List<Order> findByPortfolioUuid(UUID portfolioUuid);
}
