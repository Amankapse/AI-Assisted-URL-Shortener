# Scenario 01: Greenfield

## Original requirement
Build the initial URL-shortening capability.

## Normalized requirement
Implement a secure URL shortener with user registration, login, link creation, redirect support, optional custom aliases, and paginated history.

## Ambiguities / assumptions
- Users require authentication for link creation.
- Redirects are public.
- Links have an enabled/disabled lifecycle.
- Analytics may be added later as part of the brownfield scenario.

## Impacted components
- `auth`
- `user`
- `url`
- `redirect`
- `security`

## Task decomposition
1. Define API contract for auth and URL creation.
2. Define data model for users and links.
3. Build user registration and login.
4. Build URL create and redirect endpoints.
5. Add ownership validation and paginated history.
6. Add tests for happy path and authorization.

## Dependencies
- `auth` and `security` before protected URL endpoints.
- `user` model before link ownership.
- `redirect` path before cache optimization.

## Risks
- Exposing ownership incorrectly.
- Creating insecure URL validation.
- Using the wrong auth model for CSRF.

## Acceptance criteria
- Users can register and login.
- Users can create short URLs.
- Short URL redirects work.
- Users can view their own links.
- Public redirect endpoint does not require auth.

## Validation
- Unit and integration tests for auth and URL flow.
- Security tests for access control.
- API contract documented.
