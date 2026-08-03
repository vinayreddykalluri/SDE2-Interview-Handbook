# Locking and Concurrency Protection

Optimistic and pessimistic locking are interview-level decisions. A repository method without lock policy is often incomplete for SDE-2 readiness.

## Optimistic lock baseline

Optimistic locking uses version fields and checks updates.

- Detects stale write by version mismatch.
- Low contention path, good throughput.
- Requires exception handling and retry policy in service layer.

In JPA the common mechanism is a version column:

```java
@Entity
class OrderEntity {
    @Id
    private Long id;

    @Version
    private long version;
}
```

An update is conceptually guarded by the old version:

```sql
update customer_order
set status = ?, version = version + 1
where id = ? and version = ?;
```

Zero affected rows means the expected snapshot did not win. Reload and retry only when the business transition remains valid; never loop blindly around `save` with the same stale state.

## Pessimistic lock baseline

Pessimistic locking blocks writers/ readers at row level.

- Prevents conflicts in contention hotspots.
- Increases wait/lock timeout risk.
- Needs clear timeout and fallback.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select o from OrderEntity o where o.id = :id")
Optional<OrderEntity> findForUpdateById(long id);
```

The call needs an active transaction. SQL syntax, lock scope, gap/predicate behavior, timeout exceptions, and deadlock detection are database- and dialect-specific.

## Interview-safe locking flow

```text
Read with version / lock intent
      |
      v
Apply invariants and mutate state
      |
      v
Save and handle stale write / timeout exceptions
```

## Vendor boundary: do not claim universal lock escalation

Lock escalation is a documented behavior of some engines, not a portable JPA rule. MySQL/InnoDB, PostgreSQL, SQL Server, and MongoDB expose different row, key, range, predicate, document, and transaction models. Name the target store, exact statement and index, isolation/concern, rows or ranges touched, and evidence such as a deadlock graph or lock-wait view. Spring's `@Lock` selects a JPA lock mode; it does not normalize those internals.

## Failure matrix

| Symptom | Likely class | Next evidence |
|---|---|---|
| optimistic exception | version predicate affected zero rows | competing command and current version |
| lock timeout | incompatible lock held beyond wait policy | blocker transaction, query, index, hold time |
| deadlock victim | cyclic wait chosen for rollback | engine deadlock graph and acquisition order |
| high conflicts after retry | hot aggregate or stale workflow | contention rate and transition design |
| pool wait during locked work | too much concurrent or long transaction work | pool pending, transaction duration, useful DB concurrency |

## Debugging exercise

Two workers edit the same row using optimistic locking.

- Worker 1 reads version 3 and saves to version 4.
- Worker 2 reads version 3 and saves.

What happens and what does worker 2 need to do?

Expected: worker 2 gets stale-write signal and retries from refreshed state.

## Quick check

1. Why can optimistic locking fit a low-conflict path?
2. When might pessimistic locking be justified?
3. What makes lock timeout strategy interview-safe?

## Practice

- **Foundation:** Write a pseudo retry loop for stale writes.
- **Interview Core:** Explain why lock scope should be minimal.
- **SDE-2 Follow-up:** Compare lock behavior across MySQL and MongoDB at a high level.

## Interviewer question and model answer

**Interviewer:** When would you choose optimistic versus pessimistic locking?

**Model answer:** I start from the invariant and measured contention. Optimistic versioning is a good default when conflicts are uncommon and a command can be re-read and safely retried. Pessimistic locking can fit a short, high-contention critical section when bounded waiting is preferable to repeated conflicts. I keep either transaction short, use an index-supported predicate, set a bounded wait, classify vendor exceptions, and never claim JPA makes lock behavior identical across databases.
