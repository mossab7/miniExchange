package dev.miniExchange.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.miniExchange.order.entity.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByUuid(UUID uuid);

    Optional<Order> findById(Long id);

    List<Order> findByPortfolio_User_Id(Long userId);

    List<Order> findByPortfolioId(Long portfolioId);

    List<Order> findByPortfolioUuid(UUID portfolioUuid);

    @Query("""
                SELECT o.portfolio.id FROM ExchangeOrder o WHERE o.id = :id
            """)
    Long findPortfolioIdByOrderId(@Param("id") Long id);
}
