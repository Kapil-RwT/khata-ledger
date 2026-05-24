package com.khataledger.ledger;

import com.khataledger.customer.CustomerRepository;
import com.khataledger.ledger.dto.DashboardResponse;
import com.khataledger.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final TransactionRepository transactions;
    private final CustomerRepository customers;

    @Value("${app.reminder.overdue-after-days}")
    private long overdueAfterDays;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(Long merchantId) {
        var balances = transactions.outstandingByCustomerForMerchant(merchantId);
        BigDecimal receivables = BigDecimal.ZERO;
        BigDecimal payables = BigDecimal.ZERO;
        Map<Long, BigDecimal> outstandingByCustomer = new HashMap<>();
        for (Object[] row : balances) {
            Long customerId = ((Number) row[0]).longValue();
            BigDecimal bal = toBigDecimal(row[1]);
            outstandingByCustomer.put(customerId, bal);
            if (bal.signum() > 0) receivables = receivables.add(bal);
            else if (bal.signum() < 0) payables = payables.add(bal.abs());
        }

        OffsetDateTime threshold = OffsetDateTime.now().minusDays(overdueAfterDays);
        long overdue = 0;
        var lastActivity = transactions.lastActivityByCustomerForMerchant(merchantId);
        for (Object[] row : lastActivity) {
            Long customerId = ((Number) row[0]).longValue();
            OffsetDateTime last = (OffsetDateTime) row[1];
            BigDecimal bal = outstandingByCustomer.getOrDefault(customerId, BigDecimal.ZERO);
            if (bal.signum() > 0 && last.isBefore(threshold)) overdue++;
        }

        long customerCount = customers.findAllByMerchantIdOrderByCreatedAtDesc(merchantId).size();
        return new DashboardResponse(receivables, payables, customerCount, overdue);
    }

    /**
     * Defensive numeric coercion. JPQL `coalesce(sum(...), 0)` can return BigDecimal,
     * Long, or Integer depending on the Hibernate version and dialect. Funnel everything
     * through BigDecimal so downstream arithmetic is type-safe.
     */
    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return new BigDecimal(n.toString());
        throw new IllegalStateException("Unexpected numeric: " + o.getClass());
    }
}
