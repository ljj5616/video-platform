package com.example.videoplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FindIdCodeRequest(
        @NotBlank(message = "REQUIRED_FIELD_MISSING")
        @Size(max = 50, message = "REQUIRED_FIELD_MISSING")
        String name,

        @NotBlank(message = "REQUIRED_FIELD_MISSING")
        @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "INVALID_PHONE_FORMAT")
        String phone
) {
}
