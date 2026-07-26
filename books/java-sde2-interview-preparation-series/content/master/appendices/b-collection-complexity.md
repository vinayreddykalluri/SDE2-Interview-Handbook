# Appendix B - Collection Complexity and Selection Matrix

Complexities below describe common OpenJDK implementations for ordinary workloads, not interface-level guarantees unless stated. Hash-table operations are expected-time claims under an adequate hash distribution. Adversarial keys, resizing, comparator cost, allocation, cache locality, and concurrency can dominate a real service.

## List implementations

| Operation | `ArrayList` | `LinkedList` | Engineering note |
|---|---:|---:|---|
| indexed `get` / `set` | O(1) | O(n) | Array locality usually favors `ArrayList` |
| append | amortized O(1) | O(1) | Array resize occasionally copies elements |
| prepend | O(n) | O(1) | `ArrayDeque` is usually the better queue |
| insert/remove by index | O(n) shift | O(n) traversal, O(1) relink | Traversal often dominates linked-list edits |
| search by value | O(n) | O(n) | Equality cost is part of the constant |
| iteration | O(n) | O(n) | Pointer chasing can hurt locality |
| memory per element | reference plus spare capacity | node object plus links | Measure retained size for large collections |

Default choice: `ArrayList`. Choose `LinkedList` only when its list-plus-deque contract and a measured mutation pattern justify node overhead. Even frequent insertion is not sufficient if finding the insertion point is linear.

`CopyOnWriteArrayList` makes traversal stable and unsynchronized for readers by copying the backing array on every structural mutation. It fits small, read-mostly listener/configuration sets; it is disastrous for frequent writes or large lists.

## Set and map implementations

| Type | Lookup / update | Ordering | Null policy | Representative use |
|---|---:|---|---|---|
| `HashMap` | expected O(1) | unspecified | one null key, null values | general mutable map |
| `LinkedHashMap` | expected O(1) | insertion or access order | like `HashMap` | predictable traversal, bounded LRU building block |
| `TreeMap` | O(log n) | comparator/natural sorted | null key generally rejected by natural ordering | range queries, nearest-key navigation |
| `EnumMap` | O(1) array-like | enum declaration order | null key rejected | dense enum-keyed state |
| `IdentityHashMap` | expected O(1) | unspecified | permits null | graph/topology algorithms using identity semantics |
| `ConcurrentHashMap` | expected O(1) | unspecified, weakly consistent traversal | null keys/values rejected | shared concurrent lookup/update |
| immutable `Map.of` / `Map.copyOf` | implementation-dependent efficient lookup | unspecified | null rejected | fixed snapshots and safe sharing |

Corresponding sets are commonly backed by maps or tree structures:

| Type | Membership | Ordering | Notes |
|---|---:|---|---|
| `HashSet` | expected O(1) | unspecified | mutable elements can become unfindable |
| `LinkedHashSet` | expected O(1) | insertion order | extra link metadata |
| `TreeSet` | O(log n) | sorted | comparator equality controls uniqueness |
| `EnumSet` | O(1) bit-vector-like | enum order | exceptionally compact for enum domains |
| `CopyOnWriteArraySet` | O(n) lookup, O(n) copy on write | insertion-like snapshot order | very small read-mostly sets |
| `ConcurrentHashMap.newKeySet()` | expected O(1) | weakly consistent | scalable concurrent membership set |

Important `HashMap` facts:

- Capacity and load factor affect resizing, memory, and traversal cost.
- Current OpenJDK `HashMap` implementations may transform a collision-heavy bin into a tree when thresholds and table capacity permit. That is an implementation strategy, not permission to use poor keys.
- `computeIfAbsent` is not a general exactly-once side-effect facility. Read each map's contract, keep mapping functions short, and avoid recursive structural updates.
- Mutating an equality/hash field after insertion violates the lookup assumption even though the entry still occupies a bucket.
- Iteration order can look stable in a small experiment and still have no contract.

## Queues, deques, and heaps

| Type | Add/remove | Head access | Capacity / blocking | Use |
|---|---:|---:|---|---|
| `ArrayDeque` | amortized O(1) at ends | O(1) | unbounded, nonblocking | default stack, queue, deque |
| `PriorityQueue` | O(log n) add/remove | O(1) peek | unbounded, nonblocking | next minimum/maximum by comparator |
| `ConcurrentLinkedQueue` | expected O(1) | typically O(1); may traverse removed nodes | unbounded, nonblocking | scalable multi-producer/consumer queue |
| `ArrayBlockingQueue` | O(1) | O(1) | bounded, blocking | fixed-capacity back pressure |
| `LinkedBlockingQueue` | O(1) | O(1) | optionally bounded, blocking | producer/consumer, usually explicit bound |
| `PriorityBlockingQueue` | O(log n) | O(1) | unbounded, blocking take | concurrent priority scheduling without capacity control |
| `SynchronousQueue` | handoff | none stored | zero capacity, blocking | direct producer-consumer rendezvous |
| `DelayQueue` | O(log n) | eligible head | unbounded | delayed/expiry work |

Use `offer`, `poll`, and `peek` when absence/capacity is expected and should be represented by a result. Use `add`, `remove`, and `element` when failure should be exceptional. Blocking queues add `put` and `take`; timed variants let cancellation and service-level policy participate.

`PriorityQueue` guarantees that `peek`/`remove` exposes a least element according to its ordering. Its iterator is not sorted. To emit sorted order, repeatedly remove elements from a copy, or sort a collection explicitly.

## Sorted and navigable operations

`NavigableMap` and `NavigableSet` support relative queries in O(log n) for tree implementations:

- `lower`: greatest element strictly less than the key.
- `floor`: greatest element less than or equal to the key.
- `ceiling`: least element greater than or equal to the key.
- `higher`: least element strictly greater than the key.
- `subMap`, `headMap`, and `tailMap`: usually backed range views.

Backed views enforce their range. An insertion outside the view range fails. Changes through either the view or the source are visible through the other, subject to the collection's synchronization and iterator contracts.

## Views, wrappers, and copies

These APIs have deliberately different ownership semantics:

| API | Structural mutability | Backing relationship | Null handling |
|---|---|---|---|
| `Arrays.asList(array)` | fixed size; `set` allowed | backed by array | permits null |
| `list.subList(a, b)` | generally mutable within range | backed by list | source policy |
| `Collections.unmodifiableList(list)` | wrapper rejects mutation | reflects backing-list changes | source policy |
| `List.copyOf(source)` | unmodifiable | snapshot-like shallow copy/reuse | rejects null |
| `List.of(values...)` | unmodifiable | independent fixed value | rejects null |
| `stream.toList()` | unmodifiable | result collection | may contain null elements produced by stream |
| `Collectors.toList()` | no mutability/type guarantee | new result | collector behavior |

Unmodifiable is not deeply immutable. If elements are mutable, their state can change. A defensive copy protects collection structure only; copy elements or use immutable value types when deeper isolation is required.

## Iterator consistency

Iterator labels describe detection and visibility, not thread safety of arbitrary compound actions:

- Fail-fast iterators on many ordinary collections may throw `ConcurrentModificationException` after an uncoordinated structural change. Detection is best-effort, not a synchronization mechanism.
- Snapshot iterators, such as those of copy-on-write collections, traverse a stable historical array and do not see later writes.
- Weakly consistent iterators on concurrent collections tolerate concurrent updates and may reflect some updates without throwing. They do not freeze a transactionally consistent snapshot.

Use external locking when multiple operations must preserve one invariant and the collection does not supply an atomic compound method. `if (!map.containsKey(k)) map.put(k, v)` is a race on a shared map; use `putIfAbsent`, `compute`, or explicit coordination as the required semantics dictate.

## Stream and collector cost reminders

A stream pipeline is not a collection. It describes one traversal and is normally single-use. Complexity is the sum of traversal and operations:

| Operation | Typical cost | Important qualification |
|---|---:|---|
| `filter`, `map` | O(n) | function cost and boxing may dominate |
| `distinct` | expected O(n) | retains seen values; ordered mode can cost more |
| `sorted` | O(n log n) | buffers elements; comparator cost matters |
| `limit` | passes at most k downstream | upstream filters or stateful stages can still perform O(n) work; ordered parallel pipelines may coordinate heavily |
| `findFirst` | short-circuiting | encounter order constrains parallel execution |
| grouping | expected O(n) | retains keys and downstream accumulation state |
| `toMap` | expected O(n) | merge policy required when duplicate output keys are possible |

Collector characteristics such as `CONCURRENT`, `UNORDERED`, and `IDENTITY_FINISH` are promises to the reduction framework. A mutable result container alone does not make a collector concurrent.

## Selection questions

Choose a collection by answering in order:

1. What semantics are required: sequence, uniqueness, association, priority, range navigation, or handoff?
2. Is order absent, insertion-based, access-based, sorted, or merely head-priority?
3. Which operations dominate, and what are their input sizes?
4. Is mutation local, externally synchronized, or concurrent?
5. Must a compound invariant span several operations?
6. Are nulls valid domain values, and can the chosen type represent them?
7. Who owns the collection, and should the API expose a view, wrapper, or copy?
8. What bounds memory: maximum size, admission control, expiry, or back pressure?
9. Does iteration need a snapshot, weak consistency, or strict external coordination?
10. Have representative allocation, locality, contention, and tail latency been measured?

## Rapid decision matrix

The strongest interview answer states the contract first, compares two plausible choices, names the dominant workload operation, and closes with ownership, concurrency, and measurement implications.

| Requirement | Start with | Reconsider when |
|---|---|---|
| general indexed sequence | `ArrayList` | frequent measured front edits or immutable persistent structure needed |
| stack / FIFO / double-ended work | `ArrayDeque` | cross-thread blocking or bounded capacity required |
| general key lookup | `HashMap` | sorted/range operations, stable order, enum keys, or concurrency required |
| predictable insertion traversal | `LinkedHashMap` | sorted key semantics required |
| sorted map and nearest-key query | `TreeMap` | hash lookup is sufficient and ordering cost is wasted |
| enum membership | `EnumSet` | domain is not an enum |
| minimum/maximum scheduling | `PriorityQueue` | full sorted iteration or bounded blocking is required |
| shared concurrent map | `ConcurrentHashMap` | one invariant spans external state or several keys |
| bounded producer/consumer | `ArrayBlockingQueue` | transfer semantics, priority, or different throughput characteristics matter |
| fixed public result | `List.copyOf` / `Map.copyOf` | caller intentionally needs a live view |
| small read-mostly listener set | copy-on-write collection | writes or collection size grow |
