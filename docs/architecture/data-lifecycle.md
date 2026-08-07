# Data Lifecycle

## URL Mapping

URL mappings must remain redirectable for at least 10 years unless:

- expired by `expires_at`;
- disabled by the owner;
- blocked by administrative moderation;
- soft-deleted by the owner.

The current implementation stores expiration, enabled, blocked, and deleted independently. `EXPIRED` is derived from `expires_at` rather than persisted as a separate state.

## Analytics

Use lifecycle tiers:

```text
HOT
recent detailed events

WARM
aggregated data

COLD
archived historical analytics
```

Raw analytics events should not be retained forever without a concrete legal/product justification. Aggregates can be retained longer with less sensitive metadata.

## Canonicalization And Deduplication

Do not globally deduplicate destination URLs by default.

Reasons:

- users may create multiple campaign links for the same destination;
- aliases may differ;
- analytics may need independent tracking;
- equivalent-looking URLs may not be semantically equivalent.

Normalize only for validation and security. Do not silently map equivalent-looking URLs to the same shortcode.
