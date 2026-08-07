# Final Engineering Summary

## Requirement Interpretation

The original URL-shortener requirement was normalized into phased, testable requirements: core URL management, secure ownership, authentication, PostgreSQL persistence, Redis redirect optimization, analytics, observability, rate limiting, operational readiness, and final release evidence. Ambiguous terms such as "production-ready" and "enterprise-ready" were translated into explicit concerns: security boundaries, failure modes, reproducible tests, migrations, metrics, runbooks, rollback guidance, and honest limitations.

## Architecture

The application uses a modular monolith because the assessment benefits from clear package boundaries without distributed-system overhead. The modules are organized around auth, user, URL, redirect, analytics, security, common infrastructure, and configuration.

PostgreSQL is the source of truth for users, URLs, refresh-token digests, and analytics. Flyway owns schema evolution and Hibernate validates mappings against the migrated schema.

Redis is used only as an optimization for redirect cache-aside and distributed rate limiting. Redirect correctness remains PostgreSQL-backed, and Redis health does not make the service unready.

Spring Security was chosen over a custom authentication filter so JWT validation, CSRF, CORS, authorization, and security headers rely on maintained framework mechanisms. Access tokens are RS256 JWTs; refresh tokens are opaque, rotated, stored as SHA-256 digests, and protected by CSRF when transported by cookie.

Analytics are asynchronous and bounded so public redirect latency is protected. This deliberately trades perfect analytics completeness for service availability under pressure.

## Greenfield Scenario

The greenfield work decomposed the shortener into domain entities, repositories, services, DTOs, validation, controllers, migrations, tests, and documentation. AI assistance was used to draft initial structures and tests, but output was reviewed against the modular-monolith rules, security constraints, and PostgreSQL/Flyway requirements.

Validation included repository integration tests, service tests, MockMvc controller tests, DTO validation tests, redirect tests, OpenAPI exposure checks, pagination checks, collision behavior, and RFC7807 errors.

## Brownfield Scenario

Several later phases enhanced the working foundation without redesigning it.

Redis caching added cache-aside redirect lookup, bounded TTLs, schema-versioned cache values, PostgreSQL fallback, and after-commit invalidation. Impacted components included redirect service, cache service, URL mutation operations, configuration, tests, and docs. Risks were cache poisoning, stale redirects, Redis failure, and stampede behavior; tests covered hit/miss/failure/invalidation paths.

Analytics added sanitized click events, queue bounds, retry behavior, owner analytics, admin analytics, and soft delete to preserve history. Risks included privacy leakage, queue saturation, write amplification, and foreign-key conflicts; tests covered sanitization, persistence, overload, authorization, and soft-delete behavior.

Phase 5 added observability, rate limiting, correlation IDs, health behavior, and security hardening without changing business behavior. Risks included high-cardinality metrics, excessive Actuator exposure, unsafe rate-limit failure modes, and readiness tied to Redis. Tests and documentation closed those risks.

## Ambiguous Scenario

"Make it enterprise ready" was treated as an ambiguous request and converted into concrete acceptance criteria:

- deny-by-default security
- JWT and refresh-token validation
- CSRF rationale
- owner enforcement without client owner IDs
- Redis as optional optimization
- bounded queues and timeouts
- rate limiting with explicit failure modes
- liveness/readiness policy
- metrics with bounded tags
- documented runbooks and rollback behavior
- reproducible Docker/Testcontainers validation
- performance scripts with no fabricated results

The implementation intentionally stopped short of claiming enterprise deployment features that are not present, such as multi-region operation, Kubernetes manifests, external secret-manager integration, or durable event streaming.

## AI-Assisted Engineering

AI was used to transform requirements into structured specs, propose implementation slices, draft code and tests, and prepare documentation. Human review accepted, edited, or rejected outputs based on correctness, security, maintainability, and scope control.

Accepted examples:

- Modular monolith package structure with clear service/repository/controller boundaries.
- Redis cache-aside redirect resolution with PostgreSQL fallback.
- Redis Lua fixed-window rate limiting using the existing `StringRedisTemplate`.

Edited examples:

- Ownership evolved from a Phase 2 placeholder provider to `SecurityCurrentOwnerProvider` backed by JWT subject while preserving the `CurrentOwnerProvider` abstraction.
- Flyway validation was corrected by adding `flyway-database-postgresql` after identifying that PostgreSQL support is separated from Flyway core.
- URL creation metrics were adjusted to avoid double-counting custom alias conflicts.

Rejected examples:

- Invalid `org.testcontainers:redis` dependency; replaced with `GenericContainer<>("redis:7-alpine")`.
- A custom JWT filter approach; replaced with Spring Security resource server support and `NimbusJwtDecoder`.
- Redis readiness dependency; rejected because Redis is not required for correctness and would cause bad orchestration behavior.

Traceability is maintained in `docs/ai-assisted-engineering/`.

## Engineering Judgment

The implementation chose simpler, safer approaches where appropriate:

- Modular monolith instead of microservices to avoid unnecessary distributed complexity.
- No Kafka or durable broker in the prototype; analytics are best-effort and bounded.
- PostgreSQL is authoritative; Redis failures degrade performance, not correctness.
- Single-flight is in-process and documented rather than pretending to solve cross-node stampedes.
- Security is deny-by-default, with explicit public endpoints and explicit admin endpoints.
- Metrics avoid user IDs, URL IDs, short codes, emails, IP addresses, token IDs, and exception messages as tags.
- No Phase 6 feature work was added.

## Validation

Final release validation includes:

- `.\mvnw.cmd clean verify`
- `.\mvnw.cmd dependency:tree`
- `docker compose config`
- PostgreSQL Testcontainers
- Redis Testcontainers
- Flyway V1, V2, V3 validation and application
- Hibernate schema validation
- JaCoCo coverage report generation
- GitHub Actions workflow definition
- k6 scripts for reproducible performance smoke testing

Final local results:

- Tests: 72 passing
- Line coverage: 85.08%
- Branch coverage: 64.46%
- Docker: PostgreSQL and Redis Testcontainers started successfully
- Migrations: Flyway V1, V2, and V3 validated and applied
- Schema: Hibernate validation succeeded
- Compose: `docker compose config` passed without warnings

The local environment did not have k6 installed, so no measured performance results are claimed. The GitHub Actions workflow is defined but must be verified remotely after the branch is pushed.

## Risks And Trade-Offs

Known limitations:

- Analytics are best-effort and can drop events under overload.
- No durable event broker.
- No distributed single-flight.
- No external secret manager integration.
- No Kubernetes, autoscaling, or multi-AZ deployment manifests.
- Registration duplicate behavior can reveal that an email already exists.
- CI workflow must be verified remotely after the branch is pushed.
- Local load testing was not executed because k6 was unavailable.

## Production Evolution

At enterprise scale, the next evolution would include:

- managed PostgreSQL with backups, PITR, replicas, and migration runbooks
- managed Redis with authentication, TLS, monitoring, and eviction policies
- external secret manager for RSA keys, database credentials, Redis credentials, analytics pepper, and rate-limit salt
- centralized logs, metrics, traces, dashboards, and alerts
- durable event broker for analytics ingestion
- distributed stampede protection or lease/lock strategy
- Kubernetes or equivalent deployment manifests
- autoscaling, multi-AZ deployment, and infrastructure DDoS protection
- formal SAST/SCA/secret-scanning gates
- measured load tests in representative infrastructure
