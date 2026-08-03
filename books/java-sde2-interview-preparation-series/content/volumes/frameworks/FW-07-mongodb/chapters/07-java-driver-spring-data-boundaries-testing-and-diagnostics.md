# Java Driver and Spring Data Boundaries, Testing, and Diagnostics

## Driver command shape first

```java
Bson filter = and(
        eq("_id", orderId),
        eq("status", "CREATED"),
        eq("version", expectedVersion));

Bson update = combine(
        set("status", "PAID"),
        currentDate("paidAt"),
        inc("version", 1L));

UpdateResult result = orders.updateOne(filter, update);
if (result.getMatchedCount() != 1) {
    throw new StaleOrderException(orderId);
}
```

The filter is the concurrency contract. `modifiedCount` can be zero for an idempotent update that matched but changed no stored value; use the count that matches the business question.

## Client, pool, and codec lifecycle

`MongoClient` owns pools and topology monitoring and is normally shared. A collection handle is lightweight and carries database, codec, read/write concern, and preference settings. Do not create a new client per request.

Codecs convert BSON and Java. Test optional/missing fields, numeric width, enum evolution, dates, unknown fields, and constructor/default behavior. A mapping exception during cursor iteration can occur after the query succeeded.

## Timeouts and server selection

Different budgets cover server selection, pool checkout, connection establishment, socket I/O, and server execution (`maxTimeMS`). One giant timeout hides where latency occurs. Cancellation/timeout can leave an unknown write result; preserve an operation/request key.

## Spring Data progression

1. Use native `MongoCollection` to learn the exact command.
2. Use `MongoTemplate` when filters, updates, aggregation, transactions, or result metadata must remain explicit.
3. Use repositories for honest CRUD/derived-query contracts.

```java
Query query = Query.query(Criteria.where("id").is(id)
        .and("status").is(CREATED)
        .and("version").is(expectedVersion));
Update update = new Update()
        .set("status", PAID)
        .currentDate("paidAt")
        .inc("version", 1);
UpdateResult result = mongoTemplate.updateFirst(query, update, OrderDocument.class);
```

Repository `save` may replace broadly and is not a conditional state-transition API. For concurrency-sensitive partial updates, keep filter, operators, counts, and concern visible.

## Transactions in Spring

Spring transaction annotations require a configured Mongo transaction manager and a replica set/sharded cluster. Self-invocation/proxy rules still apply. A transaction does not include a remote API or Kafka unless a separate coordination design exists.

## Testing layers

- pure Java tests for model/filter/cursor/retry decisions;
- BSON command-shape tests without a server;
- driver integration tests against an exact MongoDB replica set;
- Spring tests for mapping, transactions, and exception translation;
- production telemetry for plans, lag, pool waits, and failures.

An embedded or substitute database cannot prove target-version sharding, elections, transactions, or query plans.

## Diagnostic sequence

```text
request span
  -> pool/server-selection duration
  -> command name + normalized filter/comment
  -> chosen member/shards
  -> execution stats and waits
  -> cursor batches + decode time
```

Use command comments/trace correlation carefully without putting secrets in logs.

## Java edge cases

| Mistake | Failure |
|---|---|
| client per request | connection/topology churn |
| `toJson()` logs full document | PII/secrets leakage |
| compare `modifiedCount == 1` for idempotent set | false conflict |
| repository replacement with stale object | lost/new-field deletion risk |
| retry whole callback with HTTP side effect | duplicate external action |
| block reactive driver on event loop | throughput collapse |

## Practice and solution direction

- **Foundation:** Build a typed filter/update for one state transition.
- **Interview Core:** Decide `matchedCount` versus `modifiedCount` for idempotent cancellation.
- **SDE-2 Follow-up:** Add traceable, bounded retry without exposing document values; log command shape, labels, attempt, server-selection/pool time, and request key hash.
