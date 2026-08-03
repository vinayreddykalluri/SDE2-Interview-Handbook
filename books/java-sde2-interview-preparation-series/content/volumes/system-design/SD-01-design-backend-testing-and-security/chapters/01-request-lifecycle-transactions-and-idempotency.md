# Request Lifecycle, Transactions, and Idempotency

A backend endpoint is not “controller calls repository.” It is a sequence of trust boundaries and state transitions. A strong SDE-2 candidate can follow one request from socket to durable state, name which failures are safe to retry, and explain what a timeout means when the server may already have committed.

The executable models in `BackendBoundaryPatterns.java` make idempotency, outbox state, authorization, and optimistic version checks concrete.

## Start with the smallest service boundary

Use this flow before adding brokers and caches:

```text
client
  -> edge timeout / size / rate policy
  -> HTTP transport parsing
  -> authentication
  -> tenant + resource authorization
  -> request/idempotency validation
  -> application use case
  -> one local database transaction
       -> domain row
       -> outbox row when asynchronous work is required
  -> stable response or explicit asynchronous status
```

Each step has one job. Transport code knows HTTP status and headers. The use case knows business rules. The repository knows persistence mechanics. Domain rules should not depend on a servlet request, JSON library, or database entity lifecycle.

Do not split classes merely to make a diagram taller. Add a boundary when it clarifies ownership, makes a policy independently testable, or isolates infrastructure.

## Validation has layers

Transport validation rejects malformed shape cheaply:

- required field absent;
- string or collection exceeds maximum size;
- invalid enum/date/number syntax;
- unsupported media type; or
- request body exceeds the edge/application limit.

Domain validation protects business invariants:

- amount must be positive;
- order cannot transition from cancelled to confirmed;
- username must be unique inside its namespace; or
- inventory cannot be reserved twice for one command.

Database constraints remain the final concurrency-safe guard for uniqueness and referential integrity. A prior “does this name exist?” query improves messages but cannot replace a unique constraint: two requests can both observe absence.

## Authentication is not authorization

Authentication establishes an actor. Authorization answers whether that actor may perform this action on this resource now.

For multi-tenant data, a safe repository query often includes tenant scope:

```text
find resource where id = ? and tenant_id = ?
```

Checking tenant only after loading by global ID can leak existence through response timing/status or through a missed policy branch. The companion allows an owner or tenant admin only inside the resource tenant; platform administration is an explicit separate role.

Never trust tenant, role, price, or ownership claims merely because the client supplied them. Derive authority from authenticated server-side state.

## Idempotency is a state machine

An idempotency key alone is incomplete. Bind it to:

- operation/route and caller scope;
- normalized payload fingerprint;
- state: in progress, completed, or deliberately retryable;
- stable completed response/status; and
- retention/expiry policy.

```text
ABSENT --begin--> IN_PROGRESS --commit--> COMPLETED
   ^                   |
   +--- safe abandon --+   only before any uncertain side effect
```

Duplicate behavior:

| Existing state | Same fingerprint | Different fingerprint |
|---|---|---|
| in progress | return conflict/pending; do not execute twice | key misuse conflict |
| completed | replay stable outcome | key misuse conflict |
| absent/expired | one request acquires execution | one request acquires its own key |

If the application times out after asking the database to commit, the outcome is **unknown**, not safely failed. Do not delete the idempotency record and rerun blindly. Reconcile against authoritative state using command ID/unique constraint.

The companion's `abandonBeforeSideEffect` name is intentionally narrow: it is safe only while no external or durable side effect could have happened.

## The local transaction boundary

Keep one invariant-changing use case in one database transaction where practical:

```text
BEGIN
  verify current version/state
  insert/update domain rows
  insert idempotency completion or command identity
  insert outbox event
COMMIT
```

Do not hold the transaction open across a network call to email, payment, or another service. That consumes a connection, increases lock time, and still cannot atomically commit the remote system.

## Transactional outbox

The dual-write failure is simple:

```text
database commit succeeds
process crashes before broker publish
```

Writing an outbox row in the same transaction as domain state makes the intent durable. A relay publishes pending rows and marks them published. Publication can happen more than once if the relay crashes after broker acknowledgement but before its mark; consumers must therefore be idempotent.

The outbox guarantees that committed state has a publishable event. It does not by itself guarantee exactly-once end-to-end effects, global order, or zero lag.

## Optimistic concurrency

Read-modify-write APIs should prevent silent lost updates. Include a version:

```text
UPDATE profile
SET name=?, version=version+1
WHERE id=? AND tenant_id=? AND version=?
```

Zero updated rows means missing/unauthorized or version conflict according to a non-leaking API policy. The client can refetch and merge rather than overwriting another writer.

Pessimistic locking is useful when conflicts are frequent and work is short, but it adds blocking/deadlock concerns. Choose from measured contention and invariant needs.

## Response and error contract

Return errors that help clients act without leaking internals:

- stable machine-readable code;
- human message safe for the caller;
- correlation/request ID;
- field errors when useful;
- retryability only when known; and
- no SQL, stack trace, secret, or internal hostname.

Map malformed input to client error, authorization to the chosen non-leaking 403/404 policy, version conflicts to 409/412, overload to 429/503 with bounded retry guidance, and unexpected faults to a generic 5xx while logging structured internal context.

## Failure and edge-case matrix

| Failure/edge case | Required behavior | Unsafe shortcut |
|---|---|---|
| duplicate request in progress | one executor; duplicate receives pending/conflict | execute both |
| same key, different body | reject key reuse | replay unrelated response |
| timeout before any side effect | explicitly release/retry if known | keep permanent in-progress row |
| timeout during commit | reconcile unknown outcome | assume rollback |
| DB commit, process crash before publish | outbox relay publishes later | direct DB + broker dual write |
| outbox duplicate delivery | consumer deduplicates by event/effect identity | assume exactly once |
| stale version | conflict; refetch/merge | last write silently wins |
| cross-tenant ID | deny without leaking resource | load globally then forget policy |
| unique-name race | database constraint decides | check-then-insert only |
| oversized body | reject before expensive parse/allocation | deserialize unbounded input |
| downstream slow | deadline, pool/bulkhead, explicit failure | hold transaction and threads forever |
| exception response | safe stable error plus internal trace | return stack/SQL details |

## Seven live interview Q&A chains

### 1. Controller responsibility

**Interviewer:** What belongs in the controller?

**Candidate:** Transport adaptation: parse bounded input, obtain authenticated context, call one use-case boundary, and map the typed result to HTTP. Business transitions and transaction rules belong below it.

### 2. Idempotency fingerprint

**Interviewer:** Why store a payload fingerprint with the key?

**Candidate:** A client bug may reuse a key for a different command. Replaying the first result would silently apply the wrong meaning. Same key plus different normalized payload is a conflict.

### 3. Commit timeout

**Interviewer:** The DB call timed out. Can we retry the insert?

**Candidate:** Not blindly. The commit may have succeeded. I query by command/idempotency identity or rely on a unique constraint, then return/replay the authoritative outcome.

### 4. Outbox guarantee

**Interviewer:** Does the outbox give exactly-once messages?

**Candidate:** No. It closes the database-to-publication loss window, but relay publication can duplicate. The consumer needs idempotent effect identity, and ordering remains per chosen key/partition contract.

### 5. Tenant authorization

**Interviewer:** The user is a tenant admin. Can they read any resource ID?

**Candidate:** Only inside their tenant. I scope the lookup/policy by tenant before owner/admin evaluation. Platform-wide access is a separate explicit role with auditing.

### 6. Optimistic conflict

**Interviewer:** Why not automatically retry a stale profile update?

**Candidate:** Retrying the same overwrite against the new version can erase another user's change. I return a conflict unless the operation is commutative or I can safely merge its intent.

### 7. Service boundaries

**Interviewer:** Would you create separate services for order, payment, and notification on day one?

**Candidate:** Only if ownership, scale, compliance, or failure isolation justifies it. A modular service with one relational transaction is easier to make correct. I split when evidence outweighs distributed coordination cost.

## Run the companion

```bash
javac --release 21 -Xlint:all -Werror BackendBoundaryPatterns.java
java BackendBoundaryPatterns
```

Expected final line: `PASS 24 backend-boundary checks`.
