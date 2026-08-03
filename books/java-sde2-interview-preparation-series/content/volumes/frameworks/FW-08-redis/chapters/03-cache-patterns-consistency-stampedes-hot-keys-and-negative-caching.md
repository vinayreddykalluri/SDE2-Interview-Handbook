# Cache Patterns, Consistency, Stampedes, Hot Keys, and Negative Caching

## Cache-aside

```text
read:
  GET cache key
    hit -> decode + validate logical freshness
    miss -> read source -> SET value with TTL -> return

write:
  commit source change -> invalidate/update cache
```

The dangerous interval is between source commit and cache invalidation. If invalidation fails, stale data remains until TTL. A short TTL limits but does not eliminate inconsistency.

For database-first writes, delete cache after commit. Updating cache before database commit can publish rolled-back state. Updating after commit can still fail; use an outbox/change stream and idempotent invalidation when the freshness contract demands recovery.

## Write-through and write-behind

- **Write-through:** application writes through a cache layer to source; centralizes policy but adds coupling/latency.
- **Write-behind:** cache acknowledges before asynchronous source persistence; improves latency but creates durability, ordering, replay, and conflict risk.
- **Refresh-ahead:** refresh before expiry for predictable hot keys; wastes work for cold keys.
- **Stale-while-revalidate:** serve bounded stale value while one caller refreshes.

Name who owns truth and what happens when either side is unavailable.

## Stampede control

A naive miss causes every caller to query the database. Techniques:

1. per-key single-flight in one process;
2. distributed short lease for cross-instance refresh;
3. serve stale value while refresh runs;
4. TTL jitter;
5. bounded source concurrency/admission control;
6. prewarm known hot keys.

A refresh lease must expire, use an owner token, and not block serving safe stale data longer than necessary. If refresh fails, back off to avoid a failure storm.

## Negative caching

Caching “not found” prevents repeated misses but can hide a newly created resource. Use a shorter TTL, version/namespace strategy, and invalidate on creation. Never negative-cache an authorization denial across principals unless the key includes every security dimension.

## Cache key completeness

If response varies by tenant, locale, currency, permissions, experiment, or API version, the key must reflect it or the cached payload must be independently safe.

```text
bad:  profile:42
good: profile:v3:{tenant-9}:42:locale=en-US:permission-set=hash
```

Permission hashes add invalidation complexity; often do not cache security-filtered responses globally.

## Hot keys

A key can saturate a node, network link, or client connection even if total cluster traffic looks moderate. Diagnose per-key/slot and payload size. Options:

- local near-cache for immutable/bounded-stale data;
- replicate/read from replicas only with an acceptable consistency contract;
- split aggregatable counters;
- remove giant payload/projection;
- add request coalescing;
- redesign access rather than random key suffixes that break consistency.

## Serialization and versioning

JSON is debuggable but verbose; binary codecs are compact but require schema discipline. Defend against Java native deserialization risks. Include a schema version when necessary, tolerate compatible old fields, bound payload sizes, and distinguish “missing” from “decode failure.” A decode failure should not trigger unlimited source retries.

## Cache failure matrix

| Event | Risk | Policy choice |
|---|---|---|
| Redis unavailable | source overload | circuit/bulkhead, fail-open/closed per data |
| stale authorization | data exposure | do not serve beyond security freshness |
| invalidation lost | old value until TTL | outbox/change event and versioned key |
| deserialize failure | retry storm | evict/quarantine + metric + bounded source read |
| hot-key expiry | synchronized miss | jitter + single-flight + stale window |
| old writer repopulates | stale-after-delete race | version token/key namespace or delete-after-write strategy |

## Interview exercise

**Scenario:** Read misses, thread A loads version 1. Source updates to version 2 and invalidates. A then writes stale version 1 into cache.

**Solutions:** Store/compare a version in the value, use versioned keys plus current-version pointer, prevent stale fill using a lease/token, or perform a second invalidation after the write. Choose based on complexity and staleness budget.

## Practice

- **Foundation:** Draw cache-aside read and write timelines.
- **Interview Core:** Protect a hot product key during expiry and Redis outage.
- **SDE-2 Follow-up:** Cache permission-dependent data without cross-tenant/user leakage.
