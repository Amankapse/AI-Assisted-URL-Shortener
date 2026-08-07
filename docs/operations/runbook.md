# Operational Runbook

## Startup

Prerequisites:

- Java 21
- Docker Desktop or compatible Docker runtime
- PostgreSQL 15
- Redis 7
- Environment-provided JWT RSA keys for non-test environments
- `APP_ANALYTICS_IP_HASH_PEPPER` from secret management in production

Start local dependencies:

```powershell
docker compose up -d postgres redis
```

Start the application with the required datasource, Redis, JWT, analytics, and rate-limit environment variables. Flyway runs on startup and applies forward-only migrations.

## API latency increase

Check:

- `http.server.requests`
- Redis cache hit/miss/failure metrics
- PostgreSQL latency and Hikari pool usage
- analytics queue depth and dropped events
- CPU and memory
- rate-limit rejection/failure metrics

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
