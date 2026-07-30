# Spring Data Redis for Cache and Rate-State

Redis is often used for caches, locks, and short-lived state. Spring Data Redis can help, but cache logic is not repository CRUD.

## Cache decisions before repositories

1. Define TTL and eviction policy.
2. Define key namespace and collision prevention.
3. Define read/write strategy (write-through, write-behind, refresh-ahead).

Treat Redis as a performance and coordination layer, not system-of-record unless explicitly designed.

## Repository integration pattern

```text
Primary DB write -> outbox/state update -> Redis cache update -> read path validation
```

## Common interview questions

- When should TTL be short vs long?
- What happens during stampede after TTL expiration?
- How do you avoid cache inconsistency after writes?

## Common mistakes

- Using repository-like methods for long-lived mutable counters.
- Caching every repository method result without measuring read/write ratio.
- Forgetting namespace versioning when schema changes.

## Practical checklist

- Key shape should include version and tenant.
- Use atomic ops for counters and rate checks.
- Provide fallback path when Redis is unavailable.

## Quick check

1. Why is cache-aside easy and dangerous?
2. What is a stampede and how do you handle it?
3. Why do key naming contracts matter across teams?

## Debugging exercise

A count endpoint using Redis returns stale values after a hot deploy.

What checks do you run?

Expected: check TTL reset rules, versioned key migration, and fallback read-through path.

## Practice

- **Foundation:** Draft Redis key naming for one endpoint.
- **Interview Core:** Explain a safe fallback strategy for temporary Redis outage.
- **SDE-2 Follow-up:** Compare lock semantics in distributed counters with optimistic locking in DB writes.
