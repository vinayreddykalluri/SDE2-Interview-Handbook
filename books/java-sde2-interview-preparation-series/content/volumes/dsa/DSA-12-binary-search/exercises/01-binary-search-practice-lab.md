# Binary Search Practice Lab

1. Trace exact search for target 8 in `[2, 5, 8, 12, 16]`.
2. Give lower and upper bounds of 2 in `[1, 2, 2, 2, 5]`.
3. Explain why `while (left < right)` matches a half-open lower-bound interval.
4. Debug `middle = (left + right) / 2` for very large indexes.
5. Debug a loop that assigns `left = middle` with a lower-biased midpoint.
6. Recover insertion point from a negative `Arrays.binarySearch` result.
7. Implement integer square root without multiplication overflow.
8. Return the first index whose timestamp is at least a requested instant.
9. Find a peak element with a proof of the retained side.
10. Search a row-major sorted matrix without allocating a flattened copy.
11. Find the minimum eating speed or processing rate that meets a deadline.
12. Test whether a proposed feasibility predicate is actually monotone.
13. Explain how duplicates change rotated-array worst-case complexity.
14. Compare binary search on an array with search in a linked list.
15. Define a safe stopping rule for floating-point binary search.
16. **SDE-2 Follow-up:** Find the median of two sorted arrays by binary-searching a partition in the shorter array. Explain all four boundary values.
17. **SDE-2 Follow-up:** Find the kth smallest occurrence in a row-and-column sorted matrix by searching the value domain. Prove the `count(values <= middle)` predicate is monotone.

## Boundary engineering lab

18. **Foundation:** Annotate lower bound with its two proven regions and dry-run target before, inside, and after the array.
19. **Interview Core:** Build a closed-domain first-true helper whose high endpoint must be true and whose midpoint is safe across signed `long` bounds.
20. **Interview Core:** Implement floor integer square root for every nonnegative `long` without multiplying the midpoint.
21. **Interview Core:** Search a rotated distinct array, then give a duplicate counterexample where sorted-half detection is ambiguous.
22. **Interview Core:** Search a row-major rectangular matrix through flat indexes computed in `long`; reject jagged input.
23. **SDE-2 Follow-up:** Differential-test lower/upper bounds against linear scans on random sorted duplicate-heavy arrays.
