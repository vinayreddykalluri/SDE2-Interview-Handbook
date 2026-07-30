# Common Interview Traps for Spring Data

Interviewers rarely ask for one repository method name. They ask whether behavior is safe under real constraints.

## Top traps

1. Assuming `findAll()` is harmless for large data.
2. Forgetting deterministic sort.
3. Ignoring repository method side effects in transactions.
4. Using broad delete methods.
5. Missing pagination or cursor semantics.
6. Treating optimistic lock exceptions as transient and never handling retries.
7. Believing native queries are automatically faster.
8. Assuming soft-delete means secure-by-default.

## Code-read trap

When you read one long repository method name, inspect:

- null checks,
- sorting, including tie-breakers,
- return type,
- checked exceptions in callers,
- surrounding transaction boundaries.

## Fast interview answer framework

For each trap, answer:

- What is the hidden behavior?
- Why can it fail in production?
- What one safer design change solves it?

## Debugging exercise

Method: `List<User> findAllByCreatedAtAfterOrderByCreatedAt(Date from);`

Observed duplicate rows after repeated calls.

What do you fix?

Expected: add stable secondary order (`OrderByCreatedAtDesc,IdDesc`), add index on `(created_at, id)`, add bounded pagination.

## Practice

- **Foundation:** Pick two traps and map cause-and-fix.
- **Interview Core:** Turn one trap into one precise interview answer.
- **SDE-2 Follow-up:** Explain when repository convenience hides isolation-level risk.
