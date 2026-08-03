# Repository APIs and Honest CRUD Contracts

Spring Data repository style removes repeated adapter code. It becomes dangerous when a broad inherited API is mistaken for a domain contract.

## Start with the Commons hierarchy

The important interfaces are capabilities, not maturity levels:

| Interface | Adds | Interview caution |
|---|---|---|
| `Repository<T, ID>` | marker and selective method exposure | use when a narrow contract matters |
| `CrudRepository<T, ID>` | save, identity lookup, existence, count, delete | methods do not define aggregate policy or endpoint safety |
| `ListCrudRepository<T, ID>` | list-shaped variants of multi-result CRUD | `findAll` is still unbounded |
| `PagingAndSortingRepository<T, ID>` | `Pageable` and `Sort` access | ordering and count cost remain explicit decisions |
| `JpaRepository<T, ID>` | JPA-oriented operations including flush/batch helpers | exposes ORM lifecycle details; do not leak it directly to controllers |

Store modules can add or omit behavior. Program to the smallest useful interface, and expose application-specific methods from the service boundary.

## The baseline contract

For each repository, interview-level clarity usually starts with these intents:

1. **Fetch by identity** (`findById`, `existsById`, `getBy...`)
2. **Create/update by command** (`save`)
3. **Delete when legal** (`deleteBy...`, `delete`)
4. **List by bounded window** (explicit sort + pagination)

Each intent has a failure mode:

- missing row,
- duplicate identity,
- invalid transition,
- stale snapshot,
- write lost under concurrency.

## Missing-result semantics

Do not infer behavior only from `find`, `get`, or `read` in a derived method name.

```java
interface OrderRepository extends Repository<OrderEntity, Long> {
    Optional<OrderEntity> findByRequestKey(String requestKey);
    OrderEntity findRequiredByExternalId(String externalId);
}
```

For supported Spring Data modules, an `Optional` communicates normal absence. A non-wrapper single result may be nullable under the module's nullability rules or may trigger an empty-result exception when declared non-null. Multiple matches can raise an incorrect-result-size exception. `JpaRepository.getReferenceById` is different again: it can return a lazy JPA reference whose missing row is discovered only when state is accessed. Verify the exact module/version contract and translate it at the application boundary.

## Existence is not counting

Use `existsBy...` to ask whether at least one matching row exists. Use `countBy...` only when the count is the required result.

```sql
-- existence intent; an engine can stop after the first match
select 1 from customer_order where request_key = ? limit 1;

-- counting intent; all matching rows contribute
select count(*) from customer_order where status = ?;
```

Generated SQL is store/provider specific, so inspect it. Neither operation is an application health check: health should test the service's readiness contract with bounded work, not count a business table.

## Behavioral semantics you should name

- The application service must establish aggregate invariants before `save`; the repository does not invent them.
- `find` should define what happens when no result exists.
- `delete` should define soft-delete, hard-delete, and audit behavior.
- List operations should define a stable order and bounded size.

## Method naming pattern preview

```text
findBy{field}           -> absence follows the declared return/nullability contract
getBy{field}            -> another query subject; the prefix alone does not promise an exception
getReferenceById        -> JPA reference; access can fail later if the row is missing
deleteBy{field}         -> scope and returned count/result must be deliberate
countBy{field}          -> exact count intent, potentially more work than existence
existsBy{field}         -> existence intent; still requires an index and bounded predicate
```

For SDE-2 discussions, explicit `Optional`/exception behavior is often clearer than implicit null handling.

## Common confusion

- `findAll()` without limit is rarely interview-safe for endpoints.
- `delete` by relation fields can silently remove multiple rows if naming is ambiguous.
- `save` on detached objects needs clear merge/attach policy.

## Failure and edge matrix

| Case | Observable result | Design response |
|---|---|---|
| no single result | empty wrapper, null, or exception by contract | normalize at service boundary |
| multiple rows for a single-result query | incorrect-result-size failure | protect uniqueness in the database |
| duplicate insert | often discovered at flush/commit | classify the known constraint and roll back |
| detached entity passed to JPA `save` | provider may merge a copied state graph | prefer load-and-mutate command handling |
| broad derived delete | many rows and locks | require tenant/identity scope and assert affected count |
| `findAll` on growing table | unbounded memory, context, and response | bounded projection/page/cursor |

## Quick check

1. Why is `findAll()` usually a scaling smell?
2. Which method names can accidentally hide an update-by-accident bug?
3. When is returning `Optional` clearer than returning null?

## Debugging exercise

A ticket service uses `deleteByCustomerId(String)` and runs at high scale.

- What is the hidden risk?
- How do you correct it with safer method naming and preconditions?

Expected correction: restrict delete scope, assert caller intent, and validate count/range before deletion.

## Practice

- **Foundation:** Rewrite three repository methods to add explicit return types and failure behavior.
- **Interview Core:** Choose between `find`, `get`, and `exists` for two use cases.
- **SDE-2 Follow-up:** Explain a production issue caused by an overly broad `deleteBy...` method.

## Interviewer question and model answer

**Interviewer:** What is the difference between `findById`, `getReferenceById`, `existsById`, and `countByStatus`?

**Model answer:** `findById` performs identity lookup and represents absence with `Optional`. In JPA, `getReferenceById` may return a lazy reference without reading the row immediately, so missing data can fail when the reference is initialized. `existsById` asks a boolean question and may use an existence-shaped query. `countByStatus` must calculate the number of matches and should not be used when I only need existence. I still inspect generated SQL and indexes, and I never expose these methods directly as an unbounded HTTP contract.
