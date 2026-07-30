# Practice, Solutions, Readiness, and Sources

The final section is your bridge from reading to interviews.

## Cumulative assessment 1 - Design discipline

1. Design three repository methods for one aggregate.
2. Define one boundary where repository abstraction must stop.
3. Add optimistic locking and one conflict recovery branch.
4. Write ordering strategy for pagination.
5. Explain failure behavior for duplicate IDs.

### Solution sketch

Use narrow interfaces, explicit state transitions, deterministic sort keys, and explicit exception handling for every command path. Add idempotent retries only around safe state transitions and track user-visible error contracts.

## Cumulative assessment 2 - Correctness and performance

1. Show one derived method that is too broad.
2. Convert it to declared query and projection.
3. Identify potential N+1 risks.
4. Add query-count assertion in a test.
5. Add one lock strategy for conflicting writes.

### Solution sketch

Use explicit select + join intent in the declared query, remove unnecessary eager graph, and add deterministic paging. Add assertions for page window size and ordering.

## Cumulative assessment 3 - Persistence store boundaries

1. Choose repository strategy for one SQL feature.
2. Choose repository strategy for one document feature.
3. Choose Redis strategy for one coordination task.
4. Define rollback and cache handling on write failure.
5. Explain observability evidence required for each.

### Solution sketch

Use SQL repositories when consistency and joins dominate. Use Mongo documents when write shape is local-read heavy and stable. Use Redis for lock/rate/TTL state with explicit fallback. Add logs for query/cached path and timeout/failure events.

## Cumulative assessment 4 - Mock interview round

Given this API description, explain repository choices and failure handling:

- Create order
- Pay order
- Cancel order
- List open orders with pagination

### Solution sketch

Model command-oriented repositories, ensure idempotent order creation, isolate payment side-effects from transaction-critical DB updates, and paginate `OPEN` orders deterministically.

## Cumulative assessment 5 - Readiness check

1. Do you explain repository behavior without naming only annotations?
2. Can you describe where abstraction hides cost?
3. Can you propose one test for lock contention?
4. Can you explain a deterministic paging strategy?
5. Can you map one bug directly to a query/call trace?

### Solution sketch

Readiness is demonstrated when you can discuss contract, semantics, failure handling, and evidence in one coherent flow.

## Predict the outcome

1. Derived query for bounded page without `OrderBy` can still be paginated but nondeterministic.
2. Optimistic lock retry without refresh can still rethrow repeatedly.
3. Mongo schema changes may require migration scripts and read-rewrite strategy.
4. Redis count drift usually appears when cache and DB update ordering are inconsistent.

## Interview follow-ups

1. When would you avoid Spring Data entirely and use direct DB APIs?
2. How do you prove your query path in a live interview?
3. When is native query justified with version locking?
4. Which repository method should never be exposed directly to controllers?
5. How do you keep repository contracts portable across teams?

## Cross-book boundaries

- Transaction fundamentals and AOP semantics -> **SD 04 - Spring Framework**.
- Auto-configuration and app packaging -> **SD 05 - Spring Boot**.
- ORM mapping and JPA behavior -> **SD 03 - Hibernate and JPA**.
- Query and indexing fundamentals -> **SD 02 - MySQL**.
- Cache and cache patterns -> **SD 08 - Redis**.
- Distributed architecture trade-offs -> **SD 09 - Apache Kafka and Spring Kafka**.

## Primary sources

- Spring Data Commons Reference: `https://docs.spring.io/spring-data/commons/reference/`
- Spring Data JPA Reference: `https://docs.spring.io/spring-data/jpa/reference/`
- Spring Data MongoDB Reference: `https://docs.spring.io/spring-data/mongodb/reference/`
- Spring Data Redis Reference: `https://docs.spring.io/spring-data/redis/reference/`
- Spring Framework transaction model: `https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html`
- Java concurrency and locking model: `https://docs.oracle.com/en/java/javase/21/docs/`
