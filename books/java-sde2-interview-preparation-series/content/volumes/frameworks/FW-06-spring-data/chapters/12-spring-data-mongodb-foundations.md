# Spring Data MongoDB Foundations

Spring Data MongoDB is useful when document modeling is the right fit, but document stores require explicit modeling trade-offs.

## The mental shift

In SQL, joins are normalized by default.

In MongoDB, many relationships are denormalized by design.

The real question is not “SQL or NoSQL?” It is whether one bounded document can own the data and serve the important access patterns without unbounded growth or frequent cross-document invariants.

## Core decisions for interviews

1. **Embedded vs referenced documents:** embed for read-side locality, reference when independent lifecycle is required.
2. **Indexing fields:** every filter and sort field should be indexed with access pattern in mind.
3. **Update model:** use atomic field updates over full-document writes when possible.

## Repository mapping notes

Document repositories are familiar, but behavior differs:

- a single-document write is atomic; multi-document transactions are available on supported replica-set or sharded deployments but add coordination and do not repair a poor document boundary,
- large arrays and in-place updates need careful `$push/$set` usage,
- ordering of projections in large arrays is storage-dependent behavior.

Start with the native operation, then choose the abstraction:

```javascript
db.orders.updateOne(
  { _id: orderId, version: expectedVersion },
  { $set: { status: "PAID" }, $inc: { version: 1 } }
)
```

`MongoTemplate` can express explicit update operators and inspect the matched/modified counts. A repository `save` of an entire document is not always the correct substitute for an atomic field update.

## Failure matrix

| Failure | Evidence/response |
|---|---|
| matched count is zero | missing document or version conflict; distinguish deliberately |
| duplicate key | classify the known unique index |
| transient transaction label | retry the whole transaction under MongoDB driver guidance and deadline |
| document/array grows without bound | redesign boundary or bucket/reference data |
| secondary read is stale | state read preference/read concern and client-visible guarantee |

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

## Interviewer question and model answer

**Interviewer:** MongoDB supports transactions, so why care about embedding?

**Model answer:** Transactions make some multi-document changes possible on supported deployments, but they add coordination, latency, and failure handling. I still model the common invariant and read path inside one bounded document when that lifecycle fits. I reference independent or unbounded data. Then I define indexes, document growth, read/write concerns, and whether an atomic update with a version predicate is clearer than loading and saving the whole document.
