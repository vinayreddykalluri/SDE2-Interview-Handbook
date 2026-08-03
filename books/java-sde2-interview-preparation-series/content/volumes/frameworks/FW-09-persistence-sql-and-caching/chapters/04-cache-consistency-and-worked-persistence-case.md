# Redis and Cache Consistency: Patterns, Failures, and a Worked Case

## Learning objectives

After this chapter, you should be able to:

- decide whether a cache is justified by measured latency/load and a clear source of truth;
- specify cache-aside reads and invalidation with explicit stale-data windows;
- choose TTLs from freshness, recovery, privacy, and load requirements;
- mitigate stampedes, hot keys, penetration, eviction, and failover without treating Redis as magic shared memory;
- distinguish cache consistency from database transaction consistency;
- design a cache key/version/serialization contract; and
- explain a complete read/write path with query plan, ORM fetch plan, cache failure policy, metrics, and recovery.

## 1. A cache is a replicated derived view

### Intuition and formal model

Let database state be authoritative `D(t)`. A cache entry `C_k(t)` is a derived copy for key `k`, created at time `t_w`, and valid under a freshness policy. Cache correctness is not “value exists”; it is:

```text
key identity + serialization/schema version + authorization scope
+ source version/freshness + invalidation/expiry behavior
```

Every cache design must answer:

- What is the source of truth?
- Is the cached value an entity, projection, computation, negative result, or token?
- How stale may it be, and for which operations?
- Who writes, invalidates, expires, or rebuilds it?
- What happens when Redis is slow, unavailable, partitioned, or evicts the key?
- Can a stale value violate authorization, money, inventory, or another invariant?
- How are key and value schemas versioned?
- How is sensitive data minimized and erased?

If stale data can cause an invalid state transition, re-check the invariant in the authoritative database transaction. A cache can speed discovery but is rarely the final concurrent guard.

### Recognition and decision rules

Cache when repeated reads or computations dominate, values are reusable across requests, and a defined staleness window is acceptable. Do not cache automatically when:

- hit rate will be low due to unique keys;
- data changes as often as it is read;
- correctness requires the latest value and authoritative lookup is already fast;
- payloads are huge or per-user and create memory/cardinality pressure;
- the team lacks invalidation, observability, and incident ownership;
- the database query is slow only because an obvious index/fetch problem remains.

Measure baseline query plans, p50/p95/p99, request rate, database load, and expected working set before adding a second stateful system.

## 2. Cache-aside

### Read contract

```text
read(k):
  v = cache.get(versioned(k))
  if valid v exists: return v
  v = database.read(k)
  cache.set(versioned(k), serialize(v), ttl+jitter)
  return v
```

The application owns population. Cache failure normally falls back to the database only if that fallback is capacity-safe and within the deadline. A mass cache outage can overwhelm the source; use concurrency limits, degraded behavior, and controlled recovery.

### Write/invalidation order

A common cache-aside mutation is:

1. commit database change;
2. invalidate the cache key after commit.

Why not invalidate before commit? A concurrent reader can miss, load old committed database state, repopulate it, then the writer commits—leaving stale cache. Why not write cache first? Database failure leaves uncommitted future state exposed.

Even DB-then-delete has a race:

```text
R misses cache and reads old DB value
W commits new DB value
W deletes cache
R writes old value into cache after deletion
```

TTL bounds the stale duration but does not eliminate it. Stronger options include:

- value carries a monotonic source version and writes use compare/version policy;
- short TTL plus explicit acceptable stale window;
- transaction outbox/change stream invalidates after commit, with duplicates tolerated;
- write-through or coordinated service ownership with carefully specified failure semantics;
- never cache correctness-critical transition reads; query authoritative state;
- namespace/key version bump for coarse invalidation.

There is no general atomic transaction across an ordinary relational database and Redis. Lua scripts can be atomic *inside Redis* under documented server semantics, not across systems.

### Concrete Java sketch

```java
public ProductView get(ProductId id) {
    String key = "product:v3:" + id.value();
    try {
        ProductView cached = codec.decode(cache.get(key));
        if (cached != null) {
            return cached;
        }
    } catch (CacheUnavailable failure) {
        metrics.cacheFailure("read");
    }

    ProductView loaded = repository.requireProjection(id);
    try {
        cache.set(key, codec.encode(loaded), ttlWithJitter(id));
    } catch (CacheUnavailable failure) {
        metrics.cacheFailure("fill");
    }
    return loaded;
}
```

This dependency-requiring pseudocode omits a stampede guard and timeout budget. Cache calls need tight deadlines. Serialization failures should not be treated as a normal miss forever; they indicate schema/corruption issues and need bounded eviction/alerting.

## 3. TTL, eviction, and serialization

### TTL decision model

TTL is a correctness/recovery/load control, not merely a memory setting. Consider:

- maximum acceptable staleness;
- update frequency and invalidation reliability;
- source capacity during refill;
- working-set size and eviction policy;
- incident recovery after a bad value or missed invalidation;
- privacy/retention obligations;
- synchronized expiry and stampede risk.

Add bounded jitter so many related entries do not expire simultaneously:

```text
effectiveTTL = baseTTL * random(0.9, 1.1)
```

Jitter does not fix a hot single key. It distributes population expiry across many keys.

### Key and value schema

Good key design contains a namespace and schema version, stable canonical identity, and tenant boundary if values differ by tenant:

```text
catalog-product:v3:tenant-17:sku-BOOK-21
```

Avoid secrets, raw personal data, enormous user-controlled fragments, and ambiguous concatenation. Hashing hides length/content but does not remove authorization or deletion requirements.

Use a versioned serialization schema with:

- explicit field meanings, units, nullability, and enum evolution;
- size limit and compression policy;
- backward/forward compatibility window during deploys;
- safe deserialization into DTOs, not arbitrary executable object graphs;
- metrics for decode failures and old schema hits.

An application deploy where old and new versions share Redis is a mixed-version protocol. Prefix/version keys or support both encodings during the transition.

### Eviction is not expiration

Expiration removes a key after a time policy. Eviction removes keys under memory policy/pressure. Neither is exact at every instant under every deployment mode. The application must tolerate a miss at any time. If losing a key breaks correctness, that key is durable state, not a cache.

## 4. Stampedes, hot keys, and penetration

### Stampede

If a popular key expires, thousands of requests may miss and query the database. Mitigations:

- **single-flight/request coalescing:** one loader per key in a process; other callers await under a deadline;
- **distributed lease/lock:** one loader across instances, with lease/fencing and failure recovery;
- **stale-while-revalidate:** serve bounded stale data while one refreshes;
- **probabilistic early refresh:** refresh before expiry with probability increasing near deadline;
- **prewarming:** populate known hot keys carefully before traffic shift;
- **source bulkhead:** cap concurrent fallback queries.

A distributed lock that expires while the loader is still working can allow a second loader. For pure cache fill, duplicate work may be acceptable; for side effects, use fencing/idempotency. Never wait on a cache-fill lock longer than the request deadline.

### Hot key

One key can saturate a Redis shard/network/CPU even with a high hit rate. Options include local near-cache with a very short TTL, replicated/read-scaled cache architecture, sharded derived representation when semantics allow, response/CDN caching, or eliminating per-request fetch through configuration snapshotting. Replicating the key raises invalidation complexity and staleness.

### Cache penetration

Requests for nonexistent keys repeatedly hit the database. Negative caching stores “not found” briefly, but authorization and creation races matter. A user forbidden from seeing a record should not poison a global “not found” entry. If the object can be created soon, keep the negative TTL short or invalidate on create. Bloom filters can reject definitely absent values with false positives (never false negatives under a correctly maintained basic model), but maintaining them adds another consistency problem.

### Failure modes

| Failure | Symptom | Safe response |
|---|---|---|
| Redis timeout | latency/threads blocked | tight timeout, bulkhead, fallback or degrade under source capacity |
| full cache flush/restart | miss storm | staged warming, fallback limit, stale/local layer where acceptable |
| replica lag/failover | older cached values | version/freshness policy; authoritative recheck for writes |
| hot key | one shard saturated | local/CDN layer, replicate carefully, redesign access |
| serialization mismatch | decode failures | versioned key/codec, evict bounded bad entry, alert |
| missed invalidation | stale reads | TTL/version/event reconciliation; never rely on cache for invariant |
| oversized value | network/heap/GC latency | projections, size limit, split only with clear consistency |

## 5. Worked case: product catalog plus price update

### Requirements

- Product details are read 20,000 times/second at peak.
- Database can sustainably serve 2,000 product reads/second with headroom.
- Descriptions may be stale for 5 minutes; displayed price may be stale for at most 10 seconds.
- Checkout must never use cached price as the authoritative charge.
- Product updates occur around 50/second, heavily skewed to a small hot set.
- A rolling deploy can have two codec versions active.

### Data and query design

Authoritative tables:

```sql
product(id, tenant_id, sku, description, active, version, updated_at)
product_price(product_id, currency, amount_minor, version, updated_at)
```

Unique `(tenant_id, sku)`. Read projection joins by product ID/currency and selects only API fields. An index supports tenant/SKU lookup; primary/foreign-key access supports ID. Inspect plans under skewed data.

Separate cache entries by freshness class:

```text
product-description:v2:{tenant}:{productId} TTL about 5m + jitter
product-price:v4:{tenant}:{productId}:{currency} TTL about 10s + jitter
```

This prevents an infrequently changing description from forcing price staleness and allows price invalidation independently. Values include source version and `asOf` instant. The API documents that catalog price is indicative; checkout re-reads and validates authoritative price in its transaction/workflow.

### Write path

1. authorization checks product-management capability and tenant;
2. transaction loads/updates price with expected version;
3. database constraint/check protects amount and currency rules;
4. transaction inserts `ProductPriceChanged` outbox event with source version;
5. commit;
6. local best-effort cache delete may reduce common latency but is not the sole correctness mechanism;
7. outbox relay publishes versioned invalidation/update event;
8. cache consumer deletes or conditionally replaces only if event source version is newer;
9. TTL recovers from a lost invalidation;
10. reconciliation samples cache/source versions and alerts on excessive lag.

If events arrive out of order, an older change must not overwrite a newer cached version. Delete is often simpler and naturally safe under duplicate delivery, though the earlier cache-fill race remains bounded by TTL/version checks.

### Read path and stampede control

1. read cache with a strict sub-deadline;
2. validate codec/schema and freshness;
3. on hit, return with `asOf` metadata where product requires transparency;
4. on miss, acquire a per-key single-flight slot;
5. winner reads bounded database projection and fills TTL+jitter;
6. waiters share result only within their deadline;
7. database fallback concurrency is capped;
8. if source is overloaded, serve a bounded stale description but fail/degrade price according to product policy.

This uses a local single-flight, so each instance may still load once. If that residual load is too high, consider a distributed lease but evaluate failure complexity. Prewarm the measured top hot products before a planned cache restart.

### Consistency walkthrough

At `t0`, cached price version 7 is $10. At `t1`, admin commits version 8 at $12. Before invalidation reaches all readers, they can see version 7 for up to the policy window. Checkout ignores catalog cache, reads version 8, and quotes/charges under its own price-lock contract. If an old version-7 event arrives after version 8, conditional version logic prevents rollback. If all invalidations fail, 10-second TTL bounds the catalog display error.

The product must decide whether that is acceptable; engineering cannot call it “eventually consistent” and stop. Regulatory or contractual price display rules may require a different design.

### Observability

Track by cache name/operation/outcome, never product ID:

- hit, miss, stale-served, negative hit, decode failure;
- get/set/delete latency and timeout;
- single-flight waiter count and load duration;
- fallback database concurrency/rejection;
- estimated working set, memory, eviction, expiry;
- invalidation event lag and out-of-order count;
- source/cache version sample delta;
- hot-key evidence through sampled logs/traces, not unbounded metric labels.

High hit ratio is not sufficient. A 99% hit ratio at 20,000/s still sends 200 reads/s; one hot miss wave can exceed the source. Measure tail latency and source protection.

## 6. Interview questions and model checkpoints

### Q1. Which should happen first: database write or cache delete?

**Model checkpoint:** usually commit authoritative DB change, then invalidate after commit. Invalidate-before-write permits old-value repopulation before commit. DB-then-delete still has a read/fill race; bound it with TTL/version/event protocol.

### Q2. How do you choose TTL?

**Model checkpoint:** freshness tolerance, invalidation reliability, refill/source capacity, working set/eviction, privacy, bad-value recovery, and expiry synchronization. Add jitter; separate data with different freshness needs.

### Q3. Can Redis make a database transaction distributed?

**Model checkpoint:** an atomic Redis operation is local to Redis. It does not atomically commit with a relational transaction. Use recoverable events/outbox, idempotency, or a deliberately selected distributed protocol.

### Q4. What is a cache stampede?

**Model checkpoint:** many callers miss/expire the same reusable value and concurrently load the source. Use coalescing, early refresh, bounded stale serving, prewarm, and source bulkheads; define lock failure and deadlines.

### Q5. What makes a cache key safe?

**Model checkpoint:** unambiguous versioned namespace, correct tenant/authorization scope, canonical identity, length bounds, no secrets/PII, and migration/deletion semantics.

### SDE-2 follow-ups

1. A cache cluster fails and fallback takes the database down. Design staged recovery and admission controls.
2. A GDPR deletion removes the database row but personal data remains in cache. Add lifecycle and evidence.
3. Two application versions disagree on enum encoding. Design a mixed-version rollout.
4. Cache hit ratio is 99.8% but p99 regressed. Enumerate payload, network, hot-key, GC, and fallback hypotheses.

## 7. Exercises

1. Draw all interleavings of cache miss/fill and database write/delete; mark stale outcomes.
2. Choose TTL and invalidation for permissions, product text, price, feature flags, and one-time tokens. Explain why one policy cannot fit all.
3. Design a version-aware invalidation consumer robust to duplicates and out-of-order events.
4. Calculate fallback load at 50,000 reads/s for hit ratios from 90% to 99.99%, then add a 30-second cold-start scenario.
5. Specify a chaos test for Redis latency, partial failover, mass eviction, and codec mismatch.

## 8. Final persistence-and-cache checklist

- [ ] The source of truth and permitted staleness are named per field/use case.
- [ ] Database query and ORM fetch plans are already efficient and bounded.
- [ ] Cache keys and values are scoped, versioned, size-bounded, and privacy-safe.
- [ ] DB/cache ordering and every stale race are documented.
- [ ] TTL, jitter, invalidation, and reconciliation provide layered recovery.
- [ ] Stampede/hot-key/penetration and source overload have controls.
- [ ] Checkout/state transitions re-check authoritative invariants.
- [ ] Cache outage behavior fits the request deadline and source capacity.
- [ ] Metrics cover hit quality, latency, fallback, evictions, lag, and version drift.

## 9. Cache operating laboratory

### Pattern comparison

| Pattern | Writer/read behavior | Strength | Failure burden |
|---|---|---|---|
| cache-aside | app loads on miss, invalidates after DB commit | simple, demand-populated | stale fill race, stampede, cold start |
| read-through | cache abstraction loads from source | central loader policy | cache becomes critical coordinator; loader failures |
| write-through | write passes through cache to source | cache updated on write path | source/cache atomicity and latency still need definition |
| write-behind | cache acknowledges before asynchronous source write | low write latency | cache is now durable workflow/state; loss/order/replay risk |
| refresh-ahead | refresh before expiry | reduces popular-key misses | refreshes unused data; failure/backpressure |
| near-cache | process-local copy in front of shared cache | hot-read latency and shared-cache relief | per-instance staleness/invalidation and memory |

Names do not define guarantees. Write the commit/ack sequence and crash state. A “write-behind cache” that can lose acknowledged writes is not a cache optimization; it changes durability semantics.

### Cold-start capacity drill

Peak read traffic is 30,000/s, normal hit ratio 98%, and DB headroom is 1,000 reads/s. Normal misses are 600/s. A cache flush causes up to 30,000 fallback reads/s—30x headroom.

Recovery plan:

1. cap fallback concurrency below safe DB budget;
2. shed/degrade noncritical reads or serve bounded stale near-cache;
3. coalesce hot-key loads;
4. warm measured hot set in controlled batches;
5. restore traffic gradually while watching DB latency/pool and cache fill;
6. avoid every instance prewarming the same full dataset;
7. preserve an emergency bypass/disable switch with audit;
8. test this before an incident.

High availability of Redis does not eliminate logical flush, codec deploy, mass expiry, network partition, or client timeout.

### Cache correctness test matrix

- read miss -> source -> fill -> subsequent hit;
- write commit -> delete failure -> TTL/event eventually repairs;
- concurrent miss/read with write/delete interleaving -> staleness bounded;
- out-of-order invalidation versions -> old event cannot erase/overwrite newer value incorrectly;
- duplicate invalidation -> safe no-op;
- negative cache then create -> create invalidates or short TTL bounds absence;
- tenant A key cannot return tenant B value;
- corrupt/old codec -> bounded eviction and source reload, with metric;
- Redis slow -> deadline/bulkhead protects request and source;
- mass expiry -> coalescing and fallback admission hold DB below budget;
- privacy deletion -> keys and derived values removed within declared SLO.

### Hot-key decision checkpoint

Start with evidence: per-shard CPU/network/ops, sampled key frequency, payload size, client connections, and request path. Local near-cache helps read hot keys but increases stale copies. Splitting a counter requires merge/error semantics. Replicating reads may move load to network or replicas. Sometimes the correct fix is to remove per-request cache lookup by publishing an immutable configuration snapshot to each process.

### Laboratory checkpoint

An SDE-2 cache answer includes source capacity during total cache loss. If the fallback cannot survive, caching increased normal capacity but created a cliff; admission, staged recovery, or a different authoritative read architecture must address it.

## Primary references

- Redis Documentation, “Client-side caching”: <https://redis.io/docs/latest/develop/clients/client-side-caching/>
- Redis Documentation, “Key eviction”: <https://redis.io/docs/latest/develop/reference/eviction/>
- Redis command documentation, `EXPIRE`: <https://redis.io/docs/latest/commands/expire/>
- Redis Documentation, “Distributed locks with Redis”: <https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/>
- PostgreSQL Documentation: <https://www.postgresql.org/docs/current/>

> **Version boundary:** Redis expiration timing, eviction, replication/failover, cluster routing, client tracking, and scripting/function behavior depend on server/client versions and topology. Treat official deployment documentation as authoritative. The worked Java service baseline is Java 21; later JDK/framework features are optional.
