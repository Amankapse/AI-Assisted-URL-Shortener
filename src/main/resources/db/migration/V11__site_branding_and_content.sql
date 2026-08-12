-- Flyway migration V11: bounded public site experience and platform CMS metadata.

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
    'SITE_SETTINGS_UPDATED',
    'CONTENT_PAGE_UPDATED',
    'CONTENT_PAGE_PUBLISHED',
    'ANNOUNCEMENT_CREATED',
    'ANNOUNCEMENT_UPDATED',
    'ANNOUNCEMENT_DISABLED',
    'MEDIA_ASSET_REGISTERED',
    'MEDIA_ASSET_UPDATED',
    'ADMIN_PROMOTED',
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
    resource_type IN ('URL', 'WORKSPACE', 'WORKSPACE_MEMBER', 'API_KEY', 'CAMPAIGN', 'SITE_SETTINGS', 'CONTENT_PAGE', 'ANNOUNCEMENT', 'MEDIA_ASSET', 'USER')
);

CREATE TABLE media_assets (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    url VARCHAR(800) NOT NULL,
    alt_text VARCHAR(200) NOT NULL,
    source_name VARCHAR(120),
    source_url VARCHAR(800),
    license VARCHAR(160),
    attribution VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by UUID,
    CONSTRAINT fk_media_assets_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_media_assets_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_media_assets_alt_not_blank CHECK (length(trim(alt_text)) > 0),
    CONSTRAINT ck_media_assets_https_url CHECK (url LIKE 'https://%'),
    CONSTRAINT ck_media_assets_source_https_url CHECK (source_url IS NULL OR source_url LIKE 'https://%')
);

CREATE TABLE site_settings (
    id UUID PRIMARY KEY,
    brand_name VARCHAR(80) NOT NULL,
    tagline VARCHAR(160) NOT NULL,
    support_email VARCHAR(320),
    support_url VARCHAR(500),
    contact_text VARCHAR(1000),
    logo_asset_id UUID,
    logo_dark_asset_id UUID,
    favicon_asset_id UUID,
    login_background_asset_id UUID,
    landing_hero_asset_id UUID,
    primary_color VARCHAR(7) NOT NULL,
    secondary_color VARCHAR(7) NOT NULL,
    accent_color VARCHAR(7) NOT NULL,
    footer_description VARCHAR(500) NOT NULL,
    footer_copyright VARCHAR(160) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by UUID,
    CONSTRAINT fk_site_settings_logo FOREIGN KEY (logo_asset_id) REFERENCES media_assets(id) ON DELETE SET NULL,
    CONSTRAINT fk_site_settings_logo_dark FOREIGN KEY (logo_dark_asset_id) REFERENCES media_assets(id) ON DELETE SET NULL,
    CONSTRAINT fk_site_settings_favicon FOREIGN KEY (favicon_asset_id) REFERENCES media_assets(id) ON DELETE SET NULL,
    CONSTRAINT fk_site_settings_login_background FOREIGN KEY (login_background_asset_id) REFERENCES media_assets(id) ON DELETE SET NULL,
    CONSTRAINT fk_site_settings_landing_hero FOREIGN KEY (landing_hero_asset_id) REFERENCES media_assets(id) ON DELETE SET NULL,
    CONSTRAINT fk_site_settings_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_site_settings_brand_not_blank CHECK (length(trim(brand_name)) > 0),
    CONSTRAINT ck_site_settings_tagline_not_blank CHECK (length(trim(tagline)) > 0),
    CONSTRAINT ck_site_settings_support_https_url CHECK (support_url IS NULL OR support_url LIKE 'https://%'),
    CONSTRAINT ck_site_settings_primary_color CHECK (primary_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT ck_site_settings_secondary_color CHECK (secondary_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT ck_site_settings_accent_color CHECK (accent_color ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE TABLE content_pages (
    id UUID PRIMARY KEY,
    page_key VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    content VARCHAR(8000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by UUID,
    published_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uk_content_pages_page_key UNIQUE (page_key),
    CONSTRAINT fk_content_pages_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_content_pages_key CHECK (page_key IN ('ABOUT', 'FEATURES', 'SECURITY', 'HELP', 'CONTACT', 'PRIVACY', 'TERMS', 'ACCESSIBILITY', 'DISCLAIMER')),
    CONSTRAINT ck_content_pages_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT ck_content_pages_title_not_blank CHECK (length(trim(title)) > 0),
    CONSTRAINT ck_content_pages_summary_not_blank CHECK (length(trim(summary)) > 0),
    CONSTRAINT ck_content_pages_content_not_blank CHECK (length(trim(content)) > 0)
);

CREATE TABLE announcements (
    id UUID PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    audience VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    start_at TIMESTAMP WITHOUT TIME ZONE,
    end_at TIMESTAMP WITHOUT TIME ZONE,
    dismissible BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_announcements_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_announcements_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_announcements_title_not_blank CHECK (length(trim(title)) > 0),
    CONSTRAINT ck_announcements_message_not_blank CHECK (length(trim(message)) > 0),
    CONSTRAINT ck_announcements_severity CHECK (severity IN ('INFO', 'NOTICE', 'WARNING', 'MAINTENANCE')),
    CONSTRAINT ck_announcements_audience CHECK (audience IN ('PUBLIC', 'AUTHENTICATED', 'ADMIN')),
    CONSTRAINT ck_announcements_time_range CHECK (end_at IS NULL OR start_at IS NULL OR end_at >= start_at)
);

CREATE INDEX idx_announcements_active_window
    ON announcements (enabled, audience, start_at, end_at, created_at DESC);

INSERT INTO site_settings (
    id, brand_name, tagline, support_email, support_url, contact_text,
    primary_color, secondary_color, accent_color, footer_description, footer_copyright,
    version, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000010',
    'Shortener Ops',
    'Secure link management for teams and workspaces.',
    'support@example.com',
    'https://example.com/help',
    'For demo support, use the registered project owner contact channel.',
    '#185A9D',
    '#123047',
    '#0F766E',
    'A production-oriented URL shortener prototype with secure ownership, auditability, and operational controls.',
    'Copyright 2026 Shortener Ops demo.',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO content_pages (id, page_key, title, summary, content, status, version, created_at, updated_at, published_at) VALUES
('00000000-0000-0000-0000-000000000101', 'ABOUT', 'About Shortener Ops', 'A focused URL shortener for secure team-managed links.', 'Shortener Ops is a modular monolith built with Spring Boot, PostgreSQL, Redis and Angular. It demonstrates secure link ownership, workspace collaboration, operational readiness and AI-assisted engineering traceability for an assessment project.', 'PUBLISHED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000102', 'FEATURES', 'Features', 'Core capabilities available in this prototype.', 'Create short links, manage aliases, organize links with campaigns and tags, inspect analytics, use API keys for machine clients, and administer moderation, audit, outbox and site content with explicit platform administrator access.', 'PUBLISHED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000103', 'SECURITY', 'Security', 'Implemented security controls in the application.', 'The service uses RS256 JWT access tokens, rotating opaque refresh tokens, CSRF protection for refresh and logout, role-based access, workspace isolation, API key scopes, rate limits, moderation controls, audit events and secure operational defaults. Platform administrator MFA is not currently implemented.', 'PUBLISHED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000104', 'HELP', 'Help', 'How to start using the service.', 'Register an account, sign in, create a workspace link, copy the generated short URL and review link analytics after redirects occur. Platform administration requires an existing user to be promoted through the documented bootstrap environment variable.', 'PUBLISHED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000105', 'CONTACT', 'Contact', 'Support and project contact information.', 'This demo does not operate a public support desk. Configure organization-specific contact text, support email and support URL before production use.', 'PUBLISHED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000106', 'PRIVACY', 'Privacy', 'Privacy notes for the assessment prototype.', 'The application stores account email addresses, link metadata and anonymized analytics signals. IP addresses used for analytics are HMAC-hashed with a configured pepper. Organization-specific privacy language should receive legal review before commercial production use.', 'PUBLISHED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000107', 'TERMS', 'Terms', 'Usage expectations for the assessment prototype.', 'Use the service only for lawful links that you are authorized to manage. Administrators may block destinations that violate policy. Organization-specific terms should receive legal review before commercial production use.', 'PUBLISHED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000108', 'ACCESSIBILITY', 'Accessibility', 'Accessibility commitment and current implementation notes.', 'The frontend uses semantic navigation, labeled forms, visible focus states, keyboard-operable controls, reduced-motion handling and high-contrast defaults. Accessibility should be retested after organization-specific branding changes.', 'PUBLISHED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000109', 'DISCLAIMER', 'Disclaimer', 'Important note about external destinations.', 'Short links may redirect to third-party websites. The service does not control or endorse external destination content. Users should verify unfamiliar destinations before opening or sharing links.', 'PUBLISHED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
