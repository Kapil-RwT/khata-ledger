package com.khataledger.reminder.channel;

import com.khataledger.customer.Customer;
import com.khataledger.reminder.Reminder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stub SMS sender. In production this would call Twilio / MSG91 / Gupshup.
 * For the demo we log; the abstraction is what matters.
 */
@Slf4j
@Component
public class SmsReminderChannel implements ReminderChannel {

    @Override
    public Reminder.Channel channel() { return Reminder.Channel.SMS; }

    @Override
    public void send(Customer customer, BigDecimal outstanding) {
        log.info("[SMS] To {} ({}): Aapka outstanding Rs.{} hai. Kripya jaldi se chukayein.",
                customer.getName(), customer.getPhone(), outstanding);
    }
}
