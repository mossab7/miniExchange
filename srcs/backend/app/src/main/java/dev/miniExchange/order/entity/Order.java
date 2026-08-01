package dev.miniExchange.order.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "orders")
public class Order {

    @Id
    private Long id;
}
