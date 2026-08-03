# 29. Queues, Deques, PriorityQueue, and Heaps

## Print the queue and see what happens

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();
for (int v : new int[]{5, 1, 8, 3, 9, 2, 7}) {
    pq.offer(v);
}
System.out.println(pq);          // [1, 3, 2, 5, 9, 8, 7]
```

Not `[1, 2, 3, 5, 7, 8, 9]`. Not the insertion order either. And `forEach`, the enhanced `for` loop, and `stream()` all give you the same thing, because they all use the same iterator.

Only `poll()` returns priority order - one element at a time, `O(log n)` each. This is the most common `PriorityQueue` bug in production code, and it survives testing easily because with two or three elements the array *is* usually sorted.

Everything else in this chapter follows from understanding why.

> **Specification boundary:** `PriorityQueue` documents that its iterator makes no ordering guarantee,
> and that it is unbounded and not thread-safe. Those are contracts you may rely on. The binary heap,
> the array layout, and the sift procedures are the implementation that explains them - a conforming
> implementation could use a different structure and still satisfy every documented promise.

## A heap is an array pretending to be a tree

![Figure 29.1 - A heap is an array pretending to be a tree](assets/diagrams/24-heap-array-layout.png)

There are no node objects. There is one array, and the tree structure is arithmetic:

```text
parent(i) = (i - 1) / 2      left(i) = 2i + 1      right(i) = 2i + 2
```

The invariant is deliberately weak: **a parent is no greater than its children.** That is enough to guarantee the minimum is at index 0, and it says nothing whatsoever about siblings. `3` and `2` are siblings in the array above, in that order, and the heap is perfectly valid.

That single sentence answers a whole family of questions:

- `peek` is `O(1)` - read index 0.
- `poll` and `offer` are `O(log n)` - one root-to-leaf path.
- `contains` and `remove(Object)` are `O(n)` - there is no index by value, only by priority.
- Iteration is not sorted, and `toString` shows array order.
- There is **no decrease-key**. Nothing maps an element to its array position.

## Sift up, sift down

![Figure 29.2 - Sift up on offer, sift down on poll](assets/diagrams/25-heap-sift.png)

**`offer`** appends at the end, then swaps upward while the new element is smaller than its parent. It stops as soon as the parent is smaller, because everything above the parent is already smaller than the parent.

**`poll`** takes index 0, moves the *last* element to the root, then swaps downward - always with the **smaller** of the two children. Swapping with the smaller child is what restores the invariant; swapping with either child does not, and that is the detail interviewers ask you to justify.

Both walk one root-to-leaf path, so both are `O(log n)`.

One consequence worth knowing: building a heap from `n` elements by calling `offer` `n` times costs `O(n log n)`, but heapifying an existing array bottom-up costs `O(n)` - most nodes are near the leaves and barely move. `new PriorityQueue<>(collection)` can take the linear path; a loop of `offer` cannot.

## The other queue: a ring, not a chain

`ArrayDeque` is the default for FIFO and LIFO work, and it is also an array.

![Figure 29.3 - ArrayDeque is a ring, which is why both ends are cheap](assets/diagrams/23-arraydeque-ring.png)

Elements do not move; `head` and `tail` do. Adding at the front decrements `head`, wrapping around the end of the array. Because capacity is always a power of two, wrapping is a mask rather than a modulo:

```text
(head - 1) & (capacity - 1)
```

That is why both ends are constant time on an array, which surprises people who assume you need links for that.

Use `ArrayDeque` in preference to both alternatives. `Stack` is legacy, synchronised for no benefit, and - genuinely confusingly - iterates from the *bottom*. `LinkedList` allocates a node per element. `ArrayDeque` rejects null, which is a feature rather than a limitation: null is the "empty" signal for `poll` and `peek`, so permitting null elements would make the API ambiguous.

> **HotSpot note:** current OpenJDK `ArrayDeque` uses a resizable circular array with head and tail indexes. Array-length conventions, growth, and wrap logic have changed across releases and are not public contracts.

## Two method families, and when to use which

Every queue operation comes in a throwing form and a returning form:

| Intent | Throws on failure | Returns a special value |
|---|---|---|
| insert | `add(e)` | `offer(e)`->`false` |
| remove head | `remove()`->`NoSuchElementException` | `poll()`->`null` |
| inspect head | `element()`->`NoSuchElementException` | `peek()`->`null` |

Use the returning form when the failure is an expected condition - a bounded queue being full, or a queue being empty in a polling loop. Use the throwing form when it would be a bug. Choosing `add` on a bounded queue and then not handling the exception is a common way to turn back-pressure into an outage.

`Deque` doubles everything: `addFirst`/`offerFirst`, `removeLast`/`pollLast`, and so on. For stack use, prefer `push`, `pop`, and `peek` on a `Deque`.

## Equal priorities are not FIFO

```java
PriorityQueue<Task> q = new PriorityQueue<>(Comparator.comparing(Task::dueAt));
```

Two tasks with the same `dueAt` come out in an arbitrary order - and the order can differ between runs and between JDK versions, because it depends on where sift operations happened to leave them. If fairness matters, make it explicit with a monotonic sequence number as the final comparator field:

```java
record Retry(long sequence, Instant dueAt, String jobId, int attempt) { }

static final Comparator<Retry> ORDER =
        Comparator.comparing(Retry::dueAt).thenComparingLong(Retry::sequence);
```

This is the same "end the chain on something unique" rule as Chapter 28, applied for a different reason: there, an ambiguous comparator *loses* elements; here, it merely reorders them unpredictably. A `long` sequence is ample for realistic process lifetimes, but the overflow policy still deserves one sentence somewhere.

And never mutate the priority field of an enqueued element. The queue will not notice and will not reposition it, so the heap silently becomes invalid - `poll` starts returning the wrong element. Remove and reinsert, or enqueue immutable descriptors and keep mutable state elsewhere.

## Worked example: a retry scheduler

```java
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

record Retry(long sequence, Instant dueAt, String jobId, int attempt) { }

final class RetryScheduler {
    private static final Comparator<Retry> ORDER =
            Comparator.comparing(Retry::dueAt).thenComparingLong(Retry::sequence);

    private final PriorityQueue<Retry> pending = new PriorityQueue<>(ORDER);
    private final AtomicLong sequence = new AtomicLong();
    private final int maxPending;

    RetryScheduler(int maxPending) {
        this.maxPending = maxPending;
    }

    boolean schedule(String jobId, Instant dueAt, int attempt) {
        if (pending.size() >= maxPending) {
            return false;                    // shed load explicitly
        }
        return pending.offer(
                new Retry(sequence.getAndIncrement(), dueAt, jobId, attempt));
    }

    List<Retry> dueBy(Instant now) {
        List<Retry> due = new ArrayList<>();
        while (!pending.isEmpty() && !pending.peek().dueAt().isAfter(now)) {
            due.add(pending.poll());         // peek is O(1), so the guard is cheap
        }
        return due;
    }
}
```

Three decisions:

1. **`peek` before `poll`.** Checking the head is `O(1)`, so the loop stops without removing anything it should not.
2. **An explicit `maxPending`.** `PriorityQueue` is unbounded. An unbounded in-memory queue turns a slow downstream dependency into an out-of-memory incident - the queue absorbs the backlog until the heap does not.
3. **`Retry` is a record.** Nothing can mutate `dueAt` while the element is in the heap.

Trace `offer` of a task due at 10:02 into a heap holding 10:00, 10:05, 10:10, 10:07:

```text
append at index 4:   [10:00, 10:05, 10:10, 10:07, 10:02]
parent of 4 is 1:    10:02 < 10:05  -> swap
                     [10:00, 10:02, 10:10, 10:07, 10:05]
parent of 1 is 0:    10:02 > 10:00  -> stop
```

Two comparisons, one for each level, for a five-element heap.

## Complexity

| Operation | `ArrayDeque` | `PriorityQueue` |
|---|---|---|
| `offer` / `add` | amortized `O(1)` | `O(log n)` |
| `poll` / `remove()` | `O(1)` | `O(log n)` |
| `peek` | `O(1)` | `O(1)` |
| `contains` | `O(n)` | `O(n)` |
| `remove(Object)` | `O(n)` | `O(n)` find + `O(log n)` repair |
| build from `n` elements | `O(n)` | `O(n)` bulk, `O(n log n)` by repeated `offer` |
| iteration order | front to back | **array order, not priority order** |

## Edge cases and common mistakes

- Reading iteration, `toString`, `forEach`, or `stream()` output as priority order.
- Expecting FIFO among equal priorities without a sequence tie-break.
- Mutating a priority field while the element is enqueued.
- Using `contains` or `remove(Object)` on a hot path - both are `O(n)`.
- Expecting a decrease-key operation to exist.
- Using an unbounded queue as a buffer in front of a slower consumer.
- Using `add` where `offer` was the right choice on a bounded queue, then not handling the exception.
- Inserting null into `ArrayDeque` or `PriorityQueue`.
- Using `Stack` - legacy, synchronised, and iterates from the bottom.
- Building a heap with `n` calls to `offer` when the collection constructor would heapify in `O(n)`.
- Assuming `PriorityQueue` or `ArrayDeque` is thread-safe.
- Writing a subtraction comparator for priorities.

## Production engineering notes

Queue choice is a systems decision, not a collection decision. Name the producer, the consumer, whether either may block, and what happens under overload *before* choosing a type.

**Always bound an in-memory queue.** Then decide what a full queue means: reject and signal back-pressure, drop the oldest, drop the lowest priority, or block the producer. Each is defensible; silently growing is not.

For cross-thread hand-off, use `java.util.concurrent`: `ArrayBlockingQueue` for a fixed bound, `LinkedBlockingQueue` for an optional one, `PriorityBlockingQueue` for priority with blocking, `DelayQueue` when elements become available at a time. Note that even a concurrent queue makes individual operations atomic, not your multi-step workflow.

Instrument queue depth and the age of the head element. Depth alone tells you the backlog; head age tells you whether you are draining it.

## Interview questions and model answers

**What does iterating a `PriorityQueue` give you?**

Array order - the internal heap layout - not priority order. The heap invariant only relates parents to children, so siblings are unordered. Only `poll` yields priority order, one element per `O(log n)` call.

**Why is `poll` `O(log n)` but `peek` `O(1)`?**

`peek` reads index 0. `poll` must remove that element and restore the invariant, which means moving the last element to the root and sifting it down one root-to-leaf path.

**Why swap with the smaller child when sifting down?**

Because the new parent must be no greater than *both* children. Promoting the larger child would leave it above the smaller one and violate the invariant immediately.

**How do you get FIFO among equal priorities?**

Add a monotonically increasing sequence number as the final field in the comparator. `PriorityQueue` gives no ordering guarantee among elements that compare equal.

**How would you find the 100 largest of ten million elements?**

A bounded min-heap of size 100: push each element, and poll whenever the size exceeds 100. `O(n log k)` time and `O(k)` space, against `O(n log n)` and `O(n)` for sorting everything. Note the inversion - you keep a *min*-heap to find the largest, because you evict the smallest of the current best.

**Why does `ArrayDeque` reject null?**

`poll` and `peek` return null to mean "empty". Permitting null elements would make that return value ambiguous.

**`ArrayDeque` is an array - how is `addFirst` constant time?**

It is circular. `head` moves backwards with a mask, `(head - 1) & (capacity - 1)`, rather than shifting elements. Capacity is a power of two so the mask works.

## Exercises

1. Offer `5, 1, 8, 3, 9, 2, 7` to a `PriorityQueue` and print it. Confirm you get `[1, 3, 2, 5, 9, 8, 7]`, then draw the tree and check every parent-child pair.
2. Drain that queue with `poll` and confirm the output is sorted. Explain in one sentence why iteration and draining disagree.
3. Implement sift-up and sift-down over a raw array and test them against `PriorityQueue` on a few thousand random sequences.
4. Enqueue three tasks with identical priorities, drain, and record the order. Then add a sequence tie-break and repeat.
5. Mutate the priority field of an enqueued element, then poll the whole queue. Show that the output is not in priority order.
6. Implement top-k with a bounded heap. Count comparisons against a full sort at n = 1,000,000 and k = 100.
7. Take an unbounded producer-consumer queue and add a bound. Implement two different full-queue policies and say which you would choose for a payment retry pipeline, and why.
8. Draw an `ArrayDeque` of capacity 8 after `addLast` x 6, `pollFirst` x 3, `addLast` x 4. Mark head, tail, and where the wrap occurs.

## Chapter summary

A queue is defined by its removal policy, and a `PriorityQueue` is a binary heap stored in a plain array with the tree structure supplied by index arithmetic. Its invariant - a parent is no greater than its children - is deliberately weak, which is why the minimum is always at index 0 and why iteration, `toString`, and `stream()` expose array order rather than priority order; only `poll` gives priority order, one `O(log n)` step at a time. The same invariant explains the rest of the API: `peek` is constant, `contains` and `remove(Object)` are linear because nothing indexes elements by value, and no decrease-key exists. Sift-down must take the *smaller* child, and bulk heapification is `O(n)` where `n` calls to `offer` are `O(n log n)`. `ArrayDeque` is the other array in disguise - a power-of-two ring where `head` and `tail` move and the elements do not, which is what makes both ends constant time and why it should displace both `Stack` and `LinkedList`. Equal priorities are unordered unless you add a sequence tie-break, an enqueued element's priority must never change, and every in-memory queue needs an explicit bound and an explicit policy for what happens when it is reached.

## Revision checklist

- [ ] I know iteration, `toString`, and `stream()` show array order, and only `poll` gives priority order.
- [ ] I can state the heap invariant and derive `peek`, `poll`, `contains`, and "no decrease-key" from it.
- [ ] I can explain why sift-down must use the smaller child.
- [ ] I know bulk heapify is `O(n)` and repeated `offer` is `O(n log n)`.
- [ ] I add a sequence tie-break whenever FIFO among equal priorities matters.
- [ ] I never mutate the priority of an enqueued element.
- [ ] I can explain the ring buffer and why `addFirst` is constant time on an array.
- [ ] I choose `ArrayDeque` over `Stack` and `LinkedList`, and know why it rejects null.
- [ ] Every queue I put in production has a bound and a documented overload policy.
