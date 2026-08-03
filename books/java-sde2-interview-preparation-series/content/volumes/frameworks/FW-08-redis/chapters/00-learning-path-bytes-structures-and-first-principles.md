# Redis for Java Backend Interviews

## Learning Path: Bytes and Data Structures Before Caching

Redis is a networked data-structure server. Keys and most values are byte sequences; commands apply atomic transformations to structures such as strings, hashes, sets, sorted sets, lists, and streams. “Fast cache” describes only one use.

> **From Vinay:** Before saying “put it in Redis,” write the key, value structure, command, TTL, source of truth, and failure response. A cache that is fast but returns forbidden or stale data is still incorrect.

## Dependency order

```text
RESP command and keyspace
  -> data-structure semantics
  -> TTL, memory, eviction, persistence
  -> cache consistency and stampedes
  -> atomic commands, transactions, scripts/functions
  -> rate limits and coordination
  -> replication, Sentinel/Cluster, failover
  -> Java/Lettuce/Spring Data boundary
```

## One running keyspace

```text
order:{42}:summary        STRING/JSON, TTL 5m
order:{42}:items          HASH, bounded fields
customer:{7}:recent       ZSET score=epochMillis member=orderId
rate:{tenant-9}:checkout  ZSET of request timestamps
stream:order-events       STREAM with consumer groups
lock:order:{42}           STRING random owner token + short TTL
```

The braces are Redis Cluster hash tags: keys sharing the same tag map to one slot. Use them only when an atomic multi-key operation truly needs co-location; a popular tag can create a hot slot.

## The command path

```text
Java object
 -> serializer -> bytes
 -> client pool/native connection/event loop
 -> RESP command over TCP/TLS
 -> Redis parses + executes command on owning node
 -> memory/persistence/replication work
 -> RESP response
 -> decoder -> Java result
```

Latency can occur in client queueing, DNS/connect/TLS, network, command execution, slow scripts, memory pressure, persistence forks/rewrite, replication, cluster redirection, or decoding. “Redis took 2 ms” must identify which interval was measured.

## Command atomicity is not workflow atomicity

`INCR`, `SET key value NX PX 5000`, and a Lua/function invocation execute atomically on the relevant Redis server context. A Java sequence `GET`, calculate, `SET` is not atomic because other clients can run between commands. A Redis command cannot roll back a MySQL commit, Kafka publish, or email.

## The SDE-2 answer frame

1. What is the source of truth?
2. Which Redis structure and exact commands fit the operation?
3. What are the TTL, invalidation, eviction, and stale-data contracts?
4. Which concurrency race exists between commands?
5. What happens on timeout, failover, duplicate retry, or partial outage?
6. How do keys distribute across nodes and tenants?
7. Which metrics and repair path prove correctness?

## First failure matrix

| Failure | Risk | Safe posture |
|---|---|---|
| cache miss | extra source load | bounded fallback and stampede control |
| command timeout | write outcome unknown | idempotent command/token/reconciliation |
| eviction before TTL | unexpected miss | treat cache as disposable unless designed otherwise |
| replica/failover lag | acknowledged data may be lost/stale under policy | choose durability/consistency per use |
| hot key | one node/slot saturates | local cache, shard key, aggregate, or redesign |

## Practice

- **Foundation:** For three use cases, name the structure and command—not just “Redis.”
- **Interview Core:** Define cache source of truth, TTL, invalidation, and outage behavior.
- **SDE-2 Follow-up:** Explain why a successful Redis lock does not make a MySQL-plus-HTTP workflow atomic.
