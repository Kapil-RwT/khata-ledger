package com.khataledger.transaction.dto;

import com.khataledger.transaction.Transaction;
import com.khataledger.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionResponse(
        Long id,
        Long customerId,
        TransactionType type,
        BigDecimal amount,
        String note,
        OffsetDateTime occurredAt,
        OffsetDateTime createdAt
) {
    public static TransactionResponse of(Transaction t) {
        return new TransactionResponse(
                t.getId(), t.getCustomerId(), t.getType(),
                t.getAmount(), t.getNote(), t.getOccurredAt(), t.getCreatedAt());
    }
}
