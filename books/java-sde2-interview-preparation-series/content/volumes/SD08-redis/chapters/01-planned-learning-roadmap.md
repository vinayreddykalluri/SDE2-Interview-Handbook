# Redis for Caching, Coordination, and Streaming - Planned Learning Roadmap

> **Publication status:** roadmap edition. Later revisions will add command traces, Java examples, race-condition labs, and production sizing exercises.

Redis is an in-memory data platform with multiple data structures and persistence options. This book will teach it as a separate consistency and failure boundary, not as a universal speed switch.

## Planned sequence

1. Keys, strings, hashes, sets, sorted sets, lists, streams, and memory trade-offs.
2. Expiration, eviction, TTL contracts, serialization, and key design.
3. Cache-aside, read-through, write-through, invalidation, and stale-data policy.
4. Stampede prevention, hot keys, request coalescing, and bounded fallback behavior.
5. Atomic commands, transactions, Lua scripts, optimistic coordination, and limitations.
6. Pub/Sub versus Streams, consumer groups, pending entries, and replay.
7. Replication, Sentinel, Cluster, persistence, failover, and data-loss windows.
8. Java clients, connection pools, timeouts, Spring Cache, Spring Data Redis, and testing.

## Interview focus

The completed edition will ask readers to design keys and TTLs, explain invalidation ownership, quantify memory, prevent cache stampedes, and distinguish a distributed lock claim from a proven coordination contract.

## Completion gate

A reader is ready to include Redis in a design when they can state the source of truth, stale-data tolerance, failure behavior, memory and eviction policy, key distribution, and evidence that Redis improves the target bottleneck without weakening correctness silently.
