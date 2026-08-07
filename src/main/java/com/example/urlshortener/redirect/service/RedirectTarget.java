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
        boolean deleted
) {
    public static RedirectTarget fromEntity(ShortUrlEntity entity) {
        return new RedirectTarget(
                entity.getId(),
                entity.getShortCode(),
                entity.getOriginalUrl(),
                entity.isEnabled(),
                entity.getExpiresAt(),
                entity.isDeleted()
        );
    }
}
