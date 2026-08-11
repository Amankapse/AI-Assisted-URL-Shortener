-- Flyway migration V9: transactional outbox for durable event delivery.

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    workspace_id UUID NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    event_version INTEGER NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    next_attempt_at TIMESTAMP NOT NULL,
    claimed_at TIMESTAMP NULL,
    claimed_by VARCHAR(160) NULL,
    processed_at TIMESTAMP NULL,
    last_error_code VARCHAR(80) NULL,
    CONSTRAINT ck_outbox_events_status CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'DEAD')),
    CONSTRAINT ck_outbox_events_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_outbox_events_event_version CHECK (event_version > 0)
);

CREATE INDEX idx_outbox_events_dispatch
    ON outbox_events (status, next_attempt_at, created_at);

CREATE INDEX idx_outbox_events_claim_recovery
    ON outbox_events (status, claimed_at);

CREATE INDEX idx_outbox_events_created_at
    ON outbox_events (created_at);
