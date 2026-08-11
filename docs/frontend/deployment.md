# Frontend Deployment

The Angular app remains independently buildable from `frontend/`, but the current Render live/demo deployment packages its production output into the Spring Boot Docker image.

## Current Single-Service Mode

Render builds the root `Dockerfile`:

1. Node 24 builds Angular with `npm run build:render`.
2. The Docker build copies `frontend/dist/frontend/browser` into Spring Boot static resources.
3. Maven packages one executable Spring Boot JAR.
4. The runtime image contains only the JRE and `app.jar`.

Current public runtime config:

```text
FRONTEND_API_BASE_URL=
FRONTEND_PUBLIC_SHORT_URL_BASE=https://ai-url-shortener-682u.onrender.com
FRONTEND_ENVIRONMENT=production
```

An empty `FRONTEND_API_BASE_URL` makes Angular call `/api/v1/...` on the same origin. This keeps login, refresh, logout, CSRF, and `SameSite=Strict` refresh cookies in a same-origin browser flow.

## Future Static Hosting Split

The app can later move to Render Static Site or CDN hosting without TypeScript rewrites:

- Root directory: `frontend`
- Build command: `npm ci && npm run build:render`
- Publish directory: `dist/frontend/browser`
- SPA fallback/rewrite: `/*` to `/index.html`

For a split deployment, set `FRONTEND_API_BASE_URL` to the public API origin and configure `APP_AUTH_ALLOWED_ORIGINS` with the exact frontend origin. Do not use wildcard origins with credentialed refresh/logout flows.

See [../deployment/frontend-render.md](../deployment/frontend-render.md) for optional split-host rewrite, header, and SameSite validation details.
