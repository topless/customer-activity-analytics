package com.swissquote.caa.analysis;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.swissquote.caa.auth.OperatorDto;

public final class AnalysisDtos {

    private AnalysisDtos() {
    }

    public record FindingDto(String title, String severity, String detail,
                             List<UUID> relatedTransactionIds) {
    }

    public record CitedPolicyDto(UUID chunkId, String documentTitle, String sectionTitle,
                                 String excerpt) {
    }

    public record AnalysisDetailDto(UUID id, UUID customerId, OperatorDto requestedBy,
                                    Instant requestedAt, Instant completedAt, AnalysisStatus status,
                                    String model, RiskLevel riskLevel, String summary,
                                    List<FindingDto> findings, List<String> recommendations,
                                    List<CitedPolicyDto> citedPolicies,
                                    Instant activityWindowFrom, Instant activityWindowTo,
                                    Integer transactionCount, String errorMessage) {
    }

    public record AnalysisSummaryDto(UUID id, UUID customerId, OperatorDto requestedBy,
                                     Instant requestedAt, Instant completedAt, AnalysisStatus status,
                                     String model, RiskLevel riskLevel, String summary,
                                     Integer transactionCount, String errorMessage) {
    }
}
