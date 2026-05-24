package com.khataledger.ai;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keyword-based fallback router used when the Gemini API key isn't configured.
 * Intentionally simple: lowercases the question and looks for trigger phrases.
 * The point of having both is the Strategy/Decorator-style design talking point —
 * AskService asks Gemini first, falls back to this router on failure or when disabled.
 */
@Component
public class LocalIntentRouter {

    private static final Pattern LIMIT = Pattern.compile("(?:top|first|last)?\\s*(\\d{1,3})");

    public GeminiClient.IntentDecision classify(String question) {
        String q = question.toLowerCase();
        int limit = parseLimit(q);

        if (q.contains("owe") || q.contains("debt") || q.contains("udhaar")
                || (q.contains("top") && q.contains("customer"))
                || q.contains("most")) {
            return new GeminiClient.IntentDecision(QueryIntent.TOP_DEBTORS, Map.of("limit", limit));
        }
        if (q.contains("total") || q.contains("overall") || q.contains("receivable")) {
            return new GeminiClient.IntentDecision(QueryIntent.TOTAL_OUTSTANDING, Map.of());
        }
        if (q.contains("recent") || q.contains("last") || q.contains("latest") || q.contains("transaction")) {
            return new GeminiClient.IntentDecision(QueryIntent.RECENT_TRANSACTIONS, Map.of("limit", limit));
        }
        if (q.contains("overdue") || q.contains("not paid") || q.contains("pending")) {
            return new GeminiClient.IntentDecision(QueryIntent.OVERDUE_LIST, Map.of());
        }
        return new GeminiClient.IntentDecision(QueryIntent.UNKNOWN, Map.of());
    }

    private int parseLimit(String q) {
        Matcher m = LIMIT.matcher(q);
        if (m.find()) {
            try { return Math.min(50, Math.max(1, Integer.parseInt(m.group(1)))); }
            catch (NumberFormatException ignored) {}
        }
        return 5;
    }
}
