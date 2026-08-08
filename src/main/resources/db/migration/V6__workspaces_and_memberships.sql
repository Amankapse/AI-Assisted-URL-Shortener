-- Flyway migration V6: workspace tenant foundation and memberships.

CREATE TABLE workspaces (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    default_workspace BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_workspaces_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE workspace_memberships (
    workspace_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    default_membership BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (workspace_id, user_id),
    CONSTRAINT fk_workspace_memberships_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_memberships_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_workspace_memberships_role CHECK (role IN ('OWNER', 'ADMIN', 'EDITOR', 'ANALYST', 'VIEWER'))
);

ALTER TABLE short_urls
    ADD COLUMN workspace_id UUID;

WITH default_workspaces AS (
    INSERT INTO workspaces (id, name, default_workspace, created_by_user_id, created_at, updated_at, version)
    SELECT
        (
            substr(md5(u.id::text || ':default-workspace'), 1, 8) || '-' ||
            substr(md5(u.id::text || ':default-workspace'), 9, 4) || '-' ||
            substr(md5(u.id::text || ':default-workspace'), 13, 4) || '-' ||
            substr(md5(u.id::text || ':default-workspace'), 17, 4) || '-' ||
            substr(md5(u.id::text || ':default-workspace'), 21, 12)
        )::uuid,
        split_part(u.email, '@', 1) || '''s Workspace',
        TRUE,
        u.id,
        COALESCE(u.created_at, CURRENT_TIMESTAMP),
        COALESCE(u.updated_at, CURRENT_TIMESTAMP),
        0
    FROM users u
    ON CONFLICT (id) DO NOTHING
    RETURNING id, created_by_user_id
)
INSERT INTO workspace_memberships (workspace_id, user_id, role, default_membership, created_at, updated_at)
SELECT
    (
        substr(md5(u.id::text || ':default-workspace'), 1, 8) || '-' ||
        substr(md5(u.id::text || ':default-workspace'), 9, 4) || '-' ||
        substr(md5(u.id::text || ':default-workspace'), 13, 4) || '-' ||
        substr(md5(u.id::text || ':default-workspace'), 17, 4) || '-' ||
        substr(md5(u.id::text || ':default-workspace'), 21, 12)
    )::uuid,
    u.id,
    'OWNER',
    TRUE,
    COALESCE(u.created_at, CURRENT_TIMESTAMP),
    COALESCE(u.updated_at, CURRENT_TIMESTAMP)
FROM users u
ON CONFLICT (workspace_id, user_id) DO NOTHING;

UPDATE short_urls su
SET workspace_id = (
    substr(md5(su.owner_id::text || ':default-workspace'), 1, 8) || '-' ||
    substr(md5(su.owner_id::text || ':default-workspace'), 9, 4) || '-' ||
    substr(md5(su.owner_id::text || ':default-workspace'), 13, 4) || '-' ||
    substr(md5(su.owner_id::text || ':default-workspace'), 17, 4) || '-' ||
    substr(md5(su.owner_id::text || ':default-workspace'), 21, 12)
)::uuid
WHERE su.workspace_id IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM short_urls WHERE workspace_id IS NULL) THEN
        RAISE EXCEPTION 'short_urls.workspace_id backfill failed';
    END IF;
END $$;

ALTER TABLE short_urls
    ALTER COLUMN workspace_id SET NOT NULL,
    ADD CONSTRAINT fk_short_urls_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id);

CREATE INDEX idx_workspace_memberships_user_id ON workspace_memberships(user_id);
CREATE INDEX idx_short_urls_workspace_created_at ON short_urls(workspace_id, created_at);
CREATE INDEX idx_short_urls_workspace_short_code ON short_urls(workspace_id, short_code);
