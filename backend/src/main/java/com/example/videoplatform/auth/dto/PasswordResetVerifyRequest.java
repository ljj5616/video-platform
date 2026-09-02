package com.example.videoplatform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetVerifyRequest(
        @NotBlank(message = "REQUIRED_FIELD_MISSING")
        @Email(message = "INVALID_EMAIL_FORMAT")
        @Size(max = 255, message = "INVALID_EMAIL_FORMAT")
        String email,
        @NotBlank(message = "REQUIRED_FIELD_MISSING")
        @Pattern(regexp = "^\\d{6}$", message = "INVALID_PASSWORD_RESET_CODE")
        String verificationCode
) {}
