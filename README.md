# AI-Assisted URL Shortener Assessment

This repository captures a phased, AI-assisted URL shortener assessment built with Java 21, Spring Boot 3.5, PostgreSQL, Flyway, JPA, Testcontainers, and OpenAPI.

## Purpose

This assessment is designed to demonstrate disciplined AI-assisted engineering execution rather than fully automated code generation. The repository will show:

- Requirement understanding and ambiguity resolution
- Task decomposition into phased, reviewable work
- AI usage in implementation, debugging, testing, documentation, and review preparation
- Traceability of generated, edited, and rejected AI outputs
- Human approval for high-impact decisions
- Architecture, security, and validation artifacts

## Current status

Phase 4 Redis caching and click analytics validation is complete:

- Requirement normalization
- Non-functional requirements
- Architecture overview and ADRs
- Three documented scenarios: greenfield, brownfield, ambiguous
- AI-assisted execution plan, prompt log, traceability matrix, and approval log
- Test strategy
- Core URL, redirect, repository, DTO validation, Flyway, PostgreSQL Testcontainers, pagination, collision, RFC7807, and OpenAPI tests
- RS256 JWT access tokens, BCrypt password hashing, opaque refresh-token rotation, CSRF-protected refresh/logout, explicit CORS allowlist, deny-by-default security, and owner-aware URL operations through `CurrentOwnerProvider`
- Production URL APIs remain free of owner-input request parameters
- Redis cache-aside redirect resolution with bounded TTLs, jitter, safe fallback to PostgreSQL, after-commit invalidation, and in-process single-flight protection
- Best-effort async click analytics with bounded queueing, sanitized IP/referrer/user-agent data, PostgreSQL event persistence, atomic aggregate updates, and owner/admin analytics APIs

## Next step

Phase 4 cache and analytics behavior is ready for review. Rate limiting, broader observability/metrics, retention jobs, and Phase 5 work remain future scope.

## Project structure

Key documentation added under `docs/`:

- `docs/requirements`
- `docs/architecture`
- `docs/scenarios`
- `docs/ai-assisted-engineering`
- `docs/testing`

## How to review

Read `AGENTS.md` first. Then review the requirements documents, ADRs, and execution plan before authorizing implementation.

## Validation

The current Phase 4 validation was completed with:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd dependency:tree
docker compose config
```

`clean verify` passed with 60 tests. Testcontainers started PostgreSQL 15 Alpine and Redis 7 Alpine for the Redis integration test. Flyway applied `V1__initial_schema.sql`, `V2__authentication_refresh_tokens.sql`, and `V3__click_analytics_indexes.sql`; Hibernate schema validation succeeded. `dependency:tree` confirmed `flyway-core:11.7.2`, `flyway-database-postgresql:11.7.2`, `spring-boot-starter-data-redis:3.5.0`, and Boot-managed Lettuce `6.5.5.RELEASE`. `docker compose config` passed with only the existing obsolete `version` warning.

Production/local JWT keys must be supplied through `APP_AUTH_PRIVATE_KEY_PEM` and `APP_AUTH_PUBLIC_KEY_PEM`. The `test` profile generates ephemeral RSA keys only for reproducible tests.
Production/local analytics IP hashing should set `APP_ANALYTICS_IP_HASH_PEPPER`; raw IP addresses, full user agents, full referrers, cookies, headers, tokens, and owner data are not stored in click events or Redis cache values.
