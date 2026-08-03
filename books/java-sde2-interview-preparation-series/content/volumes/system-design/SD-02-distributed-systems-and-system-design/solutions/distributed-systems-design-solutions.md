# Reasoned Solutions: Distributed Systems Design Drills

The numbers and technologies may vary. The invariant and failure explanation are what make an answer defensible.

## 1. Unit-bearing capacity

```text
average RPS = 120,000,000 / 86,400 ~= 1,389 requests/s
peak RPS    = 1,389 * 12          ~= 16,667 requests/s
DB RPS      = 16,667 * 0.25       ~= 4,167 calls/s
concurrency = 4,167 * 0.030       ~= 125 in-flight calls
```

Little's Law gives a starting concurrency estimate, not a pool setting. Missing factors include latency distribution rather than mean, transaction duration, retries, connection multiplexing, cache miss bursts, query mix, replica routing, headroom, maintenance/failure capacity, request correlation, and daily/seasonal skew.

## 2. Idempotent write

Key records by `(tenant, operation, idempotencyKey)` and store normalized-payload fingerprint, state, stable domain/provider identities, response/error code, timestamps, lease/owner for recovery, and possibly expiry under a documented retention contract.

Useful states are `STARTED`, `PROVIDER_UNKNOWN`, `SUCCEEDED`, and `DETERMINISTICALLY_REJECTED`. Same key/fingerprint in `STARTED` returns in-progress or bounded wait; a different fingerprint conflicts. Terminal local results replay. `PROVIDER_UNKNOWN` triggers provider lookup/reconciliation using the same provider idempotency key—never a blind new charge. A local timeout is not a deterministic rejection.

## 3. Cache contract

Two valid choices:

1. Return version 12 from the write; the next read carries `minimumVersion=12`. A regional cache/replica that is older routes to the primary or waits within a bound. This gives explicit read-your-writes at extra routing/latency cost and may reduce availability during primary failure.
2. Pin that user's session to the write region or populate a versioned cache after commit for a short window. This is fast but requires routing/session state and careful failover. Cache population must occur only after commit.

An eventual-consistency contract is also possible if the product accepts it, but then the UI should expose pending state rather than silently showing the old address. TTL alone cannot guarantee read-after-write.

## 4. Stream completion

Assume the next initial commit offset is 20; a commit offset denotes the next record to read.

| completed | next safe commit | remembered completed gaps |
|---:|---:|---|
| 22 | 20 | {22} |
| 20 | 21 | {22} |
| 24 | 21 | {22,24} |
| 21 | 23 | {24} |
| 23 | 25 | {} |

If the consumer commits 23 after only offset 22 finished, a crash resumes after 22 and permanently skips unfinished 20 and 21. Serial per-partition processing avoids this bookkeeping at a throughput cost.

## 5. Poison record

Validate the envelope/schema before business work. Classify known permanent input errors separately from transient dependency errors and unknown bugs. After a small bounded attempt policy, write a quarantine record containing original topic/partition/offset, event ID, schema version, producer identity, failure category, safe diagnostic summary, first/last failure times, and a protected reference to the original payload. Do not copy credentials, tokens, or unnecessary personal data into broadly accessible logs/DLQs.

Alert on oldest quarantine age and rate. Assign an owning team and runbook. Replay through an audited tool that preserves event identity, supports dry-run/selection, and cannot bypass normal authorization/validation. A DLQ without ownership is delayed data loss.

## 6. Hot key

A mitigation ladder is:

1. jitter TTLs and use single-flight per instance/key;
2. allow bounded stale-while-revalidate for safe catalog fields;
3. add origin concurrency limits and negative-cache valid misses;
4. replicate the hot value in local/edge caches and pre-warm before launch;
5. isolate hot-key traffic and enforce tenant/client fairness;
6. redesign the value into versioned immutable objects if update invalidation dominates.

The cache controls reduce repeated refresh, the origin bulkhead prevents collapse, and fair admission preserves unrelated traffic. More database shards alone do not split reads for one key.

## 7. Retry budget

After reserving 100 ms, 700 ms remains. One policy could allow attempt 1 up to 200 ms, then full-jitter backoff capped at 40 ms; attempt 2 up to 200 ms, then jitter capped at 80 ms; attempt 3 only if the measured remaining budget can hold its timeout plus response reserve. Every attempt derives its deadline from the original monotonic deadline.

Three 200 ms timeouts plus scheduling, serialization, network overhead, and fixed sleeps can exceed 800 ms. On explicit overload, honor `Retry-After` when it fits or fail quickly; a global retry budget caps amplification. Do not retry a non-idempotent mutation without stable operation identity.

## 8. Shard expansion

Publish routing map version `v2` while clients still understand `v1`. Snapshot/copy each moving range with checkpoints, then apply change capture from a known log position. During transition, dual-read the new shard with old-source fallback or use a carefully bounded dual-write plus reconciliation. Validate row counts, key-range checksums, versions, and sampled semantic reads. Shift traffic gradually, observe mismatches, retain a rollback route, then stop old writes and retire data only after retention criteria.

Failure modes include missing changes between snapshot and capture, stale routers writing the old shard after cutover, dual-write succeeding on only one shard, tombstones/deletes not copied, and duplicate rows with divergent versions. Routing epochs and repair tooling are part of the design.

## 9. Region failover

The isolated old primary may still accept writes, so DNS or load-balancer routing alone cannot prevent split brain. Promotion obtains a monotonically increasing epoch/lease from a quorum authority. Every write includes that fencing epoch, and the authoritative storage rejects epochs lower than the latest accepted one. The old region cannot renew and its writes fail once the newer epoch is installed.

If storage does not enforce the token, the old process can ignore its expired lease and corrupt state. Fencing must be checked at the resource being protected, not only by the coordinator. Choose whether to sacrifice writes during ambiguous partitions or accept conflicts under an explicit merge rule.

## 10. Full design defense

Use `POST /jobs` with tenant-scoped idempotency key and return `jobId`; `GET /jobs/{id}` exposes state/version; `POST /jobs/{id}:cancel` records cancellation intent. In one transaction, persist `Job(QUEUED)` and an outbox event before acknowledging, so accepted jobs cannot disappear in a broker dual-write gap.

Partition queue traffic by a stable job key, but schedule through per-tenant queues or weighted fair queues so one tenant receives at most 20% of active leases. A dispatcher grants a versioned lease `(jobId, attempt, leaseUntil, fencingToken)`; workers heartbeat. After expiry, another worker may retry. The durable result write checks the current fencing token, preventing an expired worker from overwriting the new attempt.

At-least-once delivery means duplicate execution is possible. Make the job effect idempotent, checkpoint safe stages, or explicitly document tasks whose external side effects need provider idempotency/reconciliation. Cancellation races with completion: record intent, signal the current lease, and define a terminal-state conditional transition; do not promise that already-completed external work can be undone.

Use bounded admission per tenant/global capacity, queued-work age, and estimated start deadline. Shed low-priority work before memory is exhausted. Recovery scans expired leases and unpublished outbox rows. Useful SLIs include accepted-job durability, queue-to-start latency by priority, completion success, lease-expiry/retry rate, cancellation latency, oldest queued age, and fairness saturation. Avoid tenant ID as an unbounded metric label; use sampled logs/traces for per-tenant investigation.
