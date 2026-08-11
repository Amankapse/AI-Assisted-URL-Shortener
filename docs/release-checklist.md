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
- [x] `git diff --check`
- [x] repository secret scan

## Evidence

- [x] Backend test count recorded: 118
- [x] Frontend test count recorded: 21
- [x] JaCoCo line/branch coverage recorded: 85.11% / 60.78%
- [x] Angular bundle size recorded: 103.64 kB raw / 26.71 kB estimated transfer
- [x] Flyway V1-V10 validation recorded
- [x] PostgreSQL Testcontainers evidence recorded
- [x] Redis Testcontainers evidence recorded
- [x] Docker Compose validation recorded
- [x] CI configuration reviewed
- [ ] Remote GitHub Actions run checked after push

## Deployment

- [ ] Backend Render Web Service branch selected
- [ ] Backend health check `/actuator/health/liveness`
- [ ] Neon JDBC URL uses `sslmode=require`
- [ ] Render Key Value internal URL configured
- [ ] `APP_PUBLIC_BASE_URL` points to backend redirect base
- [ ] `APP_AUTH_ALLOWED_ORIGINS` includes exact frontend origin
- [x] Frontend Render Static Site root documented: `frontend`
- [x] Frontend build command documented: `npm ci && npm run build:render`
- [x] Frontend publish directory documented: `dist/frontend/browser`
- [x] Static Site rewrite documented: `/* -> /index.html`
- [x] Static Site security/cache headers documented

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

## Operations

- [ ] Liveness checked
- [ ] Readiness checked
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
