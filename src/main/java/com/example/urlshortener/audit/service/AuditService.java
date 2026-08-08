package com.example.urlshortener.audit.service;

import com.example.urlshortener.audit.config.AuditProperties;
import com.example.urlshortener.audit.entity.AuditAction;
import com.example.urlshortener.audit.entity.AuditActorType;
import com.example.urlshortener.audit.entity.AuditEventEntity;
import com.example.urlshortener.audit.entity.AuditResourceType;
import com.example.urlshortener.audit.repository.AuditRepository;
import com.example.urlshortener.common.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {
    private static final int SCHEMA_VERSION = 1;

    private final AuditRepository auditRepository;
    private final AuditProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuditService(AuditRepository auditRepository,
                        AuditProperties properties,
                        ObjectMapper objectMapper,
                        Clock clock) {
        this.auditRepository = auditRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditEventEntity recordUser(AuditAction action,
                                       UUID workspaceId,
                                       UUID actorId,
                                       AuditResourceType resourceType,
                                       UUID resourceId,
                                       Map<String, Object> metadata) {
        return record(action, workspaceId, AuditActorType.USER, actorId, resourceType, resourceId, metadata);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditEventEntity recordApiKey(AuditAction action,
                                         UUID workspaceId,
                                         UUID actorId,
                                         AuditResourceType resourceType,
                                         UUID resourceId,
                                         Map<String, Object> metadata) {
        return record(action, workspaceId, AuditActorType.API_KEY, actorId, resourceType, resourceId, metadata);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditEventEntity recordSystem(AuditAction action,
                                         UUID workspaceId,
                                         AuditResourceType resourceType,
                                         UUID resourceId,
                                         Map<String, Object> metadata) {
        return record(action, workspaceId, AuditActorType.SYSTEM, null, resourceType, resourceId, metadata);
    }

    private AuditEventEntity record(AuditAction action,
                                    UUID workspaceId,
                                    AuditActorType actorType,
                                    UUID actorId,
                                    AuditResourceType resourceType,
                                    UUID resourceId,
                                    Map<String, Object> metadata) {
        Map<String, Object> safeMetadata = safeMetadata(metadata);
        enforceMetadataBound(safeMetadata);
        AuditEventEntity entity = new AuditEventEntity(
                UUID.randomUUID(),
                LocalDateTime.now(clock),
                SCHEMA_VERSION,
                workspaceId,
                actorType,
                actorId,
                action,
                resourceType,
                resourceId,
                currentCorrelationId(),
                safeMetadata
        );
        return auditRepository.save(entity);
    }

    public Map<String, Object> urlCreatedMetadata(boolean customAlias, boolean expiresAtSet) {
        return metadata(
                "customAlias", customAlias,
                "expiresAtSet", expiresAtSet
        );
    }

    public Map<String, Object> destinationChangedMetadata(String previousUrl, String newUrl) {
        return metadata(
                "changedFields", List.of("originalUrl"),
                "previousHost", host(previousUrl),
                "newHost", host(newUrl),
                "previousUrlHash", sha256(previousUrl),
                "newUrlHash", sha256(newUrl)
        );
    }

    public Map<String, Object> expirationChangedMetadata(boolean previousExpiresAtSet, boolean newExpiresAtSet) {
        return metadata(
                "changedFields", List.of("expiresAt"),
                "previousExpiresAtSet", previousExpiresAtSet,
                "newExpiresAtSet", newExpiresAtSet
        );
    }

    public Map<String, Object> stateChangedMetadata(String previousState, String newState) {
        return metadata(
                "previousState", previousState,
                "newState", newState
        );
    }

    public Map<String, Object> workspaceCreatedMetadata(boolean defaultWorkspace) {
        return metadata("defaultWorkspace", defaultWorkspace);
    }

    public Map<String, Object> memberRoleMetadata(String previousRole, String newRole, boolean defaultMembership) {
        return metadata(
                "previousRole", previousRole,
                "newRole", newRole,
                "defaultMembership", defaultMembership
        );
    }

    public Map<String, Object> apiKeyMetadata(UUID keyId, String prefix, String name, Object scopes, LocalDateTime expiresAt) {
        return metadata(
                "keyId", keyId == null ? null : keyId.toString(),
                "prefix", prefix,
                "name", name,
                "scopes", scopes,
                "expiresAtSet", expiresAt != null
        );
    }

    public Map<String, Object> emptyMetadata() {
        return Map.of();
    }

    private Map<String, Object> metadata(Object... values) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            Object value = values[i + 1];
            if (value != null) {
                result.put((String) values[i], value);
            }
        }
        return result;
    }

    private Map<String, Object> safeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && key.matches("^[A-Za-z0-9_.-]{1,64}$")) {
                safe.put(key, value);
            }
        });
        return safe;
    }

    private void enforceMetadataBound(Map<String, Object> metadata) {
        try {
            int byteCount = objectMapper.writeValueAsString(metadata).getBytes(StandardCharsets.UTF_8).length;
            if (byteCount > properties.getMetadataMaxBytes()) {
                throw new BadRequestException("Audit metadata exceeds configured maximum");
            }
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Audit metadata is not serializable");
        }
    }

    private String host(String value) {
        try {
            String host = URI.create(value).getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String currentCorrelationId() {
        String value = MDC.get("correlationId");
        if (value != null && !value.isBlank()) {
            return value;
        }
        return "system-" + UUID.randomUUID();
    }
}
