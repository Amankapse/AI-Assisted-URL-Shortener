package com.example.urlshortener.redirect.cache;

import com.example.urlshortener.redirect.config.RedirectCacheProperties;
import com.example.urlshortener.redirect.service.RedirectTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RedirectCacheService {
    private static final Logger log = LoggerFactory.getLogger(RedirectCacheService.class);
    private static final String KEY_PREFIX = "url:v1:redirect:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedirectCacheProperties properties;
    private final Clock clock;

    public RedirectCacheService(StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper,
                                RedirectCacheProperties properties,
                                Clock clock) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    public Optional<RedirectTarget> get(String shortCode) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        String key = key(shortCode);
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            RedirectCacheEntry entry = objectMapper.readValue(value, RedirectCacheEntry.class);
            if (!isUsable(entry)) {
                evict(shortCode);
                return Optional.empty();
            }
            return Optional.of(entry.toTarget());
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.warn("Malformed redirect cache entry for key {}", key);
            evict(shortCode);
            return Optional.empty();
        } catch (RedisConnectionFailureException | RedisSystemException | QueryTimeoutException ex) {
            log.warn("Redis read failed for redirect cache key {}", key);
            return Optional.empty();
        }
    }

    public void put(RedirectTarget target) {
        if (!properties.isEnabled()) {
            return;
        }
        Duration ttl = ttlFor(target);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(target.shortCode()), objectMapper.writeValueAsString(RedirectCacheEntry.fromTarget(target)), ttl);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.warn("Unable to serialize redirect cache entry for short code {}", target.shortCode());
        } catch (RedisConnectionFailureException | RedisSystemException | QueryTimeoutException ex) {
            log.warn("Redis write failed for redirect cache key {}", key(target.shortCode()));
        }
    }

    public void evict(String shortCode) {
        if (!properties.isEnabled() || shortCode == null || shortCode.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(key(shortCode));
        } catch (RedisConnectionFailureException | RedisSystemException | QueryTimeoutException ex) {
            log.warn("Redis eviction failed for redirect cache key {}", key(shortCode));
        }
    }

    public String key(String shortCode) {
        return KEY_PREFIX + shortCode;
    }

    public Duration ttlFor(RedirectTarget target) {
        Duration base = target.enabled() && !target.deleted() ? properties.getTtl() : properties.getIneligibleTtl();
        if (target.expiresAt() != null) {
            Duration remaining = Duration.between(LocalDateTime.now(clock), target.expiresAt());
            if (remaining.compareTo(base) < 0) {
                base = remaining;
            }
        }
        if (base.isZero() || base.isNegative()) {
            return Duration.ZERO;
        }
        Duration jitter = properties.getJitter();
        if (!jitter.isZero() && !jitter.isNegative()) {
            long maxJitterMillis = Math.min(jitter.toMillis(), Math.max(0, base.toMillis() - 1));
            if (maxJitterMillis > 0) {
                base = base.minusMillis(ThreadLocalRandom.current().nextLong(maxJitterMillis + 1));
            }
        }
        return base;
    }

    private boolean isUsable(RedirectCacheEntry entry) {
        if (entry == null || entry.schemaVersion() != RedirectCacheEntry.CURRENT_SCHEMA_VERSION) {
            return false;
        }
        if (entry.urlId() == null || entry.shortCode() == null || entry.shortCode().isBlank()) {
            return false;
        }
        if (entry.destinationUrl() == null || entry.destinationUrl().isBlank()) {
            return false;
        }
        if (entry.expiresAt() != null && !entry.expiresAt().isAfter(LocalDateTime.now(clock))) {
            return false;
        }
        return true;
    }
}
