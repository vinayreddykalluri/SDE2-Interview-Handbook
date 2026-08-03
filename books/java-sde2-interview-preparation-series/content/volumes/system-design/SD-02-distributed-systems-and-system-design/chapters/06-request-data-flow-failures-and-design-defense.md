# Request and Data Flow: Defending a Distributed Design Under Failure

Architecture boxes are easy to draw. The interview becomes meaningful when the interviewer points between two boxes and asks, “What if the reply never arrives?” This chapter turns the drawing into state transitions you can defend.

The goal is not to force every system into Kafka, Redis, and five services. Start with the smallest design that satisfies the contract, then add distribution only where scale, ownership, geography, or failure isolation requires it.

## 1. Begin with three truths

Before choosing technology, write three things:

1. **authoritative truth** — which durable state decides the answer;
2. **operation identity** — how retries refer to the same logical command; and
3. **observable completion** — how a caller or operator learns what happened after a timeout.

For an order command, these might be the relational order row, `(tenantId, idempotencyKey)`, and `GET /operations/{id}`. For a stream projection, truth may be the event log plus the consumer's durable offset/effect ledger. A cache is normally a copy, not truth.

If those three are unclear, adding more components multiplies ambiguity.

## 2. Low-level write flow

Consider `POST /orders`. The product accepts a command synchronously, then performs fulfillment asynchronously.

```text
client
  |  commandId + deadline + authenticated request
  v
edge/admission
  |  bounded body, tenant context, request identity
  v
order service
  |  validate -> authorize -> reserve idempotency record
  |  begin local transaction
  |    insert order
  |    insert outbox(OrderCreated)
  |    store stable command result
  |  commit
  v
202/201 + orderId + operation status

outbox relay -> broker partition -> fulfillment consumer
                                  |  insert effectId + update state
                                  |  in one local transaction
                                  v
                              durable effect
```

### What each identifier means

- `requestId` traces one network attempt. It changes on retry.
- `commandId` or idempotency key identifies one logical mutation. It stays stable on retry.
- `orderId` identifies the domain entity.
- `eventId` identifies one emitted fact.
- `traceId` correlates a causal path for diagnostics; it is not a deduplication key.

Using one value for every purpose creates accidental coupling. For example, a tracing system may sample or rewrite trace context; correctness cannot depend on it.

### Why order and outbox share a transaction

Two independent writes have two loss windows:

```text
DB commit succeeds -> broker publish fails    => durable order, missing event
broker publish succeeds -> DB rolls back      => event describes nonexistent order
```

The outbox stores the domain change and publication intent atomically. A relay may publish more than once if it crashes after publish but before marking the row. That is why consumers still need stable effect identity.

### The honest response to a timeout

A timeout means the caller stopped waiting; it does not prove the server stopped. If commit acknowledgement is lost, the outcome is **unknown** until reconciled. The safe retry uses the same command identity. A unique constraint and stored result turn a retry into replay rather than a second order.

## 3. Low-level read and cache flow

Suppose `GET /orders/{id}` has a regional cache.

```text
request carries tenant + optional minimumVersion
  -> authorize resource scope
  -> cache lookup by tenant/order
       | fresh and version >= minimumVersion -> serve
       | miss/expired/too old                 -> origin read
  -> source-of-truth read
  -> cache projection with version + TTL
  -> response with version/ETag
```

A TTL answers “how long may this copy remain without refresh?” A version token answers “is this copy at least as new as the caller requires?” They solve different problems.

For read-after-write, choose and state a contract:

- populate/invalidate the cache after commit and route the caller consistently;
- read the primary until a returned version becomes visible;
- pass `minimumVersion` and fail/refresh rather than return an older copy; or
- explicitly allow eventual visibility and expose that in the API.

“We use Redis” is not a consistency contract.

### Stampede and hot-key control

On expiry of a popular key, thousands of callers can miss together. Controls include single-flight request coalescing, jittered TTLs, bounded stale-while-revalidate for data safe to serve stale, and an origin bulkhead. A viral single key may need edge replication; adding database shards does not split one key's traffic by itself.

### When stale-on-error is unsafe

Stale product descriptions may be acceptable. Stale account lock, authorization, or inventory reservation may violate the product contract. Decide by data semantics, not one cache-wide flag. The executable companion refuses an entry older than the caller's required version even when expired data is otherwise allowed.

## 4. Delivery, ordering, and duplicate effects

Most practical broker guarantees are built from separate boundaries:

- producer acknowledgement describes broker persistence under a chosen replication policy;
- partition key defines the scope of order;
- consumer offset describes where reading can resume;
- an effect ledger describes which business effects are already durable.

“Exactly once” must name the scope. A broker transaction may atomically consume and produce inside the broker ecosystem, yet an email provider or independent database can still observe a duplicate after an acknowledgement is lost.

### Out-of-order completion

If records `10`, `11`, and `12` are processed concurrently and `11` finishes first, committing offset `12` would skip unfinished `10` after a crash. Track completed offsets and advance only across a contiguous prefix:

```text
completed {11}      -> next safe commit = 10
complete 10         -> next safe commit = 12
complete 12         -> next safe commit = 13
```

An alternative is one-at-a-time processing per partition, which is simpler but may reduce throughput. State the trade-off.

### Poison records

An infinite retry loop blocks the partition. Classify errors:

- transient dependency failure: bounded retry within deadline/budget;
- overload: delay/backpressure rather than hot retry;
- invalid permanent payload: quarantine/DLQ with enough context and an audited replay tool;
- unknown bug: stop or quarantine according to correctness risk, alert, preserve evidence.

A DLQ is not completion. Someone must own diagnosis, repair, replay, retention, and access control.

## 5. Partitioning and movement

Choose a key from access and correctness requirements:

| Key | Helps | Hurts |
|---|---|---|
| `customerId` | customer history and per-customer order | one celebrity/tenant can be hot |
| random/order hash | even write distribution | customer queries fan out or need an index |
| time bucket | retention and range scans | current bucket becomes a hotspot |
| region + entity | locality/residency | global entity operations coordinate |

Modulo hashing (`hash % N`) moves many keys when `N` changes. Rendezvous hashing scores each key against each node and chooses the highest score. Adding one node moves only the keys won by that node, which the companion verifies. Its simple FNV-based score is educational, not a security hash and not a complete production membership system.

### Resharding without pretending it is instant

A safe online move commonly needs:

1. a versioned routing map;
2. copy/backfill with checkpoints;
3. dual-read or source fallback during the gap;
4. change capture or bounded dual-write with reconciliation;
5. validation by counts/checksums/sample reads;
6. traffic cutover;
7. rollback window; and
8. old-shard retirement after safety criteria.

Dual-write alone is not atomic across shards. Name the repair process.

## 6. Deadlines, retries, and overload

Timeouts should descend from one end-to-end deadline. If an API has 500 ms left, a dependency cannot receive three independent 400 ms attempts.

```text
remaining budget
  - local processing reserve
  - network/attempt time
  - bounded jittered backoff
  = budget available for another retry
```

Retry only when all are true:

- the failure is plausibly transient;
- the operation is safe or idempotent under a stable identity;
- another attempt fits the remaining deadline;
- a retry budget prevents every caller from amplifying an incident; and
- the dependency is not explicitly shedding load.

Queues hide overload until latency and memory explode. Monitor queue depth **and oldest work age**. A half-full queue whose oldest item already exceeds its usefulness deadline is unhealthy. Reject early with a bounded response, slow producers, or degrade optional work. Unbounded executor queues are not resilience.

## 7. Multi-region: choose the conflict policy first

Ask whether two regions may accept writes for the same logical key.

- **single writer/home region:** simpler ordering and uniqueness; failover needs routing and fencing;
- **active-active, disjoint ownership:** route each tenant/key to one writer; movement is an explicit protocol;
- **active-active, shared key:** needs conflict-free operations, consensus/coordination, or a business conflict-resolution rule;
- **asynchronous replicas:** good read locality, but expose lag and read-after-write policy.

Clock timestamps alone do not make last-write-wins safe: clocks skew, a later arrival may represent older intent, and deletion can resurrect. Prefer version/sequence/fencing tokens where ordering matters. If the business accepts conflict resolution, state exactly which field-level operations commute and what happens to concurrent non-commutative writes.

## 8. Failure and edge-case matrix

| Failure or edge case | Unsafe shortcut | Defensible behavior | Evidence to watch |
|---|---|---|---|
| client retries after response loss | create a new order | same command identity, unique constraint, replay stable result | idempotency replay/conflict counts |
| commit acknowledgement lost | assume rollback | reconcile authoritative row before retry | unknown outcomes and resolution time |
| outbox relay republishes | assume broker deduplicates forever | stable event ID plus transactional consumer effect ledger | duplicate delivery/effect rate |
| cache contains older version | serve because TTL is valid | refresh/fail when minimum version is unmet | version misses and replica lag |
| cache outage | send all traffic to database | bounded fallback and admission; preserve source | fallback RPS, DB saturation |
| hot tenant or key | add partitions only | isolate/rate-shape, shard sub-key if semantics allow, replicate reads | per-key traffic without unsafe labels |
| consumer finishes out of order | commit highest finished offset | commit only contiguous completion prefix | gap count and oldest unfinished age |
| poison event | retry forever | classify, quarantine, alert, owned replay | retry age and DLQ age |
| retry storm | retry every 5xx immediately | deadline + exponential jitter + retry budget | retry ratio and dependency saturation |
| queue backlog ages out | accept until memory is full | age-aware load shedding/backpressure | oldest age, rejection rate |
| region partition | let both sides write blindly | enforce ownership/fencing or defined conflict rule | fencing rejection and replication lag |
| node added to shard pool | `hash % N` immediate cutover | versioned movement and validation | moved-key count/checksum mismatch |
| metrics backend fails | block request to emit metric | bounded/nonblocking telemetry path | dropped telemetry counter |
| trace/log contains secret | log entire request for debugging | allowlisted structured fields and redaction | audit findings/sample checks |

## 9. Eight realistic live-interview Q&A chains

### 1. Duplicate order after timeout

**Interviewer:** The client timed out and retried. How do you prevent two orders?

**Candidate:** The retry carries the same tenant-scoped command ID. I reserve it under a unique constraint and store the normalized payload fingerprint and stable result. Same key and same payload replays or reports in progress; same key with a different payload conflicts.

**Interviewer:** What if the first transaction committed but its response was lost?

**Candidate:** I query by the command identity and return the recorded result. I do not infer rollback from a network timeout.

### 2. “Exactly once” Kafka claim

**Interviewer:** Can Kafka guarantee that the customer receives exactly one email?

**Candidate:** Not end to end. Broker transactions can cover Kafka reads/writes, but the email provider may accept a send while its acknowledgement is lost. I deduplicate the notification intent locally, pass a provider idempotency key when supported, and reconcile ambiguous provider outcomes. The product promise should acknowledge that boundary.

**Interviewer:** Then what does your consumer offset prove?

**Candidate:** Only where consumption may resume. Business completion is proved by the durable effect state, ideally committed atomically with the offset/ledger within the boundary that supports it.

### 3. Cache freshness

**Interviewer:** Why not use a five-minute TTL everywhere?

**Candidate:** TTL expresses time, not business freshness. An authorization revocation and a catalog description have different stale risk. I define per-data freshness, use version tokens where callers require read-after-write, and permit stale-on-error only for safe projections.

**Interviewer:** Redis is down. Do you bypass it?

**Candidate:** With bounded fallback. I protect the source using concurrency limits and admission so a cache failure does not become a database failure.

### 4. Partition choice

**Interviewer:** Partition orders by `customerId` or `orderId`?

**Candidate:** It depends on dominant access and ordering. `customerId` keeps history together but risks hot tenants; hashed `orderId` spreads point writes but customer history needs a secondary index or fan-out. I would quantify the hottest tenant and query mix, state the invariant, then choose.

**Interviewer:** A celebrity tenant becomes 30% of traffic. Now what?

**Candidate:** Apply tenant admission/fairness first, replicate read projections, and sub-shard writes by a stable suffix only if cross-suffix ordering is not required. A tenant-wide ordered stream cannot be split without changing the contract.

### 5. Retry policy

**Interviewer:** A dependency returns 503. How many times do you retry?

**Candidate:** There is no safe constant without the deadline and operation semantics. I reserve time for the response, retry an idempotent call only while budget remains, use exponential full jitter and a retry budget, and honor overload signals such as `Retry-After`.

**Interviewer:** Why a retry budget?

**Candidate:** During an incident, retries multiply offered load precisely when capacity is lowest. The budget caps that amplification across callers.

### 6. Queue depth looks fine

**Interviewer:** The queue is only 40% full. Why are requests timing out?

**Candidate:** Depth ignores service rate and age. If the oldest item already exceeds its useful deadline, the queue contains doomed work. I measure oldest age and drain rate, shed new low-priority work, and remove the bottleneck rather than raise the bound blindly.

**Interviewer:** Would an unbounded queue avoid rejection?

**Candidate:** It converts explicit rejection into memory growth and enormous latency. A bounded queue makes overload visible and recoverable.

### 7. Active-active writes

**Interviewer:** We need multi-region availability. Make every region writable.

**Candidate:** I first ask whether the same key can be written concurrently. If not, home-region ownership with fenced failover is simpler. If yes, the business must define merge semantics or accept coordination. “Last timestamp wins” can lose intent under clock skew and should not be the unstated default.

**Interviewer:** How do you prevent the old primary writing after failover?

**Candidate:** Issue a monotonically increasing fencing epoch from the coordination authority and require storage to reject writes carrying an older epoch. Routing alone is insufficient.

### 8. Evolving the design

**Interviewer:** Why not start with microservices, sharding, and active-active so we never rearchitect?

**Candidate:** Those choices create failure modes and operational cost before evidence demands them. I keep stable module/data contracts, measure saturation and ownership pressure, and identify triggers—for example connection-pool utilization or regional latency. Then I evolve one boundary with backfill, shadow validation, compatible schemas, and rollback.

**Interviewer:** What do you present at the end of the interview?

**Candidate:** The contract and scale, authoritative data and partition key, happy write/read path, three critical failures, consistency choice, overload behavior, security boundary, SLIs/SLO, and the next scaling trigger.

## 10. Run the executable companion

```bash
javac --release 21 -Xlint:all -Werror DistributedSystemsPatterns.java
java -ea DistributedSystemsPatterns
```

Expected output:

```text
PASS distributed-systems executable checks
```

The program exercises capacity units, token-bucket admission, deadline-bounded retries, contiguous offset commits, saga transitions, quorum intersection, SLO burn rate, idempotent effects, cache-version decisions, rendezvous movement, and age-aware overload rejection. It is a reasoning lab, not a production framework.
