# Heap Foundations: Partial Order Before Priority Patterns

A heap is useful when the next minimum or maximum matters more than complete sorted order. A binary min-heap guarantees that every parent is no greater than its children. It does not guarantee that siblings or entire levels are sorted.

## Complete-tree shape and array indexes

A binary heap uses a complete binary-tree shape, which packs naturally into an array.

```text
array indexes:       0
                  /     \
                 1       2
               /  \     / \
              3    4   5   6

left(i)   = 2*i + 1
right(i)  = 2*i + 2
parent(i) = (i - 1) / 2 for i > 0
```

Use bounds checks before calculating or accessing child indexes. For extremely large abstract indexes, `2*i + 1` can overflow; real Java arrays are already bounded, but production heap-like structures should use wider arithmetic when necessary.

## The min-heap invariant

For every index `i > 0`:

```text
heap[parent(i)] <= heap[i]
```

Therefore index 0 is globally minimal. The second-smallest item is somewhere among its children, not necessarily index 1 after arbitrary operations.

## Insert: sift up

Append the new value at the end to preserve complete shape. While it violates the parent relation, swap it upward.

```java
static void siftUp(List<Integer> heap, int index) {
    while (index > 0) {
        int parent = (index - 1) / 2;
        if (heap.get(parent) <= heap.get(index)) {
            return;
        }
        Collections.swap(heap, parent, index);
        index = parent;
    }
}
```

The height is O(log n), so insertion is O(log n).

## Remove minimum: replace and sift down

Move the last value to the root, shrink the heap, and repeatedly swap with the smaller child while the invariant is violated. Choosing the smaller child is essential; swapping with an arbitrary child can leave another violation.

## Why bottom-up heap construction is O(n)

Calling insert `n` times gives O(n log n). Bottom-up heapify sifts down internal nodes from the last parent to root. Most nodes are near the leaves and travel zero or one level; few nodes travel far. Summing work by height produces O(n), not O(n log n).

## Java `PriorityQueue`

```java
PriorityQueue<Integer> minimums = new PriorityQueue<>();
minimums.add(7);
minimums.add(2);
minimums.add(5);
System.out.println(minimums.peek()); // 2
System.out.println(minimums.remove()); // 2
```

For a max-heap:

```java
PriorityQueue<Integer> maximums =
        new PriorityQueue<>(Comparator.reverseOrder());
```

Important contracts:

- `peek`/`poll` return null on empty; `element`/`remove` throw;
- null elements are not permitted;
- iteration order is not sorted order;
- arbitrary `remove(object)` may be linear;
- mutating an enqueued object's priority field does not automatically reposition it.

## Comparator safety

Do not subtract:

```java
// Can overflow and violate comparator ordering
(a, b) -> a.priority - b.priority
```

Use:

```java
Comparator<Job> order = Comparator
        .comparingInt(Job::priority)
        .thenComparingLong(Job::sequence);
```

Tie-breakers make output deterministic and can preserve arrival policy. The comparator must be consistent enough for a stable ordering relation; mutable comparison fields are dangerous after insertion.

## Recognize bounded heaps

To retain the `k` largest values, use a min-heap of size at most `k`. The root is the weakest retained candidate. When a larger value arrives, replace the root.

```text
stream item -> compare with weakest top-k item -> discard or replace
```

This costs O(n log k) time and O(k) space, often better than sorting all `n` values when `k` is small.

For `k` smallest values, reverse the policy: retain a max-heap of size `k`.

## K-way merge intuition

For `k` sorted sources, keep only the next unconsumed item from each source in a min-heap. Polling chooses the next global output; then insert the following item from the same source. If `N` total items are emitted, time is O(N log k), auxiliary heap space O(k), excluding output.

## Foundation checkpoint

1. What order does a min-heap guarantee and what does it not guarantee?
2. Why does top-k largest use a min-heap?
3. Why is comparator subtraction unsafe?
4. Why is heap iteration not sorted output?
5. Explain bottom-up heapify without saying each node costs O(log n).
