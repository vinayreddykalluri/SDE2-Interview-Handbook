# Linked-List Practice Lab Solutions

1. Assignment copies only the reference value, so both variables reach the same nodes.
2. A sentinel is a synthetic predecessor used to normalize head operations. Its payload is not logical data; return `sentinel.next`.
3. State the actual loop. With `while (fast != null && fast.next != null)`, slow ends at 3, the second middle. With `while (fast.next != null && fast.next.next != null)`, slow ends at 2, the first middle.
4. Every active call consumes a Java stack frame, producing O(n) call depth.
5. After reversal, `current.next` points backward. Moving through it returns to the processed prefix. Save the original next reference first.
6. Either require `1 <= n <= length` and validate before mutation, or use a sentinel and advance fast `n + 1` steps while checking null, failing before relinking.
7. The second half remains reversed. Compare into a result variable, restore, then return; use `finally` if comparison can fail unexpectedly.
8. Use a sentinel tail. Append the smaller current node, advance that list, and attach the remaining suffix. O(n + m) time, O(1) auxiliary space, inputs are relinked.
9. Build `before` and `after` chains with two sentinels, detach or advance nodes carefully, then connect them. Stable O(n) time and O(1) auxiliary nodes.
10. Align lengths, advance the longer head by the difference, then move both until references are identical. O(n + m) time, O(1) space.
11. Use a map from original node identity to clone, or interleave clones between originals and then separate. State whether temporary mutation is permitted.
12. Merge runs of widths 1, 2, 4, and so on. Split safely, merge with a tail, and repeat until width reaches the length. O(n log n) time, O(1) auxiliary references.
13. Shared nodes can be appended twice or relinked into a cycle. Validate disjointness or define copy/ownership semantics.
14. An immutable cons list shares its tail. Prepending is O(1); changing an interior element copies the path to it. Readers avoid mutation races.
15. `ArrayDeque` has compact array storage and efficient operations at both ends. `LinkedList` allocates a node per element and has poorer locality; it is useful only when its specific contracts matter.
16. Map each key to its one live node. Sentinels bound a most-recent-to-least-recent doubly linked list. An access detaches the found node and inserts it after the front sentinel; insertion evicts `back.previous` when size exceeds capacity and removes that node from the map. At every public boundary, map membership and list membership must be bijective and size must not exceed capacity. `get` changes recency order, so concurrent safety must cover both structures as one operation.
