# JPA and Hibernate: Lifecycle, Fetching, Equality, and Locking

## Learning objectives

After this chapter, you should be able to:

- distinguish Jakarta Persistence contracts from Hibernate features and database behavior;
- explain transient, managed, detached, and removed entity states;
- reason about the persistence context as an identity map plus unit of work;
- predict dirty checking and flush boundaries without equating flush with commit;
- choose entity equality that remains safe across generated IDs, sets, detachment, and proxies;
- detect and remove N+1 query behavior using projections, fetch joins, entity graphs, or batching;
- prevent cartesian explosions and pagination/fetch-join traps; and
- use optimistic and pessimistic locking from explicit conflict semantics.

## 1. Three layers of contract

Keep three authorities separate:

1. **Jakarta Persistence specification:** entity lifecycle, persistence context, query language, lock modes, and standardized API semantics;
2. **provider documentation:** Hibernate fetching options, batching properties, proxy/enhancement details, statistics, and SQL generation choices;
3. **database/driver:** SQL plan, isolation, locks, constraints, timeout enforcement, and actual concurrency.

An annotation is not a query plan. `FetchType.LAZY` expresses a contract/hint boundary defined by the spec, while the provider decides mechanisms. A generated SQL query is not guaranteed across provider versions. A pessimistic lock request maps through the provider to capabilities of the target database.

Recognition rule: in every ORM interview answer, state which layer owns the claimed behavior.

## 2. Entity lifecycle and persistence context

### State model

```text
new/transient --persist--> managed --remove--> removed
                         |
                         +--clear/close/detach--> detached

detached --merge--> copy becomes managed (argument remains detached)
```

- **new/transient:** Java object has no managed persistence identity in the context;
- **managed:** associated with an active persistence context; changes may be synchronized;
- **detached:** retains values/identity but is no longer tracked by that context;
- **removed:** scheduled for deletion when synchronized according to transaction rules.

The persistence context normally provides identity: within one context, finding the same entity identity yields the same managed Java object under the API contract. It is also a unit of work: it tracks managed state and synchronizes changes at flush.

Do not share an `EntityManager` across arbitrary threads. Scope and thread-safety follow the specification/container integration. Entities returned beyond the transaction become detached; lazy association access may then fail or trigger an architectural dependency on an “open session in view” policy.

### Concrete entity — dependency-requiring

```java
@Entity
@Table(name = "customer_order",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_order_tenant_external",
           columnNames = {"tenant_id", "external_order_id"}))
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private long tenantId;

    @Column(name = "external_order_id", nullable = false, length = 80)
    private String externalOrderId;

    @Version
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
               orphanRemoval = true)
    private final List<OrderLineEntity> lines = new ArrayList<>();

    protected OrderEntity() { }

    public void addLine(OrderLineEntity line) {
        Objects.requireNonNull(line);
        lines.add(line);
        line.attachTo(this);
    }
}
```

The database constraint is authoritative for concurrent uniqueness. Bidirectional helper methods maintain both in-memory sides. Cascade and orphan removal encode lifecycle ownership; do not apply `CascadeType.ALL` mechanically to shared relationships such as products or users.

## 3. Dirty checking, flush, and commit

### Formal model

For a managed entity, a provider tracks state and detects changes. At a flush, pending entity transitions are translated to SQL in an order that satisfies provider/database constraints as far as possible. Flush can occur:

- explicitly through `flush()`;
- before transaction commit;
- before certain queries depending on flush mode and affected state;
- at provider-defined/standardized synchronization points.

Flush sends/synchronizes changes to the database transaction. It does **not** mean the transaction committed. Later rollback can undo database changes. Conversely, a constraint failure may appear at flush/commit rather than at the setter call.

### Execution walkthrough

```java
@Transactional
public void rename(long tenantId, long id, String newName) {
    OrderEntity order = repository.require(tenantId, id); // managed
    order.rename(newName);                                // no explicit save needed
    auditRepository.add(...);                             // managed/persisted work
} // provider flushes; transaction manager commits or rolls back
```

If `rename` violates a length/check/unique constraint, failure can surface when SQL executes. An intermediate query may trigger flush earlier than the final method return. Do not catch a persistence exception and continue using the same transaction as if it were healthy; it may be marked rollback-only and the context can contain state that no longer represents database outcome.

### Bulk DML trap

JPQL/SQL bulk updates operate directly against database rows and do not behave like per-entity mutation with ordinary managed-state synchronization and callbacks. Existing managed entities can become stale.

```java
int changed = entityManager.createQuery("""
    update OrderEntity o
       set o.status = :newStatus
     where o.status = :oldStatus
    """)
    .setParameter("newStatus", CANCELLED)
    .setParameter("oldStatus", EXPIRED)
    .executeUpdate();
entityManager.clear(); // one deliberate policy after bulk work
```

The correct clear/refresh strategy depends on whether pending changes exist and what must remain managed. Flush intentionally before bulk DML if required; never clear away unsynchronized work accidentally.

## 4. `persist`, `merge`, and detached updates

`persist` makes a new entity managed under the context. `merge` copies state from its argument into a managed instance and returns that managed instance; the supplied object does not become managed merely because it was passed to `merge`.

```java
OrderEntity managed = entityManager.merge(detached);
assert managed != detached;
```

Blindly merging a web-bound detached entity is dangerous:

- missing fields may overwrite current values;
- stale associations can cascade;
- unauthorized fields can be changed;
- a concurrent update can be lost without a version check;
- the graph can cause unexpected loads/writes.

Prefer a command-specific transaction: load the authorized managed aggregate, call invariant-protecting methods with allowed fields, and rely on version/constraint guards. DTOs do not need entity annotations.

## 5. Entity equality and hashing

### The generated-ID problem

A new entity may have `id == null`; after persistence it receives an ID. If `hashCode()` changes while the object is in a `HashSet`, the set may no longer find it. If all transient instances with null ID are considered equal, distinct new entities collapse.

Equality options:

- immutable, assigned, globally/tenant-unique ID available at construction;
- immutable natural key whose meaning never changes;
- provider-aware generated-ID strategy documented and tested across transient/managed/detached/proxy states;
- identity equality for entities while value objects use value equality.

There is no universal five-line recipe independent of ID strategy, inheritance, proxies, and collection use. Requirements:

1. reflexive, symmetric, transitive, consistent;
2. equal objects have equal stable hash codes while stored in hash collections;
3. proxy/subclass comparisons do not break symmetry;
4. two distinct transient entities are not accidentally equal;
5. the chosen business key is immutable.

Records are excellent DTO/value types but usually a poor direct fit for mutable ORM entities with provider construction/proxy requirements. Keep value semantics where they belong.

### Interview example

If the service assigns a UUID order ID before persistence, equality can use that immutable ID from construction. If the database assigns a numeric identity, avoid putting a transient entity in a hash collection whose membership must survive ID assignment, or adopt a provider-recommended strategy and test lifecycle states. State the tradeoff instead of reciting code.

## 6. Fetching and the N+1 problem

### What N+1 means

Code loads `N` parent rows with one query, then accesses one lazily obtained association for each parent and triggers up to `N` more queries:

```java
List<OrderEntity> orders = repository.findRecent(tenantId); // 1 query
for (OrderEntity order : orders) {
    render(order.getCustomer().getName());                  // up to N
}
```

N+1 is an *execution-shape* problem, not simply a lazy/eager annotation problem. Making every relationship eager can create huge joins, extra secondary selects, circular graph loads, and unpredictable query cost across use cases.

Recognition signals:

- query count grows with result count;
- latency rises even though each query is individually fast;
- ORM statistics/log trace shows repeated SQL differing only by ID;
- serialization walks associations outside deliberate fetch planning;
- a repository returns entities for a read projection.

### Fetch-plan toolbox

| Tool | Best fit | Caveats |
|---|---|---|
| DTO/projection query | read API needs a fixed small shape | separate mapping/query; avoids managed graph |
| fetch join | one use case needs association in same query | duplicates/cartesian multiplication; collection pagination traps |
| entity graph | select associations per query through standardized graph concepts | provider SQL still needs inspection |
| batch fetching | many lazy references/collections can be fetched in groups | provider feature/config; reduces, does not necessarily eliminate queries |
| explicit secondary query | load parent IDs/page, then related rows in bounded query | assemble carefully; often robust for paginated collections |

### Cartesian explosion

Fetching two to-many collections in one join can multiply rows. An order with 10 lines and 5 tags can produce 50 result rows before ORM deduplication. Network, memory, sorting, and hydration costs remain even if Java returns one order object. Multiple bag/list fetches may have provider-specific restrictions.

Decision rule: fetch exactly the graph required by one use case. For read endpoints, projection often wins. For aggregate mutation, load the needed managed state in a bounded way. Assert query count or shape in tests for critical paths.

### Pagination plus collection fetch

Applying SQL limit to joined parent/child rows can cut through a parent's children or yield fewer distinct parents than requested. Providers may paginate in memory or reject/warn depending on query/version. A robust two-phase pattern is:

1. fetch one page of ordered parent IDs using a keyset query;
2. fetch required parents/children for those IDs;
3. reconstruct the requested parent order;
4. keep bounds explicit.

Do not combine collection fetch join and pageable abstraction without inspecting generated SQL and provider behavior.

## 7. JDBC batching and ORM batching

Batching reduces network round trips by grouping statements, but it does not change algorithmic row count or make one giant transaction safe. Provider ability to batch inserts can depend on ID generation, statement ordering, and driver/database support. Exact settings are Hibernate-specific.

For a large import:

```java
for (int i = 0; i < commands.size(); i++) {
    entityManager.persist(toEntity(commands.get(i)));
    if ((i + 1) % batchSize == 0) {
        entityManager.flush();
        entityManager.clear();
    }
}
```

This bounds persistence-context memory, but clearing detaches entities and changes later behavior. A huge outer transaction still holds locks/log state; chunk transaction boundaries may be appropriate if the business operation supports resumability and partial progress. Batch failures can report update counts in driver-specific ways. Test exact infrastructure.

## 8. Optimistic and pessimistic locking in JPA

### Optimistic version

`@Version` lets the provider include version checks in updates. Two contexts load version 7; one commits version 8; the second update affects no matching version and results in an optimistic-lock failure. The application must choose:

- surface a conflict to a user;
- reload and ask the user to reconcile;
- recompute and retry a commutative/internal operation;
- abandon under a deadline.

Do not catch and continue in the same failed transaction. A version on Order does not automatically protect an invariant across Inventory rows unless those rows/conflicts participate.

### Pessimistic lock

Pessimistic lock modes request database locking through the provider. Actual lock strength, range, wait/timeout, follow-on locking, and unsupported combinations depend on provider/database. Lock rows in deterministic order, keep transactions short, and classify timeout/deadlock separately from “not found.”

Use pessimistic locking when conflict is frequent and proceeding concurrently would waste expensive work, or when the invariant requires examining locked current state. Do not use it to mask an unbounded transaction or remote call.

## 9. Interview questions and model checkpoints

### Q1. Is a managed entity saved only when `save()` is called?

**Model checkpoint:** managed changes are tracked and synchronized at flush under the persistence context/transaction. Repository `save` conventions are framework-specific. Flush is not commit.

### Q2. What causes N+1, and how do you fix it?

**Model checkpoint:** accessing associations across N results triggers repeated queries. Choose a use-case fetch plan—projection, fetch join, entity graph, batch, or explicit second query—and verify generated SQL/query count. Global eager fetching is not a general fix.

### Q3. Why is `merge` risky for HTTP updates?

**Model checkpoint:** it copies detached state into a managed instance, potentially including stale/unauthorized fields and cascades. Load authorized current state and apply a command with version checks.

### Q4. When does flush happen?

**Model checkpoint:** explicit flush, transaction completion, and query-related synchronization according to flush mode/spec/provider. It executes SQL inside the transaction but does not commit.

### SDE-2 follow-ups

1. Design equality for a generated-ID entity used in a `Set`, including transient and proxy cases.
2. A page of 20 orders creates 421 queries. Produce a measurement-first remediation plan.
3. A fetch join returns 10,000 rows for 100 parents. Explain row multiplication and alternatives.
4. Compare `@Version` with an HTTP `If-Match` contract and show how they connect.

## 10. Exercises

1. Draw lifecycle state transitions for `persist`, `find`, `detach`, `merge`, `remove`, clear, close, rollback, and commit.
2. Write a test that demonstrates `merge` returns a different managed instance.
3. Build a two-phase paginated fetch for parents and children; preserve parent order and bound query count.
4. Run a batch insert with two ID strategies and compare statements, memory, and transaction time.
5. Reproduce an optimistic conflict with two independent transactions and classify the API response.

## 11. Summary checklist

- [ ] Specification, provider, and database guarantees are labeled separately.
- [ ] Entity state and context lifetime are explicit.
- [ ] Flush is not confused with commit.
- [ ] DTO updates are mapped onto authorized managed state.
- [ ] Equality remains valid across lifecycle and hash-collection use.
- [ ] Every query has a deliberate fetch plan.
- [ ] Collection joins account for row multiplication and pagination.
- [ ] Batching is measured and persistence-context memory is bounded.
- [ ] Locking strategy matches invariant scope and conflict rate.

## 12. Association ownership and read-model laboratory

### Owning side is a mapping concept

For a bidirectional association, JPA designates the side whose mapping controls the database relationship update. This does not automatically mean domain aggregate ownership. Keep both concepts explicit.

If `OrderLine` holds the foreign key, a common mapping makes the line's `@ManyToOne` the owning mapping side and `Order.lines` uses `mappedBy`. Application helper methods update both Java references:

```java
void addLine(OrderLineEntity line) {
    if (line.order() != null) {
        throw new IllegalArgumentException("line already belongs to an order");
    }
    lines.add(line);
    line.attachTo(this);
}
```

Changing only the inverse collection can leave the generated foreign-key update absent or in-memory graph inconsistent. Test SQL and reloaded state, not only the collection before flush.

### Cascade and orphan removal decision rule

Cascade answers which entity operation propagates across a relationship. Orphan removal expresses lifecycle ownership when removal from the relationship should delete the dependent entity. Use them when the child truly cannot exist independently.

An `OrderLine` commonly belongs to one Order and may be orphan-removed. A `Product` is shared; cascading remove from an order to product would be catastrophic. Many-to-many plus broad cascade is a red flag because ownership is ambiguous. Model the join as its own entity when it has attributes/lifecycle.

### Large collections

An aggregate method that loads 100,000 children to add one member is not bounded. Options:

- impose aggregate size invariant;
- use a targeted database command with constraints/versioning;
- split aggregate boundaries;
- represent a child repository/query separately;
- use bulk DML under explicit context-clear rules.

Do not expose a lazy collection as a JSON page. Pagination is a query contract, not a property getter.

### Read model versus managed graph

For `GET /orders/{id}/summary`, a projection query can return:

```java
record OrderSummary(long id, String status, long totalCents,
                    String customerDisplayName, long lineCount) {}
```

It avoids loading a mutable entity graph and makes selected columns/query cost explicit. A projection is not automatically fast: joins and aggregation still require an index/plan. But it reduces accidental lazy loads and serialization cycles.

Use managed entities when a transaction needs invariant-protecting state changes. Use projections for stable bounded reads. Do not update a projection and expect dirty checking.

### Fetch-plan test matrix

| Use case | Expected plan | Assertion |
|---|---|---|
| list 20 order summaries | one bounded projection query | query count constant; selected fields only |
| edit one order note | entity by tenant+id/version | one select plus one versioned update |
| display order with 20 lines | fetch join or bounded two-query plan | no N+1; row count bounded |
| page orders with lines | page IDs then fetch children | exactly page-size parents; stable order |
| export 1M orders | streaming/chunked projection under job protocol | context/memory bounded; resumable |

SQL logging in tests can be noisy and brittle; provider statistics or a datasource proxy can count statements. Still inspect representative SQL and plans. A test that asserts exactly one provider-internal query forever may resist legitimate upgrades; assert the performance contract at the right granularity.

### Laboratory checkpoint

When given an ORM performance problem, state context lifetime, entity state, access path, association cardinality, generated query count, rows returned, transaction/flush point, and database plan. Changing `LAZY` to `EAGER` without that model is guesswork.

### Serialization-boundary checkpoint

Never hand a managed entity directly to a general JSON serializer and assume the output is stable. Accessor traversal can initialize lazy associations, create N+1 queries, follow cycles, expose internal fields, or fail after the context closes. Provider proxy types and enhancement details can also leak into behavior.

Map inside a deliberate read transaction to a bounded DTO/projection. Decide whether absent association means `null`, omitted field, empty collection, or inaccessible resource. Put a maximum on nested collection size or expose it as its own paginated resource. Test SQL count and serialized contract together.

An “open persistence context in view” policy can keep lazy loading available through rendering, but it broadens the query/connection timing boundary and makes controller/serializer access generate SQL. If selected, document and instrument it; it does not replace fetch planning. Many services disable or avoid reliance on it so repository/query code owns data access visibly.

**Model answer:** entities model persistence lifecycle; API DTOs model compatibility and trust. Explicit mapping is not useless boilerplate—it is the place where authorization, projection, version, units, nullability, and evolution become reviewable.

## Primary references

- Jakarta Persistence specification and API: <https://jakarta.ee/specifications/persistence/>
- Hibernate ORM User Guide: <https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html>
- Spring Data JPA Reference: <https://docs.spring.io/spring-data/jpa/reference/>

> **Version boundary:** Jakarta Persistence contracts are distinct from Hibernate extensions. Fetch batching, proxy/enhancement behavior, generated SQL, supported hints, ID batching, and pagination safeguards vary by provider release. Select documentation that matches the managed dependency version. Examples assume Java 21 and Jakarta namespaces.
