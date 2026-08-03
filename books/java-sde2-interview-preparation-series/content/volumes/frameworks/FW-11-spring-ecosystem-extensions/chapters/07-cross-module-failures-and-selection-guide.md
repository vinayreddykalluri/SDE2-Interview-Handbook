# Cross-Module Failures and the Smallest-Module Decision

Production incidents rarely respect module boundaries. A token is accepted at the gateway, context disappears on an executor, a client retry repeats a payment, and the readiness probe removes every replica. The right unit of reasoning is the end-to-end invariant.

## One complete request trace

```text
1. gateway authenticates token and applies coarse route policy
2. order API validates its own audience and authorization
3. controller binds a narrow command
4. service checks tenant/resource invariant
5. client acquires connection with remaining deadline
6. payment endpoint processes stable idempotency key
7. response and attempt outcome become metrics/traces
8. audit is committed through a durable boundary
```

At each step, record:

- identity and tenant source;
- current thread/subscriber context;
- transaction/resource boundary;
- remaining deadline;
- attempt number and idempotency key;
- durable state before and after;
- observable success/failure signal.

## Cross-module failure matrix

| Incident | Hidden interaction | First evidence | Safe first design move |
|---|---|---|---|
| All replicas restart during database outage | Liveness includes DB | Probe history and restart count | Separate liveness from dependency readiness |
| Duplicate payment after slow response | Client retry + non-idempotent side effect | Logical request ID versus provider operations | Stable idempotency key and reconciliation |
| Authorized MVC call denied in async stage | Thread-bound security context lost | Trace/thread/context at handoff | Pass explicit actor or supported propagation |
| Gateway returns empty success during downstream outage | Fallback erased error semantics | Fallback counter and response contract | Explicit degraded response or fail |
| WebFlux latency climbs with low CPU | Blocking call on event loop | Thread dump/event-loop block evidence | Remove/isolate blocking dependency |
| Batch restart creates duplicate partner records | Checkpoint not atomic with remote side effect | Job execution + partner idempotency log | Outbox/idempotency/reconciliation |
| Integration memory grows steadily | Aggregator groups never complete | Group-store age/count | Expiry, completion, late-message policy |
| Security rules differ by endpoint | First chain matcher shadows later chain | Selected chain/filter list | Narrow matchers first; allowed/denied tests |
| Config refresh destabilizes clients | Timeout and retries changed independently | Config version and attempt duration | Versioned validated settings |
| Retry storm opens circuit everywhere | Retry multiplication and shared dependency | Calls per logical request | Single retry owner, jitter, concurrency cap |
| Trace disappears after reactive hop | Unsupported manual thread-local | Span graph and scheduler boundary | Framework context propagation |
| Batch reports completed with missing rows | Skip policy masks systemic parse error | Input/commit/reject reconciliation | Classified skip threshold and fail systemic errors |

## Module selection table

| Workload | Prefer first | Reconsider when |
|---|---|---|
| Ordinary blocking CRUD API | Spring MVC | Streaming or very high I/O concurrency has measured value and dependencies are async |
| Browser login/API authorization | Spring Security | Never replace with custom filters; integrate an identity provider as needed |
| Platform already supplies DNS and gateway | Native platform + focused clients | Application needs a feature not safely supplied there |
| Finite nightly import with restart | Spring Batch | A simple small atomic task has no restart/scale need |
| Continuous broker events | Kafka/Rabbit listener stack | Flow requires integration adapters/routing across protocols |
| In-process transformation across adapters | Spring Integration | A few direct calls are clearer and no messaging semantics are needed |
| Basic health and metrics | Actuator/Micrometer | Always define SLO and exposure; module alone is incomplete |

## When not to add an extension

Do not add Spring Cloud discovery when Kubernetes service DNS already provides the required location model. Do not add WebFlux around a blocking JDBC/SDK path to make the controller signature look modern. Do not model a two-line method call as an Integration flow when it adds no routing, temporal decoupling, or adapter value. Do not use Batch for one small transaction triggered by a request.

The smaller design wins when it makes ordering, failure, ownership, and tests clearer.

## SDE-2 design checklist

1. **Invariant:** What must remain true despite duplicate, delay, timeout, and restart?
2. **Ownership:** Which layer owns authentication, authorization, retry, routing, and state?
3. **Capacity:** What is bounded—threads, connections, queue, demand, partitions, group state?
4. **Time:** What is the end-to-end deadline, and who consumes it?
5. **Durability:** Which state survives process loss, and what can repeat?
6. **Security:** Which input is trusted, validated, least-privileged, and tenant-scoped?
7. **Evidence:** Which test and telemetry distinguish success, degradation, and silent loss?
8. **Version:** Which Boot/release-train/module line is supported together?

## Quick check

1. Why does a fallback belong in the business contract?
2. What makes a queue “bounded” in operational terms?
3. Why can platform and application retries multiply?
4. What evidence proves no batch rows disappeared?
5. When is a direct Java call better than an Integration flow?

## Practice

- **Interview Core:** Take one incident row and produce a five-minute diagnosis plan.
- **Interview Core:** Remove one unnecessary Spring extension from a hypothetical CRUD service and justify the simpler design.
- **SDE-2 Follow-up:** Design one capacity budget spanning gateway, WebFlux/client concurrency, database pool, and downstream limit.
