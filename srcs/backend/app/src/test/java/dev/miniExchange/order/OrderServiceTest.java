package dev.miniExchange.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import dev.miniExchange.asset.dto.CreateAssetRequest;
import dev.miniExchange.asset.service.AssetService;
import dev.miniExchange.order.dto.CreateOrderRequest;
import dev.miniExchange.order.entity.Order;
import dev.miniExchange.order.entity.OrderStatus;
import dev.miniExchange.order.entity.Side;
import dev.miniExchange.order.service.OrderService;
import dev.miniExchange.portfolio.dto.UpdateBalanceRequest;
import dev.miniExchange.portfolio.entity.Portfolio;
import dev.miniExchange.portfolio.repository.PortfolioRepository;
import dev.miniExchange.portfolio.service.PortfolioService;
import dev.miniExchange.security.user.SecurityUser;
import dev.miniExchange.user.dto.CreateUserRequest;
import dev.miniExchange.user.entity.User;
import dev.miniExchange.user.service.UserService;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private AssetService assetService;

    private User testUser;
    private Portfolio testPortfolio;

    @BeforeEach
    void setUp() {
        if (!assetService.getAll().stream().anyMatch(a -> a.getSymbol().equals("BTC"))) {
            assetService.create(new CreateAssetRequest("BTC", "Bitcoin", 8, true));
        }
        if (!assetService.getAll().stream().anyMatch(a -> a.getSymbol().equals("USD"))) {
            assetService.create(new CreateAssetRequest("USD", "US Dollar", 2, true));
        }

        testUser = userService.createUser(new CreateUserRequest("orderUser", "orderUser@test.com", "pass123456"));
        testPortfolio = portfolioRepository.findByUser_IdAndName(testUser.getId(),"default portfolio").orElseThrow();

        // Setup security context for testUser
        SecurityUser securityUser = new SecurityUser(testUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities())
        );

        // Deposit $1000.00 USD into portfolio
        portfolioService.updateBalance(testPortfolio.getUuid(), new UpdateBalanceRequest("1000.00", "DEPOSIT", "USD"));
    }

    @Test
    void testPlaceBuyOrderLocksBalanceWithCorrectPrecision() {
        // Price: $500.00 per BTC, Quantity: 0.1 BTC -> Total Cost = $50.00 (5000 in smallest USD units)
        CreateOrderRequest request = new CreateOrderRequest(
                testPortfolio.getUuid(),
                "BTC/USD",
                "500.00",
                "0.10000000",
                Side.BUY
        );

        Order order = orderService.createOrder(request);
        assertNotNull(order);
        assertEquals(OrderStatus.OPEN, order.getStatus());

        // Check portfolio balances:
        // Initial available: 100,000 cents ($1000)
        // Locked for order: 5,000 cents ($50.00)
        // Remaining available: 95,000 cents ($950.00)
        Portfolio refreshedPortfolio = portfolioRepository.findByUuid(testPortfolio.getUuid()).orElseThrow();
        assertEquals(BigInteger.valueOf(95000), refreshedPortfolio.getAvailableBalance());
        assertEquals(BigInteger.valueOf(5000), refreshedPortfolio.getLockedBalance());

        // Cancel order -> should unlock funds back to available balance
        orderService.cancelOrder(order.getUuid());
        Portfolio canceledPortfolio = portfolioRepository.findByUuid(testPortfolio.getUuid()).orElseThrow();
        assertEquals(BigInteger.valueOf(100000), canceledPortfolio.getAvailableBalance());
        assertEquals(BigInteger.ZERO, canceledPortfolio.getLockedBalance());
    }
}
