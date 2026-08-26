package com.example.videoplatform.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserSignUpRequest(
        @NotBlank(message = "REQUIRED_FIELD_MISSING")
        @Email(message = "INVALID_EMAIL_FORMAT")
        @Size(max = 255, message = "INVALID_EMAIL_FORMAT")
        String email,

        @NotBlank(message = "REQUIRED_FIELD_MISSING")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,64}$",
                message = "INVALID_PASSWORD_FORMAT"
        )
        String password,

        @NotBlank(message = "REQUIRED_FIELD_MISSING")
        @Pattern(regexp = "^[가-힣A-Za-z0-9_]{2,20}$", message = "INVALID_NICKNAME_FORMAT")
        String nickname,

        @NotBlank(message = "REQUIRED_FIELD_MISSING")
        @Size(max = 50, message = "REQUIRED_FIELD_MISSING")
        String name
) {
}
