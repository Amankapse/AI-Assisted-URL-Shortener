package com.example.urlshortener.url.service;

import com.example.urlshortener.campaign.dto.CampaignSummaryResponse;
import com.example.urlshortener.campaign.entity.CampaignEntity;
import com.example.urlshortener.campaign.repository.CampaignRepository;
import com.example.urlshortener.audit.entity.AuditAction;
import com.example.urlshortener.audit.entity.AuditActorType;
import com.example.urlshortener.audit.entity.AuditResourceType;
import com.example.urlshortener.audit.service.AuditService;
import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.exception.PreconditionFailedException;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.common.ratelimit.RateLimiterService;
import com.example.urlshortener.outbox.domain.DomainEvent;
import com.example.urlshortener.outbox.domain.DomainEventPublisher;
import com.example.urlshortener.outbox.domain.OutboxEventType;
import com.example.urlshortener.outbox.payload.UrlCacheInvalidationPayloadV1;
import com.example.urlshortener.outbox.payload.UrlStateChangedPayloadV1;
import com.example.urlshortener.redirect.cache.RedirectCacheInvalidationEvent;
import com.example.urlshortener.redirect.service.RedirectTarget;
import com.example.urlshortener.tag.entity.TagEntity;
import com.example.urlshortener.tag.service.TagService;
import com.example.urlshortener.url.config.ShortCodeProperties;
import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.dto.UpdateDestinationRequest;
import com.example.urlshortener.url.dto.UpdateShortUrlRequest;
import com.example.urlshortener.url.dto.UpdateUrlCampaignRequest;
import com.example.urlshortener.url.dto.UpdateUrlTagsRequest;
import com.example.urlshortener.url.entity.ShortUrlEntity;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.url.search.UrlStateResolver;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.repository.UserRepository;
import com.example.urlshortener.user.service.CurrentOwnerProvider;
import com.example.urlshortener.workspace.service.WorkspaceContext;
import com.example.urlshortener.workspace.service.WorkspaceContextResolver;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
public class UrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final UrlValidationService validationService;
    private final ShortCodeGenerator shortCodeGenerator;
    private final CurrentOwnerProvider currentOwnerProvider;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RateLimiterService rateLimiter;
    private final AppMetrics metrics;
    private final ShortCodeProperties shortCodeProperties;
    private final UrlQuotaService quotaService;
    private final PublicUrlBuilder publicUrlBuilder;
    private final WorkspaceContextResolver workspaceContextResolver;
    private final AuditService auditService;
    private final DomainEventPublisher domainEventPublisher;
    private final CampaignRepository campaignRepository;
    private final TagService tagService;
    private final UrlStateResolver stateResolver;

    public UrlService(ShortUrlRepository shortUrlRepository,
                      UrlValidationService validationService,
                      ShortCodeGenerator shortCodeGenerator,
                      CurrentOwnerProvider currentOwnerProvider,
                      UserRepository userRepository,
                      ApplicationEventPublisher eventPublisher,
                      RateLimiterService rateLimiter,
                      AppMetrics metrics,
                      ShortCodeProperties shortCodeProperties,
                      UrlQuotaService quotaService,
                      PublicUrlBuilder publicUrlBuilder,
                      WorkspaceContextResolver workspaceContextResolver,
                      AuditService auditService,
                      DomainEventPublisher domainEventPublisher,
                      CampaignRepository campaignRepository,
                      TagService tagService,
                      UrlStateResolver stateResolver) {
        this.shortUrlRepository = shortUrlRepository;
        this.validationService = validationService;
        this.shortCodeGenerator = shortCodeGenerator;
        this.currentOwnerProvider = currentOwnerProvider;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
        this.shortCodeProperties = shortCodeProperties;
        this.quotaService = quotaService;
        this.publicUrlBuilder = publicUrlBuilder;
        this.workspaceContextResolver = workspaceContextResolver;
        this.auditService = auditService;
        this.domainEventPublisher = domainEventPublisher;
        this.campaignRepository = campaignRepository;
        this.tagService = tagService;
        this.stateResolver = stateResolver;
    }

    @Transactional
    public ShortUrlResponse create(CreateShortUrlRequest request) {
        return create(request, (String) null);
    }

    @Transactional
    public ShortUrlResponse create(CreateShortUrlRequest request, String workspaceHeader) {
        WorkspaceContext workspace = workspaceContextResolver.resolveForLinkWriter(workspaceHeader);
        return create(request, workspace);
    }

    @Transactional
    public ShortUrlResponse create(CreateShortUrlRequest request, WorkspaceContext workspace) {
        rateLimiter.enforce("url-create", workspace.actorType() + ":" + workspace.actorId());
        UserEntity owner = ownerReference(workspace);
        try {
            String destinationHost = validationService.validatedDestinationHost(request.getOriginalUrl());
            validationService.validateCustomAlias(request.getCustomAlias());
            validationService.validateExpiration(request.getCustomAlias(), request.getExpiresAt());
            quotaService.enforceCreateQuota(workspace.workspaceId(), request);
            CampaignEntity campaign = resolveCampaign(request.getCampaignId(), workspace.workspaceId());
            LinkedHashSet<TagEntity> tags = new LinkedHashSet<>(tagService.resolveOrCreate(workspace.workspace(), request.getTags()));

            String shortCode = request.getCustomAlias();
            if (shortCode != null && !shortCode.isBlank()) {
                if (shortUrlRepository.existsByCustomAlias(shortCode) || shortUrlRepository.existsByShortCode(shortCode)) {
                    throw new BadRequestException("customAlias is already in use");
                }
                try {
                    ShortUrlEntity entity = new ShortUrlEntity(UUID.randomUUID(), shortCode, request.getOriginalUrl(), request.getCustomAlias(), owner, workspace.workspace(), request.getExpiresAt());
                    entity.setDestinationHost(destinationHost);
                    entity.setCampaign(campaign);
                    entity.setTags(tags);
                    ShortUrlEntity saved = shortUrlRepository.save(entity);
                    recordActor(AuditAction.URL_CREATED, workspace, saved.getId(), auditService.urlCreatedMetadata(true, saved.getExpiresAt() != null));
                    publishUrlMutation(OutboxEventType.URL_CREATED, saved, workspace, "CREATED");
                    metrics.urlCreated();
                    return mapToResponse(saved);
                } catch (DataIntegrityViolationException ex) {
                    throw new BadRequestException("customAlias is already in use");
                }
            } else {
                return createWithGeneratedCode(request, owner, workspace, destinationHost, campaign, tags);
            }
        } catch (BadRequestException ex) {
            metrics.urlCreationFailed(failureReason(ex));
            throw ex;
        } catch (RuntimeException ex) {
            metrics.urlCreationFailed("error");
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public ShortUrlResponse get(UUID id) {
        return get(id, null);
    }

    @Transactional(readOnly = true)
    public ShortUrlResponse get(UUID id, String workspaceHeader) {
        WorkspaceContext workspace = workspaceContextResolver.resolveForLinkReader(workspaceHeader);
        ShortUrlEntity entity = shortUrlRepository.findByIdAndWorkspaceId(id, workspace.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public Page<ShortUrlResponse> list(int page, int size) {
        return list(page, size, null);
    }

    @Transactional(readOnly = true)
    public Page<ShortUrlResponse> list(int page, int size, String workspaceHeader) {
        WorkspaceContext workspace = workspaceContextResolver.resolveForLinkReader(workspaceHeader);
        Pageable pageable = PageRequest.of(page, size);
        return shortUrlRepository.findByWorkspaceId(workspace.workspaceId(), pageable).map(this::mapToResponse);
    }

    @Transactional
    public ShortUrlResponse updateExpiration(UUID id, UpdateShortUrlRequest request) {
        return updateExpiration(id, request, null);
    }

    @Transactional
    public ShortUrlResponse updateExpiration(UUID id, UpdateShortUrlRequest request, String workspaceHeader) {
        validationService.validateExpiration(null, request.getExpiresAt());
        WorkspaceContext workspace = workspaceContextResolver.resolveForLinkWriter(workspaceHeader);
        ShortUrlEntity entity = shortUrlRepository.findByIdAndWorkspaceId(id, workspace.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        boolean previousExpiresAtSet = entity.getExpiresAt() != null;
        entity.setExpiresAt(request.getExpiresAt());
        ShortUrlResponse response = mapToResponse(shortUrlRepository.save(entity));
        recordActor(AuditAction.URL_EXPIRATION_CHANGED, workspace, entity.getId(), auditService.expirationChangedMetadata(previousExpiresAtSet, entity.getExpiresAt() != null));
        publishUrlMutation(OutboxEventType.URL_EXPIRATION_CHANGED, entity, workspace, entity.isEnabled() ? "ENABLED" : "DISABLED");
        publishCacheInvalidation(entity);
        eventPublisher.publishEvent(new RedirectCacheInvalidationEvent(entity.getShortCode()));
        return response;
    }

    @Transactional
    public ShortUrlResponse updateDestination(UUID id, UpdateDestinationRequest request, long expectedVersion) {
        return updateDestination(id, request, expectedVersion, null);
    }

    @Transactional
    public ShortUrlResponse updateDestination(UUID id, UpdateDestinationRequest request, long expectedVersion, String workspaceHeader) {
        String destinationHost = validationService.validatedDestinationHost(request.getOriginalUrl());
        WorkspaceContext workspace = workspaceContextResolver.resolveForLinkWriter(workspaceHeader);
        ShortUrlEntity entity = shortUrlRepository.findByIdAndWorkspaceId(id, workspace.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        if (entity.getVersion() != expectedVersion) {
            throw new PreconditionFailedException("If-Match header does not match the current URL version.");
        }
        String previousUrl = entity.getOriginalUrl();
        entity.setOriginalUrl(request.getOriginalUrl());
        entity.setDestinationHost(destinationHost);
        ShortUrlResponse response = mapToResponse(shortUrlRepository.save(entity));
        recordActor(AuditAction.URL_DESTINATION_CHANGED, workspace, entity.getId(), auditService.destinationChangedMetadata(previousUrl, entity.getOriginalUrl()));
        publishUrlMutation(OutboxEventType.URL_DESTINATION_CHANGED, entity, workspace, "DESTINATION_CHANGED");
        publishCacheInvalidation(entity);
        eventPublisher.publishEvent(new RedirectCacheInvalidationEvent(entity.getShortCode()));
        return response;
    }

    @Transactional
    public void delete(UUID id) {
        delete(id, null);
    }

    @Transactional
    public void delete(UUID id, String workspaceHeader) {
        WorkspaceContext workspace = workspaceContextResolver.resolveForLinkWriter(workspaceHeader);
        ShortUrlEntity entity = shortUrlRepository.findByIdAndWorkspaceId(id, workspace.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        entity.setDeleted(true);
        entity.setEnabled(false);
        shortUrlRepository.save(entity);
        recordActor(AuditAction.URL_DELETED, workspace, entity.getId(), auditService.stateChangedMetadata("ACTIVE", "DELETED"));
        publishUrlMutation(OutboxEventType.URL_DELETED, entity, workspace, "DELETED");
        publishCacheInvalidation(entity);
        eventPublisher.publishEvent(new RedirectCacheInvalidationEvent(entity.getShortCode()));
    }

    @Transactional
    public ShortUrlResponse updateCampaign(UUID id, UpdateUrlCampaignRequest request, long expectedVersion, String workspaceHeader) {
        WorkspaceContext workspace = workspaceContextResolver.resolveForLinkWriter(workspaceHeader);
        ShortUrlEntity entity = shortUrlRepository.findByIdAndWorkspaceId(id, workspace.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        if (entity.getVersion() != expectedVersion) {
            throw new PreconditionFailedException("If-Match header does not match the current URL version.");
        }
        boolean previousCampaignSet = entity.getCampaign() != null;
        CampaignEntity campaign = resolveCampaign(request.getCampaignId(), workspace.workspaceId());
        entity.setCampaign(campaign);
        ShortUrlResponse response = mapToResponse(shortUrlRepository.save(entity));
        recordActor(AuditAction.URL_CAMPAIGN_CHANGED, workspace, entity.getId(), auditService.urlCampaignChangedMetadata(previousCampaignSet, campaign != null));
        return response;
    }

    @Transactional
    public ShortUrlResponse replaceTags(UUID id, UpdateUrlTagsRequest request, long expectedVersion, String workspaceHeader) {
        WorkspaceContext workspace = workspaceContextResolver.resolveForLinkWriter(workspaceHeader);
        ShortUrlEntity entity = shortUrlRepository.findByIdAndWorkspaceId(id, workspace.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        if (entity.getVersion() != expectedVersion) {
            throw new PreconditionFailedException("If-Match header does not match the current URL version.");
        }
        LinkedHashSet<TagEntity> tags = new LinkedHashSet<>(tagService.resolveOrCreate(workspace.workspace(), request.getTags()));
        entity.setTags(tags);
        ShortUrlResponse response = mapToResponse(shortUrlRepository.save(entity));
        recordActor(AuditAction.URL_TAGS_CHANGED, workspace, entity.getId(), auditService.urlTagsChangedMetadata(tags.stream().map(TagEntity::getNormalizedName).sorted().toList()));
        metrics.tags("replace", "success");
        return response;
    }

    @Transactional
    public ShortUrlResponse disable(UUID id) {
        return disable(id, null);
    }

    @Transactional
    public ShortUrlResponse disable(UUID id, String workspaceHeader) {
        WorkspaceContext workspace = workspaceContextResolver.resolveForLinkWriter(workspaceHeader);
        ShortUrlEntity entity = shortUrlRepository.findByIdAndWorkspaceId(id, workspace.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        entity.setEnabled(false);
        ShortUrlResponse response = mapToResponse(shortUrlRepository.save(entity));
        recordActor(AuditAction.URL_DISABLED, workspace, entity.getId(), auditService.stateChangedMetadata("ENABLED", "DISABLED"));
        publishUrlMutation(OutboxEventType.URL_DISABLED, entity, workspace, "DISABLED");
        publishCacheInvalidation(entity);
        eventPublisher.publishEvent(new RedirectCacheInvalidationEvent(entity.getShortCode()));
        return response;
    }

    @Transactional
    public ShortUrlResponse enable(UUID id) {
        return enable(id, null);
    }

    @Transactional
    public ShortUrlResponse enable(UUID id, String workspaceHeader) {
        WorkspaceContext workspace = workspaceContextResolver.resolveForLinkWriter(workspaceHeader);
        ShortUrlEntity entity = shortUrlRepository.findByIdAndWorkspaceId(id, workspace.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        if (entity.isBlocked()) {
            throw new BadRequestException("Blocked short URLs cannot be enabled by the owner");
        }
        entity.setEnabled(true);
        ShortUrlResponse response = mapToResponse(shortUrlRepository.save(entity));
        recordActor(AuditAction.URL_ENABLED, workspace, entity.getId(), auditService.stateChangedMetadata("DISABLED", "ENABLED"));
        publishUrlMutation(OutboxEventType.URL_ENABLED, entity, workspace, "ENABLED");
        publishCacheInvalidation(entity);
        eventPublisher.publishEvent(new RedirectCacheInvalidationEvent(entity.getShortCode()));
        return response;
    }

    @Transactional(readOnly = true)
    public ShortUrlEntity resolveByShortCode(String shortCode) {
        return shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
    }

    @Transactional(readOnly = true)
    public RedirectTarget resolveRedirectTarget(String shortCode) {
        return RedirectTarget.fromEntity(resolveByShortCode(shortCode));
    }

    @Transactional
    public void block(UUID id) {
        ShortUrlEntity entity = shortUrlRepository.findById(id)
                .filter(url -> !url.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        if (!entity.isBlocked()) {
            entity.setBlocked(true);
            shortUrlRepository.save(entity);
            auditService.recordUser(
                    AuditAction.URL_BLOCKED,
                    entity.getWorkspace().getId(),
                    currentOwnerProvider.getCurrentOwner().userId(),
                    AuditResourceType.URL,
                    entity.getId(),
                    auditService.stateChangedMetadata("UNBLOCKED", "BLOCKED")
            );
            publishUrlMutation(OutboxEventType.URL_BLOCKED, entity, AuditActorType.USER, currentOwnerProvider.getCurrentOwner().userId(), "BLOCKED");
            publishCacheInvalidation(entity);
            eventPublisher.publishEvent(new RedirectCacheInvalidationEvent(entity.getShortCode()));
        }
    }

    @Transactional
    public void unblock(UUID id) {
        ShortUrlEntity entity = shortUrlRepository.findById(id)
                .filter(url -> !url.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
        if (entity.isBlocked()) {
            entity.setBlocked(false);
            shortUrlRepository.save(entity);
            auditService.recordUser(
                    AuditAction.URL_UNBLOCKED,
                    entity.getWorkspace().getId(),
                    currentOwnerProvider.getCurrentOwner().userId(),
                    AuditResourceType.URL,
                    entity.getId(),
                    auditService.stateChangedMetadata("BLOCKED", "UNBLOCKED")
            );
            publishUrlMutation(OutboxEventType.URL_UNBLOCKED, entity, AuditActorType.USER, currentOwnerProvider.getCurrentOwner().userId(), "UNBLOCKED");
            publishCacheInvalidation(entity);
            eventPublisher.publishEvent(new RedirectCacheInvalidationEvent(entity.getShortCode()));
        }
    }

    private ShortUrlResponse createWithGeneratedCode(CreateShortUrlRequest request, UserEntity owner, WorkspaceContext workspace, String destinationHost, CampaignEntity campaign, LinkedHashSet<TagEntity> tags) {
        for (int attempt = 0; attempt < shortCodeProperties.getMaxRetries(); attempt++) {
            String candidate = shortCodeGenerator.generate();
            if (shortUrlRepository.existsByShortCode(candidate)) {
                metrics.shortCodeGeneration("collision_retry");
                continue;
            }
            try {
                ShortUrlEntity entity = new ShortUrlEntity(UUID.randomUUID(), candidate, request.getOriginalUrl(), null, owner, workspace.workspace(), request.getExpiresAt());
                entity.setDestinationHost(destinationHost);
                entity.setCampaign(campaign);
                entity.setTags(tags);
                ShortUrlEntity saved = shortUrlRepository.save(entity);
                recordActor(AuditAction.URL_CREATED, workspace, saved.getId(), auditService.urlCreatedMetadata(false, saved.getExpiresAt() != null));
                publishUrlMutation(OutboxEventType.URL_CREATED, saved, workspace, "CREATED");
                metrics.shortCodeGeneration("success");
                metrics.urlCreated();
                return mapToResponse(saved);
            } catch (DataIntegrityViolationException ex) {
                metrics.shortCodeGeneration("collision_retry");
            }
        }
        metrics.shortCodeGeneration("retry_exhausted");
        throw new BadRequestException("Unable to generate unique short code after retries");
    }

    private String failureReason(BadRequestException ex) {
        if ("customAlias is already in use".equals(ex.getMessage())) {
            return "alias_conflict";
        }
        if (ex.getMessage() != null && ex.getMessage().contains("Unable to generate unique short code")) {
            return "short_code_retry_exhausted";
        }
        return "validation";
    }

    private ShortUrlResponse mapToResponse(ShortUrlEntity entity) {
        CampaignSummaryResponse campaign = entity.getCampaign() == null
                ? null
                : new CampaignSummaryResponse(entity.getCampaign().getId(), entity.getCampaign().getName(), entity.getCampaign().getNormalizedName());
        List<String> tags = entity.getTags().stream()
                .map(TagEntity::getNormalizedName)
                .sorted(Comparator.naturalOrder())
                .toList();
        return new ShortUrlResponse(
                entity.getId(),
                entity.getShortCode(),
                publicUrlBuilder.shortUrl(entity.getShortCode()),
                entity.getCustomAlias(),
                entity.getOriginalUrl(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.isEnabled(),
                entity.getClickCount(),
                entity.getVersion(),
                stateResolver.state(entity),
                campaign,
                tags
        );
    }

    private CampaignEntity resolveCampaign(UUID campaignId, UUID workspaceId) {
        if (campaignId == null) {
            return null;
        }
        return campaignRepository.findActiveByIdAndWorkspaceId(campaignId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
    }

    private void recordActor(AuditAction action, WorkspaceContext workspace, UUID resourceId, java.util.Map<String, Object> metadata) {
        if (workspace.actorType() == AuditActorType.API_KEY) {
            auditService.recordApiKey(action, workspace.workspaceId(), workspace.actorId(), AuditResourceType.URL, resourceId, metadata);
        } else {
            auditService.recordUser(action, workspace.workspaceId(), workspace.actorId(), AuditResourceType.URL, resourceId, metadata);
        }
    }

    private void publishUrlMutation(OutboxEventType eventType, ShortUrlEntity entity, WorkspaceContext workspace, String state) {
        publishUrlMutation(eventType, entity, workspace.actorType(), workspace.actorId(), state);
    }

    private void publishUrlMutation(OutboxEventType eventType, ShortUrlEntity entity, AuditActorType actorType, UUID actorId, String state) {
        domainEventPublisher.publish(new DomainEvent(
                UUID.randomUUID(),
                entity.getWorkspace().getId(),
                "SHORT_URL",
                entity.getId().toString(),
                eventType,
                1,
                new UrlStateChangedPayloadV1(
                        entity.getId(),
                        entity.getWorkspace().getId(),
                        entity.getShortCode(),
                        state,
                        actorType,
                        actorId
                ),
                java.time.LocalDateTime.now()
        ));
    }

    private void publishCacheInvalidation(ShortUrlEntity entity) {
        domainEventPublisher.publish(new DomainEvent(
                UUID.randomUUID(),
                entity.getWorkspace().getId(),
                "SHORT_URL",
                entity.getId().toString(),
                OutboxEventType.URL_CACHE_INVALIDATION_REQUIRED,
                1,
                new UrlCacheInvalidationPayloadV1(entity.getId(), entity.getShortCode()),
                java.time.LocalDateTime.now()
        ));
    }

    private UserEntity ownerReference(WorkspaceContext workspace) {
        if (workspace.actorType() == AuditActorType.USER) {
            return userRepository.getReferenceById(workspace.actorId());
        }
        UserEntity creator = workspace.workspace().getCreatedBy();
        if (creator == null) {
            throw new ResourceNotFoundException("Workspace owner not found");
        }
        return creator;
    }
}
