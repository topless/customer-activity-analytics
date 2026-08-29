package com.swissquote.caa.analysis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swissquote.caa.analysis.AnalysisDtos.AnalysisDetailDto;
import com.swissquote.caa.analysis.AnalysisDtos.AnalysisSummaryDto;
import com.swissquote.caa.analysis.AnalysisDtos.CitedPolicyDto;
import com.swissquote.caa.analysis.AnalysisDtos.FindingDto;
import com.swissquote.caa.analysis.llm.LlmAnalysisResponse;
import com.swissquote.caa.analysis.llm.LlmClient;
import com.swissquote.caa.analysis.llm.LlmException;
import com.swissquote.caa.auth.Operator;
import com.swissquote.caa.auth.OperatorDto;
import com.swissquote.caa.auth.OperatorRepository;
import com.swissquote.caa.common.NotFoundException;
import com.swissquote.caa.customer.Customer;
import com.swissquote.caa.customer.CustomerService;
import com.swissquote.caa.rag.PolicyRetrievalService;
import com.swissquote.caa.rag.RetrievedChunk;

/**
 * Orchestrates one AI analysis run: digest the activity, retrieve policy context (RAG),
 * prompt the model, parse and validate its JSON, and persist the result — including failed
 * attempts, so every run is auditable (spec #5).
 */
@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    private static final int EXCERPT_CHARS = 300;

    private final CustomerService customerService;
    private final DigestBuilder digestBuilder;
    private final PolicyRetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final AiAnalysisRepository analyses;
    private final OperatorRepository operators;
    private final ObjectMapper objectMapper;

    public AnalysisService(CustomerService customerService, DigestBuilder digestBuilder,
                           PolicyRetrievalService retrievalService, PromptBuilder promptBuilder,
                           LlmClient llmClient, AiAnalysisRepository analyses,
                           OperatorRepository operators, ObjectMapper objectMapper) {
        this.customerService = customerService;
        this.digestBuilder = digestBuilder;
        this.retrievalService = retrievalService;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.analyses = analyses;
        this.operators = operators;
        this.objectMapper = objectMapper;
    }

    // No surrounding transaction: the LLM call can take tens of seconds, and the digest and
    // final save manage their own (short) transactions.
    public AnalysisDetailDto run(UUID customerId, UUID operatorId) {
        Customer customer = customerService.require(customerId);
        ActivityDigest digest = digestBuilder.build(customer);
        List<RetrievedChunk> chunks = retrievalService.retrieve(ragQuery(digest));

        String systemPrompt = promptBuilder.systemPrompt();
        String userPrompt = promptBuilder.userPrompt(digest, chunks);

        AiAnalysis analysis = new AiAnalysis(UUID.randomUUID(), customerId, operatorId, Instant.now());
        analysis.setActivityWindow(digest.windowFrom(), digest.windowTo(),
            (int) digest.transactionCount());

        String rawResponse = null;
        try {
            rawResponse = llmClient.complete(
                new LlmClient.LlmRequest(systemPrompt, userPrompt, digest, chunks));
            LlmAnalysisResponse parsed = parse(rawResponse);

            List<FindingDto> findings = mapFindings(parsed);
            List<String> recommendations = parsed.recommendations() == null
                ? List.of() : parsed.recommendations();
            List<CitedPolicyDto> cited = mapCitedPolicies(parsed, chunks);

            analysis.markCompleted(Instant.now(), llmClient.modelId(),
                riskLevel(parsed.riskLevel()), parsed.summary(),
                toJson(findings), toJson(recommendations), toJson(cited));
        } catch (Exception e) {
            log.error("AI analysis failed for customer {}", customerId, e);
            analysis.markFailed(Instant.now(), llmClient.modelId(), e.getMessage());
        }
        analysis.setAudit(systemPrompt + "\n\n----- USER PROMPT -----\n\n" + userPrompt, rawResponse);
        analyses.save(analysis);
        return toDetail(analysis);
    }

    public List<AnalysisSummaryDto> list(UUID customerId) {
        customerService.require(customerId);
        List<AiAnalysis> items = analyses.findByCustomerIdOrderByRequestedAtDesc(customerId);
        Map<UUID, Operator> operatorById = operators
            .findAllById(items.stream().map(AiAnalysis::getRequestedBy).collect(Collectors.toSet()))
            .stream().collect(Collectors.toMap(Operator::getId, Function.identity()));
        return items.stream()
            .map(a -> new AnalysisSummaryDto(a.getId(), a.getCustomerId(),
                operatorDto(operatorById.get(a.getRequestedBy())),
                a.getRequestedAt(), a.getCompletedAt(), a.getStatus(), a.getModel(),
                a.getRiskLevel(), a.getSummary(), a.getTransactionCount(), a.getErrorMessage()))
            .toList();
    }

    public AnalysisDetailDto get(UUID analysisId) {
        AiAnalysis analysis = analyses.findById(analysisId)
            .orElseThrow(() -> new NotFoundException("Analysis " + analysisId + " not found"));
        return toDetail(analysis);
    }

    /** The retrieval query is derived from the data: rule names plus activity-type vocabulary. */
    private static String ragQuery(ActivityDigest digest) {
        Set<String> parts = new LinkedHashSet<>();
        for (var rule : digest.triggeredRules()) {
            parts.add(rule.ruleName());
        }
        for (var type : digest.byType()) {
            switch (type.activityType()) {
                case "CARD" -> parts.add("card transactions merchant declines fraud");
                case "PAYMENT" -> parts.add("payments wire transfer cross-border beneficiary");
                case "CRYPTO" -> parts.add("cryptocurrency wallet exchange transfers");
                default -> { }
            }
        }
        if (digest.triggeredRules().isEmpty()) {
            parts.add("routine transaction monitoring baseline low risk customer review");
        }
        return String.join(" ", parts);
    }

    private LlmAnalysisResponse parse(String raw) {
        String json = raw.strip();
        // tolerate markdown fences or stray prose around the JSON object
        int first = json.indexOf('{');
        int last = json.lastIndexOf('}');
        if (first < 0 || last <= first) {
            throw new LlmException("Model response contains no JSON object");
        }
        try {
            return objectMapper.readValue(json.substring(first, last + 1), LlmAnalysisResponse.class);
        } catch (Exception e) {
            throw new LlmException("Model response is not valid JSON: " + e.getMessage(), e);
        }
    }

    private static RiskLevel riskLevel(String value) {
        if (value == null) {
            throw new LlmException("Model response is missing riskLevel");
        }
        try {
            return RiskLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new LlmException("Model returned invalid riskLevel: " + value);
        }
    }

    private static List<FindingDto> mapFindings(LlmAnalysisResponse parsed) {
        if (parsed.findings() == null) {
            return List.of();
        }
        return parsed.findings().stream()
            .map(f -> new FindingDto(f.title(), normaliseSeverity(f.severity()), f.detail(),
                parseUuids(f.relatedTransactionIds())))
            .toList();
    }

    private static String normaliseSeverity(String severity) {
        String s = severity == null ? "" : severity.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "LOW", "MEDIUM", "HIGH" -> s;
            default -> "MEDIUM";
        };
    }

    private static List<UUID> parseUuids(List<String> values) {
        List<UUID> ids = new ArrayList<>();
        for (String value : values == null ? List.<String>of() : values) {
            try {
                ids.add(UUID.fromString(value.trim()));
            } catch (IllegalArgumentException ignored) {
                // model referenced something that is not a transaction id — drop it
            }
        }
        return ids;
    }

    /** Only chunks that were actually retrieved can be cited; unknown ids are dropped. */
    private static List<CitedPolicyDto> mapCitedPolicies(LlmAnalysisResponse parsed,
                                                         List<RetrievedChunk> chunks) {
        Map<UUID, RetrievedChunk> byId = chunks.stream()
            .collect(Collectors.toMap(RetrievedChunk::chunkId, Function.identity()));
        List<CitedPolicyDto> cited = new ArrayList<>();
        for (UUID id : parseUuids(parsed.citedPolicyChunkIds())) {
            RetrievedChunk chunk = byId.get(id);
            if (chunk != null) {
                String excerpt = chunk.content().length() <= EXCERPT_CHARS
                    ? chunk.content()
                    : chunk.content().substring(0, EXCERPT_CHARS) + "…";
                cited.add(new CitedPolicyDto(id, chunk.documentTitle(), chunk.sectionTitle(), excerpt));
            }
        }
        return cited;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialise analysis payload", e);
        }
    }

    private AnalysisDetailDto toDetail(AiAnalysis a) {
        return new AnalysisDetailDto(a.getId(), a.getCustomerId(),
            operatorDto(operators.findById(a.getRequestedBy()).orElse(null)),
            a.getRequestedAt(), a.getCompletedAt(), a.getStatus(), a.getModel(),
            a.getRiskLevel(), a.getSummary(),
            fromJson(a.getFindings(), new TypeReference<List<FindingDto>>() { }),
            fromJson(a.getRecommendations(), new TypeReference<List<String>>() { }),
            fromJson(a.getCitedPolicies(), new TypeReference<List<CitedPolicyDto>>() { }),
            a.getActivityWindowFrom(), a.getActivityWindowTo(), a.getTransactionCount(),
            a.getErrorMessage());
    }

    private <T> List<T> fromJson(String json, TypeReference<List<T>> type) {
        if (json == null) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("Corrupt analysis payload, returning empty list", e);
            return List.of();
        }
    }

    private static OperatorDto operatorDto(Operator operator) {
        return operator == null ? null : OperatorDto.of(operator);
    }
}
