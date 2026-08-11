package com.example.urlshortener.url.search;

import com.example.urlshortener.campaign.dto.CampaignSummaryResponse;
import com.example.urlshortener.campaign.entity.CampaignEntity;
import com.example.urlshortener.campaign.repository.CampaignRepository;
import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.tag.entity.TagEntity;
import com.example.urlshortener.tag.repository.TagRepository;
import com.example.urlshortener.tag.service.TagNormalizer;
import com.example.urlshortener.url.config.UrlOrganizationProperties;
import com.example.urlshortener.url.dto.ShortUrlResponse;
import com.example.urlshortener.url.entity.ShortUrlEntity;
import com.example.urlshortener.url.service.PublicUrlBuilder;
import com.example.urlshortener.workspace.service.WorkspaceContext;
import com.example.urlshortener.workspace.service.WorkspaceContextResolver;
import io.micrometer.core.instrument.Timer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UrlSearchService {
    private final WorkspaceContextResolver workspaceContextResolver;
    private final UrlSearchRepository searchRepository;
    private final CampaignRepository campaignRepository;
    private final TagRepository tagRepository;
    private final TagNormalizer tagNormalizer;
    private final UrlOrganizationProperties properties;
    private final PublicUrlBuilder publicUrlBuilder;
    private final UrlStateResolver stateResolver;
    private final AppMetrics metrics;

    public UrlSearchService(WorkspaceContextResolver workspaceContextResolver,
                            UrlSearchRepository searchRepository,
                            CampaignRepository campaignRepository,
                            TagRepository tagRepository,
                            TagNormalizer tagNormalizer,
                            UrlOrganizationProperties properties,
                            PublicUrlBuilder publicUrlBuilder,
                            UrlStateResolver stateResolver,
                            AppMetrics metrics) {
        this.workspaceContextResolver = workspaceContextResolver;
        this.searchRepository = searchRepository;
        this.campaignRepository = campaignRepository;
        this.tagRepository = tagRepository;
        this.tagNormalizer = tagNormalizer;
        this.properties = properties;
        this.publicUrlBuilder = publicUrlBuilder;
        this.stateResolver = stateResolver;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public Page<ShortUrlResponse> search(UrlSearchCriteria criteria, String workspaceHeader) {
        UrlSearchCriteria normalized = normalize(criteria);
        WorkspaceContext workspace = workspaceContextResolver.resolveForLinkReader(workspaceHeader);
        Timer.Sample sample = metrics.startTimer();
        try {
            List<ShortUrlEntity> urls = searchRepository.search(workspace.workspaceId(), normalized);
            long total = searchRepository.count(workspace.workspaceId(), normalized);
            Map<UUID, List<String>> tags = tagsByUrl(urls.stream().map(ShortUrlEntity::getId).toList());
            Map<UUID, CampaignSummaryResponse> campaigns = campaignsById(workspace.workspaceId(), urls);
            List<ShortUrlResponse> content = urls.stream()
                    .map(url -> toResponse(url, campaignSummary(url, campaigns), tags.getOrDefault(url.getId(), List.of())))
                    .toList();
            metrics.urlSearch("success");
            metrics.recordUrlSearch(sample, "success");
            return new PageImpl<>(content, PageRequest.of(normalized.page(), normalized.size()), total);
        } catch (RuntimeException ex) {
            metrics.urlSearch("failure");
            metrics.recordUrlSearch(sample, "failure");
            throw ex;
        }
    }

    private UrlSearchCriteria normalize(UrlSearchCriteria criteria) {
        int size = criteria.size() <= 0 ? properties.getDefaultPageSize() : criteria.size();
        if (size > properties.getMaxPageSize()) {
            throw new BadRequestException("size may not exceed " + properties.getMaxPageSize());
        }
        if (criteria.page() < 0) {
            throw new BadRequestException("page must be zero or greater");
        }
        String q = criteria.q();
        if (q != null) {
            q = q.trim();
            if (q.isBlank()) {
                q = null;
            } else if (q.length() > properties.getSearchQueryMaxLength()) {
                throw new BadRequestException("q may not exceed " + properties.getSearchQueryMaxLength() + " characters");
            }
        }
        String tag = criteria.tag() == null || criteria.tag().isBlank() ? null : tagNormalizer.normalizeOne(criteria.tag());
        return new UrlSearchCriteria(q, criteria.state(), criteria.campaignId(), tag, criteria.createdFrom(), criteria.createdTo(),
                criteria.expiresBefore(), criteria.expiresAfter(), criteria.customAlias(), criteria.sort(), criteria.page(), size);
    }

    private Map<UUID, List<String>> tagsByUrl(List<UUID> urlIds) {
        if (urlIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<String>> result = new LinkedHashMap<>();
        tagRepository.findTagsByUrlIds(urlIds).forEach(row -> {
            UUID urlId = (UUID) row[0];
            TagEntity tag = (TagEntity) row[1];
            result.compute(urlId, (ignored, values) -> {
                List<String> next = values == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(values);
                next.add(tag.getNormalizedName());
                next.sort(Comparator.naturalOrder());
                return List.copyOf(next);
            });
        });
        return result;
    }

    private Map<UUID, CampaignSummaryResponse> campaignsById(UUID workspaceId, List<ShortUrlEntity> urls) {
        List<UUID> campaignIds = urls.stream()
                .map(this::campaignId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (campaignIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, CampaignSummaryResponse> result = new LinkedHashMap<>();
        for (CampaignEntity campaign : campaignRepository.findByWorkspaceIdAndIdIn(workspaceId, campaignIds)) {
            result.put(campaign.getId(), new CampaignSummaryResponse(campaign.getId(), campaign.getName(), campaign.getNormalizedName()));
        }
        return result;
    }

    private UUID campaignId(ShortUrlEntity entity) {
        return entity.getCampaign() == null ? null : entity.getCampaign().getId();
    }

    private CampaignSummaryResponse campaignSummary(ShortUrlEntity entity, Map<UUID, CampaignSummaryResponse> campaigns) {
        UUID campaignId = campaignId(entity);
        return campaignId == null ? null : campaigns.get(campaignId);
    }

    private ShortUrlResponse toResponse(ShortUrlEntity entity, CampaignSummaryResponse campaign, List<String> tags) {
        return new ShortUrlResponse(entity.getId(), entity.getShortCode(), publicUrlBuilder.shortUrl(entity.getShortCode()),
                entity.getCustomAlias(), entity.getOriginalUrl(), entity.getCreatedAt(), entity.getExpiresAt(), entity.isEnabled(),
                entity.getClickCount(), entity.getVersion(), stateResolver.state(entity), campaign, tags);
    }
}
