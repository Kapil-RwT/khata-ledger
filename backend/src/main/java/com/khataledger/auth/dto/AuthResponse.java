package com.khataledger.auth.dto;

public record AuthResponse(
        String token,
        Long merchantId,
        String businessName
) {}
