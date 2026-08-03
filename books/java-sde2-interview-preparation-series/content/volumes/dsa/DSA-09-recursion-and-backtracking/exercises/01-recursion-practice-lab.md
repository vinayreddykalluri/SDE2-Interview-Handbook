# Recursion and Backtracking Practice Lab

## Knowledge checks

1. **Foundation:** What must become smaller or closer to completion on every recursive branch?
2. **Foundation:** Why is a shared path list removed from after a child returns?
3. **Interview Core:** Distinguish pruning from memoization.
4. **SDE-2 Follow-up:** Why can a recursive O(n) algorithm still be unsafe for large input?

## Predict and debug

5. Predict the output order of a preorder countdown that prints before recursing from 3 to 0.
6. What happens if a subsets method adds the mutable `path` reference to every leaf without copying?
7. Fix a permutation method that marks `used[i] = true` but never resets it.
8. A combination-sum solution prunes when the remaining target is negative, but values may be negative. Explain the correctness problem.

## Coding tasks

9. Implement recursive exponentiation by squaring, including negative-exponent contract discussion.
10. Generate all balanced parentheses for `n` pairs with sound pruning.
11. Return only the count of N-Queens arrangements without materializing boards.
12. Solve a maze while restoring the input on every exit path.
13. Convert a deep recursive graph traversal to an explicit `ArrayDeque` stack.

## Interview follow-ups

14. When would you use a boolean return instead of collecting all solutions?
15. Explain why output-sensitive complexity is more honest for subsets and permutations.
16. Describe how cancellation or time budgets should interact with restoration.

## Essential clinic task

17. **SDE-2 Follow-up:** Implement a Sudoku solver that validates the initial board, uses row/column/box constraint state, restores every failed choice, and distinguishes invalid input from a valid board with no completion.

## Recursion engine lab

18. **Foundation:** Write the exact enter/return frame trace for recursive sum over `[4,7,2]`.
19. **Interview Core:** Implement unique permutations for duplicate values and justify the sibling-skip condition using `used[]` state.
20. **Interview Core:** Implement reusable-candidate combination sum; reject any candidate that prevents strict progress.
21. **Interview Core:** Count N-Queens solutions using column and two diagonal arrays. Define the `n=0` result.
22. **Interview Core:** Implement word search without a board sentinel, support jagged rows, and prove state restoration after success and failure.
23. **SDE-2 Follow-up:** Differential-check unique permutation count against `n! / product(frequency!)` on small random multisets, then explain why this test is not a proof.
