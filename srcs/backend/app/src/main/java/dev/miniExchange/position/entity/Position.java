package dev.miniExchange.position.entity;

import jakarta.persistence.Entity;
import jakarta.annotation.Generated;
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
import java.util.UUID;
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
    private UUID uuid = UUID.randomUUID();

    @Column(nullable = false, name = "asset_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Asset asset;

    @Column(nullable = false, name = "portfolio_id")
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

    public Long getAssetId() {
        return asset.getId();
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
}
