package com.example.videoplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordUpdateRequest(
        @NotBlank(message = "REQUIRED_FIELD_MISSING") String resetToken,
        @NotBlank(message = "REQUIRED_FIELD_MISSING")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,64}$", message = "INVALID_PASSWORD_FORMAT")
        String newPassword
) {}
