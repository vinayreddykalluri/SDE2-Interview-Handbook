# Locking and Concurrency Protection

Optimistic and pessimistic locking are interview-level decisions. A repository method without lock policy is often incomplete for SDE-2 readiness.

## Optimistic lock baseline

Optimistic locking uses version fields and checks updates.

- Detects stale write by version mismatch.
- Low contention path, good throughput.
- Requires exception handling and retry policy in service layer.

## Pessimistic lock baseline

Pessimistic locking blocks writers/ readers at row level.

- Prevents conflicts in contention hotspots.
- Increases wait/lock timeout risk.
- Needs clear timeout and fallback.

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

## Debugging exercise

Two workers edit the same row using optimistic locking.

- Worker 1 reads version 3 and saves to version 4.
- Worker 2 reads version 3 and saves.

What happens and what does worker 2 need to do?

Expected: worker 2 gets stale-write signal and retries from refreshed state.

## Quick check

1. Why is optimistic lock best for read-dominant paths?
2. When is pessimistic lock unavoidable?
3. What makes lock timeout strategy interview-safe?

## Practice

- **Foundation:** Write a pseudo retry loop for stale writes.
- **Interview Core:** Explain why lock scope should be minimal.
- **SDE-2 Follow-up:** Compare lock behavior across MySQL and MongoDB at a high level.
