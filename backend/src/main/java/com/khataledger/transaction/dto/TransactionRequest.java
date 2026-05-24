package com.khataledger.transaction.dto;

import com.khataledger.transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionRequest(
        @NotNull TransactionType type,
        @NotNull @DecimalMin(value = "0.01", message = "amount must be > 0") BigDecimal amount,
        @Size(max = 255) String note,
        OffsetDateTime occurredAt
) {}
