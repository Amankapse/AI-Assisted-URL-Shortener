package com.example.urlshortener.outbox.dto;

import com.example.urlshortener.outbox.domain.OutboxEventRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class OutboxDtos {
    private OutboxDtos() {
    }

    public record OutboxEventSummaryResponse(
            UUID eventId,
            String eventType,
            int eventVersion,
            String aggregateType,
            String status,
            int attemptCount,
            LocalDateTime createdAt,
            LocalDateTime nextAttemptAt,
            LocalDateTime claimedAt,
            LocalDateTime processedAt,
            String lastErrorCode
    ) {
        public static OutboxEventSummaryResponse from(OutboxEventRecord record) {
            return new OutboxEventSummaryResponse(
                    record.id(),
                    record.eventType().name(),
                    record.eventVersion(),
                    record.aggregateType(),
                    record.status().name(),
                    record.attemptCount(),
                    record.createdAt(),
                    record.nextAttemptAt(),
                    record.claimedAt(),
                    record.processedAt(),
                    record.lastErrorCode()
            );
        }
    }

    public record OutboxPageResponse(
            List<OutboxEventSummaryResponse> content,
            int page,
            int size
    ) {
    }
}
