# 4. Composite Structures and Open Addressing

## Why this chapter exists

Two things are missing from a hashing volume that stops at chaining and the `equals`/`hashCode` contract.

The first is **composition**. "Design an LRU cache" is one of the most-asked data-structure questions in the industry, and it is not a hash question or a list question - it is the observation that a hash map gives O(1) *lookup* and a doubly-linked list gives O(1) *reordering*, and that you need both. Candidates who know each structure separately still fail this if they have never combined them.

The second is **open addressing**, the collision strategy the chapter on internals did not cover. It is what most high-performance hash tables actually use, its deletion semantics are genuinely subtle, and "why not just store everything in the array?" is a fair interview question with a real answer.

> **Scope note:** JDK-specific `HashMap` behaviour - bucket treeification, the exact resize policy, hash spreading - belongs to the Collections and Streams volume, which covers it against the implementation. This chapter stays at the level of structure design, which is what the coding round tests.

## Part 1: LRU cache

### The requirement drives the structure

*Fixed capacity. `get(key)` and `put(key, value)` both O(1). When full, evict the least recently used entry.*

Work the requirement backwards:

- O(1) lookup by key forces a **hash map**.
- Eviction needs to identify the least recently used entry in O(1), so recency must be an *order* you maintain, not something you search for.
- Every `get` and `put` moves an entry to the most-recent end. Moving a node in O(1) requires knowing its neighbours, which forces a **doubly-linked** list.

A singly-linked list is the trap. You can find the node via the map, but unlinking it needs its predecessor, and finding that is O(n). The `prev` pointer is not decoration; it is what makes the whole design O(1).

```text
map:  key -> node

head <-> A <-> B <-> C <-> tail
        MRU          LRU

get(B): unlink B, reinsert after head
evict:  remove tail.prev
```

Sentinel `head` and `tail` nodes that are never removed eliminate every null check in the link and unlink paths. That is worth doing on a whiteboard, because null-handling in pointer surgery is where candidates lose time.

### Implementation

```java
import java.util.HashMap;
import java.util.Map;

public final class LruCache<K, V> {

    private static final class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> index = new HashMap<>();
    private final Node<K, V> head = new Node<>(null, null);   // sentinel, MRU side
    private final Node<K, V> tail = new Node<>(null, null);   // sentinel, LRU side

    public LruCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public V get(K key) {
        Node<K, V> node = index.get(key);
        if (node == null) {
            return null;
        }
        moveToFront(node);
        return node.value;
    }

    public void put(K key, V value) {
        Node<K, V> existing = index.get(key);
        if (existing != null) {
            existing.value = value;       // update, do not insert a duplicate
            moveToFront(existing);
            return;
        }
        if (index.size() == capacity) {
            Node<K, V> leastRecent = tail.prev;
            unlink(leastRecent);
            index.remove(leastRecent.key);   // needs the key, which is why nodes store it
        }
        Node<K, V> fresh = new Node<>(key, value);
        index.put(key, fresh);
        linkAfterHead(fresh);
    }

    private void moveToFront(Node<K, V> node) {
        unlink(node);
        linkAfterHead(node);
    }

    private void unlink(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void linkAfterHead(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }
}
```

Three details are where interviews are won or lost:

**Nodes store their key.** Eviction starts from the list and must then remove the entry from the map, which requires the key. A node holding only a value makes eviction impossible without an O(n) reverse lookup. This is the single most common omission.

**`put` on an existing key updates rather than inserts.** Inserting a second node for the same key desynchronizes the map and the list, and the duplicate becomes unreachable garbage that still occupies capacity.

**Eviction happens before insertion**, and only when the key is genuinely new. Evicting on an update would drop an entry while capacity was never exceeded.

`LinkedHashMap` with `accessOrder = true` and an overridden `removeEldestEntry` gives the same behaviour in about five lines, and mentioning it shows library knowledge - but interviewers almost always want the hand-rolled version, because the point is the composition.

### Thread safety

The structure above is not thread-safe, and saying so unprompted is a strong signal. Even `get` mutates the list, so concurrent reads corrupt it - which is unusual and worth flagging, since callers reasonably assume reads are safe.

Wrapping every method in a single lock works and serializes all access. Real caches - Caffeine, Guava - instead buffer access records and replay them under a lock periodically, trading exact LRU ordering for concurrency. That trade is a good answer to "how would you make this concurrent".

## Part 2: LFU, and why it is harder

*Evict the least **frequently** used entry; break ties by least recently used.*

The naive design stores a frequency count per entry and scans for the minimum on eviction - O(n), which fails the O(1) requirement.

The structure that achieves O(1) is **a map from frequency to its own LRU list**, plus a running minimum frequency:

```text
byKey:   key -> node (value, frequency)
byFreq:  frequency -> doubly-linked list of nodes at that frequency, MRU first
minFreq: smallest frequency currently present
```

- **Access:** remove the node from its `byFreq[f]` list, insert at the front of `byFreq[f+1]`. If `byFreq[f]` is now empty and `f == minFreq`, increment `minFreq`.
- **Evict:** remove the tail of `byFreq[minFreq]`, which is the least recently used among the least frequently used.
- **Insert:** frequency 1, and set `minFreq = 1`.

The reason `minFreq` can be maintained in O(1) is the load-bearing insight: a frequency only ever increases by exactly one, and only for the node just accessed. So `minFreq` either stays put or increases by one when its list empties. It never needs to be searched for.

LFU is worth understanding as the natural escalation from LRU. It is rarely asked cold, but "now evict by frequency instead" is a standard follow-up, and the answer is this two-level structure rather than a heap - a heap would make eviction O(log n) and, worse, updating a node's frequency requires decrease-key, which a plain binary heap does not support.

## Part 3: Open addressing

### Chaining versus probing

Chaining stores collisions in a structure hanging off the bucket. **Open addressing stores everything in the array itself**: on collision, probe for another slot by a deterministic rule.

```text
chaining:        bucket[3] -> [k1|v1] -> [k2|v2]
open addressing: bucket[3] = k1, bucket[4] = k2   (k2 probed forward)
```

| | Chaining | Open addressing |
|---|---|---|
| Memory | Per-entry node overhead | Contiguous array, no pointers |
| Cache behaviour | Pointer chasing | Excellent locality |
| Load factor | Works above 1.0 | Degrades badly above ~0.7 |
| Deletion | Trivial unlink | **Requires tombstones** |
| Worst case | Long chain | Long probe run, clustering |

Open addressing wins on memory and cache locality, which is why performance-focused libraries and most modern language runtimes use it. It loses on deletion, and that is the part interviews probe.

### Probing strategies

```text
linear:     index = (hash + i)         mod capacity
quadratic:  index = (hash + i*i)       mod capacity
double:     index = (hash + i*hash2)   mod capacity
```

Linear probing has the best cache behaviour - the next slot is usually the same cache line - but suffers **primary clustering**: occupied runs merge and grow, and long runs make every probe through them slower. Quadratic probing spreads the sequence and avoids primary clustering, at the cost of locality. Double hashing gives each key its own probe sequence and clusters least, but needs a second hash that is never zero and is coprime with the capacity.

### Deletion and the tombstone

This is the subtle part, and it is the standard follow-up question.

You cannot simply clear a deleted slot. Doing so breaks the probe chain of every key that probed *past* that slot during insertion:

```text
insert A -> slot 3
insert B -> slot 3 taken, probes to slot 4
delete A -> slot 3 cleared

lookup B: hashes to 3, finds slot 3 empty, concludes B is absent.
          B is still in slot 4, and is now unreachable.
```

The fix is a **tombstone**: a marker meaning "empty, but keep probing". Lookups treat it as occupied and continue; insertions may reuse it.

Tombstones accumulate. A table with many deletions fills with markers that slow every probe while holding no data, so implementations must periodically rehash to clear them. "How do you delete from an open-addressed table?" is answered by naming the tombstone *and* the rehash that eventually removes them - the second half is what distinguishes a memorized answer.

Chaining has none of this, which is a genuine argument in its favour for delete-heavy workloads and is the honest counterweight to open addressing's memory and locality advantages.

## Edge cases and common mistakes

- Using a singly-linked list for LRU, making unlink O(n) via predecessor search.
- Omitting the key from the node, so eviction cannot remove the map entry.
- Inserting a duplicate node on `put` of an existing key, desynchronizing map and list.
- Evicting on an update rather than only on a genuinely new key.
- Omitting sentinel head and tail, then mishandling null in the link paths.
- Assuming `get` is read-only and therefore concurrency-safe; it mutates the list.
- Implementing LFU with a heap, which makes eviction O(log n) and needs decrease-key.
- Scanning for the minimum frequency in LFU rather than maintaining `minFreq` incrementally.
- Clearing a deleted slot in an open-addressed table, orphaning every key that probed past it.
- Never rehashing to clear tombstones, so probe runs grow without bound.
- Running an open-addressed table above about 0.7 load factor.
- Using double hashing with a secondary hash that can return zero, producing an infinite probe loop.

## Interview questions and model answers

**Design an LRU cache with O(1) get and put.**

A hash map from key to node, plus a doubly-linked list ordered by recency with sentinel head and tail. `get` looks up the node and moves it to the front; `put` updates in place if present, otherwise evicts the tail's predecessor when at capacity and inserts at the front. The list must be doubly linked because unlinking a node found through the map needs its predecessor, and nodes must store their key because eviction starts from the list and has to remove the map entry.

**Why does the node store the key when the map already has it?**

Eviction is driven from the list end, so you have a node and need its key to remove the corresponding map entry. Without it you would scan the map to find which key maps to that node, which is O(n) and defeats the design.

**Is your LRU cache thread-safe?**

No, and unusually, not even for reads - `get` reorders the list, so concurrent gets corrupt it. A single lock around every method works but serializes access. Production caches buffer access records and replay them under a lock periodically, giving up exact LRU ordering for concurrency.

**Now evict by frequency instead.**

A map from frequency to its own LRU list, plus a `minFreq` counter. Access moves a node from `byFreq[f]` to the front of `byFreq[f+1]`; eviction removes the tail of `byFreq[minFreq]`. `minFreq` stays O(1) because frequencies only increase by one at a time, so it either holds or increments when its list empties. A heap would make eviction O(log n) and would need decrease-key for the frequency updates.

**How does open addressing differ from chaining, and when would you pick it?**

Open addressing stores every entry in the array and probes on collision, so there are no per-node pointers and locality is much better - which is why performance-oriented implementations prefer it. It degrades sharply above about 0.7 load factor and makes deletion hard. Chaining tolerates higher load and deletes trivially, so it is the better choice for delete-heavy workloads.

**How do you delete from an open-addressed table?**

With a tombstone, because clearing the slot would break the probe chain of any key that probed past it during insertion - those keys become unreachable. Lookups treat a tombstone as occupied and keep probing; insertions may reuse it. Tombstones accumulate and slow probes while holding no data, so the table must periodically rehash to clear them.

## Exercises

1. **Foundation:** Draw the map and list after `put(a)`, `put(b)`, `get(a)`, `put(c)` on a capacity-2 LRU cache.
2. **Foundation:** Implement LRU with a singly-linked list and measure how `get` degrades as capacity grows.
3. **Interview Core:** Implement the LRU cache above. Then remove the key from the node and describe exactly where eviction breaks.
4. **Interview Core:** Make `put` insert a new node for an existing key, then find the input where capacity is exceeded.
5. **Interview Core:** Reimplement the cache with `LinkedHashMap` and `removeEldestEntry`, and compare line counts.
6. **Interview Core:** Implement LFU with the frequency-list structure. Assert `minFreq` is correct after every operation.
7. **Interview Core:** Build an open-addressed table with linear probing, delete a key by clearing its slot, and construct the lookup that now fails.
8. **SDE-2 Follow-up:** Add tombstones, then measure average probe length as the tombstone ratio rises. Decide the rehash threshold from the data.
9. **SDE-2 Follow-up:** Compare linear, quadratic, and double hashing on the same key set at load factors 0.5, 0.7, and 0.9.
10. **Challenge:** Make the LRU cache safe for concurrent access, first with one lock, then with per-entry access buffering. Measure throughput under 8 threads.

## Chapter summary

These are the two things a hashing volume needs beyond chaining and the key contract. LRU is a composition question rather than a hashing question: a hash map supplies O(1) lookup, a doubly-linked list supplies O(1) reordering, and neither alone is sufficient - the `prev` pointer and the key stored inside the node are the two details that make eviction O(1), and both are what candidates omit. LFU escalates naturally to a map of frequency to LRU list with an incrementally maintained `minFreq`, which works because a frequency only ever rises by one. Open addressing is the collision strategy that stores everything in the array: better memory and locality, worse behaviour above roughly 0.7 load, and a deletion problem that requires tombstones because clearing a slot orphans every key that probed past it - with a periodic rehash to clear the tombstones that otherwise accumulate.

## Revision checklist

- [ ] I can derive the LRU structure from the O(1) requirement rather than recalling it.
- [ ] I know why the list must be doubly linked.
- [ ] I know why the node stores its key.
- [ ] I handle `put` on an existing key as an update, and evict only on genuinely new keys.
- [ ] I can state that `get` mutates the structure and is therefore not read-safe.
- [ ] I can describe the LFU frequency-list design and why `minFreq` stays O(1).
- [ ] I can say why a heap is the wrong structure for LFU.
- [ ] I can compare chaining and open addressing on memory, locality, load, and deletion.
- [ ] I can explain the tombstone and the orphaning it prevents.
- [ ] I know tombstones require a periodic rehash.
