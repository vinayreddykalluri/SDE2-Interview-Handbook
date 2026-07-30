# Application Events and Decoupled Workflows

An application event communicates that something happened inside one application context. It can decouple the publisher from optional reactions, but it is not automatically durable, asynchronous, remote, or exactly once.

## Publish a plain object event

```java
record OrderPlaced(long orderId, String requestKey) { }

@Service
final class OrderService {
    private final ApplicationEventPublisher events;

    OrderService(ApplicationEventPublisher events) {
        this.events = events;
    }

    void place(long orderId, String requestKey) {
        // validate and persist the order
        events.publishEvent(new OrderPlaced(orderId, requestKey));
    }
}
```

Modern Spring events do not need to extend `ApplicationEvent`.

```java
@Component
final class OrderMetricsListener {
    @EventListener
    void on(OrderPlaced event) {
        // record a bounded in-process metric
    }
}
```

## Default execution model

With the default multicaster, listeners normally execute synchronously in the publisher's thread. A slow listener therefore slows the publisher; an unchecked listener failure can propagate back. The publisher API is a hand-off contract, so a customized multicaster may change execution. Know your configuration.

```text
publisher method
    |
    +-- publishEvent
            |
            +-- listener A (same thread by default)
            +-- listener B (same thread by default)
    |
    v
publisher continues
```

## Events are not commands

A command asks one owner to perform required work. An event reports a fact that occurred. If payment authorization is required for order creation, hiding it in an optional event listener makes the business contract unclear. Use a direct dependency or explicit workflow.

Events fit reactions such as local cache invalidation, metrics, or separately owned follow-up behavior when failure semantics are deliberate.

## Transaction-bound listeners

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
void afterOrderCommit(OrderPlaced event) {
    // react only if the publishing transaction committed
}
```

Phases include before commit, after commit, after rollback, and after completion. Without an active transaction, a transactional listener does not run by default unless fallback execution is enabled.

After-commit does not make a remote action durable. The database commit may succeed and the process may crash before an email or broker publish. For reliable cross-system delivery, write an outbox record in the same database transaction and let a separate relay publish it idempotently.

```text
transaction: order row + outbox row -> COMMIT
                                      |
                                      v
                              relay reads outbox
                                      |
                                      v
                              broker / email API
```

## Ordering and idempotency

Listeners can be ordered, but a large chain of ordered listeners is an implicit workflow. Prefer an explicit orchestrator when later steps depend on earlier results. Make side-effecting listeners idempotent because retries or duplicate external delivery may occur around the process boundary.

## Event design

- Use immutable payloads with stable identifiers.
- Do not publish mutable managed entities for later use.
- Do not put secrets or entire object graphs into events.
- Name past-tense facts: `OrderPlaced`, not `PlaceOrder`.
- Version durable external messages separately from in-process events.

## Common mistakes

- Claiming `publishEvent` is asynchronous.
- Treating in-process events as a message broker.
- Performing mandatory business work in an optional listener.
- Assuming `AFTER_COMMIT` guarantees external delivery.
- Publishing a lazy entity and accessing it after its persistence context closes.
- Creating invisible workflow ordering through many listeners.

## Interview angle

**Interviewer:** Would you send an email in an `@TransactionalEventListener(AFTER_COMMIT)`?

**Strong answer:** It avoids sending before a rolled-back order, but it still has a crash window after commit and before the email call. For best-effort notification it may be acceptable with telemetry. For reliable delivery, I commit an outbox record with the order, relay it asynchronously, and make the consumer idempotent. I also keep network I/O outside the database transaction.

## Quick check

1. Are application events synchronous by default?
2. What is the command-versus-event distinction?
3. When does a transactional listener run without a transaction?
4. Why is after-commit not durable messaging?
5. What belongs in a safe event payload?

## Predict and debug

**Predict:** A default synchronous listener throws an unchecked exception. It can fail the publishing call.

**Debug:** Orders commit but some emails vanish during deployments. Replace the process-memory hand-off with an outbox plus monitored idempotent relay.

## Practice

- **Foundation:** Publish and listen for one immutable event.
- **Foundation:** Prove the default listener and publisher use the same thread.
- **Interview Core:** Classify three interactions as command, local event, or durable message.
- **Interview Core:** Test a listener that should run only after commit.
- **SDE-2 Follow-up:** Design outbox schema, relay lease, idempotency key, retries, and dead-letter handling.

## Readiness checkpoint

Continue when you can state execution, transaction, durability, ordering, and retry semantics for every listener rather than assuming them from the annotation.
