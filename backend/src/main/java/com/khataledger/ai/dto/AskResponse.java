package com.khataledger.ai.dto;

import java.util.List;
import java.util.Map;

public record AskResponse(
        String answer,
        String intent,            // e.g. "TOP_DEBTORS", "RECENT_TXNS", "TOTAL_OUTSTANDING"
        List<Map<String, Object>> data, // tabular result the LLM consumed
        boolean usedLlm           // false when the stub LLM handled it (no GEMINI_API_KEY)
) {}
