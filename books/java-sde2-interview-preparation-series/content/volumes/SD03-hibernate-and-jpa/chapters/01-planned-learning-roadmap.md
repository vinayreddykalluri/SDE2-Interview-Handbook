# Hibernate and JPA for SDE-2 Interviews - Planned Learning Roadmap

> **Publication status:** roadmap edition. The expanded book will add runnable mappings, SQL traces, test fixtures, debugging tasks, and performance investigations.

JPA defines persistence contracts; Hibernate is a widely used implementation with additional behavior and tooling. The book will begin from the SQL and transaction model established in the MySQL volume so ORM convenience never hides database work.

## Planned sequence

1. Entity identity, persistence context, lifecycle states, and unit-of-work behavior.
2. Basic mappings, value objects, converters, inheritance, and identifier strategies.
3. Associations, ownership, cascading, orphan removal, and aggregate boundaries.
4. Lazy and eager loading, proxies, fetch joins, entity graphs, and the N+1 problem.
5. Dirty checking, flushing, batching, statement ordering, and generated SQL.
6. JPQL, Criteria APIs, native SQL, projections, and pagination.
7. Optimistic and pessimistic locking, transaction boundaries, and retry policy.
8. Caches, auditing, testing, migration compatibility, and production diagnostics.

## Interview focus

The completed edition will make readers predict SQL before running code, detect accidental graph traversal, distinguish cascade from database referential actions, and explain why an open persistence context is not a substitute for an explicit service transaction. Exercises will include broken mappings and performance regressions.

## Completion gate

A reader is ready for Spring Data when they can map an aggregate deliberately, control fetching, interpret generated SQL, preserve transaction and locking semantics, and explain when direct SQL or JDBC is clearer than an ORM abstraction.
