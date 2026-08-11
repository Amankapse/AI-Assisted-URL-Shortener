CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    schema_version INTEGER NOT NULL DEFAULT 1,
    workspace_id UUID,
    actor_type VARCHAR(20) NOT NULL,
    actor_id UUID,
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_id UUID,
    correlation_id VARCHAR(128),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ck_audit_events_actor_type CHECK (actor_type IN ('USER', 'SYSTEM', 'API_KEY', 'SERVICE')),
    CONSTRAINT ck_audit_events_action CHECK (action IN (
        'URL_CREATED',
        'URL_DESTINATION_CHANGED',
        'URL_EXPIRATION_CHANGED',
        'URL_ENABLED',
        'URL_DISABLED',
        'URL_DELETED',
        'URL_BLOCKED',
        'URL_UNBLOCKED',
        'WORKSPACE_CREATED',
        'WORKSPACE_MEMBER_ADDED',
        'WORKSPACE_MEMBER_ROLE_CHANGED',
        'WORKSPACE_MEMBER_REMOVED'
    )),
    CONSTRAINT ck_audit_events_resource_type CHECK (resource_type IN ('URL', 'WORKSPACE', 'WORKSPACE_MEMBER')),
    CONSTRAINT ck_audit_events_metadata_object CHECK (jsonb_typeof(metadata) = 'object')
);

CREATE INDEX idx_audit_events_workspace_occurred_at
    ON audit_events (workspace_id, occurred_at DESC)
    WHERE workspace_id IS NOT NULL;

CREATE INDEX idx_audit_events_resource_occurred_at
    ON audit_events (resource_type, resource_id, occurred_at DESC)
    WHERE resource_id IS NOT NULL;

CREATE INDEX idx_audit_events_actor_occurred_at
    ON audit_events (actor_type, actor_id, occurred_at DESC)
    WHERE actor_id IS NOT NULL;

CREATE INDEX idx_audit_events_action_occurred_at
    ON audit_events (action, occurred_at DESC);
