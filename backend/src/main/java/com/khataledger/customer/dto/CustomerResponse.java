package com.khataledger.customer.dto;

import com.khataledger.customer.Customer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CustomerResponse(
        Long id,
        String name,
        String phone,
        BigDecimal outstanding,
        OffsetDateTime createdAt
) {
    public static CustomerResponse of(Customer c, BigDecimal outstanding) {
        return new CustomerResponse(c.getId(), c.getName(), c.getPhone(),
                outstanding == null ? BigDecimal.ZERO : outstanding,
                c.getCreatedAt());
    }
}
