# RESP, Command Execution, Data Types, and Key Design

## RESP framing

Redis clients encode commands using the Redis Serialization Protocol. A conceptual RESP2 request:

```text
*3\r\n
$3\r\nSET\r\n
$7\r\norder:1\r\n
$4\r\nPAID\r\n
```

The length prefixes make binary-safe values possible. Production clients pipeline, multiplex, reconnect, follow cluster redirects, authenticate, negotiate protocol version, and decode push/aggregate types. Do not hand-roll RESP in an application; the lab implements a small encoder only to expose the wire boundary.

## Execution model without slogans

Redis traditionally executes commands serially on a server execution path, which makes one command atomic relative to other commands. Modern versions can use additional threads for I/O/background work, and clusters execute on multiple nodes. Therefore “Redis is single-threaded” is incomplete and “all operations are globally serialized” is false.

A long Lua script, large key deletion, expensive sorted-set range, or huge response can delay unrelated commands on that node. Complexity and payload size still matter.

## Strings

Strings hold bytes and support `GET`, `SET`, `MGET`, `INCRBY`, bit operations, and conditional/TTL options.

```text
SET order:42:status PAID NX EX 300
INCRBY inventory:book-1 -1
```

`INCRBY` is atomic but does not enforce “never below zero” alone. Use a script/function that checks and decrements atomically, or model a reservation command.

## Hashes

Hashes map fields to values under one key:

```text
HSET order:42 status PAID totalCents 5000 version 4
HINCRBY order:42 version 1
```

They suit bounded objects with partial field updates. TTL applies to the key unless using version-specific field-expiration features; label the target version before relying on newer commands.

## Lists, sets, and sorted sets

- **List:** ordered sequence, push/pop and blocking operations. Good for simple queues, but reliability/replay need explicit design.
- **Set:** unique unordered members, membership and set algebra.
- **Sorted set:** unique members ordered by floating-point score; ranks, leaderboards, delayed timestamps, sliding windows.

Sorted-set scores are IEEE-754 doubles. Very large integer timestamps/counters can lose exact integer precision; keep the safe range or encode tie-breakers in members.

## Streams

Streams are append-only entries with IDs, range reads, and consumer groups. Consumer groups track pending entries and acknowledgements, but processing is not automatically exactly once. Consumers can crash after the side effect and before `XACK`; the message will be delivered/claimed again. Make the effect idempotent and observe pending/idle entries.

Kafka offers a partitioned durable log with different scaling/retention/consumer semantics; Redis Streams are not a drop-in synonym.

## Other structures

Bitmaps/bitfields, HyperLogLog, geospatial indexes, probabilistic structures, JSON, time-series, and vector structures fit specialized questions. State accuracy, module/version, memory, and query requirements. Do not force them into basic caching because they sound advanced.

## Key naming and tenancy

Use stable, bounded names with namespace, tenant, entity, and version when useful:

```text
prod:checkout:v2:{tenant-9}:rate
```

Never put secrets/PII in keys; keys appear in diagnostics and consume memory. Estimate total key/value overhead and TTL population. Avoid unbounded `KEYS pattern` in production; use indexed ownership, `SCAN` with caveats, or administrative tooling.

## Structure edge cases

| Trap | Effect | Repair |
|---|---|---|
| giant hash/list | node stalls/large transfer | bound and partition |
| blocking queue as durable workflow | loss/replay gaps | stream/broker with explicit delivery contract |
| floating score as arbitrary integer | precision/tie surprise | safe numeric range + deterministic member |
| multi-key command across slots | cluster error | hash tag or redesign |
| `KEYS` in hot path | scans keyspace/blocking risk | explicit index/`SCAN` for admin only |

## Practice

- **Foundation:** Choose structures for unique tags, leaderboard, recent orders, and object fields.
- **Interview Core:** Explain stream pending entries and duplicate processing.
- **SDE-2 Follow-up:** Redesign a 50 MB hot hash with per-field updates and uneven tenant load.
