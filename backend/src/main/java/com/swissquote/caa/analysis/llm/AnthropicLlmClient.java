package com.swissquote.caa.analysis.llm;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.swissquote.caa.config.AppProperties;

/**
 * Adapter for the Anthropic Messages API (POST /v1/messages). Activated when an API key is
 * configured; see {@link LlmClientConfig}.
 */
public class AnthropicLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmClient.class);
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final AppProperties.Llm.Anthropic config;

    public AnthropicLlmClient(AppProperties properties) {
        this.config = properties.llm().anthropic();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(120_000);
        this.restClient = RestClient.builder()
            .baseUrl(config.baseUrl())
            .requestFactory(requestFactory)
            .defaultHeader("x-api-key", config.apiKey())
            .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
            .build();
    }

    @Override
    public String modelId() {
        return config.model();
    }

    @Override
    public String complete(LlmRequest request) {
        MessagesRequest body = new MessagesRequest(
            config.model(),
            config.maxTokens(),
            request.systemPrompt(),
            List.of(new Message("user", request.userPrompt())));
        try {
            MessagesResponse response = restClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(MessagesResponse.class);
            if (response == null || response.content() == null || response.content().isEmpty()
                || response.content().get(0).text() == null) {
                throw new LlmException("Anthropic API returned an empty response");
            }
            log.debug("Anthropic call ok: model={}, stop_reason={}", response.model(), response.stopReason());
            return response.content().get(0).text();
        } catch (RestClientResponseException e) {
            throw new LlmException("Anthropic API error (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (ResourceAccessException e) {
            throw new LlmException("Anthropic API unreachable: " + e.getMessage(), e);
        }
    }

    record MessagesRequest(String model,
                           @JsonProperty("max_tokens") int maxTokens,
                           String system,
                           List<Message> messages) {
    }

    record Message(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MessagesResponse(String model,
                            @JsonProperty("stop_reason") String stopReason,
                            List<ContentBlock> content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContentBlock(String type, String text) {
    }
}
