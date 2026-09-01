package com.example.videoplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "REQUIRED_FIELD_MISSING")
        String refreshToken
) {
}
