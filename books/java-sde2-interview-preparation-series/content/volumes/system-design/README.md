# System Design Series

This shelf is the ordered System Design path. It begins with correctness inside one Java backend boundary, then expands to failures and trade-offs across machines.

## Reading order

| Order | Workspace | Focus |
|---:|---|---|
| SD-01 | `SD-01-design-backend-testing-and-security/` | request lifecycle, authorization, idempotency, local transactions, outbox, optimistic concurrency, testing, security, and operational boundaries |
| SD-02 | `SD-02-distributed-systems-and-system-design/` | capacity, partitioning, replication, consistency, streaming, retries, sagas, overload, observability, multi-region design, and interview method |

## How to study each book

1. Read the native chapters in order and redraw each request/data flow from memory.
2. Run the dependency-free Java companion with assertions/checks enabled.
3. Attempt the exercises before opening the reasoned solutions.
4. For each interview case, state the invariant, authoritative state, failure behavior, and evidence before naming infrastructure.
5. Revisit a design after changing one constraint—traffic, latency, geography, ownership, or consistency—and explain what must change and what can remain simple.

## Scope boundary

SD-01 does not require distribution when a modular Java service and one local transaction are sufficient. SD-02 introduces distribution only when the requirements justify its coordination and operational cost. Framework-specific mechanics remain in the Frameworks series; detailed storage behavior remains in the persistence books.
