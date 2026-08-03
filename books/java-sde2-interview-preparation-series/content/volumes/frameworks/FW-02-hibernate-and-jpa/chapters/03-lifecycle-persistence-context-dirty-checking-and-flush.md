# Lifecycle, Persistence Context, Dirty Checking, and Flush

## Four lifecycle states

```text
new/transient --persist--> managed --remove--> removed
                         |
                         +--detach/clear/close--> detached
detached --merge--> state copied into a managed instance
```

- **New:** ordinary object with no persistent identity in this context.
- **Managed:** tracked; changes may be synchronized automatically.
- **Detached:** has persistent identity but is no longer tracked.
- **Removed:** scheduled for deletion while managed.

## Identity map and repeatable Java identity

Within one persistence context, a given entity type and database identity correspond to one managed instance. Repeated `find` calls can return the same Java object without another SQL query.

This first-level cache is mandatory context behavior, not a general cross-request cache. It can also become stale when bulk SQL or external writers change the row.

## Dirty checking

```java
PurchaseOrder order = entityManager.find(PurchaseOrder.class, orderId);
order.markPaid();
// no persist/save call required for an already-managed entity
```

Hibernate tracks managed state and detects changes. At flush it can generate:

```sql
update purchase_order
set status=?, total_cents=?, version=?
where order_id=? and version=?
```

The exact columns depend on mappings/provider features such as dynamic update. Predicting “one update” is portable reasoning; promising exact SQL text is not.

## Flush is not commit

Flush synchronizes persistence-context changes with the database transaction. Commit completes the transaction. Therefore:

```text
Java mutation -> flush executes SQL -> later error -> rollback
```

SQL happened, but no durable commit remained. Calling `flush()` is useful to detect database failures earlier or satisfy query consistency; it does not make a transaction durable.

With JPA `AUTO`, a query in a joined transaction must see relevant pending changes, so the provider may flush before it. With `COMMIT`, visibility of unflushed changes to queries is less predictable under the portable contract. Outside an active joined transaction, the provider must not flush.

## `persist`, `merge`, and the ignored-return trap

`persist(newEntity)` makes that instance managed. `merge(detached)` copies state into a managed instance and returns it; the original remains detached.

```java
PurchaseOrder managed = entityManager.merge(detached);
// continue with managed, not detached
```

Blindly merging a whole client graph can overwrite concurrent changes or cascade unexpected inserts. Load the aggregate, authorize/validate the command, and mutate explicit fields.

## Clear, detach, and refresh

- `detach(entity)`: stop tracking one entity.
- `clear()`: detach all managed entities; useful in large batches.
- `refresh(entity)`: overwrite managed state from the database; pending local changes can be lost.

Flush before clear when work must reach the transaction. Batch loops often `flush()` and `clear()` every chunk to bound memory.

## Failure timeline

```text
persist duplicate business key
  -> Java call may return
  -> unrelated query triggers AUTO flush
  -> unique constraint fails at the query line
```

The query did not create the duplicate; it exposed deferred SQL. Diagnose from transaction history, not merely the top stack-frame line.

## Quick check and practice

1. Is the object passed to `merge` made managed?
2. Why can a select throw an insert constraint exception?
3. What survives a flush followed by rollback?
4. When should a batch clear its context?

- **Foundation:** Label six objects as new, managed, detached, or removed.
- **Interview Core:** Predict SQL for find-mutate-query-commit under `AUTO`.
- **SDE-2 Follow-up:** Repair a 500,000-row batch whose persistence context grows until OOM.
