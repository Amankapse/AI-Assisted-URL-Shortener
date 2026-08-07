# Operational Runbook

## Startup

Prerequisites:

- Java 21
- Docker Desktop or compatible Docker runtime
- PostgreSQL 15
- Redis 7
- Environment-provided JWT RSA keys for non-test environments
- `APP_ANALYTICS_IP_HASH_PEPPER` from secret management in production
- `SHORTENER_CODE_LENGTH`, `SHORTENER_CODE_MAX_RETRIES`, and quota settings reviewed for the environment

Start local dependencies:

```powershell
docker compose up -d postgres redis
```

Start the application with the required datasource, Redis, JWT, analytics, and rate-limit environment variables. Flyway runs on startup and applies forward-only migrations.

Current migrations are V1 through V4. V4 adds administrative URL blocking with `short_urls.blocked`.

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
