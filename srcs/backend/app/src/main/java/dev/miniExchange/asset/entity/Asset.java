package dev.miniExchange.asset.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import java.util.UUID;

import jakarta.persistence.Column;


@Entity
@Table(name = "assets")
public class Asset {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator="asset_seq"
    )
    @SequenceGenerator(
        name="asset_seq",
        sequenceName="asset_sequence",
        allocationSize=50
    )
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;
    @PrePersist
    public void generateUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
    @Column(unique = true, nullable = false, length = 4)
    private String symbol;
    @Column(unique = true, nullable = false, length = 20)
    private String name;
    @Column(nullable = false,name = "decimal_places")
    private int decimalPlaces;
    @Column(nullable = false,name = "is_active")
    private boolean isActive;

    protected Asset() {
    }

    public Asset(String symbol, String name, int decimalPlaces, boolean isActive) {
        this.symbol = symbol;
        this.name = name;
        this.decimalPlaces = decimalPlaces;
        this.isActive = isActive;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public boolean isActive() {
        return isActive;
    }
    public UUID getUuid() {
        return uuid;
    }
}
