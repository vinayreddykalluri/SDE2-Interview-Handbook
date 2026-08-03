# Binary Heap and Quickselect Internals

A priority queue answers “which item should come out next?” It does not keep every item in sorted iteration order. A binary heap is the compact structure that usually makes that contract efficient. Quickselect solves a different contract: find one rank in a fixed collection without maintaining a stream.

This chapter builds both from first principles, then reconnects them to Java's `PriorityQueue`. Complete executable code is in `HeapInterviewChecks.java`.

## Heap shape and order are separate invariants

A binary min-heap has two properties:

1. **Shape:** it is a complete binary tree—levels fill left to right.
2. **Order:** every parent is less than or equal to each child.

The shape permits dense array storage. For zero-based index `i`:

```text
parent     = (i - 1) / 2       when i > 0
left child = 2 * i + 1
right child= 2 * i + 2
```

```text
array: [1, 3, 2, 8, 5, 7]

             1 (0)
           /       \
       3 (1)       2 (2)
      /   \         /
   8 (3) 5 (4)   7 (5)
```

This is a valid heap but not a sorted array. Only the root is globally minimum. A breadth-first traversal or `PriorityQueue` iterator does not promise sorted order.

## Offer: append, then sift up

Appending preserves the complete-tree shape but may violate parent order. Compare the new child with its parent and swap upward until order is restored.

```text
offer 0 into [1, 3, 2, 8, 5, 7]

[1, 3, 2, 8, 5, 7, 0]  compare indexes 6 and 2
[1, 3, 0, 8, 5, 7, 2]  compare indexes 2 and 0
[0, 3, 1, 8, 5, 7, 2]  stop at root
```

The tree height is `O(log n)`, so offer is `O(log n)` worst case.

The companion doubles its backing array when full. Geometric growth makes append capacity work amortized `O(1)`; sifting still makes the complete offer `O(log n)`.

## Poll: move the last value, then sift down

Removing the root leaves a hole. Move the last value to index zero, shrink logical size, then repeatedly swap with the smaller child.

```text
poll from [1, 3, 2, 8, 5, 7]

remove 1; move 7 to root: [7, 3, 2, 8, 5]
smaller child is 2:       [2, 3, 7, 8, 5]
stop: 7 has no child
```

Always choose the smaller child in a min-heap. Swapping with the left child merely because it exists can leave a smaller right child under a larger parent.

The removed array slot does not belong to logical heap state after `size` decreases. For object heaps, clear that slot so the backing array does not retain an unnecessary reference.

## Bottom-up heapify is `O(n)`, not `O(n log n)`

To heapify an existing array, every leaf is already a valid one-node heap. Start at the last parent `n / 2 - 1` and sift each parent downward.

Although one sift can be `O(log n)`, most nodes are near the leaves and move very little. Roughly half the nodes have height zero, one quarter height one, one eighth height two, and so on:

```text
n/4 * 1 + n/8 * 2 + n/16 * 3 + ... = O(n)
```

Building by calling `offer` repeatedly is `O(n log n)` as a simple upper bound. Bottom-up heapify uses the structure more efficiently.

## Java `PriorityQueue` contracts that matter

- It is a min-heap under natural order unless a comparator changes priority.
- `peek` reads the head; `poll` removes it; both return `null` on an empty queue.
- Iteration order is unspecified and is not sorted order.
- It does not efficiently remove an arbitrary known item; that search can be linear.
- Mutating fields used by an enqueued object's comparator does not automatically reposition it.
- It is not thread-safe.

For a max-heap of integers, use `Comparator.reverseOrder()`. For custom records, compare with `Comparator.comparingInt` or `Integer.compare`, never subtraction:

```java
Comparator<Job> byPriority = Comparator.comparingInt(Job::priority);
```

`first.priority() - second.priority()` can overflow and reverse ordering.

## Heap or quickselect for kth largest?

| Workload | Technique | Time | Space | Important property |
|---|---|---:|---:|---|
| one kth query, fixed unsorted array | randomized quickselect | expected `O(n)` | `O(1)` if mutation allowed | input order is destroyed |
| one kth query, preserve input | quickselect on a clone | expected `O(n)` | `O(n)` clone | clear ownership contract |
| stream with unknown future values | min-heap of size `k` | `O(n log k)` | `O(k)` | answer updates online |
| many ranks or sorted result needed | sort | `O(n log n)` | implementation-dependent | establishes complete order |
| merge sorted streams | heap of current heads | `O(N log m)` | `O(m)` | one frontier item per stream |

Do not choose a heap merely because the prompt says “largest.” The operation pattern decides.

## Iterative randomized three-way quickselect

For one-based kth largest, convert to the zero-based ascending target:

```text
target = length - k
```

Partition the active region into `< pivot`, `== pivot`, and `> pivot`:

```text
[ values < p | values == p | unknown | values > p ]
              ^ target inside this equal band means answer found
```

After partition:

- target before the equal band: keep the left region;
- target after the equal band: keep the right region;
- target in the band: return the pivot value.

The loop discards at least the equal band each iteration. This is particularly helpful for duplicate-heavy input. Random pivot choice makes consistently bad partitions unlikely, producing expected `O(n)` time, but the formal worst case remains `O(n^2)`.

The companion clones caller input and accepts a seed so randomized tests are reproducible. In a space-constrained interview, ask whether mutation is allowed and remove the clone if it is.

## Top-k design choices beyond the toy problem

For a streaming top-k service, clarify:

- Is `k` fixed or changed by clients?
- Are duplicate values separate events?
- Must ties have deterministic order?
- Is deletion or correction required?
- Is the result exact or can it be approximate?
- Does one process hold the stream, or must partial top-k sets merge?

A size-`k` min-heap retains the largest `k` values seen; its root is the kth largest only after at least `k` values arrive. If `k` changes upward, discarded history cannot be recovered without another store. A good SDE-2 answer calls out that product contract.

## Edge-case matrix

| Case | Expected handling | Frequent failure |
|---|---|---|
| empty heap | `peek`/`poll` follow explicit failure contract | reading backing index zero |
| first resize | grow from nonzero capacity | doubling zero to zero |
| duplicate priorities | retain all unless dedup is specified | treating heap as a set |
| integer extremes | direct comparisons or safe comparator | subtraction overflow |
| one child during sift-down | compare only existing child | reading past logical size |
| stale object priority | remove/reinsert or use immutable priority | assuming automatic repositioning |
| heap iteration | unordered except head guarantee | presenting iterator output as sorted |
| `k = 1` | maximum for kth-largest contract | off-by-one target conversion |
| `k = n` | minimum | rejecting a valid boundary rank |
| invalid `k` or empty input | throw with explicit contract | returning an ambiguous sentinel |
| all values equal | equal band ends selection immediately | repeatedly partitioning one element |
| caller input ownership | clone or document mutation | surprising destructive selection |

## Real interview follow-up round

**Interviewer:** Why is a heap not a BST?

**Candidate:** A heap guarantees only parent-child priority and optimizes access to one extreme. It cannot efficiently search for an arbitrary value. A BST maintains left/right key order and supports ordered search and traversal, with different shape requirements.

**Interviewer:** Why is bottom-up heapify linear?

**Candidate:** The `O(log n)` maximum applies only near the root. Most nodes are leaves or one level above them. Summing the amount of possible movement over all node heights gives a convergent weighted series bounded by `O(n)`.

**Interviewer:** Your priority queue contains objects. I changed one object's priority. What happens?

**Candidate:** The queue is not notified, so its internal heap can be inconsistent with current comparator values. Priority fields should be immutable while enqueued, or the item must be removed and reinserted through a supported design.

**Interviewer:** Why quickselect instead of a size-k heap?

**Candidate:** For one query over an in-memory array, quickselect is expected linear and uses no ongoing heap. For a stream or when values arrive incrementally, quickselect cannot maintain the answer, while a size-k heap does so in `O(log k)` per accepted value.

**Interviewer:** How do you know your heap code works?

**Candidate:** After every random offer or poll, I verify parent order, size, and head against Java's `PriorityQueue`, then drain both and compare removal order. I compare quickselect at every rank with a sorted clone. Those differential tests are in the companion.

## Run the verified companion

```bash
javac -Xlint:all -Werror HeapInterviewChecks.java
java HeapInterviewChecks
```

Expected final line:

```text
PASS 17 heap checks
```

Continue to the graph volume when a heap is used as a frontier for Dijkstra or Prim. Remember that the heap supplies the next candidate; the graph invariant is what makes choosing that candidate correct.
