# Rollback Guide

## Application rollback

Rollback by deploying the previous application artifact or Docker image. Keep environment variables compatible with the target version.

## Docker image rollback

Use the previous immutable image tag. Verify that its expected database schema is compatible with already-applied migrations.

## Flyway policy

Migrations are forward-only. Do not blindly roll back destructive database changes. The hyperscale evolution added V4 for the backward-compatible `short_urls.blocked` column and index. Existing migrations V1, V2, and V3 were not modified.

## Feature/config rollback

Operational behavior can be adjusted with configuration:

- disable rate limiting with `APP_RATE_LIMIT_ENABLED=false` only as an emergency mitigation
- increase/decrease individual limiter thresholds
- disable redirect cache with `APP_REDIRECT_CACHE_ENABLED=false`
- tune analytics queue and batch settings

## Redis cache flush

Redis cache keys can be safely flushed because PostgreSQL is authoritative. Flushing Redis increases PostgreSQL load until hot keys are repopulated.

## Authentication changes

Do not rotate JWT keys or refresh-token behavior without a coordinated plan. If keys are rolled back, tokens issued by the newer key may stop validating.

## Analytics compatibility

Soft-deleted URL rows preserve historical click events. Application rollback must preserve compatibility with the existing `short_urls.deleted` and click analytics columns.
