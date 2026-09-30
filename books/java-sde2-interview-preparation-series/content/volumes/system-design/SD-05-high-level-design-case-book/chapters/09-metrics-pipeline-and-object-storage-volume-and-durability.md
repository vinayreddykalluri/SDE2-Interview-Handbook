# 9. Metrics Pipeline and Object Storage: Volume and Durability

## Learning objectives

By the end of this chapter, you should be able to:

- size a metrics pipeline by series count and resolution, and separate the effects of compression and downsampling;
- design ingestion, storage, and query paths for time-series data;
- explain cardinality as the failure mode of metrics systems;
- compare replication and erasure coding by storage overhead and loss probability; and
- design an object store's metadata and data paths.

## Why this matters at SDE-2

"Design a monitoring system" and "design S3" look unrelated but share the same core: very large volumes of data whose cost is dominated by storage, where the design is decided by arithmetic about bytes rather than by request routing. Candidates who reason about requests per second and ignore bytes per day tend to produce designs that are correct and unaffordable.

## First-principles model

Both systems separate **a small, strongly consistent metadata layer** from **a large, cheap, append-oriented data layer**.

- In a metrics system, metadata is the set of series and their labels; data is the stream of timestamped values.
- In an object store, metadata is the bucket, key, and location of each object's pieces; data is the bytes.

Durability and cost are decided in the data layer, by how many bytes you keep and how you protect them.

## Core terminology

- **Time series:** a stream of timestamped values identified by a metric name and a set of labels.
- **Cardinality:** the number of distinct series, driven by label combinations.
- **Downsampling (rollup):** replacing fine-grained points with aggregates over coarser intervals.
- **Delta-of-delta and XOR encoding:** the compression scheme from Facebook's Gorilla paper, which stores timestamps and values as small differences.
- **Replication factor:** full copies of each object.
- **Erasure coding RS(k, m):** splitting data into `k` data fragments plus `m` parity fragments; any `k` of the `k + m` reconstruct it.
- **Repair window:** the time between a node's failure and the restoration of its data elsewhere.

## Detailed mechanics

### Sizing a metrics system

Start from series and resolution, not from hosts. The companion's numbers for one million series scraped every 10 seconds:

```text
1,000,000 series every 10s: 8,640,000,000 points/day
16 bytes/point raw: 138 GB/day; ~1.37 bytes/point compressed: 11.8 GB/day
```

Sixteen bytes is a 64-bit timestamp and a 64-bit value. The 1.37 bytes per point is the average the Gorilla paper (Pelkonen et al., VLDB 2015) reports for its delta-of-delta timestamp and XOR value encoding on production data. Your ratio depends on your data; state it as the published figure, not a guarantee.

Over a year, two levers matter:

```text
one year at 10s: 50.5 TB raw, 4.32 TB compressed
compressed and tiered (10s for 14d, 1m for 90d, 1h for 1y): 0.36 TB
compression saves 11.7x; tiering saves a further 12.2x
```

Compression and downsampling are separate and roughly equal levers here, and together they reduce a year of storage by more than a hundred times. Keeping every 10-second point for a year is rarely what anyone needs: dashboards over weeks use minute or hour rollups anyway.

### Ingestion, storage, and query

```text
agents -> ingest gateways -> partitioned log (Kafka) -> TSDB writers -> blocks on disk / object store
                                                               ^
                               query service  -----------------+ (recent data from memory, old from blocks)
```

- **Ingest** validates, rate-limits per tenant, and appends to a partitioned log. The log decouples bursts from storage and allows replay.
- **Writers** keep the most recent hours in memory, compressed per series, and flush immutable blocks - for example every two hours - to local disk and then to object storage.
- **Compaction** merges small blocks into larger ones and produces the rollups.
- **Queries** fan out to the partitions holding the requested series and time range, reading recent data from memory and older data from blocks.

Partition by series - a hash of metric name and labels - so that each series' points land together, which is what makes compression effective.

### Cardinality is the failure mode

The number of series is the product of label values. Add a `user_id` label to a request-latency metric and one metric becomes a million series; add a `request_id` label and it becomes unbounded. Memory in the writers grows with active series, so a single bad label can take the system down. Defences: per-tenant series limits enforced at ingest, rejecting or dropping labels that exceed a cardinality budget, and alerting on the rate of new series. Mention this without being asked: it is the most common real-world failure of metrics systems.

### Replication or erasure coding?

For an object store, the durability question is: what is the probability that enough pieces of an object are lost before the repair process restores them? The companion assumes each node independently has a 1% chance of failing within one repair window:

```text
3x replication      overhead 3.00x  P(loss per object) 1.00e-06
RS(6,3) erasure     overhead 1.50x  P(loss per object) 1.21e-06
RS(10,4) erasure    overhead 1.40x  P(loss per object) 1.86e-07
```

Three observations, each worth saying in an interview:

- **RS(10,4) is about five times more durable than 3x replication at under half the storage.** For large, cold data, that is why object stores use erasure coding.
- **RS(6,3) is not automatically better than replication**: at this failure rate it halves the storage but is slightly *less* durable. The parameters matter, not the technique's name.
- **The repair window is the real lever.** The probability of loss rises steeply with the failure probability, and that probability is proportional to how long repair takes. Faster detection and parallel repair across many nodes improve durability more than adding parity.

Erasure coding has costs: reading a small object may touch `k` nodes, and repairing one lost fragment requires reading `k` others, so it suits large objects. Many systems replicate small or hot objects and erasure-code large or cold ones.

Independent failures are the model's big assumption. Real failures correlate - a rack, a power feed, a bad firmware rollout - so place fragments across failure domains, and say that the independent-failure numbers are an upper bound on reality.

### The object store's two paths

- **Metadata:** `bucket, key -> object id, size, checksum, fragment locations, version`. Strongly consistent, partitioned by bucket and key, and small relative to the data. Listing by prefix needs the metadata to be ordered by key within a bucket.
- **Data:** large objects are split into chunks, each chunk erasure-coded or replicated across storage nodes in different failure domains. Uploads are multipart, so a 50 GB upload can resume after a failure.
- **Integrity:** store a checksum per chunk, verify on read, and run a background scrubber that re-verifies data at rest and repairs silent corruption.

## Failure modes and common mistakes

- **Sizing by hosts instead of series.** Series count and resolution determine cost.
- **Unbounded label cardinality.** One label can multiply series by millions.
- **Keeping full resolution forever.** Tiered rollups cut storage by an order of magnitude.
- **Assuming erasure coding always beats replication.** Compare loss probability for the actual parameters.
- **Ignoring the repair window and correlated failures.** Both dominate real durability.
- **No checksums or scrubbing.** Silent corruption goes undetected until a read fails.

## Interview questions and model answers

**Q: How much storage does a metrics system need?**
A: From series and resolution. A million series at 10 seconds is 8.64 billion points a day - 138 GB raw at 16 bytes per point, about 12 GB with Gorilla-style compression. For a year, tiered rollups - 10 seconds for two weeks, one minute for 90 days, one hour for a year - bring it from 4.3 TB compressed to about 0.36 TB.

**Q: What breaks a metrics system in practice?**
A: Cardinality. A label like a user id turns one metric into millions of series, and writer memory grows with active series. I enforce per-tenant series limits at ingest and alert on new-series rate.

**Q: Replication or erasure coding for an object store?**
A: Depends on object size and the parameters. With a 1% chance of a node failing within the repair window, RS(10,4) loses an object with probability about 2e-7 at 1.4x overhead, against 1e-6 at 3x for triple replication - so for large objects erasure coding wins on both. RS(6,3) is slightly less durable than 3x at that rate, so the parameters matter. Small or hot objects I'd replicate, because erasure coding makes small reads and repairs expensive.

**Q: How do you protect against silent corruption?**
A: Checksums per chunk, verified on every read, and a background scrubber that re-reads and repairs data at rest.

## Exercises

1. Recompute the metrics storage for 20 million series at 15-second resolution with the same tiering.
2. Compute the loss probability for RS(12,4) and for 2x replication at a 1% failure rate. Which would you use for a backup tier?
3. Design `LIST bucket prefix=logs/2026/` over a metadata store partitioned by key. What partitioning makes it efficient?

## Chapter summary

Metrics systems and object stores split a small, consistent metadata layer from a large data layer whose cost is bytes. Size metrics by series and resolution: compression and tiered rollups each cut storage by roughly an order of magnitude, and cardinality is the failure mode to guard. For durability, compare actual loss probabilities: RS(10,4) beats triple replication at under half the storage, RS(6,3) does not, and the repair window and correlated failures matter more than the choice of technique.

## Revision checklist

- [ ] I can size points per day and storage per year from series and resolution.
- [ ] I can separate the savings from compression and from downsampling.
- [ ] I can explain cardinality and three defences.
- [ ] I can compare 3x, RS(6,3) and RS(10,4) with numbers.
- [ ] I can describe the metadata and data paths of an object store.
