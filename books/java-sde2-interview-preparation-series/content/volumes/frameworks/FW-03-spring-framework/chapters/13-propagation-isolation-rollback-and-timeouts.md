# Propagation, Isolation, Rollback, and Timeouts

Transaction attributes are not decoration. They change resource participation and failure behavior. Begin with `REQUIRED`, then use another mode only for a named reason.

## Propagation mental model

| Propagation | Existing transaction | No transaction | Primary use/caution |
|---|---|---|---|
| `REQUIRED` | join | create | normal application use case |
| `REQUIRES_NEW` | suspend, create independent | create | independent durable unit; needs another resource/connection |
| `SUPPORTS` | join | run without | operations valid either way; semantics vary by caller |
| `MANDATORY` | join | fail | require caller-owned boundary |
| `NOT_SUPPORTED` | suspend, run without | run without | explicitly non-transactional work |
| `NEVER` | fail | run without | enforce absence |
| `NESTED` | savepoint within physical transaction when supported | create | partial rollback through savepoint; manager/resource dependent |

## `REQUIRED` and rollback-only surprise

```java
@Transactional
public void placeOrder(...) {
    paymentLedger.record(...); // REQUIRED
    // caller catches an inner failure and continues
}
```

Joined scopes share one physical transaction. If the inner scope marks it rollback-only and the outer scope still attempts commit, Spring throws `UnexpectedRollbackException`. This is deliberate: the caller must not be told that commit succeeded.

## `REQUIRES_NEW` is independent, not free

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void writeAudit(...) { ... }
```

The outer resource is suspended while a new transaction obtains its own resources. Under concurrency, each thread may hold an outer connection while requesting another, exhausting a small pool. The inner commit remains even if the outer transaction later rolls back. That may be correct for audit evidence or incorrect for business state.

Also remember the proxy boundary: a same-class call to this method does not activate new propagation in proxy mode.

## `NESTED` versus `REQUIRES_NEW`

`NESTED` typically uses a database savepoint inside one physical transaction. Rolling back to the savepoint does not commit the inner work independently; the outer final rollback still removes everything. Support depends on transaction manager and resource capabilities. `REQUIRES_NEW` uses an independent physical transaction.

## Isolation belongs to the database contract

Spring exposes isolation choices such as read uncommitted, read committed, repeatable read, and serializable. Their exact phenomena, locking, snapshots, and defaults come from the database and access path. Review **FW-01 MySQL** for InnoDB details.

Do not set `SERIALIZABLE` as a reflex. First name the anomaly or invariant, then compare:

- database constraint;
- atomic conditional update;
- optimistic version;
- explicit lock;
- higher isolation;
- redesigned workflow.

## `readOnly`

`@Transactional(readOnly = true)` is a hint and optimization contract whose effect depends on the manager, provider, driver, and database. It is not a security boundary and must not be your only write prevention. Tests should verify critical behavior rather than assuming universal enforcement.

## Timeout

A transaction timeout bounds transaction execution according to manager/resource support. It is not a complete end-to-end request deadline. Also configure database statement/lock timeouts, client timeouts, queue time, and cancellation semantics. A request that times out in a caller may still be executing unless cancellation propagates.

## Rollback rules and exception translation

Use type-safe rollback rules where possible. Pattern-based class-name rules can match more broadly than intended. Translate low-level exceptions at a stable boundary while preserving category and cause:

```java
try {
    repository.insert(order);
} catch (DuplicateKeyException duplicate) {
    throw new DuplicateRequestKey(command.requestKey(), duplicate);
}
```

The translated exception should still trigger the intended rollback.

## Manager selection

With multiple `PlatformTransactionManager` beans, choose explicitly:

```java
@Transactional(transactionManager = "ordersTransactionManager")
public void updateOrder(...) { ... }
```

This does not create atomicity with a second manager. Distributed transaction support is a separate architecture decision with significant operational cost; many services use local transactions plus outbox, idempotency, and compensation.

## Common mistakes

- Using `REQUIRES_NEW` to make errors disappear.
- Confusing nested savepoint with independent commit.
- Ignoring connection-pool demand from suspended outer transactions.
- Treating `readOnly` as authorization.
- Assuming timeout covers remote calls and queueing end to end.
- Selecting propagation on a self-invoked method.

## Interview angle

**Interviewer:** Explain `REQUIRED` versus `REQUIRES_NEW`.

**Strong answer:** `REQUIRED` joins the caller's transaction or creates one, so all participants share commit and rollback. `REQUIRES_NEW` suspends the outer transaction and commits independently with separate resource demand. I use it only when independent durability is a business requirement, account for pool capacity and lock interactions, and ensure the invocation crosses a proxy.

## Quick check

1. Why can `UnexpectedRollbackException` be correct?
2. What resource risk does `REQUIRES_NEW` add?
3. How does `NESTED` differ from an independent transaction?
4. Is `readOnly` a write-security guarantee?
5. Why is isolation database-specific?

## Predict and debug

**Predict:** Inner `REQUIRED` marks rollback-only; outer catches and returns. The final commit attempt produces `UnexpectedRollbackException` rather than a false success.

**Debug:** Audit traffic exhausts the pool after adding `REQUIRES_NEW`. Measure active/awaiting connections, outer hold time, concurrency, and pool/database capacity; reconsider whether independent audit writes belong in an outbox.

## Practice

- **Foundation:** Trace `REQUIRED` with and without an existing transaction.
- **Foundation:** Contrast new transaction and savepoint on a timeline.
- **Interview Core:** Explain an unexpected rollback from inner failure.
- **Interview Core:** Choose an isolation/invariant strategy for last-item inventory.
- **SDE-2 Follow-up:** Capacity-plan outer plus `REQUIRES_NEW` connection demand and design a safer alternative.

## Readiness checkpoint

Continue when you can trace physical transaction, logical scope, resource count, rollback-only state, and final durable outcome for nested service calls.
