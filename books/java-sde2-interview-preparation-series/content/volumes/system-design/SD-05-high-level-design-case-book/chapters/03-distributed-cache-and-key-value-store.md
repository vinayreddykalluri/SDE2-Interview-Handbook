# 3. Distributed Cache and Key-Value Store

## Learning objectives

By the end of this chapter, you should be able to:

- explain why `hash(key) mod N` fails when the cluster changes size, with the fraction of keys it moves;
- implement consistent hashing and show, with measurements, why virtual nodes are needed;
- choose replication factor and read/write quorums for a stated consistency requirement;
- handle hot keys, cache stampedes, and node failure; and
- say which of these belong in a cache and which in a durable key-value store.

## Why this matters at SDE-2

"Design a distributed cache" or "design a key-value store" is asked because it exposes whether a candidate understands partitioning as a mechanism rather than a word. Most candidates say "consistent hashing". Far fewer can say what it fixes, why a naive ring is badly unbalanced, or what happens to a key that becomes a million times hotter than its neighbours. The numbers in this chapter are the ones to reach for.

## First-principles model

A distributed key-value system answers two questions for every key: **which nodes own it**, and **how many of them must agree** before a read or write succeeds.

- **Ownership** is partitioning. The goal is even load and minimal data movement when nodes join or leave.
- **Agreement** is replication. The goal is a stated consistency level at an acceptable latency and availability cost.

A cache and a store use the same two mechanisms with different defaults: a cache can lose data and rebuild it from the source of truth, so it usually replicates little and favours availability; a store cannot, so it replicates and uses quorums.

## Core terminology

- **Partitioning (sharding):** assigning each key to a node.
- **Consistent hashing:** placing nodes and keys on a hash ring; a key belongs to the next node clockwise.
- **Virtual node:** one of many ring positions owned by a single physical node.
- **Replication factor (N):** the number of nodes holding each key.
- **Quorum (R, W):** the number of replicas that must answer a read or acknowledge a write.
- **Hot key:** a key whose traffic far exceeds the average.
- **Cache stampede:** many concurrent misses on one key all hitting the source at once.

## Detailed mechanics

### Why `mod N` fails

The simplest partitioning is `node = hash(key) mod N`. It balances well and is one line. The problem appears when `N` changes. The companion places 1,000,000 keys on 10 nodes and adds an eleventh:

```text
add an 11th node: ring moves 8.8% of keys, hash mod N moves 90.9%
ideal is 1/11 = 9.1%
```

With `mod N`, about 10 of every 11 keys change owner. For a cache, that is a near-total miss storm at the moment you added capacity - usually because you were already under load. For a store, it is a migration of almost the whole dataset.

### Consistent hashing

Hash each node to a position on a ring of 64-bit values; hash each key onto the same ring; a key belongs to the first node clockwise from it. A `TreeMap<Long, String>` makes the lookup one `ceilingEntry` call:

```java
String owner(String key) {
    Map.Entry<Long, String> e = ring.ceilingEntry(hash(key));
    return e != null ? e.getValue() : ring.firstEntry().getValue();
}
```

Adding a node takes over only the arc between it and its predecessor, so only about `1/(N+1)` of keys move - the companion measures 8.8% against an ideal of 9.1%.

### Why virtual nodes are not optional

With one position per node, the arcs between positions are very uneven, and so is the load:

```text
  1 virtual nodes each: busiest node holds 2.55x the mean
 10 virtual nodes each: busiest node holds 1.61x the mean
100 virtual nodes each: busiest node holds 1.14x the mean
200 virtual nodes each: busiest node holds 1.12x the mean
```

With a single position each, one node carries two and a half times its fair share, so capacity planning must be done for the busiest node and most of the fleet sits idle. A hundred virtual nodes per physical node brings the worst case within about 15% of the mean. Virtual nodes also make heterogeneous hardware easy - a node with twice the memory gets twice the virtual nodes - and spread a failed node's load across many survivors instead of dumping it all on one neighbour.

An alternative used by Redis Cluster is a fixed number of **hash slots** (16,384), assigned to nodes by a table. It has the same property - adding a node moves only the slots reassigned to it - with an explicit, inspectable mapping instead of a hashed ring. Mention it as the same idea with a different bookkeeping trade-off.

### Replication and quorums

For a durable store, replicate each key to the next `N` distinct physical nodes on the ring. Reads and writes then choose how many replicas to wait for:

```text
W + R > N   -> every read quorum overlaps every write quorum: reads see the latest acknowledged write
W + R <= N  -> reads may miss the latest write: eventual consistency, lower latency
```

With `N = 3`, `W = 2, R = 2` gives read-your-latest-write behaviour while tolerating one slow or failed replica on each side. `W = 1, R = 1` is fastest and most available, and gives no such guarantee. State the choice per use case: a session cache can take `W = 1, R = 1`; an account setting that the user just changed should not.

Quorums need two supporting mechanisms, which you should name: **read repair** (a read that sees disagreeing replicas writes the newest value back to the stale ones) and **hinted handoff** (a write whose target replica is down is held by another node and delivered when it returns). Conflict resolution - last-writer-wins by timestamp, or version vectors when concurrent writes must be merged - is the next question an interviewer will ask.

### Hot keys

Consistent hashing balances *keys*, not *traffic*. One celebrity's profile or one flash-sale item can send a large share of all reads to one node. Remedies, in increasing complexity:

- **Local caching** in the application tier for a few seconds. Most effective and simplest; it trades a little staleness for a large drop in remote reads.
- **Key replication for reads:** store the hot key under `key#1..key#k` on different nodes and read from a random copy. Writes must update every copy.
- **Request coalescing:** at most one in-flight fetch per key per node; other requests for the same key wait for that result.

Detecting hot keys needs per-key counters sampled at the proxy or client - a count-min sketch keeps that bounded in memory.

### Cache stampede

When a popular key expires, thousands of concurrent requests miss at once and all go to the database. Fixes:

- **Request coalescing** (single-flight), as above.
- **Early probabilistic refresh:** a request that finds a key close to expiry refreshes it with a small probability, so one request refreshes it before the crowd arrives.
- **Stale-while-revalidate:** serve the expired value while one request fetches the new one.

### Eviction and memory

Each cache node evicts with LRU or an approximation of it (SD-04 chapter 4 covers the data structures). At fleet scale, the important numbers are the working set and the hit rate: size the fleet so that the working set fits, and alert when the hit rate drops, because a falling hit rate is the leading indicator of database overload.

## Failure modes and common mistakes

- **`mod N` partitioning** in a system that will ever resize.
- **A ring without virtual nodes.** The busiest node carried 2.55 times the mean in the measurement above.
- **Quorum settings without a stated consistency goal.** `N`, `R` and `W` are answers to a requirement, not defaults.
- **Assuming consistent hashing solves hot keys.** It balances keys, not traffic.
- **Treating the cache as durable.** If the design fails when the cache is empty, the cache is a database without the guarantees of one.
- **No stampede protection** on popular keys with a TTL.

## Interview questions and model answers

**Q: How do you partition keys across cache nodes?**
A: Consistent hashing with virtual nodes. With `mod N`, adding an eleventh node to ten moves about 91% of keys; the ring moves about 9%. Without virtual nodes the ring is badly unbalanced - one node carried 2.55 times the mean in a million-key test - so I use around a hundred per physical node, which brought the worst case to about 1.14 times the mean.

**Q: How do you make reads consistent in a replicated store?**
A: Choose quorums so that `R + W > N`. With three replicas, writing to two and reading from two guarantees overlap, so a read sees the latest acknowledged write, and each side still tolerates one slow replica. I'd add read repair and hinted handoff, and choose last-writer-wins or version vectors for conflicts.

**Q: One key gets 100 times the traffic of any other. What happens?**
A: Its owning node saturates, because hashing balances keys, not traffic. I'd add a short-lived local cache in the application tier first, then replicate the hot key under several suffixed keys for reads if needed, with coalescing so a miss triggers only one fetch.

**Q: A hot key expires and the database falls over. Why, and how do you prevent it?**
A: Every concurrent request missed at once and went to the database - a stampede. Single-flight coalescing, early probabilistic refresh, or serving stale data while one request revalidates all prevent it.

## Exercises

1. Compute the fraction of keys that move when a 10-node `mod N` cluster grows to 12 nodes. Compare with a ring.
2. Choose `N`, `R` and `W` for a shopping cart, a user's session token, and a bank balance, and justify each.
3. Design hot-key detection with bounded memory, and say where in the request path it runs.

## Chapter summary

Partitioning decides ownership; replication decides agreement. `mod N` moves almost every key when the cluster changes; consistent hashing moves about `1/(N+1)`, but only balances load with many virtual nodes per physical node. Quorums with `R + W > N` give reads that see the latest acknowledged write. Hashing balances keys, not traffic, so hot keys and stampedes need their own defences.

## Revision checklist

- [ ] I can state the fraction of keys moved by `mod N` and by a ring when a node is added.
- [ ] I can explain why virtual nodes are needed, with the imbalance numbers.
- [ ] I can pick `N`, `R` and `W` for a stated consistency goal.
- [ ] I can list three hot-key defences and three stampede defences.
