# Analytics Scale Evolution

## Current Implementation

```text
Redirect
-> bounded in-memory queue
-> batch writer
-> PostgreSQL
```

The current analytics path is best-effort. Redirects continue when the analytics queue is saturated; dropped-event metrics indicate degraded analytics completeness.

## Hyperscale Target

```text
Redirect
-> Kafka/Kinesis/Pulsar
-> partitioned durable event stream
-> independent consumers
```

Consumers can support:

- raw event persistence;
- aggregate counters;
- abuse and fraud detection;
- analytics warehouse loading;
- audit/event replay.

Target:

```text
99% of analytics events queryable within 60 seconds
```

This is an architecture target, not a current implementation claim.

## Delivery Semantics

Use at-least-once delivery with idempotent event handling. Consumers must tolerate duplicate events through event IDs or deterministic idempotency keys.

Required operational signals:

- producer success/failure;
- stream append latency;
- consumer lag;
- replay progress;
- dead-letter count;
- aggregate freshness;
- analytics query latency.

## Analytics Store Separation

High-volume analytical queries should move away from the redirect transaction store. Candidate stores include ClickHouse, BigQuery, Snowflake, Druid, or an equivalent OLAP system.

Raw event retention should be finite. Aggregates and archives should satisfy business reporting requirements without retaining sensitive detailed events indefinitely.
