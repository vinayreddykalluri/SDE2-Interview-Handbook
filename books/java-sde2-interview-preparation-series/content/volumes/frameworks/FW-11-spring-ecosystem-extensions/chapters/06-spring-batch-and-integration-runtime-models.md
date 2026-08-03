# Spring Batch and Spring Integration: Durable Progress and Message Flow

Both modules coordinate work, but they model different work.

- Spring Batch models a finite **job** made of steps with durable execution metadata and restart semantics.
- Spring Integration models a **message flow** through channels, endpoints, transformers, routers, filters, and adapters.

They can be used together; they should not be treated as synonyms.

## Spring Batch vocabulary

```text
Job + identifying JobParameters -> JobInstance
one launch/restart attempt       -> JobExecution
one step attempt                 -> StepExecution
restart data                     -> ExecutionContext
```

A job instance is the logical run, such as `settlementDate=2026-08-01`. A job execution is one attempt. Reusing the wrong identifying parameters can either refuse a rerun or accidentally create a second logical job.

## Chunk-oriented processing

```text
begin transaction
  -> read item
  -> process item
  -> repeat until chunk full/end
  -> write chunk
  -> update checkpoint/job metadata
commit transaction
```

If the transaction rolls back, the chunk may be read and processed again. That means processors should be deterministic and writers must tolerate the configured retry/restart semantics.

A chunk size of 1 maximizes checkpoint frequency but adds transaction overhead. A huge chunk reduces commits but increases rollback work, memory, and lock duration. Choose from record cost, resource limits, restart target, and database behavior.

## Restartability is not idempotency

Spring Batch can remember a committed checkpoint. It cannot make an external email, HTTP charge, or non-transactional file append atomic with job metadata.

```text
external side effect succeeds
          |
process crashes before checkpoint commit
          |
restart repeats the item
```

Use a business idempotency key, transactional outbox, staging table, or reconciliation workflow. “The job is restartable” is not enough.

## Skip and retry require classification

- **Retry** when the same item may succeed later within a bounded attempt/time budget: transient lock timeout, short network reset for an idempotent operation.
- **Skip** when policy permits recording a bad item and continuing: malformed optional row with an auditable reject file.
- **Fail** when correctness or systemic health is uncertain: schema mismatch, authorization failure, corrupted input header.

An unlimited skip policy turns data loss into a green job. Record counts and reasons; set thresholds; reconcile input, committed, rejected, and quarantined totals.

## Scaling a batch job

| Technique | Shape | Main correctness question |
|---|---|---|
| Parallel steps | Independent steps run together | Do they contend for the same state? |
| Multi-threaded/local chunking | Items processed concurrently in one process | Are reader/writer/state and ordering safe? |
| Partitioning | Manager assigns disjoint input ranges | Are partitions complete, non-overlapping, restartable? |
| Remote step/chunk pattern | Work crosses processes | How are duplicates, lost workers, and broker delivery handled? |

Scale only after measuring read, transform, write, lock, and commit time. A faster reader can overload the writer or database.

## Spring Integration runtime model

```text
inbound adapter
     |
Message(payload + headers)
     |
channel -> filter -> transformer -> router -> service activator
                                                   |
                                             outbound adapter
```

- A `DirectChannel`-style subscribable channel invokes a subscriber in the sender’s thread unless an executor-backed channel changes it.
- A queue/pollable channel buffers and requires a polling consumer.
- A publish-subscribe channel fans out to subscribers; delivery, error, and executor semantics depend on configuration.
- A channel adapter connects a one-way external boundary; a gateway usually models request/reply.

Headers can carry correlation, reply, sequence, tenant, and tracing information. Do not trust an inbound tenant/role header merely because it exists; establish it at an authenticated boundary.

## Error flow and transaction boundary

For synchronous direct channels, an exception can return to the sender. For executor-backed or polling endpoints, failure occurs on another thread and may be published to an error channel or drive adapter-specific redelivery.

```text
poll external source
  -> begin transaction (if configured)
  -> receive message
  -> handler chain
  -> acknowledge/commit
  -> error => rollback/redelivery or error flow
```

Do not assume one database transaction spans a remote broker, HTTP endpoint, and filesystem. Design idempotency and reconciliation across those boundaries.

## Routing and correlation

Routers choose channels; splitters create related messages; aggregators reconstruct a group. An aggregator needs:

- a stable correlation key;
- a completion rule;
- a bounded store/expiry policy;
- behavior for duplicates and late messages;
- recovery after process restart if completion is durable.

Without expiry, incomplete groups can leak memory/storage forever.

## Failure and edge-case matrix

| Scenario | Failure | Strong response |
|---|---|---|
| Same job parameters relaunched | Completed instance refuses rerun | Deliberately choose identifying/non-identifying parameters |
| Crash after remote effect | Item repeats on restart | Stable idempotency key/outbox/reconciliation |
| Reader not restart-safe | Checkpoint resumes at wrong record | Persist sufficient state; test mid-chunk crash |
| Skip limit too high | Silent data loss | Threshold, reject artifact, reconciliation counts |
| Partitions overlap | Duplicate writes | Deterministic disjoint partition key plus uniqueness |
| Direct channel handler blocks | Sender thread stalls | Make boundary explicit; choose executor/queue only with capacity policy |
| Queue channel has no poller | Messages never handled | Configure and observe a polling consumer |
| Async handler throws | Caller sees success | Error channel/dead-letter/alert and durable semantics |
| Aggregator never completes | State grows forever | Timeout/expiry and partial-group policy |
| Retried integration handler | Side effect duplicates | Idempotent receiver/store and retry classifier |

## Quick check

1. Distinguish job instance from job execution.
2. Why can a committed external side effect repeat after batch restart?
3. When is skip acceptable?
4. Which thread executes a direct-channel subscriber?
5. What five policies does an aggregator need?

## Practice

- **Foundation:** Model a CSV import as job, step, reader, processor, and writer.
- **Interview Core:** Choose a chunk size experiment and list the measurements.
- **Interview Core:** Draw error propagation for direct and executor-backed channels.
- **SDE-2 Follow-up:** Design a restartable, partitioned settlement import that calls a non-transactional partner API exactly as safely as the API permits.
