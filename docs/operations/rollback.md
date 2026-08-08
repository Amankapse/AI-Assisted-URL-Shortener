# Rollback Guide

## Application rollback

Rollback by deploying the previous application artifact or Docker image. Keep environment variables compatible with the target version.

## Docker image rollback

Use the previous immutable image tag. Verify that its expected database schema is compatible with already-applied migrations.

## Flyway policy

Migrations are forward-only. Do not blindly roll back destructive database changes. The hyperscale evolution added V4 for the backward-compatible `short_urls.blocked` column and index. Enterprise stages added V5 idempotency records, V6 workspaces/memberships, V7 audit events, V8 API keys, and V9 outbox events as additive migrations. Existing migrations V1 through V8 were not modified when V9 was added.

Audit rollback consideration: application rollback must tolerate the existing `audit_events` table. Do not delete audit rows as part of a rollback; they are accountability records. If a previous application version ignores the table, leave it in place for forward compatibility.

API-key rollback consideration: application rollback must tolerate the existing `api_keys` table. Do not export raw API keys because only digests are stored. If rolling back to a version without API-key support, machine clients must use human-token flows or wait for redeployment of the API-key-capable version.

Outbox rollback consideration: application rollback must tolerate the existing `outbox_events` table. If rolling back to a version without outbox dispatch, leave rows in place and expect durable delivery to pause until an outbox-capable version is redeployed. Do not delete `PENDING`, `PROCESSING`, or `DEAD` rows as part of ordinary rollback.

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

## Analytics compatibility

Soft-deleted URL rows preserve historical click events. Application rollback must preserve compatibility with the existing `short_urls.deleted` and click analytics columns.
