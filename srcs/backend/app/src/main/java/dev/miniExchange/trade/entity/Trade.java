package dev.miniExchange.trade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.util.UUID;
import dev.miniExchange.portfolio.entity.Portfolio;
import dev.miniExchange.asset.entity.Asset;
import java.math.BigInteger;
import java.time.Instant;
import dev.miniExchange.order.entity.Order;

@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trade_seq")
    @SequenceGenerator(name = "trade_seq", sequenceName = "trade_sequence", allocationSize = 50)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private UUID uuid;
    
    @PrePersist
    void PrePersist()
    {
        if (uuid == null)
            uuid = UUID.randomUUID();
        if (date == null)
            date = Instant.now();
    }
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id",nullable = false)
    private Portfolio buyer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id",nullable = false)
    private Portfolio seller;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_order_id", nullable = false)
    private Order sellerOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_order_id", nullable = false)
    private Order buyerOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id",nullable = false)
    private Asset asset;
    @Column(nullable = false, updatable = false)
    private BigInteger amount;
    @Column(nullable = false, updatable = false)
    private BigInteger price;
    @Column(nullable = false, updatable = false)
    private Instant date;
    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public UUID getUuid() {
        return uuid;
    }
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    public Portfolio getBuyer() {
        return buyer;
    }
    public void setBuyer(Portfolio buyer) {
        this.buyer = buyer;
    }
    public Portfolio getSeller() {
        return seller;
    }
    public void setSeller(Portfolio seller) {
        this.seller = seller;
    }
    public Asset getAsset() {
        return asset;
    }
    public void setAsset(Asset asset) {
        this.asset = asset;
    }
    public BigInteger getAmount() {
        return amount;
    }
    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }
    public BigInteger getPrice() {
        return price;
    }
    public void setPrice(BigInteger price) {
        this.price = price;
    }
    public Instant getDate() {
        return date;
    }
    public void setDate(Instant date) {
        this.date = date;
    }
    public Order getSellerOrder()
    {
        return sellerOrder;
    }
    public void setSellerOrder(Order sellerOrder)
    {
        this.sellerOrder = sellerOrder;
    }
    public Order getBuyerOrder()
    {
        return buyerOrder;
    }
    public void setBuyerOrder(Order buyerOrder)
    {
        this.buyerOrder = buyerOrder;
    }
}
