package com.khataledger.reminder.channel;

import com.khataledger.customer.Customer;
import com.khataledger.reminder.Reminder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class WhatsAppReminderChannel implements ReminderChannel {

    @Override
    public Reminder.Channel channel() { return Reminder.Channel.WHATSAPP; }

    @Override
    public void send(Customer customer, BigDecimal outstanding) {
        log.info("[WhatsApp] To {} ({}): Outstanding Rs.{}. Pay link: https://pay.example/abc",
                customer.getName(), customer.getPhone(), outstanding);
    }
}
