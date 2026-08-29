package com.swissquote.caa.analysis;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    void userPromptContainsActivityRulesAndPolicyChunks() {
        String prompt = promptBuilder.userPrompt(TestDigests.highRiskCustomer(), TestDigests.chunks());

        assertThat(prompt)
            .contains("CUST-90002")
            .contains("Structuring: repeated sub-threshold payments")
            .contains(TestDigests.TX_2.toString())
            .contains(TestDigests.CHUNK_1.toString())
            .contains("AML Transaction Monitoring Policy");
    }

    @Test
    void systemPromptDemandsJsonShape() {
        assertThat(promptBuilder.systemPrompt())
            .contains("riskLevel")
            .contains("citedPolicyChunkIds")
            .contains("SINGLE JSON object");
    }
}
