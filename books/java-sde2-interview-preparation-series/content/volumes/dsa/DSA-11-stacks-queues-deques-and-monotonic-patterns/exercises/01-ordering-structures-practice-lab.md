# Stack, Queue, and Deque Practice Lab

1. **Foundation:** Map `push`, `pop`, and `peek` to explicit deque ends.
2. **Foundation:** Explain the difference between `removeFirst` and `pollFirst` on empty input.
3. **Interview Core:** State the invariant for a delimiter-validation stack.
4. **Interview Core:** Why should BFS usually mark visited at enqueue time?
5. Predict the output after `addLast(1)`, `addLast(2)`, `removeLast()`.
6. Debug a queue that uses `addLast` and `removeLast`.
7. Debug a next-greater loop that stores values but later computes index distance.
8. Implement postfix-expression evaluation with malformed-input checks.
9. Implement a fixed-capacity circular queue.
10. Implement next smaller value to the right.
11. Solve largest rectangle in a histogram using sentinels or a final flush.
12. Implement a min stack with duplicate minima.
13. Explain why `PriorityQueue` iteration is not sorted output.
14. Compare a monotonic deque with a balanced tree for sliding maximum.
15. Define backpressure behavior for a bounded work queue.
16. **Interview Core:** Compute trapped rain water with a monotonic stack. For every popped basin bottom, identify the left boundary, right boundary, width, and bounded height.

## Internal mechanics lab

17. **Foundation:** Given capacity eight, head six, and size four, map logical offsets to physical indexes.
18. **Interview Core:** Implement a resizable primitive circular deque with both-end operations and a nonzero initial capacity.
19. **Interview Core:** Resize a wrapped deque by copying logical order; show the bug produced by copying physical order directly.
20. **Interview Core:** Implement largest histogram rectangle with a virtual trailing zero and `long` area.
21. **Interview Core:** Implement postfix evaluation with operand-order, arity, division, token, and overflow validation.
22. **SDE-2 Follow-up:** Differential-test the custom deque against `ArrayDeque` after every randomized operation, including repeated wraparound and resize.
