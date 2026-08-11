# Transactional Outbox

Stage 7 adds a PostgreSQL-backed transactional outbox for durable event delivery without adding a broker or a new production dependency.

## Purpose

The outbox records events in the same database transaction as the business mutation. A bounded dispatcher later claims rows with PostgreSQL `FOR UPDATE SKIP LOCKED`, invokes a typed handler, and marks the row `PROCESSED`, retries it, or moves it to `DEAD`.

Audit remains separate. Audit records are accountability evidence. Outbox records are delivery work items.

## Schema

`V9__outbox_events.sql` creates `outbox_events` with the approved delivery fields: identifiers, workspace scope, aggregate metadata, event type/version, bounded JSONB payload, status, retry counters, claim timestamps, processing timestamp, and stable error code.

Statuses are `PENDING`, `PROCESSING`, `PROCESSED`, and `DEAD`. Indexes support dispatch ordering, stale claim recovery, and created-at inspection/cleanup.

## Published Events

URL mutations publish `URL_CREATED`, `URL_DESTINATION_CHANGED`, `URL_EXPIRATION_CHANGED`, `URL_ENABLED`, `URL_DISABLED`, `URL_DELETED`, `URL_BLOCKED`, and `URL_UNBLOCKED`.

Cache invalidation publishes `URL_CACHE_INVALIDATION_REQUIRED`.

Optional analytics outbox mode publishes `CLICK_RECORDED`. `APP_ANALYTICS_PUBLISHER` defaults to `local` in all profiles. `outbox` mode is available for validation and operational comparison, but it is not the hyperscale click-stream target.

## Dispatcher

The dispatcher is enabled by `APP_OUTBOX_ENABLED` and bounded by `APP_OUTBOX_BATCH_SIZE`, `APP_OUTBOX_WORKERS`, `APP_OUTBOX_POLL_INTERVAL`, `APP_OUTBOX_CLAIM_TIMEOUT`, `APP_OUTBOX_MAX_ATTEMPTS`, `APP_OUTBOX_BASE_BACKOFF`, and `APP_OUTBOX_MAX_BACKOFF`.

Delivery is at-least-once. Handlers must be idempotent. `claimed_by` is an ephemeral application instance identifier used for diagnostics only; correctness is provided by PostgreSQL row locking and claim timeout recovery.

Retries use exponential backoff with jitter. Permanent failures and exhausted transient failures move to `DEAD` with a stable `last_error_code`.

## Handlers

- Cache invalidation handler evicts Redis using the same redirect cache namespace. Redis failures are transient and retried.
- Analytics handler writes `CLICK_RECORDED` events through the existing analytics writer. It reuses the click event ID and relies on `ON CONFLICT DO NOTHING`, so retries do not double-increment aggregate click counts.
- URL mutation handler is intentionally a no-op placeholder for durable downstream delivery. It preserves the control-plane event stream without adding external infrastructure in this stage.

## Admin Inspection

`GET /api/v1/admin/outbox` is read-only and requires platform `ROLE_ADMIN`. It supports filters for `status`, `eventType`, `from`, `to`, `page`, and `size`. Responses expose safe summary fields only and never return raw payload JSON.

API keys cannot access this endpoint.

## Health And Metrics

Outbox does not affect liveness or readiness. PostgreSQL readiness already covers the authoritative store.

Metrics are low-cardinality and include created, claimed, processed, failed, retried, dead, backlog, oldest pending age, dispatch latency, and handler latency. Tags are limited to controlled values such as event type, handler, and outcome.

## Limitations

- No external broker is implemented.
- Delivery is at-least-once, not exactly-once.
- URL mutation events are durably recorded but only no-op handled until a downstream integration is added.
- Optional analytics outbox mode writes one database row per redirect and is not the long-term high-volume click-stream architecture.
- Processed row cleanup deletes only bounded batches of processed records older than retention.
