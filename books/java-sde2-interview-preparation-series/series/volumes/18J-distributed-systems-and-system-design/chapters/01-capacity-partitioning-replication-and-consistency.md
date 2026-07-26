# Capacity, Partitioning, Replication, and Consistency

## Learning objectives

After this chapter, you should be able to:

- estimate throughput, storage, bandwidth, and concurrency with units and headroom;
- explain latency distributions and why queueing dominates near saturation;
- choose partition keys from routing, load distribution, locality, growth, and rebalancing needs;
- distinguish replication durability, availability, read scaling, and consistency goals;
- reason about quorums using explicit assumptions rather than the slogan `R + W > N`;
- apply CAP only to behavior during a network partition and PACELC to the latency/consistency tradeoff outside partitions;
- specify linearizable, sequential, causal, session, monotonic-read, read-your-writes, and eventual consistency contracts; and
- identify hot partitions, replica lag, split brain, failover, and repair requirements.

## 1. Numbers before boxes

### Throughput and concurrency

If a service receives 50 million requests/day:

```text
average RPS = 50,000,000 / 86,400 ≈ 579
peak factor 8 -> design peak ≈ 4,630 RPS
```

If 30% of requests call the database for an average 40 ms, average concurrent database work near peak is:

```text
L = λW = (4,630 * 0.30 requests/s) * 0.040 s ≈ 56 operations
```

Little's Law is a steady-state relationship, not a pool-sizing command. Tail latency, bursts, retries, long transactions, multiple queries per request, and database useful concurrency require headroom and measurement. Keep units on every estimate.

### Storage and bandwidth

For 10 million new events/day at 1.5 KiB average encoded size:

```text
raw/day ≈ 15 GiB
365 days ≈ 5.35 TiB
replication factor 3 ≈ 16 TiB before indexes, metadata, compaction, and headroom
```

Network egress for 4,630 RPS at 4 KiB response average is about 18 MiB/s before protocol overhead and replicas. Compression saves bandwidth at CPU/latency cost. Retention, secondary indexes, backups, multi-region copies, and write amplification can dominate raw payload.

### Latency is a distribution

End-to-end latency includes queueing, application work, serialization, network, downstream queueing, storage, and retries. Percentiles cannot generally be added to obtain an end-to-end percentile; distributions and correlation matter. An average of 50 ms can coexist with p99 of seconds.

As utilization approaches a bottleneck's capacity, queueing rises nonlinearly. Therefore:

- bound queues and concurrency;
- keep work inside a deadline;
- measure p50/p95/p99 and saturation together;
- shed low-priority work before all requests miss their deadlines;
- avoid retry storms that increase arrival rate during failure.

## 2. Partitioning

### Formal model

A partition function maps key `k` to partition `p`:

```text
p = f(k, partitionMetadata)
```

Hash partitioning spreads uniformly distributed keys and supports point routing. Range partitioning preserves ordered/range locality but can create hotspots for monotonic keys. Directory-based routing stores an explicit mapping and supports controlled placement at metadata complexity. Consistent-hashing families reduce remapping when membership changes but do not automatically solve skew, replicas, or operational rebalancing.

### Partition-key decision rule

Evaluate:

1. routing: can the common request name the key?
2. distribution: does real traffic—not only key count—spread?
3. locality: which data and operations must be co-located?
4. growth: can one tenant/key exceed a partition?
5. ordering/transactions: what scope needs them?
6. rebalancing: how are data and requests moved safely?
7. secondary access: what fan-out or index is required?
8. privacy/residency: must certain data stay in a region?

### Hot-partition walkthrough

Partitioning an event store by `customerId` appears uniform by number of customers. One enterprise tenant emits 30% of events, so its partition saturates. Adding partitions does not split that key. Options:

- use a compound shard key `(customerId, bucket)` and merge reads;
- dedicate capacity/partition placement for the whale tenant;
- separate tenant tiers/workloads;
- split time windows if query semantics permit;
- rate/admission limit the source;
- cache or aggregate hot reads.

Every split weakens single-key ordering/transaction locality and adds fan-out. State that tradeoff.

### Rebalancing protocol

Moving partition ownership needs more than copying files:

1. choose source and target under capacity constraints;
2. copy a snapshot while tracking subsequent changes;
3. catch up log/delta;
4. update routing metadata with a version/epoch;
5. reject or redirect stale routers;
6. cut over reads/writes under the store's protocol;
7. verify checksums/counts and lag;
8. retire old replica after rollback window.

Epoch/fencing tokens prevent an old owner from accepting writes after a new owner takes control. Exact protocols belong to the chosen datastore.

## 3. Replication

### Goals and costs

Replication can improve:

- availability under node failure;
- durability through independent copies;
- read throughput/locality;
- disaster recovery.

It also introduces lag, conflict, coordination, bandwidth/storage cost, failover state, and repair. Three copies in one failure domain do not provide regional disaster tolerance.

Common leadership models:

- **single leader:** one ordered write authority per shard; followers replicate; simple conflict model but leader/failover bottlenecks;
- **multi-leader:** writes accepted in several sites; lower local write latency but conflicts/loops/identity must be resolved;
- **leaderless/dynamo-style:** writes/reads contact subsets; application/store resolves versions and repairs; quorum slogans require careful assumptions;
- **consensus-replicated state machine:** a quorum agrees on an ordered log; strong semantics at coordination and availability/latency cost.

Do not infer a database's exact consistency from category alone. Read its documented protocol and configuration.

### Failover and fencing

Failure detection is suspicion based on time and observation. A slow or partitioned leader may still be alive. Promoting a new leader without preventing the old from writing creates split brain. Use consensus lease/term/epoch and fencing at the storage boundary. A distributed lock token without fencing cannot stop a paused old owner from resuming and overwriting new work.

Failover objectives include:

- **RPO:** acceptable committed data loss;
- **RTO:** acceptable service restoration time;
- read/write availability during election;
- client retry and duplicate behavior;
- DNS/routing/cache convergence;
- validation before old primary rejoins;
- backup restore independent from replica corruption.

Replication is not backup: deletion or corruption can replicate.

## 4. Quorums without slogans

In a simplified replicated register with `N` replicas, write acknowledgement count `W`, and read response count `R`, the intersection condition `R + W > N` suggests a read and acknowledged write set overlap. But “overlap” yields the latest value only if:

- versions are comparable under a correct protocol;
- reads choose/repair the newest version;
- membership is stable or uses epochs;
- failed/partitioned writes follow defined rules;
- sloppy quorums/hinted handoff do not substitute unrelated nodes without accounting;
- clocks are not misused for ordering;
- concurrent writes/conflicts have resolution semantics;
- the system actually implements the assumed model.

Likewise, `W > N/2` causes write quorums to intersect in the simplified model, but does not by itself implement linearizability. Consensus protocols require terms, log matching, commit rules, leader fencing, and membership-change safety.

### Example

`N=3, W=2, R=2` gives intersection. Client A's write reaches replicas 1 and 2. A read reaches 2 and 3; it sees the new version from 2 if the version is committed and selection is correct. During concurrent writes with last-write-wins timestamps, clock skew can discard an intended later business write. A logical version/vector/conflict merge may be needed depending on the datastore.

## 5. CAP and PACELC

### CAP's precise question

During a network partition that prevents required nodes from communicating, a replicated system cannot both:

- provide linearizable behavior for all operations; and
- make every request to every partitioned side complete successfully.

The design chooses behavior per operation/configuration: reject or delay some operations to preserve a consistency contract, or accept operations with weaker/conflict semantics. “CA system” in a distributed network is often an unhelpful label because partitions are a fault to handle, not an optional feature toggle.

CAP does not say a system is always consistent or always available. It does not describe latency in normal operation, transaction isolation, or durability.

### PACELC

PACELC adds: if there is a Partition, choose Availability or Consistency; Else, under normal operation, choose Latency or Consistency. Synchronous cross-region coordination can reduce stale reads but adds network latency and sensitivity to a remote quorum. Asynchronous replication improves local latency/availability but exposes lag and conflict windows.

Use these frameworks to ask concrete questions:

- Which operation and consistency model?
- What happens in a regional link partition?
- Which side accepts writes?
- What error/timeout does a client see?
- How is conflict reconciled?
- What normal-case coordination latency is paid?

## 6. Consistency models as client-visible contracts

- **linearizability:** each operation appears to take effect atomically between invocation and response, respecting real-time order;
- **sequential consistency:** operations appear in one total order preserving each process's program order, not necessarily real-time order;
- **causal consistency:** causally related operations are observed in causal order; concurrent operations may differ in order;
- **eventual convergence:** absent new updates and with successful communication/repair, replicas converge under the system's conflict rules;
- **read-your-writes:** a session sees its completed writes;
- **monotonic reads:** a session does not later observe an older version than it already saw;
- **monotonic writes:** a session's writes are applied in its order;
- **bounded staleness:** reads lag by at most a documented time/version bound under specified conditions.

Session guarantees can be implemented with sticky routing, version tokens, leader reads, quorum reads, or waiting for replica catch-up. Each has failure and latency tradeoffs. A client that switches regions can lose read-your-writes unless it carries a version/session token or routes to a sufficiently caught-up replica.

### Product decision examples

- user profile edit: read-your-writes is highly visible; route to leader or use a version token;
- social feed ranking: eventual/bounded staleness may be acceptable;
- payment balance/authorization: stronger serialized invariant and idempotent commands;
- analytics dashboard: snapshot/as-of semantics may be better than pretending “current” across sources;
- inventory display: bounded stale display may be okay, but reservation must use authoritative concurrency control.

## 7. Interview questions and model checkpoints

### Q1. How do you choose a shard key?

**Model checkpoint:** query routing, traffic skew, co-location/transaction/order scope, growth, rebalancing, fan-out, and residency. Hashing IDs is not sufficient if one ID is hot.

### Q2. Does `R + W > N` guarantee strong consistency?

**Model checkpoint:** only an intersection in a simplified model. Version selection, committed state, membership, conflict, sloppy quorum, repair, and actual datastore protocol determine semantics.

### Q3. Explain CAP without “pick two.”

**Model checkpoint:** during a communication partition, cannot make all operations on all sides both complete and preserve linearizability. Choice is per operation/configuration; normal-time latency is outside basic CAP.

### Q4. How do you provide read-your-writes from replicas?

**Model checkpoint:** leader read, sticky session, carry observed version and wait/route to caught-up replica, or stronger quorum/protocol. Bound the wait and handle failover.

### SDE-2 follow-ups

1. A tenant grows beyond one shard. Design split and routing migration while preserving identity.
2. A region is isolated for 20 minutes. Specify writes, reads, client errors, and reconciliation for orders versus product catalog.
3. Estimate capacity when retries double write traffic and replication is three-way.
4. Distinguish linearizability, serializability, and strict serializability in an interview.

## 8. Exercises

1. Estimate one year of storage, peak network, and concurrent DB calls for a workload; show units and three sensitivity scenarios.
2. Compare hash, range, time, and tenant partitioning for an event service. Identify hot keys and query fan-out.
3. Simulate `N=5` quorum choices and list assumptions needed for a latest-value read.
4. Write client-visible contracts for regional failover under linearizable and eventual product choices.
5. Design a rebalancing runbook with epochs, validation, throttling, rollback, and observability.

## 9. Summary checklist

- [ ] Estimates show units, peaks, replication, indexes, retention, and headroom.
- [ ] Latency is treated as a distribution with queueing and saturation.
- [ ] Partition key handles traffic skew, locality, growth, and rebalance.
- [ ] Replication goals and failure domains are explicit.
- [ ] Failover includes fencing, client semantics, RPO/RTO, and repair.
- [ ] Quorum claims list protocol assumptions.
- [ ] CAP is scoped to partition behavior; PACELC covers normal latency tradeoff.
- [ ] Every consistency model is phrased as an observable client contract.

## 10. Multi-region decision laboratory

Assume users in North America and Europe create and read orders. Product asks for low latency and “no lost orders.” Do not jump directly to active-active writes. Decompose the promises:

- accepted create must be durable under which failures?
- can one order receive commands from both regions concurrently?
- must a European client immediately read a North American write?
- can the service reject writes during inter-region partition?
- what RPO/RTO applies to region loss?
- where may personal data reside?

### Option A: home-region single writer

Assign each order/customer a home region. Route writes there; asynchronously replicate read views elsewhere. Carry home/observed version in session/routing metadata.

Benefits: one write authority simplifies order invariants and per-order sequence. Costs: remote write latency for traveling users, home-region outage/failover, routing metadata, stale remote reads. During link partition, non-home region can serve bounded-stale reads but rejects/queues writes according to policy. Promotion uses a term/epoch and fencing so the old home cannot resume writes.

### Option B: synchronous cross-region quorum

Commit requires replicas across failure domains. This can reduce RPO and provide stronger global semantics under its consensus protocol. It adds wide-area round-trip latency to writes and can reject them if quorum is unavailable. “Three replicas” is insufficient detail: placement and quorum membership determine which region failures are tolerated.

### Option C: multi-writer with conflict resolution

Both regions accept writes. This improves local write availability/latency during partition but requires conflict semantics. Last-write-wins based on wall clock is dangerous for order transitions. A domain merge might be possible for independent profile fields, but `CANCELLED` versus `SHIPPED` is not a mechanical merge. Escrow/reservation, single-aggregate leadership, CRDTs for suitable data, or workflow reconciliation may narrow the conflict domain.

### Partition walkthrough

At `t0`, link fails. Europe accepts a cancellation while North America ships the order.

- Under home-region consistency, Europe should route/fail the write if North America is home; it must not acknowledge cancellation it cannot serialize.
- Under synchronous quorum, one or both sides lack quorum and rejects/blocks within deadline.
- Under multi-writer availability, both can commit and reconciliation needs a business outcome: stop shipment if possible, return/refund, audit, and prevent stale state overwrite.

CAP appears as a concrete product choice, not a label.

### Disaster recovery is separate

Replicas can faithfully copy accidental deletion, bad migration, or application corruption. Maintain tested backups/point-in-time recovery, immutable/offline policy as required, restore drills, schema/config artifacts, secret/key recovery, and reconciled restart. Record RPO/RTO from observed drills, not architecture diagrams.

### Multi-region observability

Track per region and cross-region:

- replication apply/commit lag in time and versions;
- quorum/leader/term changes and fencing failures;
- remote routing latency and error;
- stale-read/session-token waits;
- conflict/reconciliation count and oldest age;
- data-residency policy violations;
- regional SLI/error-budget burn;
- failover RTO and post-failover validation.

Do not tag every tenant in metrics. Use tenant tier/region and investigate identities through controlled logs/traces.

### Laboratory checkpoint

A defensible multi-region answer specifies one operation's write authority, acknowledgement rule, read source, partition response, conflict behavior, failover fencing, RPO/RTO, and data residency. “Active-active” is a topology adjective, not a consistency contract.

## Primary references

- Gilbert and Lynch, “Brewer's Conjecture and the Feasibility of Consistent, Available, Partition-Tolerant Web Services”: <https://dl.acm.org/doi/10.1145/564585.564601>
- Ongaro and Ousterhout, “In Search of an Understandable Consensus Algorithm (Raft)”: <https://raft.github.io/raft.pdf>
- Apache Kafka Design Documentation, replication background: <https://kafka.apache.org/documentation/#design_replicatedlog>
- OpenJDK Java 21 documentation: <https://docs.oracle.com/en/java/javase/21/>

> **Version boundary:** partitioning, quorum, leader election, consistency, and failover are datastore-specific contracts. Categories in this chapter are reasoning tools, not guarantees about a named product. Java examples in this mini-book compile on Java 21; later JDK features are separated as optional deltas.
