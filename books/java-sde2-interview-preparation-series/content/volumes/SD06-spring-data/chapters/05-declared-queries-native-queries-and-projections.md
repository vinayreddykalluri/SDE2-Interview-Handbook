# Declared Queries, Native SQL, and Projections

Declared queries keep behavior readable while enabling fine control.

## Three query channels

1. **Derived methods:** generated from method names.
2. **Declared queries (`@Query`):** explicit, readable, often portable.
3. **Native queries:** full control, vendor-specific power, stronger responsibility.

## Choosing the right channel

Use declared/native query when:

- you need explicit joins,
- you need aggregation or windowed sorting,
- return shape needs fields not present in entity graph,
- optimizer behavior must be controlled.

Use derived when:

- query stays inside method-level intent,
- team clarity is more important than SQL novelty,
- execution remains bounded.

## Projection pattern

Projection reduces payload and accidental update risk.

```text
Entity table (wide)
    |
    +---> projection interface/class
         (selected columns only)
```

If projection includes computed fields, confirm if they are DB-native or application computed.

## Common trap

- Native query changes with each DB version.
- Declared query may still load extra columns unless projection is correct.
- Projections can hide expensive joins behind cheap-looking method names.

## Quick check

1. Why does a projection not automatically mean performance is good?
2. Which layer owns SQL portability checks for native query?
3. When should you avoid native query in interviews?

## Debugging exercise

Two queries fetch the same fields, one derived and one declared.

Latency jumps 4x in production.

Explain the 4 checks you run first.

Expected checks:

- compare execution plans,
- verify indexes,
- validate sort + filter order,
- confirm projection columns and cardinality.

## Practice

- **Foundation:** Write one declared query equivalent to a derived method.
- **Interview Core:** Name why native queries should include test data with vendor versions.
- **SDE-2 Follow-up:** Explain projection risks with entity-to-DTO mapping.
