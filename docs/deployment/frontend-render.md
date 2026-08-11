# Future Render Static Site Frontend

The current live/demo deployment packages Angular into the Spring Boot Docker image and serves both from one Render Web Service. This document preserves the optional future split path for CDN/static hosting without changing frontend or backend business logic.

## Render Settings

Use these values from the actual Angular workspace output:

| Setting | Value |
| --- | --- |
| Service type | Static Site |
| Root Directory | `frontend` |
| Build Command | `npm ci && npm run build:render` |
| Publish Directory | `dist/frontend/browser` |

The Angular build output contains `dist/frontend/browser/index.html`; this is the directory Render must publish.

## Public Runtime Config

The checked-in `frontend/public/app-config.json` intentionally contains local values only. For Render, configure these public Static Site environment variables:

| Variable | Secret | Example |
| --- | --- | --- |
| `FRONTEND_API_BASE_URL` | No | `https://api.example.com` |
| `FRONTEND_PUBLIC_SHORT_URL_BASE` | No | `https://ai-url-shortener-682u.onrender.com` |
| `FRONTEND_ENVIRONMENT` | No | `production` |

`npm run build:render` writes these values to `dist/frontend/browser/app-config.json` after the Angular bundle is built. Do not put secrets in frontend environment variables; all values are browser-visible.

## SPA Rewrite

Render Static Site must rewrite Angular client routes to `index.html`.

Configure Redirects/Rewrites:

| Source | Destination | Action |
| --- | --- | --- |
| `/*` | `/index.html` | Rewrite |

Render serves existing files before applying rewrites, so hashed JavaScript/CSS assets continue to load normally. This is required for direct browser refreshes such as `/app/urls`, `/app/audit`, `/app/api-keys`, and `/app/admin/outbox`.

## Static Security Headers

Configure headers on the Static Site after browser validation:

| Path | Header | Value |
| --- | --- | --- |
| `/*` | `X-Content-Type-Options` | `nosniff` |
| `/*` | `X-Frame-Options` | `DENY` |
| `/*` | `Referrer-Policy` | `no-referrer` |
| `/*` | `Content-Security-Policy` | `default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self' https://ai-url-shortener-682u.onrender.com; img-src 'self' data:; font-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'` |
| `/chunk-*` | `Cache-Control` | `public, max-age=31536000, immutable` |
| `/main-*` | `Cache-Control` | `public, max-age=31536000, immutable` |
| `/styles-*` | `Cache-Control` | `public, max-age=31536000, immutable` |
| `/app-config.json` | `Cache-Control` | `no-store` |
| `/index.html` | `Cache-Control` | `no-cache` |

Do not add `unsafe-eval`. Validate the CSP in the browser console before making it enforcing for a public release.

## CORS Dependency For Split Deployment

The backend must include the exact frontend origin:

```text
APP_AUTH_ALLOWED_ORIGINS=https://<frontend-static-site>.onrender.com
```

Local examples:

```text
APP_AUTH_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:3000,http://localhost:8080
```

Production must not use `*` with credentialed refresh/logout flows.

## SameSite Cookie Risk For Split Deployment

The current backend refresh cookie uses `Secure`, `HttpOnly`, and `SameSite=Strict`. A frontend at `https://<frontend>.onrender.com` calling an API at `https://ai-url-shortener-682u.onrender.com` is likely cross-site for cookie purposes because the subdomains differ.

Before weakening the cookie policy, validate in a real browser:

1. Login succeeds and the backend sends the refresh cookie.
2. Hard reload triggers Angular startup refresh.
3. Browser sends the refresh cookie to the backend.
4. Logout sends cookie and `X-XSRF-TOKEN`.

If Strict cookies are not sent, stop and review. Preferred long-term deployment is same-site custom domains such as `app.example.com` and `api.example.com`. Do not silently switch to `SameSite=None`.
