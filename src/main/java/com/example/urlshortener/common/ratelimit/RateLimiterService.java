package com.example.urlshortener.common.ratelimit;

import com.example.urlshortener.common.metrics.AppMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

@Service
public class RateLimiterService {
    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
    private static final DefaultRedisScript<List> SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            local ttl = redis.call('PTTL', KEYS[1])
            if current > tonumber(ARGV[1]) then
              return {0, ttl}
            end
            return {1, ttl}
            """, List.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;
    private final AppMetrics metrics;

    public RateLimiterService(StringRedisTemplate redisTemplate, RateLimitProperties properties, AppMetrics metrics) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.metrics = metrics;
    }

    public RateLimitResult check(String limiter, String subject) {
        if (!properties.isEnabled()) {
            metrics.rateLimit(limiter, "accepted");
            return RateLimitResult.allow();
        }
        RateLimitProperties.Policy policy = properties.policy(limiter);
        if (policy.getLimit() <= 0 || policy.getWindow().isZero() || policy.getWindow().isNegative()) {
            metrics.rateLimit(limiter, "accepted");
            return RateLimitResult.allow();
        }
        String key = "rl:v1:" + limiter + ":" + digest(limiter + ":" + subject);
        try {
            List<?> result = redisTemplate.execute(
                    SCRIPT,
                    List.of(key),
                    String.valueOf(policy.getLimit()),
                    String.valueOf(policy.getWindow().toMillis())
            );
            boolean allowed = number(result, 0, 1) == 1;
            long ttlMillis = number(result, 1, policy.getWindow().toMillis());
            if (allowed) {
                metrics.rateLimit(limiter, "accepted");
                return RateLimitResult.allow();
            }
            metrics.rateLimit(limiter, "rejected");
            return RateLimitResult.reject(Duration.ofMillis(Math.max(1000, ttlMillis)).toSeconds());
        } catch (RedisConnectionFailureException | RedisSystemException | QueryTimeoutException ex) {
            metrics.rateLimit(limiter, "failure");
            if (policy.isFailOpen()) {
                log.warn("Rate limiter Redis failure for limiter {}; failing open", limiter);
                return RateLimitResult.degradedAllow();
            }
            log.warn("Rate limiter Redis failure for limiter {}; failing closed", limiter);
            metrics.rateLimit(limiter, "rejected");
            return RateLimitResult.reject(policy.getWindow().toSeconds());
        }
    }

    public void enforce(String limiter, String subject) {
        RateLimitResult result = check(limiter, subject);
        if (!result.allowed()) {
            throw new RateLimitExceededException(limiter, result.retryAfterSeconds());
        }
    }

    public String digest(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = properties.getKeySalt() + ":" + value;
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create rate limit key digest", ex);
        }
    }

    private long number(List<?> result, int index, long fallback) {
        if (result == null || result.size() <= index || result.get(index) == null) {
            return fallback;
        }
        Object value = result.get(index);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }
}
