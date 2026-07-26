# 29. Queues, Deques, PriorityQueue, and Heaps

## Learning objectives

By the end of this chapter, you should be able to:

- select FIFO queues, double-ended queues, and priority queues by removal policy;
- use exception-throwing and special-value queue methods correctly;
- derive circular-buffer and binary-heap invariants;
- analyze enqueue, dequeue, heap maintenance, and iteration costs;
- avoid assumptions about priority-queue traversal, stability, and thread safety; and
- apply queues safely to scheduling, buffering, breadth-first search, and top-k problems.

## Why this matters at SDE-2

Queues define work order. They appear in request buffering, retries, graph traversal, schedulers, rate-limited pipelines, and event loops. Choosing the wrong removal policy can violate fairness or priority rules even when all operations compile.

Interviewers expect more than "a heap gives O(log n)." You should know which operation is logarithmic, why peeking is constant, why heap iteration is not sorted, and why finding an arbitrary element is linear. In production, you must also address capacity, overload, thread ownership, and tie-breaking. An unbounded in-memory queue can convert downstream slowness into an out-of-memory incident.

## First-principles model

A queue separates insertion policy from removal policy:

- A FIFO queue removes the oldest eligible element.
- A deque permits insertion and removal at both ends.
- A priority queue removes the least or greatest element under an ordering, not necessarily the oldest.

An array deque can use a circular logical sequence over a physical array:

```text
physical slots: [D, E, empty, empty, A, B, C]
logical order:   A, B, C, D, E
head index:      4
tail index:      2   (next insertion position)
```

Indices wrap around. The invariant is that logical elements occupy the circular range from head to tail according to the representation's empty/full convention.

A binary min-heap uses a complete binary tree whose parent is no greater than either child:

```text
array: [2, 5, 4, 9, 7, 8]

          2
        /   \
       5     4
      / \   /
     9   7 8
```

For zero-based index `i`, children are commonly `2i + 1` and `2i + 2`; parent is `(i - 1) / 2`. Completeness enables array storage without node links.

> **Specification boundary:** `Queue`, `Deque`, and `PriorityQueue` define behavior. `PriorityQueue` documents heap-like complexity but does not promise a particular arity, array layout, growth policy, or stable ordering among equal-priority elements.

## Core terminology

- **Head:** Element selected for the next removal or inspection.
- **Tail:** End normally used for FIFO insertion.
- **FIFO:** First in, first out.
- **LIFO:** Last in, first out, or stack order.
- **Bounded queue:** Queue with a maximum capacity.
- **Backpressure:** Mechanism that slows, rejects, or redirects producers when consumers cannot keep up.
- **Heap:** Complete tree satisfying a local parent-child ordering invariant.
- **Sift up:** Move an inserted heap element toward the root until the invariant holds.
- **Sift down:** Move a displaced root toward leaves until the invariant holds.
- **Stable ordering:** Equal-priority elements retain their original relative order.
- **Tie-breaker:** Secondary comparison field that makes priority deterministic.

## Detailed mechanics

### Queue method pairs

The queue API offers two styles:

| Intent | Throws on failure | Returns special value |
|---|---|---|
| insert | `add(e)` | `offer(e)` returns `false` |
| remove head | `remove()` | `poll()` returns `null` |
| inspect head | `element()` | `peek()` returns `null` |

For capacity-restricted queues, `offer` is usually the clearer insertion method because full capacity is an expected condition. `poll` and `peek` use null to mean empty, which is one reason queue implementations commonly reject null elements.

### Deque method families

`Deque` generalizes both ends. It provides `addFirst/offerFirst`, `addLast/offerLast`, `removeFirst/pollFirst`, and corresponding last methods. The queue aliases generally target the last for insertion and first for removal. Stack usage should prefer `push`, `pop`, and `peek` on a deque rather than the legacy `Stack` class.

`ArrayDeque` is usually the default general-purpose deque. It avoids a node allocation per element and offers good locality. It rejects null. `LinkedList` also implements `Deque`, but its per-node overhead and locality are usually worse.

> **HotSpot note:** Current OpenJDK `ArrayDeque` uses a resizable circular array and head/tail indexes. Exact array-length conventions, growth increments, and wrap logic have changed across releases and are not public contracts.

### PriorityQueue heap mechanics

In a min-priority queue, the root is the minimum under natural ordering or a supplied comparator. Insertion appends at the next array position, then sifts upward while smaller than its parent. Removal saves the root, moves the last element to the root, decreases size, and sifts down by exchanging with the smaller child.

The heap invariant is local. It guarantees every parent is no greater than its children, which implies the root is globally minimal. It does not imply siblings or array positions are globally sorted. Thus iteration order is unspecified rather than priority order.

Ordinary deque and priority-queue iterators commonly fail fast after detected structural modification. This is diagnostic, best-effort behavior, not thread coordination. Iterator traversal is not a snapshot and still does not expose priority order.

For max-priority behavior, reverse the comparator. Be careful with arithmetic comparators; use `comparingInt`, `comparingLong`, or safe comparison methods instead of subtraction.

### Equal priorities and mutation

`PriorityQueue` does not guarantee FIFO order for equal elements. Add a monotonically increasing sequence as a final comparator field when stable tie-breaking is required. Consider sequence overflow and lifecycle; a `long` is ample for many process lifetimes but still deserves an explicit policy.

Never mutate an enqueued element's priority fields. The queue does not automatically locate and reposition it, so the heap can become logically invalid. Remove and reinsert the element, or enqueue immutable task descriptors and keep changing state elsewhere.

### Arbitrary operations and bulk heap construction

`contains` and `remove(Object)` generally scan the backing representation, costing `O(n)`, because the heap orders only by priority and has no key index. After an arbitrary removal is found, restoring the heap costs `O(log n)`.

Building a heap by repeated insertion costs `O(n log n)`. Bottom-up heap construction can heapify an array in `O(n)` because most nodes are near leaves and move only a short distance. A `PriorityQueue` constructor from a suitable collection may take advantage of bulk heapification; exact constructor paths remain implementation-specific.

### Bounded and concurrent queues

General-purpose `ArrayDeque` and `PriorityQueue` are unbounded in the API sense and not thread-safe. Capacity-aware and blocking behavior lives in concurrent queue types. A bounded blocking queue can wait, time out, or reject when full. A concurrent priority queue provides thread-safe priority access but still does not make a multi-step workflow atomic.

Queue selection must include who produces, who consumes, whether operations can block, and what happens under overload. Those are system semantics, not merely collection details.

## Worked Java example

This retry scheduler orders tasks by due time and preserves insertion order among equal due times:

```java
import java.time.Instant;
import java.util.Comparator;
import java.util.PriorityQueue;

record Retry(long sequence, Instant dueAt, String jobId, int attempt) {}

final class RetrySchedule {
    private static final Comparator<Retry> ORDER = Comparator
            .comparing(Retry::dueAt)
            .thenComparingLong(Retry::sequence);

    private final PriorityQueue<Retry> queue = new PriorityQueue<>(ORDER);
    private long nextSequence;

    void schedule(Instant dueAt, String jobId, int attempt) {
        if (dueAt == null || jobId == null || attempt < 1) {
            throw new IllegalArgumentException("invalid retry");
        }
        queue.offer(new Retry(nextSequence++, dueAt, jobId, attempt));
    }

    Retry pollReady(Instant now) {
        Retry next = queue.peek();
        if (next == null || next.dueAt().isAfter(now)) {
            return null;
        }
        return queue.poll();
    }

    int pending() {
        return queue.size();
    }
}
```

This class assumes single-threaded ownership. A lock would be needed if multiple threads share it; using a thread-safe queue alone would not make the `peek` followed by conditional `poll` atomic. A production retry service also persists tasks, caps capacity, supports cancellation through an index, and handles sequence lifecycle.

## Execution or memory walkthrough

Suppose the comparator sees tasks `(12:00, seq 0)`, `(12:10, seq 1)`, and `(12:05, seq 2)`.

```text
insert 12:00/0: [12:00/0]
insert 12:10/1: [12:00/0, 12:10/1]
insert 12:05/2: append then compare with root
                 [12:00/0, 12:10/1, 12:05/2]
```

The array is a valid heap even though positions 1 and 2 are not sorted with each other. Polling removes `12:00/0`, moves `12:05/2` to the root, and sifts down:

```text
before removal: [12:00/0, 12:10/1, 12:05/2]
move last:      [12:05/2, 12:10/1]
result:         12:00/0
```

If another task due at 12:05 has sequence 3, sequence 2 sorts first. The tie-breaker turns an unspecified equal-priority outcome into a domain guarantee.

The priority queue stores references in an array and `Retry` record objects separately. Removed slots are cleared by normal implementations so they do not retain tasks. Exact capacity may exceed size. A deque similarly keeps spare slots to make end operations cheap.

## Complexity and performance

| Operation | `ArrayDeque` | `PriorityQueue` |
|---|---:|---:|
| add/remove at supported end | amortized `O(1)` | add `O(log n)`, poll `O(log n)` |
| peek head | `O(1)` | `O(1)` |
| remove arbitrary object | `O(n)` | `O(n)` search plus repair |
| contains | `O(n)` | `O(n)` |
| iterate all | `O(n)` | `O(n)`, not sorted |
| copy then ordered drain | `O(n)` copy | `O(n log n)` drain |
| bottom-up heap construction | not applicable | `O(n)` algorithmically |

Deque resize is occasionally `O(n)`, giving amortized constant end operations under geometric growth. Heap operations follow height `O(log n)` because a complete binary tree with `n` nodes has logarithmic height.

For top-k selection from `n` values, maintain a heap of size `k`: `O(n log k)` time and `O(k)` space. Sorting everything costs `O(n log n)` and `O(n)` input storage or sorting space depending on representation. If `k` is close to `n`, constants and required output order can make sorting preferable.

## Edge cases and common mistakes

- Assuming iteration over `PriorityQueue` returns sorted order.
- Assuming equal-priority tasks are stable without a tie-breaker.
- Mutating an element's comparator fields while it is enqueued.
- Calling `remove(Object)` or `contains` frequently and expecting logarithmic behavior.
- Using null as a legitimate queue element when `poll` uses null for emptiness.
- Choosing exception-throwing `add` when full capacity is expected control flow.
- Using a queue that grows without bound under slower consumers.
- Sharing `ArrayDeque` or `PriorityQueue` across threads without protection.
- Separating `peek` and `poll` in concurrent code without one atomic policy.
- Using `PriorityQueue` for scheduling but ignoring wall-clock changes, durability, or wakeup coordination.
- Implementing a max heap by negating an integer and overflowing at `Integer.MIN_VALUE`.
- Assuming a heap is a binary search tree or that arbitrary search follows one branch.
- Forgetting stale duplicate entries in algorithms that reinsert improved priorities.

## Production engineering notes

Capacity is a reliability contract. Establish maximum depth, enqueue timeout or rejection behavior, retry/drop policy, and metrics for depth, age, throughput, and rejection. FIFO does not guarantee fairness if tasks have dramatically different service times. Priority scheduling can starve low-priority work; aging or quotas may be necessary.

Use immutable queue elements. When cancellation or priority updates are frequent, pair a heap with a map from ID to state, accept lazy deletion, or use a specialized indexed heap. Lazy deletion leaves stale entries until they reach the head, so bound and observe the overhead.

For breadth-first search, mark a node visited when enqueuing, not when dequeuing, to prevent duplicate queue growth. For stacks, prefer `ArrayDeque.push/pop` and never use null sentinels. For thread handoff, choose a concurrent or blocking queue whose ordering, capacity, and memory-consistency contract match the workflow.

Do not persist only an in-memory retry heap if process restart must not lose jobs. Keep durable source-of-truth state and rebuild the heap, or use an external scheduler. Collection choice solves in-process ordering, not distributed delivery guarantees.

## Interview questions and model answers

**Why is priority-queue iteration not sorted?**

The heap invariant only orders each parent relative to its children. The backing array is a level-order heap representation, not a sorted sequence. Repeated polling, preferably from a copy, produces priority order.

**What are offer/poll/peek for?**

They use special return values for expected failure: `offer` can report full capacity, while `poll` and `peek` return null for an empty queue. The paired methods `add/remove/element` throw exceptions.

**How does heap insertion work?**

Append at the next complete-tree position, then sift upward while the new element precedes its parent. At most one root-to-leaf height is traversed, so cost is `O(log n)`.

**Why is removing an arbitrary heap element O(n)?**

The heap has no global search ordering for arbitrary values. It may scan all entries to locate the value, then needs only logarithmic repair.

**How do you make equal-priority scheduling deterministic?**

Add a stable secondary comparator field, typically a monotonic sequence number or immutable ID, matching the desired business rule.

**Why prefer `ArrayDeque` over `Stack` or often `LinkedList`?**

It directly implements deque and stack operations, avoids legacy synchronized `Vector` behavior, uses fewer objects than a linked list, and usually has better locality.

## Exercises

1. Dry-run heap insertion of `7, 3, 9, 1, 4` and then two polls. Draw the array after each step.
2. Use an `ArrayDeque` to implement breadth-first traversal and explain when each node is marked visited.
3. Find the largest five values in a stream using a min-heap of size five. State time and space bounds.
4. Design overload behavior for a bounded email-work queue. Include observability and retry policy.
5. Modify the retry scheduler to support lazy cancellation through a set of canceled IDs. Analyze stale-entry memory.
6. Demonstrate that printing or iterating a priority queue is not a sorted-output algorithm.

## Chapter summary

Queues are defined by removal policy. Deques efficiently support both ends, while priority queues expose the minimum or maximum under an ordering. Circular arrays provide amortized constant deque operations; complete binary heaps provide constant-time peek and logarithmic insertion and head removal. Their local invariant does not sort iteration or accelerate arbitrary search. Production use adds bounded capacity, overload behavior, immutability, concurrency ownership, fairness, and durability.

## Revision checklist

- [ ] I know the exception and special-value queue method pairs.
- [ ] I can map every `Deque` operation to an end and use it as a stack.
- [ ] I can state circular-buffer and min-heap invariants.
- [ ] I can dry-run sift-up and sift-down.
- [ ] I do not assume priority-queue iteration or equal-priority stability.
- [ ] I know why arbitrary heap search and removal are linear.
- [ ] I can derive `O(n log k)` top-k selection.
- [ ] I include capacity, backpressure, concurrency, fairness, and durability in production designs.
