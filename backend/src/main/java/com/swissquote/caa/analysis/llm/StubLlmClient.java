package com.swissquote.caa.analysis.llm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swissquote.caa.analysis.ActivityDigest;
import com.swissquote.caa.customer.CustomerDtos.TriggeredRuleDto;
import com.swissquote.caa.rag.RetrievedChunk;

/**
 * Deterministic, offline "analyst": derives a defensible risk level, findings, and
 * recommendations directly from the rule signals in the digest, and emits exactly the JSON
 * shape a real model is prompted for — so the entire pipeline (RAG retrieval, response
 * parsing, persistence, UI) runs unchanged without an API key.
 */
public class StubLlmClient implements LlmClient {

    public static final String MODEL_ID = "stub-analyst-v1";

    private final ObjectMapper objectMapper;

    public StubLlmClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String modelId() {
        return MODEL_ID;
    }

    @Override
    public String complete(LlmRequest request) {
        ActivityDigest digest = request.digest();
        RiskBand band = riskBand(digest);

        List<LlmAnalysisResponse.Finding> findings = digest.triggeredRules().stream()
            .limit(6)
            .map(rule -> finding(rule, digest))
            .toList();

        LlmAnalysisResponse response = new LlmAnalysisResponse(
            band.level(),
            summary(digest, band),
            findings,
            recommendations(digest, band),
            citedChunks(digest, request.retrievedChunks()));
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new LlmException("Stub response serialisation failed", e);
        }
    }

    private record RiskBand(String level, String label) {
    }

    private static RiskBand riskBand(ActivityDigest digest) {
        double score = digest.riskScore().doubleValue();
        boolean sanctionsSignal = digest.triggeredRules().stream()
            .anyMatch(r -> r.ruleName().toLowerCase(Locale.ROOT).contains("high-risk jurisdiction"));
        if (score >= 400 || (sanctionsSignal && score >= 150)) {
            return new RiskBand("CRITICAL", "requires immediate escalation");
        }
        if (score >= 150 || sanctionsSignal) {
            return new RiskBand("HIGH", "needs investigation");
        }
        if (score >= 50) {
            return new RiskBand("MEDIUM", "worth monitoring");
        }
        return new RiskBand("LOW", "routine");
    }

    private static LlmAnalysisResponse.Finding finding(TriggeredRuleDto rule, ActivityDigest digest) {
        List<ActivityDigest.NotableTransaction> examples = digest.notableTransactions().stream()
            .filter(t -> t.ruleNames().contains(rule.ruleName()))
            .limit(3)
            .toList();

        StringBuilder detail = new StringBuilder();
        detail.append("Rule '").append(rule.ruleName()).append("' fired ")
            .append(rule.timesTriggered())
            .append(rule.timesTriggered() == 1 ? " time" : " times")
            .append(", contributing ").append(rule.totalContribution())
            .append(" points to the customer's risk score.");
        if (!examples.isEmpty()) {
            detail.append(" Example activity: ");
            detail.append(String.join("; ", examples.stream()
                .map(t -> t.amount() + " " + t.currency() + " " + t.type() + " on "
                    + t.at().toString().substring(0, 10) + " (" + t.descriptor() + ")")
                .toList()));
            detail.append('.');
        }

        double weightPerHit = rule.totalContribution().doubleValue() / rule.timesTriggered();
        String severity = weightPerHit >= 40 ? "HIGH" : weightPerHit >= 25 ? "MEDIUM" : "LOW";

        return new LlmAnalysisResponse.Finding(
            rule.ruleName(), severity, detail.toString(),
            examples.stream().map(t -> t.id().toString()).toList());
    }

    private static String summary(ActivityDigest digest, RiskBand band) {
        StringBuilder sb = new StringBuilder();
        sb.append("Customer ").append(digest.customer().customerNumber())
            .append(" shows ").append(digest.transactionCount())
            .append(" transactions in the review window");
        if (!digest.byType().isEmpty()) {
            sb.append(" (");
            sb.append(String.join(", ", digest.byType().stream()
                .map(t -> t.count() + " " + t.activityType().toLowerCase(Locale.ROOT)).toList()));
            sb.append(')');
        }
        sb.append(", with a cumulative rule-based risk score of ").append(digest.riskScore()).append(". ");

        if (digest.triggeredRules().isEmpty()) {
            sb.append("No monitoring rules fired; the activity pattern is consistent with ")
                .append("routine personal account usage. ");
        } else {
            sb.append("The score is driven mainly by: ");
            sb.append(String.join("; ", digest.triggeredRules().stream()
                .limit(3)
                .map(r -> r.ruleName() + " (" + r.timesTriggered() + "x)").toList()));
            sb.append(". ");
        }
        sb.append("Overall assessment: ").append(band.level()).append(" — ").append(band.label()).append('.');
        return sb.toString();
    }

    private static List<String> recommendations(ActivityDigest digest, RiskBand band) {
        List<String> recs = new ArrayList<>();
        Set<String> ruleNames = new LinkedHashSet<>(digest.triggeredRules().stream()
            .map(r -> r.ruleName().toLowerCase(Locale.ROOT)).toList());

        switch (band.level()) {
            case "CRITICAL" -> {
                recs.add("Escalate to the AML/compliance team within 24 hours per the escalation policy.");
                recs.add("Consider temporary restrictions on outgoing transfers pending the compliance review.");
                recs.add("Prepare a case file with the flagged transactions for a possible MROS suspicious-activity report.");
            }
            case "HIGH" -> {
                recs.add("Escalate to the AML/compliance team for investigation.");
                recs.add("Contact the customer to obtain source-of-funds documentation for the flagged transfers.");
                recs.add("Schedule an enhanced due diligence (EDD) refresh of the customer profile.");
            }
            case "MEDIUM" -> {
                recs.add("Keep the customer under heightened monitoring for the next 30 days.");
                recs.add("Verify that recent activity matches the customer's stated profile at the next review.");
            }
            default -> recs.add("No action required beyond routine monitoring.");
        }

        if (ruleNames.stream().anyMatch(n -> n.contains("declines"))) {
            recs.add("Advise the customer about the repeated card declines — possible compromised card or limit issue.");
        }
        if (ruleNames.stream().anyMatch(n -> n.contains("merchant category"))) {
            recs.add("Review gambling-related spending against affordability and responsible-gaming guidance.");
        }
        if (ruleNames.stream().anyMatch(n -> n.contains("unhosted") || n.contains("mixer"))) {
            recs.add("Request proof of ownership for the destination wallet(s) and travel-rule counterparty information.");
        }
        return recs;
    }

    /** Cites the retrieved chunks that share the most vocabulary with the triggered rules. */
    private static List<String> citedChunks(ActivityDigest digest, List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return List.of();
        }
        if (digest.triggeredRules().isEmpty()) {
            return List.of(chunks.get(0).chunkId().toString());
        }
        Set<String> ruleWords = new LinkedHashSet<>();
        for (TriggeredRuleDto rule : digest.triggeredRules()) {
            for (String word : rule.ruleName().toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
                if (word.length() > 3) {
                    ruleWords.add(word);
                }
            }
        }
        return chunks.stream()
            .sorted((a, b) -> Integer.compare(overlap(b, ruleWords), overlap(a, ruleWords)))
            .limit(3)
            .map(c -> c.chunkId().toString())
            .toList();
    }

    private static int overlap(RetrievedChunk chunk, Set<String> words) {
        String content = chunk.content().toLowerCase(Locale.ROOT);
        return (int) words.stream().filter(content::contains).count();
    }
}
