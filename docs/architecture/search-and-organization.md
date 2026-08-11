# Campaigns, Tags, Search, and Large-Workspace UX

Stage 8 adds lightweight organization metadata without changing redirect correctness.

## Data Model

- `campaigns` are workspace-scoped, soft-deletable records with an active-name uniqueness rule.
- `tags` are workspace-scoped normalized records.
- `url_tags` stores the many-to-many URL/tag relationship.
- `short_urls.campaign_id` is nullable. Deleting a campaign detaches URLs and leaves those URLs active.
- `short_urls.destination_host` is derived from the existing destination URL validation path on create and destination update. Legacy null values are valid.

## Search Policy

`GET /api/v1/urls` supports bounded filtering across:

- `q`
- `state`
- `campaignId`
- `tag`
- `createdFrom` / `createdTo`
- `expiresBefore` / `expiresAfter`
- `customAlias`
- `sort`
- `page` / `size`

`q` is exact/prefix oriented and searches only short code, custom alias, destination host, campaign name, and normalized tag name. It intentionally does not search raw URL query strings, emails, user IDs, API keys, audit events, outbox payloads, or analytics metadata.

Allowed sort fields are `createdAt`, `expiresAt`, `clickCount`, and `shortCode`. The default sort is `createdAt,desc` with `id desc` as a stable tie-breaker.

## Authorization

Campaign reads and tag reads are available to workspace members and API keys with `links:read` or `links:write`.

Campaign lifecycle management is human-only:

- create/update: `OWNER`, `ADMIN`, `EDITOR`
- delete: `OWNER`, `ADMIN`

URL campaign and tag assignment uses the existing URL write policy. API keys with `links:write` may assign URL metadata but cannot manage campaign lifecycle.

## Performance Notes

Search remains PostgreSQL-backed offset pagination. Response enrichment is batched for campaigns and tags after the URL page is selected to avoid lazy serialization and N+1 behavior.

The redirect hot path does not load campaign or tag metadata. Redis redirect cache contents remain focused on redirect target state.

At very large workspace size, future evolution should consider keyset pagination, async campaign detach jobs, search-specific read replicas, and dedicated indexing infrastructure. Those are not implemented in Stage 8.
