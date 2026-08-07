package com.example.urlshortener.operations;

import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.common.ratelimit.RateLimitProperties;
import com.example.urlshortener.common.ratelimit.RateLimitResult;
import com.example.urlshortener.common.ratelimit.RateLimiterService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimiterServiceTests {
    @Test
    void redisFailureShouldFollowFailClosedPolicy() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(), anyList(), any(String.class), any(String.class)))
                .thenThrow(new RedisConnectionFailureException("down"));
        RateLimiterService limiter = new RateLimiterService(redisTemplate, properties(false), metrics());

        RateLimitResult result = limiter.check("login", "subject");

        assertThat(result.allowed()).isFalse();
        assertThat(result.retryAfterSeconds()).isPositive();
    }

    @Test
    void redisFailureShouldFollowFailOpenPolicy() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(), anyList(), any(String.class), any(String.class)))
                .thenThrow(new RedisConnectionFailureException("down"));
        RateLimiterService limiter = new RateLimiterService(redisTemplate, properties(true), metrics());

        RateLimitResult result = limiter.check("redirect", "subject");

        assertThat(result.allowed()).isTrue();
        assertThat(result.degraded()).isTrue();
    }

    @Test
    void digestShouldNotExposeSubject() {
        RateLimiterService limiter = new RateLimiterService(mock(StringRedisTemplate.class), properties(true), metrics());

        assertThat(limiter.digest("user@example.com")).doesNotContain("user", "example", "@");
    }

    private RateLimitProperties properties(boolean failOpen) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setKeySalt("test-salt");
        RateLimitProperties.Policy policy = new RateLimitProperties.Policy();
        policy.setLimit(1);
        policy.setWindow(Duration.ofSeconds(30));
        policy.setFailOpen(failOpen);
        properties.getPolicies().put("login", policy);
        properties.getPolicies().put("redirect", policy);
        return properties;
    }

    private AppMetrics metrics() {
        return new AppMetrics(new SimpleMeterRegistry());
    }
}
