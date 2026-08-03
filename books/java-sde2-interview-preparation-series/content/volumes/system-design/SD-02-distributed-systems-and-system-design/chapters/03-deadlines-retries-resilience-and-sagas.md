# Deadlines, Retries, Circuit Breakers, Backpressure, and Sagas

## Learning objectives

After this chapter, you should be able to:

- propagate one end-to-end deadline through queues, attempts, and downstream calls;
- classify retryable failures and calculate retry amplification;
- distinguish circuit breakers, bulkheads, rate limits, concurrency limits, and timeouts;
- implement token-bucket reasoning and choose a distributed enforcement boundary;
- design backpressure from bounded capacity rather than unbounded queues;
- specify a saga as durable forward and compensating transitions with idempotency; and
- walk through partial failure without confusing timeout, cancellation, rollback, and non-occurrence.

## 1. Deadline is a correctness boundary

A timeout is a local waiting limit. A deadline is an absolute latest useful completion time. If a request arrives with deadline `T_d`, each component computes:

```text
remaining = T_d - monotonicNow
downstream budget <= remaining - localResponseMargin
```

Do not propagate a wall-clock instant between machines without accounting for clock behavior/protocol. Many RPC frameworks carry a relative/absolute deadline under documented semantics. Inside Java, use monotonic elapsed-time sources such as `System.nanoTime()` for durations rather than subtracting wall-clock timestamps.

### Budget example

An API has 800 ms server budget:

```text
admission/queue:       50 ms
local validation:     20 ms
inventory call:      250 ms max
payment call:        300 ms max
DB commit/outbox:    100 ms
serialization/margin:80 ms
```

These are caps, not a reason to wait each maximum sequentially. Before each call, use remaining budget. If queueing consumed 400 ms, starting a 300 ms payment plus commit may be pointless. Reject/abort before performing a side effect that cannot return usefully.

Cancellation is cooperative and races with completion. A client timeout does not prove downstream work stopped. Every side-effecting command needs identity/status reconciliation. Interrupting a Java thread does not roll back remote operations; code must preserve interruption policy and close/abort only through supported APIs.

## 2. Retries

### Retry decision rule

Retry only when all are true:

1. failure is plausibly transient and classified from a stable signal;
2. another attempt can succeed within the remaining deadline;
3. operation is idempotent or protected by an idempotency key/deduplication;
4. retry load will not violate a budget or worsen overload;
5. attempts and elapsed time are bounded;
6. metrics expose attempts and terminal outcome.

Good candidates: connection reset before a request is accepted, selected leader-change/unavailable signals, classified serialization/deadlock victim for a local transaction. Bad candidates: validation, permission denial, invariant conflict needing user input, deterministic serialization bug, arbitrary `500` for a non-idempotent command.

### Exponential backoff with jitter

One full-jitter form:

```text
cap_i = min(maxBackoff, base * 2^i)
sleep_i = random(0, cap_i)
```

Jitter prevents synchronized clients from retrying in lockstep. Respect server retry guidance if safe, but bound it by the caller deadline. Avoid overflow when calculating powers.

### Amplification

If each layer performs up to three attempts across gateway, service, and client, one user action can cause `3 * 3 * 3 = 27` downstream attempts. Centralize retry ownership where possible. Define a retry budget as a fraction/count of normal traffic, not only attempts per request.

### Failure walkthrough

Payment provider receives command and charges, but response is lost. A blind retry with a new identity charges again. Correct protocol sends stable merchant operation ID; provider returns/reconciles the previous outcome. The order workflow stores “payment unknown/pending reconciliation” rather than assuming timeout means failure. Compensation/refund is a separate idempotent business action, not network rollback.

## 3. Circuit breakers

A circuit breaker is a state machine protecting callers/dependency from repeated work that is unlikely to succeed:

```text
CLOSED --failure threshold/window--> OPEN
OPEN --cooldown--> HALF_OPEN
HALF_OPEN --limited successes--> CLOSED
HALF_OPEN --failure--> OPEN
```

The contract needs:

- which outcomes count as failures (not client validation);
- sliding window/minimum sample size;
- failure/slow-call threshold;
- open duration and half-open probe concurrency;
- fallback/rejection response;
- metrics and manual/automatic recovery;
- scope: per instance, dependency, endpoint, tenant, or region.

Per-instance breakers can disagree; that may be acceptable because each protects local resources. A globally synchronized breaker can become a control-plane dependency. Do not use a breaker for a healthy-but-saturated service when admission/rate control is the needed signal.

Fallback must be semantically safe. Returning stale catalog data may be acceptable. Returning a guessed authorization decision or stale bank balance is not. Silent empty lists turn outages into data loss signals.

## 4. Bulkheads and capacity isolation

A bulkhead partitions scarce capacity:

- separate connection pools for critical and batch paths;
- separate executors/concurrency semaphores by dependency;
- tenant quotas;
- workload queues with weighted admission;
- dedicated Kafka consumer groups/resources.

Without isolation, a slow recommendation provider can consume every request thread/connection and take down order status. A semaphore limit around the dependency bounds in-flight calls; callers that cannot acquire within the deadline fail fast or degrade.

Bulkheads reduce maximum utilization and require capacity allocation. Too many tiny pools strand resources. Review with measured demand and priority policy.

## 5. Rate and concurrency limiting

### Token bucket

A token bucket has capacity `B` and refill rate `r` tokens/second. At elapsed time `Δt`:

```text
tokens = min(B, tokens + r * Δt)
admit cost c iff tokens >= c; then tokens -= c
```

It permits bursts up to capacity while limiting long-term average. Use monotonic elapsed time. Decide rounding/fractional tokens, maximum idle accumulation, and weighted request cost.

Other policies:

- fixed window: simple but boundary bursts;
- sliding window log/counter: smoother, more state/cost;
- leaky bucket: controls departure rate/queueing;
- concurrency limit: caps in-flight work, often better when latency varies;
- adaptive concurrency: adjusts from measured latency/queueing under careful stability controls.

### Distributed limit placement

- edge/gateway: protects downstream early; needs authenticated identity and failure policy;
- per-instance: cheap but total scales with instances and load balance;
- centralized Redis/service: coordinated limit but adds latency/dependency/hot keys;
- hierarchical: local leases/budgets allocated from global policy, trading precision for scale.

Define behavior when limiter storage is unavailable: fail open risks overload/abuse; fail closed risks outage. Choose by endpoint (login/payment versus low-risk read), use emergency local limits, and observe.

Return `429` with useful retry semantics only when the caller can retry safely. Internal callers need backpressure signals, not a synchronized tight retry loop.

## 6. Backpressure and bounded queues

Backpressure prevents producers from creating more in-flight work than consumers can finish. It can be pull-based demand, bounded blocking, rejection, pause/resume, load shedding, or rate feedback.

An unbounded queue preserves acceptance by spending memory and deadline. At arrival `λ > service μ`, backlog grows at `λ - μ`. Once queued wait exceeds the request deadline, doing work wastes capacity and increases recovery time.

Design each queue with:

- capacity in items and bytes;
- priority/fairness and tenant quotas;
- maximum queue age/deadline expiration;
- rejection/drop policy;
- producer feedback;
- retry ownership;
- drain/shutdown behavior;
- depth, age, throughput, rejection, and saturation metrics.

Kafka lag is a durable backlog, but retention is finite and downstream recovery throughput must exceed arrivals. Simply adding consumers helps only until partition count or downstream capacity binds. Pause partitions when a dependency is saturated, continue polling under supported patterns, and avoid exceeding consumer liveness rules.

## 7. Sagas

### Model

A saga is a sequence of local transactions connected by durable commands/events. If a later step fails, compensating actions attempt to semantically reverse or offset earlier committed effects:

```text
T1 ReserveInventory -> T2 AuthorizePayment -> T3 ConfirmOrder
        C1 ReleaseInventory <- C2 Void/RefundPayment
```

Compensation is not rollback:

- inventory may have been unavailable to others temporarily;
- an authorization may require void, a captured payment refund;
- refund can fail and require reconciliation;
- notifications cannot be “unsent”;
- policy may choose manual intervention rather than compensation.

Every transition and compensation needs a stable command/event ID, idempotent handling, allowed-source-state predicate, timeout/retry, and audit. Store saga state durably with version/sequence.

### Orchestration versus choreography

**Orchestration:** a coordinator sends commands and records transitions. Pros: visible workflow, timers, centralized policy. Cons: coordinator coupling/availability and possible “god service.”

**Choreography:** services react to events. Pros: decoupled local evolution. Cons: implicit global flow, cycles, difficult status/timeouts, event sprawl. Complex business workflows usually benefit from an explicit durable process manager even if communication is event-based.

### Order saga walkthrough

States:

```text
PENDING -> INVENTORY_RESERVED -> PAYMENT_AUTHORIZED -> CONFIRMED
       \-> REJECTED
INVENTORY_RESERVED -> COMPENSATING -> RELEASED/FAILED_RECONCILIATION
PAYMENT_AUTHORIZED -> COMPENSATING -> VOIDED/REFUND_PENDING
```

1. Create order and outbox `ReserveInventory` command atomically.
2. Inventory consumes idempotently and emits reserved/rejected with source command ID.
3. Coordinator accepts only expected order version/state; duplicates no-op.
4. On reserved, append `AuthorizePayment` with stable operation ID.
5. Provider timeout yields `PAYMENT_UNKNOWN`, not immediate failure; query/reconcile using operation ID.
6. On terminal payment failure, command inventory release.
7. Repeated release is idempotent; reservation has an expiry as a safety net.
8. Operators see sagas stuck beyond age SLO and can safely replay/repair.

Concurrent cancel request is another transition. Use optimistic version and define whether cancellation before/after capture triggers release/refund.

## 8. Java/Spring implementation boundaries

Dependency-requiring resilience libraries can implement mechanics, but configuration needs domain semantics:

```java
@Service
final class PricingGateway {
    private final HttpClient client;
    private final Bulkhead bulkhead;
    private final CircuitBreaker breaker;

    Price quote(Command command, Deadline deadline) {
        return breaker.executeSupplier(() ->
            bulkhead.executeSupplier(() ->
                client.quote(command, deadline.remaining())));
    }
}
```

Decorator order matters. If retry occurs inside a bulkhead permit, one request can hold capacity through backoff; if outside, each attempt reacquires. Metrics should distinguish logical request from attempt. Time limiter cancellation may not interrupt underlying blocking I/O. Test failure windows on the real client.

Java 21 virtual threads make blocking code cheaper in thread terms but do not remove deadlines, idempotency, connection limits, queues, or downstream saturation. Structured concurrency APIs have differed across later JDK releases; this book does not assume preview APIs.

## 9. Interview questions and model checkpoints

### Q1. Timeout versus deadline?

**Model checkpoint:** timeout limits one wait; deadline bounds the whole operation. Recompute remaining budget and leave commit/response margin. Timeout does not prove non-execution.

### Q2. Circuit breaker versus rate limiter?

**Model checkpoint:** breaker rejects because recent evidence predicts dependency failure; limiter enforces allowed demand/capacity regardless of health. Bulkhead isolates resources; timeout bounds waiting.

### Q3. How do you retry safely?

**Model checkpoint:** classify transient failure, stable operation identity/idempotency, bounded attempts/elapsed budget, exponential backoff+jitter, capacity-aware policy, metrics, and reconciliation for unknown outcome.

### Q4. Is saga compensation guaranteed?

**Model checkpoint:** compensation is another fallible local transaction. Retry idempotently, persist progress, reconcile/manual repair, and model irreversible effects.

### SDE-2 follow-ups

1. Compute worst-case downstream attempts for three retrying layers and redesign ownership.
2. Design rate limits for global, tenant, user, and expensive-route constraints without a hot central key.
3. A breaker opens due to client `400`s. Fix classification and describe test evidence.
4. Handle simultaneous payment callback, timeout reconciliation, and user cancellation in one saga state machine.

## 10. Exercises

1. Allocate an end-to-end 1-second deadline across two parallel and one sequential dependency, including queue margin.
2. Implement a Java 21 monotonic token bucket and assert burst/refill behavior.
3. Design a bounded executor policy for critical and batch tasks; calculate memory and queue-age limits.
4. Draw an order saga with every command ID, local transaction, timeout, duplicate, compensation, and terminal/manual state.
5. Write a chaos matrix for slow dependency, reset, lost response, partial region, retry storm, and recovery.

## 11. Summary checklist

- [ ] One end-to-end deadline controls queueing, attempts, and commits.
- [ ] Retry classification, identity, attempts, elapsed time, backoff, and amplification are explicit.
- [ ] Breakers, bulkheads, limits, and timeouts have distinct purposes.
- [ ] Every queue is bounded by capacity, age, and rejection policy.
- [ ] Rate limiting has a scope and unavailable-store policy.
- [ ] Side-effect timeout produces unknown/reconciliation state where necessary.
- [ ] Saga states, commands, compensations, duplicates, and manual recovery are durable.
- [ ] Framework decorators are tested in their actual order and execution model.

## Primary references

- RFC 9110, HTTP method/idempotency and status semantics: <https://www.rfc-editor.org/rfc/rfc9110>
- Reactive Streams specification (backpressure contract): <https://www.reactive-streams.org/>
- Java SE 21 API: <https://docs.oracle.com/en/java/javase/21/docs/api/>
- Spring Framework Reference, resilience-related integration boundaries: <https://docs.spring.io/spring-framework/reference/>

> **Version boundary:** circuit-breaker libraries, HTTP clients, Spring integrations, virtual-thread support, and structured-concurrency APIs vary. The patterns specify behavior independent of a library. All complete companion code targets final Java 21 APIs and avoids later-JDK preview features.
