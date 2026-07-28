# Strings and String Patterns Practice Solutions

Use these after attempting the lab. The purpose is to explain the reasoning and failure boundary, not to reward memorized code.

## A. Knowledge Check Solutions

1. A local `String` variable stores a reference value or null. The String object represents the immutable sequence; the variable does not directly contain the complete object.
2. Immutability prevents changing an existing String value. Reassignment stores a different reference in a non-final variable; it does not mutate the old object.
3. `==` compares reference identity. `String.equals` compares represented UTF-16 sequences. Use `Objects.equals` when either reference may be null.
4. Null means no String object; empty means length zero; blank means every contained code point is whitespace according to `isBlank`.
5. Valid indexes are `[0, n)`. No index is valid when `n == 0`.
6. `substring(begin, end)` includes `begin` and excludes `end`; result length is `end - begin`.
7. String methods cannot mutate the receiver. `strip()` returns the original value or another String; ignoring it leaves the variable unchanged.
8. `length()` counts UTF-16 units, `isEmpty()` tests length zero, and `isBlank()` tests whether the sequence is empty or only whitespace.
9. Every concatenation can copy the entire growing prefix. The total `1 + ... + n` copied work is quadratic for one-unit additions.
10. Builder length is stored content; capacity is buffer space available before growth. Capacity is not output length.
11. `split` accepts a regex and dot is a wildcard. Use `"\\."` or `Pattern.quote(delimiter)`.
12. Subtraction from `'0'` is safe only after validating an ASCII digit from `'0'` through `'9'`.
13. Java passes a copy of the String reference value. Reassigning the parameter cannot reassign the caller's variable.
14. A UTF-16 unit is a Java `char`; a code point is a Unicode value; a grapheme approximates a displayed character; a byte belongs to an encoded representation.
15. Supplementary code points consume two UTF-16 units, so unit count can exceed point count.
16. Canonically equivalent text may use a precomposed code point or a base-plus-combining sequence. Explicit normalization such as NFC can align them when the contract permits.
17. Before each comparison, every position outside `[left, right]` has already been paired with an equal mirror.
18. A 26-slot array requires an input alphabet mapped exactly to lowercase English `a..z`.
19. Right moves forward `n` times and left moves forward at most `n` times; total boundary movement is linear.
20. After repair, the window has at least every required multiplicity. `formedTypes == requiredTypes` is the constant-time validity test in the presented method.
21. `lps[i]` is the length of the longest proper prefix of pattern range `[0, i]` that is also its suffix.
22. Distinct sequences may hash to the same finite value. Verify content so collisions change performance, not correctness.
23. A code-point array position, UTF-16 index, byte offset, and grapheme position can differ. A caller cannot interpret a bare integer safely.
24. Bound decoded input, pattern length, regex work, distinct state, collected matches, logs, cache entries, and processing time where appropriate.

## B. Predict-the-Output Solutions

1. `false true`. The constructor creates a distinct object, while contents are equal.
2. `[  Java  ]`. The returned stripped value was ignored.
3. `terv`. Indexes 2, 3, 4, and 5 are selected.
4. `false`. Short-circuiting prevents `text.length()` from executing.
5. `3 2`. The supplementary code point occupies a surrogate pair.
6. `7`. ASCII digit code points are contiguous.
7. `cX a` without the space: the exact output is `cXa`. The builder first becomes `aXc`, then reverses.
8. First `2`, then `3`; the negative limit preserves the trailing empty field.
9. `true`, then `0`. Only the sign of a nonzero comparison result is contractual.
10. `2`. At the repeated `b`, left moves to 2; the older `a` position cannot move it backward.
11. `0`, then `1`; both overlapping occurrences are discoverable with a new start.
12. `0`, then `0`; Java's first empty-pattern position is the beginning boundary.

## C. Debugging Solutions

1. Use `index < text.length()`. Index `length()` is outside `[0, length)`.
2. Use `Objects.equals(first, second)` for nullable content comparison or reject null and call `first.equals(second)`.
3. Decide whether null is invalid or a false result, then check it before invoking methods: `text != null && !text.isEmpty()`.
4. Use one local `StringBuilder`; append the delimiter before every item except the first, and call `toString()` once.
5. Either validate ASCII with `'0' <= unit && unit <= '9'` before subtracting, or use `Character.digit(codePoint, 10)` for the broader Unicode contract.
6. Narrow the name/contract to UTF-16-unit reversal, use builder/code-point reversal for code points, or use a grapheme-boundary service for displayed characters.
7. Use `split("\\.")` or `split(Pattern.quote("."))`.
8. Use `left = Math.max(left, lastPosition + 1)` or store the next boundary and take the maximum directly.
9. Remove the key when its count reaches zero. Map size otherwise overstates current distinct units.
10. On a mismatch with nonzero prefix length, set `prefixLength = lps[prefixLength - 1]` and recompare the same current index.
11. When hashes agree, compare the candidate region with the pattern before returning it.
12. Return normalized positions explicitly, preserve a mapping from transformed boundaries to original indexes, or return the matched transformed value without claiming an original offset.

## D. Coding Task Guidance

1. Traverse `[0, length)`, increment on exact unit equality, and state whether null is rejected. Time `O(n)`, auxiliary `O(1)`.
2. Define null ordering first. Compare the common prefix; when it is equal, the shorter value differs at its length. Return `-1` only for value equality.
3. Skip runs of ASCII spaces, append a delimiter only between emitted words, and either scan backward by word ranges or collect boundaries. Define whether non-space whitespace counts.
4. Check nonempty, validate the first unit as an ASCII letter, then validate each remaining unit against letter/digit/underscore.
5. Negative accumulation supports `Integer.MIN_VALUE`; check multiply and subtract boundaries before each operation. Reject sign-only and non-ASCII inputs.
6. Advance UTF-16 index by `Character.charCount(codePoint)` and record each code point with its starting unit index.
7. Opposing pointers skip disallowed ASCII units, compare ASCII-lower values, and always make progress.
8. Scan matching ends. At the first mismatch, verify exactly the two ranges produced by deleting left or right. Branching only once preserves linear time.
9. Use a delimiter-safe 26-count signature and `LinkedHashMap` when stable first-group order is part of the output contract.
10. Maintain a prefix length bounded by every visited word; it can only shrink. Reject or define null elements.
11. Encode a documented grammar such as `unit + decimalCount` only when units cannot be decimal digits, or escape units. A decoder must reject zero, missing, overflowed, and malformed counts.
12. Maintain a 26-slot difference and a nonzero-slot count. Remove one outgoing unit and add one incoming unit per shift.
13. Store next boundary after last appearance, never move left backward, and update only on strictly longer results to keep the earliest tie.
14. Count at-most-`k` and at-most-`k - 1`; subtract. Add `right - left + 1L` because every suffix start inside the valid window contributes.
15. Track required and current counts plus satisfied required types. Expand to valid, record, then shrink until invalid. Update only on shorter length to keep earliest tie.
16. Build LPS once. After a complete match, fall back through LPS rather than reset to zero so overlaps remain discoverable.
17. Keep multiplication in `long`, use a documented base/modulus, normalize subtraction, and verify every hash candidate. Force a small test modulus to generate collisions in tests.
18. Generate small inputs from a fixed seed, compare every optimized result with an obviously correct brute-force result, and print the seed/input on mismatch.

## E. Interview Follow-up Solutions

1. Prefer `indexOf` for ordinary first-match application code when its API contract suffices; it is clearer and maintained by the JDK. Implement KMP for a required deterministic linear bound, education, or retained streaming pattern state.
2. Sorting is broadly applicable and simple at `O(n log n)` with copies. Frequency is `O(n)` but requires a bounded array alphabet or map state and a precise normalization unit.
3. `char[]` offers arbitrary unit swaps, builder is convenient for append/reverse, and `int[]` prevents surrogate splitting but uses code-point positions and `O(p)` storage. None alone gives grapheme segmentation.
4. Two indexes only describe boundaries. Correctness requires that expanding can create the invalid state and removing left units can repair it without reconsidering discarded starts.
5. Use `int` code points and map/array state over their domain. Return code-point positions or retain boundary mappings to original UTF-16 indexes.
6. Decode complete code points across byte chunks, retain pattern LPS and matched length, keep an absolute chosen-unit position, and emit bounded results. Incomplete byte sequences belong to the decoder state.
7. Limit input/pattern/output/cache sizes, reject or sandbox unsafe regex, redact logs, use time budgets where appropriate, and instrument counts/durations rather than text bodies.
8. Many patterns may justify a multi-pattern automaton or trie; approximate matching may require dynamic programming or specialized indexes. Workload and update frequency decide whether preprocessing pays.

## F. Cumulative Assessment Rubrics

### F1 rubric

A passing solution distinguishes null/empty/blank, quotes a literal delimiter, preserves trailing fields intentionally, parses both integer extremes, reverses supplementary code points without splitting a surrogate pair, and states the grapheme limitation. It explains immutable return values and uses failure tests rather than only happy paths.

### F2 rubric

A passing solution starts with quadratic baselines, defines the interval convention, states the unique and covering invariants, explains aggregate pointer movement, preserves tie policy, and includes empty, repeated, no-answer, final-boundary, and duplicate-target tests.

### F3 rubric

A passing solution derives `0,0,1,2,3,0,1`, handles empty and overlapping matches, never rewinds the text index, validates against naive search, and refuses to present normalized or code-point positions as original UTF-16 indexes without a mapping.

## G. Final Readiness Rubric

Ready work separates decoding from matching, defines every unit and failure policy, carries incomplete byte and KMP state across chunks, bounds emitted matches, proves linear decoded-unit work, retains only necessary state, avoids logging sensitive text, and compares algorithms by workload rather than prestige. A strong answer also identifies that normalization can require buffering or a streaming-normalization strategy and that a prebuilt index changes ownership, freshness, and operational cost.
