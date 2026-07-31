package dev.miniExchange.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.miniExchange.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
