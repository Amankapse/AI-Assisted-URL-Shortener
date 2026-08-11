package com.example.urlshortener.url.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

public class CreateShortUrlRequest {

    @NotBlank(message = "originalUrl is required")
    @Size(max = 2048, message = "originalUrl may not exceed 2048 characters")
    private String originalUrl;

    @Size(max = 100, message = "customAlias may not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "customAlias may contain only letters, digits, hyphen, and underscore")
    private String customAlias;

    @NotNull(message = "expiresAt is required")
    private LocalDateTime expiresAt;

    private UUID campaignId;

    @Size(max = 50, message = "tags list is too large")
    private List<String> tags = new ArrayList<>();

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

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }
}
