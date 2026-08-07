package com.example.urlshortener.redirect.cache;

import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.redirect.config.RedirectCacheProperties;
import com.example.urlshortener.redirect.service.RedirectTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedirectCacheServiceTests {
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedirectCacheProperties properties;
    private RedirectCacheService cacheService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        properties = new RedirectCacheProperties();
        properties.setJitter(Duration.ZERO);
        cacheService = new RedirectCacheService(
                redisTemplate,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                properties,
                Clock.fixed(Instant.parse("2026-08-07T00:00:00Z"), ZoneOffset.UTC),
                mock(AppMetrics.class)
        );
    }

    @Test
    void cacheHitShouldReturnValidTarget() throws Exception {
        RedirectTarget target = target(LocalDateTime.of(2026, 8, 8, 0, 0), true);
        when(valueOperations.get("url:v1:redirect:abc1234"))
                .thenReturn(new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(RedirectCacheEntry.fromTarget(target)));

        Optional<RedirectTarget> cached = cacheService.get("abc1234");

        assertThat(cached).contains(target);
    }

    @Test
    void malformedOrUnsupportedCachedValueShouldEvictAndMiss() throws Exception {
        when(valueOperations.get("url:v1:redirect:abc1234")).thenReturn("{");
        assertThat(cacheService.get("abc1234")).isEmpty();
        verify(redisTemplate).delete("url:v1:redirect:abc1234");

        reset(redisTemplate, valueOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RedirectCacheEntry unsupported = new RedirectCacheEntry(99, UUID.randomUUID(), "abc1234", "https://example.com", true, LocalDateTime.of(2026, 8, 8, 0, 0), false);
        when(valueOperations.get("url:v1:redirect:abc1234"))
                .thenReturn(new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(unsupported));

        assertThat(cacheService.get("abc1234")).isEmpty();
        verify(redisTemplate).delete("url:v1:redirect:abc1234");
    }

    @Test
    void redisReadWriteAndEvictionFailuresShouldNotEscape() {
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        assertThat(cacheService.get("abc1234")).isEmpty();

        doThrow(new RedisConnectionFailureException("down")).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        cacheService.put(target(LocalDateTime.of(2026, 8, 8, 0, 0), true));

        doThrow(new RedisConnectionFailureException("down")).when(redisTemplate).delete(anyString());
        cacheService.evict("abc1234");
    }

    @Test
    void ttlShouldBeBoundedByUrlExpirationAndUseShortTtlForDisabledTarget() {
        properties.setTtl(Duration.ofHours(1));
        RedirectTarget expiring = target(LocalDateTime.of(2026, 8, 7, 0, 5), true);
        assertThat(cacheService.ttlFor(expiring)).isEqualTo(Duration.ofMinutes(5));

        properties.setIneligibleTtl(Duration.ofSeconds(20));
        assertThat(cacheService.ttlFor(target(LocalDateTime.of(2026, 8, 8, 0, 0), false))).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void blockedTargetShouldUseIneligibleTtlAndRoundTripThroughCacheDto() {
        properties.setIneligibleTtl(Duration.ofSeconds(20));
        RedirectTarget blocked = new RedirectTarget(UUID.randomUUID(), "abc1234", "https://example.com", true, LocalDateTime.of(2026, 8, 8, 0, 0), false, true);

        assertThat(cacheService.ttlFor(blocked)).isEqualTo(Duration.ofSeconds(20));
        assertThat(RedirectCacheEntry.fromTarget(blocked).toTarget().blocked()).isTrue();
    }

    @Test
    void cacheKeyShouldContainNoSensitiveData() {
        assertThat(cacheService.key("abc1234")).isEqualTo("url:v1:redirect:abc1234");
        assertThat(cacheService.key("abc1234")).doesNotContain("Bearer", "refresh", "owner", "@");
    }

    private RedirectTarget target(LocalDateTime expiresAt, boolean enabled) {
        return new RedirectTarget(UUID.randomUUID(), "abc1234", "https://example.com", enabled, expiresAt, false);
    }
}
