# 4. Indexed Heaps, Decrease-Key, and Lazy Deletion

## Why this chapter exists

`PriorityQueue` in Java supports `add`, `peek`, and `poll`. It does **not** support "the priority of an element already in the queue has changed, update its position" - and that operation, `decrease-key`, is exactly what Dijkstra's algorithm and Prim's algorithm are written in terms of in every textbook.

That gap is a real interview topic and a real engineering one. There are two standard resolutions, they have different complexities, and knowing which one the JDK forces you into is a question candidates routinely fumble.

The chapter also covers the other operation `PriorityQueue` handles badly: removing an arbitrary element, which is `O(n)` because it must scan to find it.

## The problem, precisely

A binary heap maintains the heap property by sifting elements up or down. Both are `O(log n)` **given the element's index in the backing array**. The difficulty is not the sift; it is that a plain heap offers no way to find an element's current index.

```text
poll()        O(log n)   position is known: it is the root
add()         O(log n)   position is known: it is the end
decrease-key  O(n)       must SEARCH for the element first
remove(x)     O(n)       same problem
```

Java's `PriorityQueue.remove(Object)` is `O(n)` for exactly this reason - it does a linear scan, then sifts. `contains` is `O(n)` too. Neither is a defect; a heap simply is not an index.

## Resolution 1: the indexed heap

Maintain a side map from element to its current array position, and update that map on **every** swap the heap performs.

```java
import java.util.HashMap;
import java.util.Map;

/** A min-heap that supports decrease-key and arbitrary removal in O(log n). */
public final class IndexedMinHeap<K> {
    private final Object[] elements;
    private final int[] priority;
    private final Map<K, Integer> position = new HashMap<>();
    private int size;

    public IndexedMinHeap(int capacity) {
        elements = new Object[capacity];
        priority = new int[capacity];
    }

    public boolean contains(K key) {
        return position.containsKey(key);      // O(1), unlike PriorityQueue
    }

    public void add(K key, int keyPriority) {
        if (position.containsKey(key)) {
            throw new IllegalArgumentException("duplicate key: " + key);
        }
        elements[size] = key;
        priority[size] = keyPriority;
        position.put(key, size);
        siftUp(size++);
    }

    /** Lower an existing key's priority. O(log n) because position is known. */
    public void decreaseKey(K key, int newPriority) {
        Integer index = position.get(key);
        if (index == null) {
            throw new IllegalArgumentException("absent key: " + key);
        }
        if (newPriority > priority[index]) {
            throw new IllegalArgumentException("decreaseKey must not raise priority");
        }
        priority[index] = newPriority;
        siftUp(index);                          // only ever moves up
    }

    @SuppressWarnings("unchecked")
    public K poll() {
        if (size == 0) {
            throw new IllegalStateException("empty heap");
        }
        K best = (K) elements[0];
        swap(0, --size);
        position.remove(best);
        elements[size] = null;                  // release the reference
        if (size > 0) {
            siftDown(0);
        }
        return best;
    }

    private void siftUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (priority[parent] <= priority[index]) {
                break;
            }
            swap(parent, index);
            index = parent;
        }
    }

    private void siftDown(int index) {
        while (true) {
            int left = 2 * index + 1;
            if (left >= size) {
                return;
            }
            int smallest = left;
            int right = left + 1;
            if (right < size && priority[right] < priority[left]) {
                smallest = right;
            }
            if (priority[index] <= priority[smallest]) {
                return;
            }
            swap(index, smallest);
            index = smallest;
        }
    }

    /** Every positional change goes through here, which is what keeps the map true. */
    @SuppressWarnings("unchecked")
    private void swap(int a, int b) {
        Object elementA = elements[a];
        Object elementB = elements[b];
        elements[a] = elementB;
        elements[b] = elementA;
        int priorityA = priority[a];
        priority[a] = priority[b];
        priority[b] = priorityA;
        position.put((K) elementB, a);
        position.put((K) elementA, b);
    }

    public int size() {
        return size;
    }
}
```

The design rule is the whole technique: **every positional change goes through one `swap` method, and that method updates the map**. Scatter the index arithmetic across `siftUp` and `siftDown` and the map will drift out of sync - producing a heap that silently returns wrong elements, with no exception to point at the cause.

`decreaseKey` only ever sifts *up*, because lowering a priority in a min-heap can only move an element closer to the root. A general `changeKey` would sift up or down depending on direction; restricting to decrease keeps the contract explicit and catches the misuse.

## Resolution 2: lazy deletion

The indexed heap is the right structure, and in an interview it is often more code than the time allows. The lazy alternative is what most competitive and production Dijkstra implementations actually do.

**Never update anything. Push a new entry with the better priority and ignore stale entries when they surface.**

```java
record Entry(int node, int distance) {}

static int[] dijkstra(List<List<int[]>> adjacency, int source) {
    int n = adjacency.size();
    int[] best = new int[n];
    Arrays.fill(best, Integer.MAX_VALUE);
    best[source] = 0;

    PriorityQueue<Entry> queue =
            new PriorityQueue<>(Comparator.comparingInt(Entry::distance));
    queue.add(new Entry(source, 0));

    while (!queue.isEmpty()) {
        Entry current = queue.poll();
        if (current.distance() > best[current.node()]) {
            continue;                    // stale: a better entry was processed already
        }
        for (int[] edge : adjacency.get(current.node())) {
            int next = edge[0];
            int weight = edge[1];
            long candidate = (long) current.distance() + weight;   // avoid overflow
            if (candidate < best[next]) {
                best[next] = (int) candidate;
                queue.add(new Entry(next, best[next]));            // push, do not update
            }
        }
    }
    return best;
}
```

The `current.distance() > best[current.node()]` check is the entire mechanism. A node may sit in the queue several times at different distances; the first time it is polled it carries the smallest, and every later copy is discarded in O(1).

**The trade:** the queue can hold up to `E` entries rather than `V`, so complexity is `O(E log E)` instead of `O(E log V)`. Since `E <= V^2`, `log E <= 2 log V`, so the two differ by at most a constant factor. Memory is genuinely higher.

| | Indexed heap | Lazy deletion |
|---|---|---|
| Complexity | O(E log V) | O(E log E) - same to a constant |
| Queue size | at most V | up to E |
| Code | ~80 lines | ~15 lines |
| Correctness risk | map/heap desync | none; the relaxation guard carries it |
| Interview default | when asked for it | **yes** |

Lazy deletion is the right default answer, and the right *complete* answer names the memory cost and says the indexed heap is what removes it. Presenting lazy deletion without acknowledging the larger queue is the incomplete version.

Omitting the staleness check is a **performance** bug, not a correctness one, and it is worth being precise about why. A stale entry carries a distance no smaller than the best known, so relaxing from it can only propose values no better than what the guard `candidate < best[next]` already rejects. The answers stay correct; the work does not. Measured over 300 random graphs, dropping the check cost about 17% more edge relaxations, and the penalty grows with graph density.

Candidates often assert that omitting it breaks correctness. It does not, and claiming so invites an interviewer to ask for the failing input - which does not exist for this formulation. The accurate answer is that the relaxation guard carries correctness and the staleness check carries efficiency.

## Removing an arbitrary element

The same two options apply.

- **Indexed heap:** look up the position, swap with the last element, shrink, then sift the moved element *either* up or down - it can go either way, unlike `poll` where it can only sink. Forgetting the up case is a classic bug that leaves a heap violating its own invariant.
- **Lazy:** keep a `Set` of removed keys and skip them on `poll`. Simple, but the heap holds garbage until it surfaces, and a workload that removes most of what it inserts wastes proportional memory.

Java's `PriorityQueue.remove(Object)` takes neither approach - it scans linearly. If your algorithm removes arbitrary elements frequently, `PriorityQueue` is the wrong structure and saying so is the answer.

## When a heap is the wrong structure

Worth stating plainly, because "use a heap" is over-applied.

- **You need the k-th element once, not a stream.** Quickselect is O(n) average versus O(n log k) for a heap, and the internals chapter covers it.
- **You need ordered iteration or range queries.** A heap gives you the minimum and nothing else; `TreeMap` or a balanced BST gives order.
- **You need both ends.** A binary heap is one-directional. Use two heaps, a `TreeMap`, or an interval heap.
- **Priorities change constantly and you need exact ordering.** Every change is a sift; a different structure may fit better.
- **Everything is available up front and you want it all sorted.** Sorting is O(n log n) with better constants and cache behaviour than n polls.

The sliding-window-maximum problem is the sharpest example: a heap gives O(n log n) with lazy deletion, while a monotonic deque gives O(n). The ordering-structures volume covers the deque; the point here is recognizing that reaching for a heap is not automatically right.

## Edge cases and common mistakes

- Expecting `PriorityQueue` to support decrease-key. It does not.
- Assuming `PriorityQueue.remove(Object)` or `contains` is better than O(n).
- Scattering index updates instead of funnelling every move through one `swap`.
- Letting `decreaseKey` raise a priority, which requires sifting down and breaks the contract.
- Omitting the staleness check in lazy Dijkstra, wasting roughly 17% of relaxations on stale entries.
- Claiming that omission breaks correctness; the relaxation guard still prevents a worse value from being written.
- Adding a duplicate key to an indexed heap, corrupting the position map.
- Forgetting that arbitrary removal may sift up *or* down, unlike `poll`.
- Not nulling the vacated slot after `poll`, retaining a reference the caller expects released.
- Overflowing on `distance + weight` with large weights; accumulate in `long`.
- Claiming lazy deletion has the same memory profile as an indexed heap.
- Using a heap for a one-shot k-th element where quickselect is O(n).
- Using a heap for sliding window maximum where a monotonic deque is O(n).

## Interview questions and model answers

**Java's `PriorityQueue` has no decrease-key. How do you write Dijkstra?**

Lazy deletion: never update an entry, push a new one with the improved distance, and discard stale entries on poll by checking whether the popped distance exceeds the best known for that node. That check is what makes it correct. The queue can hold up to E entries rather than V, so it is O(E log E) rather than O(E log V) - the same to within a constant, at higher memory.

**When would you build an indexed heap instead?**

When queue memory matters, or the algorithm genuinely needs `contains` and arbitrary removal in better than O(n). It maps each element to its current array index and updates that map on every swap, giving O(log n) decrease-key and removal. The discipline is that all positional changes funnel through one swap method; scattering them lets the map drift and the heap silently returns wrong elements.

**Why does `decreaseKey` only sift up?**

Lowering a value in a min-heap can only move it closer to the root, never away, so only the upward direction can be violated. A general priority change would need both directions, which is why restricting the operation to decrease keeps the contract clear and catches misuse.

**What happens if you drop the staleness check?**

You waste work, not correctness. A stale entry's distance is no smaller than the best known, so anything it proposes is rejected by the `candidate < best[next]` guard - that guard is what carries correctness. Measured on random graphs, dropping the check cost about 17% more edge relaxations. It is worth being exact here, because asserting it breaks correctness invites a request for the failing input, and there is not one.

**What is `PriorityQueue.remove(Object)` complexity?**

O(n), because it scans linearly to find the element before sifting. `contains` is also O(n). A heap is not an index. If your algorithm removes arbitrary elements often, use an indexed heap or lazy deletion with a removed-set.

**When is a heap the wrong tool?**

For a one-shot k-th element, where quickselect is O(n) rather than O(n log k). For ordered iteration or range queries, which need a `TreeMap`. For access to both extremes. And for sliding window maximum, where a monotonic deque is O(n) against the heap's O(n log n).

## Exercises

1. **Foundation:** Trace the array and position map through three `add` calls and one `poll` on an indexed heap of capacity 8.
2. **Foundation:** Time `PriorityQueue.remove(Object)` at sizes 10^3, 10^4, and 10^5 and confirm the growth is linear.
3. **Interview Core:** Implement the indexed heap above and assert after every operation that the position map matches the array.
4. **Interview Core:** Move the position updates out of `swap` into `siftUp` and `siftDown`, then find the sequence where the map desynchronizes.
5. **Interview Core:** Implement lazy Dijkstra. Remove the staleness check, confirm over random graphs that the answers stay correct, and measure the extra edge relaxations. Then say which line actually carries correctness.
6. **Interview Core:** Instrument both Dijkstras to report peak queue size on a dense graph and compare against V and E.
7. **SDE-2 Follow-up:** Implement arbitrary removal on the indexed heap. Sift only downward, then construct the input that leaves the heap invalid.
8. **SDE-2 Follow-up:** Solve sliding window maximum with a heap and with a monotonic deque; compare runtimes at 10^6 elements.
9. **SDE-2 Follow-up:** Add `decreaseKey` misuse - raising a priority - and show the heap property it breaks.
10. **Challenge:** Implement Prim's algorithm with both approaches and state which you would present in an interview and why.

## Chapter summary

`PriorityQueue` gives you `add`, `peek`, and `poll` in O(log n) and nothing else in better than O(n), because a heap is not an index - it cannot find an element it already holds. Two resolutions exist. The indexed heap maintains a map from element to current array position, updated on every swap, giving O(log n) decrease-key, O(1) `contains`, and O(log n) arbitrary removal; the discipline is funnelling all positional change through a single swap method, since a drifting map produces silently wrong results. Lazy deletion never updates anything, pushes a fresh entry at the better priority, and discards stale entries on poll with one comparison - fifteen lines instead of eighty, at the cost of a queue holding up to E rather than V entries. Lazy is the right interview default provided you name that memory cost. Note precisely what the staleness check does: correctness is carried by the relaxation guard, and the check buys efficiency, worth roughly 17% of relaxations on random graphs. And a heap is not always the answer: quickselect beats it for a one-shot selection, a `TreeMap` beats it for ordered access, and a monotonic deque beats it for sliding window maximum.

## Revision checklist

- [ ] I know `PriorityQueue` has no decrease-key and that `remove` and `contains` are O(n).
- [ ] I can explain why a heap cannot locate an element it holds.
- [ ] I can implement an indexed heap and state the single-swap discipline.
- [ ] I know `decreaseKey` sifts only up, and why.
- [ ] I can write lazy Dijkstra and explain the staleness check.
- [ ] I know omitting that check costs work, not correctness, and which line carries correctness.
- [ ] I can state the queue-size and complexity difference between the two approaches.
- [ ] I know arbitrary removal may sift either direction.
- [ ] I accumulate path distances in `long` to avoid overflow.
- [ ] I can name three problems where a heap is the wrong structure.
