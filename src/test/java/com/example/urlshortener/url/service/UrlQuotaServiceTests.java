package com.example.urlshortener.url.service;

import com.example.urlshortener.common.exception.QuotaExceededException;
import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.url.config.UrlQuotaProperties;
import com.example.urlshortener.url.dto.CreateShortUrlRequest;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.entity.UserRole;
import com.example.urlshortener.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UrlQuotaServiceTests {
    private UrlQuotaProperties properties;
    private ShortUrlRepository repository;
    private AppMetrics metrics;
    private UrlQuotaService quotaService;
    private UserEntity owner;
    private CreateShortUrlRequest request;

    @BeforeEach
    void setUp() {
        properties = new UrlQuotaProperties();
        repository = Mockito.mock(ShortUrlRepository.class);
        metrics = Mockito.mock(AppMetrics.class);
        quotaService = new UrlQuotaService(
                properties,
                repository,
                Clock.fixed(Instant.parse("2026-08-07T10:00:00Z"), ZoneOffset.UTC),
                metrics
        );
        owner = new UserEntity(UUID.randomUUID(), "owner@example.com", "hash", UserRole.USER, UserStatus.ACTIVE);
        request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://example.com");
    }

    @Test
    void shouldAllowRequestBelowConfiguredQuotas() {
        properties.setDailyCreationsPerUser(2);
        properties.setMaxActiveLinksPerUser(2);
        when(repository.countCreatedByOwnerSince(any(), any())).thenReturn(1L);
        when(repository.countActiveByOwner(owner)).thenReturn(1L);

        quotaService.enforceCreateQuota(owner, request);

        verify(metrics).quota("daily_creations", "accepted");
        verify(metrics).quota("active_links", "accepted");
    }

    @Test
    void shouldRejectDailyCreationQuotaExhaustion() {
        properties.setDailyCreationsPerUser(1);
        when(repository.countCreatedByOwnerSince(any(), any())).thenReturn(1L);

        assertThatThrownBy(() -> quotaService.enforceCreateQuota(owner, request))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("Daily URL creation quota exceeded");
        verify(metrics).quota("daily_creations", "rejected");
    }

    @Test
    void shouldRejectCustomAliasQuotaOnlyForAliasRequests() {
        properties.setDailyCustomAliasesPerUser(1);
        request.setCustomAlias("campaign");
        when(repository.countCreatedByOwnerSince(any(), any())).thenReturn(0L);
        when(repository.countActiveByOwner(owner)).thenReturn(0L);
        when(repository.countCustomAliasesByOwnerSince(any(), any())).thenReturn(1L);

        assertThatThrownBy(() -> quotaService.enforceCreateQuota(owner, request))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("Daily custom alias quota exceeded");
    }

    @Test
    void disabledQuotaShouldNotQueryRepository() {
        properties.setEnabled(false);

        quotaService.enforceCreateQuota(owner, request);

        verify(repository, never()).countCreatedByOwnerSince(any(), any());
        verify(repository, never()).countActiveByOwner(any());
    }
}
