# Redis Code Validation — Backend Wave 3

## Results

- Dependency-free companion: **1/1 strict Java 21 compile and smoke pass**.
- Maven/JUnit mechanics: **7 passed, 0 failed/errors/skipped**.
- Observed output: `RedisInterviewCompanion checks passed`.

```bash
bash content/volumes/frameworks/FW-08-redis/labs/validate_redis_labs.sh
```

Tests prove UTF-8 RESP byte lengths, exact TTL boundary, `NX` rejection until expiry, token-safe release, sliding-window boundary, per-key scoping, and fencing stale owners. They do not emulate a Redis server/topology.

## Exact root source array

Use `series_native: true` for each:

```text
content/volumes/frameworks/FW-08-redis/chapters/00-learning-path-bytes-structures-and-first-principles.md
content/volumes/frameworks/FW-08-redis/chapters/01-resp-command-execution-data-types-and-key-design.md
content/volumes/frameworks/FW-08-redis/chapters/02-ttl-expiration-eviction-memory-persistence-and-durability.md
content/volumes/frameworks/FW-08-redis/chapters/03-cache-patterns-consistency-stampedes-hot-keys-and-negative-caching.md
content/volumes/frameworks/FW-08-redis/chapters/04-atomicity-transactions-lua-functions-rate-limits-and-locks.md
content/volumes/frameworks/FW-08-redis/chapters/05-replication-sentinel-cluster-failover-operations-and-security.md
content/volumes/frameworks/FW-08-redis/chapters/06-java-lettuce-spring-data-serialization-testing-and-diagnostics.md
content/volumes/frameworks/FW-08-redis/chapters/07-live-interviews-rapid-qa-practice-solutions-and-sources.md
```

```json
"code_companion": {
  "path": "content/volumes/frameworks/FW-08-redis/code/RedisInterviewCompanion.java",
  "title": "Java 21 Redis Interview Reasoning Companion",
  "description": "Executable models for structure selection, TTL jitter, logical freshness, Cluster hash tags, lease ownership, and binary-safe encoding."
}
```

Set `publication_status: "published"`, `volume_label: "Publication Edition"`, `min_pages: 20`, and `max_pages: 80`.

After root integration:

```bash
python3 scripts/validate_series.py --source-only
python3 scripts/build_series.py --volume REDIS
python3 scripts/validate_series.py
```
