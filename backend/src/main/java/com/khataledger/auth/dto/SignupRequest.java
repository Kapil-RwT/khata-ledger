package com.khataledger.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(min = 2, max = 120) String businessName,
        @NotBlank @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "invalid phone") String phone,
        @NotBlank @Size(min = 8, max = 72) String password
) {}
