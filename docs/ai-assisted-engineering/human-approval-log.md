# Human Approval Log

This log records explicit approvals for decisions that affect architecture, security, public API, dependencies, or database migrations.

## Phase 0–1
- No implementation changes made.
- Approval pending for Phase 1 foundation and initial design.

## Phase 2 Validation
- Approved adding `org.flywaydb:flyway-database-postgresql` as a production/runtime dependency managed by Spring Boot dependency management.
- Constraints: retained `flyway-core`; did not modify migrations; did not disable Flyway; did not replace PostgreSQL with H2; did not suppress the unsupported-database error; did not begin Phase 3 auth/JWT/Redis/rate limiting/observability work.
- Validation: `.\mvnw.cmd clean verify` and `.\mvnw.cmd dependency:tree` passed after the approved correction.
- Phase 2 validation continued with end-to-end tests. Defects found after Flyway startup were fixed as application/test defects, including PostgreSQL enum mapping, owner lookup semantics, malformed JSON RFC7807 handling, explicit controller binding names, pagination validation, and Springdoc compatibility for Spring Boot 3.5.
- No database migration was changed and no Phase 3 feature work was introduced.

## Phase 3 Authentication
- Approved adding only `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server`.
- Approved using Spring Security resource server, `NimbusJwtDecoder`, RS256 JWT validation, and a custom JWT authentication converter instead of a custom bearer filter.
- Approved retaining `CurrentOwnerProvider` and replacing the placeholder provider with `SecurityCurrentOwnerProvider`; production URL APIs must remain free of owner-input request parameters.
- Approved adding only `V2__authentication_refresh_tokens.sql`; `V1__initial_schema.sql` was not modified.
- Approved opaque refresh tokens with SHA-256 digest storage, rotation, reuse detection, family revocation, CSRF-protected refresh/logout, and explicit CORS origins.
- Explicitly deferred Redis cache, rate limiting, observability expansion, and admin feature expansion.
- Validation: `.\mvnw.cmd clean verify` passed with 45 tests; `.\mvnw.cmd dependency:tree` passed; `docker compose config` passed with only the existing obsolete-version warning.

## Phase 4 Redis Caching and Click Analytics
- Approved adding only `spring-boot-starter-data-redis`; direct `redis.clients:jedis` was not added.
- Approved Redis cache-aside using key format `url:v1:redirect:{shortCode}`, versioned redirect DTO values only, bounded TTLs, jitter, short ineligible TTL, no database-miss caching, Redis failure fallback to PostgreSQL, and after-commit invalidation.
- Approved bounded in-process single-flight miss protection with PostgreSQL fallback on capacity exhaustion or timeout.
- Approved best-effort async click analytics with sanitized IP/referrer/user-agent/correlation data, bounded queueing, drops on overload, retry handling, idempotent event inserts, and atomic SQL aggregate updates.
- Approved owner analytics endpoints and explicit `ROLE_ADMIN` analytics endpoints; normal URL APIs still enforce owner scope and do not allow admin bypass.
- Approved adding `V3__click_analytics_indexes.sql`; `V1__initial_schema.sql` and `V2__authentication_refresh_tokens.sql` were not modified.
- Explicitly deferred rate limiting, broad observability expansion, retention jobs, and Phase 5 work.
- Validation: `.\mvnw.cmd clean verify` passed with 60 tests; `.\mvnw.cmd dependency:tree` passed; `docker compose config` passed with only the existing obsolete-version warning.
