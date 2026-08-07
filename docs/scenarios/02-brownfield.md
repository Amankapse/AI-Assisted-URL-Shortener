# Scenario 02: Brownfield

## Existing module
An initial URL shortener with auth, create, redirect, and history features.

## Requirement
Add link expiration, Redis caching, and analytics to the existing URL shortener.

## Impacted components
- `url`
- `redirect`
- `analytics`
- `config`

## Task decomposition
1. Extend the URL model with expiration and status fields.
2. Add validation for expiration timestamps.
3. Implement Redis cache-aside for redirect lookups.
4. Add click event recording for successful redirects.
5. Add admin analytics and platform summary APIs.
6. Add regression tests for existing create/redirect flows.

## Dependencies
- Data model migration before expiry and status enforcement.
- Redirect cache before analytics processing on redirect path.

## Risks
- Cache invalidation after a link is disabled.
- Backward compatibility with existing short codes.
- Accurate analytics while preserving redirect throughput.

## Acceptance criteria
- Links expire and cannot redirect after expiration.
- Redis speeds up redirect resolution and falls back to PostgreSQL on cache miss.
- Redirects record click events.
- Existing create and redirect clients continue to work.

## Validation
- Regression integration tests.
- Redis hit/miss and fallback tests.
- Analytics recording tests.
- Backwards-compatibility behavior documented.
