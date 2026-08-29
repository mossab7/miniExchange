package dev.miniExchange.trade;

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
import dev.miniExchange.portfolio.position.dto.CreatePositionCommand;
import dev.miniExchange.portfolio.position.service.PositionService;
import dev.miniExchange.portfolio.repository.PortfolioRepository;
import dev.miniExchange.portfolio.service.PortfolioService;
import dev.miniExchange.security.user.SecurityUser;
import dev.miniExchange.trade.entity.Trade;
import dev.miniExchange.trade.event.TradeEvent;
import dev.miniExchange.trade.repository.TradeRepository;
import dev.miniExchange.trade.service.TradeService;
import dev.miniExchange.user.dto.CreateUserRequest;
import dev.miniExchange.user.entity.User;
import dev.miniExchange.user.service.UserService;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TradeServiceTest {

    @Autowired private TradeService tradeService;
    @Autowired private OrderService orderService;
    @Autowired private UserService userService;
    @Autowired private PortfolioService portfolioService;
    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private AssetService assetService;
    @Autowired private PositionService positionService;
    @Autowired private TradeRepository tradeRepository;

    // ── Seller ────────────────────────────────────────────────────────────────
    private User seller;
    private Portfolio sellerPortfolio;
    private Order sellOrder;

    // ── Buyer ─────────────────────────────────────────────────────────────────
    private User buyer;
    private Portfolio buyerPortfolio;
    private Order buyOrder;

    /**
     * BTC/USD market:
     *   seller holds 1 BTC (100_000_000 satoshis) and posts a sell order.
     *   buyer holds $1 000 USD and posts a matching buy order.
     *
     * Price  : $500.00  = 50_000 cents
     * Qty    : 0.10 BTC = 10_000_000 satoshis
     * Cost   : $50.00   = 5_000 cents
     *            = price(50_000) * qty(10_000_000) / 10^8 = 5_000
     */
    @BeforeEach
    void setUp() {
        // Ensure assets exist (idempotent)
        if (assetService.getAll().stream().noneMatch(a -> a.getSymbol().equals("BTC"))) {
            assetService.create(new CreateAssetRequest("BTC", "Bitcoin", 8, true));
        }
        if (assetService.getAll().stream().noneMatch(a -> a.getSymbol().equals("USD"))) {
            assetService.create(new CreateAssetRequest("USD", "US Dollar", 2, true));
        }

        // ── Create seller ─────────────────────────────────────────────────────
        seller = userService.createUser(new CreateUserRequest("seller", "seller@test.com", "pass123456"));
        // Bug fix: portfolio name is "default portfolio" (space), not "default_portfolio"
        sellerPortfolio = portfolioRepository
                .findByUser_IdAndName(seller.getId(), "default portfolio")
                .orElseThrow();

        loginAs(seller);

        // Give seller 1 BTC position
        long btcAssetId = assetService.getBySymbol("BTC").getId();
        positionService.createPosition(new CreatePositionCommand(
                btcAssetId, sellerPortfolio.getId(), new BigInteger("100000000")));

        // Seller places a sell order: 0.10 BTC at $500.00
        sellOrder = orderService.createOrder(new CreateOrderRequest(
                sellerPortfolio.getUuid(), "BTC/USD", "500.00", "0.10000000", Side.SELL));

        // ── Create buyer ──────────────────────────────────────────────────────
        buyer = userService.createUser(new CreateUserRequest("buyer", "buyer@test.com", "pass123456"));
        buyerPortfolio = portfolioRepository
                .findByUser_IdAndName(buyer.getId(), "default portfolio")
                .orElseThrow();

        loginAs(buyer);

        // Deposit $1000.00 (= 100_000 cents)
        portfolioService.updateBalance(buyerPortfolio.getUuid(),
                new UpdateBalanceRequest("1000.00", "DEPOSIT", "USD"));

        // Buyer places a matching buy order: 0.10 BTC at $500.00
        buyOrder = orderService.createOrder(new CreateOrderRequest(
                buyerPortfolio.getUuid(), "BTC/USD", "500.00", "0.10000000", Side.BUY));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void executeTrade_persistsTrade() {
        UUID tradeUuid = UUID.randomUUID();
        TradeEvent event = buildEvent(tradeUuid, new BigInteger("10000000")); // 0.10 BTC

        tradeService.executeTrade(event);

        Optional<Trade> saved = tradeRepository.findByUuid(tradeUuid);
        assertTrue(saved.isPresent(), "Trade must be persisted");
        Trade trade = saved.get();
        assertEquals(new BigInteger("10000000"), trade.getAmount());
        assertEquals(new BigInteger("50000"), trade.getPrice());
        assertNotNull(trade.getBuyer());
        assertNotNull(trade.getSeller());
        assertNotNull(trade.getAsset());
        assertNotNull(trade.getBuyerOrder());
        assertNotNull(trade.getSellerOrder());
    }

    @Test
    void executeTrade_buyerLockedBalanceDeductedByTradeValue() {
        // Buyer's locked balance before trade: 5_000 cents ($50.00)
        Portfolio buyerBefore = portfolioRepository.findById(buyerPortfolio.getId()).orElseThrow();
        assertEquals(BigInteger.valueOf(5_000), buyerBefore.getLockedBalance());

        tradeService.executeTrade(buildEvent(UUID.randomUUID(), new BigInteger("10000000")));

        Portfolio buyerAfter = portfolioRepository.findById(buyerPortfolio.getId()).orElseThrow();
        // Locked balance must be zero after a fully matched order
        assertEquals(BigInteger.ZERO, buyerAfter.getLockedBalance());
    }

    @Test
    void executeTrade_sellerReceivesTradeValueInAvailableBalance() {
        // Seller's available balance before trade: 0 (all in BTC, no USD)
        Portfolio sellerBefore = portfolioRepository.findById(sellerPortfolio.getId()).orElseThrow();
        BigInteger sellerUsdBefore = sellerBefore.getAvailableBalance();

        tradeService.executeTrade(buildEvent(UUID.randomUUID(), new BigInteger("10000000")));

        Portfolio sellerAfter = portfolioRepository.findById(sellerPortfolio.getId()).orElseThrow();
        // Seller receives 5_000 cents ($50.00)
        assertEquals(sellerUsdBefore.add(BigInteger.valueOf(5_000)), sellerAfter.getAvailableBalance());
    }

    @Test
    void executeTrade_sellerPositionDeducted() {
        BigInteger sellerQtyBefore = positionService
                .getPosition(sellerPortfolio.getId(), "BTC").getQuantity();

        tradeService.executeTrade(buildEvent(UUID.randomUUID(), new BigInteger("10000000")));

        BigInteger sellerQtyAfter = positionService
                .getPosition(sellerPortfolio.getId(), "BTC").getQuantity();
        assertEquals(sellerQtyBefore.subtract(new BigInteger("10000000")), sellerQtyAfter);
    }

    @Test
    void executeTrade_buyerPositionCredited() {
        tradeService.executeTrade(buildEvent(UUID.randomUUID(), new BigInteger("10000000")));

        BigInteger buyerQty = positionService
                .getPosition(buyerPortfolio.getId(), "BTC").getQuantity();
        assertEquals(new BigInteger("10000000"), buyerQty);
    }

    @Test
    void executeTrade_buyerPositionCreatedWhenAbsent() {
        // Buyer has no BTC position before the trade
        assertFalse(positionService.getAllPositions(buyerPortfolio.getId())
                .stream().anyMatch(p -> p.getAssetSymbol().equals("BTC")),
                "Buyer should have no BTC position before trade");

        tradeService.executeTrade(buildEvent(UUID.randomUUID(), new BigInteger("10000000")));

        assertTrue(positionService.getAllPositions(buyerPortfolio.getId())
                .stream().anyMatch(p -> p.getAssetSymbol().equals("BTC")),
                "Buyer should have a BTC position after trade");
    }

    @Test
    void executeTrade_ordersMarkedFilled_whenFullyMatched() {
        tradeService.executeTrade(buildEvent(UUID.randomUUID(), new BigInteger("10000000")));

        Order refreshedSell = orderService.getReference(sellOrder.getId());
        Order refreshedBuy  = orderService.getReference(buyOrder.getId());
        assertEquals(OrderStatus.FILLED, refreshedSell.getStatus());
        assertEquals(OrderStatus.FILLED, refreshedBuy.getStatus());
    }

    @Test
    void executeTrade_ordersMarkedPartiallyFilled_whenPartialMatch() {
        // Trade only 0.05 BTC out of the 0.10 BTC order
        tradeService.executeTrade(buildEvent(UUID.randomUUID(), new BigInteger("5000000")));

        Order refreshedSell = orderService.getReference(sellOrder.getId());
        Order refreshedBuy  = orderService.getReference(buyOrder.getId());
        assertEquals(OrderStatus.PARTIALLY_FILLED, refreshedSell.getStatus());
        assertEquals(OrderStatus.PARTIALLY_FILLED, refreshedBuy.getStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Build a TradeEvent with the given UUID and quantity (in satoshis). */
    private TradeEvent buildEvent(UUID tradeUuid, BigInteger quantity) {
        return new TradeEvent(
                tradeUuid,
                sellOrder.getId(),
                buyOrder.getId(),
                "BTC",
                quantity,
                new BigInteger("50000"),  // $500.00 in cents
                Instant.now()
        );
    }

    private void loginAs(User user) {
        SecurityUser su = new SecurityUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(su, null, su.getAuthorities())
        );
    }
}
