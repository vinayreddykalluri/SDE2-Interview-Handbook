# Worked Spring Service: A Retry-Safe Order API

## 1. Problem statement and requirements

Design a Java 21 Spring Boot service that lets an authenticated customer create and read orders.

Functional requirements:

- `POST /v1/orders` creates one logical order even if a client retries;
- each order contains 1–100 SKU/quantity lines;
- creation reserves inventory in the same relational database;
- downstream fulfillment receives an `OrderCreated` event;
- `GET /v1/orders/{id}` returns only an order visible to the caller;
- `GET /v1/orders` provides bounded newest-first traversal;
- `PUT /v1/orders/{id}/note` updates a note conditionally;
- failures use a documented Problem Details contract.

Nonfunctional requirements:

- 99.9% monthly availability objective for the API's defined good-event criteria;
- a 500 ms server deadline for ordinary create/read requests, excluding asynchronous fulfillment;
- no overselling under concurrent requests;
- horizontally scalable stateless application instances;
- auditable state transitions without secrets or full payloads in logs;
- safe rolling restart and observable outbox lag.

Clarify during an interview: inventory and orders are in one database; payment is outside this version; fulfillment accepts duplicate delivery and supports event-ID deduplication; immediate global read-after-write through arbitrary replicas is not promised.

## 2. Architecture and ownership

```text
client
  -> gateway/TLS/rate policy
  -> Spring Security filter chain
  -> OrderController (transport mapping)
  -> CreateOrderUseCase (business + transaction boundary)
  -> OrderRepository -----+
  -> InventoryRepository -+--> relational DB
  -> IdempotencyRepository+
  -> OutboxRepository ----+

outbox relay -> broker -> fulfillment consumer

Actuator/metrics/logs/traces -> restricted operations plane
```

The controller owns HTTP translation. The use case owns the invariant and local transaction. Repositories own persistence translation. The outbox relay owns eventual publication. Security establishes identity at the edge; the use case still scopes every resource access to the tenant/customer.

The bean graph is deliberately acyclic:

```text
OrderController -> CreateOrderUseCase -> repository ports
                -> ReadOrderQuery   -> read repository
OutboxRelay     -> outbox repository + event publisher
```

The relay does not call the controller or use case. The use case does not know Spring MVC. This supports plain-Java domain tests and narrow integration tests.

## 3. API contract

### Create

```http
POST /v1/orders
Authorization: Bearer ...
Idempotency-Key: 3f4d...
Content-Type: application/json

{
  "lines": [
    {"sku": "BOOK-21", "quantity": 2}
  ],
  "clientNote": "Leave at reception"
}
```

First successful creation returns `201 Created`, a `Location`, an ETag derived from the order version, and a representation. A replay using the same key and equivalent canonical request returns the same logical resource and a documented replay indicator; whether the status remains `201` or becomes `200` is part of the API contract. A key reused with a different payload returns a client error problem. A concurrent request may wait briefly or return a documented operation-in-progress problem.

Do not accept `customerId` from the body. Derive it from the authenticated principal. Limit header length and body size before expensive parsing.

### Read and list

```http
GET /v1/orders/O-91
If-None-Match: "order-O-91-v8"
```

Return `304` if the selected representation validator matches. Query by both order ID and customer/tenant boundary so a guessed identifier cannot expose another customer's record.

```http
GET /v1/orders?limit=50&after=eyJ2IjoxLCJhZnRlciI6...
```

Limit defaults to 25 and is capped at 100. Ordering is `(created_at DESC, id DESC)`. The opaque authenticated cursor binds the last tuple, tenant, filter, and cursor schema version. The response contains items and `nextCursor`; it does not promise an exact total.

### Conditional note update

```http
PUT /v1/orders/O-91/note
If-Match: "order-O-91-v8"
Content-Type: application/json

{"note":"Ring the bell"}
```

This is replacement of the note subresource. An atomic update predicate includes order ID, owner, and expected version. Return `204` with the new ETag, `412` for a failed precondition, `404` under the resource-disclosure policy, and a validation problem for an oversized note.

## 4. Domain and persistence contracts

### Domain transition

```java
final class Order {
    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderLine> lines;
    private OrderStatus status;
    private long version;

    public static Order place(
            OrderId id, CustomerId customerId, List<OrderLine> lines) {
        if (lines.isEmpty() || lines.size() > 100) {
            throw new InvalidOrder("line count must be 1..100");
        }
        // Defensively copy and validate each line.
        return new Order(id, customerId, List.copyOf(lines),
                OrderStatus.PENDING, 0);
    }
}
```

This example omits mapping details but keeps invariants independent from MVC and JPA. The persistence adapter can map to rows/entities. Equality of domain identifiers is value-based; a mutable ORM entity needs a separately considered equality strategy.

### Database constraints

Illustrative relational constraints:

```sql
create table orders (
    id varchar(40) primary key,
    customer_id varchar(40) not null,
    status varchar(24) not null,
    note varchar(200),
    version bigint not null,
    created_at timestamp with time zone not null
);

create index orders_customer_created_id
    on orders (customer_id, created_at desc, id desc);

create table idempotency_request (
    customer_id varchar(40) not null,
    operation varchar(40) not null,
    request_key varchar(128) not null,
    fingerprint varchar(128) not null,
    state varchar(24) not null,
    order_id varchar(40),
    expires_at timestamp with time zone not null,
    primary key (customer_id, operation, request_key)
);

create table outbox_event (
    event_id varchar(40) primary key,
    aggregate_id varchar(40) not null,
    event_type varchar(80) not null,
    payload text not null,
    occurred_at timestamp with time zone not null,
    published_at timestamp with time zone
);
```

The exact types, descending-index semantics, and query plans are database-specific. Migrations—not ORM auto-DDL in production—own schema evolution. Add foreign keys/check constraints appropriate to the chosen model.

### Inventory reservation

Avoid “read quantity, subtract in Java, write value.” Use an atomic conditional update:

```sql
update inventory
set available = available - :quantity,
    version = version + 1
where sku = :sku
  and available >= :quantity;
```

Update count zero means missing or insufficient inventory; translate it through a deliberate query or domain policy. For several lines, reserve in a deterministic SKU order to reduce deadlock opportunities, keep the transaction short, and retry only classified transient transaction failures within the deadline.

## 5. Spring adapter sketch — dependency-requiring

```java
@RestController
@RequestMapping("/v1/orders")
final class OrderController {
    private final CreateOrderUseCase create;
    private final ReadOrderQuery read;

    OrderController(CreateOrderUseCase create, ReadOrderQuery read) {
        this.create = create;
        this.read = read;
    }

    @PostMapping
    ResponseEntity<OrderResponse> create(
            @AuthenticationPrincipal Jwt principal,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody CreateOrderRequest request) {
        CustomerId customer = CustomerId.fromSubject(principal.getSubject());
        CreateOrderResult result = create.execute(
                customer, key, OrderMapper.toCommand(request));
        return ResponseEntity.created(result.location())
                .eTag(result.etag())
                .header("Idempotency-Replayed",
                        Boolean.toString(result.replayed()))
                .body(OrderMapper.toResponse(result.order()));
    }

    @GetMapping("/{id}")
    ResponseEntity<OrderResponse> get(
            @AuthenticationPrincipal Jwt principal,
            @PathVariable String id,
            @RequestHeader(value = "If-None-Match", required = false)
                    String ifNoneMatch) {
        // A helper compares validators according to HTTP rules.
        OrderView order = read.requireVisible(
                CustomerId.fromSubject(principal.getSubject()),
                new OrderId(id));
        if (EntityTags.matches(ifNoneMatch, order.etag())) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(order.etag()).build();
        }
        return ResponseEntity.ok().eTag(order.etag())
                .body(OrderMapper.toResponse(order));
    }
}
```

The example shows boundaries, not copy-paste production code. Validate key syntax and ID length. Review framework utilities for correct entity-tag parsing. Avoid hand-parsing authentication tokens in a controller. Do not trust a raw subject without issuer/audience/signature validation in the configured resource server.

## 6. End-to-end execution walkthrough

### Happy path

1. Gateway admits the request under per-customer and global capacity policy.
2. Security verifies the credential and produces an authenticated principal.
3. MVC converts JSON into a request DTO and validates syntactic constraints.
4. Controller derives `CustomerId`, validates the idempotency-key format, and maps to a command.
5. Transactional use case computes a canonical fingerprint.
6. Idempotency repository reserves the scoped key under a unique constraint.
7. Domain validates line rules.
8. Inventory rows are conditionally updated in deterministic order.
9. Order and lines are inserted.
10. Outbox event is inserted.
11. Idempotency record is completed with the order ID.
12. Local transaction commits.
13. Response returns `201`, `Location`, representation, and ETag.
14. Independently, relay claims the outbox event, publishes it, and records publication progress.
15. Fulfillment deduplicates by event ID and performs its own transition.

### Failure matrix

| Failure point | Client observation | Durable state | Recovery |
|---|---|---|---|
| malformed JSON | `400` problem | none | fix request; new or same key has not been reserved |
| insufficient inventory | conflict/semantic problem | transaction rolls back | caller changes order; key policy must permit/document final failure |
| DB deadlock victim before commit | transient failure or bounded internal retry | transaction rolled back | retry only within budget using same key |
| response lost after commit | timeout | order, outbox, completed key exist | retry same key replays same result |
| relay crashes before publish | create already successful | unpublished outbox exists | another relay attempt claims it |
| relay crashes after publish before mark | create successful; duplicate event possible | outbox still appears pending | republish; consumer deduplicates |
| fulfillment unavailable | API create may remain healthy | backlog grows | alert on age, retry with backoff, apply backlog controls |
| process receives shutdown | readiness false | committed work preserved | drain bounded requests, stop relay/consumer safely |

The relay cannot obtain exactly-once side effects simply by marking a row. The publish/mark boundary can duplicate. At-least-once delivery plus idempotent consumption is the explicit contract.

## 7. Security and abuse review

### Threats and controls

- **object-ID guessing:** scope repository operations by authenticated customer; consistent disclosure policy;
- **token confusion:** validate signature, issuer, audience, time claims, and algorithm through supported security configuration;
- **payload/resource exhaustion:** request-body, line-count, field-length, rate, concurrency, and database-statement limits;
- **idempotency-key abuse:** length/character restrictions, tenant scoping, retention limits, no key values in metrics;
- **SQL injection:** parameterized values and sort/filter allow-lists;
- **mass assignment:** explicit request DTO and mapper;
- **sensitive logs:** log order ID, outcome code, latency, and trace ID—not tokens, address/note, or raw body;
- **management exposure:** separate network/auth policy and minimum endpoint allow-list;
- **dependency compromise:** locked and reviewed dependency graph, signed/verified build inputs as organizational policy permits.

Authorization belongs in more than the route declaration. A method-security check that loads an order and a later repository update using only ID can create a time-of-check/time-of-use gap. Include tenant/owner and version predicates in the mutation itself.

## 8. Observability and SLO signals

Define a “good” API request: an eligible request completed within 500 ms and returned the expected non-5xx outcome; exclude explicitly rejected abusive traffic according to the SLO policy. Track:

- request count, outcome, and duration by low-cardinality route template;
- admission rejection, deadline expiry, and retry attempts;
- idempotency first-use, replay, fingerprint conflict, and stuck age;
- inventory conflict and database transaction retry;
- connection pool wait and timeout;
- outbox oldest-unpublished age, pending count, attempts, and permanent failures;
- fulfillment consumer lag and deduplication rate;
- saturation: CPU, allocation, GC, request concurrency, pool occupancy.

Alert on user impact and backlog age, not individual expected validation failures. A trace should connect request, transaction metadata, outbox event ID, and relay publish without putting personal data into span attributes.

## 9. Test strategy

### Unit tests

- domain rejects empty, duplicate-policy, excessive quantity, and too many lines;
- canonical fingerprint is stable for semantically equivalent request DTOs;
- ETag/version mapping and cursor codec reject malformed inputs;
- retry classifier never retries validation or authorization failures.

### MVC/security tests

- missing/invalid token, wrong audience, forbidden ownership, validation body;
- exact status, media type, `Location`, ETag, and Problem Details shape;
- unknown fields and enum evolution follow the documented policy;
- maximum payload/header sizes are enforced at the relevant deployment layer.

### Database integration tests

- two concurrent reservations cannot oversell;
- two concurrent identical keys create one order and outbox event;
- same key/different fingerprint is rejected;
- transaction rollback leaves no partial inventory/order/idempotency completion;
- keyset query uses correct tuple ordering and target-engine plan;
- version predicate returns update count zero on conflict.

### End-to-end tests

- lost-response simulation followed by a replay returns the same order;
- relay crash after publish produces a duplicate that fulfillment deduplicates;
- graceful shutdown removes readiness and preserves committed work;
- management endpoints are inaccessible from the public security boundary.

## 10. Interview question bank and model checkpoints

1. **Why not publish directly to Kafka inside `@Transactional`?**

   **Checkpoint:** the database and broker are independent atomic domains. A Java method cannot make their commits atomic. Use an outbox or an explicitly selected distributed protocol; design duplicate delivery.

2. **Why store the idempotency result instead of only the key?**

   **Checkpoint:** after a lost response, the server must reproduce or locate the original logical outcome. A bare key says “seen” but cannot answer what happened.

3. **How do you prevent overselling?**

   **Checkpoint:** use a database-enforced atomic conditional update or appropriate locking/isolation within a short transaction; inspect update count and test concurrency on the target engine.

4. **What is the list endpoint's complexity?**

   **Checkpoint:** with a matching composite index, keyset lookup plus `limit` can avoid work proportional to deep offset, but the real claim depends on the query plan, selectivity, row visibility, and fetches. State response serialization as `O(pageSize)`.

5. **Can an ETag be the ORM version number?**

   **Checkpoint:** it can be encoded into a representation validator if the version changes whenever that representation changes and the API follows strong/weak validator rules. A partial projection or external computed fields may invalidate that assumption.

6. **What does `@WebMvcTest` prove here?**

   **Checkpoint:** selected MVC mapping, conversion, validation, advice, and configured security behavior. It does not prove the transaction, SQL, outbox, or complete production context.

7. **How would you handle payment next?**

   **Checkpoint:** create a persistent order/payment workflow with idempotent provider calls, deadlines, callback verification, compensating transitions, and reconciliation. Do not hold a database transaction over the network call.

8. **What breaks first during overload?**

   **Checkpoint:** identify measured scarce resources—request concurrency, connection pool, database locks/CPU, relay backlog, broker/downstream capacity. Bound admission and shed work before queues consume the entire deadline.

### SDE-2 design extensions

- split inventory into another service and design reservation expiration plus saga compensation;
- add multi-region reads and state the consistency/read-your-writes contract;
- support customer deletion while retaining legally required order records and minimizing personal data;
- migrate cursor format without invalidating every active cursor abruptly;
- run a zero-downtime schema change that makes `client_note` encrypted or separately stored.

## 11. Exercises

1. Write the idempotency-record state diagram, including two callers, transaction rollback, lease expiry, and retention deletion.
2. Design SQL and application flow for reserving five SKUs without deadlock-prone arbitrary order.
3. Specify the outbox relay claim algorithm for multiple relay instances and explain duplicate windows.
4. Add cancellation of a pending order. Define allowed transitions, optimistic concurrency, event contracts, and replay behavior.
5. Produce a 30-minute interview answer: estimates, API, schema, transaction, failure table, security, observability, and scale evolution.

## 12. Final readiness checklist

- [ ] Requirements distinguish synchronous commit from asynchronous fulfillment.
- [ ] HTTP, validation, Problem Details, pagination, conditional update, and idempotency contracts are explicit.
- [ ] Security identity flows into tenant-scoped database predicates.
- [ ] One local transaction protects inventory, order, outbox, and idempotency outcome.
- [ ] Every cross-system boundary has a duplicate and timeout strategy.
- [ ] Queueing and scarce resources are bounded.
- [ ] Metrics, logs, traces, probes, and shutdown have operational semantics.
- [ ] Tests are matched to claims and include target-engine concurrency.

## Primary references

- Spring Framework Reference, “Spring Web MVC”: <https://docs.spring.io/spring-framework/reference/web/webmvc.html>
- Spring Framework Reference, “Transaction Management”: <https://docs.spring.io/spring-framework/reference/data-access/transaction.html>
- Spring Security Reference: <https://docs.spring.io/spring-security/reference/>
- Spring Boot Reference, “Actuator”: <https://docs.spring.io/spring-boot/reference/actuator/>
- RFC 9110, “HTTP Semantics”: <https://www.rfc-editor.org/rfc/rfc9110>
- RFC 9111, “HTTP Caching”: <https://www.rfc-editor.org/rfc/rfc9111>
- RFC 9457, “Problem Details for HTTP APIs”: <https://www.rfc-editor.org/rfc/rfc9457>

> **Version boundary:** this case uses Java 21 and Jakarta-era Spring concepts but intentionally avoids a particular Boot patch release. Dependency-specific annotations and defaults must be checked against the project's managed BOM. Later Java features are optional deltas, not assumed syntax or runtime behavior.
