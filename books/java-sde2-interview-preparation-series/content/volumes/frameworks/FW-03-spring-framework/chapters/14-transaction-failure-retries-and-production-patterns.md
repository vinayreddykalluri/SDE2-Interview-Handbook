# Transaction Failure, Retries, and Production Patterns

Production transaction design begins where the happy-path annotation ends: deadlocks, lock timeouts, duplicate requests, uncertain remote outcomes, and partial delivery.

## A failed transaction is finished

After a database exception marks a transaction rollback-only, do not catch it and continue issuing work as if the transaction were healthy. Roll back, release state, classify the failure, and decide whether the **whole idempotent operation** can run in a fresh transaction.

```text
attempt 1: BEGIN -> read -> write -> deadlock -> ROLLBACK
                     wait with bounded jitter
attempt 2: BEGIN -> reread current state -> write -> COMMIT
```

Retrying one statement inside the same failed transaction is usually incorrect.

## Classify before retrying

| Failure | Retry automatically? | Required reasoning |
|---|---|---|
| deadlock victim | often, bounded | whole transaction idempotent; fresh state |
| transient lock timeout | sometimes | contention and latency budget |
| connection interruption | uncertain | commit outcome may be unknown |
| unique constraint | usually no | may represent idempotent replay or business conflict |
| validation/authorization | no | deterministic caller error |
| optimistic conflict | maybe | command still semantically valid after reload |
| serialization failure | often bounded | transaction replay is safe |

The exception class alone is insufficient. Driver/database translation, SQL state, operation semantics, and whether commit acknowledgement was received all matter.

## Retry and transaction advice ordering

The intended shape is usually:

```text
retry advice
   |
   +-- attempt 1 transaction -> rollback
   +-- wait
   +-- attempt 2 transaction -> commit
```

If retry runs inside one transaction, later attempts may reuse a rollback-only context. Make ordering explicit and test the number of begins/rollbacks/commits.

## Idempotency key pattern

```text
request key: customer-42/place-order/abc123
        |
        v
UNIQUE constraint on request key
        |
        +-- first request inserts result
        +-- replay reads and returns the same result
        +-- same key + different payload -> conflict
```

Store a payload fingerprint and stable outcome where needed. A plain "duplicate means success" rule can return the wrong result when the same key is reused for different input.

## Remote call uncertainty

Three outcomes exist after a payment timeout:

1. Provider never received the request.
2. Provider completed it but response was lost.
3. Provider is still processing.

Blind retry can charge twice. Use provider-supported idempotency keys, durable local attempt state, status lookup/reconciliation, and an explicit state machine.

```text
CREATED -> PAYMENT_PENDING -> PAID
                    |
                    +-> UNKNOWN -> reconcile -> PAID or FAILED
```

## Outbox pattern

Write domain state and an outbox message in one local transaction. A relay claims unpublished rows, publishes with a stable message ID, and marks them sent. The relay can crash after publish but before marking sent, so consumers must be idempotent.

Key design points:

- immutable event payload and schema version;
- aggregate/order key when ordering matters;
- claim/lease that recovers after crash;
- bounded retries and poison-message policy;
- backlog age, attempt count, and publish latency metrics;
- retention and personally identifiable information controls.

## Transaction observability

Measure:

- active, committed, rolled-back, timed-out transaction counts;
- duration and connection acquisition time;
- lock/deadlock/serialization failure category;
- retry attempts and exhausted retries;
- outbox backlog age and relay failures;
- operation name without high-cardinality raw IDs.

A stack trace without transaction name, manager, SQL state/category, attempt number, and resource timing is weak evidence.

## Incident playbook: rollback spike

1. Contain overload or disable the offending workflow safely.
2. Separate acquisition, execution, lock wait, and commit latency.
3. Group failures by operation and translated exception category.
4. Inspect recent deployment, schema/index, traffic, and query-plan changes.
5. Determine whether retries amplify load.
6. Correct the invariant/query/boundary, canary, and watch rollback plus success latency.
7. Add a regression test and operational guardrail.

## Common mistakes

- Retrying every `DataAccessException`.
- Retrying inside the same failed transaction.
- Retrying a non-idempotent remote side effect.
- Treating unique violation as universally transient or fatal.
- Assuming timeout means the remote operation failed.
- Marking outbox sent before publish succeeds.

## Interview angle

**Interviewer:** How do you retry a deadlocked transaction?

**Strong answer:** I let the attempt roll back completely, classify the database exception, verify the command and external effects are idempotent, wait with bounded jitter, then rerun the entire unit in a fresh transaction against current state. I cap attempts and latency, expose metrics, and fix persistent contention or access-order issues instead of treating retries as the solution.

## Quick check

1. Why must a retry use a fresh transaction?
2. What makes a command idempotent?
3. Why is an HTTP timeout an uncertain outcome?
4. Can an outbox deliver exactly once by itself?
5. Which metrics reveal retry amplification?

## Predict and debug

**Predict:** Relay publishes and crashes before setting `sent_at`. It republishes later. The consumer needs a message ID and idempotent effect.

**Debug:** Deadlock retries raise database load and p99. Inspect retry rate, contention keys, lock order, query plans, transaction length, and synchronized retry bursts; cap attempts and fix the underlying conflict.

## Practice

- **Foundation:** Classify five failures as deterministic, transient, conflict, or uncertain.
- **Foundation:** Draw a fresh-transaction retry timeline.
- **Interview Core:** Design an idempotency record with payload fingerprint.
- **Interview Core:** Model a payment `UNKNOWN` state and reconciliation.
- **SDE-2 Follow-up:** Write an outbox incident runbook with recovery and duplicate handling.

## Readiness checkpoint

Continue when you can explain not only rollback but also replay safety, uncertainty, durable hand-off, resource demand, and measurable recovery.
