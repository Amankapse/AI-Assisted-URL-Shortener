package com.example.urlshortener.idempotency.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupJob.class);

    private final IdempotencyService idempotencyService;

    public IdempotencyCleanupJob(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Scheduled(fixedDelayString = "${app.idempotency.cleanup-interval:1h}")
    public void deleteExpiredRecords() {
        long deleted = idempotencyService.cleanupExpired();
        if (deleted > 0) {
            log.info("Deleted {} expired idempotency records", deleted);
        }
    }
}
