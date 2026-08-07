# Performance Plan

## Architecture targets

These are design targets, not guaranteed SLAs:

- Redis cache-hit redirect P95 < 50 ms
- PostgreSQL fallback redirect P95 < 200 ms
- URL creation P95 < 300 ms
- Login P95 < 500 ms
- User analytics P95 < 300 ms
- Admin analytics P95 < 750 ms for local smoke data volumes

## k6 scripts

Scripts:

- `performance/k6/redirect-cache-hit.js`
- `performance/k6/redirect-cache-miss.js`
- `performance/k6/url-create.js`
- `performance/k6/login.js`
- `performance/k6/user-analytics.js`
- `performance/k6/admin-analytics.js`

Each script is parameterized with `BASE_URL`, `VUS`, and `DURATION`. Authenticated scripts require `ACCESS_TOKEN`, `ADMIN_ACCESS_TOKEN`, or `URL_ID` as appropriate.

## Scenarios

- Cache-hit redirect: warm key first and repeat redirects.
- Cache-miss redirect: use unique/missing short codes or clear cache.
- Redis outage: stop Redis and confirm valid redirects fall back to PostgreSQL.
- Analytics pressure: generate redirects faster than analytics persistence and observe queue depth/drops.
- Database load: monitor Hikari and PostgreSQL while exercising creation and analytics queries.
