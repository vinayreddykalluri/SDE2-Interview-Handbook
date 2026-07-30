# Derived Query Methods and Caveats

Derived query methods are fast to write and easy to read. They are not free.

## How derived methods are built

Method names are parsed into predicates and ordering clauses.

```text
findByStatusAndPriorityGreaterThanOrderByCreatedAtDesc
```

That single line expresses status, numeric filter, and descending order.

## Why naming can still fail interviews

People memorize fragments but miss intent and runtime consequences:

- Missing sort is equivalent to nondeterministic order.
- Implicit joins may force large query plans.
- `Or` and nested groups can produce unexpectedly broad results.
- Parameter naming that changes does not change runtime behavior.

## Mini dry run

Input: `findByStatusAndCreatedAtAfterOrderByCreatedAtDesc("OPEN", cutoff)`

Output behavior:

- Filter on status and timestamp.
- Return newest-open items first.
- Stable only if secondary tie-breakers are explicit (for example `ThenById`).

If an index is missing on `status`, query cost can still be high.

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

Explain whether this has ambiguous precedence and what safer form is easier to audit.

Expected: group criteria explicitly and prefer documented `Specification` for complex boolean logic.

## Quick check

1. What runtime behavior is hidden behind a long derived name?
2. Why should you name deterministic sort keys explicitly?
3. When should you switch to declared/typed queries?

## Practice

- **Foundation:** Convert one vague method name into explicit predicates and sort.
- **Interview Core:** Rewrite a long derived method as two simpler repository methods.
- **SDE-2 Follow-up:** Diagnose whether a specific derived method can use a covering index.
