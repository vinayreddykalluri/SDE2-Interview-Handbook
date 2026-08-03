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

## Pointer surgery solutions

17. Start `previous=null,current=1`. Save 2, point 1 to null, advance to 2; save 3, point 2 to 1, advance to 3; save null, point 3 to 2, advance null. `previous=3` is the new head. Saving next before overwriting is the reachability invariant.
18. First validate `1 <= left <= right <= length`. Link a sentinel to head, walk `before` to position `left-1`, keep `rangeTail=before.next`, and repeat `right-left` times: detach `rangeTail.next` and insert it immediately after `before`. Return `sentinel.next`.
19. Link a sentinel, advance `ahead` exactly `n` nodes while validating length, then move `ahead` and `beforeTarget` together until ahead is last. Delete `beforeTarget.next`. The sentinel makes `n==length` remove the original head without a special branch.
20. Floyd detects a meeting and then advances head seeker and meeting pointer one step to entry. Intersection first aligns acyclic list lengths and advances until references are identical. Payload equality is irrelevant; cyclic intersection requires a different case analysis and is outside this method's contract.
21. Maintain a sentinel tail. On equality take the left node first, advance only the chosen input, and append by relinking. Attach the remaining suffix once one input ends. Require sorted, acyclic, disjoint inputs; overlapping inputs can be linked twice and form a cycle.
22. Insert each copy after its original, set copied random as `original.random == null ? null : original.random.next`, then detach by restoring original next and linking copy next. Verify no copied node is an original, copied random identities point inside the copy chain, self-random remains copied self, and every original next/random is unchanged.
