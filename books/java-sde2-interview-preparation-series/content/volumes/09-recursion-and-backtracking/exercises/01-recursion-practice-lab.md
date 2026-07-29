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
