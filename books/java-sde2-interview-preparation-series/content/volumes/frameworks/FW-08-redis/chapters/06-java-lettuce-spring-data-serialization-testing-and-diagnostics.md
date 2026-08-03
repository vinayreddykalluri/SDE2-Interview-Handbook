# Java, Lettuce, Spring Data Redis, Serialization, Testing, and Diagnostics

## Connection model

Lettuce uses Netty and supports thread-safe connection sharing for many nonblocking commands, while blocking/transactional/pub-sub usage often needs dedicated connection semantics. A pool is not automatically required for every ordinary asynchronous command; choose it for isolation/stateful operations with measured limits.

Never block an event-loop thread waiting for its own future. In reactive code, keep the entire path nonblocking and propagate cancellation/backpressure.

## Native command first

```java
String result = commands.set(
        key,
        ownerToken,
        SetArgs.Builder.nx().px(leaseMillis));
boolean acquired = "OK".equals(result);
```

For production lock release, execute a compare-and-delete script. Store tokens as opaque random bytes/strings and use a fencing mechanism at the protected resource when correctness requires it.

## Spring Data Redis progression

1. Native client command establishes exact Redis semantics.
2. `RedisTemplate`/typed operations make serializers and operations explicit.
3. Spring Cache annotations fit simple cache contracts after key, TTL, invalidation, null, and stampede policy are known.

```java
ValueOperations<String, OrderSummary> values = template.opsForValue();
OrderSummary cached = values.get(key);
if (cached != null) {
    return cached;
}
```

That code still lacks single-flight, logical freshness, null/decode distinction, source deadline, TTL jitter, and invalidation. An annotation cannot decide those product semantics.

## Serialization

Configure key and value serializers explicitly. Avoid Java native serialization for untrusted or long-lived cache data. JSON needs stable type/version handling; generic polymorphic type metadata can create security and compatibility risks. Test:

- old/new fields and schema version;
- null versus missing key;
- corrupted/truncated payload;
- numeric/time precision;
- maximum size and compression limits;
- cross-service interoperability.

## Pipelining and transactions in clients

Pipelining reduces round trips; replies still correspond to commands in order and each can fail. `MULTI/EXEC` has connection state, so keep the same dedicated connection and inspect results. In Cluster, split/group pipelines by slot/node or let a cluster-aware client do so under documented behavior.

## Timeouts and unknown outcomes

Separate connect, command, pool, and total request deadlines. If a mutating command times out after send, the server may have applied it. Use idempotency tokens/conditional commands and do not retry non-idempotent scripts blindly.

## Testing layers

- deterministic Java tests for TTL, jitter, limiter, fencing, and RESP encoding;
- client command-shape tests;
- integration against exact Redis release/topology;
- failover/cluster movement/persistence restore tests;
- load tests for hot keys, scripts, payloads, and reconnect storms.

Embedded substitutes rarely reproduce eviction, fork/AOF pressure, replication loss, Cluster redirects, or timing. The included lab intentionally validates logic without claiming server behavior.

## Diagnostic sequence

```text
request -> cache decision (hit/miss/stale/bypass)
        -> connection/queue wait
        -> command + key namespace hash + payload bytes
        -> node/slot + server duration
        -> decode/fallback/source duration
```

Track hits that were unusable due to decode/version/security separately from normal misses.

## Edge cases

| Mistake | Effect |
|---|---|
| default serializer changes | old data unreadable |
| new client per command | connection/TLS churn |
| huge pipeline | memory and timeout spike |
| generic retry on timeout | duplicate increment/script |
| cache annotation on self-invocation | proxy advice may not run |
| blocking call on reactive loop | stalled throughput |

## Practice and solutions

- **Foundation:** Define serializers and a key version for an order summary.
- **Interview Core:** Instrument hit, stale, decode failure, fallback, and source overload separately.
- **SDE-2 Follow-up:** Test a failover during a non-idempotent increment; show why the durable source or idempotent operation ledger must decide the final value.
