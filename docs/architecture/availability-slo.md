# Availability And SLO

These are production targets. The local Docker/Testcontainers environment does not satisfy or prove these SLOs.

## Redirect

- Availability target: 99.99%.
- Redis-hit redirect latency target: P95 < 50 ms.
- Overall redirect latency target: P99 < 100 ms.
- Authoritative-store fallback redirect target: P95 < 200 ms.

At 99.99%, the annual error budget is about 52.6 minutes.

Critical SLIs:

- redirect success rate;
- redirect latency;
- cache hit ratio;
- Redis fallback rate;
- authoritative store errors;
- blocked/disabled/expired/not-found rates;
- hot-key skew;
- future event-stream lag.

## Management API

- Availability target: 99.9% to 99.99%.
- URL creation latency target: P95 < 300 ms.
- Login latency target: P95 < 500 ms.

Critical SLIs:

- creation success rate;
- login success/failure rate;
- quota rejection rate;
- rate-limit rejection rate;
- database latency/errors;
- migration health.

## Analytics

Analytics are eventually consistent and may have a lower availability target than redirects. Future durable-stream target:

```text
99% of analytics events queryable within 60 seconds
```

Critical SLIs:

- accepted events;
- dropped events;
- persistence failures;
- queue depth or stream lag;
- aggregate freshness;
- analytics query latency.

## Recovery Targets

- RPO target: <= 5 minutes.
- RTO target: <= 30 minutes.
- Redis is rebuildable from authoritative URL mappings.
- URL mapping data requires PITR, replication, backups, and restore drills.
