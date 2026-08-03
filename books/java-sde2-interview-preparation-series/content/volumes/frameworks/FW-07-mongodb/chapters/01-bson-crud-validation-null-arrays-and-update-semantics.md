# BSON, CRUD, Validation, Nulls, Arrays, and Update Semantics

## BSON is typed binary data

BSON distinguishes strings, booleans, dates, binary values, documents, arrays, object identifiers, and several numeric types. A Java `long` should not silently become a double when exact range matters. Choose the driver codec/mapping deliberately and test legacy documents containing older types.

Field order usually should not carry domain meaning. Document equality and some command behavior can be order-sensitive, so avoid treating raw BSON as an unordered Java map in low-level tests.

## Missing and null are different states

```javascript
{ nickname: null }
{ /* nickname is missing */ }
```

`{ nickname: null }` can match both explicit null and absent field in common query semantics. Use `$exists` when the distinction matters:

```javascript
{ nickname: { $exists: true, $eq: null } }
```

Decide what each state means during schema evolution. A default applied only by new Java code does not repair older documents.

## Create and unique invariants

```javascript
db.orders.createIndex(
  { customerId: 1, requestKey: 1 },
  { unique: true, name: "uq_order_request" }
)
```

An existence query before insert is race-prone. The unique index arbitrates concurrent claims. For optional unique fields, learn how null/missing values interact with sparse or partial indexes and express the intended subset explicitly.

## Projection and sort

```javascript
db.orders.find(
  { customerId: "c-42", status: "PAID" },
  { status: 1, totals: 1, createdAt: 1 }
).sort({ createdAt: -1, _id: -1 }).limit(50)
```

The `_id` tie-breaker makes order stable. Projection reduces transfer and accidental exposure, but inclusion/exclusion rules and `_id` defaults need explicit testing.

## Operators versus replacement

This updates selected fields:

```javascript
db.orders.updateOne(
  { _id: id },
  { $set: { status: "CANCELLED" }, $inc: { version: 1 } }
)
```

A replacement operation replaces the document apart from `_id`; omitted fields disappear. Java object replacement can therefore delete fields written by newer services. Prefer targeted operators for commands unless full replacement is the explicit contract.

## Array operations

```javascript
db.orders.updateOne(
  { _id: id, "lines.sku": { $ne: "BOOK-1" } },
  { $push: { lines: { sku: "BOOK-1", quantity: 1 } } }
)
```

This shape is not a universal uniqueness guarantee under every concurrent/model condition. A document-level filter plus update is atomic, but array uniqueness should be modeled and tested carefully; an object keyed by SKU or separate collection with a unique compound index can be clearer.

Use positional operators deliberately:

- `$` updates the first matched element;
- `$[]` targets all elements;
- `$[identifier]` uses `arrayFilters`.

Accidentally applying `$[]` is a high-impact bug. Always test empty arrays, duplicate matching elements, and missing paths.

## Upsert race reasoning

`upsert: true` combines match/update-or-insert, but concurrent upserts need a unique index matching the logical key. Otherwise several distinct documents can be inserted. The inserted document is derived from equality filter fields plus update operators; do not assume every query operator becomes stored state.

## Schema validation

Collection validators can require types and fields at the database boundary. Roll them out compatibly: measure existing violations, add warning/moderate behavior if appropriate, backfill, then tighten. Driver POJO validation and Bean Validation improve errors but do not protect writes from other clients.

## Edge cases

| Case | Risk | Safer reasoning |
|---|---|---|
| missing versus explicit null | ambiguous query/results | `$exists` plus migration contract |
| replacement from stale DTO | erases unknown fields | targeted updates and version filter |
| unbounded `$push` | 16 MiB limit and growing write cost | cap/window or separate event collection |
| floating money | precision loss | integer minor units/decimal128 with policy |
| concurrent upsert | duplicates | unique index on logical key |
| positional update | wrong/all elements changed | precise match and array filters |

## Practice and solution direction

- **Foundation:** Query only documents with an explicit null nickname. Use `$exists: true` and `$eq: null`.
- **Interview Core:** Make order creation idempotent. Use a scoped unique request key and interpret duplicate key as prior claim.
- **SDE-2 Follow-up:** Evolve `status` from free text to a validated set. Inventory, dual-compatible deployment, backfill, validation tightening, then remove compatibility.
