package com.swissquote.caa.risk;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "risk_assessments")
public class RiskAssessment {

    @Id
    @Column(name = "assessment_id")
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "score_contribution", nullable = false)
    private BigDecimal scoreContribution;

    protected RiskAssessment() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getRuleId() {
        return ruleId;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public BigDecimal getScoreContribution() {
        return scoreContribution;
    }
}
