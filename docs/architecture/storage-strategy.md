# Storage Strategy

## Current Implementation

```text
Spring Boot modular monolith
-> PostgreSQL
```

PostgreSQL is currently authoritative for users, refresh-token digests, URL mappings, and analytics. Redis is an optimization for redirect cache-aside and rate limiting.

This is appropriate for the validated baseline, but a single relational URL mapping table is not an honest design for 365 billion mappings and 100K redirects/sec.

## Hyperscale Evolution

Keep PostgreSQL for relational/account data:

- users;
- authentication metadata;
- refresh tokens;
- roles;
- quota configuration;
- administrative metadata;
- audit records that need relational joins.

Move URL mappings toward a distributed key-value store:

```text
shortCode -> redirect metadata
```

Candidate systems include DynamoDB, Cassandra, ScyllaDB, Bigtable, or an equivalent distributed KV system.

The URL mapping workload differs from account/auth data because it is:

- extremely high-cardinality;
- lookup-heavy by short code;
- globally read-heavy;
- tolerant of denormalized redirect metadata;
- less dependent on relational joins in the redirect path.

## Future Store Interface

A future implementation can introduce:

```text
UrlMappingStore
  -> PostgresUrlMappingStore
  -> DynamoUrlMappingStore
  -> CassandraUrlMappingStore
```

This phase does not implement a new datastore. PostgreSQL remains the current source of truth.

## Quotas At Scale

The current quota implementation uses PostgreSQL aggregate counts. That is acceptable for the baseline but not final for hyperscale. Production should move quota accounting to materialized/distributed counters with durable reconciliation against the authoritative mapping/event store.
