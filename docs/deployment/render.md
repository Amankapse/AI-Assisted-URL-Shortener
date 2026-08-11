# Render Deployment

## Single Web Service

1. Create or sign in to a Render account.
2. Connect the GitHub repository.
3. Create a Render Key Value instance for Redis-compatible cache and rate-limit storage.
4. Place the Web Service and Key Value instance in the same region where possible.
5. Create a new Render Web Service from the repository.
6. Select Docker deployment so Render builds the root `Dockerfile`.
7. Configure the branch intended for live deployment.
8. Set the health check path to `/actuator/health/liveness`.
9. Add the production environment variables from [environment-variables.md](environment-variables.md).
10. Deploy and inspect startup logs.

Render provides `PORT`. The application maps this through `server.port=${PORT:8080}` and does not bind to `localhost` or `127.0.0.1`.

The current live URL is:

```text
https://ai-url-shortener-682u.onrender.com
```

The same Web Service serves Angular and Spring Boot:

```text
/                  Angular SPA
/login             Angular SPA
/register          Angular SPA
/app/**            Angular SPA
/api/v1/**         Spring Boot REST API
/r/**              public redirects
/actuator/**       operational endpoints
/swagger-ui/**     Swagger UI
/v3/api-docs/**    OpenAPI
```

Keep `APP_PUBLIC_BASE_URL` set to this origin while public redirects continue to be served by `/r/{shortCode}` on the Spring Boot application.

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

## Packaged Angular Frontend

The Dockerfile builds the Angular project, copies `frontend/dist/frontend/browser` into Spring Boot static resources inside the build stage, and packages one executable JAR. Do not commit Angular `dist/` files or generated static resources.

The frontend public config for the current same-origin deployment is:

```text
FRONTEND_API_BASE_URL=
FRONTEND_PUBLIC_SHORT_URL_BASE=https://ai-url-shortener-682u.onrender.com
FRONTEND_ENVIRONMENT=production
```

`FRONTEND_API_BASE_URL` is intentionally empty so Angular calls `/api/v1/...` on the same origin. Keep backend secrets out of frontend variables; `write-app-config.mjs` reads only explicit `FRONTEND_*` public values.

## Future Static Site Split

The Angular source remains independently buildable and can later move to a CDN or Render Static Site. That future split should use `frontend` as the root directory, `npm ci && npm run build:render` as the build command, and `dist/frontend/browser` as the publish directory. See [frontend-render.md](frontend-render.md).
