package com.example.videoplatform.auth.dto;

public record PasswordResetVerifyResponse(String resetToken, long expiresIn) {}
