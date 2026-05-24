package com.khataledger.transaction;

import com.khataledger.config.MerchantPrincipal;
import com.khataledger.transaction.dto.TransactionRequest;
import com.khataledger.transaction.dto.TransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/customers/{customerId}/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @AuthenticationPrincipal MerchantPrincipal me,
            @PathVariable Long customerId,
            @Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.ok(service.record(me.merchantId(), customerId, req));
    }

    @GetMapping
    public List<TransactionResponse> list(
            @AuthenticationPrincipal MerchantPrincipal me,
            @PathVariable Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return service.listForCustomer(me.merchantId(), customerId, from, to);
    }
}
