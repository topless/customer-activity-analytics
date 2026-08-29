package com.swissquote.caa;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack journey against a real pgvector Postgres (Testcontainers): migrations + seed
 * data + policy ingestion at startup, then login -> search -> overview -> transactions ->
 * AI analysis (stub) -> history. Requires Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "caa.llm.provider=stub")
@Import(TestcontainersConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerJourneyIT {

    private static final String HIGH_RISK_CUSTOMER_ID = "c0000000-0000-0000-0000-000000000004";

    @Autowired
    private TestRestTemplate rest;

    @Test
    @Order(1)
    void unauthenticatedRequestsAreRejected() {
        assertThat(rest.getForEntity("/api/customers", String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(2)
    void wrongCredentialsAreRejected() {
        ResponseEntity<String> response = rest.postForEntity("/api/auth/login",
            Map.of("username", "alice", "password", "wrong"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(3)
    void operatorJourney_searchOverviewTransactionsAnalysis() {
        String token = login();

        // search by name fragment
        JsonNode search = get(token, "/api/customers?query=weber");
        assertThat(search.get("totalElements").asLong()).isEqualTo(1);
        JsonNode found = search.get("content").get(0);
        assertThat(found.get("customerNumber").asText()).isEqualTo("CUST-10004");
        assertThat(found.get("riskScore").asDouble()).isGreaterThan(400);

        // overview aggregates
        JsonNode overview = get(token, "/api/customers/" + HIGH_RISK_CUSTOMER_ID + "/overview");
        assertThat(overview.get("transactionCount").asLong()).isGreaterThan(30);
        assertThat(overview.get("triggeredRules").size()).isGreaterThanOrEqualTo(4);
        assertThat(overview.get("monthlyCounts").size()).isGreaterThanOrEqualTo(6);

        // filtered transactions carry type-specific details and rule annotations
        JsonNode payments = get(token,
            "/api/customers/" + HIGH_RISK_CUSTOMER_ID + "/transactions?type=PAYMENT&size=50");
        assertThat(payments.get("content").size()).isGreaterThan(5);
        for (JsonNode tx : payments.get("content")) {
            assertThat(tx.get("activityType").asText()).isEqualTo("PAYMENT");
            assertThat(tx.get("payment").isObject()).isTrue();
            assertThat(tx.get("card").isNull()).isTrue();
        }

        // run an AI analysis (stub provider) and read it back
        ResponseEntity<JsonNode> created = rest.exchange("/api/customers/"
                + HIGH_RISK_CUSTOMER_ID + "/analyses", HttpMethod.POST,
            new HttpEntity<>(null, bearer(token)), JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode analysis = created.getBody();
        assertThat(analysis.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(analysis.get("riskLevel").asText()).isEqualTo("CRITICAL");
        assertThat(analysis.get("findings").size()).isGreaterThan(0);
        assertThat(analysis.get("citedPolicies").size()).isGreaterThan(0);
        assertThat(analysis.get("requestedBy").get("username").asText()).isEqualTo("alice");

        // persisted and listed (spec #5)
        JsonNode history = get(token, "/api/customers/" + HIGH_RISK_CUSTOMER_ID + "/analyses");
        assertThat(history.isArray()).isTrue();
        assertThat(history.size()).isGreaterThanOrEqualTo(1);
        String analysisId = analysis.get("id").asText();
        JsonNode reloaded = get(token, "/api/analyses/" + analysisId);
        assertThat(reloaded.get("riskLevel").asText()).isEqualTo("CRITICAL");
        assertThat(reloaded.get("summary").asText()).contains("CUST-10004");
    }

    @Test
    @Order(4)
    void unknownCustomerIs404WithProblemDetail() {
        String token = login();
        ResponseEntity<JsonNode> response = rest.exchange(
            "/api/customers/00000000-0000-0000-0000-00000000dead", HttpMethod.GET,
            new HttpEntity<>(null, bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("detail").asText()).contains("not found");
    }

    private String login() {
        ResponseEntity<JsonNode> response = rest.postForEntity("/api/auth/login",
            Map.of("username", "alice", "password", "operator123"), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().get("token").asText();
    }

    private JsonNode get(String token, String url) {
        ResponseEntity<JsonNode> response = rest.exchange(url, HttpMethod.GET,
            new HttpEntity<>(null, bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
