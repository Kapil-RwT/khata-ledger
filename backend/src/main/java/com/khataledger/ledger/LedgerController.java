package com.khataledger.ledger;

import com.khataledger.config.MerchantPrincipal;
import com.khataledger.ledger.dto.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal MerchantPrincipal me) {
        return ledgerService.dashboard(me.merchantId());
    }
}
