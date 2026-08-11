package com.example.urlshortener.audit.service;

import com.example.urlshortener.audit.dto.AuditDtos.AuditEventResponse;
import com.example.urlshortener.audit.entity.AuditAction;
import com.example.urlshortener.audit.entity.AuditActorType;
import com.example.urlshortener.audit.entity.AuditEventEntity;
import com.example.urlshortener.audit.entity.AuditResourceType;
import com.example.urlshortener.audit.repository.AuditRepository;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.url.entity.ShortUrlEntity;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.workspace.service.WorkspaceAuthorizationService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditQueryService {
    private final AuditRepository auditRepository;
    private final ShortUrlRepository shortUrlRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public AuditQueryService(AuditRepository auditRepository,
                             ShortUrlRepository shortUrlRepository,
                             WorkspaceAuthorizationService authorizationService) {
        this.auditRepository = auditRepository;
        this.shortUrlRepository = shortUrlRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> workspaceEvents(UUID workspaceId,
                                                    LocalDateTime from,
                                                    LocalDateTime to,
                                                    AuditAction action,
                                                    AuditResourceType resourceType,
                                                    AuditActorType actorType,
                                                    int page,
                                                    int size) {
        authorizationService.requireMemberManager(workspaceId);
        return auditRepository.findAll(
                        filters(workspaceId, null, null, from, to, action, resourceType, actorType),
                        pageable(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> urlEvents(UUID urlId,
                                              LocalDateTime from,
                                              LocalDateTime to,
                                              AuditAction action,
                                              AuditActorType actorType,
                                              int page,
                                              int size) {
        ShortUrlEntity url = shortUrlRepository.findById(urlId)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        authorizationService.requireLinkWriter(url.getWorkspace().getId());
        return auditRepository.findAll(
                        filters(null, AuditResourceType.URL, urlId, from, to, action, null, actorType),
                        pageable(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> adminEvents(UUID workspaceId,
                                                LocalDateTime from,
                                                LocalDateTime to,
                                                AuditAction action,
                                                AuditResourceType resourceType,
                                                AuditActorType actorType,
                                                int page,
                                                int size) {
        return auditRepository.findAll(
                        filters(workspaceId, null, null, from, to, action, resourceType, actorType),
                        pageable(page, size))
                .map(this::toResponse);
    }

    private Specification<AuditEventEntity> filters(UUID workspaceId,
                                                    AuditResourceType fixedResourceType,
                                                    UUID resourceId,
                                                    LocalDateTime from,
                                                    LocalDateTime to,
                                                    AuditAction action,
                                                    AuditResourceType resourceType,
                                                    AuditActorType actorType) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            addEqual(predicates, builder, root.get("workspaceId"), workspaceId);
            addEqual(predicates, builder, root.get("resourceType"), fixedResourceType);
            addEqual(predicates, builder, root.get("resourceId"), resourceId);
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            addEqual(predicates, builder, root.get("action"), action);
            addEqual(predicates, builder, root.get("resourceType"), resourceType);
            addEqual(predicates, builder, root.get("actorType"), actorType);
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private <T> void addEqual(List<Predicate> predicates,
                              jakarta.persistence.criteria.CriteriaBuilder builder,
                              jakarta.persistence.criteria.Path<T> path,
                              T value) {
        if (value != null) {
            predicates.add(builder.equal(path, value));
        }
    }

    private PageRequest pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
    }

    private AuditEventResponse toResponse(AuditEventEntity entity) {
        return new AuditEventResponse(
                entity.getId(),
                entity.getOccurredAt(),
                entity.getSchemaVersion(),
                entity.getWorkspaceId(),
                entity.getActorType(),
                entity.getActorId(),
                entity.getAction(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getCorrelationId(),
                new LinkedHashMap<>(safeMetadata(entity.getMetadata()))
        );
    }

    private Map<String, Object> safeMetadata(Map<String, Object> metadata) {
        return metadata == null ? Map.of() : metadata;
    }
}
