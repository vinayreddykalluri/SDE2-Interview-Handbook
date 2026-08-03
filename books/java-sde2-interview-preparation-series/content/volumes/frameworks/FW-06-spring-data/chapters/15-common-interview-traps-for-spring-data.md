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
9. Assuming `getBy...` universally throws when no row exists.
10. Counting when the requirement is only existence.
11. Mixing a cursor with a numeric offset.
12. Treating `REQUIRES_NEW` as retry.
13. Retrying a stale entity without re-reading/reapplying the command.
14. Assuming MongoDB has no transactions or that transactions remove modeling costs.
15. Claiming lock escalation is a portable database/JPA behavior.

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

Method: `Page<User> findAllByCreatedAtAfterOrderByCreatedAtDesc(Date from, Pageable page);`

Observed duplicate and skipped rows while clients traverse offset pages during inserts.

What do you fix, and what can no live pagination method promise?

Expected: add a stable secondary order (`OrderByCreatedAtDescIdDesc`), align an index to the filter/order, and use a keyset cursor when deep/live traversal semantics fit. Explain that live cursor traversal is not a snapshot when sort keys mutate; an exhaustive export may need snapshot/materialization semantics.

## Practice

- **Foundation:** Pick two traps and map cause-and-fix.
- **Interview Core:** Turn one trap into one precise interview answer.
- **SDE-2 Follow-up:** Explain when repository convenience hides isolation-level risk.

## Interviewer question and model answer

**Interviewer:** A repository method is one line and passes unit tests. What evidence do you still request before approving a hot endpoint?

**Model answer:** I request the generated SQL or native command, parameters by shape, projection width, deterministic order, result bound, query/count statement count, target-engine plan with representative distribution, transaction and lock duration, pool demand, and failure behavior for missing, duplicate, timeout, and concurrent updates. Then I add a regression test for the specific contract. Repository brevity is not performance or correctness evidence.
