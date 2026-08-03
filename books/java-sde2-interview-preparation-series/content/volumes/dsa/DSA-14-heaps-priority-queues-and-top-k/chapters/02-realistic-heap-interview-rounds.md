# Realistic Heap and Priority-Queue Interview Rounds

## Round 1: kth largest value in a stream

### Prompt

Design a class that accepts values one at a time and reports the kth largest value seen so far once at least `k` values exist.

### Candidate design

Maintain a min-heap of the largest `k` values. Its root is the kth largest because every retained value is at least as large as every discarded value.

```java
static final class KthLargest {
    private final int k;
    private final PriorityQueue<Integer> largest = new PriorityQueue<>();

    KthLargest(int k) {
        if (k <= 0) throw new IllegalArgumentException("k must be positive");
        this.k = k;
    }

    OptionalInt add(int value) {
        if (largest.size() < k) {
            largest.add(value);
        } else if (value > largest.peek()) {
            largest.poll();
            largest.add(value);
        }
        return largest.size() == k ? OptionalInt.of(largest.peek()) : OptionalInt.empty();
    }
}
```

Duplicates count as separate stream observations unless the contract says kth distinct. Each add costs O(log k) when the heap changes and O(1) when discarded.

### Follow-up answers

**Kth distinct?** Add a membership set synchronized with the heap, and remove from the set when evicting. Clarify whether unbounded historical duplicates need tracking.

**Distributed stream?** Local top-k summaries can be merged, but ordering, duplicate identity, late events, and consistency windows need a system contract.

## Round 2: merge k sorted linked lists

### Prompt

Merge `k` sorted linked lists by reusing nodes.

### Model answer

Put each non-null head in a min-heap. Poll the smallest node, append it, and enqueue its original successor.

```java
static ListNode merge(ListNode[] lists) {
    PriorityQueue<ListNode> queue = new PriorityQueue<>(
            Comparator.comparingInt(node -> node.value));
    for (ListNode head : lists) {
        if (head != null) queue.add(head);
    }
    ListNode sentinel = new ListNode(0);
    ListNode tail = sentinel;
    while (!queue.isEmpty()) {
        ListNode node = queue.poll();
        if (node.next != null) queue.add(node.next);
        tail.next = node;
        tail = node;
    }
    return sentinel.next;
}
```

If there are `N` nodes total, time is O(N log k), heap space O(k), and relinking uses O(1) extra nodes. The input lists are consumed/relinked.

### Follow-up answers

**Why enqueue successor before or after append?** Either can work if the original successor reference remains accessible. With more aggressive detachment, save it before overwriting links.

**What if lists share nodes?** Destructive merge can duplicate or cycle. Require disjoint ownership or copy nodes and track identity.

## Round 3: running median

### Prompt

After each inserted integer, return the median.

### Candidate design

Use a max-heap `lower` for the smaller half and min-heap `upper` for the larger half. Maintain:

- sizes differ by at most one;
- `lower` may contain one extra item;
- every lower value is no greater than every upper value.

```java
static final class MedianTracker {
    private final PriorityQueue<Integer> lower =
            new PriorityQueue<>(Comparator.reverseOrder());
    private final PriorityQueue<Integer> upper = new PriorityQueue<>();

    void add(int value) {
        if (lower.isEmpty() || value <= lower.peek()) lower.add(value);
        else upper.add(value);
        if (lower.size() > upper.size() + 1) upper.add(lower.poll());
        else if (upper.size() > lower.size()) lower.add(upper.poll());
    }

    double median() {
        if (lower.isEmpty()) throw new IllegalStateException("no values");
        if (lower.size() != upper.size()) return lower.peek();
        return ((long) lower.peek() + upper.peek()) / 2.0;
    }
}
```

Cast before addition to prevent `int` overflow.

### Follow-up answers

**Sliding-window median?** Arbitrary expiry is not efficient with plain `PriorityQueue`; use lazy deletion keyed by value/index, indexed heaps, or balanced multisets with counts.

**Concurrency?** Both heaps and their invariant must be updated atomically. A thread-safe wrapper needs one coherent synchronization boundary.

## Closing answer pattern

State the retained partial order, heap direction, size bound, comparator/tie policy, mutation assumptions, per-operation and total costs, and why sorting everything is unnecessary.
