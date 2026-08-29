package com.swissquote.caa.analysis;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.swissquote.caa.customer.CustomerDtos.StatusCountDto;
import com.swissquote.caa.customer.CustomerDtos.TriggeredRuleDto;
import com.swissquote.caa.customer.CustomerDtos.TypeBreakdownDto;
import com.swissquote.caa.transaction.ActivityType;
import com.swissquote.caa.transaction.TransactionStatus;

/**
 * Structured snapshot of a customer's activity handed to the LLM adapters: the prompt is
 * rendered from it, and the stub adapter reasons over it directly. Customer name, email and
 * date of birth are deliberately absent — the analysis works on the pseudonymous customer
 * number (data minimisation towards external LLM providers).
 */
public record ActivityDigest(
    CustomerFacts customer,
    Instant windowFrom,
    Instant windowTo,
    long transactionCount,
    BigDecimal riskScore,
    List<TypeBreakdownDto> byType,
    List<StatusCountDto> byStatus,
    List<TriggeredRuleDto> triggeredRules,
    List<NotableTransaction> notableTransactions) {

    public record CustomerFacts(String customerNumber, String country, String kycLevel,
                                Instant onboardedAt) {
    }

    /** One transaction rendered into the prompt; descriptor is a compact human-readable line. */
    public record NotableTransaction(UUID id, Instant at, ActivityType type, BigDecimal amount,
                                     String currency, TransactionStatus status, String descriptor,
                                     BigDecimal riskScore, List<String> ruleNames) {
    }
}
