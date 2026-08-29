package com.swissquote.caa.analysis.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swissquote.caa.config.AppProperties;

/**
 * Selects the LLM adapter: {@code caa.llm.provider} = stub | anthropic | auto (default —
 * anthropic when an API key is configured, stub otherwise, so the app always starts).
 */
@Configuration
public class LlmClientConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmClientConfig.class);

    @Bean
    LlmClient llmClient(AppProperties properties, ObjectMapper objectMapper) {
        String provider = properties.llm().provider();
        boolean hasApiKey = StringUtils.hasText(properties.llm().anthropic().apiKey());

        LlmClient client = switch (provider) {
            case "stub" -> new StubLlmClient(objectMapper);
            case "anthropic" -> {
                if (!hasApiKey) {
                    throw new IllegalStateException(
                        "caa.llm.provider=anthropic requires ANTHROPIC_API_KEY to be set");
                }
                yield new AnthropicLlmClient(properties);
            }
            case "auto" -> hasApiKey
                ? new AnthropicLlmClient(properties)
                : new StubLlmClient(objectMapper);
            default -> throw new IllegalStateException("Unknown caa.llm.provider: " + provider);
        };
        log.info("AI analysis provider: {} (model '{}')", client.getClass().getSimpleName(), client.modelId());
        return client;
    }
}
