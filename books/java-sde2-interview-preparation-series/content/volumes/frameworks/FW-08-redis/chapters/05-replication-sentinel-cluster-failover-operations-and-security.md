# Replication, Sentinel, Cluster, Failover, Operations, and Security

## Replication and failover

Redis replication is commonly asynchronous. Replicas can serve reads under a stated staleness policy and provide promotion candidates. A primary can acknowledge a write that has not reached the promoted replica, so failover may lose it depending on topology and settings.

Sentinel monitors non-clustered primary/replica deployments, participates in failure detection/election, and tells clients the promoted primary. Clients must support Sentinel discovery and reconnect. Sentinel does not shard the keyspace.

## Redis Cluster

Cluster maps keys to 16,384 hash slots distributed across primary nodes. Clients receive slot maps and follow `MOVED`/`ASK` redirections during steady routing/migration.

```text
key -> CRC16(hash-tag-or-full-key) mod 16384 -> slot -> node
```

Multi-key operations, scripts, and transactions generally require keys in one slot. Hash tags such as `{order-42}` force co-location. Do not put every key under `{global}`; that defeats distribution.

Cluster provides availability and partitioning with documented windows where writes can be lost or unavailable. It is not a consensus-backed linearizable database for arbitrary cross-slot invariants.

## Read scaling

Replica reads trade freshness for capacity/latency. Cache-aside reads can often tolerate them; locks, rate limits, session revocation, and read-after-write may not. Confirm whether the client routes reads to replicas and how it reacts to lag/failover.

## Failure timeline

```text
client sends INCR to primary
primary applies and replies
reply reaches client
primary fails before replica applies
stale replica promoted
counter increment disappears
```

If this counter enforces a billing or security invariant, Redis alone under that policy is the wrong source of truth. Make the durable system authoritative or choose a stronger design.

## Operations

Monitor:

- command rate/latency and slow log;
- event-loop/client queueing, blocked clients, timeouts;
- memory used, fragmentation, allocator/fork headroom;
- evictions, expirations, hit/miss and stale/fallback outcomes;
- persistence fsync/rewrite/snapshot duration and errors;
- replication offset/lag, link state, failovers;
- cluster slot distribution, redirects, migrations, hot nodes/keys;
- client connections/output buffers;
- backup age and restore-test success.

Use `SCAN`-style administrative iteration cautiously; it can return duplicates/miss transient changes during mutation and still consumes work. Never run broad destructive key operations from an unchecked pattern.

## Capacity and overload

Set `maxmemory` and eviction policy intentionally. Bound client connections, pipelines, response sizes, and command rate. When Redis slows, client retries can multiply traffic. Use deadlines, exponential backoff with jitter, circuit breakers, and source bulkheads.

Pipeline length is a memory/latency trade-off; a million queued commands are not a free batch. Cluster pipelines must group by owning node and handle partial errors/redirections.

## Backup/restore and upgrades

If Redis holds recoverable state, back up RDB/AOF artifacts according to RPO/RTO and test restore plus application reconciliation. For pure cache, test cold-start/source protection instead. Upgrade clients and servers through compatibility testing for RESP, ACL, cluster, persistence, and command changes.

## Security

- private network/TLS and authenticated ACL users;
- least command/key-prefix permissions;
- rotate credentials without reconnect storms;
- disable/restrict dangerous administration commands;
- never expose Redis directly to the public internet;
- avoid secrets/PII in keys, logs, and diagnostics;
- validate sizes/commands before scripts;
- isolate tenants/workloads when noisy-neighbor or policy needs it.

## Incident matrix

| Incident | Evidence | Correction |
|---|---|---|
| rising p99 but low CPU | slow command/large response/client queue | command and payload trace |
| one node overloaded | hot key/slot imbalance | key/slot distribution and redesign |
| failover loses counter | async replication window | source-of-truth/durability change |
| mass eviction | capacity/policy/payload growth | budget and separate critical state |
| reconnect storm | failover + eager retry | backoff/jitter and connection budgets |

## Practice

- **Interview Core:** Compare Sentinel and Cluster.
- **SDE-2 Follow-up:** Define an outage plan for cache, login rate limiter, and job stream; each needs a different fail-open/closed and recovery policy.
