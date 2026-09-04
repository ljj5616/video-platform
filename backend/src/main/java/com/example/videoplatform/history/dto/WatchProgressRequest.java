package com.example.videoplatform.history.dto;

import jakarta.validation.constraints.NotNull;

public record WatchProgressRequest(
        @NotNull(message = "REQUIRED_FIELD_MISSING") Long positionSeconds
) {
}
