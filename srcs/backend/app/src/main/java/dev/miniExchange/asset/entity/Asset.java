package dev.miniExchange.asset.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import jakarta.persistence.Column;

@Entity
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, length = 4)
    private String symbol;
    @Column(unique = true, nullable = false, length = 20)
    private String name;
    @Column(nullable = false,name = "decimal_places")
    private int decimalPlaces;
    @Column(nullable = false,name = "is_active")
    private boolean isActive;

    protected Asset() {
        // Default constructor for JPA
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
}
