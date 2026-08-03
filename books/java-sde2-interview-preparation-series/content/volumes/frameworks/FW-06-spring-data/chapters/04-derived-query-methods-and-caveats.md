# Derived Query Methods and Caveats

Derived query methods are fast to write and easy to read. They are not free.

## How derived methods are built

Method names are parsed into predicates and ordering clauses.

```text
findByStatusAndPriorityGreaterThanOrderByCreatedAtDesc
```

That single line expresses status, numeric filter, and descending order.

At repository creation, the store module parses the subject (`find`, `exists`, `count`, `delete`, `First`, `Top`) and predicate. It resolves property paths against the domain type and selects a module-specific query implementation. A misspelled property normally fails application startup rather than waiting for the first request.

```java
interface OrderRepository extends Repository<OrderEntity, Long> {
    Slice<OrderSummary> findByStatusOrderByCreatedAtDescIdDesc(
            String status, Pageable page);

    boolean existsByTenantIdAndRequestKey(long tenantId, String requestKey);
}
```

`Slice` can answer whether another window exists without promising a total count. The explicit `id` tie-breaker makes equal timestamps deterministic.

## Why naming can still fail interviews

People memorize fragments but miss intent and runtime consequences:

- Missing sort is equivalent to nondeterministic order.
- Implicit joins may force large query plans.
- `And` and `Or` follow the query parser's grouping rules, but method names cannot express arbitrary parentheses clearly.
- Parameter naming that changes does not change runtime behavior.

## Mini dry run

Input: `findByStatusAndCreatedAtAfterOrderByCreatedAtDesc("OPEN", cutoff)`

Output behavior:

- Filter on status and timestamp.
- Return newest-open items first.
- Stable only if secondary tie-breakers are explicit (for example `ThenById`).

If an index is missing on `status`, query cost can still be high.

Likely relational shape, subject to provider and dialect:

```sql
select ...
from customer_order
where status = ? and created_at > ?
order by created_at desc, id desc
limit ?;
```

The Java name cannot prove that the index supports this filter/order or that the projection is narrow.

## Interview guidance

Use derived methods for:

- clear single-aggregate rules,
- high readability,
- low query variance.

Avoid derived methods for:

- complex boolean groups,
- vendor-specific functions,
- heavy joins,
- pagination requiring deterministic cross-field ordering.

## Debugging exercise

Repository method: `findByNameContainingOrDescriptionContainingAndPriorityBetweenOrderByCreatedAt(String a, String b, int min, int max)`

Explain the parser grouping and why the business intent is still hard to audit.

Expected: the derived grammar treats the `And` part as belonging to the second `Or` branch, approximately `name contains a OR (description contains b AND priority between min and max)`. If priority must apply to both text fields, a long method name cannot add those parentheses naturally. Use a declared query or a tested `Specification` that expresses `(name OR description) AND priority` explicitly.

## Quick check

1. What runtime behavior is hidden behind a long derived name?
2. Why should you name deterministic sort keys explicitly?
3. When should you switch to declared/typed queries?

## Practice

- **Foundation:** Convert one vague method name into explicit predicates and sort.
- **Interview Core:** Rewrite a long derived method as two simpler repository methods.
- **SDE-2 Follow-up:** Diagnose whether a specific derived method can use a covering index.

## Interviewer question and model answer

**Interviewer:** Are derived query methods evaluated in Java after loading entities?

**Model answer:** Normally no. The repository factory parses the method into a store query, and Spring Data JPA asks the provider to execute JPQL/SQL. I confirm the actual statement because joins, case handling, null parameters, pagination, and count queries vary by method and module. If the name hides complex grouping or performance, I switch to an explicit query or custom fragment and test the generated SQL and result order.
