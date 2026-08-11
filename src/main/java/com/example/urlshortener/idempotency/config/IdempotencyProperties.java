package com.example.urlshortener.idempotency.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.idempotency")
public class IdempotencyProperties {
    private Duration retention = Duration.ofHours(24);
    private Duration cleanupInterval = Duration.ofHours(1);

    public Duration getRetention() {
        return retention;
    }

    public void setRetention(Duration retention) {
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("app.idempotency.retention must be positive");
        }
        this.retention = retention;
    }

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(Duration cleanupInterval) {
        if (cleanupInterval == null || cleanupInterval.isZero() || cleanupInterval.isNegative()) {
            throw new IllegalArgumentException("app.idempotency.cleanup-interval must be positive");
        }
        this.cleanupInterval = cleanupInterval;
    }
}
