-- Flyway migration V2: refresh-token persistence for Phase 3 authentication

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_digest VARCHAR(64) NOT NULL UNIQUE,
    family_id UUID NOT NULL,
    issued_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITHOUT TIME ZONE,
    replaced_by_token_id UUID,
    reuse_detected_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_refresh_tokens_replacement FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens(id)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_active_lookup ON refresh_tokens(token_digest, revoked_at, expires_at);
