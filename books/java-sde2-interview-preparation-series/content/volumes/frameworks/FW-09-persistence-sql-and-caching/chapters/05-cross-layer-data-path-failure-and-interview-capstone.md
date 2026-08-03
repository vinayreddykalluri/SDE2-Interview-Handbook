# Capstone: One Data Path Across Cache, ORM, SQL, and Events

This chapter joins the existing relational, transaction, JPA, and cache chapters into one observable data path. It deliberately avoids repeating the dedicated MySQL, Hibernate/JPA, Spring Data, and Redis books.

> **From Vinay:** A repository method and a cache annotation make code shorter, not the data path simpler. In an SDE-2 interview, narrate every store boundary: which version you read, which lock or predicate protects the write, what committed, and how stale copies are repaired.

## 1. Read path: derived cache to authoritative row

Consider `GET /products/{id}` where product descriptions tolerate 30 seconds of staleness but checkout prices do not.

```text
request + tenant/authorization
 -> build versioned, scoped cache key
 -> Redis client queue/network/GET
    -> hit: decode schema + verify logical freshness/version
    -> miss/unusable: enter per-key single-flight
       -> acquire DB pool connection
       -> JPA/SQL query with tenant predicate + projection
       -> optimizer/index/storage produces row
       -> map authoritative row to read model
       -> cache SET with TTL+jitter and source version
 -> return DTO
```

Checkout must read the authoritative price or use an explicit price-lock contract. A catalog cache is not upgraded into financial truth because its hit ratio is high.

### Lower-level cost table

| Convenience call | Hidden path to measure |
|---|---|
| cache `get` | serializer, client queue/connection, cluster slot/node, response bytes, decode/version |
| repository `find` | possible auto-flush, SQL generation, pool acquisition, server plan/I/O, entity tracking |
| lazy getter | proxy/collection initialization and possibly another SQL statement |
| DTO projection | query/result mapping without managed mutation; still database work |
| cache `put` | encode, network, TTL/eviction policy; write outcome may be unknown on timeout |

## 2. Write path: one local commit, recoverable propagation

```text
PATCH /products/{id} If-Match: version-7
 -> transaction starts
 -> UPDATE product
      SET price=?, version=version+1
      WHERE tenant_id=? AND id=? AND version=7
 -> require affected rows = 1
 -> INSERT outbox(event_id, aggregate_id, version=8, payload)
 -> COMMIT product + outbox
 -> return version-8 ETag

outbox relay -> Kafka ProductChanged(v8)
cache invalidator -> delete/replace only if event version is not older
readers -> miss/refill version 8
```

The database transaction owns product and outbox. Redis and Kafka are separate atomic domains. A relay crash after publish produces a duplicate; consumers use event ID/version. A cache delete timeout leaves an unknown result; TTL and later invalidation/reconciliation repair it.

## 3. Flush, commit, response, and propagation are different points

```text
managed entity mutation
 -> JPA dirty checking notices change
 -> flush emits versioned SQL
 -> database accepts statements
 -> commit makes local transaction durable
 -> HTTP response may be lost
 -> outbox publication may happen later or duplicate
 -> cache invalidation may lag/fail/reorder
```

Calling repository `save`, JPA `flush`, Kafka `send`, or Redis `delete` is not one global commit. State exactly which result has been acknowledged.

## 4. Failure and consistency windows

| Window | Observable risk | Contract/repair |
|---|---|---|
| cache miss loads v7; DB commits v8; old reader fills v7 | stale value resurrected | source version in value; reject older fill or second invalidation |
| JPA flush succeeds; transaction rolls back | SQL appeared in logs but no commit | distinguish flush from durable state |
| DB commit succeeds; HTTP response lost | client sees failure, row exists | idempotency/conditional replay/read reconciliation |
| DB commit succeeds; outbox unpublished | downstream stale | durable relay backlog/age and retry |
| relay publishes; crashes before marking sent | duplicate event | consumer inbox/event ID |
| v8 invalidation arrives before delayed v7 | old event erases newer cache | version-aware invalidation |
| Redis timeout on delete | deletion may have happened | safe repeated delete plus TTL/reconciliation |
| connection acquired, then waits on lock | pool slot held while no progress | lock/query deadlines and short transaction |
| bulk JPQL updates rows | managed context/cache stale | flush, bulk, clear/evict and version policy |
| replica read after source write | old row | source/causal routing for read-your-write |

## 5. Concurrency choices across layers

### Conditional SQL before distributed locks

For stock or versioned state, prefer a database atomic predicate:

```sql
UPDATE inventory
SET available = available - :quantity,
    version = version + 1
WHERE sku = :sku
  AND available >= :quantity;
```

One affected row means success. Redis locking does not strengthen the database if its lease expires and a paused owner writes later. Use database constraints/version/fencing at the authoritative resource.

### Optimistic versus pessimistic

Optimistic versioning fits low-conflict, replayable commands. Pessimistic locks fit high contention or expensive late failure but hold pool connections/locks and can deadlock. A single conditional update can be clearer than either entity-level pattern.

### Retry boundaries

Retry the entire local transaction after a classified deadlock/serialization conflict, not one failed statement in a partially attempted unit. Keep emails, HTTP calls, and non-idempotent publishes outside. Bound attempts by the caller deadline and observe amplification.

## 6. Capacity is a connected queueing system

```text
HTTP concurrency
 -> DB pool waiters -> active DB connections -> locks/CPU/I/O
 -> cache fallback concurrency -> additional DB demand
 -> outbox backlog -> broker/downstream demand
```

A 99% hit ratio at 100,000 reads/s still sends 1,000 DB reads/s. A cold cache sends 100,000/s unless single-flight/admission/stale serving protects the source. Increasing the connection pool can move the queue into MySQL and worsen contention.

For `N` application instances each with pool maximum `P`, budget approximately `N × P` against database connection and workload capacity, including migration jobs, relays, administration, failover headroom, and nested `REQUIRES_NEW` connections.

Virtual threads and asynchronous APIs do not expand the database’s capacity. Backpressure must exist before scarce resources.

## 7. Evidence-first diagnosis

Trace one request through:

```text
cache outcome/latency/payload version
 -> single-flight wait or source fallback
 -> pool acquisition
 -> transaction duration
 -> normalized SQL + bind distribution
 -> query count/plan/rows examined/lock wait
 -> flush/commit
 -> outbox age/publication attempts
 -> invalidation lag/cache version drift
```

Do not use product IDs, SQL binds, or cache keys as unbounded metric labels. Put high-cardinality correlation in sampled traces/logs with redaction.

### Symptom matrix

| Symptom | Plausible layer | Evidence |
|---|---|---|
| endpoint slow but query 10 ms | pool/cache/network/serialization | span breakdown |
| query count grows with results | N+1 | statement budget and call site |
| p99 rises during cache recovery | fallback cliff/stampede | miss waves, DB concurrency |
| stale value after update | invalidation/fill ordering | source/cache/event versions |
| deadlocks after batch feature | inconsistent row order/wider scan | deadlock graph and plan |
| memory grows in import | persistence context retains entities | heap/entity count; flush+clear cadence |
| outbox age grows | relay/broker/downstream bottleneck | claim/send/mark stages |

## 8. Seven live interview chains with worked answers

### Interview 1 — 99% cache hit but slow p99

**Interviewer:** “Redis hit ratio is 99%; why is the endpoint slow?”

**Candidate:** “Hit ratio says neither hit latency nor payload cost. I split client queue/network, Redis server, payload bytes, decode/GC, and fallback. At high volume the 1% misses may saturate the DB, and one hot key can dominate a shard. I compare p99 by cache outcome and source fallback rather than add capacity blindly.”

### Interview 2 — stale fill race

**Interviewer:** “A deleted cache value returns old data after an update.”

**Candidate:** “A miss loaded v7 before the write; v8 committed and invalidated; then the slow reader filled v7. I carry source version and reject an older fill, or use versioned keys/second invalidation. TTL bounds but does not prevent the race. Checkout revalidates authoritative price.”

### Interview 3 — N+1 under a repository

**Interviewer:** “One repository method generates 501 queries.”

**Candidate:** “The root query returned 500 managed entities and a lazy association was traversed. I capture the use-case shape and use a DTO projection, fetch join/entity graph for a bounded aggregate, or batch/two-step load. I compare query count with row multiplication and add a statement-budget test.”

### Interview 4 — optimistic conflict API

**Interviewer:** “Two admins edit version 7.”

**Candidate:** “Both send `If-Match`/expected version. The database update includes `WHERE version=7`; one changes to 8, the other affects zero rows. I return `412 Precondition Failed` for HTTP conditional mismatch, include current representation/link as policy allows, and never silently merge fields the caller did not own.”

### Interview 5 — deadlock retry with outbox

**Interviewer:** “Order transaction deadlocks after writing outbox.”

**Candidate:** “The victim transaction rolls back both order and outbox. I retry the whole deterministic transaction with the same request/event identity, bounded backoff+jitter, and stable row order. No external publish occurs inside the attempt; the relay sees only the committed outbox row.”

### Interview 6 — pool exhaustion

**Interviewer:** “Queries are fast, but every request waits one second.”

**Candidate:** “I inspect pool acquisition separately. Long transactions, remote calls inside transactions, leaks, lock waits, nested `REQUIRES_NEW`, or total per-instance pools may exhaust capacity. I fix hold time/admission and global budget before raising pool size, which could only move overload into the database.”

### Interview 7 — zero-downtime persistence change

**Interviewer:** “Split `price` into amount and currency with mixed app versions.”

**Candidate:** “Expand with nullable/compatible new fields, deploy readers/writers that support both and define authority, backfill in bounded indexed batches, verify counts/value equivalence and cache/event schema compatibility, switch reads, tighten constraints, then remove old fields later. I monitor locks, replication, outbox lag, and rollback at each stage.”

## 9. Focused exercises and solution sketches

1. **Draw the read path.** Include cache decode failure and single-flight. **Solution:** treat corrupt entry as distinct from miss, evict/quarantine once, bound source concurrency, and record outcome metrics.
2. **Prove stale-fill repair.** Interleave reader v7 and writer v8; assert `putIfNewerOrEqual` rejects v7 after v8. The existing companion models this rule.
3. **Design offset-like outbox claims.** Use stable event ID, claim token/lease, publish, then conditional mark. Crash after publish repeats safely; stale claimant cannot mark a newer lease.
4. **Budget cold-cache load.** At 40,000 reads/s and safe DB capacity 800/s, allow at most 800 unique fallbacks/s, coalesce duplicates, degrade/shed the rest, and warm measured hot keys gradually.
5. **Test transaction truth.** Use the target database to assert duplicate constraints, version conflict, deadlock retry of the whole unit, commit-time failure, and outbox atomicity. H2 alone cannot prove MySQL locks/plans.

## 10. Capstone boundary

Use the dedicated MySQL volume for InnoDB/SQL internals, Hibernate/JPA for lifecycle/mapping, Spring Data for repository behavior, and Redis for structures/topology. This capstone tests whether you can join those layers into one correctness and observability story.

## Primary references

- Jakarta Persistence specification and Hibernate ORM User Guide.
- MySQL Reference Manual for indexes, transactions, InnoDB, and recovery.
- Spring Framework transaction/data-access references and Spring Data JPA reference.
- Redis documentation for expiration, eviction, persistence, replication, Cluster, and scripting.
