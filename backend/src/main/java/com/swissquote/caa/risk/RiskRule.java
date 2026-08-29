package com.swissquote.caa.risk;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "risk_rules")
public class RiskRule {

    @Id
    @Column(name = "rule_id")
    private UUID id;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "applies_to", nullable = false)
    private String appliesTo;

    @Column(name = "threshold_logic", nullable = false)
    private String thresholdLogic;

    @Column(nullable = false)
    private BigDecimal weight;

    protected RiskRule() {
    }

    public UUID getId() {
        return id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getAppliesTo() {
        return appliesTo;
    }

    public String getThresholdLogic() {
        return thresholdLogic;
    }

    public BigDecimal getWeight() {
        return weight;
    }
}
