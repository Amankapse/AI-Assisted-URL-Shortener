# Release Readiness Checklist

## Repository Audit

Reviewed source, tests, dependencies, Docker configuration, CI workflow, environment configuration, migrations, README, ADRs, architecture docs, API docs, testing docs, security docs, AI traceability docs, operational docs, and performance scripts.

Release gaps found and addressed:

- CI workflow was minimal; expanded with Docker check, Java 21 setup, Maven cache, `./mvnw clean verify`, Compose validation, and test/coverage artifacts.
- JaCoCo was not configured; added Maven plugin and coverage summary.
- `compose.yaml` used obsolete top-level `version`; removed.
- README was Phase 5-focused; rewritten for final reviewer submission.
- Final engineering summary was missing; added `docs/final-engineering-summary.md`.
- Phase 6 traceability was missing; updated AI logs.

No product feature, architecture redesign, microservice split, Kafka, authentication redesign, or migration need was identified.

## Secret And Sensitive-Data Audit

Checks performed:

- Searched repository excluding `.git` and `target` for private-key markers, API token patterns, refresh-token assignments, analytics pepper values, JWT secret names, and secret-looking assignments.
- Verified `.gitignore` excludes `.env`, logs, temp files, `target`, IDE files, and Maven wrapper jar.
- Reviewed `.env.example` and converted secret-bearing values to placeholders.
- Reviewed production config defaults and test profile separation.

Result:

- No private key committed.
- No `.env` committed.
- No real JWT secret, refresh token, API token, analytics pepper, certificate, or private key found.
- Test-only auth keys are generated in the `test` profile; production must provide RSA keys through environment variables.

## Dependency Audit

Command:

```powershell
.\mvnw.cmd dependency:tree
```

Result:

- Build succeeded.
- Spring Boot dependency management controls core dependency versions.
- Removed redundant explicit Testcontainers BOM/property after audit showed Boot resolves Testcontainers to `1.21.0`.
- No direct Jedis dependency.
- No new rate-limiting library.
- No unexpected authentication/security library beyond Spring Security OAuth2 resource server and transitive JOSE support.
- Flyway modules are aligned: `flyway-core:11.7.2` and `flyway-database-postgresql:11.7.2`.

## Static And Security Checks

Configured checks:

- Maven compile/test verification.
- JaCoCo coverage report.
- Docker Compose config validation.
- Manual secret-pattern scan.
- Manual Spring Security configuration review.

Not configured:

- SAST plugin.
- SCA/vulnerability scanning plugin.
- Mutation testing.

These were not added at the final stage to avoid introducing unstable tooling without prior approval.

## Final Security Sanity Check

Confirmed:

- No private keys or `.env` committed.
- No raw refresh tokens, analytics pepper, passwords, or production secrets committed.
- CORS uses explicit configured origins and no wildcard credentials.
- No public admin creation endpoint exists.
- Public `permitAll()` endpoints are limited to auth entrypoints, public redirect, health probes, OpenAPI/Swagger, and documented public resources.
- URL APIs do not accept client owner IDs.
- Metrics endpoint requires `ROLE_ADMIN`; sensitive Actuator endpoints are not exposed.
- Test-only insecure behavior is isolated to the `test` profile or local placeholders.

## Remaining Warnings

- Mockito dynamic-agent warning remains. It is documented and non-blocking for the current Java 21 build.
- `scripts/verify.sh` requires `JAVA_HOME` in Bash environments. Windows validation should use `.\mvnw.cmd`; GitHub Actions sets Java through `actions/setup-java`.
- k6 is not installed locally, so performance scripts were not executed.
- Remote GitHub Actions status is pending until the branch is pushed and CI runs on GitHub.
