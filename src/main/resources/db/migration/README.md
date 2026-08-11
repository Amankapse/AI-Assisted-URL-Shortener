# Flyway migrations

Place Flyway SQL migration scripts in this directory when the schema evolves.

Current migration sequence:

- `V1__initial_schema.sql`: users, short URLs, click events.
- `V2__authentication_refresh_tokens.sql`: refresh-token digest and family tracking.
- `V3__click_analytics_indexes.sql`: analytics correlation ID, soft-delete, analytics indexes.
- `V4__url_moderation_and_hyperscale_controls.sql`: URL moderation `blocked` flag and index.
- `V5__idempotency_keys.sql`: idempotency records.
- `V6__workspaces_and_memberships.sql`: workspaces and memberships.
- `V7__audit_events.sql`: immutable audit events.
- `V8__api_keys.sql`: workspace API keys.
- `V9__outbox_events.sql`: transactional outbox events.
- `V10__campaigns_tags_and_search.sql`: campaigns, tags, URL tag assignments, nullable URL campaign assignment, destination host, search indexes, and audit enum expansion.

Historical migrations are forward-only and must not be edited after validation.
