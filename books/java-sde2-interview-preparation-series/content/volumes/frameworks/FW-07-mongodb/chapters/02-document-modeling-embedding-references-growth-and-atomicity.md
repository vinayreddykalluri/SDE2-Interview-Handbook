# Document Modeling: Embedding, References, Growth, and Atomicity

MongoDB modeling begins with operations, not entity diagrams. The same real-world relationship may be embedded, referenced, duplicated as a snapshot, or represented by a separate event collection depending on lifecycle and access.

## Embed when the boundary agrees

Embedding fits when children:

- are normally read with the parent;
- have bounded count/size;
- share lifecycle and ownership;
- must change atomically with the parent;
- are not independently queried or shared heavily.

Order lines fit many of these properties. An unbounded chat history does not.

## Reference when facts evolve independently

Reference when data:

- has independent lifecycle/authorization;
- is shared by many aggregates;
- grows without a safe bound;
- is queried independently;
- changes at a different frequency.

Avoid recreating relational normalization automatically. A reference means the application may issue another query/aggregation, handle absence, and accept consistency between documents.

## Deliberate duplication

An order can retain `shippingAddressSnapshot` and `unitPriceCents`. They represent facts at purchase time. For a duplicated current customer name, define:

1. authoritative source;
2. acceptable staleness;
3. propagation mechanism;
4. reconciliation and repair;
5. behavior during partial failure.

## Growth patterns

### Unbounded arrays

Embedding every event, follower, or reading eventually hits document-size limits and makes each update/read progressively heavier. Use:

- bounded recent window plus archive collection;
- bucket pattern by time/count;
- child collection with parent ID and index;
- summary counters with recoverable source events.

### Hot documents

A single global counter serializes writes and concentrates replication. Stripe counters across buckets and sum them when approximate/mergeable semantics permit, or choose another design. Do not shard away an invariant without defining how it is restored.

## One-to-many choices

| Shape | Write/read effect | Best fit |
|---|---|---|
| child embedded in parent | one atomic write/read, parent grows | bounded owned aggregate |
| parent ID on child | independent writes, extra query | large/unbounded children |
| child IDs in parent | parent array grows, extra lookup | bounded references where list ownership matters |
| duplicated summary | fast read, propagation needed | explicit staleness budget |

## Atomic command design

Use update operators and predicates instead of load-modify-replace:

```javascript
db.inventory.updateOne(
  { sku: "BOOK-1", available: { $gte: 2 } },
  { $inc: { available: -2, version: 1 } }
)
```

This prevents overselling for one inventory document. A multi-SKU reservation spans documents and requires either a transaction, compensating reservation state, or a redesigned aggregate. State the cost and failure behavior.

## Multi-document transactions

Transactions exist for replica sets and sharded clusters, but they add coordination, runtime/resource limits, and failure/retry complexity. They should preserve a real cross-document invariant—not rescue a model that needlessly split one aggregate.

The driver transaction callback may retry, so the body must not send emails or call payment APIs without idempotency. Use an outbox/state document and perform external work after commit.

## Polymorphic document schemas

Different document shapes in one collection can simplify queries when they share access and indexes. They can also create sparse fields, branching mapping code, and broad validation. Include a `schemaVersion` or stable discriminator when migration/decoding needs it; do not make every read support indefinite historical shapes.

## Modeling failure matrix

| Symptom | Modeling cause | Repair |
|---|---|---|
| document near size limit | unbounded embedded history | bucket/archive/separate children |
| one counter dominates latency | hot document | shard/stripe or change invariant |
| many round trips | over-referencing aggregate | embed bounded owned data |
| massive rewrites | frequently changed value duplicated everywhere | reference or async projection with repair |
| transaction everywhere | aggregate split by table-thinking | reconsider boundary |

## Practice

- **Foundation:** Model blog post and last 20 comments; explain where older comments go.
- **Interview Core:** Model product catalog price versus order price snapshot.
- **SDE-2 Follow-up:** Design a 100-million-member community without one unbounded member array.

## Solution direction

Keep bounded recent comments or reference comment documents; persist agreed order price in the order; represent memberships as separate documents keyed/indexed by community and member, then page them and maintain repairable counts.
