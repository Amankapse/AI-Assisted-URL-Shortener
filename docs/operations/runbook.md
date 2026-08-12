# Operational Runbook

## Startup

Prerequisites:

- Java 21
- Docker Desktop or compatible Docker runtime
- PostgreSQL 15
- Redis 7
- Environment-provided JWT RSA keys for non-test environments
- `APP_ANALYTICS_IP_HASH_PEPPER` from secret management in production
- `APP_API_KEY_HASH_PEPPER` from secret management in production
- `APP_AUDIT_METADATA_MAX_BYTES` and `APP_AUDIT_RETENTION` reviewed for the environment
- `APP_OUTBOX_*` dispatcher, retry, payload, and retention settings reviewed for the environment
- `SHORTENER_CODE_LENGTH`, `SHORTENER_CODE_MAX_RETRIES`, and quota settings reviewed for the environment
- Optional first-admin bootstrap `APP_BOOTSTRAP_ADMIN_EMAIL` used only for one existing registered user, then removed
- Angular public runtime config values set for the packaged Web Service:
  `FRONTEND_API_BASE_URL`, `FRONTEND_PUBLIC_SHORT_URL_BASE`, and `FRONTEND_ENVIRONMENT`

Start local dependencies:

```powershell
docker compose up -d postgres redis
```

Start the application with the required datasource, Redis, JWT, analytics, and rate-limit environment variables. Flyway runs on startup and applies forward-only migrations.

Current migrations are V1 through V11. V7 adds append-only application-level audit events with bounded JSONB metadata. V8 adds workspace-bound API keys and expands audit constraints for API-key create/revoke events. V9 adds transactional outbox events. V10 adds campaigns, tags, URL tag assignments, and search/filter indexes. V11 adds bounded site settings, content pages, announcements, media asset metadata, and CMS audit constraint updates. Audit rows intentionally avoid cascading foreign keys so records survive user, URL, workspace, and CMS content lifecycle changes.

## API latency increase

Check:

- `http.server.requests`
- Redis cache hit/miss/failure metrics
- PostgreSQL latency and Hikari pool usage
- analytics queue depth and dropped events
- CPU and memory
- rate-limit rejection/failure metrics
- quota rejection metrics
- short-code collision retry/exhaustion metrics
- outbox backlog, oldest pending age, retry, and dead-letter metrics

## Redis unavailable

Expected behavior:

- Redirect cache lookups miss/fail and fall back to PostgreSQL.
- Valid redirects continue if PostgreSQL is available.
- Public redirect and URL/admin rate limits fail open where configured.
- Login/register/refresh fail closed if Redis limiter is unavailable.
- Redis failure metrics and safe warnings increase.

## PostgreSQL unavailable

Expected behavior:

- Readiness fails.
- Writes and PostgreSQL-backed reads fail safely through Problem Details.
- Cache-hit redirects may continue only if the cache entry is present and valid, but correctness depends on PostgreSQL as source of truth.
- Liveness remains healthy unless the JVM/application itself is unhealthy.

## Analytics queue saturated

Expected behavior:

- Redirects continue.
- Dropped-event counter rises.
- Analytics completeness degrades.
- Queue depth remains bounded by configuration.

## URL quota exceeded

Expected behavior:

- Request returns RFC7807 `quota-exceeded` with HTTP 403.
- Rate limiting remains separate; quota exhaustion is a resource allowance failure, not request velocity.
- Review per-user daily creation, active-link, and custom-alias limits.
- In hyperscale production, replace repeated aggregate SQL counting with materialized/distributed counters and durable reconciliation.

## URL blocked for moderation

Expected behavior:

- Admin block/unblock endpoints are restricted to `ROLE_ADMIN`.
- Blocked redirects return safe not-found behavior.
- Owners cannot remove an administrative block through normal enable endpoints.
- Redis redirect cache is invalidated after the moderation transaction.
- Analytics history is retained.
- `URL_BLOCKED` and `URL_UNBLOCKED` audit events are recorded with the target workspace and platform admin actor.

## Audit trail query or write issue

Expected behavior:

- Mutating URL, workspace, membership, and moderation operations write audit rows in the same PostgreSQL transaction where practical.
- Audit persistence failure fails the mutation rather than silently losing the accountability record.
- Audit metadata is bounded by `APP_AUDIT_METADATA_MAX_BYTES` and stores safe metadata only.
- Destination changes store host and SHA-256 URL hashes, not raw destination URLs.
- Audit APIs are read-only and role-gated: workspace `OWNER`/`ADMIN` for workspace audit, workspace `OWNER`/`ADMIN`/`EDITOR` for URL audit, and platform `ROLE_ADMIN` for `/api/v1/admin/audit`.

## API key issue

Expected behavior:

- API keys authenticate only through `X-API-Key`.
- Revoked, expired, malformed, and unknown keys return generic HTTP 401 responses.
- API-key traffic is workspace-bound and scope-limited.
- API-key request velocity uses the `api-key` rate limiter and fails closed when Redis limiter state is unavailable.
- Create and revoke operations are audited; raw keys and digests are not logged.

Investigate:

- `APP_API_KEY_HASH_PEPPER` presence and consistency after deployment.
- `api-key` rate-limit rejections and Redis availability.
- API-key expiration and revocation state.
- Caller storage or accidental exposure of raw key material.

## Transactional outbox backlog or dead letters

Expected behavior:

- URL mutation events, durable cache invalidation work, and optional outbox analytics are stored in PostgreSQL as outbox rows.
- Dispatcher workers claim rows with PostgreSQL row locks and `SKIP LOCKED`.
- Transient handler failures retry with bounded exponential backoff and jitter.
- Exhausted or permanent failures move to `DEAD` with a stable error code.
- Liveness and readiness do not depend on dispatcher progress.

Investigate:

- `/api/v1/admin/outbox?status=DEAD` as a platform admin.
- `url_shortener.outbox.backlog` and `url_shortener.outbox.oldest_pending_age`.
- Redis availability when cache invalidation events are retrying.
- PostgreSQL health and Hikari pool metrics.
- Payload-size errors if creation fails before an outbox row is committed.

Do not manually update outbox rows in production without a documented repair plan. Retrying `DEAD` rows is intentionally not exposed through the public API in this stage.

## Frontend deploy issue

Expected single Render Web Service behavior:

- Docker build runs Angular `npm run build:render`.
- Docker build copies `frontend/dist/frontend/browser` into Spring Boot static resources before Maven package.
- `/`, `/login`, `/register`, and `/app/**` serve Angular.
- `/api/v1/**`, `/r/**`, `/actuator/**`, `/swagger-ui/**`, and `/v3/api-docs/**` remain backend routes.
- Public runtime config: `FRONTEND_API_BASE_URL`, `FRONTEND_PUBLIC_SHORT_URL_BASE`, and `FRONTEND_ENVIRONMENT`

Investigate:

- The built JAR contains `BOOT-INF/classes/static/index.html` and `BOOT-INF/classes/static/app-config.json`.
- `app-config.json` contains `apiBaseUrl: ""` for same-origin calls and no secrets.
- Browser network requests target `/api/v1/...`, not `localhost` or an internal Render URL.
- Direct Angular routes reload successfully through Spring MVC SPA forwarding.
- Health check remains `/actuator/health/liveness`, not `/`.

## Site experience or CMS issue

Expected behavior:

- Public `/api/v1/site/**` reads expose only safe published content.
- Platform-admin CMS APIs under `/api/v1/admin/site/**` require `ROLE_ADMIN`; workspace admins and API keys are not sufficient.
- CMS content is plain text/structured text and must not be rendered as raw HTML.
- Media assets are HTTPS URL metadata only; Stage 10 rejects SVG URLs.
- Redirects under `/r/{shortCode}` do not query CMS tables or APIs.
- `APP_BOOTSTRAP_ADMIN_EMAIL` promotes only an existing registered user and should be removed after successful promotion.

Investigate:

- CMS update failures caused by stale `If-Match` versions.
- `SITE_SETTINGS_UPDATED`, `CONTENT_PAGE_*`, `ANNOUNCEMENT_*`, `MEDIA_ASSET_*`, and `ADMIN_PROMOTED` audit events.
- Public content status and announcement audience/date filters.
- Missing external media or mixed-content browser blocking for administrator-provided asset URLs.

## CORS, cookie, or CSRF issue

Expected behavior:

- Backend CORS uses an explicit allowlist and does not use wildcard credentials.
- Refresh and logout remain CSRF protected.
- Refresh cookies stay `Secure` and `HttpOnly` in production.
- SameSite policy remains strict for the same-origin deployment.

Same-origin deployment should not require CORS for Angular's own API calls. If CORS errors appear in the browser, first inspect `app-config.json` for a malformed `apiBaseUrl`. Do not switch to `SameSite=None`, loosen CSRF, or add wildcard CORS as an emergency workaround.

## High login failures

Investigate:

- login failure metrics
- login rate-limit rejection metrics
- source network patterns at the trusted proxy layer
- user enumeration attempts
- JWT validation failures

## Refresh-token reuse detected

Expected behavior:

- Token family is revoked.
- User must log in again.
- Security metric and safe warning are recorded.
- Raw tokens are never logged.

## Backup and disaster recovery targets

Architecture targets:

- RPO <= 5 minutes.
- RTO <= 30 minutes.
- PostgreSQL requires PITR, backups, restore drills, and multi-AZ replication.
- Redis is rebuildable from the authoritative URL mapping store.
- Future durable event streams require retention and replay validation.

The local Docker Compose environment does not implement these production DR targets.
