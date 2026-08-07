# AI Prompt Log

This document records the major prompts used during AI-assisted planning and implementation.

| ID | Objective | Prompt Summary | Result | Decision |
| --- | --- | --- | --- | --- |
| P-001 | Requirements analysis | Analyze assignment and propose Phase 2 core implementation plan | Produced a phase-aligned implementation plan | Accepted |
| P-002 | Architecture validation | Confirm modular monolith package structure and owner abstraction design | Validated package ownership and service boundaries | Accepted |
| P-003 | Domain modeling | Generate JPA entities and relationships for users, URLs, and clicks | Created entity classes and repository interfaces | Accepted |
| P-004 | Migration design | Create Flyway schema migration for UUID PKs and owner FK | Added initial schema migration file | Accepted |
| P-005 | URL validation | Define original URL, alias, and expiration rules | Added validation service and safe URL checks | Accepted |
| P-006 | Owner provider | Introduce temporary current-owner abstraction for Phase 2 | Added `CurrentOwnerProvider` and placeholder implementation | Accepted |
| P-007 | Service layer | Implement `UrlService` with owner-aware repository access | Added create/get/list/update/delete/enable/disable logic | Accepted |
| P-008 | Controller layer | Expose REST endpoints without accepting owner input | Added URL creation and management controllers | Accepted |
| P-009 | Error handling | Define RFC7807-style exception handling | Added global `ProblemDetailExceptionHandler` | Accepted |
| P-010 | Unit testing | Update `UrlServiceTests` to align with owner abstraction | Fixed owner lookup mocking and validation assertions | Accepted |
| P-011 | Dependency resolution | Fix missing OpenAPI and Flyway dependencies that blocked compile | Updated `pom.xml` with required dependencies | Accepted |
| P-012 | Build validation | Run targeted Maven tests after refactor | Verified `UrlServiceTests` pass | Accepted |
| P-013 | Phase 2 validation | Resolve Maven/Testcontainers/Flyway validation blockers without Phase 3 work | Added test Docker API pin, added approved Flyway PostgreSQL module, switched test profile to Hibernate schema validation | Accepted |
| P-014 | Phase 2 end-to-end validation | Expand Phase 2 tests across repositories, services, controllers, redirects, OpenAPI, pagination, collisions, and RFC7807 errors | Added regression coverage and fixed defects found after Flyway/Testcontainers startup | Accepted |
| P-015 | Phase 3 authentication | Implement approved authentication, authorization, and secure ownership refinements without Redis/rate limiting/observability/admin expansion | Added Spring Security resource server, RS256 JWT, refresh-token rotation, CSRF refresh/logout, secure current-owner provider, V2 refresh-token migration, tests, and docs | Accepted |
| P-016 | Phase 4 Redis and analytics | Implement approved Redis cache-aside and click analytics refinements without Phase 5 work | Added Redis redirect cache, single-flight miss protection, async sanitized analytics, V3 migration, owner/admin analytics endpoints, tests, and docs | Accepted |
| P-017 | Phase 5 operational readiness | Implement approved observability, Redis Lua rate limiting, health/readiness, security hardening, k6 scripts, and operations documentation without new business features | Added bounded Micrometer metrics, correlation IDs, protected Actuator metrics, Redis Lua limiter policies, security headers, production analytics pepper validation, k6 scripts, tests, and docs | Accepted |

## P-013 Validation Notes

- Blocker: Flyway failed during PostgreSQL Testcontainers startup with `Unsupported Database: PostgreSQL 15.18`.
- Root cause: Flyway 11 separates PostgreSQL database support from `flyway-core`.
- Approved correction: retained `flyway-core` and added `org.flywaydb:flyway-database-postgresql` without an explicit version.
- Test environment correction: added `src/test/resources/docker-java.properties` with `api.version=1.44` so Testcontainers 1.x can talk to Docker Engine 29.
- Validation: `.\mvnw.cmd clean verify` passed with PostgreSQL Testcontainers, Flyway V1 migration, and Hibernate `ddl-auto=validate`.
- Dependency validation: `.\mvnw.cmd dependency:tree` confirmed `flyway-core:11.7.2` and `flyway-database-postgresql:11.7.2`.
- Follow-up: after Flyway started successfully, later failures were treated as application/test defects. The Phase 2 suite was expanded in P-014.

## P-014 Validation Notes

- Scope: Phase 2 validation only. No Phase 3 auth/JWT/Redis cache/rate limiting/observability work was started.
- Added tests for short-code generation, URL validation, owner-aware URL service behavior, short-code collision retries and exhaustion, redirect resolution, repository constraints and pagination on PostgreSQL Testcontainers, controller DTO validation, redirect behavior, RFC7807 responses, and OpenAPI exposure.
- Defects found after Flyway startup:
  - PostgreSQL named enum writes failed for `UserRole` and `UserStatus`; fixed Hibernate enum mapping with `SqlTypes.NAMED_ENUM`.
  - Read/manage operations could create a placeholder owner; fixed them to require an existing owner while keeping create as the bootstrap path.
  - Collision exhaustion surfaced as `IllegalStateException`; fixed to return a safe `BadRequestException`.
  - Malformed JSON fell through to the generic 500 handler; added an RFC7807 malformed-request handler.
  - Controller request/path variables relied on missing Java parameter metadata; made binding names explicit.
  - Pagination bounds were not enforced at the controller boundary; added `@Min`/`@Max` validation.
  - `springdoc-openapi-starter-webmvc-ui:2.2.0` was incompatible with Spring Boot 3.5 / Spring Framework 6.2 and failed `/v3/api-docs`; updated to the Spring Boot 3.5-compatible `2.8.6` line.
- Validation:
  - `.\mvnw.cmd clean verify` passed with 41 tests, PostgreSQL Testcontainers, Flyway V1 migration, and Hibernate schema validation.
  - `.\mvnw.cmd dependency:tree` passed and confirmed `flyway-core:11.7.2`, `flyway-database-postgresql:11.7.2`, and `springdoc-openapi-starter-webmvc-ui:2.8.6`.
  - `docker compose config` passed; Docker Compose reported only the existing obsolete `version` attribute warning.
  - `.\scripts\verify.ps1` was not run because that file does not exist.
- Coverage: no JaCoCo plugin is configured yet, so line/branch coverage was not generated during this validation.

## P-015 Validation Notes

- Scope: Phase 3 authentication, authorization, and secure ownership only. Redis cache, rate limiting, observability expansion, and admin feature expansion were not started.
- Approved dependencies: added only `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server`. No explicit `spring-security-oauth2-jose` dependency was added; it is present transitively from resource server.
- Approved migration: added `V2__authentication_refresh_tokens.sql`; `V1__initial_schema.sql` was not modified.
- Carried-forward blocker record: Flyway PostgreSQL support was missing from `flyway-core`; root cause was Flyway 11 separating database-specific support; approved correction was the production/runtime `org.flywaydb:flyway-database-postgresql` dependency managed by Spring Boot. Validation reconfirmed `flyway-core:11.7.2` and `flyway-database-postgresql:11.7.2`.
- Defects found after Flyway started were treated as code/test defects:
  - V2 digest column initially mismatched Hibernate validation and was corrected to `VARCHAR(64)`.
  - Refresh-token family revocation was rolled back by the outer transaction during reuse detection; fixed with no-rollback handling and explicit family revocation.
  - Invalid refresh responses needed to clear the cookie; fixed at the controller boundary.
  - Repository integration cleanup needed to delete refresh tokens before users because of the V2 foreign key.
- Validation:
  - `.\mvnw.cmd clean verify` passed with 45 tests.
  - PostgreSQL Testcontainers started successfully using Docker Desktop over the local npipe strategy.
  - Flyway applied `V1__initial_schema.sql` and `V2__authentication_refresh_tokens.sql`.
  - Hibernate schema validation succeeded against PostgreSQL 15.18.
  - `.\mvnw.cmd dependency:tree` passed and confirmed Spring Boot-managed compatible versions: `flyway-core:11.7.2`, `flyway-database-postgresql:11.7.2`, Spring Security `6.5.0`, and transitive `spring-security-oauth2-jose:6.5.0`.
  - `docker compose config` passed with only the existing obsolete `version` attribute warning.

## P-016 Validation Notes

- Scope: Phase 4 Redis caching and click analytics only. No rate limiting, broad observability expansion, retention jobs, or Phase 5 work was started.
- Approved dependency: added only `spring-boot-starter-data-redis`; no direct `redis.clients:jedis` dependency was added.
- Approved migration: added `V3__click_analytics_indexes.sql`; `V1__initial_schema.sql` and `V2__authentication_refresh_tokens.sql` were not modified.
- Carried-forward blocker record: Flyway PostgreSQL support is separated from `flyway-core`; approved correction remains the production/runtime `org.flywaydb:flyway-database-postgresql` dependency managed by Spring Boot. Validation reconfirmed `flyway-core:11.7.2` and `flyway-database-postgresql:11.7.2`.
- Defects found after Flyway started were treated as migration/schema/mapping/test defects:
  - Physical URL deletion violated the `click_events` foreign key once analytics existed; fixed by introducing soft delete in V3 and filtering normal owner/redirect queries while preserving analytics history.
  - Existing auth test cleanup did not delete short URLs before users after cross-suite data was present; fixed cleanup order.
  - Redirect analytics assertions raced the async queue; fixed tests to use bounded eventual assertions.
- Validation:
  - `.\mvnw.cmd clean verify` passed with 60 tests.
  - PostgreSQL Testcontainers started successfully using Docker Desktop over the local npipe strategy.
  - Redis Testcontainers started successfully for cache integration validation.
  - Flyway validated 3 migrations and applied V1, V2, and V3.
  - Hibernate schema validation succeeded against PostgreSQL 15.18.
  - `.\mvnw.cmd dependency:tree` passed and confirmed `flyway-core:11.7.2`, `flyway-database-postgresql:11.7.2`, `spring-boot-starter-data-redis:3.5.0`, Lettuce `6.5.5.RELEASE`, and no direct Jedis dependency.
  - `docker compose config` passed with only the existing obsolete `version` attribute warning.

## P-017 Validation Notes

- Scope: Phase 5 operational readiness only. No Phase 6 work, new business features, authentication redesign, Redis cache redesign, analytics redesign, migration change, or new production dependency was introduced.
- Approved correction/design:
  - Used the existing `StringRedisTemplate` with an atomic Redis Lua fixed-window script.
  - Added configuration-driven limiter policies for registration, login, refresh, URL creation, public redirect, and admin analytics.
  - Kept login/register/refresh fail-closed, public redirect fail-open, and URL/admin limiters fail-open as documented.
  - Protected `/actuator/metrics` behind `ROLE_ADMIN`; exposed only health, liveness, readiness, info, and metrics at management exposure level.
  - Centralized `X-Correlation-ID` validation in a servlet filter and reused the same ID in Problem Details.
- Defects and refinements found during Phase 5:
  - Root health/readiness initially became 503 when Redis was unavailable because Redis health was still part of aggregate health status; fixed by excluding Redis health and documenting Redis degradation through metrics/logs.
  - URL creation alias conflicts initially double-counted failure metrics as both alias conflict and validation; fixed to record a single bounded reason.
  - A Redis Testcontainers integration test left the Lettuce client active until container shutdown; added explicit client cleanup.
- Validation:
  - `.\mvnw.cmd clean verify` passed with 72 tests.
  - PostgreSQL Testcontainers started successfully using Docker Desktop over the local npipe strategy.
  - Redis Testcontainers started successfully for cache and rate-limit integration validation.
  - Flyway validated 3 migrations and applied V1, V2, and V3.
  - Hibernate schema validation succeeded against PostgreSQL 15.18.
  - `.\mvnw.cmd dependency:tree` passed and confirmed `flyway-core:11.7.2`, `flyway-database-postgresql:11.7.2`, Spring Boot Actuator `3.5.0`, Micrometer `1.15.0`, `spring-boot-starter-data-redis:3.5.0`, Lettuce `6.5.5.RELEASE`, and no added rate-limiting library.
  - `docker compose config` passed with only the existing obsolete `version` attribute warning.
  - k6 was not executed because it is not installed in this environment.
  - `scripts/verify.sh` was attempted but failed before Maven startup because Bash did not have `JAVA_HOME` configured; the equivalent Windows validation commands passed.

## P-017 AI Output Examples

### Accepted

AI-generated Redis Lua fixed-window limiting through `StringRedisTemplate` was accepted because it met the no-new-dependency requirement and keeps Redis operations atomic.

### Edited

AI-generated URL creation metrics initially counted a custom-alias conflict twice. The implementation was edited to emit only `reason=alias_conflict`.

### Rejected

Treating Redis health as part of readiness was rejected after tests showed Redis outage made health probes fail. Redis is not required for correctness because PostgreSQL fallback exists, so Redis was removed from Actuator health readiness and is tracked through degradation metrics instead.

## AI was wrong example

### Prompt

> Generate Testcontainers dependencies for Redis.

### AI Output

Suggested:

```text
org.testcontainers:redis
```

### Human Review

This dependency does not exist in Maven Central as a standalone artifact.

### Action

Rejected.

Replaced with:

```text
org.testcontainers:testcontainers
GenericContainer<>("redis:7-alpine")
```

### Validation

```bash
./mvnw clean verify
```

Passed.
