package com.example.videoplatform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "REQUIRED_FIELD_MISSING")
        @Email(message = "INVALID_EMAIL_FORMAT")
        String email,
        @NotBlank(message = "REQUIRED_FIELD_MISSING")
        String password
) {
}
