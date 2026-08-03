# Hashing Practice Lab

Do these without reading the solutions. Write the key, stored value, invariant, and complexity before code.

## Knowledge checks

1. **Foundation:** Why can two unequal objects have the same hash code?
2. **Foundation:** When does `putIfAbsent` preserve information that `put` would destroy?
3. **Interview Core:** Why does longest-subarray length usually store an earliest index while subarray count stores a frequency?
4. **SDE-2 Follow-up:** What assumption is hidden inside expected O(1) lookup language?

## Predict the behavior

5. What does `Map.of("a", 1).getOrDefault("b", 0)` return, and is `"b"` inserted?
6. A `HashSet<int[]>` receives two separately created arrays containing `{1, 2}`. What is the set size, and why?

## Debug the code

7. Fix a frequency loop that calls `counts.put(value, counts.get(value) + 1)` on the first occurrence.
8. A prefix-sum solution omits the initial `{0 -> 1}` state. Name the class of answers it misses.
9. A custom key uses `equals` on `customerId` and `region` but hashes only `customerId`. Is it correct? Is it good?

## Coding tasks

10. Return the first character that occurs exactly once in a string of Unicode code points.
11. Return the length of the longest subarray with equal numbers of zeroes and ones.
12. Group file paths by `(extension, sizeBucket)` using an immutable composite key.
13. Count pairs with a given difference without double-counting duplicates.
14. Design an API that tracks frequencies in a bounded recent time window. State what must expire.

## Interview follow-ups

15. Compare sorting and hashing for deduplication when input mutation is allowed.
16. Explain why an exact frequency service over an unbounded key stream needs a memory policy.
17. Explain how a denial-of-service input could challenge a hash-based design and what layer should mitigate it.

## Essential clinic tasks

18. **Interview Core:** Count subarrays whose XOR equals a target. State the empty-prefix seed and why the answer uses `long`.
19. **SDE-2 Follow-up:** Count subarrays with exactly K distinct values by deriving the result from two at-most counts. Prove what `right - left + 1` counts.

## Hash-table internals lab

20. **Foundation:** Draw the lookup path from `hashCode` to bucket to `equals` for three keys, two of which collide.
21. **Interview Core:** Implement a chained map with `put`, `get`, `containsKey`, and `remove`. Updating an existing key must not change size.
22. **Interview Core:** Add power-of-two resizing at load factor `0.75`. Demonstrate why copying the old bucket array without rehashing is incorrect.
23. **Interview Core:** Create a constant-hash test key and verify removal from the head, middle, and tail of one collision chain.
24. **SDE-2 Follow-up:** Demonstrate the mutable-key failure using a field included in both `equals` and `hashCode`. Explain why the entry remains in size but lookup fails.
25. **SDE-2 Follow-up:** Differential-test your scoped map contract against `HashMap` over randomized put/get/remove operations, including the null policy you explicitly choose.
