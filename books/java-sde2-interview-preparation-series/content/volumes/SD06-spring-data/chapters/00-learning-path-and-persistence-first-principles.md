# Learning Path and Persistence First Principles

Spring Data is useful when you already understand one thing: **all persistence has an explicit contract**.
Repositories are only the last mile of that contract.

Repository code is convenient for CRUD and simple filters, but repository methods are not the storage system.
The interview objective is to preserve database truth even when repository abstractions feel easier.

## Why this volume exists

- You already know Java fundamentals and Spring Core.
- You know how transactional code behaves in principle.
- You need a clear path from repository methods to SQL/MongoDB/Redis behavior.

## Learning path map

```text
Domain model -> Aggregate boundaries -> Repository contract -> Query strategy ->
Transaction + flush + isolation decisions -> Pagination + sorting + locks ->
Template fallback when abstraction leaks -> Testing with real stores -> Trade-off discussion
```

## What this book is and is not

This book teaches:

- Correctly defining repository contracts.
- Reading repository behavior as a policy layer, not storage truth.
- Where query derivation helps and where it hides cost.
- Debugging interview-ready snippets for correctness and performance.

This is not a deep dive into internals of query parser/optimizer, Spring internals, or complete admin operations.

## Core invariant for interviews

In every interview explanation, separate three layers:

1. **Method contract** — Java method names, arguments, return type, exception profile.
2. **Behavior contract** — SQL/NoSQL query, filters, sort, and projection.
3. **Failure contract** — what happens on duplicate keys, missing rows, lock timeout, stale data.

If you can state these three layers before writing code, you are interview-ready for Spring Data discussions.

## Quick check

1. Why can repository code feel too simple during interviews?
2. Which behavior is always database behavior, not repository behavior?
3. What should be in a repository boundary before adding method names?

## Practice

- **Foundation:** Identify the aggregate boundaries in a simple `Order` service and write a three-method repository contract.
- **Foundation:** Map repository method return type decisions for `find` vs `get` variants.
- **Interview Core:** Give a non-technical explanation of repository convenience vs storage guarantees.
- **SDE-2 Follow-up:** Explain how repository choice changes rollback reasoning, monitoring, and performance.

## Readiness checkpoint

Move to Chapter 1 when you can answer: "If a repository method returns a `List`, what are the behavior questions not answered by that method signature?".
