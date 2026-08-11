# Frontend Deployment

The Angular app is designed for static hosting such as Render Static Site or any CDN-backed static host.

## Render Static Site

- Root directory: `frontend`
- Build command: `npm ci && npm run build:render`
- Publish directory: `dist/frontend/browser`
- SPA fallback/rewrite: `/*` to `/index.html`
- Runtime config: `build:render` writes `app-config.json` from public frontend environment variables.

Public frontend variables:

```text
FRONTEND_API_BASE_URL
FRONTEND_PUBLIC_SHORT_URL_BASE
FRONTEND_ENVIRONMENT
```

The backend must allow the exact frontend origin through `APP_AUTH_ALLOWED_ORIGINS`. Do not use wildcard origins with credentialed refresh/logout flows.

For production cookie compatibility, prefer same-site custom domains such as `app.example.com` and `api.example.com`.

See [../deployment/frontend-render.md](../deployment/frontend-render.md) for Render-specific rewrite, header, and SameSite validation details.
