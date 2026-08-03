# MongoDB for Java Backend Interviews

## Learning Path and Document-First Principles

MongoDB stores BSON documents in collections. That sentence is simple; the engineering work is deciding which facts belong in one atomic document, which access patterns need indexes, and which consistency/durability guarantees the application actually requests.

> **From Vinay:** Do not choose MongoDB because JSON feels natural in JavaScript or because “the schema changes often.” Begin with the operations the service must make correct. A document is valuable when it is the unit you read, update, and protect together.

## What this book builds

```text
requirements + access patterns
  -> document boundary and validation
  -> CRUD and atomic update shape
  -> index and query-plan evidence
  -> aggregation and pagination
  -> replication + read/write concern
  -> transactions and retryable failure
  -> sharding and operations
  -> Java driver / Spring Data boundary
```

The MySQL volume taught relational invariants. This volume keeps that discipline while changing the model: some relationships become embedded arrays or subdocuments; others remain references because they grow independently or have different lifecycle/ownership.

## One running domain

```javascript
{
  _id: ObjectId("..."),
  customerId: "c-42",
  status: "CREATED",
  version: NumberLong(0),
  createdAt: ISODate("2026-08-02T10:00:00Z"),
  lines: [
    { sku: "BOOK-1", quantity: 2, unitPriceCents: NumberLong(2500) }
  ],
  totals: { subtotalCents: NumberLong(5000), currency: "USD" },
  requestKey: "req-8fd"
}
```

The order and its lines are often one aggregate: they are read together, have bounded growth, and must change atomically. Customer details are referenced because the customer has an independent lifecycle. `unitPriceCents` is a purchase-time fact, not an accidental copy of the current catalog price.

## Single-document atomicity first

MongoDB writes are atomic at one document. Encode state preconditions in an update:

```javascript
db.orders.updateOne(
  { _id: orderId, status: "CREATED", version: 3 },
  {
    $set: { status: "PAID", paidAt: new Date() },
    $inc: { version: 1 }
  }
)
```

`matchedCount: 0` is not success. It means missing document, wrong state, or stale version. Read only when the API must distinguish those cases.

## The SDE-2 answer frame

1. **Access pattern:** What does one read/write request?
2. **Boundary:** Which fields must change atomically and how can the document grow?
3. **Index:** Which filter and sort must be supported?
4. **Consistency:** What read concern, write concern, and read preference are required?
5. **Failure:** Can a retry duplicate or reorder a side effect?
6. **Scale:** What is the shard key and hot-key distribution?
7. **Operations:** How do we migrate, observe, back up, and restore?

## First failure matrix

| Event | What is known | Safe response |
|---|---|---|
| duplicate key | unique invariant rejected write | return conflict/idempotent prior result |
| network error during write | outcome may be unknown | retry only retryable/idempotent operation or reconcile by key |
| primary steps down | in-flight operation can fail | driver discovers new primary; classify and bound retry |
| secondary read lags | read can omit latest write | use primary/causal session/stronger contract where required |
| document hits size/growth limit | update fails or model degrades | bound arrays and split independent history |

## Quick check and practice

1. Why is “schema flexible” not “schema absent”?
2. Which guarantee comes for free inside one document?
3. Why should an update contain the expected state/version?

- **Foundation:** List three reads and three writes for the order domain.
- **Interview Core:** Mark which facts belong inside the order and which do not.
- **SDE-2 Follow-up:** Reconcile a timed-out order creation using a unique request key.
