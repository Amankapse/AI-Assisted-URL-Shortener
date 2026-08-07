# Capacity Model

This model describes architecture targets for hyperscale production planning. It is not a claim about the local implementation.

## Traffic Calculation

```text
100,000,000 creates/day
/ 86,400 seconds/day
= about 1,157 writes/sec average
```

At 5x peak:

```text
1,157 * 5 = about 5,800 writes/sec
```

The rounded initial write design target is:

```text
10,000 creates/sec
```

For a minimum 10:1 read/write ratio:

```text
100,000,000 redirects/day equivalent write ratio * 10
= about 11,574 redirects/sec average

11,574 * 5
= about 58,000 redirects/sec at 5x peak
```

The rounded initial redirect design target is:

```text
100,000 redirects/sec
```

## Ten-Year Retention

```text
100,000,000 URLs/day * 365 days/year * 10 years
= 365,000,000,000 URL records
```

That is 365 billion URL mapping records before accounting for analytics.

## Storage Assumptions

Do not plan storage using only destination URL length. A logical URL record includes:

- UUID or internal identifier;
- short code;
- destination URL;
- owner/account reference;
- creation and expiration timestamps;
- enabled/deleted/blocked state;
- click counters or aggregate references;
- versioning and metadata;
- row headers and alignment;
- indexes.

Use 500 bytes as a minimum planning estimate per logical mapping and up to about 1 KB depending metadata and index overhead.

Approximate logical mapping storage:

| Estimate | 365B records |
| --- | --- |
| 500 bytes/record | about 182.5 TB before replicas/indexes/backups |
| 1 KB/record | about 365 TB before replicas/indexes/backups |

Physical storage will be higher because of:

- primary and secondary indexes;
- replication;
- database/page overhead;
- backups and point-in-time recovery logs;
- warm/cold archives;
- analytics raw events and aggregates.

## Headroom

Critical resources should normally operate below roughly 60-70% utilization. This leaves headroom for spikes, node loss, deployments, rebalancing, and incident mitigation.

Monitor CPU, heap, database connections, Redis memory, Redis latency, queue or stream lag, disk throughput, storage growth, collision retries, and cache hit ratio.
