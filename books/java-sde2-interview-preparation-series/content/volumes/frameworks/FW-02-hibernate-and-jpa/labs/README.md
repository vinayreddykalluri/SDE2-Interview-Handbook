# Hibernate/JPA Volume Labs

This Java 21 Maven fixture uses the real Hibernate ORM provider with Jakarta Persistence and H2. It validates portable persistence-context identity, dirty checking, flush/commit behavior, bidirectional association ownership, orphan removal, lazy N+1 versus fetch join, optimistic locking, bulk-DML staleness, projections, and bounded pagination.

H2 is intentionally a fast behavior fixture, not a MySQL substitute. Run separate target-MySQL integration tests for dialect SQL, collations, index plans, lock ranges, timeouts/deadlocks, schema migration, and batching at the wire level.

```bash
bash content/volumes/frameworks/FW-02-hibernate-and-jpa/labs/validate_hibernate_jpa_labs.sh
```
