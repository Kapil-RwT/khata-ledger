package com.khataledger.config;

import java.security.Principal;

/**
 * Lightweight principal carried in SecurityContext after JWT validation.
 * Controllers can take this as a parameter via @AuthenticationPrincipal.
 */
public record MerchantPrincipal(Long merchantId, String businessName) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(merchantId);
    }
}
