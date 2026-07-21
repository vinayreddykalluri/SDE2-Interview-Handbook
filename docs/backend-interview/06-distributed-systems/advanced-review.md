# Advanced Review: Distributed Systems

## Advanced model

| Area | Required depth |
| --- | --- |
| Limits | Asynchrony, partial failure, FLP intuition, CAP, PACELC |
| Consensus | Term/epoch, quorum, replicated log, commit, leader change |
| Time | Monotonic/wall clocks, logical/vector clocks, causality |
| Coordination | Leases, fencing tokens, ownership, membership |
| Replication | Leader, multi-leader, leaderless, quorum, repair |
| Conflicts | LWW limits, merge rules, CRDT intuition |
| Messaging | Ordering, delivery, replay, poison messages, lag |
| Workflows | Outbox, inbox, saga, compensation, reconciliation |

## Reconstruction exercise

Draw a three-node replicated log through normal commit, leader loss, election, and recovery. Then draw an at-least-once workflow and mark every duplicate window.

## Drill ladder

- **Foundation:** state assumptions and failures precisely.
- **SDE-2:** design idempotent delivery and recovery.
- **Advanced:** add election, stale-owner prevention, and partitions.
- **Conflict:** support multi-region writes with an explicit merge rule.

## Retrieval prompts

1. What does consensus agree on, and what does it not solve?
2. Why is a lease unsafe without fencing assumptions?
3. How does a fencing token stop an old owner?
4. How do causal and total ordering differ?
5. Why is exactly-once an end-to-end property?
6. How do rebalances create duplicate windows?
7. When can a CRDT preserve an invariant?
8. How do safety and liveness appear in payment processing?

## Exit criteria

Explain one consensus path, one partition scenario, and one reliable workflow with assumptions, safety, liveness, recovery, and observability.
