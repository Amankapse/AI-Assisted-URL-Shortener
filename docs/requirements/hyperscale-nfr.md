# Hyperscale Non-Functional Requirements

This document defines production evolution targets for the URL shortener. These are architecture targets, not measured local throughput. The current repository remains a production-grade implementation baseline; CDN, WAF, Redis Cluster, distributed URL storage, durable event streams, multi-region deployment, and OLAP analytics are not implemented locally.

## Traffic

| Target | Value |
| --- | --- |
| New URLs | 100,000,000/day |
| Average URL creations | about 1,157/sec |
| 5x peak write planning value | about 5,800/sec |
| Rounded initial write design target | 10,000 creates/sec |
| Minimum read/write ratio | 10:1 |
| Average redirects | about 11,574/sec |
| 5x peak redirect planning value | about 58,000/sec |
| Rounded initial redirect design target | 100,000 redirects/sec |

Redirect traffic must scale independently from management and write traffic. The current application separates redirect and management code paths, but hyperscale production requires independent fleets and backing stores.

## Latency

| Operation | Architecture target |
| --- | --- |
| Redis cache-hit redirect | P95 < 50 ms |
| Redirect overall | P99 < 100 ms |
| Fallback redirect through authoritative store | P95 < 200 ms |
| URL creation | P95 < 300 ms |
| Login | P95 < 500 ms |
| Analytics query | P95 < 300 ms where practical |

Local k6 scripts provide reproducible smoke scenarios only. They are not evidence that a laptop or local Docker environment meets these targets.

## Availability

- Redirect SLO target: 99.99%.
- Management/write API target: 99.9% to 99.99%, depending production tier and dependency topology.
- Analytics availability may be lower because analytics are eventually consistent and non-critical to redirect correctness.
- Liveness must not depend on PostgreSQL, Redis, CDN, stream, or analytics stores.
- Readiness must reflect ability to serve critical operations. In the current app, PostgreSQL is required and Redis is not.

99.99% availability allows about 52.6 minutes of unavailability per year for the redirect path. Error-budget burn should trigger deployment freezes, incident review, and capacity or resilience work.

## Durability And Recovery

Architecture targets:

- no acknowledged URL mapping loss under normal infrastructure failures;
- RPO <= 5 minutes;
- RTO <= 30 minutes;
- multi-AZ deployment for production;
- backup and restore validation through scheduled drills.

The local Docker Compose environment does not implement these durability targets.

## Retention

- URL mappings must remain redirectable for at least 10 years unless expired, deleted, or blocked.
- Raw analytics events should use shorter hot retention.
- Aggregated analytics should be retained longer and archived to warm/cold tiers.
- Retention must be enforced through lifecycle policy, not unbounded growth in transactional tables.

## Security

Required controls:

- strong authentication and authorization;
- IDOR prevention through server-derived ownership;
- rate limiting for velocity abuse;
- quotas for resource consumption;
- malicious URL moderation and block/unblock controls;
- phishing/malware detection through future reputation providers;
- secret management outside source control;
- SSRF controls for any future server-side fetch functionality;
- WAF/gateway controls for Internet-scale exposure.

Open redirect behavior is the product, but malicious destination controls must mitigate abuse without silently rewriting user links.
