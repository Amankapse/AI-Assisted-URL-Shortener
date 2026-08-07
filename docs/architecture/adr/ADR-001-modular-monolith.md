# ADR-001: Modular Monolith

## Status
Accepted

## Context
The assignment requires a production-quality URL shortener with multiple features, security controls, and observability. A modular monolith is easier to build, test, and review in the available timeframe.

## Decision
Use a modular monolith architecture with feature packages for `auth`, `user`, `url`, `redirect`, `analytics`, `admin`, `security`, `common`, and `config`.

## Consequences
- Pros: simpler deployment, easier integration testing, fewer operational components.
- Cons: less service isolation, but acceptable for the assignment scope.
- Future extraction is possible if traffic or team boundaries justify microservices.
