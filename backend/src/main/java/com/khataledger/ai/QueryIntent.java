package com.khataledger.ai;

/**
 * The closed set of query intents the AI layer is allowed to map natural-language
 * questions onto. This is intentionally a small, safe whitelist:
 *  - the LLM never gets to write SQL
 *  - the LLM only chooses which named, parameterized query to run
 * That's the "LLM as router, not interpreter" pattern. Safer, cheaper, easier to test.
 */
public enum QueryIntent {
    TOP_DEBTORS,
    TOTAL_OUTSTANDING,
    RECENT_TRANSACTIONS,
    OVERDUE_LIST,
    UNKNOWN
}
