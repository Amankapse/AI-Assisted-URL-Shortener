# AI-Assisted Execution Plan

## Phase 0: Assessment and planning

Objective
- Understand the assessment requirements.
- Normalize ambiguous requirements.
- Define architecture and execution phases.
- Create key documentation artifacts.

Dependencies
- Assignment requirements.
- Repository structure.

Expected outputs
- Requirements documents.
- Architecture overview and ADRs.
- Scenario definitions.
- AI traceability scaffolding.

Acceptance criteria
- Documentation for requirements, architecture, and scenarios exists.
- Execution plan defined with phases.
- AI traceability and approval logs are initialized.

Tests
- Review documentation for completeness.

Security considerations
- Validate security decisions before implementation.

Performance considerations
- Establish performance assumptions and validation strategy.

Rollback
- No code changes.

Human approval
- Required before changing architecture, API contracts, auth, or adding dependencies.

## Phase 1: Repository foundation

Objective
- Create repository structure, build config, Docker compose, and initial CI workflow.

Dependencies
- Approved architecture and module boundaries.

Expected outputs
- `pom.xml`
- `Dockerfile`
- `compose.yaml`
- `.github/workflows/ci.yml`
- `.env.example`
- base directories and placeholder docs

Acceptance criteria
- Project builds cleanly.
- Docker compose config is valid.
- CI workflow can run Maven verify.

Tests
- `./mvnw clean verify`
- `docker compose config`

Security considerations
- No secrets in repo.
- Base image and plugin versions approved.

Performance considerations
- Build using reproducible dependencies.

Rollback
- Remove foundation files.

Human approval
- Required before adding new build or CI dependencies.

## Phase 2: Core URL-shortening capability

Objective
- Implement user registration/login, short URL creation, redirect, and paginated URL history.

Dependencies
- Repository foundation.
- Security configuration.

Expected outputs
- `auth` module
- `url` module
- `redirect` endpoint
- DTOs and service layer

Acceptance criteria
- Create and redirect flows work.
- Ownership controls work.
- API contract documented.

Tests
- Unit tests for auth and URL services.
- Integration tests for auth and redirect.

Security considerations
- Strong password hashing.
- Ownership enforcement.

Performance considerations
- Validate route latency and DB queries.

Rollback
- Revert feature implementation.

Human approval
- Required before changing auth model or API contract.

## Phase 3: Security and auth hardening

Objective
- Add JWT refresh tokens, roles, CSRF rationale, and secure headers.

Dependencies
- Core URL feature.
- Security module.

Expected outputs
- JWT auth config
- `USER` and `ADMIN` enforcement
- Security tests

Acceptance criteria
- Role-based access control works.
- Invalid and expired tokens rejected.

Tests
- Security controller tests.
- Auth failure tests.

Security considerations
- Token validation.
- Rate limiting for login.

Performance considerations
- Auth path latency.

Rollback
- Revert security config.

Human approval
- Required before auth/authorization changes.

## Phase 4: Redis caching and analytics

Objective
- Implement redirect cache-aside and click analytics.

Dependencies
- Core URL feature.
- Redis integration.

Expected outputs
- Redis cache config
- Analytics event capture
- Admin analytics API

Acceptance criteria
- Redis reduces redirect DB lookups.
- Redirects still work without Redis.
- Click events are recorded.

Tests
- Integration tests for cache hit/miss.
- Analytics recording tests.

Security considerations
- No auth caching.
- Analytics events do not leak data.

Performance considerations
- Cache TTL and fallback.

Rollback
- Remove cache layer.

Human approval
- Required before adding Redis or analytics storage dependencies.

## Phase 5: Observability, security hardening and performance validation

Objective
- Add health endpoints, metrics, runbook, and load test plan.
- Harden security configuration and validate performance assumptions.

Dependencies
- Full application.

Expected outputs
- Actuator endpoints
- Micrometer metrics
- `docs/operations`
- k6 scripts

Acceptance criteria
- Health/readiness endpoints pass.
- Metrics capture redirect and auth rates.
- Security hardening is documented and validated.
- Performance targets are defined and testable.

Tests
- Health endpoint tests.
- Performance validation checks.

Security considerations
- Do not expose sensitive info in health responses.
- Validate secure headers and CSRF decisions.

Performance considerations
- Validate metrics overhead.
- Measure redirect and create latency targets.

Rollback
- Revert observability and security config.

Human approval
- Review before adding monitoring or security dependencies.

## Phase 6: CI/CD and release readiness

Objective
- Finalize GitHub Actions, security scans, Docker build, and documentation.

Dependencies
- All application features.

Expected outputs
- CI workflow with tests and static analysis
- `CHANGELOG.md`
- Release readiness docs

Acceptance criteria
- CI passes.
- Documentation covers setup, testing, and risks.

Tests
- Full CI pipeline execution.

Security considerations
- Dependency and secret scans.

Performance considerations
- Validate container build.

Rollback
- Remove workflow changes.

Human approval
- Required before updating CI workflows or adding scans.
