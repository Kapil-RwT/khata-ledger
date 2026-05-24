package com.khataledger.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank @Size(max = 120) String name,
        @Pattern(regexp = "^[0-9+\\-\\s]{0,20}$", message = "invalid phone") String phone
) {}
