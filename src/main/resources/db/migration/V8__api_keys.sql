-- Flyway migration V8: workspace-bound API keys for machine-to-machine authentication.

ALTER TABLE audit_events DROP CONSTRAINT ck_audit_events_action;
ALTER TABLE audit_events ADD CONSTRAINT ck_audit_events_action CHECK (action IN (
    'URL_CREATED',
    'URL_DESTINATION_CHANGED',
    'URL_EXPIRATION_CHANGED',
    'URL_ENABLED',
    'URL_DISABLED',
    'URL_DELETED',
    'URL_BLOCKED',
    'URL_UNBLOCKED',
    'API_KEY_CREATED',
    'API_KEY_REVOKED',
    'WORKSPACE_CREATED',
    'WORKSPACE_MEMBER_ADDED',
    'WORKSPACE_MEMBER_ROLE_CHANGED',
    'WORKSPACE_MEMBER_REMOVED'
));

ALTER TABLE audit_events DROP CONSTRAINT ck_audit_events_resource_type;
ALTER TABLE audit_events ADD CONSTRAINT ck_audit_events_resource_type CHECK (resource_type IN ('URL', 'WORKSPACE', 'WORKSPACE_MEMBER', 'API_KEY'));

CREATE TABLE api_keys (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    key_prefix VARCHAR(32) NOT NULL,
    key_digest VARCHAR(128) NOT NULL,
    scopes TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE,
    revoked_at TIMESTAMP WITHOUT TIME ZONE,
    last_used_at TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_api_keys_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE RESTRICT,
    CONSTRAINT fk_api_keys_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uq_api_keys_key_prefix UNIQUE (key_prefix),
    CONSTRAINT uq_api_keys_key_digest UNIQUE (key_digest),
    CONSTRAINT ck_api_keys_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_api_keys_scopes_not_blank CHECK (length(trim(scopes)) > 0)
);

CREATE INDEX idx_api_keys_workspace_revoked_at ON api_keys(workspace_id, revoked_at);
CREATE INDEX idx_api_keys_workspace_created_at ON api_keys(workspace_id, created_at);
