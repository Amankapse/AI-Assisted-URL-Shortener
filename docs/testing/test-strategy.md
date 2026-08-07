# Testing Strategy

## Test tiers

- Unit tests: validate service, validation, and Phase 2 ownership logic in isolation.
- Integration tests: verify API flows against PostgreSQL using Testcontainers.
- Security tests: validate authentication, authorization, CSRF decisions, token handling, secure ownership, and deny-by-default behavior.
- Regression tests: preserve existing behavior during brownfield enhancements.

## Key coverage areas

- Phase 2 URL foundation: create, read, list, update expiration, delete, enable/disable, custom alias validation, generated short-code collision retry, and collision exhaustion.
- Phase 2 ownership: service and repository access are scoped through `CurrentOwnerProvider` and `OwnerIdentity`; production request DTOs do not accept owner identifiers.
- Phase 2 redirect: active redirects return 302 and disabled, expired, deleted, or missing links return safe RFC7807 responses.
- Phase 2 persistence: PostgreSQL Testcontainers runs Flyway V1, Hibernate schema validation, unique constraints, foreign keys, optimistic versioning, owner filtering, pagination, and short-code resolution checks.
- Phase 2 API: MockMvc verifies DTO validation, malformed JSON, pagination bounds, OpenAPI `/v3/api-docs`, and response DTOs that do not expose JPA internals.
- Phase 3 auth/security: registration, login, BCrypt hashing, generic authentication failures, RS256 JWT validation, issuer/audience/expiration/claim enforcement, non-RS256 rejection, refresh-token rotation and reuse detection, CSRF-protected refresh/logout, explicit cookie clearing, URL ownership enforcement from JWT `sub`, owner-input injection rejection, and deny-by-default admin path behavior.
- Phase 4 Redis cache: cache hit/miss behavior, malformed and unsupported cache entries, TTL bounding, Redis read/write/eviction failure fallback, after-commit invalidation through Redis integration, and bounded single-flight concurrency/timeout/capacity behavior.
- Phase 4 analytics: sanitized click metadata, bounded queue overload drops, retry behavior, idempotent batch persistence, atomic aggregate updates, owner analytics, admin analytics authorization, top-links limits, soft-delete analytics preservation, and Redis Testcontainers integration.
- Phase 5 operations: Redis Lua rate limiting, 429 RFC7807 responses, correlation ID validation, security headers, Actuator exposure, liveness/readiness policy, application metric counters, sensitive metric tag prevention, and documented Redis/PostgreSQL failure behavior.
- Future areas: retention jobs, mutation testing, and formal SAST/SCA tooling.

## Tooling

- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers for PostgreSQL and Redis integration tests
- MockMvc for controller/security tests
- JaCoCo for coverage reporting
- k6 scripts for load/performance validation

## Historical phase validation results

The following phase counts are retained as historical milestones. The current final suite contains 72 tests.

## Historical Phase 2 validation result

The Phase 2 validation suite now contains 41 tests and passes with:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd dependency:tree
docker compose config
```

The final green run used PostgreSQL Testcontainers with `jdbc:tc:postgresql:15-alpine:///shortener`, applied `V1__initial_schema.sql`, and validated Hibernate mappings against the migrated schema.

## Historical Phase 3 validation result

The Phase 3 validation suite contains 45 tests and passes with:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd dependency:tree
docker compose config
```

The final green run used PostgreSQL Testcontainers, applied `V1__initial_schema.sql` and `V2__authentication_refresh_tokens.sql`, validated Hibernate mappings against the migrated schema, and verified the complete current test suite. The dependency tree confirmed Spring Boot-managed `flyway-core:11.7.2`, `flyway-database-postgresql:11.7.2`, Spring Security `6.5.0`, and transitive `spring-security-oauth2-jose:6.5.0`.

## Historical Phase 4 validation result

The Phase 4 validation suite contains 60 tests and passes with:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd dependency:tree
docker compose config
```

The final green run used PostgreSQL Testcontainers and Redis Testcontainers, applied `V1__initial_schema.sql`, `V2__authentication_refresh_tokens.sql`, and `V3__click_analytics_indexes.sql`, validated Hibernate mappings against the migrated PostgreSQL schema, and verified the complete current test suite. The dependency tree confirmed Spring Boot-managed `flyway-core:11.7.2`, `flyway-database-postgresql:11.7.2`, `spring-boot-starter-data-redis:3.5.0`, Lettuce `6.5.5.RELEASE`, and no direct Jedis dependency. `docker compose config` passed with only the existing obsolete `version` warning.

## Final validation result

The final validation suite contains 72 tests and passes with:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd dependency:tree
docker compose config
```

The final suite includes tests for Redis-backed limiter isolation and outage behavior, generic 429 Problem Details, correlation ID sanitization and propagation, Actuator exposure, security headers, liveness/readiness policy, application metric counters, and bounded Micrometer tags.

The final green run used PostgreSQL Testcontainers and Redis Testcontainers, validated Flyway V1, V2, and V3, applied all three migrations to clean PostgreSQL containers, and completed Hibernate schema validation. `dependency:tree` confirmed Spring Boot-managed `flyway-core:11.7.2`, `flyway-database-postgresql:11.7.2`, Micrometer `1.15.0`, Spring Boot Actuator `3.5.0`, `spring-boot-starter-data-redis:3.5.0`, Lettuce `6.5.5.RELEASE`, Testcontainers `1.21.0`, and no added rate-limiting library. `docker compose config` passed without warnings after removing the obsolete Compose `version` attribute.

`scripts/verify.sh` was attempted, but the Bash environment failed before Maven startup because `JAVA_HOME` is not defined there. The equivalent required Windows commands above passed.
