package com.example.urlshortener.outbox.service;

import com.example.urlshortener.outbox.config.OutboxProperties;
import com.example.urlshortener.outbox.domain.DomainEvent;
import com.example.urlshortener.outbox.domain.OutboxEventRecord;
import com.example.urlshortener.outbox.domain.OutboxEventType;
import com.example.urlshortener.outbox.domain.OutboxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class OutboxEventStore {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxProperties properties;
    private final Clock clock;

    private final RowMapper<OutboxEventRecord> mapper = (rs, rowNum) -> new OutboxEventRecord(
            rs.getObject("id", UUID.class),
            rs.getObject("workspace_id", UUID.class),
            rs.getString("aggregate_type"),
            rs.getString("aggregate_id"),
            OutboxEventType.valueOf(rs.getString("event_type")),
            rs.getInt("event_version"),
            rs.getString("payload"),
            OutboxStatus.valueOf(rs.getString("status")),
            rs.getInt("attempt_count"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("next_attempt_at").toLocalDateTime(),
            rs.getTimestamp("claimed_at") == null ? null : rs.getTimestamp("claimed_at").toLocalDateTime(),
            rs.getString("claimed_by"),
            rs.getTimestamp("processed_at") == null ? null : rs.getTimestamp("processed_at").toLocalDateTime(),
            rs.getString("last_error_code")
    );

    public OutboxEventStore(JdbcTemplate jdbcTemplate,
                            ObjectMapper objectMapper,
                            OutboxProperties properties,
                            Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    public void insert(DomainEvent event) {
        String payload = serialize(event.payload());
        int bytes = payload.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > properties.getMaxPayloadBytes()) {
            throw new IllegalStateException("Outbox payload exceeds configured size limit");
        }
        LocalDateTime createdAt = event.occurredAt() == null ? LocalDateTime.now(clock) : event.occurredAt();
        jdbcTemplate.update("""
                        INSERT INTO outbox_events
                        (id, workspace_id, aggregate_type, aggregate_id, event_type, event_version, payload, status,
                         attempt_count, created_at, next_attempt_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, 'PENDING', 0, ?, ?)
                        """,
                event.eventId(),
                event.workspaceId(),
                event.aggregateType(),
                event.aggregateId(),
                event.eventType().name(),
                event.eventVersion(),
                payload,
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(createdAt)
        );
    }

    @Transactional
    public List<OutboxEventRecord> claimBatch(String claimedBy) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime staleClaimCutoff = now.minus(properties.getClaimTimeout());
        return jdbcTemplate.query("""
                        WITH claimable AS (
                            SELECT id
                            FROM outbox_events
                            WHERE (
                                status = 'PENDING'
                                AND next_attempt_at <= ?
                            ) OR (
                                status = 'PROCESSING'
                                AND claimed_at < ?
                            )
                            ORDER BY created_at
                            FOR UPDATE SKIP LOCKED
                            LIMIT ?
                        )
                        UPDATE outbox_events e
                        SET status = 'PROCESSING',
                            claimed_at = ?,
                            claimed_by = ?
                        FROM claimable
                        WHERE e.id = claimable.id
                        RETURNING e.*
                        """,
                mapper,
                Timestamp.valueOf(now),
                Timestamp.valueOf(staleClaimCutoff),
                properties.getBatchSize(),
                Timestamp.valueOf(now),
                claimedBy
        );
    }

    public void markProcessed(UUID id) {
        jdbcTemplate.update("""
                        UPDATE outbox_events
                        SET status = 'PROCESSED',
                            processed_at = ?,
                            last_error_code = NULL
                        WHERE id = ?
                        """,
                Timestamp.valueOf(LocalDateTime.now(clock)),
                id
        );
    }

    public void markFailed(UUID id, int nextAttemptCount, LocalDateTime nextAttemptAt, String errorCode, boolean dead) {
        jdbcTemplate.update("""
                        UPDATE outbox_events
                        SET status = ?,
                            attempt_count = ?,
                            next_attempt_at = ?,
                            claimed_at = NULL,
                            claimed_by = NULL,
                            last_error_code = ?
                        WHERE id = ?
                        """,
                dead ? OutboxStatus.DEAD.name() : OutboxStatus.PENDING.name(),
                nextAttemptCount,
                Timestamp.valueOf(nextAttemptAt),
                safeErrorCode(errorCode),
                id
        );
    }

    public long backlog() {
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE status IN ('PENDING', 'PROCESSING')",
                Long.class
        );
        return value == null ? 0 : value;
    }

    public long oldestPendingAgeSeconds() {
        LocalDateTime now = LocalDateTime.now(clock);
        Timestamp oldest = jdbcTemplate.queryForObject(
                "SELECT MIN(created_at) FROM outbox_events WHERE status IN ('PENDING', 'PROCESSING')",
                Timestamp.class
        );
        if (oldest == null) {
            return 0;
        }
        return java.time.Duration.between(oldest.toLocalDateTime(), now).toSeconds();
    }

    public int cleanupProcessed() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minus(properties.getRetention());
        return jdbcTemplate.update("""
                        WITH expired AS (
                            SELECT id
                            FROM outbox_events
                            WHERE status = 'PROCESSED'
                              AND processed_at < ?
                            ORDER BY processed_at
                            LIMIT ?
                        )
                        DELETE FROM outbox_events
                        WHERE id IN (SELECT id FROM expired)
                        """,
                Timestamp.valueOf(cutoff),
                properties.getCleanupBatchSize()
        );
    }

    public List<OutboxEventRecord> findForAdmin(String status, String eventType, LocalDateTime from, LocalDateTime to, int page, int size) {
        StringBuilder sql = new StringBuilder("SELECT * FROM outbox_events WHERE 1=1");
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        if (eventType != null && !eventType.isBlank()) {
            sql.append(" AND event_type = ?");
            args.add(eventType);
        }
        if (from != null) {
            sql.append(" AND created_at >= ?");
            args.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND created_at <= ?");
            args.add(Timestamp.valueOf(to));
        }
        sql.append(" ORDER BY created_at DESC OFFSET ? ROWS FETCH FIRST ? ROWS ONLY");
        args.add(page * size);
        args.add(size);
        return jdbcTemplate.query(sql.toString(), mapper, args.toArray());
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize outbox payload", ex);
        }
    }

    private String safeErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return "outbox_handler_failure";
        }
        String normalized = errorCode.replaceAll("[^A-Za-z0-9_-]", "_");
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }
}
