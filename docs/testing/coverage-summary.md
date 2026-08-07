# Coverage Summary

## Phase 2 validation

`.\mvnw.cmd clean verify` passed with 41 tests.

Covered areas:

- Short-code generation length, character set, sample uniqueness, collision retry, and collision exhaustion.
- URL validation for supported schemes, malformed values, reserved hosts, private/link-local hosts, aliases, and expiration.
- Owner-aware service behavior through `CurrentOwnerProvider` and `OwnerIdentity`.
- Repository constraints, foreign keys, indexes, optimistic locking, owner filtering, pagination, and short-code resolution on PostgreSQL Testcontainers.
- Redirect 302 behavior and disabled, expired, deleted, or missing-code failures.
- Controller DTO validation, malformed JSON, pagination bounds, safe response DTOs, and RFC7807 problem details.
- OpenAPI `/v3/api-docs` exposure and hiding of internal entity/repository types.

## Line coverage

JaCoCo is not configured in the current POM, so line and branch coverage reports were not generated during this validation. Adding JaCoCo remains a separate approved build-tooling change.

## Phase 3 validation

`.\mvnw.cmd clean verify` passed with 45 tests.

Additional covered areas:

- Registration and login behavior, including normalized email, BCrypt password hashes, safe DTOs, duplicate registration, invalid input, inactive users, and generic authentication failures.
- RS256 JWT authentication with strict issuer, audience, expiration, required-claim, signature, and algorithm rejection checks.
- Refresh-token persistence, SHA-256 digest storage, rotation, reuse detection, family revocation, CSRF protection, and cookie clearing.
- Secure ownership through `SecurityCurrentOwnerProvider` and JWT `sub` UUID, including attempted owner/user ID injection and cross-user access denial.
- Deny-by-default behavior for non-public paths, plus RFC7807 security responses.
- Flyway V2 refresh-token table and indexes on PostgreSQL Testcontainers.

## Phase 4 validation

`.\mvnw.cmd clean verify` passed with 60 tests.

Additional covered areas:

- Redis cache-aside redirect resolution, including cache hit, miss, malformed payload eviction, unsupported schema eviction, TTL bounding, disabled/ineligible short TTL, Redis read/write/eviction failure fallback, and key privacy.
- Redis Testcontainers integration for cache population and after-commit invalidation after URL disable.
- In-process single-flight behavior for concurrent misses, capacity fallback, timeout fallback, leader failure cleanup, and one-JVM limitation documentation.
- Async click analytics publisher behavior, including sanitized IP hash, referrer host, user-agent category, safe correlation ID, queue overload drops, transient retry, and bounded flush behavior.
- PostgreSQL analytics writer behavior, including idempotent event inserts, atomic SQL click-count increments, and click event persistence without raw IP/full referrer/full user agent.
- Owner-scoped analytics endpoints, daily analytics grouping, cross-owner denial, admin overview/top-links authorization, and prevention of admin bypass on normal URL APIs.
- Soft-delete behavior that preserves historical click events while excluding deleted links from normal owner and redirect queries.
- Flyway V3 `click_events.correlation_id`, `short_urls.deleted`, and `idx_click_events_url_clicked_at` migration validation on PostgreSQL Testcontainers.
