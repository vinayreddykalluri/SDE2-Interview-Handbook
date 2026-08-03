# Redis Live Interviews, Rapid Q&A, Practice, and Sources

## Live interview 1: cache-aside race

**Interviewer:** “A stale value reappears after invalidation.”

**Candidate:** “A reader missed, loaded v1, a writer committed v2 and deleted the cache, then the reader filled v1. I would use a versioned value/key, refresh lease plus version check, or second invalidation. TTL only bounds the error; it does not prevent it.”

## Live interview 2: stampede

**Interviewer:** “Ten thousand requests miss one key.”

**Candidate:** “Coalesce refresh per key, serve bounded stale data if safe, jitter TTLs, cap source concurrency, and back off failed refreshes. A distributed lease coordinates instances but uses owner tokens/expiry and must not make the cache a permanent dependency.”

## Live interview 3: rate limiter

**Interviewer:** “Design API rate limiting.”

**Candidate:** “Define scope, limit/window, burst, accuracy, clock, and outage policy. Fixed window is cheap but boundary-bursty; sliding log is exact but memory-heavy; token bucket gives controlled bursts. One script/function makes trim/refill/check/update atomic, sets cleanup TTL, and returns remaining/reset metadata.”

## Live interview 4: distributed lock

**Interviewer:** “Is `SET NX PX` enough?”

**Candidate:** “It provides a lease claim. Release must compare owner token. A paused owner can resume after expiry, so the protected database needs a monotonically increasing fencing token or its own version/conditional write. I keep lease work bounded and treat timeout outcome carefully.”

## Live interview 5: Redis Cluster cross-slot error

**Interviewer:** “Our script fails only in production cluster.”

**Candidate:** “Its keys map to different slots. Use a deliberate shared hash tag when atomic co-location is required, or redesign into independent operations with reconciliation. I would ensure the tag does not concentrate all tenants into one slot.”

## Live interview 6: failover loses data

**Interviewer:** “An acknowledged counter decreased after promotion.”

**Candidate:** “Common Redis replication is asynchronous; the promoted replica may not have applied the write. I would inspect acknowledgement/persistence/topology and decide whether Redis is allowed to be authoritative. For billing/security truth, store an idempotent durable ledger and use Redis as acceleration.”

## Live interview 7: slow Redis

**Interviewer:** “CPU is low but p99 is high.”

**Candidate:** “Break down client queue/connection, network, command execution, response size, and decoding. Inspect slow commands, big/hot keys, blocked clients, persistence fork/rewrite, output buffers, cluster redirects, and retry amplification. Low CPU does not rule out event-loop stalls or network/large payloads.”

## Rapid answered questions

1. **Is Redis just a cache?** No; it is a data-structure server with caching, coordination, streams, and more.
2. **Are all workflows atomic?** One command/script block can be atomic on its node; multi-system workflows are not.
3. **TTL versus eviction?** Expiration follows time; eviction follows memory policy and may occur earlier.
4. **Is expiry exact?** Logical expiry is exact by time check; physical deletion can be delayed.
5. **Pipeline versus transaction?** Pipeline batches network traffic; transaction prevents command interleaving during `EXEC` but has no SQL-style rollback.
6. **What does `WATCH` do?** Aborts `EXEC` if watched state changed, enabling optimistic retry.
7. **Why scripts must be short?** They block other command execution on that node while running.
8. **`SETNX` plus `EXPIRE`?** Race leaves permanent lock; use atomic `SET NX PX`.
9. **Why random lock token?** Only the owner should release its still-current lease.
10. **Why fencing?** It lets the protected resource reject a stale paused owner.
11. **Sentinel versus Cluster?** Sentinel discovers/fails over one unsharded primary group; Cluster partitions slots and fails over shard primaries.
12. **Can replica reads be stale?** Yes; use only when the data contract permits it.
13. **Is replication backup?** No; it copies mistakes and lacks historical recovery by itself.
14. **Why hot key?** One key maps to one owning node/slot and can dominate compute/network.
15. **Do Streams give exactly once?** No; crash between effect and ack produces redelivery; make effects idempotent.
16. **Should cache failure fail open?** It depends on security/correctness. Decide per use case, not globally.
17. **Why no `KEYS` in hot path?** It scans the keyspace and can delay the node.
18. **Why serializer versioning?** Cached bytes can outlive deployments and be read by mixed versions.

## Cumulative assessment

Design Redis support for product cache, checkout rate limit, leaderboards, and a cross-instance refresh lease. Include keys/structures/commands, TTL+jitter, source truth, atomic scripts, cluster slots, failover outcomes, memory estimate, security ACLs, dashboards, and restore/cold-start tests.

**Strong solution:** isolates disposable cache from correctness-sensitive state, prevents stampedes, uses idempotency/fencing, handles unknown outcomes, distributes keys, and defines per-use-case outage behavior.

## Authoritative references

- Redis documentation: [data types](https://redis.io/docs/latest/develop/data-types/), commands, pipelining, transactions, scripting/functions, expiration, eviction, persistence, replication, Sentinel, Cluster, Streams, ACLs, and observability.
- Lettuce reference: connection, async/reactive, cluster, Sentinel, topology refresh, codecs, and timeouts.
- Spring Data Redis reference: templates, serializers, cache, transactions, scripting, streams, repositories, and observability.

The included lab proves deterministic Java logic and RESP framing, not live-server eviction, replication, failover, persistence, or Cluster behavior.
