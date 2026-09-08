package com.swissquote.caa.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caa")
public record AppProperties(Jwt jwt, Llm llm, Rag rag, Analysis analysis) {

    public record Jwt(String secret, Duration ttl) {
    }

    public record Llm(String provider, Anthropic anthropic) {

        public record Anthropic(String apiKey, String baseUrl, String model, int maxTokens, boolean thinking) {
        }
    }

    public record Rag(int topK, int embeddingDim) {
    }

    public record Analysis(int maxPromptTransactions) {
    }
}
