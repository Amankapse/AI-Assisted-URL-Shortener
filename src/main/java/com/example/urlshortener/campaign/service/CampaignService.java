package com.example.urlshortener.campaign.service;

import com.example.urlshortener.audit.entity.AuditAction;
import com.example.urlshortener.audit.entity.AuditActorType;
import com.example.urlshortener.audit.entity.AuditResourceType;
import com.example.urlshortener.audit.service.AuditService;
import com.example.urlshortener.campaign.dto.CampaignCreateRequest;
import com.example.urlshortener.campaign.dto.CampaignResponse;
import com.example.urlshortener.campaign.dto.CampaignUpdateRequest;
import com.example.urlshortener.campaign.entity.CampaignEntity;
import com.example.urlshortener.campaign.repository.CampaignRepository;
import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.repository.UserRepository;
import com.example.urlshortener.workspace.service.WorkspaceAuthorizationService;
import com.example.urlshortener.workspace.service.WorkspaceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CampaignService {
    private final CampaignRepository campaignRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final CampaignNameNormalizer normalizer;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final AppMetrics metrics;

    public CampaignService(CampaignRepository campaignRepository,
                           WorkspaceAuthorizationService authorizationService,
                           CampaignNameNormalizer normalizer,
                           UserRepository userRepository,
                           AuditService auditService,
                           AppMetrics metrics) {
        this.campaignRepository = campaignRepository;
        this.authorizationService = authorizationService;
        this.normalizer = normalizer;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.metrics = metrics;
    }

    @Transactional
    public CampaignResponse create(UUID workspaceId, CampaignCreateRequest request) {
        WorkspaceContext workspace = authorizationService.requireCampaignWriter(workspaceId);
        UserEntity creator = userRepository.getReferenceById(workspace.actorId());
        String name = normalizer.displayName(request.getName());
        String normalizedName = normalizer.normalize(request.getName());
        String description = normalizer.description(request.getDescription());
        if (campaignRepository.existsActiveByWorkspaceIdAndNormalizedName(workspaceId, normalizedName)) {
            metrics.campaign("create", "duplicate");
            throw new BadRequestException("campaign name is already in use");
        }
        try {
            CampaignEntity saved = campaignRepository.save(new CampaignEntity(UUID.randomUUID(), workspace.workspace(), name, normalizedName, description, creator));
            auditService.recordUser(AuditAction.CAMPAIGN_CREATED, workspaceId, workspace.actorId(), AuditResourceType.CAMPAIGN, saved.getId(), auditService.campaignMetadata("create", normalizedName, description != null));
            metrics.campaign("create", "success");
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            metrics.campaign("create", "duplicate");
            throw new BadRequestException("campaign name is already in use");
        }
    }

    @Transactional(readOnly = true)
    public CampaignResponse get(UUID workspaceId, UUID campaignId) {
        authorizationService.requireCampaignReader(workspaceId);
        return campaignRepository.findActiveByIdAndWorkspaceId(campaignId, workspaceId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> list(UUID workspaceId) {
        authorizationService.requireCampaignReader(workspaceId);
        return campaignRepository.findActiveByWorkspaceId(workspaceId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CampaignResponse update(UUID workspaceId, UUID campaignId, CampaignUpdateRequest request) {
        WorkspaceContext workspace = authorizationService.requireCampaignWriter(workspaceId);
        CampaignEntity entity = campaignRepository.findActiveByIdAndWorkspaceId(campaignId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        String name = normalizer.displayName(request.getName());
        String normalizedName = normalizer.normalize(request.getName());
        String description = normalizer.description(request.getDescription());
        if (!entity.getNormalizedName().equals(normalizedName)
                && campaignRepository.existsActiveByWorkspaceIdAndNormalizedName(workspaceId, normalizedName)) {
            metrics.campaign("update", "duplicate");
            throw new BadRequestException("campaign name is already in use");
        }
        entity.setName(name);
        entity.setNormalizedName(normalizedName);
        entity.setDescription(description);
        CampaignEntity saved = campaignRepository.save(entity);
        auditService.recordUser(AuditAction.CAMPAIGN_UPDATED, workspaceId, workspace.actorId(), AuditResourceType.CAMPAIGN, saved.getId(), auditService.campaignMetadata("update", normalizedName, description != null));
        metrics.campaign("update", "success");
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID workspaceId, UUID campaignId) {
        WorkspaceContext workspace = authorizationService.requireCampaignDeleter(workspaceId);
        CampaignEntity entity = campaignRepository.findActiveByIdAndWorkspaceId(campaignId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        campaignRepository.detachUrls(workspaceId, campaignId);
        entity.setDeleted(true);
        campaignRepository.save(entity);
        auditService.recordUser(AuditAction.CAMPAIGN_DELETED, workspaceId, workspace.actorId(), AuditResourceType.CAMPAIGN, entity.getId(), auditService.campaignMetadata("delete", entity.getNormalizedName(), entity.getDescription() != null));
        metrics.campaign("delete", "success");
    }

    public CampaignResponse toResponse(CampaignEntity entity) {
        return new CampaignResponse(
                entity.getId(),
                entity.getWorkspace().getId(),
                entity.getName(),
                entity.getNormalizedName(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
