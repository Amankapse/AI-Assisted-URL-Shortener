package com.example.urlshortener.url.dto;

import com.example.urlshortener.campaign.dto.CampaignSummaryResponse;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ShortUrlResponse {

    private UUID id;
    private String shortCode;
    private String shortUrl;
    private String customAlias;
    private String originalUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean enabled;
    private long clickCount;
    private String state;
    private CampaignSummaryResponse campaign;
    private List<String> tags = List.of();
    @JsonIgnore
    private long version;

    public ShortUrlResponse() {
    }

    public ShortUrlResponse(UUID id, String shortCode, String shortUrl, String customAlias, String originalUrl, LocalDateTime createdAt, LocalDateTime expiresAt, boolean enabled, long clickCount, long version) {
        this(id, shortCode, shortUrl, customAlias, originalUrl, createdAt, expiresAt, enabled, clickCount, version, null, null, List.of());
    }

    public ShortUrlResponse(UUID id, String shortCode, String shortUrl, String customAlias, String originalUrl, LocalDateTime createdAt, LocalDateTime expiresAt, boolean enabled, long clickCount, long version, String state, CampaignSummaryResponse campaign, List<String> tags) {
        this.id = id;
        this.shortCode = shortCode;
        this.shortUrl = shortUrl;
        this.customAlias = customAlias;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.enabled = enabled;
        this.clickCount = clickCount;
        this.version = version;
        this.state = state;
        this.campaign = campaign;
        this.tags = tags == null ? List.of() : tags;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(String customAlias) {
        this.customAlias = customAlias;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getClickCount() {
        return clickCount;
    }

    public void setClickCount(long clickCount) {
        this.clickCount = clickCount;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public CampaignSummaryResponse getCampaign() {
        return campaign;
    }

    public void setCampaign(CampaignSummaryResponse campaign) {
        this.campaign = campaign;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? List.of() : tags;
    }
}
