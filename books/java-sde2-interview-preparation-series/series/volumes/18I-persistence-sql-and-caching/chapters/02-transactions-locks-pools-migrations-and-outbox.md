# Transactions, Isolation, Locks, Pools, Migrations, and Outbox Boundaries

## Learning objectives

After this chapter, you should be able to:

- define a transaction from the business invariant and resource boundary;
- distinguish dirty read, nonrepeatable read, phantom, lost update, write skew, and serialization failure;
- choose atomic SQL, optimistic versioning, pessimistic locking, or serializable retry from contention and invariant shape;
- prevent, diagnose, and safely retry selected deadlock/serialization victims;
- size a connection pool as a concurrency limit rather than a throughput multiplier;
- evolve schemas with expand/migrate/contract compatibility; and
- use an outbox and idempotent consumer to cross database/message boundaries without claiming impossible local atomicity.

## 1. Transaction as a recoverable invariant boundary

A transaction groups operations in one resource manager under atomic commit/rollback and isolation behavior. The useful question is not “which methods have an annotation?” It is:

> Which state changes must become visible together so every committed state satisfies the business invariant?

For a bank transfer within one database:

```text
debit source + credit destination + append ledger entries = one local transaction
```

For “update database and call email provider,” there is no single local transaction. Email must be derived from recoverable state and retried/deduplicated. Holding the database transaction open during the call increases lock and connection time but does not make the remote side atomic.

### JDBC execution skeleton

```java
// Dependency-requiring only for the configured JDBC driver/DataSource.
public TransferResult transfer(DataSource dataSource, Command command)
        throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            TransferResult result = executeTransfer(connection, command);
            connection.commit();
            return result;
        } catch (Throwable failure) {
            try {
                connection.rollback();
            } catch (SQLException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }
}
```

Production pool integrations normally reset documented session state, but code should not rely on an undocumented cleanup miracle. Use try-with-resources. Do not leak result sets/statements. Map classified SQL failures through vendor error codes/states rather than parsing localized messages. Framework transaction abstractions can handle boilerplate; the invariant and failure semantics remain yours.

## 2. Isolation anomalies as executions

### Phenomena

Use schedules rather than labels:

- **dirty read:** T2 observes T1's uncommitted write; T1 later rolls back;
- **nonrepeatable read:** T1 reads row R, T2 commits a change, T1 reads R again and sees a different value;
- **phantom:** T1 repeats a predicate query and sees a changed set due to concurrent insert/delete/update;
- **lost update:** two transactions derive writes from the same old value and one overwrites the other;
- **write skew:** transactions read a shared condition and update different rows, jointly violating a cross-row invariant;
- **serialization failure:** the database rejects an execution that cannot be ordered under its serializable model; the application may retry the *whole transaction*.

SQL isolation names are not complete portable behavior specifications. Lock-based and MVCC engines map levels differently, and vendor documentation defines actual guarantees. State the engine/version when claiming which anomaly is prevented.

### Write-skew walkthrough

Invariant: at least one doctor must remain on call.

```text
T1 reads Alice=on, Bob=on
T2 reads Alice=on, Bob=on
T1 updates Alice=off
T2 updates Bob=off
both commit -> invariant violated
```

Each row update touched a different row, so row-level optimistic versions may not conflict. Solutions include serializable execution with retry, locking a shared invariant row/range, redesigning the data so one atomic constraint can protect it, or a database-specific constraint/procedure. “Use optimistic locking” is incomplete unless its conflict set covers the invariant.

## 3. Concurrency-control decision table

| Pattern | Best fit | Contract | Costs/traps |
|---|---|---|---|
| atomic conditional update | simple numeric/state predicate | success is update count > 0 | limited to expressible predicate; translate zero carefully |
| optimistic version | low/moderate conflicting writes, user edits | `UPDATE ... WHERE id=? AND version=?` | retry or surface conflict; detached stale data; does not cover other rows automatically |
| pessimistic row/range lock | high-cost conflicts that must be serialized | acquire documented lock before decision | waits, deadlocks, pool occupancy; range behavior engine-specific |
| serializable transaction | complex invariant requiring serial execution | DB either produces serializable effect or aborts | retries and contention; no remote side effects inside retryable body |
| application mutex | process-local state only or coordinated external lock with protocol | one owner under defined lease/fencing | single-JVM locks do not protect multiple instances; leases need fencing |

### Atomic inventory example

```sql
update inventory
set available = available - :requested,
    version = version + 1
where sku = :sku
  and available >= :requested;
```

The statement makes check and decrement one database operation. Update count zero needs a product decision: unknown SKU, insufficient stock, concurrent change, or authorization filtering. Avoid an extra pre-read as the correctness guard; it can improve diagnostics but the conditional update is authoritative.

### Optimistic locking example

```sql
update customer_order
set note = :note,
    version = version + 1,
    updated_at = :now
where id = :id
  and tenant_id = :tenant
  and version = :expected_version;
```

Zero rows means the precondition failed or the row is invisible/missing. Do not automatically retry a user edit using new state: that can overwrite someone else's intent. For an internally derived commutative transition, a bounded reload/recompute retry may be correct.

## 4. Locks and deadlocks

A lock protects a resource under an engine-defined compatibility matrix and duration. Resources can be rows, keys, key ranges, pages, tables, metadata, advisory identities, or internal structures. “Row lock” is often an approximation; inspect the target engine.

A deadlock is a cycle in the wait-for graph:

```text
T1 holds A, waits for B
T2 holds B, waits for A
```

The database detects or times out a victim. Deadlocks are not eliminated by higher timeouts.

### Prevention and reduction

- access shared resources in a deterministic order;
- keep transactions short and exclude remote/user think time;
- use indexes so mutations lock/find only intended rows/ranges;
- avoid updating unrelated aggregates inside hot transactions;
- choose isolation and locking that match the invariant;
- bound batch size;
- monitor lock waits and capture deadlock diagnostics;
- retry only the whole transaction, only for classified retryable victims, with jitter and a deadline.

### Failure walkthrough: retrying at the wrong level

A transfer debits account A, sends a notification, then updates B. The database aborts as a deadlock victim. Retrying only the B update leaves an unbalanced transfer; retrying the whole block sends the notification twice. Correct design keeps remote notification out of the retryable transaction and records an outbox event atomically. The whole local transaction is retried from a clean connection/state with an idempotent command identity.

## 5. Connection pools and backpressure

A database connection is a scarce session and concurrency slot. If a service has `N` instances each with pool maximum `P`, potential sessions approximate `N * P` plus jobs, consoles, failover overlap, and other services. The database must support the total with headroom.

Little's Law provides a useful check: average in-flight database work `L = arrival rate λ * average time W`. Increasing the pool above the database's useful concurrency can increase queueing, context switching, locks, and tail latency instead of throughput.

Pool decision inputs:

- measured database capacity and latency under representative queries;
- service instance count and autoscaling maximum;
- request mix and fraction that needs a connection;
- transaction duration distribution, not only average;
- scheduled/background consumers;
- failover and rolling-deployment overlap;
- acquisition timeout shorter than the request deadline;
- leak detection and active/idle/wait metrics.

Do not hold a connection while waiting on HTTP, sleeping for retry, rendering a large response, or processing CPU-heavy work that could happen before/after the transaction. An unbounded application queue merely hides pool saturation until deadlines expire.

### Pool failure signatures

- rising acquisition wait with pool at max: database work or capacity bottleneck;
- active below max but requests slow: bottleneck elsewhere or pool configuration/traffic shape;
- many idle connections across many instances: wasted DB session capacity;
- frequent validation failures: network/database lifecycle mismatch;
- connections returned with open transaction/session changes: ownership bug or reset-policy mismatch;
- leak warnings: inspect stack evidence, but long legitimate transactions can resemble leaks.

## 6. Schema migrations with mixed-version compatibility

During a rolling deployment, old and new application versions may run against one schema. Use expand/migrate/contract:

1. **expand:** add backward-compatible structures—nullable column, new table, index built with the engine's safe procedure;
2. **deploy compatibility:** new code can read old/new form and writes data needed by both when necessary;
3. **backfill/migrate:** bounded, resumable, observable batches; throttle and verify;
4. **switch reads:** use the new representation after correctness evidence;
5. **stop old writes:** deploy and verify;
6. **contract:** later remove old column/constraint/path after rollback window.

Adding a `NOT NULL` column with a computed default, rebuilding a large table/index, or validating a constraint can lock or rewrite depending on engine/version. Check official database documentation and test on production-scale copies. Migration tools order and record scripts; they do not make every DDL operation online or reversible.

### Migration mistakes

- destructive rename/drop in the same deploy that changes code;
- one enormous backfill transaction;
- dual write without reconciliation or authoritative source;
- assuming rollback can restore discarded data;
- allowing every instance to race migration on startup without a supported lock protocol;
- ORM auto-DDL in production hiding review and rollout semantics;
- adding an index without measuring replication/log and write impact.

## 7. Transactional outbox

### Contract and flow

When a use case updates domain state and needs to publish an event:

```text
local transaction:
  update domain rows
  insert outbox(event_id, aggregate_id, type, payload, occurred_at)
commit

relay:
  claim/read unpublished events
  publish
  record progress

consumer:
  deduplicate event_id
  apply local transition atomically with inbox marker where appropriate
```

The domain change and intent-to-publish are atomic because both are database rows. Publish and mark cannot generally be atomic across DB and broker, so duplicates remain possible. Consumers must be idempotent. Ordering is only as strong as the outbox claim/publish and broker key/partition protocol; global order is costly and rarely needed.

### Relay decisions

- polling versus database-log/change-data-capture approach;
- claim strategy for multiple relay workers;
- batch size and transaction duration;
- retry/backoff and poison-event quarantine;
- per-aggregate ordering key;
- schema/event-version compatibility;
- payload privacy and retention;
- backlog age SLO and reconciliation.

Do not delete an outbox row immediately without operational evidence. Archival/retention depends on replay, audit, privacy, and storage needs. A DLQ does not solve correctness; it stores work requiring a triage/replay protocol.

## 8. Interview questions and model checkpoints

### Q1. What isolation level prevents lost updates?

**Model checkpoint:** do not answer from name alone. Define the schedule, database engine behavior, statement shape, and whether atomic predicates/version checks are used. A version predicate explicitly detects overwrite regardless of many read-level subtleties.

### Q2. Should the pool maximum equal request threads?

**Model checkpoint:** no. Size from database useful concurrency, instance count, query time, traffic mix, and deadline. More connections can worsen saturation; admission must be bounded.

### Q3. Does outbox give exactly-once delivery?

**Model checkpoint:** it atomically stores domain change plus publication intent. Relay publish/mark can duplicate; consumers need idempotency/deduplication. Define ordering and retry separately.

### Q4. How do you deploy a new non-null field?

**Model checkpoint:** expand with compatible nullable/default structure, deploy compatible writers/readers, backfill and verify, switch/enforce, then contract later. Exact online DDL depends on the engine/version.

### SDE-2 follow-ups

1. A deadlock rate rises after adding an index. Explain how changed plans/order can alter locks and how you would gather evidence.
2. Autoscaling doubled instances and caused pool timeouts. Build a connection-budget calculation.
3. A consumer processed an event, crashed before committing its dedupe marker, and ran twice. Redesign the local atomic boundary.
4. Plan a migration from integer to string/UUID external IDs with two application versions active.

## 9. Exercises

1. Draw schedules for lost update and write skew; propose three fixes and state contention tradeoffs.
2. Implement a JDBC optimistic update that distinguishes conflict from database outage without message parsing.
3. Given 40 instances, 20 background workers, and a 600-connection DB budget, propose pool/admission limits and deployment headroom.
4. Design a resumable backfill with checkpoints, throttling, validation, and rollback policy.
5. Specify outbox and inbox tables plus relay/consumer state transitions for per-order event ordering.

## 10. Summary checklist

- [ ] The transaction matches one local invariant and resource manager.
- [ ] Isolation claims name an execution and target database behavior.
- [ ] Atomic/version/lock/serializable choice matches contention and invariant scope.
- [ ] Deadlocks retry the whole safe local transaction under a deadline.
- [ ] Pool size is part of a system-wide connection and concurrency budget.
- [ ] Migrations tolerate mixed application versions and have a repair path.
- [ ] Outbox publication and consumers are explicitly duplicate-safe.
- [ ] Remote calls and unbounded work are outside database transactions.

## 11. Transaction incident laboratory

### Incident A: “successful” request rolled back

A service catches a duplicate-key exception, logs it, and returns an existing object. At method exit Spring reports an unexpected rollback. The database exception marked the transaction rollback-only; catching it did not restore a valid transactional context.

Evidence and repair:

1. capture the first database exception and transaction state, not only commit failure;
2. identify whether duplicate is an expected race protected by a unique constraint;
3. restructure reserve-or-read so the conflicting insert occurs in a boundary where rollback is expected, or use database-supported atomic upsert/insert-if-absent semantics;
4. do not continue arbitrary ORM work after a failed flush;
5. integration-test two concurrent attempts and assert exactly one durable row/result;
6. classify only the known constraint.

Starting `REQUIRES_NEW` around random repository calls can mask the symptom while breaking the use-case invariant and consuming extra pool connections.

### Incident B: deadlocks after batch feature

Two workers update orders from different input order:

```text
worker A locks O-1 then O-2
worker B locks O-2 then O-1
```

The database chooses a victim. Remediation is not “disable transactions.” Sort resource identities and acquire/update in deterministic order, reduce batch size and transaction duration, verify indexes, and retry the whole idempotent local unit for the classified victim. Measure whether a different execution plan still locks in another order. Capture deadlock graph/details supplied by the engine.

### Incident C: pool starvation with nested transactions

Sixty request threads each hold one outer transaction connection and invoke audit logic that requires an independent connection. Pool maximum is sixty. Every thread waits for a second connection that cannot become free until outer work completes: a resource deadlock outside the database's lock detector.

Fix semantics first: should audit commit independently if business work rolls back? An outbox or after-commit audit may be more honest. If independent nested transactions are required, pool/concurrency policy must account for peak simultaneous connections and leave headroom—but limiting inbound concurrency is safer than hoping every request never nests.

### Incident D: migration causes write outage

A migration adds an index/constraint using a blocking procedure on a large hot table. Application pods are healthy but writes queue behind a schema lock.

Runbook:

- stop/abort the migration only through documented safe database commands;
- reduce user impact with admission/read-only behavior if applicable;
- inspect blocker/wait evidence and replication/log impact;
- redesign as expand plus online/concurrent build or validated-in-stages technique supported by that engine/version;
- test production-scale duration/locks;
- set migration statement/lock timeouts where supported;
- separate deploy and contract cleanup.

### Transaction laboratory checkpoint

For every incident, draw connections, transactions, locks, waits, external effects, and commit points. The final answer must say what durable state exists after each exception and which operation identity makes a retry safe.

## 12. Outbox ordering and retention drill

Suppose two transactions for order `O-7` commit events with aggregate sequence 8 and 9. Multiple relay workers can claim them. Publishing 9 before 8 violates a consumer that expects state-machine order even though both rows are durable.

Possible contracts:

- claim/publish one aggregate serially and key Kafka by aggregate ID;
- let broker partition order reflect outbox sequence, ensuring producer submission for one key is ordered under documented client behavior;
- consumers accept only next expected version and buffer/retry gaps;
- consumers treat events as state facts with monotonic source version and ignore stale versions;
- redesign events to be commutative where business meaning permits.

Global commit order across every aggregate is rarely required and can destroy parallelism. State per-aggregate versus partition versus global order explicitly. Database auto-increment IDs are not automatically business commit order under concurrent transactions; allocation and commit can differ.

Retention connects publisher and consumer recovery. Deleting an outbox row after publish may be fine for relay operation if Kafka retention/replay and domain records satisfy audit; it may be wrong when the outbox is the only legal record. Inbox/deduplication retention must exceed the plausible redelivery/replay window. If an operator replays events older than the inbox TTL, effects can repeat. A replay tool should use a new controlled operation identity or restore dedupe evidence under an approved protocol.

Monitor:

- oldest unpublished age and count;
- per-aggregate sequence gaps;
- claim lease expiry/steal;
- publish attempt and classified failure;
- published-but-unmarked duplicate rate;
- poison-event quarantine age;
- inbox duplicate rate and retention cleanup;
- reconciliation between domain transitions and expected outbox events.

Payload evolution also crosses retention. A relay/consumer deployed today may read an event written weeks ago. Keep compatible decoders or migrate/quarantine deliberately; do not let a rolling deploy strand old rows forever.

**Checkpoint:** for one event, identify database commit, relay claim, broker acknowledgement, mark-progress, consumer effect, inbox commit, offset commit, and every crash window. “At least once” becomes meaningful only when each durable position is visible.

## Primary references

- Java SE 21 JDBC API, `java.sql`: <https://docs.oracle.com/en/java/javase/21/docs/api/java.sql/java/sql/package-summary.html>
- PostgreSQL Documentation, “Transaction Isolation”: <https://www.postgresql.org/docs/current/transaction-iso.html>
- PostgreSQL Documentation, “Explicit Locking”: <https://www.postgresql.org/docs/current/explicit-locking.html>
- PostgreSQL Documentation, “Deadlocks”: <https://www.postgresql.org/docs/current/explicit-locking.html#LOCKING-DEADLOCKS>
- Flyway Documentation, “Migrations”: <https://documentation.red-gate.com/flyway/reference/migrations>

> **Version boundary:** lock resources, isolation behavior, DDL locking, SQLState/vendor codes, and online migration techniques are database- and version-specific. Pool reset and timeout behavior is pool/driver-specific. The Java baseline is Java 21; framework conveniences do not change the database atomicity boundary.
