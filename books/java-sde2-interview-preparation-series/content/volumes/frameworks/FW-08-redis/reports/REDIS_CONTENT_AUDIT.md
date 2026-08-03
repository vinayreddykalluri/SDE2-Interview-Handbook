# Redis Content Audit — Backend Wave 3

## Before improvement

The volume was a 222-word roadmap with no commands, wire/runtime flow, structure selection, TTL/eviction distinction, cache race, atomic script, failover case, Java integration, runnable code, or answered interview.

## Final chapter inventory

| # | Chapter | Final evidence |
|---:|---|---|
| 00 | bytes/structures learning path | command path, source-of-truth/failure frame |
| 01 | RESP, execution, data types, key design | low-level framing, structures, Streams, big-key/slot cases |
| 02 | TTL, eviction, memory, persistence | jitter, budgeting, RDB/AOF, failover/cold-cache |
| 03 | cache consistency | cache-aside race, stampede, negative cache, hot key, serialization |
| 04 | atomicity, transactions, scripts, limits, locks | `MULTI`, `WATCH`, Lua, token bucket, token release, fencing |
| 05 | replication, Sentinel, Cluster, operations | slots/redirects, failover loss, overload, security/recovery |
| 06 | Java/Lettuce/Spring Data | client model, serializers, timeouts, testing/diagnosis |
| 07 | interviews/readiness | 7 live chains, 18 rapid answers, cumulative assessment |

The final content contains 4,946 words plus executable code and moves deliberately from bytes/commands to structures before cache and coordination abstractions.

## Critical corrections and boundaries

- Redis is not reduced to a cache or described as universally single-threaded.
- One atomic command/script is separated from an atomic multi-system workflow.
- Expiration and eviction, pipeline and transaction, Sentinel and Cluster are distinct.
- `MULTI/EXEC` is not described as SQL rollback.
- Lock acquisition uses one `SET NX PX`; release checks the owner token; fencing protects the real resource.
- TTL cleanup is not treated as an exact security scheduler.
- Replication acknowledgement is not treated as consensus or backup.
- Spring Cache annotations do not hide key, TTL, serializer, invalidation, or stampede policy.

## Remaining target-environment work

Validate command/script behavior against the chosen Redis version and client. Live topology tests are still required for eviction, persistence, replication loss, Sentinel/Cluster failover, redirects, ACL/TLS, and load. PDF/web work remains root-owned.

## Primary references

The baseline is current Redis documentation for [data types](https://redis.io/docs/latest/develop/data-types/), commands, transactions/pipelines, scripts/functions, expiration, eviction, persistence, replication, Sentinel, Cluster, Streams, and ACLs.
