package com.khataledger.reminder.channel;

import com.khataledger.customer.Customer;
import com.khataledger.reminder.Reminder;

import java.math.BigDecimal;

/**
 * Strategy pattern: every concrete delivery mechanism (SMS, WhatsApp, Email)
 * implements this interface and registers itself as a Spring bean. The reminder
 * dispatcher picks the right implementation based on Reminder.Channel.
 *
 * Talking point for LLD round: "I used the Strategy pattern so we can swap
 * delivery channels without touching the scheduler or the persistence layer.
 * Each channel is a Spring bean, so we resolve them by their declared Channel
 * enum at runtime via a Map<Channel, ReminderChannel>."
 */
public interface ReminderChannel {

    Reminder.Channel channel();

    void send(Customer customer, BigDecimal outstanding);
}
