# ADR-005: Click Analytics

## Status
Accepted

## Context
Redirects must remain fast and correct even when analytics storage is slow or temporarily unavailable. Analytics must not store raw IP addresses, full user agents, full referrers, cookies, headers, tokens, or owner data.

## Decision
Use a best-effort async analytics publisher with a bounded in-memory queue. Valid redirects enqueue sanitized click events. A background worker persists batches to PostgreSQL and updates aggregate click counts with atomic SQL:

```sql
UPDATE short_urls SET click_count = click_count + :delta WHERE id = :urlId
```

Click event IDs are immutable UUIDs and inserts use idempotent conflict handling. IP addresses are HMAC-SHA-256 hashed with the configured pepper, referrers are reduced to host, user agents are normalized to category, and correlation IDs are sanitized.

Owner analytics are exposed through owner-scoped URL endpoints. Admin analytics are exposed only through explicit `ROLE_ADMIN` endpoints and do not bypass normal URL ownership APIs.

## Consequences
- Redirect success does not depend on analytics persistence.
- Queue overload drops analytics events rather than delaying redirects.
- PostgreSQL remains the source of truth for analytics.
- Soft deletion preserves historical analytics while removing links from normal owner and redirect queries.
- Retention jobs, broader metrics, and external queue durability remain future work.
