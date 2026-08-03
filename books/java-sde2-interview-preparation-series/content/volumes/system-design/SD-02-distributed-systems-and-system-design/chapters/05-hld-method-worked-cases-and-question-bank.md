# A Rigorous HLD Interview Method with Worked Java-Backend Cases

## Learning objectives

After this chapter, you should be able to:

- run a 45–60 minute high-level design interview as a sequence of explicit decisions;
- turn vague scale claims into unit-bearing estimates and bottleneck hypotheses;
- define APIs, data models, partitioning, consistency, and failure behavior before drawing a large architecture;
- connect Java implementation boundaries to connection pools, threads/virtual threads, serialization, GC, and graceful shutdown;
- work through a URL-shortening service and multi-channel notification platform; and
- answer SDE-2 design follow-ups with invariants, tradeoffs, evidence, and evolution paths.

## 1. The interview operating system

### Phase 1: frame requirements and invariants (5–8 minutes)

Ask questions that change architecture:

- primary user journeys and explicit non-goals;
- read/write ratio, payload size, retention, peak and geographic distribution;
- availability, latency, durability, and consistency requirements per operation;
- identity, ordering, uniqueness, privacy, residency, and deletion;
- synchronous versus asynchronous outcome;
- abuse/rate limits and multi-tenancy;
- existing constraints: Java stack, relational database, Kafka, cloud, team size.

Write the hardest invariants:

```text
one idempotency key -> one logical command result
short code -> at most one active destination within namespace
one notification request -> no more than policy-allowed deliveries per channel
tenant A cannot read or consume tenant B's capacity/data
```

Do not spend half the interview collecting trivia. State reasonable assumptions and invite correction.

### Phase 2: estimate scale (4–6 minutes)

Calculate average and peak RPS, concurrent work, storage/year, bandwidth, cache working set, partitions, and downstream rate. Show equations and ranges. Use estimates to justify choices, not to perform fake precision.

Sensitivity matters more than one number:

```text
baseline peak 10k RPS
launch/incident factor 3 -> 30k
cache miss rises from 1% to 30% -> DB fallback from 100 to 3,000 RPS
```

### Phase 3: contracts and data model (7–10 minutes)

Define:

- APIs/commands/events, idempotency and pagination;
- statuses and error/unknown outcomes;
- primary identities and unique constraints;
- access patterns and candidate indexes;
- authoritative source versus cache/derived views;
- event schema/key/order and retention.

An API is not just endpoint names. Include method semantics, operation identity, conditional update/version, max size, and asynchronous status.

### Phase 4: minimum viable architecture (8–10 minutes)

Draw the shortest path satisfying current numbers:

```text
edge/admission -> stateless Java service -> authoritative store
                                   |
                                   +-> outbox/broker -> workers
                                   +-> cache/read model where justified
```

Label ownership and failure domains. Avoid adding five databases before identifying why one relational database plus replicas/partitions fails.

### Phase 5: deep dive on two risks (12–18 minutes)

Choose the most relevant:

- hot key/partition and rebalancing;
- local versus distributed transaction and duplicates;
- cache consistency/stampede;
- Kafka ordering/rebalance/retry;
- multi-region consistency/failover;
- rate limiting/backpressure;
- schema/data migration;
- security/privacy;
- observability/SLO and incident recovery.

Walk one happy path and at least three failure paths. A strong answer names the state after timeout and how recovery discovers truth.

### Phase 6: operations, evolution, and summary (5 minutes)

Close with:

- RED/USE metrics, SLI/SLO, probes, logs/traces, backlog age;
- deployment/schema compatibility and rollback;
- capacity/admission and graceful shutdown;
- top tradeoffs and next scaling trigger;
- concise architecture recap tied to requirements.

## 2. Decision record template

For each major choice, say:

```text
Decision: key URL mappings by short-code hash.
Reason: point reads dominate and name the code.
Guarantee: one code maps to one active destination under a unique constraint.
Cost: range/admin queries fan out or need a secondary index.
Failure: hot viral code creates read hotspot.
Evidence/trigger: per-key traffic and shard saturation; add edge/cache replication.
Alternative: range partition by creation time rejected because redirect lookup lacks time.
```

This pattern demonstrates judgment. “Use Cassandra because scale” does not.

## 3. Worked case A: URL shortening and redirect service

### Requirements

- Create a short URL for a valid `https` destination.
- Redirect by code with p99 under 50 ms in primary regions.
- 100 million new links/month; 100:1 redirect-to-create ratio; peak redirect 80k RPS.
- Default links do not expire; owners can disable them.
- Custom aliases are unique within tenant namespace.
- Analytics can lag minutes and must not slow redirects.
- Malicious destinations/abuse need policy; deletion/privacy supported.

Non-goals: exact real-time global click count, guaranteed arbitrary custom word, browser-side preview rendering.

### Estimate

100 million/month is about 39 writes/s average; assume 10x peak ≈ 400/s. If one mapping record averages 600 bytes including indexes/overhead estimate range, raw annual new data ≈ 720 GB before replication/backups; validate actual encoding. Redirect 80k RPS at 1 KiB response headers/body average ≈ 78 MiB/s before edge/CDN overhead. A cache working set depends on popularity skew; measure top-code coverage.

### API

```text
POST /v1/links
Idempotency-Key: ...
{destination, customAlias?, expiresAt?}
-> 201 {code, shortUrl, version, createdAt}

GET /{code}
-> 302 or 307/308 according to product permanence/cache policy

DELETE /v1/links/{code}
If-Match: ...
-> 204 / 412 / 404
```

Redirect status choice affects client/cache permanence; do not use permanent caching while owners can edit/disable unless policy reconciles it. Validate destination scheme, canonicalization, length, and block unsupported internal schemes. Avoid fetching arbitrary destination URLs synchronously in the create path because it can create SSRF; a separate safe scanner needs network egress controls.

### Data and ID generation

Authoritative mapping:

```text
Link(code PK, tenantId, destination, status, ownerId,
     createdAt, expiresAt, version, abuseState)
unique(tenantId, customAlias) when custom namespace applies
```

Generated code options:

- encode a unique numeric ID in base62: compact/collision-free under ID service, but exposes sequence/volume and needs globally available allocation;
- generate random bits and base62: decentralized/unpredictable, but insert must retry rare unique collision;
- hash destination: same destination may need different owners/policies; collision handling and predictability;
- pre-generated code pool: absorbs generator outage but adds inventory/security/operations.

Choose 72–96 random bits encoded in URL-safe alphabet for unpredictability and collision margin, with database unique constraint as final guard. Exact length follows threat/capacity calculation. Custom alias uses normalized namespace plus constraint.

### Partition and read path

Partition by hash of `code` because redirects name it and traffic spreads by code count. One viral code is still a hot key. Read flow:

1. edge rate/abuse control;
2. regional cache/near-cache lookup by versioned code;
3. on miss, mapping store point read;
4. validate active/not expired/abuse policy;
5. cache bounded projection with TTL and invalidation/version;
6. return redirect;
7. emit click telemetry asynchronously under sampling/privacy policy.

For a viral code, replicate at CDN/edge or local cache. Mapping changes must invalidate/bump version and account for cached redirect status. Checkout-style strong correctness is not needed for analytics, but disable/abuse response may require short cache TTL or push invalidation.

### Create path and consistency

1. authenticate owner and enforce quota;
2. reserve idempotency key/payload fingerprint;
3. validate/canonicalize destination without unsafe fetch;
4. generate candidate code;
5. insert mapping and completed key under transaction; retry unique random collision, not arbitrary DB failure;
6. append `LinkCreated` outbox event for analytics/scanning;
7. commit and respond.

The create response is read-your-writes because it returns committed record. A subsequent regional redirect may route to a lagging replica; route initial reads to primary, populate cache on write, or carry version token if the product requires immediate global visibility.

### Analytics

Redirect nodes produce compact `LinkClicked` events keyed based on required aggregation/order—not necessarily code if hot keys would overload one partition. Approximate/counting aggregation can shard `(code,bucket)` and merge. Protect redirect latency: bounded nonblocking emission, local buffer only if durability policy allows, or broker client with strict capacity. Define what lost/sampled clicks mean.

### Failures and operations

| Failure | Behavior |
|---|---|
| cache unavailable | bounded fallback to store; local admission prevents store collapse |
| mapping replica lag | session/version or primary route for just-created code; otherwise documented delay |
| code store shard unavailable | redirects for that shard degrade/fail; replicas/failover with fencing |
| outbox/broker down | redirects continue; click analytics may buffer/drop under stated policy; mapping outbox backlogs |
| abusive viral code | edge block/rate policy, rapid invalidation, audit |
| cache stampede | single-flight, stale policy for active links, source bulkhead, edge cache |

SLIs: eligible redirect success and latency; create availability; disable propagation freshness; mapping-store/caching errors; analytics freshness separately. Track hot keys without using code as metric label.

### Java implementation notes

Use immutable DTOs/records, bounded validation, `SecureRandom` through an owned code generator, JDBC/JPA projection reads, and explicit `Clock`. Virtual threads can support blocking redirect lookups but store/cache connections remain bounded. Avoid allocating large URI/JSON graphs on the hot redirect path. Profile before micro-optimizing base62.

### Case checkpoint

A complete answer mentions code uniqueness, tenant namespace, read hotspot, cache invalidation/disable freshness, idempotent create, store partition, analytics decoupling, SSRF/abuse, failover, and SLO—not merely “Redis plus database.”

## 4. Worked case B: multi-channel notification platform

### Requirements

- Services submit email, SMS, and push notifications.
- Peak 20k requests/s; campaigns can burst millions of recipients.
- Transactional notifications are high priority with 99% dispatched within 30 seconds.
- Marketing can be delayed and must honor consent/quiet hours/unsubscribe.
- Provider limits and costs differ by channel/tenant/region.
- Request retries must not create unintended duplicate delivery.
- Templates and destination data are sensitive; audit and deletion policies apply.

Clarify that “exactly once notification” is not fully controllable: a provider can accept and deliver while its acknowledgement is lost, and email/SMS recipients can receive duplicates. The platform promises deduplicated intent and best available provider idempotency/reconciliation.

### API and state machine

```text
POST /v1/notifications
Idempotency-Key: ...
{tenantId derived/authenticated, recipientRef, templateId,
 channelPolicy, variables, priority, scheduleAt}
-> 202 {notificationId, statusUrl}

GET /v1/notifications/{id}
-> ACCEPTED | SCHEDULED | DISPATCHING | SENT | DELIVERED?
 | RETRY_WAIT | FAILED_FINAL | CANCELLED | OUTCOME_UNKNOWN
```

Separate `SENT_TO_PROVIDER` from provider delivery/read signals. Webhooks are untrusted input: verify signature, replay window, provider event ID, and state transition. `DELIVERED` semantics differ by channel/provider.

### Architecture

```text
API -> relational command/state + outbox
              |
              v
        scheduler/router -> priority/channel Kafka topics
                                  |
              +-------------------+------------------+
              v                   v                  v
         email workers       SMS workers        push workers
              |                   |                  |
         provider pool        provider pool       APNs/etc.

webhook ingress -> inbox/dedupe -> notification state
consent/template services/read models
rate-limit and provider-health control plane
```

The API transaction stores notification intent, idempotency outcome, and outbox. It returns `202` because delivery is asynchronous. Workers are separate bulkheads by channel/provider/priority.

### Data model

```text
Notification(id, tenantId, recipientRef, templateVersion,
             channel, priority, scheduleAt, state, attempt,
             operationKey, sourceVersion, createdAt, updatedAt)
DeliveryAttempt(id, notificationId, provider, providerOperationId,
                state, startedAt, deadline, responseCode, nextAttemptAt)
Inbox(provider, providerEventId unique, receivedAt)
Outbox(eventId, aggregateId, type, payloadRef, occurredAt, publishedAt)
```

Minimize destination/content in durable event payloads. Workers can retrieve encrypted/authorized rendering data by reference, but that adds dependency/failure; or use encrypted payload with strict key/retention. Templates are immutable/versioned so a queued notification does not silently change wording.

### Partitioning and ordering

Key events by `notificationId` for per-notification state order. Campaign generation should not emit millions synchronously in one API transaction. Store a campaign job and page recipients under snapshot/consent semantics, generating bounded notification batches.

One tenant campaign can monopolize partitions/providers. Apply hierarchical admission:

- global provider/channel rate;
- tenant quota and fair scheduling;
- priority reservation for transactional traffic;
- per-destination anti-abuse limit;
- worker concurrency and bounded retry queue.

Strict priority can starve marketing; use weighted fair queues/reserved capacity. Rate tokens can be leased locally from a central policy to reduce hot coordination, accepting bounded imprecision.

### Worker flow

1. consumer receives notification event at least once;
2. DB transaction checks inbox/event ID and current notification state/version;
3. reserve a delivery attempt with stable `providerOperationId` and transition to `DISPATCHING`;
4. commit; remote provider call occurs outside DB transaction with deadline;
5. transaction records accepted/rejected/unknown result and outbox state event;
6. commit Kafka offset after durable state;
7. webhook/reconciliation can advance state idempotently.

If crash occurs after provider accepted but before result record, replay reads `DISPATCHING/unknown` and queries provider by operation ID or safely repeats only if provider deduplicates. Otherwise route to reconciliation instead of guaranteed duplicate.

### Retry and DLQ

- `429`/temporary provider outage: reschedule with server guidance, jitter, deadline/campaign expiry, and rate control;
- invalid destination/template: terminal and feed hygiene signal;
- authentication/config failure: open breaker/stop provider path and page owner;
- unknown outcome: reconcile before sending anew;
- poison event/code bug: quarantine with secure reference, not repeated hot loop;
- retry topic preserves key or state version prevents stale overwrite.

Campaign notifications past their relevance deadline should expire rather than be delivered days late. Transactional notifications can fail over provider if consent/channel policy and provider operation identity support it. Provider failover can itself duplicate if original outcome is unknown.

### Consent and security

Check consent at acceptance and again near dispatch for marketing because queue delay matters. Store the policy/version evidence used. Unsubscribe must propagate within a defined freshness SLO. Tenant-scoped authorization protects status APIs. Template variables are untrusted content; escape for HTML/text/channel and prevent template injection. Restrict preview/test sends.

Encrypt sensitive destinations at rest under managed keys, tokenize/reference where possible, redact logs/traces, and define retention/deletion across DB, events, DLQ, provider, and backups. Rate limiting is also abuse/cost control.

### Observability and SLO

Metrics:

- accepted/dispatched/provider-accepted/delivery-callback rate by bounded tenant tier/channel/provider/outcome;
- queue oldest age and count by priority/channel;
- end-to-end acceptance-to-dispatch histogram;
- provider latency/error/rate-limit/breaker;
- retries, unknown outcome, reconciliation age, DLQ age;
- consent suppression, duplicate/inbox hit;
- consumer lag/rebalance and DB pool/saturation.

Trace a sample from API to outbox/Kafka/worker/provider using links/context, but do not put phone/email/content in attributes. SLOs differ: transactional dispatch freshness, marketing completion deadline, consent propagation, and status API availability.

### Case checkpoint

A strong design separates intent, attempt, provider acceptance, and delivery; uses durable workflow/outbox/inbox; scopes ordering; protects priority and provider capacity; treats unknown outcome/reconciliation explicitly; and includes consent, privacy, cost, and operational backlog.

## 5. Cross-case comparison

| Decision | URL shortener | Notifications |
|---|---|---|
| dominant path | low-latency read | asynchronous workflow |
| authoritative key | short code | notification ID/idempotency key |
| partition pressure | viral read hot key | tenant campaign/provider limits |
| consistency focus | code uniqueness and disable freshness | state transition, deduped intent, unknown remote outcome |
| cache | central redirect optimization | limited config/template/consent views with freshness rules |
| Kafka | analytics/invalidation | core workflow transport |
| hard failure | store/cache outage and stale redirect | provider timeout, retry, duplicate delivery, backlog |
| security | malicious URL/SSRF/abuse | consent, content injection, PII, provider webhook |

The architecture follows requirements; no universal “system-design stack” exists.

## 6. HLD question bank and model checkpoints

1. **Where is the source of truth?**

   Checkpoint: identify authoritative store per fact and every derived cache/index/event view plus rebuild/repair.

2. **What is the partition key?**

   Checkpoint: common routing, load skew, hot key, transaction/order scope, rebalancing, secondary fan-out.

3. **What happens on timeout after a side effect?**

   Checkpoint: unknown outcome, stable operation ID, status/reconciliation, no assumption of rollback.

4. **How do you prevent duplicates?**

   Checkpoint: API idempotency, outbox event ID, consumer inbox/state predicate, provider idempotency; name retention.

5. **What consistency does the user observe?**

   Checkpoint: operation-specific linearizable/session/bounded/eventual contract and failover behavior.

6. **How do you handle a hot key?**

   Checkpoint: measure traffic, cache/replicate reads, split if semantics allow, isolate tenant, admission; adding random salt costs order/locality.

7. **How does the system degrade?**

   Checkpoint: explicit priority, stale-safe responses, early rejection, queue age/limit, core path isolation.

8. **Why Kafka?**

   Checkpoint: durable backlog, independent consumers, partition order/parallelism, replay; accept latency, duplicates, operations, schema burden. Do not use merely because “microservices.”

9. **How do you deploy schema change?**

   Checkpoint: additive/expand-migrate-contract, mixed versions, backfill, compatibility tests, rollback/repair.

10. **How do you know it works?**

    Checkpoint: SLIs/SLOs, RED/USE, plans/load/chaos tests, reconciliation, data quality, runbooks.

11. **Where does Java matter?**

    Checkpoint: transaction/proxy boundary, connection/executor limits, virtual threads not resource magic, serialization compatibility, GC/allocation evidence, context propagation, graceful shutdown.

12. **When would you change the design?**

    Checkpoint: measurable trigger—store capacity, shard size, hot-key rate, SLO burn, cache working set, backlog recovery time—not speculative scale.

## 7. Final SDE-2 interview checklist

- [ ] Requirements and non-goals are explicit.
- [ ] Invariants, identities, and operation-specific consistency are named.
- [ ] Estimates include units, peaks, retention, replication, and headroom.
- [ ] APIs include idempotency, bounds, versions, errors, and async status.
- [ ] Data model and indexes derive from access patterns.
- [ ] Minimal architecture labels ownership and failure domains.
- [ ] Partitioning covers skew, hot keys, ordering, fan-out, and rebalance.
- [ ] Cross-system effects have outbox/inbox/idempotency and unknown-outcome recovery.
- [ ] Cache and queues have staleness/capacity/failure contracts.
- [ ] Security, privacy, abuse, and tenant isolation are first-class.
- [ ] Observability connects SLIs to RED/USE and repair backlogs.
- [ ] Evolution uses measurable triggers and compatible migrations.
- [ ] Summary states decisions, costs, alternatives, and next bottleneck.

## 8. Exercises and mock loops

1. Re-run the URL shortener assuming editable destinations require global disable within two seconds. Change cache/replication design.
2. Re-run notifications with no Kafka allowed. Design a database queue and compare claim/lock, scale, replay, and operations.
3. Design a distributed rate limiter for 100k tenants with a few whales; quantify precision versus hot coordination.
4. Design a job scheduler with delayed execution, leases/fencing, retries, cancellation, and tenant fairness.
5. Design a file-processing service with multi-part upload, virus scanning, idempotency, lifecycle, and regional storage.
6. Conduct a 45-minute mock. Spend at most 8 minutes clarifying, show three estimates, deep-dive two failures, and reserve 5 minutes to summarize.

## 9. Common HLD anti-patterns

- **Boxes before contracts:** drawing gateway, cache, Kafka, and five stores without a user-visible invariant or failure behavior.
- **Scale adjectives without arithmetic:** “billions” with no peak RPS, bytes, retention, or hot-key distribution.
- **Exactly-once by assertion:** ignoring API retries, outbox publish/mark, consumer effect/offset, and remote provider unknown outcomes.
- **Cache as correctness:** treating a miss, stale value, or eviction as impossible and omitting source-collapse recovery.
- **Partitioning by record count:** ignoring one whale tenant, viral key, expensive event, or cross-shard transaction.
- **Retry as availability:** amplifying a saturated dependency and repeating non-idempotent work.
- **Multi-region by arrows:** no write authority, quorum, fencing, read-your-writes, conflict, or residency contract.
- **Observability appendix:** dashboards listed after design with no SLI, backlog age, reconciliation, or capacity signal.
- **Premature exotic technology:** paying operational complexity before a measured trigger.
- **Java thread optimism:** increasing platform/virtual threads while connections, CPU, memory, or downstream concurrency remain fixed.

Repair an answer by choosing one critical operation and narrating: request identity, route, validation, authoritative read/write, commit acknowledgement, derived event/cache, response, timeout state, retry identity, and telemetry. Then scale the measured bottleneck. This execution thread turns a diagram into an engineering design.

## Primary references

- Java SE 21 Documentation: <https://docs.oracle.com/en/java/javase/21/>
- Apache Kafka Documentation: <https://kafka.apache.org/documentation/>
- OpenTelemetry Specification: <https://opentelemetry.io/docs/specs/>
- RFC 9110, “HTTP Semantics”: <https://www.rfc-editor.org/rfc/rfc9110>
- OWASP Application Security Verification Standard: <https://owasp.org/www-project-application-security-verification-standard/>

> **Version boundary:** worked designs are technology-neutral at the contract layer. Kafka, databases, caches, Spring integrations, cloud load balancers, and Java runtimes must be verified at selected versions. Java 21 is the implementation baseline; later JDK structured-concurrency or framework conveniences are optional deltas, not assumed guarantees.
