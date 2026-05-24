package com.khataledger.reminder.channel;

import com.khataledger.customer.Customer;
import com.khataledger.reminder.Reminder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class EmailReminderChannel implements ReminderChannel {

    @Override
    public Reminder.Channel channel() { return Reminder.Channel.EMAIL; }

    @Override
    public void send(Customer customer, BigDecimal outstanding) {
        log.info("[Email] To {} (phone {}): Payment reminder for Rs.{}",
                customer.getName(), customer.getPhone(), outstanding);
    }
}
