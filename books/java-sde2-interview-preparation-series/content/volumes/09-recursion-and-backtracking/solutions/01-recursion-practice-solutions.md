# Recursion and Backtracking Practice Lab Solutions

1. A well-founded progress measure must move toward a base case: a smaller length, later index, lower remaining decision count, or structurally smaller subtree.
2. Sibling branches share the list. Removing restores the caller's invariant so one branch's choice does not leak into the next.
3. Pruning proves a branch cannot contain a required answer. Memoization caches the answer for a repeated state. Backtracking may have few repeated states even when its tree is large.
4. Java consumes one stack frame per active call and does not promise tail-call elimination. O(n) work with depth O(n) can overflow the stack.
5. It prints `3 2 1` when printing before the recursive call; printing after it would produce `1 2 3` during unwinding.
6. Every output entry aliases the same list and eventually reflects its final restored state, commonly appearing empty.
7. Reset `used[i] = false` after the recursive call, paired with removal of the chosen path value.
8. A negative remaining target can later rise when a negative candidate is chosen, so the prune can discard valid solutions. The problem also needs a termination rule to prevent unlimited reuse cycles.
9. For nonnegative exponent, return 1 at zero, recurse on `exponent / 2`, square once, and multiply by the base for odd exponents. For negative exponent, choose floating-point output and guard the minimum integer negation case with a wider type.
10. Track counts of open and close parentheses. Add an open parenthesis while `open < n`; add a close while `close < open`. A leaf occurs at length `2n`.
11. Track occupied columns and diagonals in boolean arrays or bit sets. Increment a counter at row `n`; do not build strings or boards.
12. Save the cell, mark it, explore, and restore before every return. `try/finally` is appropriate if deeper code can throw or observe cancellation.
13. Push the start, then pop, skip already visited nodes, mark, process, and push neighbors. To preserve recursive order, push neighbors in reverse order.
14. Use boolean short-circuiting for existence or first-solution queries; collect only when the contract requires enumeration.
15. The output itself can be exponential and includes copied elements. Complexity should include nodes visited and total emitted representation.
16. Check a cancellation token at defined boundaries and guarantee undo in `finally`. Partial output and input mutation policies must be documented.
17. Validate shape, symbols, and duplicate starting digits while initializing three `boolean[9][10]` tables. At an empty cell, try only digits unused by its row, column, and box; mark, recurse, and unmark on failure. Returning `false` after complete search means a valid initial board has no solution, while malformed or contradictory input should be rejected before search. Minimum-remaining-values ordering can reduce the practical tree without changing correctness.
