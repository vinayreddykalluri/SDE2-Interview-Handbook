# Capstone: One Spring Boot Request from Socket to Durable Result

This chapter connects the earlier container, MVC, transaction, idempotency, operations, and testing chapters. It does not repeat the dedicated Spring Framework or Spring Boot books. Its purpose is to make one request explainable at every hidden boundary.

> **From Vinay:** When an interviewer asks what Spring Boot does, do not list annotations. Follow one request. Name the thread/context, proxy, resource acquired, durable write, failure window, and response. That turns framework knowledge into engineering judgment.

## 1. End-to-end request flow

Consider `POST /v1/orders` with an `Idempotency-Key`.

```text
client/TLS connection
  -> embedded server accepts and dispatches request
  -> servlet filters (trace, security, limits)
  -> Spring Security filter chain authenticates/authorizes
  -> DispatcherServlet
  -> HandlerMapping chooses controller method
  -> HandlerAdapter + argument resolvers
  -> HttpMessageConverter decodes bytes to request DTO
  -> Bean Validation checks DTO shape
  -> controller maps identity + transport command
  -> service proxy invokes transaction interceptor
  -> transaction manager binds/acquires persistence resources
  -> idempotency row + inventory + order + outbox are changed
  -> ORM flush/JDBC statements reach database
  -> database commit succeeds or fails
  -> service returns application result
  -> controller chooses status/headers/body
  -> HttpMessageConverter encodes response
  -> server writes bytes; client may or may not receive them
```

This is a reasoning map, not a promise that every resource is acquired eagerly. A pool connection may be obtained lazily, an ORM may defer SQL until flush, and the response may be buffered. The exact server/filter/converter list depends on configuration and version.

### Boundary table

| Boundary | Input → output | Hidden work | Failure evidence |
|---|---|---|---|
| filter/security | HTTP request → authenticated request | token parsing, authorization, rate/size rules | status, security event, trace |
| converter/validation | bytes → DTO | content negotiation, Jackson/codec, validation | Problem Details without raw exception |
| controller | transport → application command | identity/tenant scoping, status mapping | contract test |
| service proxy | method call → transactional invocation | interceptor, rollback rules, thread-bound context | transaction span/name |
| repository/ORM | domain changes → SQL | dirty checking, flush, mapping, pool | SQL count/time, pool wait |
| database | SQL → commit/rollback | indexes, locks, log/durability | constraint/lock/commit result |
| response writer | result → bytes | conversion, compression, socket | response/connection telemetry |

An exception in a servlet filter may occur before MVC and therefore bypass a controller advice designed for controller exceptions. Authentication failures, unsupported content types, malformed JSON, validation failures, business conflicts, and database outages need one coherent error protocol but may originate in different infrastructure.

## 2. The durable create timeline

```text
T0 request accepted
T1 key + payload fingerprint reserved
T2 transaction active
T3 inventory condition succeeds
T4 order/outbox/idempotency result written
T5 flush succeeds
T6 COMMIT acknowledged
T7 response encoded
T8 response received by client
```

### Failure windows

| Failure point | Durable state | Correct client/server behavior |
|---|---|---|
| before T1 | none | safe to retry with same key |
| after T1 before ownership/result | reservation may be in progress | same payload receives in-progress/lease policy; different payload conflicts |
| T3 constraint/stock failure | transaction rolls back | stable domain error; no generic retry |
| after T5 before T6 | commit not yet known | roll back if failure is known; otherwise reconcile |
| after T6 before T8 | order is committed, response lost | same key returns recorded result |
| response encoding fails after T6 | committed result, no valid response | log/trace; replay by key, never “undo” by exception handler |
| process stops after T6 | outbox remains | relay resumes; consumers deduplicate event |

The most important case is **committed but unacknowledged to the caller**. HTTP retry is safe only because the server stores the scoped key, request fingerprint, and original outcome atomically with the order.

## 3. Convenience APIs and the behavior beneath them

| Convenience | What actually matters |
|---|---|
| Boot auto-configuration | conditional bean definitions chosen from classpath/properties/user beans; inspect condition report |
| `@RequestBody` | selected message converter, media type, size, unknown-field and date/numeric policy |
| `@Valid` | DTO constraints only; not authorization, uniqueness, or concurrent invariant |
| `@Transactional` | proxy/context interception, selected manager, propagation, rollback, connection/flush/commit |
| repository `save` | new/merge/managed semantics and deferred SQL; not proof of commit |
| `@Async` | executor boundary; imperative transaction/security/MDC thread locals do not automatically follow |
| retry annotation | a repeated method call; idempotency, exception classification, deadline, and amplification still yours |
| Actuator health | configured indicator result; not proof every business dependency/path is safe |

Spring Framework’s imperative transactions are commonly thread-bound. Starting a new thread or submitting work to an executor does not carry that transaction. Reactive transactions use the Reactor context and require the participating operations to remain in that reactive pipeline. Mixing these models without naming the context is an interview red flag.

## 4. Deadline and resource budget

One request has an end-to-end deadline, not independent unlimited timeouts:

```text
client deadline 2,000 ms
  server admission/queue         <= 100 ms
  pool acquisition               <= 150 ms
  DB transaction/query/lock      <= 700 ms
  optional downstream call       <= 500 ms
  serialization/network margin   <= 200 ms
  retry reserve                  <= 350 ms
```

The numbers are workload-specific. The rule is that each child attempt receives only the remaining budget. A timeout/cancellation is not proof a database or downstream operation stopped; outcome reconciliation remains necessary.

Virtual threads can reduce the cost of blocked Java threads. They do not create more database connections, CPU, memory, downstream capacity, or lock throughput. Admission control and bounded pools remain essential.

Do not hold a database transaction while calling a slow remote service. It holds a connection and possibly locks across an independent failure boundary. Commit intent/outbox/state first, then invoke externally with idempotency and reconciliation.

## 5. Concurrency and shutdown windows

Spring singleton beans can serve concurrent requests. Keep controllers/services stateless or synchronize state through the correct durable mechanism; a mutable `HashMap` field is neither tenant-safe nor cluster-safe.

During shutdown:

1. readiness changes to refusing traffic;
2. new requests stop according to server/orchestrator behavior;
3. in-flight requests receive a bounded grace period;
4. background relays/listeners stop accepting work and checkpoint safely;
5. resources close after work finishes or the deadline expires.

If the orchestrator kills the process before the application grace period, configuration cannot rescue it. Align termination grace, server shutdown timeout, load-balancer drain, consumer polling, and job leases.

## 6. Edge-case matrix

| Edge case | Wrong assumption | Correct design question |
|---|---|---|
| malformed JSON | controller advice always handles it | which converter/filter/advice boundary owns the error? |
| validation passes | request is authorized/unique | where are identity and database constraints checked? |
| self-invoked transactional method | annotation always starts transaction | did the invocation cross the proxy? |
| checked exception | always rolls back | what is the configured rollback policy/version? |
| `REQUIRES_NEW` audit | independent means free | where does its extra connection come from under load? |
| client disconnect | server work stopped | can processing/commit continue and how is result reconciled? |
| async event after commit | thread sees transaction/session | what context/data was explicitly captured? |
| liveness checks database | restart fixes shared DB outage | liveness should avoid cascading restarts; readiness choice is deliberate |
| huge request body | validation will reject cheaply | enforce network/parser size before materializing it |
| response serialization after commit | exception rolls back | commit already happened; idempotent result must remain recoverable |

## 7. Seven live interview chains with worked answers

### Interview 1 — trace the request

**Interviewer:** “What happens after an HTTP request reaches Spring Boot?”

**Candidate:** “The embedded server dispatches through servlet and security filters. `DispatcherServlet` maps a handler; argument resolvers and an `HttpMessageConverter` build the DTO; validation runs; the controller calls an application service. If that call crosses a transactional proxy, the interceptor starts/joins a transaction and binds resources. Repository work may defer SQL until flush; the database commit precedes response conversion. A client disconnect or response failure after commit does not roll the database back.”

**Follow-up:** “Where can `@ControllerAdvice` fail to help?”

**Worked answer:** Filter/security or container errors can occur outside MVC handling. Configure those layers to emit the same safe Problem Details contract or route through a shared error component.

### Interview 2 — lost create response

**Interviewer:** “The order committed, but the client timed out.”

**Candidate:** “The server cannot infer rollback from the timeout. The client retries the same scoped idempotency key and same canonical payload. Because key, fingerprint, order ID, and final response metadata committed together, the server replays the original outcome. A different payload under that key is a conflict.”

### Interview 3 — `@Transactional` did nothing

**Interviewer:** “A private/helper transaction annotation appears ignored.”

**Candidate:** “I would verify proxy mode, visibility/version rules, selected bean and manager, and whether the call was self-invocation. Default proxy interception applies to calls entering through the proxy, not `this.helper()`. Move the boundary to a collaborator/public service entry or use `TransactionTemplate` when explicit demarcation is clearer.”

### Interview 4 — retries worsen outage

**Interviewer:** “Database p99 rises and every request retries three times.”

**Candidate:** “That multiplies pool/lock load. I classify only transient failures, cap attempts inside the remaining deadline, use exponential backoff+jitter, and enforce admission/bulkheads. Constraint/validation conflicts are not retried. Unknown commits reconcile by idempotency key. I monitor attempt rate separately from logical request rate.”

### Interview 5 — async after transaction

**Interviewer:** “An `@Async` method cannot lazy-load the entity.”

**Candidate:** “The call crossed into another executor/thread after the persistence context/transaction ended; thread-bound context did not follow. Pass an immutable ID/event DTO, start a deliberate transaction in the async worker, and load what it needs. Prefer an outbox for work that must survive process failure.”

### Interview 6 — readiness during database outage

**Interviewer:** “Should liveness fail when MySQL is down?”

**Candidate:** “Usually no: restarting every application instance cannot repair a shared database and creates a cascade. Liveness means this process is irrecoverably broken. Readiness can include a dependency only if removing that instance helps; for a shared outage, keep health semantics and application overload/failure policy explicit.”

### Interview 7 — graceful deployment

**Interviewer:** “Deploy without duplicate or lost work.”

**Candidate:** “Mark unready, stop admission, allow in-flight HTTP requests within a bound, stop consumers/relays with safe checkpoints, and then close pools. Durable idempotency/outbox/inbox state handles a forced kill. I align orchestrator grace with Boot shutdown, downstream timeouts, Kafka poll rules, and lock leases, then test SIGTERM under load.”

## 8. Focused exercises and solution sketches

1. **Draw the filter-to-commit trace.** Mark where authentication, conversion, validation, proxy interception, pool acquisition, flush, commit, and response encoding occur. **Solution:** use the pipeline in Section 1; do not place commit at repository `save`.
2. **Classify ten errors.** Malformed JSON → 400; unsupported media → 415; unauthenticated → 401; forbidden → 403; duplicate idempotency payload → 409; stale `If-Match` → 412; overload → 429/503 with policy; dependency timeout → 503/504 by gateway ownership; unexpected bug → 500; committed response loss → replay on same key.
3. **Repair self-invocation.** Move transactional method to an injected collaborator or make one external service entry own the whole invariant; verify with a real transaction test.
4. **Budget a two-second request.** Reserve time for admission, pool, DB, downstream, response, and at most one classified retry. Reject a retry that cannot complete inside remaining time.
5. **Test shutdown.** Start writes, send SIGTERM, assert readiness drops, completed requests are durable, interrupted callers replay safely, and outbox work resumes once.

## 9. Capstone boundary

Use the dedicated Spring Framework and Spring Boot volumes for container/auto-configuration depth, MySQL/JPA volumes for database mechanics, Kafka volume for consumer/producer delivery, and system-design volume for multi-service workflows. This capstone assesses whether you can connect those contracts in one request without hiding behind annotations.

## Primary references

- Spring Framework, declarative transaction implementation and transaction propagation.
- Spring Framework, Spring MVC `DispatcherServlet`, argument resolution, and HTTP message conversion.
- Spring Boot, graceful shutdown, Actuator, liveness, and readiness.
- RFC 9110 and RFC 9457 for HTTP semantics and Problem Details.
