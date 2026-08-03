# Persistence, SQL, and Caching Capstone Validation

## Command

```bash
bash content/volumes/frameworks/FW-09-persistence-sql-and-caching/labs/validate_persistence_capstone.sh
```

## Validation contract

- Compile `PersistencePatterns.java` with `javac --release 21 -Xlint:all -Werror`.
- Execute with assertions enabled.
- Compare the complete process output with `PersistencePatterns assertions passed`.
- Fail on a compiler warning, assertion error, exception, or output mismatch.

## Result

Status is recorded from a clean validation run during this content wave: **PASS**.

The executable checks cover optimistic version predicates, deterministic keyset order, version-aware cache replacement, missing-value invalidation, outbox uniqueness, worker claim fencing, release and reclaim, stale-worker rejection, and terminal publication state.
