package com.example.urlshortener.analytics.service;

import com.example.urlshortener.analytics.config.AnalyticsProperties;
import com.example.urlshortener.redirect.service.RedirectTarget;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.TransientDataAccessResourceException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ClickAnalyticsServiceTests {
    @Test
    void privacySanitizerShouldHashIpAndStoreOnlySafeHeaderDerivatives() {
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setIpHashPepper("test-pepper");
        ClickPrivacySanitizer sanitizer = new ClickPrivacySanitizer(properties);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.10");
        when(request.getHeader("Referer")).thenReturn("https://referrer.example/path?token=secret");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (iPhone)");
        when(request.getHeader("X-Correlation-ID")).thenReturn("corr-123!bad");

        assertThat(sanitizer.ipHash(request)).isNotEqualTo("203.0.113.10").hasSize(64);
        assertThat(sanitizer.referrerHost(request)).isEqualTo("referrer.example");
        assertThat(sanitizer.userAgentCategory(request)).isEqualTo("mobile");
        assertThat(sanitizer.correlationId(request)).isEqualTo("corr-123bad");
    }

    @Test
    void publisherShouldDropOnQueueOverloadAndRetryTransientBatchFailure() {
        AnalyticsProperties properties = new AnalyticsProperties();
        properties.setIpHashPepper("test-pepper");
        properties.setQueueCapacity(1);
        properties.setOfferTimeout(Duration.ZERO);
        properties.setBatchSize(10);
        properties.setRetryCount(1);
        ClickPrivacySanitizer sanitizer = new ClickPrivacySanitizer(properties);
        ClickAnalyticsWriter writer = mock(ClickAnalyticsWriter.class);
        AnalyticsCounters counters = new AnalyticsCounters();
        ClickAnalyticsPublisher publisher = new ClickAnalyticsPublisher(
                properties,
                sanitizer,
                writer,
                counters,
                Clock.fixed(Instant.parse("2026-08-07T00:00:00Z"), ZoneOffset.UTC)
        );
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.10");
        RedirectTarget target = new RedirectTarget(UUID.randomUUID(), "abc1234", "https://example.com", true, null, false);

        publisher.publish(target, request);
        publisher.publish(target, request);

        assertThat(counters.getAcceptedEvents()).isEqualTo(1);
        assertThat(counters.getDroppedEvents()).isEqualTo(1);

        when(writer.persistBatch(anyList()))
                .thenThrow(new TransientDataAccessResourceException("temporary"))
                .thenReturn(1);
        publisher.flushOnce();

        assertThat(counters.getBatchRetries()).isEqualTo(1);
        assertThat(counters.getPersistedEvents()).isEqualTo(1);
    }
}
