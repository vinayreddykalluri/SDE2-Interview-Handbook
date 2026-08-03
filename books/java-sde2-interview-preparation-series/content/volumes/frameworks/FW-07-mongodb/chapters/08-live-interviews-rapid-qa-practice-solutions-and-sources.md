# MongoDB Live Interviews, Rapid Q&A, Practice, and Sources

## Live interview 1: embed or reference

**Interviewer:** “Model orders and order lines.”

**Candidate:** “Lines are bounded, owned, read with the order, and participate in one invariant, so I would embed them. Product and customer remain references; agreed price/address are snapshots. I would cap line count and reject growth near the document limit.”

**Follow-up:** “What if an order has millions of events?”

**Worked answer:** Keep current order state and a bounded recent window; store immutable events separately or in buckets indexed by order/time.

## Live interview 2: duplicate checkout

**Interviewer:** “Two identical create requests arrive.”

**Candidate:** “Use `(customerId, requestKey)` as a unique index and insert once. A duplicate-key result maps to the original request. A check-then-insert query is not race safe.”

**Follow-up:** “The network timed out.”

**Worked answer:** The outcome is unknown. Reconnect and query the idempotency key rather than generate a new key.

## Live interview 3: slow array query

**Interviewer:** “Find orders containing SKU A with quantity at least 10, but results are wrong.”

**Candidate:** “Independent dot predicates may match different array elements. Use `$elemMatch` and test a compound multikey index under representative array sizes. I would inspect keys/documents examined and avoid projecting the whole large array if not needed.”

## Live interview 4: newest orders pagination

**Interviewer:** “Page 500,000 is slow.”

**Candidate:** “Replace deep `skip` with keyset order `(createdAt DESC, _id DESC)`, store both values in the cursor, and align a compound index after equality filters. Define concurrent insert/delete behavior and cursor tamper protection.”

## Live interview 5: multi-document checkout

**Interviewer:** “Update order, inventory, and emit an event.”

**Candidate:** “First see whether inventory can use atomic conditional updates and reservation state. If order/outbox must commit together, use a short MongoDB transaction on a replica set, with an idempotent callback. External publication happens from the outbox after commit. Cross-document inventory conflict requires retry or compensation.”

## Live interview 6: stale confirmation

**Interviewer:** “Write succeeds, immediate read says not found.”

**Candidate:** “Trace member selection and session. A secondary may lag. Use primary read or causal session/appropriate concern for confirmation; keep stale secondary reads for flows with an explicit staleness budget.”

## Live interview 7: shard hot spot

**Interviewer:** “One shard handles almost every write.”

**Candidate:** “Inspect shard-key distribution and monotonicity. A timestamp/range or celebrity tenant can concentrate traffic. Compare hashed/compound candidates against targeted query needs, uniqueness, migration, and hot-tenant isolation; do not reshard without load rehearsal and rollback criteria.”

## Rapid answered questions

1. **Is MongoDB schema-less?** No; documents have shape/types. Validation may live in app/database and evolves over time.
2. **Atomicity boundary?** One document for ordinary writes; multi-document transactions are available with deployment requirements/cost.
3. **Missing versus null?** Distinct stored states; common null queries can match both unless `$exists` is used.
4. **Why unique index?** It enforces a concurrent invariant; an application pre-check does not.
5. **Replacement versus `$set`?** Replacement removes omitted fields; `$set` changes selected paths.
6. **Why `$elemMatch`?** It requires several predicates to match the same array element.
7. **Is TTL exact?** No; background deletion can be delayed. Enforce security expiry when reading/authorizing.
8. **What is a multikey index?** An index over array values where one document contributes multiple keys.
9. **Does `IXSCAN` mean fast?** No; compare keys/docs examined, returned rows, sort, fetch, and latency.
10. **Read preference versus concern?** Member routing versus consistency/isolation contract.
11. **Does majority mean latest?** It means majority-committed data, not necessarily newest known data everywhere.
12. **Why can transaction callback repeat?** Drivers may retry transient transaction attempts; external effects must not live inside unprotected.
13. **Replica set versus backup?** Replication improves availability; it reproduces mistakes and lacks historical recovery alone.
14. **Why not unbounded arrays?** Document limit, growing read/write cost, and hot-document contention.
15. **Change streams exactly once?** Design for resume and duplicates; destination idempotency/checkpointing determines outcome.
16. **`matchedCount` versus `modifiedCount`?** Match proves predicate found a document; modification says stored bytes/value changed.

## Cumulative assessment and solution rubric

Design a multi-tenant order store with idempotent creation, embedded lines, cursor search, inventory reservation, change-stream projection, and sharding. Deliver document schemas, validators, four indexes, command shapes, concern/routing decisions, retry timelines, shard-key analysis, restore plan, and dashboards.

**Strong solution:** bounds embedded growth, encodes state/version in updates, uses unique request keys, explains multikey/index plans, keeps transactions short and external work in an outbox, selects consistency per endpoint, makes projections rebuildable/idempotent, and identifies hot-tenant/scatter risks.

## Authoritative references

- MongoDB Manual: data modeling, CRUD, indexes, aggregation, replication, [read concern](https://www.mongodb.com/docs/manual/reference/read-concern/), transactions, sharding, change streams, backup, and security.
- MongoDB Java Sync Driver documentation: clients, codecs, filters/updates, sessions, transactions, concerns, and monitoring.
- Spring Data MongoDB reference: mapping, `MongoTemplate`, repositories, transactions, indexes, and observability.

The companion lab validates Java driver BSON command construction without a server. Target-version replica-set/sharded tests remain mandatory for actual plans, elections, transactions, and routing.
