package com.example.urlshortener.idempotency.service;

import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.idempotency.config.IdempotencyProperties;
import com.example.urlshortener.idempotency.entity.IdempotencyRecord;
import com.example.urlshortener.idempotency.repository.IdempotencyRecordRepository;
import com.example.urlshortener.user.service.CurrentOwnerProvider;
import com.example.urlshortener.user.service.OwnerIdentity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Service
public class IdempotencyService {
    private static final Pattern SAFE_KEY = Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");

    private final IdempotencyRecordRepository repository;
    private final IdempotencyProperties properties;
    private final CurrentOwnerProvider currentOwnerProvider;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final AppMetrics metrics;

    public IdempotencyService(IdempotencyRecordRepository repository,
                              IdempotencyProperties properties,
                              CurrentOwnerProvider currentOwnerProvider,
                              ObjectMapper objectMapper,
                              Clock clock,
                              AppMetrics metrics) {
        this.repository = repository;
        this.properties = properties;
        this.currentOwnerProvider = currentOwnerProvider;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Transactional
    public <T> T execute(String idempotencyKey, Object request, Supplier<T> operation, Class<T> responseType) {
        OwnerIdentity owner = currentOwnerProvider.getCurrentOwner();
        return execute(idempotencyKey, "user:" + owner.userId(), request, operation, responseType);
    }

    @Transactional
    public <T> T execute(String idempotencyKey, String scope, Object request, Supplier<T> operation, Class<T> responseType) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return operation.get();
        }
        String key = validateKey(idempotencyKey);
        String fingerprint = fingerprint(request);
        LocalDateTime now = LocalDateTime.now(clock);

        boolean inserted = tryInsert(scope, key, fingerprint, now);
        if (!inserted) {
            IdempotencyRecord existing = repository.findLocked(scope, key)
                    .orElseThrow(() -> new IdempotencyConflictException("Idempotency state is temporarily unavailable."));
            if (existing.getExpiresAt().isBefore(now)) {
                repository.delete(existing);
                repository.flush();
                inserted = tryInsert(scope, key, fingerprint, now);
                if (!inserted) {
                    existing = repository.findLocked(scope, key)
                            .orElseThrow(() -> new IdempotencyConflictException("Idempotency state is temporarily unavailable."));
                }
            }
            if (!inserted) {
                return replayOrReject(existing, fingerprint, responseType);
            }
        }

        T response = operation.get();
        IdempotencyRecord record = repository.findLocked(scope, key)
                .orElseThrow(() -> new IdempotencyConflictException("Idempotency state is temporarily unavailable."));
        record.setStatus("SUCCEEDED");
        record.setResponseStatus(201);
        record.setResponseBody(serialize(response));
        record.setCompletedAt(now);
        metrics.idempotency("stored");
        return response;
    }

    @Transactional
    public long cleanupExpired() {
        return repository.deleteByExpiresAtBefore(LocalDateTime.now(clock));
    }

    private boolean tryInsert(String scope, String key, String fingerprint, LocalDateTime now) {
        int inserted = repository.insertInProgress(
                UUID.randomUUID(),
                scope,
                key,
                fingerprint,
                now,
                now.plus(properties.getRetention())
        );
        if (inserted == 1) {
            metrics.idempotency("started");
            return true;
        }
        return false;
    }

    private <T> T replayOrReject(IdempotencyRecord record, String fingerprint, Class<T> responseType) {
        if (!record.getRequestFingerprint().equals(fingerprint)) {
            metrics.idempotency("fingerprint_conflict");
            throw new IdempotencyConflictException("Idempotency-Key was already used with a different request.");
        }
        if (!"SUCCEEDED".equals(record.getStatus()) || record.getResponseBody() == null) {
            metrics.idempotency("in_progress_conflict");
            throw new IdempotencyConflictException("Idempotent request is still processing.");
        }
        try {
            metrics.idempotency("replayed");
            return objectMapper.readValue(record.getResponseBody(), responseType);
        } catch (JsonProcessingException ex) {
            metrics.idempotency("replay_failure");
            throw new IdempotencyConflictException("Stored idempotent response is unavailable.");
        }
    }

    private String validateKey(String idempotencyKey) {
        String trimmed = idempotencyKey.trim();
        if (!SAFE_KEY.matcher(trimmed).matches()) {
            throw new BadRequestException("Idempotency-Key contains unsupported characters.");
        }
        return trimmed;
    }

    private String fingerprint(Object request) {
        try {
            JsonNode node = objectMapper.valueToTree(request);
            byte[] canonical = objectMapper.writeValueAsBytes(node);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create idempotency request fingerprint", ex);
        }
    }

    private String serialize(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to store idempotent response", ex);
        }
    }
}
