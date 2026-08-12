# Site Experience And CMS

Stage 10 adds a bounded platform site experience layer.

## Scope

- Public landing page at `/`.
- Public CMS-backed pages for About, Features, Security, Help, Contact, Privacy, Terms, Accessibility, and Disclaimer.
- Public read-only APIs under `/api/v1/site/**`.
- Platform-admin CMS APIs under `/api/v1/admin/site/**`.
- Angular admin routes:
  - `/app/admin/experience`
  - `/app/admin/content`
  - `/app/admin/announcements`
  - `/app/admin/media`

## Security Rules

- CMS administration requires platform `ROLE_ADMIN`.
- Workspace `OWNER` or workspace `ADMIN` does not grant platform CMS access.
- API keys do not grant CMS access.
- Public content reads expose only published/safe fields.
- Administrator content is plain text, not raw HTML.
- The frontend does not use `[innerHTML]` or `DomSanitizer.bypassSecurityTrustHtml(...)` for CMS content.
- Media assets are externally hosted HTTPS metadata only; binary uploads are not supported.
- Stage 10 rejects SVG media URLs.

## Operational Notes

`/r/{shortCode}` remains isolated from CMS tables and APIs. Redirect correctness continues to use PostgreSQL URL state plus Redis cache-aside; CMS failures must not block login, the authenticated app shell, or redirects.

Platform administrator MFA is not currently implemented. Production deployments should add privileged-account MFA through a reviewed authentication enhancement before commercial use.
