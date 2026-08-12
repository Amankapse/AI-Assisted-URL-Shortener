# Production Release Checklist

## Validation

- [x] `.\mvnw.cmd clean verify`
- [x] `.\mvnw.cmd dependency:tree`
- [x] `docker compose config`
- [x] `cd frontend; npm ci`
- [x] `cd frontend; npm test -- --watch=false`
- [x] `cd frontend; npm run build`
- [x] `cd frontend; npm audit --audit-level=moderate`
- [x] `cd frontend; npm run build:render`
- [x] `docker build -t url-shortener-fullstack .`
- [x] local packaged-image route smoke with `PORT=10000`
- [x] `git diff --check`
- [x] repository secret scan

## Evidence

- [x] Backend test count recorded after Stage 10: 125
- [x] Frontend test count recorded after Stage 10: 28
- [x] JaCoCo line/branch coverage recorded: 85.07% / 60.15%
- [x] Angular bundle size recorded: 107.57 kB raw / 28.00 kB estimated transfer
- [x] Flyway V1-V11 validation recorded
- [x] PostgreSQL Testcontainers evidence recorded
- [x] Redis Testcontainers evidence recorded
- [x] Docker Compose validation recorded
- [x] CI configuration reviewed
- [ ] Remote GitHub Actions run checked after push

## Deployment

- [ ] Render Web Service branch selected
- [ ] Render health check `/actuator/health/liveness`
- [ ] Neon JDBC URL uses `sslmode=require`
- [ ] Render Key Value internal URL configured
- [ ] `APP_PUBLIC_BASE_URL` points to backend redirect base
- [ ] `APP_AUTH_ALLOWED_ORIGINS` remains explicit for external/future split clients
- [x] Single Docker Web Service packaging documented
- [x] Angular dist path documented: `frontend/dist/frontend/browser`
- [x] SPA forwards documented for `/`, `/login`, `/register`, and `/app/**`
- [x] Backend route exclusions documented for `/api`, `/r`, Actuator, Swagger, and OpenAPI
- [x] Packaged image confirmed to serve SPA routes and backend routes separately in local smoke

## Security

- [x] No `.env` committed
- [x] No PEM/private key committed
- [x] No real Neon/Redis URLs committed
- [x] No production peppers committed
- [x] No raw API keys committed
- [ ] Refresh cookie attributes inspected
- [ ] CSRF refresh/logout flow validated
- [ ] CORS allowlist validated
- [x] Frontend build scanned for secret markers
- [ ] API-key one-time display validated
- [ ] Sensitive endpoint cache behavior reviewed
- [x] No public admin-promotion endpoint or admin password bootstrap added
- [x] Bounded CMS content renders as text, not raw HTML

## Operations

- [x] Liveness checked in local packaged-image smoke
- [x] Readiness checked in local packaged-image smoke
- [ ] Redis degradation behavior documented
- [ ] Neon outage behavior documented
- [ ] Outbox status checked
- [x] Rollback plan reviewed
- [x] Known limitations documented
- [ ] Production smoke flow completed

## Known Limitations To Retain

- PostgreSQL mapping store is not the final 365B-record hyperscale store.
- PostgreSQL outbox is not Kafka/Kinesis/Pulsar.
- Local single-flight is JVM-local.
- Render Free can cold-start.
- Free Valkey persistence/capacity is limited.
- No multi-region deployment is implemented.
- No external secret manager is implemented in this prototype.
- No measured hyperscale load test has been run.
- Angular CLI dev-chain moderate audit findings remain pending upstream remediation.
