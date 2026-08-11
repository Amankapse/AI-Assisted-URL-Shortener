# Production Validation

Run these checks after Render reports a successful deployment.

## Operational Checks

```text
GET https://<SERVICE>.onrender.com/actuator/health/liveness
GET https://<SERVICE>.onrender.com/actuator/health/readiness
GET https://<SERVICE>.onrender.com/v3/api-docs
GET https://<SERVICE>.onrender.com/swagger-ui/index.html
```

Expected behavior:

- liveness is `UP` when the JVM/application is alive;
- readiness is `UP` only when PostgreSQL is reachable;
- Redis/Valkey is not part of readiness because the application falls back to PostgreSQL for redirect correctness;
- sensitive Actuator endpoints such as environment, beans, heap dumps, and mappings are not publicly exposed.

## Functional Smoke Flow

1. Register a user with `POST /api/v1/auth/register`.
2. Log in with `POST /api/v1/auth/login`.
3. Create a URL with `POST /api/v1/urls` using the returned bearer token.
4. Follow `GET /r/{shortCode}` and confirm the redirect succeeds.
5. Query owner analytics for the created URL.
6. If an admin account exists, validate admin analytics and block/unblock moderation endpoints.

Do not create an admin user through a public API; the application has no public admin provisioning flow.

## Startup Evidence

Check Render logs for:

- active `prod` profile;
- successful Neon PostgreSQL connection;
- Flyway validating and applying migrations `V1` through `V10`;
- Hibernate schema validation success;
- successful Redis/Valkey connection or documented Redis degradation metrics if unavailable.

## Frontend Checks

For the Render Static Site:

- direct navigation to `/app/urls` returns Angular through the SPA rewrite;
- direct navigation to `/app/audit`, `/app/api-keys`, and `/app/admin/outbox` returns Angular rather than a CDN 404;
- `app-config.json` contains only public values: `apiBaseUrl`, `publicShortUrlBase`, and `environment`;
- `apiBaseUrl` points to the public backend origin, not an internal Render URL;
- static security/cache headers match [frontend-render.md](frontend-render.md).

## Cookie And CSRF Checks

Validate in a browser:

- login sets the backend refresh cookie;
- hard reload attempts refresh and restores authenticated state;
- logout sends `X-XSRF-TOKEN` and succeeds;
- invalid/missing CSRF for refresh/logout is rejected;
- CORS response allows the exact frontend origin and credentials.

If `SameSite=Strict` prevents cross-subdomain refresh cookies from being sent between the frontend static site and backend service, stop and review the domain strategy. Do not switch to `SameSite=None` without explicit security approval.
