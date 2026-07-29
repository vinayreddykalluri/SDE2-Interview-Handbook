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
