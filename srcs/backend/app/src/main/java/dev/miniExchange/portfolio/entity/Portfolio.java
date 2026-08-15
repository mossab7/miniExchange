package dev.miniExchange.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import dev.miniExchange.user.entity.User;

import java.time.Instant;
import java.math.BigInteger;

import jakarta.persistence.SequenceGenerator;
import java.util.UUID;

@Entity
@Table(name = "portfolios")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "portfolio_seq")
    @SequenceGenerator(name = "portfolio_seq", sequenceName = "portfolio_sequence", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @OneToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false, unique = true)
    private User user;

    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(nullable = false, updatable = false, name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(nullable = false)
    private BigInteger balance = BigInteger.ZERO;

    public UUID getUuid() {
        return uuid;
    }
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public BigInteger getBalance() {
        return balance;
    }

    public UUID getUserUuid() {
        return user.getUuid();
    }

    public void updateBalance(BigInteger newBalance) {
        this.balance = newBalance;
        this.updatedAt = Instant.now();
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
