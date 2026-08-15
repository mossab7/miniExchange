package dev.miniExchange.portfolio.entity;

import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import java.math.BigInteger;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import java.util.UUID;
import jakarta.persistence.PrePersist;

@Entity
@Table(name = "balance_transactions")
public class BalanceTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "balance_transaction_seq")
    @SequenceGenerator(name = "balance_transaction_seq", sequenceName = "balance_transaction_sequence",
            allocationSize = 50)
    private Long id;
    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;
    @Column(name = "amount", nullable = false)
    private BigInteger amount;
    @Column(name = "type", nullable = false)
    private String type;
    @Column(name = "Currency", nullable = false)
    private String currency;
    @ManyToOne
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;
    @PrePersist
    public void generateUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}
