# Normalized Requirements

## Objective

Build a production-grade URL shortening service that supports authenticated users, role-based access control, secure URL creation, redirection, link lifecycle management, click analytics, and operational readiness.

## Functional requirements

1. User authentication and authorization
   - Register, login, refresh tokens
   - Roles: `USER`, `ADMIN`
   - `USER` can manage their own links and analytics
   - `ADMIN` can view platform analytics and moderate links

2. URL management
   - Create a short URL for a destination URL
   - Optional custom alias
   - Redirect a short URL to its destination
   - Enable or disable links
   - Expire links after a configured expiration timestamp
   - Delete or deactivate links
   - Paginated user link history and detail retrieval

3. Analytics
   - Record click events for redirects
   - Provide per-link analytics
   - Provide platform-level summary information for admins

4. Platform health
   - Health and readiness endpoints
   - Observability metrics for redirect cache, request latency, and authentication failures

## Non-functional requirements

1. Data integrity
   - PostgreSQL is the source of truth
   - Redis is a cache, not the authoritative store
   - Short codes are unique and durable

2. Security
   - Strong password hashing
   - JWT authentication with refresh token rotation
   - Ownership validation for user resources
   - URL validation with allowed schemes and reserved alias protection
   - Rate limiting for auth and URL creation
   - CSRF decision documented for bearer-token-based API

3. Reliability and scalability
   - Stateless app instances
   - Cache-aside redirect lookups with Redis fallback to PostgreSQL
   - Graceful behavior when Redis is unavailable
   - Pagination for list APIs
   - Indexes aligned with query patterns

4. Quality gates
   - Unit tests, integration tests, security tests
   - CI workflow with compilation, tests, static analysis, vulnerability scan, and container build
   - Traceability of AI changes
