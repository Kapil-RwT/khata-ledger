package com.khataledger.ledger.dto;

import java.math.BigDecimal;

/**
 * receivables = total money customers owe the merchant (sum of positive balances)
 * payables    = total money the merchant owes customers (sum of negative balances, made positive)
 * overdueCount = number of customers with outstanding > 0 and no activity in the last N days
 */
public record DashboardResponse(
        BigDecimal receivables,
        BigDecimal payables,
        long customerCount,
        long overdueCount
) {}
