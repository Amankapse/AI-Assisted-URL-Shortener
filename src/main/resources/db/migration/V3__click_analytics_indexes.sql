-- Flyway migration V3: click analytics query support and safe correlation IDs

ALTER TABLE click_events
    ADD COLUMN correlation_id VARCHAR(128);

ALTER TABLE short_urls
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Supports owner-scoped daily analytics and last-accessed queries by URL.
CREATE INDEX idx_click_events_url_clicked_at
    ON click_events (url_id, clicked_at DESC);
