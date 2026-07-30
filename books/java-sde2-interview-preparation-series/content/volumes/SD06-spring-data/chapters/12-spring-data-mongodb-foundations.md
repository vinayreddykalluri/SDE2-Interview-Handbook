# Spring Data MongoDB Foundations

Spring Data MongoDB is useful when document modeling is the right fit, but document stores require explicit modeling trade-offs.

## The mental shift

In SQL, joins are normalized by default.

In MongoDB, many relationships are denormalized by design.

## Core decisions for interviews

1. **Embedded vs referenced documents:** embed for read-side locality, reference when independent lifecycle is required.
2. **Indexing fields:** every filter and sort field should be indexed with access pattern in mind.
3. **Update model:** use atomic field updates over full-document writes when possible.

## Repository mapping notes

Document repositories are familiar, but behavior differs:

- transactions require replica set context for stronger guarantees,
- large arrays and in-place updates need careful `$push/$set` usage,
- ordering of projections in large arrays is storage-dependent behavior.

## Interview-ready mini model

```text
Order document
   |
   +-- items[] (embedded)
   +-- totals (immutable-ish)
   +-- status audit timeline (subdocument)
```

If every query needs `items` and `statusHistory`, evaluate size and write amplification.

## Quick check

1. Why can a document model simplify read APIs?
2. What is one risk of deeply embedded arrays?
3. How does Mongo transaction behavior differ from relational transaction assumptions?

## Debugging exercise

Read endpoint gets slow after adding a nested array field.

List three first checks before redesigning the schema.

Expected:

- index coverage,
- document shape growth,
- projection and projection-only query path.

## Practice

- **Foundation:** Draw one embedded and one referenced document alternative.
- **Interview Core:** Choose one model for order lines with read-heavy dashboards.
- **SDE-2 Follow-up:** Explain safe concurrency strategy for partially updated arrays.
