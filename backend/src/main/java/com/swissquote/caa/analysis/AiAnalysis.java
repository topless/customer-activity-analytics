package com.swissquote.caa.analysis;

import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A persisted AI analysis run (spec #5). JSON payloads (findings, recommendations, cited
 * policies) are stored as JSONB and (de)serialised in the service layer; prompt and raw
 * response are kept verbatim for auditability.
 */
@Entity
@Table(name = "ai_analyses")
public class AiAnalysis {

    @Id
    @Column(name = "analysis_id")
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status;

    @Column(nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Column
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String findings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String recommendations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cited_policies", columnDefinition = "jsonb")
    private String citedPolicies;

    @Column(name = "activity_window_from")
    private Instant activityWindowFrom;

    @Column(name = "activity_window_to")
    private Instant activityWindowTo;

    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column
    private String prompt;

    @Column(name = "raw_response")
    private String rawResponse;

    protected AiAnalysis() {
    }

    public AiAnalysis(UUID id, UUID customerId, UUID requestedBy, Instant requestedAt) {
        this.id = id;
        this.customerId = customerId;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
    }

    public void markCompleted(Instant at, String model, RiskLevel riskLevel, String summary,
                              String findings, String recommendations, String citedPolicies) {
        this.completedAt = at;
        this.status = AnalysisStatus.COMPLETED;
        this.model = model;
        this.riskLevel = riskLevel;
        this.summary = summary;
        this.findings = findings;
        this.recommendations = recommendations;
        this.citedPolicies = citedPolicies;
    }

    public void markFailed(Instant at, String model, String errorMessage) {
        this.completedAt = at;
        this.status = AnalysisStatus.FAILED;
        this.model = model;
        this.errorMessage = errorMessage;
    }

    public void setActivityWindow(Instant from, Instant to, int transactionCount) {
        this.activityWindowFrom = from;
        this.activityWindowTo = to;
        this.transactionCount = transactionCount;
    }

    public void setAudit(String prompt, String rawResponse) {
        this.prompt = prompt;
        this.rawResponse = rawResponse;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public String getModel() {
        return model;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getSummary() {
        return summary;
    }

    public String getFindings() {
        return findings;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public String getCitedPolicies() {
        return citedPolicies;
    }

    public Instant getActivityWindowFrom() {
        return activityWindowFrom;
    }

    public Instant getActivityWindowTo() {
        return activityWindowTo;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}
