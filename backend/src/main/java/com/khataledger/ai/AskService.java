package com.khataledger.ai;

import com.khataledger.ai.dto.AskRequest;
import com.khataledger.ai.dto.AskResponse;
import com.khataledger.customer.Customer;
import com.khataledger.customer.CustomerRepository;
import com.khataledger.transaction.Transaction;
import com.khataledger.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Natural-language ledger Q&A.
 *
 * Pipeline:
 *   1. Try Gemini classifier -> get (intent, params)
 *   2. If Gemini disabled or fails, use LocalIntentRouter
 *   3. Execute the parameterized query for that intent against repos (NEVER raw user SQL)
 *   4. Format a human-friendly answer
 *
 * Architectural property worth defending in an interview:
 *   The LLM is a *router*, not a query author. Its blast radius is bounded by the
 *   enum of intents we expose. This is a deliberate choice over text-to-SQL.
 */
@Service
@RequiredArgsConstructor
public class AskService {

    private final GeminiClient gemini;
    private final LocalIntentRouter localRouter;
    private final CustomerRepository customers;
    private final TransactionRepository transactions;

    @Value("${app.reminder.overdue-after-days}")
    private long overdueAfterDays;

    @Transactional(readOnly = true)
    public AskResponse ask(Long merchantId, AskRequest req) {
        boolean usedLlm = false;
        GeminiClient.IntentDecision decision;

        Optional<GeminiClient.IntentDecision> llm = gemini.classify(req.question());
        if (llm.isPresent()) {
            decision = llm.get();
            usedLlm = true;
        } else {
            decision = localRouter.classify(req.question());
        }

        return switch (decision.intent()) {
            case TOP_DEBTORS         -> topDebtors(merchantId, intParam(decision, "limit", 5), usedLlm);
            case TOTAL_OUTSTANDING   -> totalOutstanding(merchantId, usedLlm);
            case RECENT_TRANSACTIONS -> recentTransactions(merchantId, intParam(decision, "limit", 5), usedLlm);
            case OVERDUE_LIST        -> overdueList(merchantId, usedLlm);
            case UNKNOWN             -> new AskResponse(
                    "I can answer questions about top debtors, total outstanding, recent transactions, or overdue customers.",
                    "UNKNOWN", List.of(), usedLlm);
        };
    }

    private AskResponse topDebtors(Long merchantId, int limit, boolean usedLlm) {
        Map<Long, BigDecimal> balByCustomer = balancesByCustomer(merchantId);
        Map<Long, Customer> custIndex = customerIndex(merchantId);
        List<Map<String, Object>> rows = balByCustomer.entrySet().stream()
                .filter(e -> e.getValue().signum() > 0)
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .map(e -> {
                    Customer c = custIndex.get(e.getKey());
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("customer", c == null ? "(deleted)" : c.getName());
                    r.put("outstanding", e.getValue());
                    return r;
                })
                .toList();
        String answer = rows.isEmpty()
                ? "No customers currently owe you anything."
                : "Top " + rows.size() + " debtor(s): " + rows.stream()
                    .map(r -> r.get("customer") + " (Rs." + r.get("outstanding") + ")")
                    .collect(Collectors.joining(", "));
        return new AskResponse(answer, "TOP_DEBTORS", rows, usedLlm);
    }

    private AskResponse totalOutstanding(Long merchantId, boolean usedLlm) {
        BigDecimal total = balancesByCustomer(merchantId).values().stream()
                .filter(b -> b.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AskResponse(
                "Total amount owed to you: Rs." + total,
                "TOTAL_OUTSTANDING",
                List.of(Map.of("totalOutstanding", total)),
                usedLlm);
    }

    private AskResponse recentTransactions(Long merchantId, int limit, boolean usedLlm) {
        Map<Long, Customer> custIndex = customerIndex(merchantId);
        // simple: gather all txns for this merchant and sort. Fine for the demo; in
        // production we'd add a repository method with a pageable parameter.
        List<Transaction> all = new ArrayList<>();
        custIndex.keySet().forEach(cid ->
                all.addAll(transactions.findAllByCustomerIdOrderByOccurredAtDesc(cid)));
        all.sort((a, b) -> b.getOccurredAt().compareTo(a.getOccurredAt()));
        List<Map<String, Object>> rows = all.stream().limit(limit).map(t -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("customer", custIndex.get(t.getCustomerId()).getName());
            r.put("type", t.getType());
            r.put("amount", t.getAmount());
            r.put("note", t.getNote());
            r.put("at", t.getOccurredAt());
            return r;
        }).toList();
        return new AskResponse("Showing " + rows.size() + " most recent transaction(s).",
                "RECENT_TRANSACTIONS", rows, usedLlm);
    }

    private AskResponse overdueList(Long merchantId, boolean usedLlm) {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(overdueAfterDays);
        Map<Long, BigDecimal> balByCustomer = balancesByCustomer(merchantId);
        Map<Long, Customer> custIndex = customerIndex(merchantId);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : balByCustomer.entrySet()) {
            if (e.getValue().signum() <= 0) continue;
            var txns = transactions.findAllByCustomerIdOrderByOccurredAtDesc(e.getKey());
            if (txns.isEmpty()) continue;
            OffsetDateTime last = txns.get(0).getOccurredAt();
            if (last.isAfter(threshold)) continue;
            Customer c = custIndex.get(e.getKey());
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("customer", c == null ? "(deleted)" : c.getName());
            r.put("outstanding", e.getValue());
            r.put("lastActivity", last);
            rows.add(r);
        }
        rows.sort((a, b) -> ((BigDecimal) b.get("outstanding")).compareTo((BigDecimal) a.get("outstanding")));
        String answer = rows.isEmpty()
                ? "No overdue customers right now."
                : rows.size() + " customer(s) overdue beyond " + overdueAfterDays + " days.";
        return new AskResponse(answer, "OVERDUE_LIST", rows, usedLlm);
    }

    // ---- helpers ----
    private Map<Long, BigDecimal> balancesByCustomer(Long merchantId) {
        Map<Long, BigDecimal> m = new HashMap<>();
        for (Object[] row : transactions.outstandingByCustomerForMerchant(merchantId)) {
            // Defensive numeric coercion: JPQL coalesce may return BigDecimal/Long/Integer.
            Long customerId = ((Number) row[0]).longValue();
            BigDecimal bal = row[1] instanceof BigDecimal b ? b
                           : row[1] instanceof Number n ? new BigDecimal(n.toString())
                           : BigDecimal.ZERO;
            m.put(customerId, bal);
        }
        return m;
    }

    private Map<Long, Customer> customerIndex(Long merchantId) {
        return customers.findAllByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .collect(Collectors.toMap(Customer::getId, c -> c));
    }

    private int intParam(GeminiClient.IntentDecision d, String key, int fallback) {
        Object v = d.params() == null ? null : d.params().get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }
}
