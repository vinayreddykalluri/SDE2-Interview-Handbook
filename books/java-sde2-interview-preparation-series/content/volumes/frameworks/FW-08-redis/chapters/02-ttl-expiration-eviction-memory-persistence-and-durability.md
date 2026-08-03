# TTL, Expiration, Eviction, Memory, Persistence, and Durability

## Expiration is a contract on a key

```text
SET session:abc payload EX 1800
EXPIRE session:abc 1800
TTL session:abc
```

Setting a value can remove/preserve TTL depending on command/options/version. Test the exact update path. A key may be removed lazily on access or actively by expiration cycles, so physical deletion is not an exact scheduled event.

Authorization must compare logical expiry in the value/request, not wait for background deletion.

## TTL jitter

If one million cache keys receive the same 10-minute TTL, they can expire together and overload the source. Add bounded random/deterministic jitter:

```text
effective TTL = base TTL + uniformly distributed 0..jitter
```

The jitter window should reflect freshness tolerance and traffic. For tests, derive it deterministically from the key to avoid flaky timing.

## Expiration versus eviction

- **Expiration:** key TTL elapsed.
- **Eviction:** Redis removes keys under configured memory pressure according to a policy.

`noeviction` rejects writes when memory limits are reached; LRU/LFU/random/TTL-oriented policies choose candidates from all or expiring keys depending on configuration. Algorithms may be approximated. A cache must tolerate early eviction. A correctness-critical lock/rate counter sharing an evicting cache can disappear early and weaken guarantees.

Separate workloads or reserve policy/capacity when semantics differ.

## Memory budgeting

Budget more than serialized payload:

```text
key bytes + value bytes + object/allocator overhead
+ expiry metadata + data-structure/index nodes
+ replication/AOF buffers + fragmentation + fork/COW headroom
```

Many tiny keys can use more memory in overhead than payload. One huge key can block commands and concentrate network. Measure `MEMORY USAGE`, allocator fragmentation, key distribution, and client output buffers on representative data.

## RDB and AOF

- **RDB snapshots:** compact point-in-time files; recovery can lose writes since the last snapshot.
- **AOF:** logs write commands and can fsync under configurable policy; rewrite compacts history.
- **Both:** can combine recovery properties and operational costs.

Persistence consumes CPU, disk, memory, and latency headroom. Fork/copy-on-write and AOF rewrite can amplify memory/disk pressure. Configuration determines durability; “Redis is in memory, so data is not durable” and “AOF means no loss” are both oversimplifications.

Replication is asynchronous in common deployments. Acknowledged writes can be lost during failover depending on acknowledgement/durability/topology. `WAIT` can ask for replica acknowledgement but does not turn Redis into a strongly consistent consensus system or guarantee fsync everywhere.

## Restart and cold-cache behavior

A cache restart can create a thundering herd while the source is cold too. Prepare:

- request coalescing/single-flight;
- admission control and bounded concurrency;
- warming only valuable keys;
- stale-while-revalidate where allowed;
- source capacity for planned failure;
- precomputed snapshots only if freshness/security allow.

## Durability decision table

| Use case | Persistence expectation | Failure posture |
|---|---|---|
| disposable product cache | none required | source fallback |
| rate limiter | loss may temporarily loosen limit | decide fail-open/closed and isolate |
| job/stream state | recovery required | AOF/replication plus idempotent source/replay |
| distributed lock | TTL essential, value not durable history | fencing at protected resource |
| session | product/security decision | source/refresh strategy and logical expiry |

## Practice and solutions

- **Foundation:** Distinguish TTL expiry from memory eviction.
- **Interview Core:** Add jitter and single-flight to a synchronized-expiry cache.
- **SDE-2 Follow-up:** Choose RDB/AOF/replication for a stream consumer state. Start from RPO/RTO and replay/idempotency; verify actual fsync/failover with tests.
