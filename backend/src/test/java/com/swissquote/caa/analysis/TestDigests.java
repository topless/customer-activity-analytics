package com.swissquote.caa.analysis;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.swissquote.caa.customer.CustomerDtos.CurrencyTotalDto;
import com.swissquote.caa.customer.CustomerDtos.StatusCountDto;
import com.swissquote.caa.customer.CustomerDtos.TriggeredRuleDto;
import com.swissquote.caa.customer.CustomerDtos.TypeBreakdownDto;
import com.swissquote.caa.rag.RetrievedChunk;
import com.swissquote.caa.transaction.ActivityType;
import com.swissquote.caa.transaction.TransactionStatus;

/** Shared digest / chunk fixtures for analysis-layer tests. */
public final class TestDigests {

    public static final UUID TX_1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    public static final UUID TX_2 = UUID.fromString("00000000-0000-0000-0000-000000000102");
    public static final UUID CHUNK_1 = UUID.fromString("00000000-0000-0000-0000-000000000201");
    public static final UUID CHUNK_2 = UUID.fromString("00000000-0000-0000-0000-000000000202");

    private TestDigests() {
    }

    public static ActivityDigest quietCustomer() {
        return new ActivityDigest(
            new ActivityDigest.CustomerFacts("CUST-90001", "CH", "VERIFIED",
                Instant.parse("2023-01-01T00:00:00Z")),
            Instant.parse("2026-03-01T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z"),
            42, BigDecimal.ZERO,
            List.of(new TypeBreakdownDto("CARD", 42, 0,
                List.of(new CurrencyTotalDto("CHF", new BigDecimal("3500.00"))))),
            List.of(new StatusCountDto("COMPLETED", 42)),
            List.of(),
            List.of(notable(TX_1, ActivityType.CARD, "55.00", "CHF", "DEBIT card at 'AlpMart' (MCC 5411), card-present", "0", List.of())));
    }

    public static ActivityDigest highRiskCustomer() {
        TriggeredRuleDto crossBorder = new TriggeredRuleDto(
            UUID.fromString("0b000000-0000-0000-0000-000000000001"),
            "High-value cross-border payment", "PAYMENT", 4, new BigDecimal("100.00"),
            Instant.parse("2026-08-10T10:00:00Z"));
        TriggeredRuleDto highRiskJurisdiction = new TriggeredRuleDto(
            UUID.fromString("0b000000-0000-0000-0000-000000000002"),
            "Payment to high-risk jurisdiction", "PAYMENT", 1, new BigDecimal("40.00"),
            Instant.parse("2026-08-12T10:00:00Z"));
        TriggeredRuleDto structuring = new TriggeredRuleDto(
            UUID.fromString("0b000000-0000-0000-0000-000000000003"),
            "Structuring: repeated sub-threshold payments", "PAYMENT", 9, new BigDecimal("270.00"),
            Instant.parse("2026-08-15T10:00:00Z"));
        return new ActivityDigest(
            new ActivityDigest.CustomerFacts("CUST-90002", "CH", "ENHANCED",
                Instant.parse("2021-01-01T00:00:00Z")),
            Instant.parse("2026-03-01T00:00:00Z"), Instant.parse("2026-08-20T00:00:00Z"),
            60, new BigDecimal("410.00"),
            List.of(new TypeBreakdownDto("PAYMENT", 30, 1,
                List.of(new CurrencyTotalDto("CHF", new BigDecimal("250000.00"))))),
            List.of(new StatusCountDto("COMPLETED", 58), new StatusCountDto("FAILED", 2)),
            List.of(structuring, crossBorder, highRiskJurisdiction),
            List.of(
                notable(TX_1, ActivityType.PAYMENT, "9500.00", "CHF",
                    "SWIFT payment, receiver bank country AE", "30.00",
                    List.of("Structuring: repeated sub-threshold payments")),
                notable(TX_2, ActivityType.PAYMENT, "25000.00", "CHF",
                    "SWIFT payment, receiver bank country MM", "65.00",
                    List.of("Payment to high-risk jurisdiction", "High-value cross-border payment"))));
    }

    public static List<RetrievedChunk> chunks() {
        return List.of(
            new RetrievedChunk(CHUNK_1, "AML Transaction Monitoring Policy",
                "Structuring and smurfing",
                "Structuring means splitting a transfer into several smaller payments below a threshold …",
                0.82),
            new RetrievedChunk(CHUNK_2, "High-Risk Jurisdictions and Sanctions Screening",
                "Prohibited and call-for-action jurisdictions",
                "Payments to jurisdictions on the FATF call-for-action list are blocked … high-risk jurisdiction …",
                0.75));
    }

    private static ActivityDigest.NotableTransaction notable(UUID id, ActivityType type,
            String amount, String currency, String descriptor, String score, List<String> rules) {
        return new ActivityDigest.NotableTransaction(id, Instant.parse("2026-08-10T10:00:00Z"),
            type, new BigDecimal(amount), currency, TransactionStatus.COMPLETED, descriptor,
            new BigDecimal(score), rules);
    }
}
