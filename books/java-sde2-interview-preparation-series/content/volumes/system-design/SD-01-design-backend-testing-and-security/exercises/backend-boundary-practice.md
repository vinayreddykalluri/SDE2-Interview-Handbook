# Backend Boundary Practice: From Request to Durable Outcome

These exercises are deliberately smaller than a full system-design interview. They train the places where otherwise reasonable Java services lose correctness: trust boundaries, retries, transaction seams, concurrent updates, and operational failure.

For every answer, state the invariant first. A library name is not an invariant.

## 1. Trace a create request — Foundation

A client sends `POST /orders` with a bearer token, tenant header, idempotency key, and JSON body. Write the ordered stages from socket acceptance through durable response. Mark which stages may reject the request before a database transaction begins.

Then answer: why should the controller not send an email directly after saving the order?

## 2. Same key, two payloads — Interview Core

The first request uses key `cmd-71` and payload `{sku:"A", quantity:1}`. While it is running, another request uses the same key and `{sku:"A", quantity:2}`.

Define the outcome for:

1. the exact duplicate while the first request is in progress;
2. the different payload while the first request is in progress;
3. the exact duplicate after completion; and
4. the different payload after completion.

Explain why storing only the key is insufficient.

## 3. Unknown commit result — Interview Core

The database driver times out during commit. The application cannot tell whether the order and outbox row were committed. A teammate proposes rerunning the whole method.

Describe a recovery algorithm that cannot create two logical orders. Include the database constraint or identity you need, the query you perform, and the response when truth is still temporarily unavailable.

## 4. Outbox relay crash — Interview Core

The relay publishes event `evt-900`, then crashes before marking the outbox row as published. It publishes the event again after restart.

Design the consumer's effect ledger. What is the unique key? In what transaction do you insert it? What happens if the consumer updates business state and records the ledger entry in separate commits?

## 5. Tenant boundary — Foundation

Tenant `north` and tenant `south` both have a resource whose local ID is `42`. A `north` administrator calls `GET /resources/42`.

Write a safe repository contract and authorization order. Explain why `findById(42)` followed by `resource.tenantId().equals(caller.tenantId())` can still be a poor default even when the final check is correct.

## 6. Lost update — Interview Core

Two clients read profile version 8. One changes the display name; the other changes the timezone. Both send a complete replacement document with `expectedVersion=8`.

Show the SQL-shaped conditional update. Decide whether the server should automatically retry the second request. Then describe one API alternative that may allow a safe field-level merge.

## 7. Integration-test boundary — Foundation

A repository query works in an in-memory fake but fails in production because of collation and transaction-isolation behavior. Propose the smallest useful test portfolio for the repository and HTTP boundary. State what each test proves and what it does not prove.

## 8. SSRF review — SDE-2 Follow-up

An endpoint accepts a webhook URL and sends a verification request to it. Identify at least six controls across parsing, DNS/IP resolution, network egress, redirects, response handling, and observability. Explain why a hostname allowlist checked only before the request is insufficient.

## 9. Graceful shutdown — SDE-2 Follow-up

A deployment terminates one service instance while it has HTTP requests, database work, and broker records in flight. Write the shutdown sequence. State the treatment of:

- new traffic;
- work that can finish inside the grace period;
- work whose external outcome is unknown; and
- uncommitted versus committed broker offsets.

## 10. Design defense — SDE-2 Follow-up

You are asked to split a correct modular order service into five services because “microservices scale.” Give a two-minute interview answer using:

1. current constraint;
2. proposed boundary;
3. correctness cost;
4. evidence that would justify the split; and
5. safe evolution path.
