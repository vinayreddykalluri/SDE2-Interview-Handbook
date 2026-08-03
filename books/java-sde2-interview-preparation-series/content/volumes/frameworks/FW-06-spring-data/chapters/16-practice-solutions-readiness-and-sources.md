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

Choose SQL when relational constraints, joins, and multi-row invariants fit the model. Choose MongoDB when a bounded document owns the lifecycle and serves the access pattern; acknowledge supported multi-document transactions without using them to excuse a poor boundary. Use Redis here for derived TTL/rate/cache state with an explicit source, atomic operation, expiry, failover, and fallback contract. Observe query/command shape, result size, latency, lock/wait, retry, and stale-data evidence.

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

## Twenty solved interviewer follow-ups

Read the question, answer aloud, then compare your answer with the model. A strong response names contract, runtime work, failure, and proof.

### 1. When would you avoid a repository for one operation?

**Model answer:** I use a custom fragment, `EntityManager`, template, or JDBC when the operation needs vendor SQL, bulk mutation, window functions, a deterministic lock sequence, or a narrow read model whose cost should remain visible. I keep repositories for the rest of the aggregate; the choice is per operation.

### 2. What happens when `save` receives a new versus detached JPA entity?

**Model answer:** Spring Data JPA selects persistence behavior from entity-new detection. A new entity is generally persisted; an existing/detached entity is generally merged, and merge copies state into a managed instance rather than reattaching the argument. I avoid binding an HTTP entity graph and blindly saving it; I load authoritative state, authorize, apply a command, and flush inside the service transaction.

### 3. Does `getByEmail` throw when no row exists?

**Model answer:** The `get` prefix alone does not define that guarantee. I inspect the declared return type, nullability rules, module/version, and cardinality. I prefer `Optional` for normal absence or an application-owned `require...` method that translates absence deliberately. `getReferenceById` is a separate JPA lazy-reference API.

### 4. Why use `existsBy...` instead of `countBy... > 0`?

**Model answer:** Existence is the actual question and can stop after one match; count must calculate the number of matches. I still inspect generated SQL and indexes. Neither is a business-table health check.

### 5. `Page` or `Slice`?

**Model answer:** I use `Page` only when the client truly needs an exact total because it can add a count query. `Slice` communicates content plus continuation without promising a total. For deep live traversal I consider a keyset `Window`/cursor with a total order.

### 6. Why must a cursor include a unique tie-breaker?

**Model answer:** If several rows share `createdAt`, a cursor containing only that timestamp cannot identify the exact continuation boundary, so rows can be skipped or repeated. I order by `(createdAt,id)` and carry both values with matching direction, scope, and filters.

### 7. Can I combine cursor and offset pagination?

**Model answer:** Not as one continuation contract. Offset advances by position; a cursor advances from an ordered key tuple. Combining them obscures semantics and can reintroduce drift. I choose one, cap the window, and document live versus snapshot behavior.

### 8. How do you diagnose N+1?

**Model answer:** I reproduce a bounded endpoint, capture SQL statement count and rows, identify which association access triggers secondary selects, and compare fetch join/entity graph, batch fetch, and DTO projection. The regression test asserts query count and result shape; a target-engine test covers the plan.

### 9. Why not mark every relationship eager?

**Model answer:** Static eagerness loads relationships for use cases that do not need them and can create large joins/object graphs. I choose a per-query fetch plan and keep to-many paging bounded. One SQL statement is not automatically cheap if it returns a Cartesian row explosion.

### 10. What exactly does flush guarantee?

**Model answer:** Flush synchronizes pending persistence-context changes to the database transaction so SQL and constraints can run. It does not commit, release every lock, make a remote call atomic, or prove another transaction can see the change. A failure means the transaction should roll back.

### 11. Is `REQUIRES_NEW` a retry strategy?

**Model answer:** No. It creates an independent physical transaction while suspending the caller's context and may require another connection. Retry starts after a classified failed transaction rolls back; it re-reads state, reapplies a safe command, preserves logical identity, and obeys attempt/deadline limits.

### 12. How do you handle an optimistic conflict?

**Model answer:** I return a conflict when user reconciliation is required, or I perform a bounded retry only if the command is still valid and idempotent. Each attempt uses a fresh transaction and refreshed state. I never retry every exception or repeatedly save the same stale object.

### 13. When is pessimistic locking justified?

**Model answer:** It can fit a short high-contention critical section where bounded waiting is preferable to frequent optimistic failures. I need an active transaction, an index-supported predicate, timeout policy, consistent acquisition order, and target-database tests. JPA lock modes do not erase vendor behavior.

### 14. Are database locks guaranteed to escalate?

**Model answer:** That is vendor-specific, not a Spring Data/JPA guarantee. I name the engine, version, isolation, statement, index, and documented row/key/range/predicate behavior, then inspect lock-wait or deadlock evidence rather than repeating a universal escalation rule.

### 15. When do you choose a native query?

**Model answer:** When a measured important path needs a database feature or SQL shape that JPQL cannot express clearly. I bind values, keep projection and ordering explicit, supply a correct count only if needed, pin target-engine tests, and record the portability/migration cost.

### 16. What can go wrong with a bulk update?

**Model answer:** Bulk JPQL/native DML bypasses normal per-entity dirty checking and callbacks, so managed objects can become stale. I define affected-row expectations, auditing/version behavior, clear or refresh the persistence context, and test concurrent and rollback cases.

### 17. Does MongoDB's transaction support make document modeling irrelevant?

**Model answer:** No. Supported deployments provide multi-document transactions, but they add coordination and failure handling. I still choose bounded document ownership from access patterns and invariants, then state read/write concerns, indexes, growth, and atomic update/version behavior.

### 18. Does `@Cacheable` solve cache stampedes?

**Model answer:** Not by itself. I define source of truth, key/version, TTL jitter, concurrent-miss coalescing, source admission, stale-fill prevention, and Redis outage behavior. Correctness must not silently depend on the cache unless that is an explicit product contract.

### 19. Why can an H2 repository test pass while MySQL fails?

**Model answer:** Dialect syntax, collation, temporal conversion, indexes, plans, isolation, lock ranges, deadlocks, and constraints can differ. H2 remains useful for fast mapping/query feedback; every engine-specific claim gets a target MySQL Testcontainers test with representative schema and data.

### 20. What evidence do you present for a repository design?

**Model answer:** I show the application contract, generated/native query, bound result and deterministic order, projection width, query/count count, target plan, transaction and lock boundary, pool demand, failure matrix, and a regression test. That is stronger than saying an annotation is convenient or that a method is `O(1)`.

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
