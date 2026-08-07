# Cost Model

No cloud pricing is assumed here. This document defines cost metrics a production team should track for a hyperscale URL shortener.

## Unit Economics

Track:

- cost per million URL creations;
- cost per million redirects;
- storage cost per billion mappings;
- cache cost per billion requests;
- analytics ingestion cost per billion events;
- analytics query cost by dashboard/report type;
- backup and restore storage cost;
- cross-region replication cost.

## Cost Drivers

Cache sizing affects origin/database load and latency. Larger caches cost more but reduce fallback reads and improve viral-link handling.

Retention affects storage, backups, and migration duration. Ten-year URL mapping retention is required, but raw analytics can use shorter hot retention plus rollups.

Edge caching reduces origin traffic and improves latency, but it can shift analytics collection from origin callbacks to edge logs/events.

Database choice affects write throughput, operational complexity, and consistency guarantees. Relational storage is appropriate for auth/account data; distributed KV storage is better suited for hyperscale short-code lookups.

Analytics rollups reduce long-term query cost and storage pressure.
