package com.swissquote.caa.risk;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    /** Rule name + contribution per transaction, for annotating transaction listings. */
    @Query("""
        select ra.transactionId as transactionId, r.ruleName as ruleName,
               ra.scoreContribution as scoreContribution
        from RiskAssessment ra
        join RiskRule r on r.id = ra.ruleId
        where ra.transactionId in :transactionIds
        """)
    List<TransactionRuleRow> findByTransactionIds(@Param("transactionIds") Collection<UUID> transactionIds);

    interface TransactionRuleRow {

        UUID getTransactionId();

        String getRuleName();

        BigDecimal getScoreContribution();
    }
}
