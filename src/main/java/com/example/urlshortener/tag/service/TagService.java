package com.example.urlshortener.tag.service;

import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.tag.dto.TagResponse;
import com.example.urlshortener.tag.entity.TagEntity;
import com.example.urlshortener.tag.repository.TagRepository;
import com.example.urlshortener.workspace.entity.WorkspaceEntity;
import com.example.urlshortener.workspace.service.WorkspaceAuthorizationService;
import com.example.urlshortener.workspace.service.WorkspaceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TagService {
    private final TagRepository tagRepository;
    private final TagNormalizer normalizer;
    private final WorkspaceAuthorizationService authorizationService;
    private final AppMetrics metrics;

    public TagService(TagRepository tagRepository,
                      TagNormalizer normalizer,
                      WorkspaceAuthorizationService authorizationService,
                      AppMetrics metrics) {
        this.tagRepository = tagRepository;
        this.normalizer = normalizer;
        this.authorizationService = authorizationService;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> list(UUID workspaceId) {
        authorizationService.requireCampaignReader(workspaceId);
        return tagRepository.findByWorkspaceId(workspaceId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public Set<TagEntity> resolveOrCreate(WorkspaceEntity workspace, List<String> rawTags) {
        List<String> normalized = normalizer.normalize(rawTags);
        LinkedHashSet<TagEntity> tags = new LinkedHashSet<>();
        for (String tag : normalized) {
            tags.add(findOrCreate(workspace, tag));
        }
        return tags;
    }

    public List<String> normalize(List<String> rawTags) {
        return normalizer.normalize(rawTags);
    }

    private TagEntity findOrCreate(WorkspaceEntity workspace, String normalizedName) {
        return tagRepository.findByWorkspaceIdAndNormalizedName(workspace.getId(), normalizedName)
                .orElseGet(() -> {
                    try {
                        TagEntity saved = tagRepository.saveAndFlush(new TagEntity(UUID.randomUUID(), workspace, normalizedName, normalizedName));
                        metrics.tags("create", "success");
                        return saved;
                    } catch (DataIntegrityViolationException ex) {
                        metrics.tags("create", "race_retry");
                        return tagRepository.findByWorkspaceIdAndNormalizedName(workspace.getId(), normalizedName)
                                .orElseThrow(() -> new BadRequestException("tag is temporarily unavailable"));
                    }
                });
    }

    private TagResponse toResponse(TagEntity entity) {
        return new TagResponse(entity.getId(), entity.getName(), entity.getNormalizedName(), entity.getCreatedAt());
    }
}
