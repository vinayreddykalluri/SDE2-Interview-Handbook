# Actuator, Observability, and Tests That Prove the Runtime

Operations are part of the design. A service is not production-ready because `/actuator/health` says `UP`; it is ready when its signals help a human decide what is failing, who is affected, and what action is safe.

## Logs, metrics, traces, and profiles

| Signal | Best question | Common misuse |
|---|---|---|
| Logs | What discrete event happened with which safe context? | Treating every request as a verbose string dump |
| Metrics | How often, how much, how long, how many? | User/order IDs as labels causing cardinality explosion |
| Traces | Where did one request spend time across boundaries? | Sampling away every rare failure or recording secrets |
| Profiles/JFR | Where is CPU, allocation, lock, or runtime pressure? | Profiling only after guessing a code line |

Actuator exposes endpoints and contributes health/metrics integration. Micrometer supplies instrumentation abstractions. A tracing backend stores and queries spans. None of these chooses an SLO for you.

## Request and observation flow

```text
HTTP request
  |
server observation starts
  |  trace/span context
  v
controller -> service -> database/client instrumentation
  |             |                |
  |         domain metric    child span / timer
  v
response status + latency recorded
  |
exporter -> metrics/tracing backend
```

Context propagation is part of correctness. If work crosses an executor or reactive boundary without its observation context, traces fragment. If you copy all request state blindly, secrets can leak. Propagate supported tracing context and explicit business correlation identifiers; do not invent an unbounded thread-local bag.

## Health is a contract, not a dependency census

Use distinct probes when the platform supports them:

- **Liveness:** should this process be restarted? It should not fail merely because a remote dependency is temporarily down.
- **Readiness:** should new traffic be routed here? It may account for local initialization and dependencies essential to serving the route.
- **Deep diagnostic health:** can an operator inspect dependency detail? Keep this authenticated and separate from orchestration probes.

If liveness calls the database and the database fails, every instance may restart at once, adding load and erasing useful evidence. If readiness performs ten slow network calls, probes can become an outage amplifier.

## Safe endpoint exposure

Actuator endpoints can reveal environment values, request mappings, log levels, heap information, or configuration structure. Apply least exposure:

```text
public traffic network: business endpoints only
management network: selected health/metrics endpoints
authenticated operator path: sensitive diagnostics
```

Sanitize secrets, constrain who may mutate log levels or shutdown behavior, and do not assume an obscure path is a security control.

## Useful SDE-2 measurements

For the order path, start with service-level evidence:

- request rate, error rate, and latency distribution;
- dependency latency and outcome by bounded low-cardinality dimensions;
- executor active count, queue depth, rejections;
- connection-pool acquisition wait and usage;
- retry attempts and exhausted retries;
- circuit state changes, but not only circuit state;
- batch lag, processed/failed/skipped records, restart count;
- authorization denials by policy family, without user ID labels.

A percentile is not an individual trace. A timer around a retry wrapper may hide that one logical request made four remote attempts. Record logical request outcome and attempt-level evidence intentionally.

## Test pyramid for extensions

```text
many: pure Java policy and transformation tests
  |
focused: security filter/method, reactive publisher, batch step tests
  |
provider: database/broker/identity/vector-store contract tests
  |
few: full application journeys under realistic failure
```

### What each test should prove

| Concern | Fast proof | Boundary proof |
|---|---|---|
| Authorization | Policy allows/denies resource cases | Mock user/JWT crosses actual filter and method boundary |
| Reactive flow | Publisher respects demand/cancellation | Real non-blocking client; blocking detector where used |
| Batch restart | Processor/writer invariants | Fail after checkpoint, restart with job repository |
| Integration flow | Transformer/router behavior | Channel/adaptor error and redelivery path |
| Cloud client | Timeout/retry classifier in plain Java | Stub server produces latency/reset/status sequence |
| Observability | Tag policy and outcome mapping | Registry/tracing test sees expected low-cardinality signal |

Starting a whole context for a pure authorization predicate makes the test slower without proving more. Mocking an entire HTTP client cannot prove timeout configuration or wire semantics.

## Failure and edge-case matrix

| Failure | Misleading signal | Better evidence/action |
|---|---|---|
| Database down | Liveness fails and pods restart | Keep process live; readiness/route health and dependency metric degrade |
| Thread pool saturated | CPU looks low | Queue depth, active threads, rejection count, request latency |
| Retry storm | Dependency call success eventually | Attempts per logical request, total load, timeout budget |
| High-cardinality metric | Dashboard initially looks detailed | Bound labels; put unique IDs in sampled logs/traces |
| Trace missing async stage | Parent request looks fast | Supported context propagation and explicit async span |
| Health endpoint public | Convenience | Separate management plane and authorize detail |
| Test passes with mock | Production TLS/DNS/pool fails | Add provider/wire-level fixture |
| Global context dirtied per test | “Isolation” | Slow suite and hidden shared design | Prefer focused slices/pure tests; reset owned state |

## Quick check

1. Why should a database failure not normally fail liveness?
2. Why is `userId` a dangerous metric label?
3. What evidence distinguishes pool exhaustion from slow SQL?
4. Which test proves a Spring Security filter chain actually matched?
5. Why can retry metrics be misleading when recorded only around the outer call?

## Practice

- **Foundation:** Classify five signals as log, metric, trace, or profile.
- **Interview Core:** Design liveness and readiness for an API that can serve cached reads while its database is down.
- **Interview Core:** Replace a metric labeled by order ID with a safe metric plus trace/log context.
- **SDE-2 Follow-up:** Build a failure-injection test plan for timeout, retry, circuit breaker, and fallback behavior.
