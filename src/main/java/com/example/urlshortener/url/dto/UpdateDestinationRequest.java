package com.example.urlshortener.url.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateDestinationRequest {
    @NotBlank(message = "originalUrl is required")
    @Size(max = 2048, message = "originalUrl may not exceed 2048 characters")
    private String originalUrl;

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }
}
