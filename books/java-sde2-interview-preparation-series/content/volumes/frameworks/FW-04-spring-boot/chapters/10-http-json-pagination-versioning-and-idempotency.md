# HTTP, JSON, Pagination, Versioning, and Idempotency

An SDE-2 candidate must reason beyond controller annotations. HTTP method semantics, JSON compatibility, pagination stability, caching, and idempotency determine whether clients survive retries and service evolution.

## Method semantics

| Method | Intent | Safe | Usually idempotent |
|---|---|---|---|
| GET | read representation | yes | yes |
| POST | create/command | no | no, unless application adds a key |
| PUT | replace known resource | no | yes |
| PATCH | partial update | no | contract-dependent |
| DELETE | remove resource | no | yes in intended final state |

Idempotent does not mean the response is identical or that no logs/metrics change. It means repeating the same intended operation has the same externally relevant effect.

## JSON compatibility

Adding an optional response field is usually backward-compatible for tolerant clients. Renaming/removing a field, changing number to string, changing nullability, or reinterpreting an enum can break clients.

Use explicit contract models and contract tests. Do not globally change the JSON mapper to fix one endpoint without auditing every consumer.

```java
record MoneyResponse(String currency, BigDecimal amount) { }
```

Represent money with a decimal value and currency contract. Avoid binary floating-point for financial amounts.

## Content negotiation

Clients express accepted representations through `Accept`; request bodies identify their type with `Content-Type`. A 415 means the request media type is unsupported; a 406 means the server cannot produce an acceptable response.

Do not use URL extensions as the main negotiation strategy in a modern API unless a legacy contract requires it.

## Pagination

Offset pagination is simple:

```text
GET /api/orders?page=4&size=50&sort=createdAt,desc
```

but concurrent inserts can shift offsets. Cursor/keyset pagination uses a stable ordered boundary:

```text
GET /api/orders?after=2026-07-30T10:15:00Z,8f2...&limit=50
```

The sort must be deterministic; add a unique tie-breaker. Never expose an unsigned database cursor containing sensitive data. Validate maximum page size.

## Idempotent create

```text
client sends Idempotency-Key K + canonical request hash H
        |
        v
atomically claim K
  +-- new -> execute and persist result
  +-- same K/H completed -> return stored result
  +-- same K/different H -> 409 conflict
  +-- same K in progress -> wait/retry contract
```

The database uniqueness constraint is the final concurrency guard. An in-memory map fails across replicas and restarts. Define retention and privacy rules for stored request hashes/results.

## API versioning

Version only when compatibility cannot be preserved. Common strategies include URL, header, media type, or platform gateway routing. Spring Framework/Boot versions may add API-versioning support, but the organization still owns lifecycle, deprecation, documentation, observability by version, and client migration.

Avoid versioning every implementation change. Prefer additive response changes and tolerant readers.

## Common mistakes

- Using POST retries without an idempotency contract.
- Offset pagination without deterministic ordering.
- Returning JPA lazy proxies as JSON.
- Accepting unbounded page sizes.
- Using enum names as eternal public values without evolution planning.
- Treating ETag and cache behavior as an afterthought for hot reads.
- Adding `/v2` but leaving error and authentication semantics undocumented.

## Interview angle

**Interviewer:** A client times out after order creation and retries. How do you avoid duplicates?

**Strong answer:** The client sends a stable idempotency key; the service atomically records the key with a canonical request fingerprint in durable shared storage; the transaction creates the order and records the result under a uniqueness constraint; same-key/same-request retries return the original outcome, while same-key/different-request returns conflict. I define in-progress and retention behavior.

## Quick check

1. Safe versus idempotent?
2. Why can offset pages duplicate items?
3. What makes cursor ordering stable?
4. What must an idempotency record include?
5. When should an API version change?

## Practice

- **Foundation:** Map common request failures to HTTP statuses.
- **Interview Core:** Design a stable cursor for `(created_at, id)`.
- **Interview Core:** Define JSON compatibility tests for an evolving response.
- **SDE-2 Follow-up:** Write the state machine for idempotent order creation and recovery after a crash.
