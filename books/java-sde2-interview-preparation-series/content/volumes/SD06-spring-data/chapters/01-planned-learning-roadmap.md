# Spring Data for Relational and Document Stores - Planned Learning Roadmap

> **Publication status:** roadmap edition. Repository implementations, database-backed tests, query diagnostics, and failure exercises will be added in later revisions.

Spring Data provides repository abstractions across multiple data technologies. This book will teach where those abstractions improve consistency and where database-specific behavior must remain visible. MySQL, Hibernate/JPA, and Spring Framework are prerequisites for the relational path.

## Planned sequence

1. Repository contracts, aggregate boundaries, domain types, and store-specific modules.
2. Spring Data JPA repositories, derived queries, declared queries, projections, and specifications.
3. Pagination, sorting, streaming, batching, auditing, and transaction ownership.
4. Fetch plans, locking, modifying queries, bulk updates, and persistence-context effects.
5. Spring Data MongoDB documents, indexes, templates, repositories, and transactions.
6. Spring Data Redis structures, repositories, serializers, TTLs, and cache boundaries.
7. Testing with real stores, containers, migrations, and deterministic fixtures.
8. Diagnosing generated queries, hidden round trips, abstraction leaks, and upgrade changes.

## Interview focus

Readers will learn to separate repository convenience from database guarantees, predict when a method name creates an unsafe query, preserve aggregate and transaction boundaries, and choose a template or lower-level API when repository methods obscure important work.

## Completion gate

A reader is ready to use Spring Data in service design when they can define a narrow repository contract, inspect the executed database operations, control fetching and paging, test against the real store, and explain which semantics belong to Spring Data versus the database.
