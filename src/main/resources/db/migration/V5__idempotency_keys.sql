-- Flyway migration V5: idempotent URL creation support

CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    scope VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    response_status INTEGER,
    response_body TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uk_idempotency_scope_key UNIQUE (scope, idempotency_key),
    CONSTRAINT ck_idempotency_status CHECK (status IN ('IN_PROGRESS', 'SUCCEEDED'))
);

CREATE INDEX idx_idempotency_keys_expires_at ON idempotency_keys(expires_at);
