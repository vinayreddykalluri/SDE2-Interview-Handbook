# Consumers: Groups, Offsets, Polling, Rebalances, and Delivery

## Consumer group ownership

Consumers with the same group ID divide subscribed partitions. Different groups independently consume the same topic. A rebalance changes ownership when membership, subscriptions, or partitions change.

Generation/member protocols prevent stale members from committing as if they still owned a partition, but your external side effect may already have happened.

## Poll loop

```text
poll -> receive records from assigned partitions
 -> deserialize/validate
 -> process in partition order
 -> make side effect durable
 -> commit next offset
 -> poll again before liveness deadline
```

Committed offset is usually the **next** record to read. After processing offset 25, commit 26.

## Commit timing windows

```text
commit 26 -> then process 25 -> crash = possible loss (at-most-once)
process 25 -> then commit 26 -> crash between = duplicate (at-least-once)
```

At-least-once plus idempotent effect is the common safe baseline.

Auto commit does not mean “commit after every successful handler.” It commits positions according to polling/time behavior and can advance beyond safely processed asynchronous work. Understand framework/container semantics before enabling it.

## Idempotent inbox

```sql
BEGIN;
INSERT INTO processed_event(consumer_name, event_id)
VALUES (?, ?); -- unique constraint
-- apply business mutation in same database transaction
COMMIT;
```

Duplicate event insert fails or reports prior processing; treat it as already applied. Commit Kafka offset afterward. A crash after DB commit but before offset commit repeats the event, and the inbox makes it harmless.

## Polling and long processing

`max.poll.interval.ms` bounds time between polls before a consumer is considered failed for group progress. `max.poll.records` bounds each batch. Session timeout/heartbeats relate to membership liveness. Client versions may run heartbeats separately, but a blocked application still violates poll interval.

For slow work, reduce batch size, move work to bounded worker queues while pausing partitions carefully, extend interval with evidence, or redesign into stages. Unbounded async submission can commit ahead, reorder one partition, exhaust memory, and trigger rebalances.

## Rebalances

During revocation, finish/commit only work known durable for revoked partitions, stop accepting new work, and release resources. Cooperative rebalancing can reduce stop-the-world movement but does not remove correctness requirements. Static membership can reduce churn for stable instances but increases stale-member recovery considerations.

## Seeking and replay

Offsets can be reset to earliest/latest/timestamp/specific position. Replay requires:

- retained data still exists;
- schemas and code can read historical versions;
- side effects are idempotent or directed to a new projection;
- downstream capacity can handle catch-up;
- progress and stop criteria are observable.

Never reset a production group casually to “fix lag.” It can replay millions of effects.

## Poison records

Separate transient (database unavailable), permanent data/schema (invalid payload), and code/invariant bugs. Infinite retry blocks a partition forever. A DLT/quarantine preserves evidence but is not success; include original topic/partition/offset, key, headers, exception class, schema version, attempt history, and remediation owner—without exposing secrets.

## Edge matrix

| Trap | Result | Repair |
|---|---|---|
| commit before async tasks finish | data loss | track contiguous durable offsets |
| process same partition concurrently | reorder | serialize per partition/key |
| one poison record | partition stuck or data skipped | bounded classified retry + quarantine |
| slow handler | poll timeout/rebalance loop | bound work/pause/adjust design |
| DLT with no replay tool | permanent graveyard | ownership, repair, replay workflow |

## Practice

- **Foundation:** Draw commit-before and commit-after failure windows.
- **Interview Core:** Build an idempotent MySQL inbox transaction.
- **SDE-2 Follow-up:** Process a polled batch concurrently while committing only the highest contiguous successful offset per partition.
