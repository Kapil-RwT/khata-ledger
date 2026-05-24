package com.khataledger.reminder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Wire-format DTO for Reminder rows. We never return JPA entities through controllers —
 * the API contract should be independent of the persistence schema.
 */
public record ReminderResponse(
        Long id,
        Long customerId,
        Reminder.Channel channel,
        BigDecimal outstanding,
        Reminder.Status status,
        OffsetDateTime createdAt,
        OffsetDateTime sentAt
) {
    public static ReminderResponse of(Reminder r) {
        return new ReminderResponse(
                r.getId(), r.getCustomerId(), r.getChannel(),
                r.getOutstanding(), r.getStatus(),
                r.getCreatedAt(), r.getSentAt());
    }
}
