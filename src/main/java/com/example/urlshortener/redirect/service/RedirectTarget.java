package com.example.urlshortener.redirect.service;

import com.example.urlshortener.url.entity.ShortUrlEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public record RedirectTarget(
        UUID urlId,
        String shortCode,
        String destinationUrl,
        boolean enabled,
        LocalDateTime expiresAt,
        boolean deleted,
        boolean blocked
) {
    public RedirectTarget(UUID urlId,
                          String shortCode,
                          String destinationUrl,
                          boolean enabled,
                          LocalDateTime expiresAt,
                          boolean deleted) {
        this(urlId, shortCode, destinationUrl, enabled, expiresAt, deleted, false);
    }

    public static RedirectTarget fromEntity(ShortUrlEntity entity) {
        return new RedirectTarget(
                entity.getId(),
                entity.getShortCode(),
                entity.getOriginalUrl(),
                entity.isEnabled(),
                entity.getExpiresAt(),
                entity.isDeleted(),
                entity.isBlocked()
        );
    }
}
