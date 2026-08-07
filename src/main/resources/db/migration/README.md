# Flyway migrations

Place Flyway SQL migration scripts in this directory when the schema evolves.

Current migration sequence:

- `V1__initial_schema.sql`: users, short URLs, click events.
- `V2__authentication_refresh_tokens.sql`: refresh-token digest and family tracking.
- `V3__click_analytics_indexes.sql`: analytics correlation ID, soft-delete, analytics indexes.
- `V4__url_moderation_and_hyperscale_controls.sql`: URL moderation `blocked` flag and index.

Historical migrations are forward-only and must not be edited after validation.
