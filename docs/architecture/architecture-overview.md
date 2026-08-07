# Architecture Overview

The URL shortener is designed as a modular monolith with clear feature boundaries. The solution prioritizes security, observability, and operational readiness while keeping the application deployable as a single artifact.

## Main components

- API gateway/entrypoint: Spring Boot REST controllers exposing `/api/v1`
- Ownership: `CurrentOwnerProvider` supplies `OwnerIdentity` to enforce owner-scoped service and repository operations without accepting owner IDs in request DTOs
- Authentication: short-lived RS256 JWT access tokens in `Authorization: Bearer` header, opaque refresh tokens in Secure HttpOnly cookies, and role-based access control
- URL Service: business logic for creating, resolving, and managing short URLs
- Redirect Service: Redis cache-aside short-code resolution backed by PostgreSQL, with safe PostgreSQL fallback on Redis failure
- Analytics Service: best-effort async click event capture, sanitized metadata, and per-link/admin analytics
- Data layer: PostgreSQL for source-of-truth persistence
- Cache layer: Redis for redirect optimization; PostgreSQL remains authoritative
- Observability: Actuator health/liveness/readiness, ADMIN-protected metrics, bounded Micrometer application meters, correlation IDs, and safe structured logging context
- Rate limiting: Redis Lua fixed-window policies with documented fail-open/fail-closed behavior

## Data flow

1. User requests create link through `/api/v1/urls`.
2. Application validates destination URL and optional alias.
3. Service resolves the current owner from `CurrentOwnerProvider`, backed by the authenticated JWT principal.
4. Service generates or validates a short code and persists the link to PostgreSQL.
5. Public redirect requests use `/r/{shortCode}` and resolve the link from Redis when a valid cache entry exists.
6. Cache misses, malformed cache entries, Redis failures, and ineligible cached entries fall back to PostgreSQL.
7. Valid redirects enqueue a best-effort analytics event. Batch persistence inserts immutable click events and atomically increments `short_urls.click_count`.
8. URL disable, enable, expiration update, and delete publish after-commit cache invalidation.

## Security flow

- All protected APIs require JWT bearer tokens.
- `USER` can manage only their resources.
- `ADMIN` does not bypass ownership on user URL endpoints; privileged behavior must be exposed through explicit future admin endpoints.
- CSRF is not required for stateless access-token API requests because the bearer token is not automatically attached by the browser. Refresh and logout endpoints using cookie-based refresh tokens are protected against CSRF.
- Refresh tokens are opaque random values; only SHA-256 digests are stored in PostgreSQL. Rotation creates a replacement token in the same family, and reuse of a revoked token revokes the active family.

## Operational flow

- Liveness reports JVM/application state and intentionally excludes PostgreSQL and Redis.
- Readiness requires PostgreSQL because it is the source of truth and intentionally excludes Redis because redirect cache and selected limiter failures degrade without breaking correctness.
- Redis cache, single-flight, analytics, authentication, and rate-limit telemetry are emitted through Micrometer with bounded tags only.
- Soft deletion preserves historical analytics while making deleted links unavailable through normal owner and redirect queries.
