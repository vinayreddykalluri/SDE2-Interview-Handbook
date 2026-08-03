# Persistence, SQL, and Caching Capstone Audit

## Audit decision

The original four chapters already covered relational modeling, plans and indexes, transactions and locks, pools and migrations, outbox, JPA/Hibernate lifecycle, fetching, caching, and a worked persistence case. The missing teaching layer was a single cross-system path. A candidate could know each component and still fail to explain which state is authoritative, where a failure becomes ambiguous, why an apparently healthy cache can hide a slow tail, or how a stale relay loses ownership.

## Changes made

- Added `05-cross-layer-data-path-failure-and-interview-capstone.md`.
- Traced the read path across cache, pool, ORM/JDBC, SQL plan, index, and storage.
- Traced the write path from transaction start through domain update, outbox insert, commit, publication, and cache convergence.
- Distinguished ORM flush, database commit, client response, and event propagation.
- Added stale-fill, deadlock, pool-exhaustion, retry, migration, and consistency-window analysis.
- Added seven realistic interviewer chains with worked answers.
- Extended the Java companion with an atomic outbox model and fenced claims that reject stale workers.
- Kept deep storage-engine, JPA, MySQL, Redis, and Kafka details in their dedicated books.

## Quality evidence

| Measure | Result |
|---|---:|
| Canonical chapters audited | 5 |
| Approximate chapter words | 13,830 |
| New capstone words | 1,792 |
| New worked live-interview chains | 7 |
| Dependency-free Java companions | 1 |
| Strict compilation target | Java 21 |

## Remaining boundary

The executable companion proves local state-machine invariants. Query plans, lock timing, isolation anomalies, pool saturation, Redis behavior, and Kafka delivery must also be tested against the relevant real service; the dedicated books contain those lower-level labs.
