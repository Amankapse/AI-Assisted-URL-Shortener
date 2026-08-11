package com.example.urlshortener.outbox.service;

import com.example.urlshortener.outbox.config.OutboxProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxCleanupJob {
    private final OutboxEventStore store;
    private final OutboxProperties properties;

    public OutboxCleanupJob(OutboxEventStore store, OutboxProperties properties) {
        this.store = store;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.outbox.cleanup-interval:1h}")
    public void cleanup() {
        if (properties.isEnabled()) {
            store.cleanupProcessed();
        }
    }
}
