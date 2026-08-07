package com.example.urlshortener.url.repository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ShortUrlSummary {
    UUID getId();
    String getShortCode();
    String getCustomAlias();
    String getOriginalUrl();
    LocalDateTime getCreatedAt();
    LocalDateTime getExpiresAt();
    boolean isEnabled();
    long getClickCount();
}
