package com.swissquote.caa.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Base activity row; exactly one of {@link #card}/{@link #payment}/{@link #crypto} is present,
 * matching {@link #activityType} (table-per-detail layout given in the assignment).
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(name = "transaction_id")
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToOne(mappedBy = "transaction", fetch = FetchType.LAZY)
    private CardActivity card;

    @OneToOne(mappedBy = "transaction", fetch = FetchType.LAZY)
    private PaymentActivity payment;

    @OneToOne(mappedBy = "transaction", fetch = FetchType.LAZY)
    private CryptoActivity crypto;

    protected Transaction() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public CardActivity getCard() {
        return card;
    }

    public PaymentActivity getPayment() {
        return payment;
    }

    public CryptoActivity getCrypto() {
        return crypto;
    }
}
