# Heaps and Priority Queues Practice Lab Solutions

1. Children of index `i` are `2i+1` and `2i+2`; parent is `(i-1)/2` for nonroot indexes.
2. Every parent is no greater than its children. Repeated parent relationships imply the root is no greater than any descendant.
3. No. Iteration exposes heap-array order; only the head is guaranteed minimal. Poll a copy repeatedly for sorted values.
4. Subtraction can overflow and reverse ordering. Use `Comparator.comparingInt(Job::score).reversed()` plus a deterministic tie-breaker.
5. The queue does not repair its position. Remove and reinsert through a controlled update API, or store immutable priority snapshots and discard stale entries lazily.
6. Compare a node with its smaller child and swap until valid. Heapify from the last parent down; aggregate height work is O(n).
7. Maintain a max-heap of size k; its root is the weakest retained small value. O(n log k) time, O(k) space.
8. Count frequencies, then use a bounded heap or sort entries. Define whether equal counts use numeric, lexical, or first-seen order.
9. Heap entries contain `(value, source, index)`. Poll, emit, then enqueue the next index from that source. O(N log k).
10. Sort intervals by start, keep end times in a min-heap, reuse all rooms ending before the next start, and track peak heap size. Clarify whether touching endpoints overlap.
11. Use lower max-heap and upper min-heap. Average as `((long)a + b) / 2.0`.
12. Sorting is O(n log n) and orders everything; bounded heap is O(n log k), streaming, and O(k) space; quickselect is expected O(n), mutates or copies input, and has O(n squared) worst case without stronger pivot guarantees.
13. Stale heap entries remain until they reach the top. Track validity/version counts and periodically compact if stale memory can grow too large.
14. Compare priority first and monotonic sequence number second. Sequence generation, overflow, and concurrency must be controlled.
15. `remove(object)` generally searches linearly before repairing the heap. Use an indexed heap or lazy invalidation when arbitrary updates/removals are core operations.
16. Keep at most k points in a max-heap whose root is the worst retained candidate. Compute squared distance in `long`, compare with `Long.compare`, and evict after each insertion beyond k. Sorting the retained k values is necessary only when the output contract requires order. Time is O(n log k), space O(k).
17. Seed a min-heap with one entry per list and track the frontier maximum. The heap minimum and maximum define a complete range. Advancing a nonminimum list cannot improve the left boundary, so advance the minimum's source. Stop when that source ends because no later frontier covers every list. Total time is O(N log k), space O(k).
