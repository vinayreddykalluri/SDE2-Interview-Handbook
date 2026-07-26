# Idempotent APIs, Operational Boundaries, and Spring Testing

## Learning objectives

After this chapter, you should be able to:

- distinguish naturally idempotent state replacement from an idempotency-key protocol for non-idempotent commands;
- design an atomic request-deduplication record with payload binding and replay semantics;
- reason about deadlines, cancellation, retries, duplicate execution, and overload as one reliability problem;
- expose Spring Boot Actuator information without turning management endpoints into a data leak or control-plane vulnerability;
- choose unit, MVC slice, persistence slice, container, and end-to-end tests from the claim under test; and
- build a production-readiness checklist that includes shutdown, probes, configuration, metrics, and security.

## 1. Idempotency is a server-side state machine

### Intuition and formal contract

Networks can lose a response after the server commits. A client sees a timeout but cannot infer whether execution occurred. Retrying a non-idempotent `POST` can therefore duplicate the effect.

An idempotency-key protocol binds a client-generated operation identity to one logical request. For principal/tenant `T`, endpoint operation `O`, key `K`, and canonical payload fingerprint `H`, the server maintains a record:

```text
(T, O, K) -> {fingerprint H, state, outcome reference, expiry}
state in {IN_PROGRESS, SUCCEEDED, FAILED_RETRYABLE?, FAILED_FINAL}
```

A useful contract is:

1. the first accepted `(T,O,K,H)` reserves the key atomically;
2. the business result and deduplication outcome become durable in one appropriate transaction, or a recoverable state machine connects them;
3. the same key with a different fingerprint is rejected;
4. the same key and fingerprint replays the documented prior outcome or directs the client to its status;
5. concurrent duplicates do not both execute the effect;
6. retention is longer than the documented client retry window;
7. authorization is evaluated for every request, including replay.

The key is scoped. A global key without tenant and operation context can collide or leak another caller's result. A payload fingerprint must use a documented canonicalization; hashing raw JSON makes whitespace or property order significant, while careless normalization can merge meaningfully different requests.

### Recognition and decision rule

Use an idempotency key when clients may retry a command that creates a new server-chosen identity or produces a costly/non-repeatable side effect. Prefer a naturally idempotent resource design when possible: `PUT /imports/{clientGeneratedId}` can make the target identity explicit. Do not add idempotency storage to a pure `GET` merely because every API should “have idempotency.”

### Concrete Spring/JDBC sketch — dependency-requiring

```java
@Transactional
public CreateOrderResult create(
        TenantId tenant,
        String key,
        CreateOrderCommand command) {
    String fingerprint = canonicalFingerprint(command);
    IdempotencyRecord record = idempotency.reserveOrRead(
            tenant, "create-order", key, fingerprint);

    if (record.completed()) {
        return orders.resultFor(record.resourceId());
    }
    if (!record.ownedByThisAttempt()) {
        throw new OperationInProgress(record.retryAfter());
    }

    Order order = Order.create(command);
    orders.insert(order);
    outbox.append(OrderCreated.from(order));
    idempotency.complete(record.id(), order.id());
    return CreateOrderResult.created(order);
}
```

The repository needs a unique constraint on the scoped key. `reserveOrRead` must handle the insert race. The database transaction connects the order, outbox event, and completed key. If external authorization must happen first, do it without holding locks. If payment must occur remotely, the workflow needs a recoverable saga/state machine; a local transaction cannot include the provider.

### Failure walkthroughs

**Response lost after commit:** the retry finds `SUCCEEDED` and returns the same logical result. This is the central success case.

**Two requests arrive concurrently:** a unique constraint/atomic insert gives one ownership; the other reads `IN_PROGRESS` and waits, polls, or receives a documented conflict/retry response. A “check then insert” without a unique constraint races.

**Same key, changed body:** return a client error. Reusing the previous result would associate the wrong intent with the key.

**Process dies after reservation but before business commit:** if reservation and work share one transaction, both roll back. If they cannot, store a lease/attempt token and define recovery. Never leave permanent `IN_PROGRESS` records with no reclamation policy.

**Retention expires too soon:** a late retry becomes a new operation. The API must document its deduplication window; longer retention has storage and privacy costs.

## 2. Deadlines, retries, and overload

### One end-to-end budget

A timeout at each hop is not a coherent deadline. If an inbound request has budget `D`, downstream work must receive a smaller remaining budget after local queueing and processing:

```text
remaining = requestDeadline - now
downstreamTimeout <= remaining - responseAndSafetyMargin
```

Retries consume time and amplify load. For attempt cost `t`, backoffs `b_i`, and `n` attempts, worst-case elapsed time approximates `sum(t_i) + sum(b_i)`. Retry only transient failures, only when the operation is idempotent or protected by a deduplication contract, and only within a bounded attempt and elapsed-time budget. Add jitter to reduce synchronized retries.

Cancellation is cooperative. A client disconnect or timeout does not prove database, broker, or remote work stopped. Propagate interruption/deadline where supported, but design duplicate safety because cancellation races with completion.

### Capacity controls

- **rate limit:** bounds admitted work over time for a caller or class;
- **concurrency limit:** bounds simultaneous in-flight work;
- **queue bound:** limits waiting work and memory;
- **bulkhead:** partitions capacity so one dependency/tenant cannot consume all of it;
- **circuit breaker:** temporarily rejects calls when evidence suggests the dependency cannot succeed;
- **backpressure:** communicates or enforces that producers must slow down.

Reject early when the system cannot meet the deadline. An unbounded servlet executor queue converts overload into high latency and heap pressure. Virtual threads can reduce thread-per-request cost for blocking code but do not expand database connections, remote capacity, memory, or CPU.

## 3. Actuator and production-safe observability

### Management endpoints are an API

Spring Boot Actuator can expose health, metrics, loggers, environment, mappings, thread information, and other management capabilities depending on dependencies and configuration. Treat every exposed endpoint as a privileged operational API. Use an explicit exposure allow-list, separate network/security boundary when appropriate, authentication/authorization, TLS, and data redaction. Never assume an endpoint is safe because it is “internal.”

Useful distinctions:

- **liveness:** should the orchestrator restart this process? It should not fail merely because an optional dependency is down, or every instance may restart together.
- **readiness:** should this instance receive new traffic? It can reflect inability to serve required work.
- **startup:** has initialization completed within its allowed window?
- **deep diagnostic health:** detailed dependency evidence for operators; not necessarily a public traffic probe.

Probe behavior must match deployment. A readiness check that runs a costly query every second can become load. A cached check can hide fast failure. A database-down response may correctly make an instance unready, but if every instance does that while work could degrade, the policy creates total outage.

### Metrics and cardinality

Prefer bounded labels such as route templates, method, outcome, and dependency name. Never use raw URL, user ID, order ID, exception message, or idempotency key as a metric tag. High cardinality consumes memory and makes backends expensive. Put per-request identity in structured logs or traces with controlled sampling and retention.

Instrument the use-case and dependency boundaries:

```text
request rate, error rate, duration distribution
in-flight requests and admission rejections
connection-pool active/idle/wait/timeout
outbox backlog age and count
idempotency conflicts and replay count
downstream latency/errors/timeouts
JVM allocation, GC, CPU, and thread/virtual-thread evidence
```

Histograms support percentiles and SLO analysis; averages hide tail latency. Define which clock and units are used. A trace is not a replacement for metrics: tracing explains selected executions, while metrics reveal aggregate change.

## 4. Graceful startup and shutdown

Readiness should become false before shutdown stops accepting work. Allow in-flight requests a bounded drain period. Stop message consumption, finish or abandon work under the consumer protocol, flush recoverable state, and close resources. Shutdown hooks have a deadline imposed by the platform; a design that requires infinite cleanup is not graceful.

Background executors must have explicit ownership. Avoid fire-and-forget `CompletableFuture` work on an unspecified common pool. Name the executor, bound or control admission, propagate only required context, observe failures, and stop it. Request-scoped state stored in thread locals does not automatically follow asynchronous work and can leak if not cleared.

## 5. Testing by claim

### Test pyramid as an evidence matrix

| Claim | Smallest credible test | What it cannot prove |
|---|---|---|
| domain transition rejects invalid status | plain JUnit test, real domain object | MVC mapping, transaction, database behavior |
| controller maps DTO and status correctly | MVC slice with security behavior explicit | production database or full auto-configuration |
| JPA mapping/query works | persistence slice or focused integration test against target engine | HTTP contract and full transaction orchestration |
| custom configuration conditions select a bean | application-context runner/narrow context | complete production startup |
| rollback and unique constraint protect duplicates | integration test with real transaction manager and target DB | deployment routing and proxy behavior outside test config |
| authorization denies cross-tenant access | security-enabled MVC/integration test | network gateway policy |
| entire critical path works | end-to-end application test | exhaustive business combinations |

“Slice” tests intentionally include only part of the application. A missing bean may be correct for the slice rather than an application bug. State the slice's boundary. Avoid `@MockBean`-style replacement for every collaborator until the test merely verifies a mock script.

### MVC contract test — dependency-requiring

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired MockMvc mvc;
    @MockBean CreateOrderUseCase useCase;

    @Test
    void rejectsMissingIdempotencyKey() throws Exception {
        mvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"customerId":"C-1","lines":[{"sku":"S-1","quantity":1}]}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_PROBLEM_JSON));
    }
}
```

This test proves mapping under the selected slice configuration. It does not prove the unique key or transaction behavior. For that, send two concurrent requests or call the real service against the real database engine, then assert exactly one order and one outbox record.

### Integration-test discipline

- isolate state with transaction rollback only when rollback matches production behavior under test;
- do not wrap a test in a transaction when you need to observe after-commit behavior;
- control time through an injected `Clock`;
- use deterministic IDs when identity itself is not under test;
- test the target database for locking, isolation, SQL, and query plans;
- assert persisted outcomes, not only returned values;
- run concurrent tests repeatedly with barriers to create the race window;
- keep external service stubs protocol-faithful, including timeouts and duplicate delivery;
- record why each full-context test needs the full context.

## 6. Interview questions and model checkpoints

### Q1. How do idempotency keys differ from optimistic locking?

**Model checkpoint:** an idempotency key deduplicates repeated command intent; optimistic locking detects that a resource changed since a version was observed. A create command may need the former, while an update may use `If-Match` plus a version predicate. They solve different races and can coexist.

### Q2. What should a readiness check verify?

**Model checkpoint:** only conditions required to admit new traffic under the deployment's service policy, cheaply and without causing failure amplification. Liveness asks whether restart can help; detailed diagnostics belong behind an operator boundary.

### Q3. When is a full Spring context test justified?

**Model checkpoint:** when the claim depends on real auto-configuration, proxy advice, security chain, serialization configuration, or cross-layer wiring. Pure decisions should remain plain unit tests.

### Q4. Why can retries worsen an outage?

**Model checkpoint:** each failed request creates more attempts, increasing queueing and dependency load. Retry budgets, backoff with jitter, admission controls, and circuit breaking limit amplification; idempotency protects correctness but not capacity.

### SDE-2 follow-ups

1. An idempotency request is stuck `IN_PROGRESS` after a host crash. Design lease ownership and recovery without executing twice.
2. An Actuator metrics endpoint causes memory growth. Identify label cardinality and observation leaks.
3. A controller test passes while production rejects every request with `403`. Enumerate security configuration differences the slice may have hidden.
4. Design a graceful shutdown protocol for an HTTP service that also consumes messages and owns an outbox relay.

## 7. Exercises

1. Specify the table schema and state transitions for a 24-hour idempotency window. Include tenant scope, fingerprint mismatch, crash recovery, and data erasure.
2. Create a deadline budget for a 750 ms API with two downstream calls and at most one retry. State the rejection policy.
3. Classify ten Actuator endpoints into exposed, operator-only, or disabled for an internet-facing service, and justify each.
4. Write a test plan that proves two concurrent identical create requests produce one logical order.
5. Diagnose why a readiness probe tied to a third-party analytics API caused every instance to leave service.

## 8. Production-readiness checklist

- [ ] Idempotency keys are scoped, payload-bound, atomic, retained for a documented window, and authorized on replay.
- [ ] Deadlines and retries share one bounded budget.
- [ ] Queue, rate, and concurrency limits protect every scarce dependency.
- [ ] Management endpoints use an explicit exposure and security policy.
- [ ] Health groups reflect orchestration semantics, not a list of every dependency.
- [ ] Metric dimensions are bounded; logs and traces are redacted.
- [ ] Background tasks have named ownership, failure observation, and shutdown.
- [ ] Each test uses the smallest environment that can prove its claim.
- [ ] Database and concurrency guarantees are tested against production-relevant infrastructure.

## 9. Operational security drill

Assume the public gateway blocks `/actuator/**`, but a misconfigured alternate ingress reaches the application port directly. “The gateway protects it” is not defense in depth. The application exposure allow-list should still omit dangerous endpoints, management access should have its own authenticated network/policy boundary, and deployment tests should scan every reachable service/port.

Test from three perspectives:

1. public internet identity cannot reach management routes or infer environment details;
2. operator identity reaches only the endpoints required by role;
3. orchestrator probes reach a minimal health path without receiving component secrets;
4. application logs do not include property values, token headers, heap snippets, or request bodies from failed management authentication;
5. emergency changes to log level or shutdown behavior are authenticated, authorized, audited, rate-limited, and disabled when unnecessary.

Health responses should reveal only what the caller needs. A public `503` can say temporarily unavailable; it need not name the database host. An operator endpoint can expose component status under policy, but secrets and personal data remain redacted.

Then simulate management-plane slowness. Scraping a metric endpoint must not exhaust request threads or lock the application. Expensive diagnostics such as heap/thread information are operator actions, not high-frequency probes. Monitor telemetry export/drop behavior so an observability outage does not become an application outage or a false “all healthy” signal.

**Checkpoint:** list the network path, Spring Security chain, Actuator exposure list, role policy, payload redaction, audit record, and resource limit. Security exists at every reachable path, not only the diagram's preferred gateway.

## Primary references

- Spring Boot Reference, “Production-ready Features”: <https://docs.spring.io/spring-boot/reference/actuator/>
- Spring Boot Reference, “Endpoints”: <https://docs.spring.io/spring-boot/reference/actuator/endpoints.html>
- Spring Framework Reference, “Testing”: <https://docs.spring.io/spring-framework/reference/testing.html>
- Spring Boot Reference, “Testing”: <https://docs.spring.io/spring-boot/reference/testing/>
- RFC 9110, “HTTP Semantics”: <https://www.rfc-editor.org/rfc/rfc9110>
- RFC 9457, “Problem Details for HTTP APIs”: <https://www.rfc-editor.org/rfc/rfc9457>

> **Version boundary:** exact Actuator endpoints, exposure defaults, health-group options, test annotations, replacement-test-bean APIs, and graceful-shutdown settings vary by Boot release line. The design rules above do not assume an undocumented default. Check the BOM-matched reference. Java examples target Java 21.
