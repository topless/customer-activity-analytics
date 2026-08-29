package com.swissquote.caa.analysis.llm;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swissquote.caa.analysis.TestDigests;
import com.swissquote.caa.analysis.llm.LlmClient.LlmRequest;
import static org.assertj.core.api.Assertions.assertThat;

class StubLlmClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StubLlmClient stub = new StubLlmClient(objectMapper);

    @Test
    void quietCustomerIsLowRiskWithNoFindings() throws Exception {
        String raw = stub.complete(request(TestDigests.quietCustomer()));
        LlmAnalysisResponse parsed = objectMapper.readValue(raw, LlmAnalysisResponse.class);

        assertThat(parsed.riskLevel()).isEqualTo("LOW");
        assertThat(parsed.findings()).isEmpty();
        assertThat(parsed.summary()).contains("CUST-90001");
        assertThat(parsed.recommendations()).isNotEmpty();
    }

    @Test
    void highRiskCustomerEscalatesToCritical() throws Exception {
        String raw = stub.complete(request(TestDigests.highRiskCustomer()));
        LlmAnalysisResponse parsed = objectMapper.readValue(raw, LlmAnalysisResponse.class);

        assertThat(parsed.riskLevel()).isEqualTo("CRITICAL");
        assertThat(parsed.findings()).isNotEmpty();
        assertThat(parsed.findings().get(0).relatedTransactionIds()).isNotEmpty();
        // sanctions-adjacent rule must surface as a HIGH severity finding
        assertThat(parsed.findings())
            .anyMatch(f -> f.title().contains("high-risk jurisdiction") && f.severity().equals("HIGH"));
    }

    @Test
    void onlyRetrievedChunksAreCited() throws Exception {
        String raw = stub.complete(request(TestDigests.highRiskCustomer()));
        LlmAnalysisResponse parsed = objectMapper.readValue(raw, LlmAnalysisResponse.class);

        List<String> retrievedIds = TestDigests.chunks().stream()
            .map(c -> c.chunkId().toString()).toList();
        assertThat(parsed.citedPolicyChunkIds()).isNotEmpty();
        assertThat(retrievedIds).containsAll(parsed.citedPolicyChunkIds());
    }

    private static LlmRequest request(com.swissquote.caa.analysis.ActivityDigest digest) {
        return new LlmRequest("system", "user", digest, TestDigests.chunks());
    }
}
