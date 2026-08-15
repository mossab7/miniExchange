package dev.miniExchange.order.service;

import org.springframework.stereotype.Service;
import dev.miniExchange.order.dto.CreateOrderRequest;
import dev.miniExchange.order.dto.OrderResponse;
import java.util.List;
import dev.miniExchange.order.entity.Order;
import dev.miniExchange.order.repository.OrderRepository;
import dev.miniExchange.order.mapper.OrderMapper;
import dev.miniExchange.market.Market;
import dev.miniExchange.market.MarketService;
import java.math.BigDecimal;
import java.math.BigInteger;
import dev.miniExchange.security.user.CurrentUser;
import dev.miniExchange.portfolio.service.PortfolioService;
import dev.miniExchange.amountConvertor.AmountConverterService;
import dev.miniExchange.order.entity.Side;
import dev.miniExchange.order.entity.OrderStatus;

import dev.miniExchange.order.service.command.PlaceOrderCommand;
import dev.miniExchange.position.service.PositionService;

import java.util.UUID;
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PortfolioService portfolioService;
    private final MarketService marketService;
    private final CurrentUser currentUser;
    private final AmountConverterService amountConverterService;
    private final PositionService positionService;

    public OrderService(OrderRepository orderRepository,  MarketService marketService,
                        PortfolioService portfolioService, CurrentUser currentUser, 
                        AmountConverterService amountConverterService, PositionService positionService) {
        this.orderRepository = orderRepository;
        this.marketService = marketService;
        this.portfolioService = portfolioService;
        this.currentUser = currentUser;
        this.amountConverterService = amountConverterService;
        this.positionService = positionService;
    }

    public List<Order>getOrdersByUserId() {
        List<Order> orders = orderRepository.findByPortfolioUserId(currentUser.get().getId());
        return orders.stream().toList();
    }

    public Order getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return order;
    }
    public Order getOrderByUuid(UUID orderUuid) {
        Order order = orderRepository.findByUuid(orderUuid)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return order;
    }
    public Order createOrder(CreateOrderRequest request) {
        
        Market market = marketService.getMarket(request.market());
        BigInteger price = amountConverterService.toSmallestUnit(request.price(), market.getQuoteAssetDecimalPlaces());
        BigInteger quantity = amountConverterService.toSmallestUnit(request.quantity(), market.getBaseAssetDecimalPlaces());
        PlaceOrderCommand command = new PlaceOrderCommand(
            request.portfolioId(),
            market,
            request.side(),
            price,
            quantity
        );
        return placeOrder(command);
    }

    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new RuntimeException("Only open orders can be canceled");
        }
        order.setStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
    }
    public void cancelOrder(UUID orderUuid) {
        Order order = orderRepository.findByUuid(orderUuid)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new RuntimeException("Only open orders can be canceled");
        }
        order.setStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
    }
    private Order placeOrder(PlaceOrderCommand command){
        validateOrder(command);
        return saveOrder(command);
    }
    private Order saveOrder(PlaceOrderCommand command) {
        Order order = new Order();
        order.setPortfolio(portfolioService.getPortfolioByUserId());
        order.setAsset(command.market().getBaseAsset());
        order.setQuoteAsset(command.market().getQuoteAsset());
        order.setSide(command.side());
        order.setPrice(command.price());
        order.setQuantity(command.quantity());
        order.setStatus(OrderStatus.OPEN);
        order.setFilledQuantity(BigInteger.ZERO);
        order.setRemainingQuantity(command.quantity());
        return orderRepository.save(order);
    }
    private void validateBuyOrder(PlaceOrderCommand command) {
        BigInteger totalCost = command.price().multiply(command.quantity());
        BigInteger portfolioBalance = portfolioService.getBalanceByPortfolioId(command.portfolioId());
        if (portfolioBalance.compareTo(totalCost) < 0) {
            throw new IllegalArgumentException("Insufficient balance to place the order");
        }
    }
    private void validateSellOrder(PlaceOrderCommand command) {
        BigInteger availableQuantity = positionService.getQuantityByPortfolioIdAndAssetSymbol(command.portfolioId(), command.market().getBaseAsset().getSymbol());
        if (availableQuantity.compareTo(command.quantity()) < 0) {
            throw new IllegalArgumentException("Insufficient quantity to place the order");
        }
    }
    private void validateOrder(PlaceOrderCommand command) {
        if (command.price().compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
        if (command.quantity().compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (command.side() == Side.BUY) {
            validateBuyOrder(command);
        } else if (command.side() == Side.SELL) {
            validateSellOrder(command);
        } else {
            throw new IllegalArgumentException("Invalid order side");
        }
    }
}
