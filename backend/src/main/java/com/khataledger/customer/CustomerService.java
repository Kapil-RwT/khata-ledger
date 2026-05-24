package com.khataledger.customer;

import com.khataledger.common.exception.NotFoundException;
import com.khataledger.customer.dto.CustomerRequest;
import com.khataledger.customer.dto.CustomerResponse;
import com.khataledger.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customers;
    private final TransactionRepository transactions;

    @Transactional
    public CustomerResponse create(Long merchantId, CustomerRequest req) {
        Customer c = Customer.builder()
                .merchantId(merchantId)
                .name(req.name().trim())
                .phone(req.phone() == null || req.phone().isBlank() ? null : req.phone().trim())
                .build();
        return CustomerResponse.of(customers.save(c), BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> list(Long merchantId) {
        return customers.findAllByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .map(c -> CustomerResponse.of(c, transactions.outstandingForCustomer(c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long merchantId, Long customerId) {
        Customer c = customers.findByIdAndMerchantId(customerId, merchantId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        return CustomerResponse.of(c, transactions.outstandingForCustomer(c.getId()));
    }

    @Transactional
    public void delete(Long merchantId, Long customerId) {
        Customer c = customers.findByIdAndMerchantId(customerId, merchantId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        customers.delete(c);
    }
}
