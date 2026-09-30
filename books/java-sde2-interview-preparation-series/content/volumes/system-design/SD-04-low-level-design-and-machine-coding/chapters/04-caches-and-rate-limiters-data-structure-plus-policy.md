# 4. Caches and Rate Limiters: Data Structure Plus Policy

## Learning objectives

By the end of this chapter, you should be able to:

- implement an LRU cache two ways - on `LinkedHashMap` and by hand - and explain when each is the right answer in an interview;
- implement an O(1) LFU cache and prove it against a simple reference model;
- explain why a `ReadWriteLock` does not make an access-ordered LRU cache faster;
- add time-to-live expiry with an injected clock, and name the memory leak that lazy expiry alone creates;
- implement fixed-window, sliding-log, and token-bucket rate limiters and show the burst that separates them; and
- choose between them with a stated reason.

## Why this matters at SDE-2

"Design an LRU cache" and "design a rate limiter" are the two most common LLD prompts that are really data-structure prompts. Both have a textbook answer every candidate has memorised. The discrimination comes from the follow-ups: thread safety, eviction ties, expiry, the boundary burst, and time as a dependency. A candidate who writes a correct LRU and then says "and I'd add `synchronized`" without explaining why reads need exclusive access has shown they memorised the structure without understanding it.

## First-principles model

Both problems have the same shape: **a data structure that answers "what now?" in O(1), plus a policy that decides what to forget.**

- An **LRU cache** is a hash map (find by key) plus a recency order (find the victim). Every hit changes the order.
- An **LFU cache** is a hash map plus a frequency order with a recency tie-break.
- A **TTL cache** adds a deadline per entry and a decision about *when* to check it.
- A **rate limiter** is a counter over time. The whole design question is how "over time" is defined: which window, and what happens at its edge.

Time is an input. Code that calls `System.currentTimeMillis()` directly cannot be tested at a boundary; code that takes a `LongSupplier` clock can be tested at the exact millisecond that matters.

## Core terminology

- **LRU (least recently used):** evict the entry whose last access is oldest.
- **LFU (least frequently used):** evict the entry with the fewest accesses; break ties by recency.
- **Access order:** a `LinkedHashMap` mode in which `get` moves the entry to the end.
- **TTL (time to live):** a duration after which an entry is treated as absent.
- **Lazy expiry:** removing an expired entry only when it is next read.
- **Fixed window:** count requests per aligned time bucket.
- **Sliding log:** keep a timestamp per request and count those within the last window.
- **Token bucket:** tokens refill at a steady rate up to a capacity; each request spends one.

## Detailed mechanics

### LRU on `LinkedHashMap`

The standard library already implements LRU. Two constructor arguments and one override:

```java
this.map = new LinkedHashMap<>(16, 0.75f, true) {          // true = access order
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
};
```

In an interview, write this first and say so. Then ask whether they want the structure by hand. Many interviewers do - the point of the question is the linked list - but offering the library version first shows you would not hand-roll it in production.

### LRU by hand

A `HashMap<K, Node>` for lookup plus a doubly linked list for order, with sentinel `head` and `tail` nodes so that no operation has to special-case an empty list:

```java
public Optional<V> get(K key) {
    Node<K, V> node = index.get(key);
    if (node == null) {
        return Optional.empty();
    }
    unlink(node);
    linkFront(node);
    return Optional.of(node.value);
}
```

`put` either updates and moves an existing node, or evicts `tail.prev` when full and links a new node at the front. Every operation is O(1).

**How do you know it is right?** Compare it against the library version. The companion runs 200,000 random `get` and `put` operations over 20 keys against both, with capacity 8, comparing every result and every size:

```text
hand-rolled LRU vs LinkedHashMap LRU: 0 mismatches over 200,000 ops
```

This is the most useful testing technique in the round: when a simple, obviously correct implementation exists, use it as the oracle for the fast one.

### Why reads need an exclusive lock

In an access-ordered `LinkedHashMap`, `get` is a *structural modification* - the Javadoc says so explicitly - because it moves the entry to the end of the list. The same is true of the hand-rolled version: `get` rewires four pointers.

So a `ReadWriteLock` with `get` under the read lock is a data race: two concurrent readers both relink nodes and can corrupt the list. Every operation needs the exclusive lock, which is why the companion's `LinkedHashMapLru` makes `get` `synchronized`. For high read concurrency, the real options are:

- **Sharding:** N independent LRU caches selected by key hash. Eviction becomes approximately LRU per shard.
- **Buffered recency:** record accesses in a lock-free buffer and apply them to the order in batches. This is how Caffeine achieves near-LRU behaviour with concurrent reads.
- **A different policy:** CLOCK, which sets a reference bit on read instead of relinking.

Saying "reads mutate the recency order, so a read lock is wrong; I would shard or buffer" is one of the clearest SDE-2 signals in this problem.

### LFU in O(1)

LFU needs three maps and one integer:

```text
values:   key -> value
counts:   key -> access count
buckets:  count -> keys with that count, in recency order (LinkedHashSet)
minFrequency: the smallest count that has a non-empty bucket
```

On access, move the key from bucket `c` to bucket `c + 1`; if bucket `c` became empty and `c` was the minimum, the minimum becomes `c + 1`. On insert when full, evict the first (least recent) key of the minimum bucket, then insert the new key with count 1 and set the minimum to 1. Every step is O(1).

The tie-break matters and is often left unstated. Among keys with the lowest count, the companion evicts the least recently used, because `LinkedHashSet` iterates in insertion order and a key is re-inserted when its count changes.

Checked the same way as LRU, against a reference that simply scans every key for the lowest count and oldest use:

```text
O(1) LFU vs linear-scan reference:     0 mismatches over 200,000 ops
```

### LRU and LFU disagree - on purpose

One sequence shows the difference. Capacity 2:

```text
put a, put b, get a, get a, get b, put c:  LRU keeps a=false b=true | LFU keeps a=true b=false
```

LRU evicts `a` because `b` was touched more recently. LFU evicts `b` because `a` was used three times and `b` twice. Neither is "correct"; they encode different beliefs about the workload. LRU suits recency-driven access such as sessions; LFU suits stable popularity such as a product catalogue, but it clings to entries that were popular once, which is why production systems often use windowed or aged frequency.

### Time to live, and the leak lazy expiry creates

A TTL cache stores a deadline with each entry and treats an entry as absent at or after its deadline. With an injected clock, the boundary is testable to the millisecond:

```text
put at t=0 with ttl 1,000   ->  get at t=999: hit    get at t=1,000: miss
```

Lazy expiry removes an expired entry when it is read. It is simple and needs no background thread. But entries that are never read again are never removed:

```text
1,000 entries written with 10ms TTL, none read: 1,000 still stored after 4s
```

That is a memory leak with a correct-looking API. The fix is to pair lazy expiry with one of: a size bound (the LRU layer evicts old entries anyway), a periodic sweep over a sample of keys (what Redis does), or a priority queue ordered by deadline. State which one you are using.

### Rate limiting: three algorithms, one boundary

Limit: 10 requests per 10 seconds. Fifteen requests arrive at t = 9.9 s and fifteen more at t = 10.0 s - 100 milliseconds apart, straddling a window boundary. The companion runs each limiter:

```text
fixed window  15 at t=9.9s -> 10, 15 at t=10.0s -> 10, total 20 in 100ms
sliding log   15 at t=9.9s -> 10, 15 at t=10.0s ->  0, total 10 in 100ms
token bucket  15 at t=9.9s -> 10, 15 at t=10.0s ->  0, total 10 in 100ms
```

The **fixed window** resets its counter at t = 10.0 s and admits twice the limit within 100 ms. That boundary burst is the reason the question exists.

The **sliding log** keeps each admitted timestamp in a deque, drops those older than the window, and admits only if fewer than 10 remain. It is exact, and costs memory proportional to the limit per client.

The **token bucket** holds up to 10 tokens and refills one per second. It permits a burst up to its capacity, then enforces the average rate:

```text
token bucket drained at t=0, 10 attempts at t=3.5s -> 3 allowed
```

The companion stores tokens in thousandths, so refilling one token per second is exactly one milli-token per millisecond and no floating point is involved. Floating-point token counts accumulate rounding error over millions of refills; integer fixed-point does not.

### Choosing a limiter

| Algorithm | Memory per client | Boundary burst | Use when |
|---|---|---|---|
| Fixed window | one counter | up to 2x limit | approximate limits, very high cardinality |
| Sliding window counter | two counters | small, approximate | good default at scale |
| Sliding log | one timestamp per admitted request | none | small limits that must be exact |
| Token bucket | one counter and one timestamp | up to capacity, by design | APIs that should allow short bursts |

The sliding window *counter* - weighting the previous window's count by how much of it overlaps the current sliding window - is the common production compromise: two integers per client and a small error. Mention it as the answer to "the sliding log uses too much memory".

In a distributed deployment, the limiter's state must live somewhere every instance can reach atomically: typically Redis, with the check-and-increment done in one Lua script so that two instances cannot both admit the tenth request. That is the same check-then-act problem as parking-spot allocation, one level up.

## Failure modes and common mistakes

- **Read lock on an LRU `get`.** Reads mutate recency; it is a race.
- **Forgetting to move on update.** `put` of an existing key must also refresh recency.
- **LFU without a tie-break.** Unspecified eviction among equal counts is non-deterministic and untestable.
- **`System.currentTimeMillis()` inside the cache.** Makes boundary tests impossible; inject a clock.
- **Lazy expiry only.** Correct reads, unbounded memory.
- **Fixed window presented as exact.** It admits up to twice the limit across a boundary.
- **Floating-point tokens.** Drift over time; use fixed-point integers.

## Interview questions and model answers

**Q: Implement an LRU cache.**
A: In production I would extend `LinkedHashMap` with access order and override `removeEldestEntry` - three lines. If you want the structure itself: a hash map from key to node, plus a doubly linked list with sentinel head and tail. `get` moves the node to the front; `put` evicts from the back when full. All O(1). I would test it against the `LinkedHashMap` version with random operations.

**Q: Make it thread-safe. Can reads share a lock?**
A: No. In an access-ordered cache, `get` relinks the node, so it is a write. A read lock would let two readers corrupt the list. Every operation takes the exclusive lock. If that is too slow, I would shard by key hash or buffer recency updates, as Caffeine does.

**Q: What does LFU evict when two keys have the same count?**
A: I evict the least recently used among them. Keeping each frequency bucket as a `LinkedHashSet` gives that order for free. I would state the tie-break explicitly because otherwise eviction is non-deterministic.

**Q: Why not just use a fixed window counter for rate limiting?**
A: Because it admits up to twice the limit across a window boundary. With 10 per 10 seconds, 10 requests at 9.9 s and 10 more at 10.0 s all pass - 20 in 100 ms. A sliding log or token bucket admits 10. I would use a token bucket if short bursts are acceptable and a sliding window counter if memory per client matters.

**Q: How does the limiter work across 20 API servers?**
A: The counter moves to shared storage - usually Redis - and the check and increment happen atomically in one script, so two servers cannot both admit the last request. I would add a local token bucket in front as a first line of defence to cut Redis traffic, accepting that the global limit is then slightly soft.

## Exercises

1. Add `remove(key)` to the hand-rolled LRU and extend the random comparison to cover it.
2. Implement the sliding window counter and measure how far it is from the sliding log on the boundary scenario.
3. Add a background sweep to the TTL cache that removes at most N expired entries per call, and show the 1,000-entry leak disappearing.
4. Make the token bucket per client with a bounded map of clients. What evicts idle clients?

## Chapter summary

Caches and rate limiters are a fast data structure plus a forgetting policy. Write the library LRU first, then the hand-rolled one, and prove the second against the first. LFU is O(1) with frequency buckets and a stated recency tie-break. Access-ordered reads are writes, so they need exclusive locking or a different concurrency design. Inject time. Lazy expiry needs a bound or a sweep. The fixed window admits a boundary burst of twice its limit; sliding logs and token buckets do not.

## Revision checklist

- [ ] I can write both LRU implementations and the random comparison test.
- [ ] I can explain why a read lock is wrong for LRU `get`.
- [ ] I can implement O(1) LFU with `minFrequency` and a recency tie-break.
- [ ] I can name the lazy-expiry leak and two fixes.
- [ ] I can show the fixed-window boundary burst and choose a limiter with a reason.
