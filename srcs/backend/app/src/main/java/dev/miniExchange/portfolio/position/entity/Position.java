package dev.miniExchange.portfolio.position.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import java.math.BigInteger;
import dev.miniExchange.asset.entity.Asset;
import dev.miniExchange.portfolio.entity.Portfolio;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.util.UUID;
import jakarta.persistence.PrePersist;

@Entity
@Table(name = "positions",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"asset_id", "portfolio_id"})
    }
)
public class Position {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator="position_seq"
    )
    @SequenceGenerator(
        name="position_seq",
        sequenceName="position_sequence",
        allocationSize=50
    )
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;
    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    @JoinColumn(nullable = false, name = "asset_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Asset asset;

    @JoinColumn(nullable = false, name = "portfolio_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Portfolio portfolio;

    @Column(nullable = false)
    private BigInteger quantity;

    protected Position() {
    }

    public Position(Asset asset, Portfolio portfolio, BigInteger quantity) {
        this.asset = asset;
        this.portfolio = portfolio;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Long getAssetId() {
        return asset.getId();
    }

    public Asset getAsset() {
        return asset;
    }

    public String getAssetSymbol() {
        return asset.getSymbol();
    }

    public Long getPortfolioId() {
        return portfolio.getId();
    }

    public BigInteger getQuantity() {
        return quantity;
    }

    public void deductQuantity(BigInteger quantity) {
        if (quantity == null || quantity.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("Deduct quantity must be positive");
        }
        this.quantity = this.quantity.subtract(quantity);
    }

    public void addQuantity(BigInteger quantity) {
        this.quantity = this.quantity.add(quantity);
    }
}
