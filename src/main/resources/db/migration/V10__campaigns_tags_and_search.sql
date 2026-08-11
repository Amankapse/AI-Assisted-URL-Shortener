-- Flyway migration V10: campaigns, tags, and bounded search metadata.
-- Existing destination_host values are intentionally not backfilled here.

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
    'URL_CAMPAIGN_CHANGED',
    'URL_TAGS_CHANGED',
    'API_KEY_CREATED',
    'API_KEY_REVOKED',
    'WORKSPACE_CREATED',
    'WORKSPACE_MEMBER_ADDED',
    'WORKSPACE_MEMBER_ROLE_CHANGED',
    'WORKSPACE_MEMBER_REMOVED',
    'CAMPAIGN_CREATED',
    'CAMPAIGN_UPDATED',
    'CAMPAIGN_DELETED'
));

ALTER TABLE audit_events DROP CONSTRAINT ck_audit_events_resource_type;
ALTER TABLE audit_events ADD CONSTRAINT ck_audit_events_resource_type CHECK (
    resource_type IN ('URL', 'WORKSPACE', 'WORKSPACE_MEMBER', 'API_KEY', 'CAMPAIGN')
);

CREATE TABLE campaigns (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_campaigns_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE RESTRICT,
    CONSTRAINT fk_campaigns_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT ck_campaigns_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_campaigns_normalized_name_not_blank CHECK (length(trim(normalized_name)) > 0)
);

CREATE UNIQUE INDEX uq_campaigns_workspace_normalized_active
    ON campaigns (workspace_id, normalized_name)
    WHERE deleted = FALSE;

CREATE INDEX idx_campaigns_workspace_created_at
    ON campaigns (workspace_id, created_at DESC);

CREATE TABLE tags (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    name VARCHAR(50) NOT NULL,
    normalized_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_tags_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT ck_tags_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_tags_normalized_name_not_blank CHECK (length(trim(normalized_name)) > 0),
    CONSTRAINT uq_tags_workspace_normalized UNIQUE (workspace_id, normalized_name)
);

CREATE TABLE url_tags (
    url_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (url_id, tag_id),
    CONSTRAINT fk_url_tags_url FOREIGN KEY (url_id) REFERENCES short_urls(id) ON DELETE CASCADE,
    CONSTRAINT fk_url_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

ALTER TABLE short_urls
    ADD COLUMN campaign_id UUID,
    ADD COLUMN destination_host VARCHAR(253),
    ADD CONSTRAINT fk_short_urls_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE SET NULL;

CREATE INDEX idx_short_urls_workspace_campaign_created_at
    ON short_urls (workspace_id, campaign_id, created_at DESC);

CREATE INDEX idx_short_urls_workspace_destination_host_created_at
    ON short_urls (workspace_id, destination_host, created_at DESC);

CREATE INDEX idx_url_tags_tag_url
    ON url_tags (tag_id, url_id);

CREATE INDEX idx_url_tags_url_tag
    ON url_tags (url_id, tag_id);
