# Redis Content Changelog — Backend Wave 3

| Area | Change |
|---|---|
| sequence | replaced roadmap with eight chapters ordered from protocol/structures to production |
| low-level flow | added Java serialization → RESP → node execution → persistence/replication → decode path |
| structures | added strings, hashes, lists, sets, sorted sets, Streams, specialized choices and edge cases |
| memory/durability | added TTL jitter, eviction policy, memory overhead, RDB/AOF, failover/cold start |
| caching | added stale-fill race, invalidation, stampede, negative cache, hot keys, key security dimensions |
| concurrency | added atomic commands, `WATCH`, `MULTI`, scripts/functions, limiters, owner tokens and fencing |
| topology | added replication, Sentinel, slots/hash tags, redirects, multi-key boundaries and incidents |
| Java/Spring | added Lettuce connection model, Spring Data progression, serializers and unknown outcomes |
| interviews | added 7 live chains, 18 rapid answers, practice solutions and cumulative assessment |

## Executable additions

- `code/RedisInterviewCompanion.java`: structure choice, deterministic TTL jitter, logical freshness, cluster hash tags, leases, UTF-8 byte length.
- `labs/maven-demo`: RESP2 encoder plus deterministic TTL/NX/release/sliding-window/fencing mechanics with seven tests.
- `labs/validate_redis_labs.sh`: strict Java 21 compile/smoke plus Maven tests.
