# Testing, Security, and Operational Boundaries

A backend is publishable when its correctness claims are testable and its trust assumptions are explicit. Security is not one filter at the edge; testing is not chasing a coverage percentage. Both are ways of turning hidden assumptions into enforced boundaries.

## Test by risk and boundary

Use the cheapest test that can falsify the behavior:

| Test layer | What it proves well | What it cannot prove alone |
|---|---|---|
| pure unit | domain transition, validation, deterministic mapping | SQL/schema/network behavior |
| component with real DB | constraints, transactions, indexes, mapping, migrations | full deployed routing/identity |
| HTTP slice | serialization, status/error contract, auth wiring | real downstream compatibility |
| consumer/provider contract | agreed request/event shapes and compatibility | production latency/semantics beyond contract |
| end-to-end | critical deployed journey | exhaustive branches; fast diagnosis |
| load/fault test | capacity, queues, timeouts, recovery behavior | functional correctness of every rule |

Mocks are useful at a deliberate boundary. Mocking every internal method only proves implementation choreography and makes refactoring painful. Prefer fakes for stable domain ports, real ephemeral infrastructure for database/broker semantics, and contract tests across independently deployed ownership boundaries.

## Test the invariant, not only the happy response

For create-order idempotency, valuable tests include:

- two same-key/same-body requests create one order and one logical outbox event;
- same key/different body is rejected;
- crash after commit/before response replays the committed result;
- concurrent starts produce one owner and one in-progress result;
- retry after a known pre-side-effect failure can acquire execution; and
- an unknown commit outcome is reconciled, not duplicated.

Concurrency tests need a controlled barrier so requests overlap at the intended line. A loop that “usually races” is flaky evidence.

## Database tests need real semantics

An in-memory fake will not reproduce:

- isolation anomalies;
- unique/FK/check constraints;
- lock waiting/deadlock;
- collation/time-zone behavior;
- SQL dialect/query plan; or
- migration compatibility.

Use the actual database engine in integration tests for these. Keep migrations forward/backward compatible with rolling deployments: expand schema, deploy compatible code, migrate/backfill, then contract later.

## Security flow

```text
untrusted request
 -> size/rate/content-type limits
 -> strict parser and validation
 -> authenticated principal
 -> action + resource authorization
 -> parameterized persistence / safe outbound client
 -> redacted response/logging
```

Important backend threats and controls:

- **SQL injection:** prepared parameters; never concatenate user values into SQL.
- **SSRF:** allow intended schemes/hosts, resolve/revalidate DNS, block private/link-local metadata ranges, control egress, and bound redirects/response size.
- **Broken object authorization:** authorize every resource action, not merely the route.
- **Mass assignment:** map explicit allowed fields; do not bind persistence entities directly.
- **Secrets:** use a secret manager/short-lived identity, rotate, never log or commit.
- **Sensitive logs:** structured allowlist/redaction; avoid tokens, passwords, full payment/health data.
- **Deserialization:** bounded schemas and allowlisted types; avoid native object deserialization from untrusted data.
- **Dependency risk:** lock/review versions, scan, patch by exploitability, and maintain SBOM/provenance.

Constant-time comparison can reduce timing leakage for fixed secrets, but use established password hashing and token verification libraries. It does not make a weak secret safe.

## Operational readiness

Expose health semantics deliberately:

- liveness: should the process be restarted?
- readiness: should it receive new traffic?
- startup: does slow initialization need a separate grace window?

Do not make liveness depend on every downstream; a database outage should not cause all application instances to restart-loop. Readiness may fail when the instance cannot serve its contract, with hysteresis to avoid flapping.

On shutdown: stop admission, mark unready, drain in-flight work within a deadline, stop polling, commit/hand back owned work safely, flush bounded telemetry, then exit. A graceful path needs an upper bound so deploys do not hang forever.

## Observability that answers a question

For request paths, capture rate, error, and duration by bounded route/status dimensions. For dependencies, capture pool saturation, timeout/error, and latency. For asynchronous work, backlog **age** often matters more than count.

Trace/correlation IDs connect edges, services, database calls, and messages. Do not put user/resource IDs into unbounded metric labels. Logs must preserve enough decision context to audit authorization/idempotency without leaking secrets.

## Security and test failure matrix

| Scenario | Detection/test | Expected control |
|---|---|---|
| SQL metacharacters in name | real repository integration test | value remains parameter data |
| tenant A requests tenant B ID | HTTP/policy test | non-leaking denial |
| URL resolves to metadata IP | outbound-client security test | block before connection and after redirects/DNS change |
| duplicate concurrent command | barrier-based concurrency test | one logical effect |
| stale entity version | real DB/component test | 409/412 conflict |
| migration with old app instance | compatibility test | both versions operate during rollout |
| dependency latency spike | fault/load test | deadline/bulkhead; bounded queues |
| shutdown during consumer work | lifecycle test | safe commit/hand-back, no silent loss |
| log event contains token | log-capture/redaction test | secret absent |
| error contains SQL/stack | HTTP contract test | stable safe error only |

## Interview-quality explanation pattern

When asked how you would test or secure a feature, answer in this order:

1. state the invariant or asset;
2. name the trust/concurrency boundary;
3. choose a production control;
4. choose the test capable of observing that control;
5. add failure and abuse cases; and
6. name the telemetry and rollout guard.

This is stronger than listing tools. Tools change; boundaries and evidence remain.
