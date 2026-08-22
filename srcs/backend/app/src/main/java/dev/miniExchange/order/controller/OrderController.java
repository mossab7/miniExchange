package dev.miniExchange.order.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;

import dev.miniExchange.order.service.OrderService;
import dev.miniExchange.order.dto.CreateOrderRequest;
import dev.miniExchange.order.dto.OrderResponse;
import dev.miniExchange.order.mapper.OrderMapper;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders() {
        return ResponseEntity.ok(orderService.getOrdersByUserId().stream().map(OrderMapper::toResponse).toList());
    }

    @GetMapping("/{orderUuid}")
    public ResponseEntity<OrderResponse> getOrderByUuid(@PathVariable UUID orderUuid) {
        return ResponseEntity.ok(OrderMapper.toResponse(orderService.getOrderByUuid(orderUuid)));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return ResponseEntity.ok(OrderMapper.toResponse(orderService.createOrder(request)));
    }

    @DeleteMapping("/{orderUuid}")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID orderUuid) {
        orderService.cancelOrder(orderUuid);
        return ResponseEntity.noContent().build();
    }
}
