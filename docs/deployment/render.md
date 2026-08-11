# Render Deployment

## Backend Web Service

1. Create or sign in to a Render account.
2. Connect the GitHub repository.
3. Create a Render Key Value instance for Redis-compatible cache and rate-limit storage.
4. Place the Web Service and Key Value instance in the same region where possible.
5. Create a new Render Web Service from the repository.
6. Select Docker deployment so Render builds the existing `Dockerfile`.
7. Configure the branch intended for live deployment.
8. Set the health check path to `/actuator/health/liveness`.
9. Add the production environment variables from [environment-variables.md](environment-variables.md).
10. Deploy and inspect startup logs.

Render provides `PORT`. The application maps this through `server.port=${PORT:8080}` and does not bind to `localhost` or `127.0.0.1`.

The current live backend URL is:

```text
https://ai-url-shortener-682u.onrender.com
```

Keep `APP_PUBLIC_BASE_URL` set to this backend origin while public redirects continue to be served by `/r/{shortCode}` on the Spring Boot application.

## Runtime Profile

Set:

```text
SPRING_PROFILES_ACTIVE=prod
```

The production profile uses Hibernate schema validation and Flyway migrations. It does not use Hibernate `create`, `create-drop`, or `update`.

## Redis / Valkey

Use the Render Key Value internal connection URL:

```text
SPRING_DATA_REDIS_URL=<RENDER_KEY_VALUE_INTERNAL_URL>
```

Redis remains an optimization for redirect cache-aside and rate limiting. Redirect correctness is backed by PostgreSQL. Redis outage behavior follows the documented cache and rate-limit failure policies.

## JVM Memory

For a 512 MB free-tier container, start conservatively:

```text
JAVA_TOOL_OPTIONS=-Xms64m -Xmx320m -XX:+UseG1GC
```

Tune only after observing memory, latency, and garbage-collection behavior.

## Frontend Static Site

Deploy the Angular app as a separate Render Static Site. Use:

```text
Root Directory: frontend
Build Command: npm ci && npm run build:render
Publish Directory: dist/frontend/browser
```

Configure a SPA rewrite from `/*` to `/index.html`. See [frontend-render.md](frontend-render.md).
