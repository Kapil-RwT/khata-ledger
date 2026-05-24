package com.khataledger.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thin Gemini REST client. We intentionally avoid the official SDK to keep the dependency
 * footprint small and the call easy to inspect.
 *
 * If GEMINI_API_KEY is blank, this client is "disabled" and returns Optional.empty(),
 * which lets AskService fall back to the local keyword router. That means you can run
 * the project end-to-end with zero external dependencies, then plug a key in for the
 * "wow" demo at interview time.
 */
@Slf4j
@Component
public class GeminiClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper mapper = new ObjectMapper();

    public GeminiClient(@Value("${app.ai.gemini.api-key}") String apiKey,
                        @Value("${app.ai.gemini.model}") String model,
                        @Value("${app.ai.gemini.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Asks Gemini to map a free-text question onto one of our QueryIntent values.
     * Returns the chosen intent and a normalized params map, e.g. {"limit": 3}.
     */
    public Optional<IntentDecision> classify(String question) {
        if (!isEnabled()) return Optional.empty();

        String system = """
                You are a router for a small business ledger app. The user asks a question
                in any language; you classify it into one of these intents and return STRICT JSON:
                  TOP_DEBTORS           - "who owes me the most", "top customers by outstanding"
                  TOTAL_OUTSTANDING     - "how much money is owed to me overall"
                  RECENT_TRANSACTIONS   - "show me recent transactions", "last 5 entries"
                  OVERDUE_LIST          - "who hasn't paid in a long time", "overdue customers"
                  UNKNOWN               - anything else
                Output JSON shape: {"intent":"<INTENT>","params":{"limit":<int optional>}}
                Do not include any text outside the JSON.
                """;
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("text", system),
                                Map.of("text", "Question: " + question)
                        )
                )),
                "generationConfig", Map.of(
                        "temperature", 0,
                        "responseMimeType", "application/json"
                )
        );

        String path = "/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        try {
            String raw = webClient.post()
                    .uri(path)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(8))
                    .block();
            JsonNode root = mapper.readTree(raw);
            String text = root.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText();
            JsonNode parsed = mapper.readTree(text);
            QueryIntent intent = QueryIntent.valueOf(parsed.path("intent").asText("UNKNOWN"));
            Map<String, Object> params = mapper.convertValue(parsed.path("params"), Map.class);
            if (params == null) params = Map.of();
            return Optional.of(new IntentDecision(intent, params));
        } catch (Exception e) {
            log.warn("Gemini classification failed, falling back to local router: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public record IntentDecision(QueryIntent intent, Map<String, Object> params) {}
}
