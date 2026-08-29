package dev.miniExchange.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.FetchType;
import jakarta.persistence.UniqueConstraint;

import dev.miniExchange.user.entity.User;
import dev.miniExchange.common.exceptions.InsufficientBalanceException;

import java.time.Instant;
import java.math.BigInteger;
import java.util.UUID;

@Entity
@Table(name = "portfolios", uniqueConstraints = {
    @UniqueConstraint(name = "uq_portfolios_user_name", columnNames = {"user_id", "name"})
})
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "portfolio_seq")
    @SequenceGenerator(name = "portfolio_seq", sequenceName = "portfolio_sequence", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(nullable = false, name = "available_balance")
    private BigInteger availableBalance = BigInteger.ZERO;

    @Column(nullable = false, name = "locked_balance")
    private BigInteger lockedBalance = BigInteger.ZERO;

    protected Portfolio() {
    }

    public Portfolio(User user) {
        this.user = user;
        this.name = "default portfolio";
        this.description = "default portfolio";
        this.availableBalance = BigInteger.ZERO;
        this.lockedBalance = BigInteger.ZERO;
    }

    public Portfolio(User user, String name, String description) {
        this.user = user;
        this.name = name;
        this.description = description;
        this.availableBalance = BigInteger.ZERO;
        this.lockedBalance = BigInteger.ZERO;
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public User getUser() {
        return user;
    }

    public UUID getUserUuid() {
        return user != null ? user.getUuid() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigInteger getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigInteger availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigInteger getLockedBalance() {
        return lockedBalance;
    }

    public void setLockedBalance(BigInteger lockedBalance) {
        this.lockedBalance = lockedBalance;
    }

    public BigInteger getTotalBalance() {
        return availableBalance.add(lockedBalance);
    }

    public void deposit(BigInteger amount) {
        if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        this.availableBalance = this.availableBalance.add(amount);
        this.updatedAt = Instant.now();
    }

    public void withdraw(BigInteger amount) {
        if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (this.availableBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient available balance for withdrawal");
        }
        this.availableBalance = this.availableBalance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    public void lockBalance(BigInteger amount) {
        if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("Lock amount must be positive");
        }
        if (this.availableBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient available balance to lock");
        }
        this.availableBalance = this.availableBalance.subtract(amount);
        this.lockedBalance = this.lockedBalance.add(amount);
        this.updatedAt = Instant.now();
    }

    public void unlockBalance(BigInteger amount) {
        if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("Unlock amount must be positive");
        }
        if (this.lockedBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Cannot unlock more than currently locked balance");
        }
        this.lockedBalance = this.lockedBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
        this.updatedAt = Instant.now();
    }

    public void deductLockedBalance(BigInteger amount) {
        if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("Deduct amount must be positive");
        }
        if (this.lockedBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Cannot deduct more than currently locked balance");
        }
        this.lockedBalance = this.lockedBalance.subtract(amount);
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
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
        if (this.availableBalance == null) {
            this.availableBalance = BigInteger.ZERO;
        }
        if (this.lockedBalance == null) {
            this.lockedBalance = BigInteger.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
