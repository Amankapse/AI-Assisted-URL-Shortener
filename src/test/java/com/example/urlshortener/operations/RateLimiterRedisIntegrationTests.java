package com.example.urlshortener.operations;

import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.common.ratelimit.RateLimitProperties;
import com.example.urlshortener.common.ratelimit.RateLimiterService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RateLimiterRedisIntegrationTests {
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;

    @BeforeAll
    static void startRedisClient() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
    }

    @AfterEach
    void flushRedis() {
        connectionFactory.getConnection().serverCommands().flushAll();
    }

    @AfterAll
    static void stopRedisClient() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void requestsAboveLimitShouldRejectAndIndependentSubjectsShouldNotCollide() {
        RateLimiterService limiter = new RateLimiterService(new StringRedisTemplate(connectionFactory), properties(), new AppMetrics(new SimpleMeterRegistry()));

        assertThat(limiter.check("login", "ip-a:account-a").allowed()).isTrue();
        assertThat(limiter.check("login", "ip-a:account-a").allowed()).isFalse();
        assertThat(limiter.check("login", "ip-a:account-b").allowed()).isTrue();
        assertThat(limiter.check("login", "ip-b:account-a").allowed()).isTrue();
    }

    private RateLimitProperties properties() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setKeySalt("test-salt");
        RateLimitProperties.Policy policy = new RateLimitProperties.Policy();
        policy.setLimit(1);
        policy.setWindow(Duration.ofSeconds(30));
        policy.setFailOpen(false);
        properties.getPolicies().put("login", policy);
        return properties;
    }
}
