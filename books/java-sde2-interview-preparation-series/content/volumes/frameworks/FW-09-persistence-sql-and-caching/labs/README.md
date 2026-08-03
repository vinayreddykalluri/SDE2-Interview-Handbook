# Persistence, SQL, and caching capstone lab

Run from the series root:

```bash
bash content/volumes/frameworks/FW-09-persistence-sql-and-caching/labs/validate_persistence_capstone.sh
```

The lab compiles the dependency-free Java 21 companion with all lint warnings treated as errors, enables assertions, and verifies its exact output. It covers optimistic updates, stable keyset pagination, version-aware cache fills, negative-cache invalidation, atomic outbox insertion, fenced relay claims, and stale-worker rejection. These are executable reasoning models, not substitutes for database integration and concurrency tests.
