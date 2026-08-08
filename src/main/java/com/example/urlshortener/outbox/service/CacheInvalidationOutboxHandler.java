package com.example.urlshortener.outbox.service;

import com.example.urlshortener.outbox.domain.OutboxEventHandler;
import com.example.urlshortener.outbox.domain.OutboxEventRecord;
import com.example.urlshortener.outbox.domain.OutboxEventType;
import com.example.urlshortener.outbox.domain.OutboxHandlingException;
import com.example.urlshortener.outbox.payload.UrlCacheInvalidationPayloadV1;
import com.example.urlshortener.redirect.cache.RedirectCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationOutboxHandler implements OutboxEventHandler {
    private final RedirectCacheService cacheService;
    private final ObjectMapper objectMapper;

    public CacheInvalidationOutboxHandler(RedirectCacheService cacheService, ObjectMapper objectMapper) {
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String handlerName() {
        return "cache-invalidation";
    }

    @Override
    public boolean supports(OutboxEventType eventType, int eventVersion) {
        return eventType == OutboxEventType.URL_CACHE_INVALIDATION_REQUIRED && eventVersion == 1;
    }

    @Override
    public void handle(OutboxEventRecord event) {
        try {
            UrlCacheInvalidationPayloadV1 payload = objectMapper.readValue(event.payload(), UrlCacheInvalidationPayloadV1.class);
            if (payload.shortCode() == null || payload.shortCode().isBlank()) {
                throw OutboxHandlingException.permanentFailure("malformed_payload", null);
            }
            cacheService.evictRequired(payload.shortCode());
        } catch (RedisConnectionFailureException | RedisSystemException | QueryTimeoutException ex) {
            throw OutboxHandlingException.transientFailure("redis_unavailable", ex);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw OutboxHandlingException.permanentFailure("malformed_payload", ex);
        }
    }
}
