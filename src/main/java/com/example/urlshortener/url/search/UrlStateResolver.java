package com.example.urlshortener.url.search;

import com.example.urlshortener.url.entity.ShortUrlEntity;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class UrlStateResolver {
    private final Clock clock;

    public UrlStateResolver(Clock clock) {
        this.clock = clock;
    }

    public String state(ShortUrlEntity entity) {
        if (entity.isDeleted()) {
            return "DELETED";
        }
        if (entity.isBlocked()) {
            return "BLOCKED";
        }
        if (entity.getExpiresAt() != null && !entity.getExpiresAt().isAfter(LocalDateTime.now(clock))) {
            return "EXPIRED";
        }
        if (!entity.isEnabled()) {
            return "DISABLED";
        }
        return "ACTIVE";
    }
}
