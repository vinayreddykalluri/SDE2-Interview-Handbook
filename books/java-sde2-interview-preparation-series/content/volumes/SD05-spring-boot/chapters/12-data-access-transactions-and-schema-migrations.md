# Data Access, Transactions, and Schema Migrations

Boot can configure a connection pool, JDBC/JPA infrastructure, transaction manager, SQL initialization, and migration tools. It cannot choose correct transaction boundaries or make an unsafe schema rollout compatible.

## DataSource auto-configuration

Typical inputs are:

- JDBC API and pool implementation on the classpath;
- a database driver;
- `spring.datasource.*` properties;
- absence of a user-defined `DataSource`.

An embedded database may start locally with no URL. Production must provide an explicit contract. Verify driver, URL, credentials, pool settings, validation, and target database behavior.

```properties
spring.datasource.url=jdbc:mysql://db:3306/orders
spring.datasource.username=orders_app
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.connection-timeout=1s
```

Do not copy pool sizes across services. Bound application concurrency and size the pool against database capacity, query time, and transaction duration.

## Transaction ownership

```java
@Service
final class OrderService {
    @Transactional
    public OrderId create(CreateOrder command) {
        // enforce invariant and persist one unit of work
    }
}
```

Boot contributes the manager; Spring transaction semantics still apply: proxy crossing, default unchecked rollback, propagation, isolation, and thread binding. Revisit SD 04 for those mechanics.

## Open-in-view warning

Keeping a persistence context open through web rendering can hide lazy loads in controllers/serialization and create query surprises. Prefer fetching what the use case needs inside the transaction and returning explicit DTOs. Configure and test the selected behavior rather than relying on a default that may change across lines.

## Schema migration is deployment coordination

Use Flyway or Liquibase as the canonical migration mechanism. Boot can run migrations during startup, but rollout safety depends on service topology and migration type.

Expand-contract example:

```text
release A: add nullable new column/index without breaking old code
release B: write old and new representation; backfill safely
release C: read new representation after evidence
release D: stop old writes
release E: enforce constraint/drop old column later
```

Large backfills and blocking DDL may not belong in every application instance's startup path. Coordinate one migration job and observe locks, replication, and duration.

## Initialization ordering

Do not mix ad hoc `schema.sql`, ORM create/update, and a migration tool without a defined owner. In production, automatic ORM schema mutation is rarely a safe deployment plan.

## Failure behavior

- Invalid credentials should fail startup if the database is required.
- A temporary database outage may keep readiness false; liveness should not restart every replica solely because a shared database is down.
- Pool exhaustion requires separating acquisition wait, leaks, slow queries, locks, and transaction duration.
- Migration failure should halt the rollout, not let incompatible code receive traffic.

## Common mistakes

- Assuming auto-configured H2 proves MySQL behavior.
- Keeping a transaction open across remote calls.
- Returning entities directly from controllers.
- Enabling schema auto-update in production.
- Running a destructive migration before all old instances stop using the field.
- Making every pod race to perform a long backfill.

## Interview angle

**Interviewer:** A deployment starts timing out on connection acquisition. Increase the pool?

**Strong answer:** First I measure active/idle/pending connections, transaction duration, slow queries, lock waits, remote calls inside transactions, and leak evidence. A larger pool can overload the database. I shorten hold time and bound concurrency, then size the pool from measured database capacity.

## Quick check

1. What inputs commonly trigger DataSource auto-configuration?
2. Why can H2 tests mislead?
3. What is expand-contract migration?
4. Why avoid ORM auto-update in production?
5. What does open-in-view conceal?

## Practice

- **Foundation:** Trace the beans created for one JDBC configuration.
- **Interview Core:** Design a no-downtime column rename.
- **Interview Core:** Diagnose pool acquisition timeout with five measurements.
- **SDE-2 Follow-up:** Decide whether migrations run in application startup, a deployment job, or both.
