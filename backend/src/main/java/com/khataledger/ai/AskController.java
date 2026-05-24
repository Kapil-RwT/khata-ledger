package com.khataledger.ai;

import com.khataledger.ai.dto.AskRequest;
import com.khataledger.ai.dto.AskResponse;
import com.khataledger.config.MerchantPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class AskController {

    private final AskService askService;

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@AuthenticationPrincipal MerchantPrincipal me,
                                           @Valid @RequestBody AskRequest req) {
        return ResponseEntity.ok(askService.ask(me.merchantId(), req));
    }
}
