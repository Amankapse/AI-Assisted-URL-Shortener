package com.example.urlshortener.outbox.payload;

import com.example.urlshortener.audit.entity.AuditActorType;

import java.util.UUID;

public record UrlStateChangedPayloadV1(
        UUID urlId,
        UUID workspaceId,
        String shortCode,
        String state,
        AuditActorType actorType,
        UUID actorId
) {
}
