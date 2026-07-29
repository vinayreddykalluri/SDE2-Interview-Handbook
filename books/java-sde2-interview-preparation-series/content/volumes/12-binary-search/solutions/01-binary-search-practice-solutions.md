# Binary Search Practice Lab Solutions

1. Start `[0,4]`, middle 2, value 8, return index 2.
2. Lower bound is 1 and upper bound is 4; the count is 3.
3. `[left, right)` is nonempty exactly when `left < right`. Assigning `right = middle` retains middle as a candidate while excluding the old right side.
4. Use `left + (right - left) / 2`; for long answer domains use long arithmetic throughout.
5. When two candidates remain, lower-biased middle can equal left, so no progress occurs. Exclude middle with `left = middle + 1` or use an upper-biased last-true template.
6. `insertionPoint = -result - 1` when result is negative.
7. Search `x` over a bounded nonnegative domain and compare `middle <= value / middle` rather than `middle * middle <= value`; handle middle zero.
8. This is lower bound over ordered timestamps using predicate `timestamp >= requested`.
9. Compare `values[middle]` with `values[middle + 1]`. If rising, a peak exists to the right; otherwise one exists at or left of middle. Retain the proven side.
10. Map virtual index `i` to row `i / columns` and column `i % columns`; validate rectangular shape and use long only if total index arithmetic can overflow.
11. Bound the answer, define `canFinish(rate)`, prove that a faster rate remains feasible, then find first true.
12. Try to construct `a < b` where predicate(a) is true but predicate(b) is false. If one exists, first-true binary search is invalid.
13. Equal boundary and middle values can hide which half is sorted. Safely shrinking boundaries can remove only one or two candidates, producing O(n) worst case.
14. Arrays provide O(1) midpoint access. Locating a linked-list midpoint costs traversal, so repeated binary search does not retain O(log n) total time.
15. Use a fixed iteration count tied to required precision, or stop when interval width is below epsilon while guarding stagnation from representable floating-point spacing.
