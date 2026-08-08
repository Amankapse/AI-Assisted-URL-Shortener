package com.example.urlshortener.outbox.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record DomainEvent(
        UUID eventId,
        UUID workspaceId,
        String aggregateType,
        String aggregateId,
        OutboxEventType eventType,
        int eventVersion,
        Object payload,
        LocalDateTime occurredAt
) {
}
