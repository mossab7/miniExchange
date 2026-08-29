package dev.miniExchange.portfolio.entity;

import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.PrePersist;
import java.math.BigInteger;
import java.util.UUID;

@Entity
@Table(name = "balance_transactions")
public class BalanceTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "balance_transaction_seq")
    @SequenceGenerator(name = "balance_transaction_seq", sequenceName = "balance_transaction_sequence", allocationSize = 50)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "amount", nullable = false)
    private BigInteger amount;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    protected BalanceTransaction() {
    }

    public BalanceTransaction(Portfolio portfolio, BigInteger amount, String type, String currency) {
        this.portfolio = portfolio;
        this.amount = amount;
        this.type = type;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public String getCurrency() {
        return currency;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    @PrePersist
    public void generateUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}
