package com.example.urlshortener.idempotency.repository;

import com.example.urlshortener.idempotency.entity.IdempotencyRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from IdempotencyRecord record where record.scope = :scope and record.idempotencyKey = :key")
    Optional<IdempotencyRecord> findLocked(@Param("scope") String scope, @Param("key") String key);

    @Modifying
    @Query(value = """
            INSERT INTO idempotency_keys (id, scope, idempotency_key, request_fingerprint, status, created_at, expires_at)
            VALUES (:id, :scope, :key, :fingerprint, 'IN_PROGRESS', :createdAt, :expiresAt)
            ON CONFLICT (scope, idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertInProgress(@Param("id") UUID id,
                         @Param("scope") String scope,
                         @Param("key") String key,
                         @Param("fingerprint") String fingerprint,
                         @Param("createdAt") LocalDateTime createdAt,
                         @Param("expiresAt") LocalDateTime expiresAt);

    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
