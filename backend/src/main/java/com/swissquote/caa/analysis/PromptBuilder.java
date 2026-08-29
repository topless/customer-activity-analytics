package com.swissquote.caa.analysis;

import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;
import com.swissquote.caa.rag.RetrievedChunk;

/** Renders the system and user prompts sent to the analysis model. */
@Component
public class PromptBuilder {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneOffset.UTC);

    public String systemPrompt() {
        return """
            You are a financial-crime and fraud risk analyst supporting customer-care operators \
            at a financial services company. You review a customer's card, payment and \
            cryptocurrency activity together with the risk-rule signals the monitoring system \
            has already produced, and internal policy excerpts retrieved for this case.

            Ground rules:
            - Base findings strictly on the activity data provided; never invent transactions.
            - Use the policy excerpts to justify findings and recommendations; cite them via \
            their chunk id. Only cite chunk ids that appear in the provided excerpts.
            - Reference transactions by their full transaction id in relatedTransactionIds.
            - Be specific and factual; quantify patterns (counts, amounts, time windows).
            - Risk level reflects the overall picture: LOW (routine), MEDIUM (worth monitoring), \
            HIGH (needs investigation), CRITICAL (immediate escalation).

            Respond with a SINGLE JSON object, no markdown fences, no prose outside JSON, \
            in exactly this shape:
            {
              "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
              "summary": "2-4 sentence narrative for the operator",
              "findings": [
                {
                  "title": "short finding title",
                  "severity": "LOW|MEDIUM|HIGH",
                  "detail": "specific, quantified explanation",
                  "relatedTransactionIds": ["uuid", "..."]
                }
              ],
              "recommendations": ["actionable next step", "..."],
              "citedPolicyChunkIds": ["uuid", "..."]
            }
            """;
    }

    public String userPrompt(ActivityDigest digest, List<RetrievedChunk> chunks) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Customer\n");
        sb.append("- Reference: ").append(digest.customer().customerNumber()).append('\n');
        sb.append("- Residence country: ").append(digest.customer().country()).append('\n');
        sb.append("- KYC level: ").append(digest.customer().kycLevel()).append('\n');
        sb.append("- Onboarded: ").append(DATE.format(digest.customer().onboardedAt())).append("\n\n");

        sb.append("# Activity overview\n");
        if (digest.transactionCount() == 0) {
            sb.append("No transactions on record.\n\n");
        } else {
            sb.append("- Window: ").append(DATE.format(digest.windowFrom()))
                .append(" to ").append(DATE.format(digest.windowTo()))
                .append(" (").append(digest.transactionCount()).append(" transactions)\n");
            sb.append("- Total rule-based risk score: ").append(digest.riskScore()).append('\n');
            for (var type : digest.byType()) {
                sb.append("- ").append(type.activityType()).append(": ")
                    .append(type.count()).append(" transactions");
                if (type.failedCount() > 0) {
                    sb.append(" (").append(type.failedCount()).append(" failed)");
                }
                sb.append(", volume ");
                sb.append(String.join(", ", type.totalsByCurrency().stream()
                    .map(t -> t.totalAmount() + " " + t.currency()).toList()));
                sb.append('\n');
            }
            sb.append('\n');
        }

        sb.append("# Risk-rule signals\n");
        if (digest.triggeredRules().isEmpty()) {
            sb.append("No monitoring rules triggered.\n\n");
        } else {
            for (var rule : digest.triggeredRules()) {
                sb.append("- ").append(rule.ruleName()).append(" (").append(rule.appliesTo())
                    .append("): triggered ").append(rule.timesTriggered())
                    .append("x, total contribution ").append(rule.totalContribution())
                    .append(", last ").append(DATE.format(rule.lastTriggeredAt())).append('\n');
            }
            sb.append('\n');
        }

        sb.append("# Notable transactions (highest risk contribution first, then largest)\n");
        for (var t : digest.notableTransactions()) {
            sb.append("- [").append(t.id()).append("] ")
                .append(DATE_TIME.format(t.at())).append(" | ")
                .append(t.type()).append(' ')
                .append(t.amount()).append(' ').append(t.currency())
                .append(" | ").append(t.status())
                .append(" | ").append(t.descriptor());
            if (t.riskScore().signum() > 0) {
                sb.append(" | risk +").append(t.riskScore())
                    .append(" (").append(String.join("; ", t.ruleNames())).append(')');
            }
            sb.append('\n');
        }
        sb.append('\n');

        sb.append("# Retrieved policy excerpts\n");
        for (RetrievedChunk chunk : chunks) {
            sb.append("## chunk ").append(chunk.chunkId())
                .append(" — ").append(chunk.documentTitle());
            if (chunk.sectionTitle() != null) {
                sb.append(" / ").append(chunk.sectionTitle());
            }
            sb.append('\n').append(chunk.content()).append("\n\n");
        }

        sb.append("Analyse this customer's activity now and respond with the JSON object.");
        return sb.toString();
    }
}
