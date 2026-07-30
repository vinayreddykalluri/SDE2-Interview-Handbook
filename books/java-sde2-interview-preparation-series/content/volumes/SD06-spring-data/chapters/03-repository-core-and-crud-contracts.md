# Repository Core and CRUD Contracts

Spring Data repository style is useful because it makes CRUD semantics obvious. It becomes dangerous when default semantics are inherited blindly.

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

## Behavioral semantics you should name

- `save` should respect aggregate invariants and validation rules.
- `find` should define what happens when no result exists.
- `delete` should define soft-delete, hard-delete, and audit behavior.
- List operations should define a stable order and bounded size.

## Method naming pattern preview

```text
findBy{field}           -> null/Optional choice is explicit
getBy{field}            -> missing row becomes defined exception path
deleteBy{field}         -> usually requires pre-checks
countBy{field}          -> cheap metric for existence and health checks
existsBy{field}         -> fast existence path, but still not a row payload
```

For SDE-2 discussions, explicit `Optional`/exception behavior is often clearer than implicit null handling.

## Common confusion

- `findAll()` without limit is rarely interview-safe for endpoints.
- `delete` by relation fields can silently remove multiple rows if naming is ambiguous.
- `save` on detached objects needs clear merge/attach policy.

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
