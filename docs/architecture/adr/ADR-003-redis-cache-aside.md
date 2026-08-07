# ADR-003: Redis Cache-Aside

## Status
Accepted

## Context
Redirect performance is critical for a URL shortener, and repeated lookups should avoid unnecessary database load.

## Decision
Use Redis as a cache-aside layer for short-code resolution. On redirect request:

1. Check Redis for the short code.
2. If present, validate schema version, destination URL, enabled state, deleted state, and expiration before redirecting.
3. If absent, malformed, unsupported, expired, or if Redis is unavailable, read from PostgreSQL.
4. Populate Redis only with the versioned redirect DTO: schema version, URL ID, destination URL, enabled flag, expiration timestamp, and deleted state.
5. Use key format `url:v1:redirect:{shortCode}`.
6. Bound TTL by configured maximum and URL expiration, with jitter. Disabled/deleted/ineligible entries receive a short TTL; database misses are not cached.
7. Invalidate cache through immutable short-code events handled by `@TransactionalEventListener(AFTER_COMMIT)`.
8. Use a bounded in-process single-flight map to collapse concurrent misses inside one JVM. On capacity exhaustion or timeout, fall back directly to PostgreSQL.

## Consequences
- Redis improves redirect latency for hot short codes.
- PostgreSQL remains authoritative.
- Application must handle Redis failures gracefully.
- Cache invalidation must occur only after a successful database commit.
- Cache entries deliberately exclude JPA entities, owner data, authentication data, tokens, cookies, headers, and click analytics metadata.
- Single-flight protection is local to a single JVM and does not prevent duplicate PostgreSQL reads across multiple application instances.
