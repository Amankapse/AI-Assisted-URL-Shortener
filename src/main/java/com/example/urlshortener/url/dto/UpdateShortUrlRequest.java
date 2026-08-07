package com.example.urlshortener.url.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class UpdateShortUrlRequest {

    @NotNull(message = "expiresAt is required")
    private LocalDateTime expiresAt;

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
