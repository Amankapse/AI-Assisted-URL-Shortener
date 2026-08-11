# URL Shortener Frontend

Angular 22 production frontend for the URL shortener API.

## Commands

```powershell
npm ci
npm start
npm run build
npm test -- --watch=false
```

## Runtime Configuration

The built artifact reads `public/app-config.json` at startup:

```json
{
  "apiBaseUrl": "http://localhost:8080",
  "publicShortUrlBase": "http://localhost:8080",
  "environment": "local"
}
```

Replace this JSON per environment. Do not put secrets in it.

## Implemented Frontend Scope

Implemented:

- runtime configuration
- application shell
- handwritten typed auth/workspace API clients
- RFC7807 Problem Details mapping
- memory-only access token state
- refresh-token cookie flow
- single-flight refresh
- CSRF header propagation for refresh/logout
- workspace header propagation
- auth/guest/admin guards
- login/register screens
- workspace-aware dashboard foundation
- production link dashboard at `/app/urls`
- link creation at `/app/urls/new`
- link details and metadata editing at `/app/urls/:id`
- campaign management at `/app/campaigns`
- query-param synchronized link filters, pagination and allowlisted sorting
- ETag/`If-Match` guarded destination, campaign and tag mutations
- create-link idempotency key retention across retries
- workspace-aware campaign and tag option loading
- URL analytics at `/app/urls/:id/analytics`
- workspace audit at `/app/audit`
- URL audit history in `/app/urls/:id`
- API-key lifecycle management at `/app/api-keys`
- one-time raw API-key display with explicit acknowledgment
- workspace and membership management at `/app/workspace`
- platform ADMIN area under `/app/admin`
- platform overview, moderation, audit, and read-only outbox visibility

Not implemented yet:

- QR codes, custom domains, tracing UI, advanced charts, and Stage 9D deployment hardening
- end-to-end browser tests
