package com.example.videoplatform.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserWithdrawalRequest(
        @NotBlank(message = "REQUIRED_FIELD_MISSING")
        String password
) {
}
