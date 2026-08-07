package com.example.urlshortener.url.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class CreateShortUrlRequest {

    @NotBlank(message = "originalUrl is required")
    @Size(max = 2048, message = "originalUrl may not exceed 2048 characters")
    private String originalUrl;

    @Size(max = 100, message = "customAlias may not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "customAlias may contain only letters, digits, hyphen, and underscore")
    private String customAlias;

    @NotNull(message = "expiresAt is required")
    private LocalDateTime expiresAt;

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(String customAlias) {
        this.customAlias = customAlias;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
