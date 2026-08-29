package dev.miniExchange.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.miniExchange.order.dto.CreateOrderRequest;
import dev.miniExchange.order.entity.Order;
import dev.miniExchange.order.repository.OrderRepository;
import dev.miniExchange.market.Market;
import dev.miniExchange.market.MarketService;
import dev.miniExchange.security.user.CurrentUser;
import dev.miniExchange.portfolio.entity.Portfolio;
import dev.miniExchange.portfolio.position.service.PositionService;
import dev.miniExchange.portfolio.repository.PortfolioRepository;
import dev.miniExchange.trade.command.ProcessTradeCommand;
import dev.miniExchange.amountConvertor.AmountConverterService;
import dev.miniExchange.order.entity.Side;
import dev.miniExchange.order.entity.OrderStatus;
import dev.miniExchange.order.service.command.PlaceOrderCommand;
import dev.miniExchange.order.exceptions.OrderNotFoundException;
import dev.miniExchange.common.exceptions.InsufficientQuantityException;
import dev.miniExchange.common.exceptions.ResourceNotFoundException;
import dev.miniExchange.common.exceptions.ValidationException;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PortfolioRepository portfolioRepository;
    private final MarketService marketService;
    private final CurrentUser currentUser;
    private final AmountConverterService amountConverterService;
    private final PositionService positionService;

    public OrderService(OrderRepository orderRepository,
            MarketService marketService,
            PortfolioRepository portfolioRepository,
            CurrentUser currentUser,
            AmountConverterService amountConverterService,
            PositionService positionService) {
        this.orderRepository = orderRepository;
        this.marketService = marketService;
        this.portfolioRepository = portfolioRepository;
        this.currentUser = currentUser;
        this.amountConverterService = amountConverterService;
        this.positionService = positionService;
    }

    public List<Order> getOrdersByUserId() {
        return orderRepository.findByPortfolio_User_Id(currentUser.getId());
    }

    public Order getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ValidationException("Order not found with ID: " + orderId));
        if (!order.getPortfolio().getUser().getId().equals(currentUser.getId())) {
            throw new ValidationException("Order not found with ID: " + orderId);
        }
        return order;
    }

    public Order getOrderByUuid(UUID orderUuid) {
        Order order = orderRepository.findByUuid(orderUuid)
                .orElseThrow(() -> new OrderNotFoundException(orderUuid));
        if (!order.getPortfolio().getUser().getId().equals(currentUser.getId())) {
            throw new OrderNotFoundException(orderUuid);
        }
        return order;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Portfolio portfolio =  portfolioRepository.findByUuid(request.portfolioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio",request.portfolioId()));
        Market market = marketService.getMarket(request.market());

        BigInteger price = amountConverterService.toSmallestUnit(request.price(), market.getQuoteAssetDecimalPlaces());
        BigInteger quantity = amountConverterService.toSmallestUnit(request.quantity(),
                market.getBaseAssetDecimalPlaces());

        if (price.compareTo(BigInteger.ZERO) <= 0) {
            throw new ValidationException("Price must be greater than zero");
        }
        if (quantity.compareTo(BigInteger.ZERO) <= 0) {
            throw new ValidationException("Quantity must be greater than zero");
        }

        PlaceOrderCommand command = new PlaceOrderCommand(
                portfolio.getUuid(),
                market,
                request.side(),
                price,
                quantity);

        if (command.side() == Side.BUY) {
            BigInteger totalCost = calculateTotalCost(command.price(), command.quantity(),
                    market.getBaseAssetDecimalPlaces());
            if (totalCost.compareTo(BigInteger.ZERO) <= 0) {
                throw new ValidationException("Calculated order cost must be greater than zero");
            }
            portfolio.lockBalance(totalCost);
        } else if (command.side() == Side.SELL) {
            BigInteger availableQuantity = positionService.getQuantity(portfolio.getUuid(),
                    market.getBaseAsset().getSymbol());
            if (availableQuantity.compareTo(command.quantity()) < 0) {
                throw new InsufficientQuantityException("Insufficient quantity to place the sell order");
            }
        } else {
            throw new ValidationException("Invalid order side: " + command.side());
        }

        Order order = new Order();
        order.setPortfolio(portfolio);
        order.setAsset(command.market().getBaseAsset());
        order.setQuoteAsset(command.market().getQuoteAsset());
        order.setSide(command.side());
        order.setPrice(command.price());
        order.setQuantity(command.quantity());
        order.setStatus(OrderStatus.OPEN);
        order.setFilledQuantity(BigInteger.ZERO);

        return orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(UUID orderUuid) {
        Order order = orderRepository.findByUuid(orderUuid)
                .orElseThrow(() -> new OrderNotFoundException(orderUuid));

        if (!order.getPortfolio().getUser().getId().equals(currentUser.getId())) {
            throw new OrderNotFoundException(orderUuid);
        }

        if (order.getStatus() != OrderStatus.OPEN && order.getStatus() != OrderStatus.PARTIALLY_FILLED) {
            throw new ValidationException("Only open or partially filled orders can be canceled");
        }

        if (order.getSide() == Side.BUY) {
            BigInteger remainingCost = calculateTotalCost(
                    order.getPrice(),
                    order.getQuantity().subtract(order.getFilledQuantity()),
                    order.getAsset().getDecimalPlaces());
            order.getPortfolio().unlockBalance(remainingCost);
        }

        order.setStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
    }

    private BigInteger calculateTotalCost(BigInteger price, BigInteger quantity, int baseAssetDecimalPlaces) {
        return price.multiply(quantity).divide(BigInteger.TEN.pow(baseAssetDecimalPlaces));
    }

    public void applyTrade(ProcessTradeCommand command) {
        Order sellOrder = orderRepository.findById(command.sellerOrderId())
                .orElseThrow(() -> new OrderNotFoundException(command.sellerOrderId()));

        Order buyOrder = orderRepository.findById(command.buyerOrderId())
                .orElseThrow(() -> new OrderNotFoundException(command.buyerOrderId()));

        sellOrder.updateFilledQuantity(command.amount());
        buyOrder.updateFilledQuantity(command.amount());

    }

    public Long getOrderPortfolioId(Long orderId) {
        return orderRepository.findPortfolioIdByOrderId(orderId);
    }

    public Order getReference(Long orderId) {
        return orderRepository.getReferenceById(orderId);
    }
}
