package com.example.urlshortener.outbox.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record OutboxEventRecord(
        UUID id,
        UUID workspaceId,
        String aggregateType,
        String aggregateId,
        OutboxEventType eventType,
        int eventVersion,
        String payload,
        OutboxStatus status,
        int attemptCount,
        LocalDateTime createdAt,
        LocalDateTime nextAttemptAt,
        LocalDateTime claimedAt,
        String claimedBy,
        LocalDateTime processedAt,
        String lastErrorCode
) {
}
