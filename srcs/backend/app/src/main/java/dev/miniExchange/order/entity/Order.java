package dev.miniExchange.order.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import dev.miniExchange.asset.entity.Asset;
import dev.miniExchange.portfolio.entity.Portfolio;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.util.UUID;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import java.math.BigInteger;

import java.time.Instant;

@Entity(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_id_generator")
    @SequenceGenerator(name = "order_id_generator", sequenceName = "order_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Side side;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_asset_id", nullable = false)
    private Asset quoteAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false)
    private BigInteger quantity;

    @Column(nullable = false)
    private BigInteger price;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(nullable = false)
    private BigInteger filledQuantity;

    @Column(nullable = false)
    private BigInteger remainingQuantity;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    //setters and getters
    public Long getId() {
        return id;
    }
    public UUID getUuid() {
        return uuid;
    }
    public Side getSide() {
        return side;
    }
    public void setSide(Side side) {
        this.side = side;
    }
    public Asset getAsset() {
        return asset;
    }
    public void setAsset(Asset asset) {
        this.asset = asset;
    }
    public String getSymbol() {
        return asset.getSymbol();
    }
    public String getQuoteSymbol() {
        return quoteAsset.getSymbol();
    }
    public Portfolio getPortfolio() {
        return portfolio;
    }
    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }
    public BigInteger getQuantity() {
        return quantity;
    }
    public void setQuantity(BigInteger quantity) {

        this.quantity = quantity;
    }
    public BigInteger getPrice() {
        return price;
    }
    public void setPrice(BigInteger price) {
        this.price = price;
    }
    public OrderStatus getStatus() {
        return status;
    }
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    public BigInteger getFilledQuantity() {
        return filledQuantity;
    }
    public void setFilledQuantity(BigInteger filledQuantity) {
        this.filledQuantity = filledQuantity;
    }
    public BigInteger getRemainingQuantity() {
        return remainingQuantity;
    }
    public void setRemainingQuantity(BigInteger remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }
    public Asset getQuoteAsset() {
        return quoteAsset;
    }
    public void setQuoteAsset(Asset quoteAsset) {
        this.quoteAsset = quoteAsset;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
