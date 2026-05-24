package com.khataledger.customer;

import com.khataledger.config.MerchantPrincipal;
import com.khataledger.customer.dto.CustomerRequest;
import com.khataledger.customer.dto.CustomerResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@AuthenticationPrincipal MerchantPrincipal me,
                                                   @Valid @RequestBody CustomerRequest req) {
        return ResponseEntity.ok(service.create(me.merchantId(), req));
    }

    @GetMapping
    public List<CustomerResponse> list(@AuthenticationPrincipal MerchantPrincipal me) {
        return service.list(me.merchantId());
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@AuthenticationPrincipal MerchantPrincipal me,
                                @PathVariable Long id) {
        return service.get(me.merchantId(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal MerchantPrincipal me,
                                       @PathVariable Long id) {
        service.delete(me.merchantId(), id);
        return ResponseEntity.noContent().build();
    }
}
