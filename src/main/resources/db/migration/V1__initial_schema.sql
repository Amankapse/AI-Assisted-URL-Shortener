-- Flyway migration V1: initial schema for users, short_urls, click_events

CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'SUSPENDED', 'DELETED');

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role user_role NOT NULL,
    status user_status NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE short_urls (
    id UUID PRIMARY KEY,
    short_code VARCHAR(16) NOT NULL UNIQUE,
    original_url VARCHAR(2048) NOT NULL,
    custom_alias VARCHAR(100),
    owner_id UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    click_count BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL,
    CONSTRAINT fk_short_urls_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE click_events (
    id UUID PRIMARY KEY,
    url_id UUID NOT NULL,
    clicked_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    ip_hash VARCHAR(128),
    user_agent VARCHAR(1024),
    referer VARCHAR(1024),
    CONSTRAINT fk_click_events_url FOREIGN KEY (url_id) REFERENCES short_urls(id)
);

CREATE INDEX idx_short_urls_short_code ON short_urls(short_code);
CREATE INDEX idx_short_urls_owner_id ON short_urls(owner_id);
CREATE INDEX idx_short_urls_enabled_expires_at ON short_urls(enabled, expires_at);
CREATE INDEX idx_click_events_url_id ON click_events(url_id);
CREATE INDEX idx_click_events_clicked_at ON click_events(clicked_at);
