package com.khataledger.transaction;

import com.khataledger.common.exception.NotFoundException;
import com.khataledger.customer.Customer;
import com.khataledger.customer.CustomerRepository;
import com.khataledger.transaction.dto.TransactionRequest;
import com.khataledger.transaction.dto.TransactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock CustomerRepository customers;
    @Mock TransactionRepository transactions;

    @InjectMocks TransactionService service;

    private Customer ramesh;

    @BeforeEach
    void setUp() {
        ramesh = Customer.builder().id(11L).merchantId(1L).name("Ramesh").phone("9876543210").build();
    }

    @Test
    void records_credit_transaction_for_owned_customer() {
        when(customers.findByIdAndMerchantId(11L, 1L)).thenReturn(Optional.of(ramesh));
        when(transactions.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });

        TransactionResponse res = service.record(1L, 11L,
                new TransactionRequest(TransactionType.CREDIT, new BigDecimal("500.00"), "samaan", OffsetDateTime.now()));

        assertThat(res.id()).isEqualTo(99L);
        assertThat(res.type()).isEqualTo(TransactionType.CREDIT);
        assertThat(res.amount()).isEqualByComparingTo("500.00");
    }

    @Test
    void rejects_transaction_for_someone_elses_customer() {
        when(customers.findByIdAndMerchantId(11L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.record(2L, 11L,
                new TransactionRequest(TransactionType.DEBIT, BigDecimal.TEN, null, null)))
                .isInstanceOf(NotFoundException.class);
    }
}
