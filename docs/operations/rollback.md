# Rollback Guide

## Application rollback

Rollback by deploying the previous application artifact or Docker image. In the current Render deployment, that image contains both the Angular SPA and Spring Boot API.

Keep environment variables compatible with the target version. Frontend public build variables such as `FRONTEND_API_BASE_URL` and `FRONTEND_PUBLIC_SHORT_URL_BASE` are baked into `app-config.json` during the Docker build, so changing them requires rebuilding/redeploying the image.

## Future split frontend rollback

If Angular is later split back to Render Static Site/CDN hosting, rollback that static deployment independently from the backend. Do not replace an SPA rewrite with a redirect; Angular client routes require a rewrite to `index.html`.

## Docker image rollback

Use the previous immutable image tag. Verify that its expected database schema is compatible with already-applied migrations.

## Flyway policy

Migrations are forward-only. Do not blindly roll back destructive database changes. The hyperscale evolution added V4 for the backward-compatible `short_urls.blocked` column and index. Enterprise stages added V5 idempotency records, V6 workspaces/memberships, V7 audit events, V8 API keys, V9 outbox events, and V10 campaigns/tags/search as additive migrations. Existing migrations V1 through V9 were not modified when V10 was added.

Audit rollback consideration: application rollback must tolerate the existing `audit_events` table. Do not delete audit rows as part of a rollback; they are accountability records. If a previous application version ignores the table, leave it in place for forward compatibility.

API-key rollback consideration: application rollback must tolerate the existing `api_keys` table. Do not export raw API keys because only digests are stored. If rolling back to a version without API-key support, machine clients must use human-token flows or wait for redeployment of the API-key-capable version.

Outbox rollback consideration: application rollback must tolerate the existing `outbox_events` table. If rolling back to a version without outbox dispatch, leave rows in place and expect durable delivery to pause until an outbox-capable version is redeployed. Do not delete `PENDING`, `PROCESSING`, or `DEAD` rows as part of ordinary rollback.

Campaign/tag rollback consideration: application rollback must tolerate the existing `campaigns`, `tags`, and `url_tags` tables plus nullable campaign references on URLs. If rolling back to a version without search/filter UI support, leave organization metadata in place for forward compatibility.

## Feature/config rollback

Operational behavior can be adjusted with configuration:

- disable rate limiting with `APP_RATE_LIMIT_ENABLED=false` only as an emergency mitigation
- increase/decrease individual limiter thresholds
- disable redirect cache with `APP_REDIRECT_CACHE_ENABLED=false`
- tune analytics queue and batch settings
- disable outbox dispatch with `APP_OUTBOX_ENABLED=false` only to stop handler processing while preserving same-transaction row creation
- keep `APP_ANALYTICS_PUBLISHER=local` unless the operational cost of outbox-backed analytics is explicitly accepted

## Redis cache flush

Redis cache keys can be safely flushed because PostgreSQL is authoritative. Flushing Redis increases PostgreSQL load until hot keys are repopulated.

## Authentication changes

Do not rotate JWT keys or refresh-token behavior without a coordinated plan. If keys are rolled back, tokens issued by the newer key may stop validating.

Do not rotate `APP_API_KEY_HASH_PEPPER` without a coordinated key reissue plan. Existing API-key digests depend on the pepper.

Do not weaken secure cookie, CSRF, or SameSite settings as a rollback shortcut. If a frontend/backend origin mismatch breaks refresh-token flows, fix the deployment origin or CORS/cookie configuration through a reviewed release.

## Analytics compatibility

Soft-deleted URL rows preserve historical click events. Application rollback must preserve compatibility with the existing `short_urls.deleted` and click analytics columns.
