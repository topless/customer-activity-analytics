package com.swissquote.caa.analysis.llm;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The JSON shape every adapter (stub or real model) must produce. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmAnalysisResponse(
    String riskLevel,
    String summary,
    List<Finding> findings,
    List<String> recommendations,
    List<String> citedPolicyChunkIds) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Finding(String title, String severity, String detail,
                          List<String> relatedTransactionIds) {
    }
}
