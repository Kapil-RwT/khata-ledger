package com.khataledger.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByCustomerIdOrderByOccurredAtDesc(Long customerId);

    List<Transaction> findAllByCustomerIdAndOccurredAtBetweenOrderByOccurredAtDesc(
            Long customerId, OffsetDateTime from, OffsetDateTime to);

    /**
     * Outstanding balance for one customer.
     * Convention: positive means the customer OWES the merchant (udhaar).
     *
     * sum(CREDIT) - sum(DEBIT) where everything is restricted to one customer.
     * COALESCE handles the empty-transactions edge case.
     */
    @Query("""
        select coalesce(sum(case when t.type = com.khataledger.transaction.TransactionType.CREDIT then t.amount else -t.amount end), 0)
        from Transaction t
        where t.customerId = :customerId
    """)
    BigDecimal outstandingForCustomer(@Param("customerId") Long customerId);

    /** Outstanding across every customer of one merchant (for dashboard). */
    @Query("""
        select t.customerId,
               coalesce(sum(case when t.type = com.khataledger.transaction.TransactionType.CREDIT then t.amount else -t.amount end), 0)
        from Transaction t
        where t.merchantId = :merchantId
        group by t.customerId
    """)
    List<Object[]> outstandingByCustomerForMerchant(@Param("merchantId") Long merchantId);

    /** Last transaction time per customer of this merchant (to detect overdue). */
    @Query("""
        select t.customerId, max(t.occurredAt)
        from Transaction t
        where t.merchantId = :merchantId
        group by t.customerId
    """)
    List<Object[]> lastActivityByCustomerForMerchant(@Param("merchantId") Long merchantId);
}
