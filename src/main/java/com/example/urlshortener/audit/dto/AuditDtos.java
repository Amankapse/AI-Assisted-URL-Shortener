package com.example.urlshortener.audit.dto;

import com.example.urlshortener.audit.entity.AuditAction;
import com.example.urlshortener.audit.entity.AuditActorType;
import com.example.urlshortener.audit.entity.AuditResourceType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public final class AuditDtos {
    private AuditDtos() {
    }

    public record AuditEventResponse(
            UUID id,
            LocalDateTime occurredAt,
            int schemaVersion,
            UUID workspaceId,
            AuditActorType actorType,
            UUID actorId,
            AuditAction action,
            AuditResourceType resourceType,
            UUID resourceId,
            String correlationId,
            Map<String, Object> safeMetadata
    ) {
    }
}
