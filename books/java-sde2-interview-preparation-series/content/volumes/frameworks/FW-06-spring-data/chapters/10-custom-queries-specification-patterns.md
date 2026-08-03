# Custom Queries and Specification Patterns

At some point derived methods become brittle. Specification-style composition helps when predicates change per request.

## Why this exists

Feature flags and filter dashboards create combinational query logic. Repeating derived names for every combination does not scale.

## Pattern model

```text
Input filters -> predicate composer -> validated query -> repository execution -> response page
```

Use this when:

- filter combinations exceed a few variants,
- business rules cross aggregate boundaries,
- sorting and paging must remain stable across dynamic conditions.

## Construction advice

- Build pure predicate builders.
- Keep each rule independently testable.
- Always preserve deterministic ordering for pageable results.

Spring Data JPA's `JpaSpecificationExecutor` composes JPA Criteria predicates:

```java
static Specification<OrderEntity> hasStatus(String status) {
    return (root, query, criteria) ->
            status == null ? criteria.conjunction()
                    : criteria.equal(root.get("status"), status);
}

Specification<OrderEntity> filter = hasStatus(status)
        .and(createdAtOrAfter(from))
        .and(tenantIs(tenantId));
```

Always add tenant/authorization scope server-side. Do not let a nullable user filter remove a mandatory security predicate. Dynamic predicates still become SQL; optional `OR` patterns and functions can make an index unusable.

## Minimal pseudo flow

- `status == OPEN`
- `amount between minAmount and maxAmount`
- `createdAt >= from` and `< to`
- `priority in ('HIGH','URGENT')`

Result: one composed query path rather than 24 method variants.

## Common mistake

- Building one huge specification that re-evaluates conditions in the wrong order and breaks index use.
- Passing unvalidated request values into query predicates.
- Accepting arbitrary property names for sort/path traversal.
- Reusing one mutable predicate builder across requests.

## Edge matrix

| Input | Required behavior |
|---|---|
| no optional filters | bounded tenant-scoped query, never global `findAll` |
| invalid range | reject before query construction |
| empty collection filter | define “no matches” versus “ignore filter” explicitly |
| unknown sort field | reject through allow-list |
| large `IN` list | cap, stage, or redesign; do not generate unbounded SQL |

## Quick check

1. Why do derived methods become hard to maintain with dynamic filters?
2. How do you keep specification code readable?
3. What test proves predicate composition is safe?

## Debugging exercise

A search endpoint accepts optional `status`, `priority`, and date range and has wrong results with null combinations.

Identify the first three validation checks.

Expected: null-safe condition builder, validated ranges, explicit sort defaults + tests for boundary nulls.

## Practice

- **Foundation:** Draft three optional filters and map them to composable predicates.
- **Interview Core:** Explain how composition avoids method explosion.
- **SDE-2 Follow-up:** Show where you validate user input before query building.

## Interviewer question and model answer

**Interviewer:** Are Specifications automatically efficient because the database optimizes them?

**Model answer:** No. A Specification only composes Criteria predicates. The resulting SQL can still contain broad `OR`, functions on indexed columns, large `IN` lists, joins, and an expensive count query. I validate and cap filters, always add tenant scope, fix the sort to a total order, inspect SQL/plans on representative data, and use an explicit read model or JDBC when the dynamic shape stops being predictable.
