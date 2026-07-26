# Spring MVC and Durable REST Contracts

## Learning objectives

After this chapter, you should be able to:

- trace a Servlet-stack Spring MVC request from filters through `DispatcherServlet`, handler mapping, argument resolution, validation, invocation, and message conversion;
- keep transport DTOs, domain models, and persistence entities separate for explicit compatibility and trust boundaries;
- choose HTTP methods and status codes from method semantics rather than habit;
- produce stable RFC 9457 Problem Details responses without leaking internals;
- design bounded offset or keyset pagination with a deterministic order;
- evolve APIs using additive compatibility, explicit version policy, conditional requests, and idempotency keys; and
- explain where authentication, authorization, CSRF, CORS, validation, and output encoding belong.

## 1. The MVC request lifecycle

### Intuition and formal pipeline

Spring MVC on the Servlet stack is a pipeline of distinct responsibilities:

```text
socket/container
  -> servlet filters (security, correlation, encoding)
  -> DispatcherServlet
  -> HandlerMapping selects handler
  -> HandlerAdapter resolves arguments
  -> data binding + Bean Validation
  -> controller invokes application use case
  -> return-value handling
  -> HttpMessageConverter serializes body
  -> exception resolvers map failures
  -> servlet response
```

Interceptors and asynchronous dispatch add branches, but the separation remains useful. A filter surrounds Servlet dispatch and can apply before Spring selects a controller. A controller advice participates in MVC exception and binding handling. A message converter translates bytes and Java values. A service implements a use case. Treating all of them as interchangeable produces security gaps and error inconsistency.

A request should be modeled as an untrusted byte sequence that becomes increasingly trusted only through parsing, syntactic validation, authorization, and domain validation. Successful JSON deserialization proves shape, not permission or business validity.

### Recognition and decision rules

| Requirement | Correct layer to consider first | Why |
|---|---|---|
| authenticate bearer token | security filter chain/resource-server support | applies before protected handler execution |
| reject malformed JSON | message conversion / exception mapping | no valid DTO exists yet |
| require nonblank field | DTO validation | transport-level syntactic contract |
| enforce customer owns account | application/domain authorization | requires business identity and current state |
| normalize error body | controller advice / error-response policy | one external contract across controllers |
| measure use-case latency | service boundary observation | controller time can mix parsing and response I/O |
| attach request correlation | filter plus explicit propagation | must cover all routes and downstream work |

### Failure walkthrough: “validated” but unauthorized

An endpoint receives `{ "accountId": "A-7", "amount": 1000 }`. Bean Validation proves the string is present and the amount positive. It does not prove the authenticated principal owns `A-7`. If the repository query uses only the request's account ID, an attacker can substitute another valid identifier. The application must derive or check authorization using the authenticated identity and use a query/transition whose predicate includes the tenant or owner boundary.

## 2. DTOs are boundary contracts

### Separate transport, domain, and entity models

A transport record is optimized for a wire contract. A domain object protects business invariants. A persistence entity participates in an ORM lifecycle. One class rarely satisfies all three without coupling accidental fields and annotations.

```java
// Dependency-requiring annotations: Jakarta Validation and Spring MVC.
public record CreateOrderRequest(
        @NotBlank String customerId,
        @NotEmpty List<@Valid LineRequest> lines,
        @Size(max = 200) String clientNote) {
}

public record LineRequest(
        @NotBlank String sku,
        @Positive int quantity) {
}

public record OrderResponse(
        String id,
        String status,
        long version,
        Instant createdAt,
        List<LineResponse> lines) {
}
```

Do not bind arbitrary request fields directly onto a managed entity. Over-posting can modify fields the endpoint never intended to expose, and ORM relationships can cause unexpected reads or writes. Map explicitly. Define whether unknown JSON properties are rejected or ignored and test that policy; the serializer's default is not a substitute for an API decision.

Validation has at least four levels:

1. **parse/shape:** valid JSON and expected types;
2. **syntactic:** length, requiredness, ranges, basic formats;
3. **semantic:** SKU exists, transition is allowed, totals match rules;
4. **concurrent:** database constraint or atomic predicate still holds at commit.

Cross-field constraints such as `start < end` belong in a type-level validator or command constructor. Database uniqueness must be enforced by a database constraint even if an early validation query improves the error message.

## 3. HTTP method and status semantics

### Method decision table

HTTP semantics are an interoperability contract shared by clients, proxies, caches, and observability systems.

| Method | Intended semantics | Typical use | Common trap |
|---|---|---|---|
| `GET` | safe retrieval; repeated requests should not request a state change | read resource or collection | triggering commands, incrementing durable state as the purpose |
| `HEAD` | same selected representation metadata as GET without response content | probes and cache checks | implementing different authorization than GET |
| `POST` | resource-specific processing; not inherently idempotent | create subordinate resource or command | assuming retry is safe without an idempotency protocol |
| `PUT` | replace/create state at a known target; idempotent intent | client-known resource URI, full replacement | silently treating omitted fields as “unchanged” |
| `PATCH` | apply partial modification under the selected patch media type | deliberate partial update | using an undocumented ad-hoc object and losing null/absent distinction |
| `DELETE` | remove association/resource; idempotent intent | deletion/tombstone transition | promising physical erasure or immediate downstream disappearance |

“Idempotent” means multiple identical requests have the same intended effect as one; responses can differ because state, timestamps, or audit records change. “Safe” means the client did not request a state change, though servers may log or meter.

### Status selection

Use the most specific status whose semantics match the outcome:

- `200 OK`: successful request with a representation;
- `201 Created`: a new resource was created; include a `Location` when a resource URI is available;
- `202 Accepted`: processing was accepted but not completed; expose job/status semantics;
- `204 No Content`: success with no response content;
- `304 Not Modified`: conditional retrieval result, not a generic success body;
- `400 Bad Request`: malformed syntax or request contract that cannot be processed as sent;
- `401 Unauthorized`: authentication is missing/invalid; the name is historical;
- `403 Forbidden`: identity is understood but policy denies access;
- `404 Not Found`: no exposed resource; can also avoid disclosing resource existence under a documented policy;
- `409 Conflict`: request conflicts with current resource state, such as an invalid transition or uniqueness collision;
- `412 Precondition Failed`: `If-Match` or another precondition failed;
- `415 Unsupported Media Type`: request representation is unsupported;
- `422 Unprocessable Content`: syntax is understood but instructions fail semantic validation, when this is the API's documented policy;
- `429 Too Many Requests`: rate policy rejected the request, often with retry guidance;
- `500`/`503`: unexpected server failure versus temporary inability; do not promise retry unless the operation is safe to retry.

Do not return `200` with `{success:false}` for every failure. It defeats generic clients, metrics, caches, and alerting. Do not expose stack traces or SQL messages in any error body.

## 4. Problem Details as an error protocol

RFC 9457 defines a media type and fields for machine-readable problem details. A useful response might be:

```json
{
  "type": "https://api.example.com/problems/order-version-conflict",
  "title": "Order version conflict",
  "status": 409,
  "detail": "The order changed after the supplied version.",
  "instance": "/orders/O-91",
  "code": "ORDER_VERSION_CONFLICT",
  "traceId": "6f6d...",
  "currentVersion": 8
}
```

The stable machine contract is the problem type URI and documented extension fields. `title` is short and stable. `detail` can help a human but should not be parsed. `traceId` correlates server evidence without exposing internals. Validation errors need a bounded structured collection such as `{path, code, message}`; never echo secrets or whole rejected payloads.

Dependency-requiring Spring sketch:

```java
@RestControllerAdvice
final class ApiExceptionHandler {
    @ExceptionHandler(VersionConflict.class)
    ResponseEntity<ProblemDetail> versionConflict(
            VersionConflict failure, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setType(URI.create(
                "https://api.example.com/problems/order-version-conflict"));
        problem.setTitle("Order version conflict");
        problem.setDetail("The order changed after the supplied version.");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "ORDER_VERSION_CONFLICT");
        problem.setProperty("currentVersion", failure.currentVersion());
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
```

Exception mapping must preserve operational classification. A client error should not generate a false server-error alert. An unexpected exception should be logged once at the responsible boundary with correlation and then mapped to a generic problem. Avoid catching `Exception` in every controller and duplicating policy.

### Failure walkthrough: leaking exception text

If a unique constraint throws a database exception containing an index name and row values, returning `exception.getMessage()` leaks storage details and possibly user data. Mapping every database exception to `409` is also wrong: connection loss or syntax errors are server failures. Translate only recognized constraint outcomes through a narrow persistence adapter; let unexpected failures remain unexpected.

## 5. Pagination: bounded work and stable order

### Pagination contract

Every collection endpoint needs:

- a maximum page size enforced server-side;
- a total deterministic ordering, including a unique tie-breaker;
- filter and sort allow-lists;
- cursor semantics tied to the query and authorization scope;
- a policy for concurrent inserts/deletes;
- opaque cursor validation and expiry/version behavior;
- no promise of an exact total unless the system can afford and define it.

Offset pagination asks the database to skip `offset` rows. It is simple and supports page numbers, but deep offsets can do increasing work and concurrent changes can shift results. Keyset pagination asks for rows after the last ordering key:

```sql
select id, created_at, status
from orders
where customer_id = :customer
  and (created_at, id) < (:last_created_at, :last_id)
order by created_at desc, id desc
limit :limit_plus_one;
```

The `(created_at, id)` tuple establishes a total order. Fetching one extra row tells the API whether a next cursor exists. The exact SQL tuple support and plan are database-specific; an equivalent lexicographic predicate may be required.

### Cursor design

A cursor can encode a version, last sort keys, normalized filter hash, and perhaps expiry, then be authenticated or otherwise tamper-resistant:

```text
v1 | createdAt=... | id=... | filterHash=... | signature=...
```

Opaque does not mean secure; base64 is merely encoding. Reject a cursor created for a different tenant or filter. Do not place secrets in it. If records mutate their ordering field, even keyset pagination can skip or duplicate items; document whether the listing is a live traversal or backed by a snapshot.

### Decision rule

- Choose offset for small administrative lists where page numbers and exact jumping matter.
- Choose keyset for large feeds or APIs where forward/backward traversal and predictable query work matter.
- Choose a snapshot/export job when the caller needs a stable exhaustive dataset.

## 6. Conditional requests and optimistic HTTP concurrency

An entity tag identifies a selected representation. A client can send `If-None-Match` on GET to avoid transferring an unchanged representation. For updates, `If-Match` can make the request conditional on the observed version.

```text
GET /orders/O-91
-> 200, ETag: "order-91-v8"

PUT /orders/O-91
If-Match: "order-91-v8"
-> 204 and new version, or 412 if the precondition fails
```

Connect the precondition to an atomic persistence predicate; checking in Java and updating later creates a race. Distinguish weak and strong validators according to HTTP semantics. Do not invent an ETag from a Java object's `hashCode()`—it is not a durable representation validator.

## 7. API evolution and versioning

### Compatibility before version numbers

Prefer additive evolution:

- add optional response fields that tolerant clients can ignore;
- add request fields with safe defaults only when absence is unambiguous;
- preserve enum handling policy or provide an unknown-value strategy;
- never silently change units, timezone meaning, nullability, precision, or ordering;
- announce and observe deprecation before removal;
- test representative old clients/contracts.

Use a new version when semantics cannot remain compatible. URI, media-type, header, or query versioning each has operational tradeoffs; consistency matters more than fashion. The version selector must participate in routing, documentation, metrics, caching (`Vary` where appropriate), and deprecation policy.

Schema and behavior versions are not always identical. A field can retain its JSON shape while its meaning changes incompatibly. Write semantic examples and consumer contract tests.

## 8. The security boundary

Security is not one annotation. The request crosses several controls:

1. TLS protects transport in deployment;
2. the security chain authenticates credentials and establishes a principal;
3. route and method policies authorize broad capabilities;
4. the use case enforces resource/tenant ownership and state-dependent policy;
5. input is constrained for resource use and injection resistance;
6. output is encoded for its destination and secrets are redacted;
7. logs and metrics avoid sensitive values;
8. rate and concurrency limits protect capacity.

CORS tells a browser whether cross-origin script access is permitted; it is not authentication. CSRF defenses matter when a browser automatically attaches credentials such as cookies. A stateless bearer-token design changes the threat model but does not eliminate authorization, token theft, replay, or origin concerns. Use the supported Spring Security model and test deny-by-default behavior.

Mass-assignment, insecure direct object reference, unrestricted page sizes, regex denial of service, decompression bombs, and oversized payloads are API-boundary failures even when deserialization succeeds.

## 9. Interview questions and model checkpoints

### Q1. Trace a request to a controller.

**Model checkpoint:** begin with container/filter chain, then `DispatcherServlet`, mapping/adapter, argument resolution and conversion, validation, handler invocation, return-value/message conversion, and exception resolution. Identify where security applies and avoid asserting undocumented internal order.

### Q2. When do you choose `409` versus `412`?

**Model checkpoint:** `412` is specifically a failed request precondition such as `If-Match`; `409` represents a conflict with current resource state when that precondition protocol is not the cause.

### Q3. Why can keyset pagination be more stable than offset?

**Model checkpoint:** the next query continues from explicit ordered keys instead of skipping a moving row count. It needs a total order and cannot offer arbitrary page jumps cheaply. Mutable ordering keys and live writes still require documented semantics.

### Q4. Is validation authorization?

**Model checkpoint:** no. Validation checks input or domain rules; authorization checks whether this principal may act on this resource under current policy. Both are necessary.

### SDE-2 follow-ups

1. Design error types that remain compatible across ten services while allowing service-specific extensions.
2. An API times out after creating an order. Define client retry behavior and the server protocol that prevents duplicates.
3. Add backward pagination to a descending `(createdAt, id)` feed. Explain predicates and response ordering.
4. Explain how API version selection affects CDN/proxy caches and observability dimensions.

## 10. Exercises

1. Specify request, success, validation, conflict, and unexpected-error contracts for `POST /orders`.
2. Convert an unbounded `GET /events` into keyset pagination. Define the cursor payload, maximum limit, tie-breaker, and behavior under deletion.
3. Design a conditional update using an ETag and an atomic SQL version predicate.
4. Threat-model an endpoint that exports a CSV using a user-provided sort column and filename.
5. Write five contract tests that distinguish malformed JSON, validation failure, forbidden access, missing resource, and version conflict.

## 11. Summary checklist

- [ ] The request lifecycle is understood as stages with distinct ownership.
- [ ] DTOs are separate from domain and persistence entities.
- [ ] Validation includes syntactic, semantic, authorization, and concurrent guards.
- [ ] Methods and statuses preserve HTTP semantics.
- [ ] Problems expose stable machine codes without internal details.
- [ ] Collection work is bounded and ordered deterministically.
- [ ] Cursor and conditional-request contracts are explicit.
- [ ] API changes follow a compatibility and deprecation policy.
- [ ] Authentication, authorization, CSRF/CORS, and resource controls are not conflated.

## 12. API contract review lab

Review this tempting endpoint:

```java
// Dependency-requiring anti-example.
@PostMapping("/orders/search")
List<OrderEntity> search(@RequestBody Map<String, Object> body) {
    return repository.search(body);
}
```

It exposes persistence entities, has no page bound, accepts untyped filters, hides authorization, and has undocumented serialization/error behavior. It also uses POST for a read without explaining why a body is required or how caches/clients should treat it.

A contract-first revision defines:

```text
GET /v1/orders?status=PENDING&createdBefore=...&limit=50&after=...
Authorization derived tenant/customer scope
Accept: application/json

200 {
  "items": [...stable OrderSummary fields...],
  "nextCursor": "opaque-or-null"
}
```

If filters are too complex for practical query parameters, a POST search resource can be valid, but document safety/idempotency, cacheability, maximum complexity, and whether the request creates an asynchronous export. Method choice follows semantics and interoperability, not a universal ban.

### Contract table

| Concern | Decision | Test evidence |
|---|---|---|
| scope | tenant/customer comes from authenticated principal | cross-tenant ID/filter returns no data |
| filter | enum and timestamp parsed with allow-list | unknown status and invalid instant produce stable problems |
| sort | fixed newest-first `(createdAt,id)` | ties traverse without skip/duplicate |
| bound | default 25, maximum 100 | 0, negative, 101, huge value rejected or normalized per contract |
| cursor | signed/versioned, binds tenant+filter | tamper and cross-filter reuse rejected |
| projection | stable `OrderSummary`, not entity | lazy fields/PII never serialize |
| total | omitted | client does not infer final page from page length alone; cursor signals continuation |
| errors | RFC 9457 types | status, media type, code, no internal message |
| cache | private/no-store or tenant-safe explicit policy | `Vary` and authorization cache behavior reviewed |

### DTO mapping walkthrough

The controller resolves `PrincipalContext`, parses a bounded filter, and invokes `ListOrdersQuery`. The query constructs a database predicate that includes tenant scope and cursor tuple, selects only summary columns, requests `limit + 1`, and returns a value object. The adapter maps the first `limit` rows and generates a cursor from the last returned row only if an extra row exists.

If the cursor contains `(createdAt,id)` but not the status filter, a client can change `status` mid-traversal and receive surprising data. Bind normalized query shape in the cursor or explicitly reject mismatch. If tenant ID is merely encoded and not authenticated, a caller can rewrite it. Use an authenticated cursor or ignore embedded authorization identity and always derive it server-side.

### Error taxonomy exercise

Create a stable mapping:

| Failure | HTTP outcome | Retry guidance |
|---|---|---|
| malformed JSON/query encoding | `400` problem | fix request |
| field validation | `400` or documented `422` | fix fields |
| unauthenticated credential | `401` | authenticate/refresh under security protocol |
| forbidden capability | `403` | do not blind retry |
| resource hidden/missing | `404` | re-check identity; no existence disclosure |
| stale `If-Match` | `412` | fetch/reconcile current representation |
| invalid domain transition | `409` | change requested action/state |
| rate policy | `429` | wait only if safe; honor bounded guidance |
| dependency unavailable | `503` generic problem | retry only idempotent operation with budget |
| unexpected defect | `500` generic problem | server investigation; no internal detail |

Whether semantic validation is `400` or `422` is less important than a consistent documented policy. A client must never parse localized prose to decide.

### Conditional-update race walkthrough

Two clients read version 8. Client A sends `If-Match: "v8"` and atomically updates to version 9. Client B also sends `v8`; its SQL predicate matches zero and the API returns `412`. If the service instead checks version 8 with a SELECT, calls a remote service, then updates without version in the predicate, B can overwrite A. The HTTP precondition has value only when connected to the authoritative atomic transition.

### Versioning checkpoint

Before creating `/v2`, classify the proposed change:

- adding optional response metadata: usually additive;
- changing cents to decimal currency units: semantic breaking change;
- removing an enum value clients may send: breaking request change;
- adding a required request field without safe default: breaking;
- changing default sort: behavior breaking even if JSON schema unchanged;
- tightening an unsafe maximum page size: potentially necessary security change; communicate and measure;
- replacing offset with cursor while keeping old contract during migration: parallel capability/deprecation.

Versioning does not eliminate compatibility work. Old versions still need security fixes, telemetry, data mapping, and decommission evidence.

## Primary references

- Spring Framework Reference, “Spring Web MVC”: <https://docs.spring.io/spring-framework/reference/web/webmvc.html>
- Spring Framework Reference, “Error Responses”: <https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html>
- Spring Security Reference: <https://docs.spring.io/spring-security/reference/>
- RFC 9110, “HTTP Semantics”: <https://www.rfc-editor.org/rfc/rfc9110>
- RFC 9111, “HTTP Caching”: <https://www.rfc-editor.org/rfc/rfc9111>
- RFC 9457, “Problem Details for HTTP APIs”: <https://www.rfc-editor.org/rfc/rfc9457>

> **Version boundary:** the architectural pipeline and HTTP RFC semantics are the baseline. Concrete Spring MVC argument resolvers, validation integration, `ProblemDetail` conveniences, and security DSLs vary across supported framework lines. Examples are Jakarta-era and use Java 21; consult the documentation matched to the deployed BOM.
