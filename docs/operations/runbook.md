# Operational Runbook

## Startup

Prerequisites:

- Java 21
- Docker Desktop or compatible Docker runtime
- PostgreSQL 15
- Redis 7
- Environment-provided JWT RSA keys for non-test environments
- `APP_ANALYTICS_IP_HASH_PEPPER` from secret management in production
- `APP_API_KEY_HASH_PEPPER` from secret management in production
- `APP_AUDIT_METADATA_MAX_BYTES` and `APP_AUDIT_RETENTION` reviewed for the environment
- `APP_OUTBOX_*` dispatcher, retry, payload, and retention settings reviewed for the environment
- `SHORTENER_CODE_LENGTH`, `SHORTENER_CODE_MAX_RETRIES`, and quota settings reviewed for the environment

Start local dependencies:

```powershell
docker compose up -d postgres redis
```

Start the application with the required datasource, Redis, JWT, analytics, and rate-limit environment variables. Flyway runs on startup and applies forward-only migrations.

Current migrations are V1 through V9. V7 adds append-only application-level audit events with bounded JSONB metadata. V8 adds workspace-bound API keys and expands audit constraints for API-key create/revoke events. V9 adds transactional outbox events. Audit rows intentionally avoid cascading foreign keys so records survive user, URL, and workspace lifecycle changes.

## API latency increase

Check:

- `http.server.requests`
- Redis cache hit/miss/failure metrics
- PostgreSQL latency and Hikari pool usage
- analytics queue depth and dropped events
- CPU and memory
- rate-limit rejection/failure metrics
- quota rejection metrics
- short-code collision retry/exhaustion metrics
- outbox backlog, oldest pending age, retry, and dead-letter metrics

## Redis unavailable

Expected behavior:

- Redirect cache lookups miss/fail and fall back to PostgreSQL.
- Valid redirects continue if PostgreSQL is available.
- Public redirect and URL/admin rate limits fail open where configured.
- Login/register/refresh fail closed if Redis limiter is unavailable.
- Redis failure metrics and safe warnings increase.

## PostgreSQL unavailable

Expected behavior:

- Readiness fails.
- Writes and PostgreSQL-backed reads fail safely through Problem Details.
- Cache-hit redirects may continue only if the cache entry is present and valid, but correctness depends on PostgreSQL as source of truth.
- Liveness remains healthy unless the JVM/application itself is unhealthy.

## Analytics queue saturated

Expected behavior:

- Redirects continue.
- Dropped-event counter rises.
- Analytics completeness degrades.
- Queue depth remains bounded by configuration.

## URL quota exceeded

Expected behavior:

- Request returns RFC7807 `quota-exceeded` with HTTP 403.
- Rate limiting remains separate; quota exhaustion is a resource allowance failure, not request velocity.
- Review per-user daily creation, active-link, and custom-alias limits.
- In hyperscale production, replace repeated aggregate SQL counting with materialized/distributed counters and durable reconciliation.

## URL blocked for moderation

Expected behavior:

- Admin block/unblock endpoints are restricted to `ROLE_ADMIN`.
- Blocked redirects return safe not-found behavior.
- Owners cannot remove an administrative block through normal enable endpoints.
- Redis redirect cache is invalidated after the moderation transaction.
- Analytics history is retained.
- `URL_BLOCKED` and `URL_UNBLOCKED` audit events are recorded with the target workspace and platform admin actor.

## Audit trail query or write issue

Expected behavior:

- Mutating URL, workspace, membership, and moderation operations write audit rows in the same PostgreSQL transaction where practical.
- Audit persistence failure fails the mutation rather than silently losing the accountability record.
- Audit metadata is bounded by `APP_AUDIT_METADATA_MAX_BYTES` and stores safe metadata only.
- Destination changes store host and SHA-256 URL hashes, not raw destination URLs.
- Audit APIs are read-only and role-gated: workspace `OWNER`/`ADMIN` for workspace audit, workspace `OWNER`/`ADMIN`/`EDITOR` for URL audit, and platform `ROLE_ADMIN` for `/api/v1/admin/audit`.

## API key issue

Expected behavior:

- API keys authenticate only through `X-API-Key`.
- Revoked, expired, malformed, and unknown keys return generic HTTP 401 responses.
- API-key traffic is workspace-bound and scope-limited.
- API-key request velocity uses the `api-key` rate limiter and fails closed when Redis limiter state is unavailable.
- Create and revoke operations are audited; raw keys and digests are not logged.

Investigate:

- `APP_API_KEY_HASH_PEPPER` presence and consistency after deployment.
- `api-key` rate-limit rejections and Redis availability.
- API-key expiration and revocation state.
- Caller storage or accidental exposure of raw key material.

## Transactional outbox backlog or dead letters

Expected behavior:

- URL mutation events, durable cache invalidation work, and optional outbox analytics are stored in PostgreSQL as outbox rows.
- Dispatcher workers claim rows with PostgreSQL row locks and `SKIP LOCKED`.
- Transient handler failures retry with bounded exponential backoff and jitter.
- Exhausted or permanent failures move to `DEAD` with a stable error code.
- Liveness and readiness do not depend on dispatcher progress.

Investigate:

- `/api/v1/admin/outbox?status=DEAD` as a platform admin.
- `url_shortener.outbox.backlog` and `url_shortener.outbox.oldest_pending_age`.
- Redis availability when cache invalidation events are retrying.
- PostgreSQL health and Hikari pool metrics.
- Payload-size errors if creation fails before an outbox row is committed.

Do not manually update outbox rows in production without a documented repair plan. Retrying `DEAD` rows is intentionally not exposed through the public API in this stage.

## High login failures

Investigate:

- login failure metrics
- login rate-limit rejection metrics
- source network patterns at the trusted proxy layer
- user enumeration attempts
- JWT validation failures

## Refresh-token reuse detected

Expected behavior:

- Token family is revoked.
- User must log in again.
- Security metric and safe warning are recorded.
- Raw tokens are never logged.

## Backup and disaster recovery targets

Architecture targets:

- RPO <= 5 minutes.
- RTO <= 30 minutes.
- PostgreSQL requires PITR, backups, restore drills, and multi-AZ replication.
- Redis is rebuildable from the authoritative URL mapping store.
- Future durable event streams require retention and replay validation.

The local Docker Compose environment does not implement these production DR targets.
