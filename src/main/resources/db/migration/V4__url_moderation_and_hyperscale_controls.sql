-- Flyway migration V4: moderation state for URL abuse controls.

ALTER TABLE short_urls
    ADD COLUMN blocked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_short_urls_blocked
    ON short_urls (blocked);
