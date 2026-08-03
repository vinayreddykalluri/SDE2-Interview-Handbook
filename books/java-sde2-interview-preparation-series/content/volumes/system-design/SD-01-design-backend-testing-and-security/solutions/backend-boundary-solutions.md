# Reasoned Solutions: Backend Boundary Practice

These are model answers, not scripts to memorize. A strong interview answer can choose a different design when it states the same invariant and defends the trade-off.

## 1. Trace a create request

One defensible flow is:

```text
accept -> TLS/edge limits -> bounded body read -> syntax parse
       -> authenticate -> resolve tenant -> authorize operation
       -> validate command semantics -> reserve idempotency identity
       -> begin transaction -> write order + outbox + completed outcome
       -> commit -> serialize stable response
```

Malformed encoding, excess size, bad JSON, invalid credentials, wrong tenant, insufficient permission, and semantic validation failures should normally reject before opening a transaction. A uniqueness or concurrency decision may still require the database.

Sending email in the controller creates a dual-write gap: the order may commit while email fails, or email may send while the transaction later rolls back. Record an outbox intent in the same transaction and let a retryable relay deliver it. The consumer must tolerate duplicate delivery.

## 2. Same key, two payloads

Use `(tenant, operation, key)` as the identity and store a fingerprint of the normalized command.

| Situation | Outcome |
|---|---|
| exact duplicate, original running | return `in progress`, wait within a bound, or expose status; do not execute twice |
| different payload, original running | conflict; key reuse changed meaning |
| exact duplicate, original complete | replay the stored status/body or resource identity |
| different payload, original complete | conflict; never replay a result for a different command |

A key alone cannot distinguish a legitimate retry from accidental reuse. The fingerprint is not an authorization mechanism and should be computed from stable, canonical command fields rather than raw JSON whitespace.

## 3. Unknown commit result

Assign a logical command identity before the attempt and enforce a unique constraint such as `(tenant_id, idempotency_key)` or a caller-supplied command ID. After the timeout:

1. query the authoritative store by that identity;
2. if a completed row exists, return its stable result;
3. if no row is visible and the database confirms the transaction aborted, retry under the same identity;
4. if the database remains unavailable or replica visibility is uncertain, return an explicit unknown/in-progress response and let the client poll or retry the same key.

Blindly creating a new identity can duplicate the order. A unique constraint is the final concurrency guard; an application pre-check alone races.

## 4. Outbox relay crash

Give every event a stable `event_id`; the consumer owns a table such as `processed_effect(consumer_name, event_id)` with a unique constraint. In one local transaction, insert the ledger row and apply the business effect. On unique-key conflict, treat the delivery as already applied and acknowledge it.

If state and ledger use separate commits, either ordering has a hole: state-first can apply twice after a crash; ledger-first can suppress an effect that never happened. The ledger deduplicates the consumer's local effect, not every downstream side effect. An external provider needs its own idempotency key or reconciliation.

## 5. Tenant boundary

A safer default contract makes tenant part of identity:

```java
Optional<Resource> findByTenantIdAndId(String tenantId, long resourceId);
```

Resolve the authenticated tenant, query inside that scope, then check owner/role policy. A global `findById` increases the chance of forgetting the later check in another caller, may expose existence through timing or errors, and makes row-level database policy harder. Return the product's chosen not-found/forbidden behavior consistently without leaking cross-tenant data.

## 6. Lost update

The conditional write is shaped like:

```sql
UPDATE profile
SET display_name = ?, timezone = ?, version = version + 1
WHERE tenant_id = ? AND id = ? AND version = 8;
```

Zero updated rows means stale version or missing resource; distinguish only if the API needs to. Automatically replaying a complete replacement can erase the other writer's change, so return `409 Conflict` or `412 Precondition Failed` with current version. A field-specific command such as `PATCH /profiles/{id}/timezone`, with a semantic merge rule, may safely reapply when the fields are independent—but the rule must be explicit.

## 7. Integration-test boundary

- Pure unit tests verify mapping and policy branches quickly; they do not prove SQL, indexes, or transaction behavior.
- Repository integration tests against the production database engine verify schema, collation, constraints, generated SQL, and selected isolation cases; they do not prove HTTP/auth wiring.
- HTTP component tests verify parsing, auth context, status/error mapping, and transaction integration through the service boundary; they need not test every repository permutation.
- A small end-to-end smoke test proves deployment wiring; it is slower and usually too coarse for edge coverage.

Use controlled containers or an equivalent real-engine environment for semantics that a fake cannot model. Keep deterministic data and assert the invariant, not internal call count.

## 8. SSRF review

Controls include: allow only `https`; parse with a strict URI parser; reject user-info and suspicious encodings; resolve and reject loopback, link-local, private, multicast, and metadata ranges; pin/revalidate the connection target to resist DNS rebinding; force traffic through an egress proxy/firewall; disable redirects or revalidate every hop; cap connect/read/total time; cap response bytes; restrict ports; avoid returning response bodies; and log decisions without secrets.

A hostname allowlist before connection is insufficient because DNS can return a different address later, multiple addresses can differ, redirects can change the destination, and the HTTP client may perform its own resolution. Network egress policy provides a second boundary when application validation fails.

## 9. Graceful shutdown

Mark the instance unready so load balancers stop new traffic, then stop polling/admitting broker work. Continue accepted requests inside a bounded grace period. Commit local transactions before reporting success. For an external call with an unknown outcome, persist reconciliation/idempotency identity; do not assume failure and repeat blindly.

For a broker, acknowledge or commit only the contiguous set whose effects are durably complete. Leave uncommitted records for redelivery. Duplicate processing remains possible, so effects must be idempotent. At deadline, cancel interruptible work, close clients/pools in dependency order, and emit drain/unfinished-work metrics.

## 10. Design defense

A concise answer could be:

> Today one team owns order, inventory reservation, and payment orchestration, and the peak fits one relational cluster. I would keep explicit modules and ports inside one deployable so the order plus outbox retain a local transaction. Splitting immediately introduces network partial failure, duplicated state, sagas, contract versioning, and five operational surfaces. I would extract a boundary when independent ownership, compliance isolation, materially different scaling, or failure containment is measured—for example, notification throughput overwhelming order latency. Before extraction I would stabilize the module API and event contract, add idempotency and observability, shadow traffic, then move one boundary with a rollback path.

The strength is not opposition to services; it is matching distributed cost to evidence.
