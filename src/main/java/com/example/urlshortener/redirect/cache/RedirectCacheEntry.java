package com.example.urlshortener.redirect.cache;

import com.example.urlshortener.redirect.service.RedirectTarget;

import java.time.LocalDateTime;
import java.util.UUID;

public record RedirectCacheEntry(
        int schemaVersion,
        UUID urlId,
        String shortCode,
        String destinationUrl,
        boolean enabled,
        LocalDateTime expiresAt,
        boolean deleted,
        boolean blocked
) {
    public static final int CURRENT_SCHEMA_VERSION = 2;

    public RedirectCacheEntry(int schemaVersion,
                              UUID urlId,
                              String shortCode,
                              String destinationUrl,
                              boolean enabled,
                              LocalDateTime expiresAt,
                              boolean deleted) {
        this(schemaVersion, urlId, shortCode, destinationUrl, enabled, expiresAt, deleted, false);
    }

    public static RedirectCacheEntry fromTarget(RedirectTarget target) {
        return new RedirectCacheEntry(
                CURRENT_SCHEMA_VERSION,
                target.urlId(),
                target.shortCode(),
                target.destinationUrl(),
                target.enabled(),
                target.expiresAt(),
                target.deleted(),
                target.blocked()
        );
    }

    public RedirectTarget toTarget() {
        return new RedirectTarget(urlId, shortCode, destinationUrl, enabled, expiresAt, deleted, blocked);
    }
}
