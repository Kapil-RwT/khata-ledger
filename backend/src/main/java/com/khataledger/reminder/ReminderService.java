package com.khataledger.reminder;

import com.khataledger.customer.Customer;
import com.khataledger.customer.CustomerRepository;
import com.khataledger.reminder.channel.ReminderChannel;
import com.khataledger.transaction.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Daily scan: for each customer with outstanding > 0 and no recent activity,
 * create a Reminder row and dispatch it through the configured channel.
 *
 * Why we persist Reminder before sending: if delivery fails we have an audit trail
 * and can retry. (For the demo we mark SENT immediately; in production this would
 * be a queued outbox.)
 */
@Slf4j
@Service
public class ReminderService {

    private final CustomerRepository customers;
    private final TransactionRepository transactions;
    private final ReminderRepository reminders;
    private final Map<Reminder.Channel, ReminderChannel> channelByEnum;
    private final long overdueAfterDays;

    public ReminderService(CustomerRepository customers,
                           TransactionRepository transactions,
                           ReminderRepository reminders,
                           List<ReminderChannel> channels,
                           @Value("${app.reminder.overdue-after-days}") long overdueAfterDays) {
        this.customers = customers;
        this.transactions = transactions;
        this.reminders = reminders;
        this.overdueAfterDays = overdueAfterDays;
        // Build a lookup: Channel enum -> the strategy bean that implements it.
        // Spring injects every ReminderChannel bean as a List; we index it here.
        this.channelByEnum = new HashMap<>();
        for (ReminderChannel ch : channels) channelByEnum.put(ch.channel(), ch);
    }

    /** Scheduled daily at 09:00 server time. Cron format: sec min hour day mon dow. */
    @Scheduled(cron = "${app.reminder.cron:0 0 9 * * *}")
    @Async
    public void scanOverdueLedgers() {
        log.info("Starting overdue ledger scan");
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(overdueAfterDays);

        // For every merchant -> for every customer with positive outstanding and last activity older than threshold,
        // queue a reminder. We iterate via repositories that already exist; in a real system this would be a
        // single SQL aggregate.
        customers.findAll().forEach(c -> {
            BigDecimal outstanding = transactions.outstandingForCustomer(c.getId());
            if (outstanding == null || outstanding.signum() <= 0) return;

            var txns = transactions.findAllByCustomerIdOrderByOccurredAtDesc(c.getId());
            if (txns.isEmpty()) return;
            if (txns.get(0).getOccurredAt().isAfter(threshold)) return; // recent activity, skip

            // pick channel: prefer WhatsApp if phone is set, else SMS, else Email
            Reminder.Channel ch = c.getPhone() != null && !c.getPhone().isBlank()
                    ? Reminder.Channel.WHATSAPP
                    : Reminder.Channel.EMAIL;
            dispatch(c, outstanding, ch);
        });
        log.info("Overdue ledger scan finished");
    }

    /**
     * Known limitation worth talking about in an LLD round:
     *   scanOverdueLedgers() above calls this method on `this`, so Spring's AOP proxy
     *   is bypassed and the @Transactional below does NOT start a new transaction.
     *   It's not a correctness bug here because each `reminders.save()` is itself
     *   wrapped in a transaction by Spring Data JPA (SimpleJpaRepository is @Transactional),
     *   so individual writes still commit atomically. The downside is that we can't
     *   roll back the audit row if delivery throws after the first save.
     *
     *   Production fix: extract dispatch into its own bean (e.g. ReminderDispatcher) and
     *   inject it into ReminderService, so the call goes through the proxy.
     */
    @Transactional
    public void dispatch(Customer customer, BigDecimal outstanding, Reminder.Channel ch) {
        Reminder r = Reminder.builder()
                .customerId(customer.getId())
                .merchantId(customer.getMerchantId())
                .channel(ch)
                .outstanding(outstanding)
                .status(Reminder.Status.PENDING)
                .build();
        r = reminders.save(r);

        ReminderChannel impl = channelByEnum.get(ch);
        try {
            impl.send(customer, outstanding);
            r.setStatus(Reminder.Status.SENT);
            r.setSentAt(OffsetDateTime.now());
        } catch (Exception e) {
            log.warn("Reminder dispatch failed for customer {}: {}", customer.getId(), e.getMessage());
            r.setStatus(Reminder.Status.FAILED);
        }
        reminders.save(r);
    }
}
