package com.swissquote.caa.analysis;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swissquote.caa.analysis.AnalysisDtos.AnalysisDetailDto;
import com.swissquote.caa.analysis.llm.LlmClient;
import com.swissquote.caa.analysis.llm.StubLlmClient;
import com.swissquote.caa.auth.Operator;
import com.swissquote.caa.auth.OperatorRepository;
import com.swissquote.caa.customer.Customer;
import com.swissquote.caa.customer.CustomerService;
import com.swissquote.caa.rag.PolicyRetrievalService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID OPERATOR_ID = UUID.randomUUID();

    @Mock
    private CustomerService customerService;
    @Mock
    private DigestBuilder digestBuilder;
    @Mock
    private PolicyRetrievalService retrievalService;
    @Mock
    private LlmClient llmClient;
    @Mock
    private AiAnalysisRepository analyses;
    @Mock
    private OperatorRepository operators;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AnalysisService service;

    @BeforeEach
    void setUp() {
        service = new AnalysisService(customerService, digestBuilder, retrievalService,
            new PromptBuilder(), llmClient, analyses, operators, objectMapper);

        Customer customer = new Customer(CUSTOMER_ID, "CUST-90002", "Lukas Weber",
            "x@example.ch", "CH", null, "ENHANCED", Instant.parse("2021-01-01T00:00:00Z"));
        when(customerService.require(CUSTOMER_ID)).thenReturn(customer);
        when(digestBuilder.build(customer)).thenReturn(TestDigests.highRiskCustomer());
        when(retrievalService.retrieve(anyString())).thenReturn(TestDigests.chunks());
        when(operators.findById(OPERATOR_ID)).thenReturn(Optional.of(
            new Operator(OPERATOR_ID, "alice", "Alice Meier", "hash", "OPERATOR")));
        when(analyses.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void completedRunPersistsParsedResultWithAudit() {
        StubLlmClient stub = new StubLlmClient(objectMapper);
        when(llmClient.modelId()).thenReturn(stub.modelId());
        when(llmClient.complete(any())).thenAnswer(inv -> stub.complete(inv.getArgument(0)));

        AnalysisDetailDto result = service.run(CUSTOMER_ID, OPERATOR_ID);

        assertThat(result.status()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(result.findings()).isNotEmpty();
        assertThat(result.citedPolicies())
            .allMatch(c -> c.documentTitle() != null && c.excerpt() != null);
        assertThat(result.requestedBy().username()).isEqualTo("alice");

        ArgumentCaptor<AiAnalysis> saved = ArgumentCaptor.forClass(AiAnalysis.class);
        verify(analyses).save(saved.capture());
        assertThat(saved.getValue().getPrompt()).contains("CUST-90002");
        assertThat(saved.getValue().getRawResponse()).contains("riskLevel");
        // pseudonymisation: the prompt must never contain direct identifiers
        assertThat(saved.getValue().getPrompt()).doesNotContain("Lukas Weber");
        assertThat(saved.getValue().getPrompt()).doesNotContain("x@example.ch");
    }

    @Test
    void malformedModelOutputIsPersistedAsFailedRun() {
        when(llmClient.modelId()).thenReturn("broken-model");
        when(llmClient.complete(any())).thenReturn("I'm sorry, I cannot produce JSON today.");

        AnalysisDetailDto result = service.run(CUSTOMER_ID, OPERATOR_ID);

        assertThat(result.status()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(result.riskLevel()).isNull();
        assertThat(result.errorMessage()).isNotBlank();
        verify(analyses).save(any());
    }

    @Test
    void invalidRiskLevelFromModelFailsTheRun() {
        when(llmClient.modelId()).thenReturn("broken-model");
        when(llmClient.complete(any()))
            .thenReturn("{\"riskLevel\":\"BANANAS\",\"summary\":\"s\",\"findings\":[],"
                + "\"recommendations\":[],\"citedPolicyChunkIds\":[]}");

        AnalysisDetailDto result = service.run(CUSTOMER_ID, OPERATOR_ID);

        assertThat(result.status()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(result.errorMessage()).contains("riskLevel");
    }

    @Test
    void citationsOutsideRetrievedChunksAreDropped() {
        when(llmClient.modelId()).thenReturn("model");
        when(llmClient.complete(any())).thenReturn(
            "{\"riskLevel\":\"HIGH\",\"summary\":\"s\",\"findings\":[],\"recommendations\":[],"
                + "\"citedPolicyChunkIds\":[\"" + TestDigests.CHUNK_1 + "\",\""
                + UUID.randomUUID() + "\",\"not-a-uuid\"]}");

        AnalysisDetailDto result = service.run(CUSTOMER_ID, OPERATOR_ID);

        assertThat(result.status()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(result.citedPolicies()).hasSize(1);
        assertThat(result.citedPolicies().get(0).chunkId()).isEqualTo(TestDigests.CHUNK_1);
    }
}
