package com.example.urlshortener.redirect.service;

import com.example.urlshortener.analytics.service.ClickEventPublisher;
import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.common.ratelimit.RateLimiterService;
import com.example.urlshortener.common.web.ClientIpResolver;
import com.example.urlshortener.redirect.cache.RedirectCacheService;
import com.example.urlshortener.redirect.cache.SingleFlightRedirectLoader;
import com.example.urlshortener.url.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedirectServiceTests {

    private UrlService urlService;
    private RedirectCacheService cacheService;
    private SingleFlightRedirectLoader singleFlightLoader;
    private ClickEventPublisher analyticsPublisher;
    private HttpServletRequest request;
    private RedirectService redirectService;

    @BeforeEach
    void setUp() {
        urlService = Mockito.mock(UrlService.class);
        cacheService = Mockito.mock(RedirectCacheService.class);
        singleFlightLoader = Mockito.mock(SingleFlightRedirectLoader.class);
        analyticsPublisher = Mockito.mock(ClickEventPublisher.class);
        request = Mockito.mock(HttpServletRequest.class);
        RateLimiterService rateLimiter = Mockito.mock(RateLimiterService.class);
        ClientIpResolver clientIpResolver = Mockito.mock(ClientIpResolver.class);
        AppMetrics metrics = Mockito.mock(AppMetrics.class);
        when(clientIpResolver.resolve(request)).thenReturn("127.0.0.1");
        redirectService = new RedirectService(
                urlService,
                cacheService,
                singleFlightLoader,
                analyticsPublisher,
                Clock.fixed(java.time.Instant.parse("2026-08-07T00:00:00Z"), ZoneOffset.UTC),
                rateLimiter,
                clientIpResolver,
                metrics
        );
    }

    @Test
    void activeCacheHitShouldResolveAndPublishAnalytics() {
        RedirectTarget target = target("abc1234", LocalDateTime.of(2026, 8, 8, 0, 0), true);
        when(cacheService.get("abc1234")).thenReturn(Optional.of(target));

        RedirectTarget resolved = redirectService.resolve("abc1234", request);

        assertThat(resolved.destinationUrl()).isEqualTo("https://example.com");
        verify(urlService, never()).resolveRedirectTarget("abc1234");
        verify(analyticsPublisher).publish(target, request);
    }

    @Test
    void cacheMissShouldUseSingleFlightLoader() {
        RedirectTarget target = target("abc1234", LocalDateTime.of(2026, 8, 8, 0, 0), true);
        when(cacheService.get("abc1234")).thenReturn(Optional.empty());
        when(singleFlightLoader.load(Mockito.eq("abc1234"), any(), any())).thenAnswer(invocation -> {
            Supplier<RedirectTarget> leader = invocation.getArgument(1);
            when(urlService.resolveRedirectTarget("abc1234")).thenReturn(target);
            return leader.get();
        });

        RedirectTarget resolved = redirectService.resolve("abc1234", request);

        assertThat(resolved).isEqualTo(target);
        verify(cacheService).put(target);
        verify(analyticsPublisher).publish(target, request);
    }

    @Test
    void unknownShortCodeShouldPropagateNotFound() {
        when(cacheService.get("missing")).thenReturn(Optional.empty());
        when(singleFlightLoader.load(Mockito.eq("missing"), any(), any()))
                .thenThrow(new ResourceNotFoundException("Short URL not found"));

        assertThatThrownBy(() -> redirectService.resolve("missing", request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(analyticsPublisher, never()).publish(any(), any());
    }

    @Test
    void disabledUrlShouldNotRedirectOrPublishAnalytics() {
        RedirectTarget target = target("abc1234", LocalDateTime.of(2026, 8, 8, 0, 0), false);
        when(cacheService.get("abc1234")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> redirectService.resolve("abc1234", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("disabled");
        verify(analyticsPublisher, never()).publish(any(), any());
    }

    @Test
    void expiredUrlShouldNotRedirectOrPublishAnalytics() {
        RedirectTarget target = target("abc1234", LocalDateTime.of(2026, 8, 6, 0, 0), true);
        when(cacheService.get("abc1234")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> redirectService.resolve("abc1234", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
        verify(analyticsPublisher, never()).publish(any(), any());
    }

    @Test
    void blockedUrlShouldReturnSafeNotFoundAndNotPublishAnalytics() {
        RedirectTarget target = new RedirectTarget(UUID.randomUUID(), "abc1234", "https://example.com", true, LocalDateTime.of(2026, 8, 8, 0, 0), false, true);
        when(cacheService.get("abc1234")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> redirectService.resolve("abc1234", request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(analyticsPublisher, never()).publish(any(), any());
    }

    private RedirectTarget target(String shortCode, LocalDateTime expiresAt, boolean enabled) {
        return new RedirectTarget(UUID.randomUUID(), shortCode, "https://example.com", enabled, expiresAt, false);
    }
}
