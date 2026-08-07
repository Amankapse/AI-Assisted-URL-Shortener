package com.example.urlshortener.url.service;

import com.example.urlshortener.common.exception.QuotaExceededException;
import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.url.config.UrlQuotaProperties;
import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.user.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class UrlQuotaService {
    private final UrlQuotaProperties properties;
    private final ShortUrlRepository repository;
    private final Clock clock;
    private final AppMetrics metrics;

    public UrlQuotaService(UrlQuotaProperties properties,
                           ShortUrlRepository repository,
                           Clock clock,
                           AppMetrics metrics) {
        this.properties = properties;
        this.repository = repository;
        this.clock = clock;
        this.metrics = metrics;
    }

    public void enforceCreateQuota(UserEntity owner, CreateShortUrlRequest request) {
        if (!properties.isEnabled()) {
            return;
        }
        LocalDateTime today = LocalDate.now(clock).atStartOfDay();
        enforce("daily_creations",
                repository.countCreatedByOwnerSince(owner, today),
                properties.getDailyCreationsPerUser(),
                "daily_url_creation_quota_exceeded",
                "Daily URL creation quota exceeded");
        enforce("active_links",
                repository.countActiveByOwner(owner),
                properties.getMaxActiveLinksPerUser(),
                "active_link_quota_exceeded",
                "Maximum active link quota exceeded");
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            enforce("custom_aliases",
                    repository.countCustomAliasesByOwnerSince(owner, today),
                    properties.getDailyCustomAliasesPerUser(),
                    "custom_alias_quota_exceeded",
                    "Daily custom alias quota exceeded");
        }
    }

    private void enforce(String quota, long current, long limit, String code, String message) {
        if (current >= limit) {
            metrics.quota(quota, "rejected");
            throw new QuotaExceededException(code, message);
        }
        metrics.quota(quota, "accepted");
    }
}
