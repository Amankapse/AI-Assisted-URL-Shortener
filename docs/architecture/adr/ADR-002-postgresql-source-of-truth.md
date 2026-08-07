# ADR-002: PostgreSQL as Source of Truth

## Status
Accepted

## Context
The URL shortener requires durable storage for users, links, and analytics, with SQL constraints and history queries.

## Decision
Use PostgreSQL as the authoritative data store for all user, URL, redirect, and analytics data. Redis is used only for cache-aside redirect lookup optimization.

## Consequences
- Data correctness and ownership checks rely on PostgreSQL.
- Redis failures do not invalidate application correctness.
- PostgreSQL schema changes must be managed through Flyway.
