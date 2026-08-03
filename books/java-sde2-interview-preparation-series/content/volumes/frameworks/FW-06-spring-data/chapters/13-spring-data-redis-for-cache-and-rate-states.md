# Spring Data Redis for Cache and Rate-State

Redis is often used for caches, locks, and short-lived state. Spring Data Redis can help, but cache logic is not repository CRUD.

## Cache decisions before repositories

1. Define TTL and eviction policy.
2. Define key namespace and collision prevention.
3. Define read/write strategy (write-through, write-behind, refresh-ahead).

Treat Redis as a separate data system with explicit persistence, replication, failover, and memory contracts. In this book it is a derived cache/coordination boundary; the dedicated Redis book covers system-of-record designs.

## Repository integration pattern

```text
Primary DB transaction commits source state + version/outbox
        -> cache invalidation/update is retried idempotently
        -> read miss loads source and fills only if not older
```

There is no universal “always delete before or after the database write” recipe. Draw the race. A versioned key/value or source-version check can prevent an older fill from overwriting newer data. TTL limits staleness duration; it does not make stale data impossible.

Before `RedisTemplate` or Spring Cache, state the native atomic operation. For a fixed-window rate counter, `INCR` plus first-write expiry needs an atomic script/transactional design so a crash does not leave a counter without TTL. For cache-aside, state exact key, serialization version, source version, TTL jitter, timeout, and fallback.

## Common interview questions

- When should TTL be short vs long?
- What happens during stampede after TTL expiration?
- How do you avoid cache inconsistency after writes?

## Common mistakes

- Using repository-like methods for long-lived mutable counters.
- Caching every repository method result without measuring read/write ratio.
- Forgetting namespace versioning when schema changes.
- Treating `@Cacheable` as a stampede, consistency, or distributed-lock solution.
- Retrying Redis indefinitely while the source database is already overloaded.

## Failure matrix

| Failure | Required behavior |
|---|---|
| Redis timeout | bounded fallback or explicit failure by product contract |
| mass expiry | jitter, request coalescing, warmup, and source admission |
| stale fill after invalidation | source version/fencing or delayed-delete protocol |
| eviction before TTL | treat miss as normal; never infer source deletion |
| failover loses acknowledged cache write | source remains authoritative; repair/refill |
| distributed lock holder pauses | fencing token at protected resource, not lease alone |

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

## Interviewer question and model answer

**Interviewer:** Can I add `@Cacheable` to every repository read and invalidate on writes?

**Model answer:** No. I first identify expensive stable reads, the source of truth, acceptable staleness, key and serialization version, TTL/eviction, tenant scope, and outage behavior. Annotation-based caching does not solve concurrent misses, stale fills, cross-instance invalidation, or hot keys. I test the write/read race, use version-aware invalidation where needed, bound Redis and source concurrency, and keep correctness independent of the cache unless the product explicitly chooses otherwise.
