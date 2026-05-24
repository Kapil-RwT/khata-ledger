package com.khataledger.transaction;

import com.khataledger.common.exception.NotFoundException;
import com.khataledger.customer.Customer;
import com.khataledger.customer.CustomerRepository;
import com.khataledger.transaction.dto.TransactionRequest;
import com.khataledger.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactions;
    private final CustomerRepository customers;

    /**
     * Recording a transaction is the canonical write path of this service.
     * It is @Transactional so the whole operation (customer ownership check + insert) is atomic.
     *
     * Why we don't recompute and persist a "balance" column on Customer:
     *   - It's a denormalization that creates a consistency hazard: every txn write must update
     *     two rows, and they can drift if anything goes wrong mid-transaction.
     *   - The current outstanding is a derived value; we compute it on read via SQL SUM.
     *   - If read volume ever forces denormalization, we'd introduce it with an idempotent
     *     materialized view or an outbox + event handler, not by mutating the customer row inline.
     * This is the kind of design decision an SDE interviewer probes for. Be ready to defend it.
     */
    @Transactional
    public TransactionResponse record(Long merchantId, Long customerId, TransactionRequest req) {
        Customer c = customers.findByIdAndMerchantId(customerId, merchantId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        Transaction t = Transaction.builder()
                .customerId(c.getId())
                .merchantId(merchantId)
                .type(req.type())
                .amount(req.amount())
                .note(req.note())
                .occurredAt(req.occurredAt() == null ? OffsetDateTime.now() : req.occurredAt())
                .build();
        return TransactionResponse.of(transactions.save(t));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listForCustomer(Long merchantId, Long customerId,
                                                     OffsetDateTime from, OffsetDateTime to) {
        // Authorize: ensure customer belongs to this merchant
        customers.findByIdAndMerchantId(customerId, merchantId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        List<Transaction> list = (from != null && to != null)
                ? transactions.findAllByCustomerIdAndOccurredAtBetweenOrderByOccurredAtDesc(customerId, from, to)
                : transactions.findAllByCustomerIdOrderByOccurredAtDesc(customerId);
        return list.stream().map(TransactionResponse::of).toList();
    }
}
