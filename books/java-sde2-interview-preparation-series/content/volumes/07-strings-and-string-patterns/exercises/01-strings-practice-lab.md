# Strings and String Patterns Practice Lab

Attempt these without the solution chapter open. For each coding task, first write the input contract, text unit, equality policy, output/tie rule, invariant, time, auxiliary space, output space, and at least four failure-focused tests.

## A. Knowledge Checks

1. **Foundation:** What does a local `String` variable store? How is that different from the String object?
2. **Foundation:** Why can a String be immutable while its variable can be reassigned?
3. **Foundation:** What does `==` compare for references? What does `.equals()` compare for String?
4. **Foundation:** Distinguish null, empty, and blank.
5. **Foundation:** Which indexes are valid for a String of length `n`?
6. **Foundation:** Explain the half-open interval used by `substring(begin, end)`.
7. **Foundation:** Why does ignoring the return value of `strip()` leave the variable unchanged?
8. **Foundation:** What is the difference between `length()`, `isEmpty()`, and `isBlank()`?
9. **Foundation:** Why can repeated `result += unit` in a loop become quadratic?
10. **Foundation:** Distinguish StringBuilder length from capacity.
11. **Foundation:** Why is `split(".")` not a literal-dot split?
12. **Foundation:** When is `character - '0'` a safe digit conversion?
13. **Interview Core:** What does Java pass when a String is an argument?
14. **Interview Core:** Distinguish UTF-16 code unit, code point, grapheme cluster, and encoded byte.
15. **Interview Core:** Why can `length()` exceed code-point count?
16. **Interview Core:** What normalization problem can make visually similar strings unequal?
17. **Interview Core:** State the palindrome pointer invariant.
18. **Interview Core:** What contract justifies a 26-element frequency array?
19. **Interview Core:** Why can a nested shrink loop still be linear overall?
20. **Interview Core:** State the validity invariant for minimum covering window.
21. **Interview Core:** Define every entry in an LPS table precisely.
22. **Interview Core:** Why does rolling-hash equality require content verification?
23. **SDE-2 Follow-up:** Why must result index units be part of a Unicode-aware API contract?
24. **SDE-2 Follow-up:** Which limits protect a production text-processing endpoint from unbounded work or retained output?

## B. Predict the Output

### B1 - Identity and value

```java
String first = "java";
String second = new String("java");
System.out.println((first == second) + " " + first.equals(second));
```

### B2 - Ignored immutable result

```java
String text = "  Java  ";
text.strip();
System.out.println("[" + text + "]");
```

### B3 - Half-open substring

```java
String text = "interview";
System.out.println(text.substring(2, 6));
```

### B4 - Null-safe short circuit

```java
String text = null;
System.out.println(text != null && text.length() > 0);
```

### B5 - Supplementary code point

```java
String text = "A\uD83D\uDE42";
System.out.println(text.length() + " " + text.codePointCount(0, text.length()));
```

### B6 - Character arithmetic

```java
char digit = '7';
System.out.println(digit - '0');
```

### B7 - Builder mutation

```java
StringBuilder builder = new StringBuilder("abc");
builder.setCharAt(1, 'X');
System.out.println(builder.reverse());
```

### B8 - Trailing split fields

```java
System.out.println("a,b,".split(",").length);
System.out.println("a,b,".split(",", -1).length);
```

### B9 - Compare contract

```java
System.out.println("apple".compareTo("banana") < 0);
System.out.println("same".compareTo("same"));
```

### B10 - Longest unique boundary

```java
String text = "abba";
int[] next = new int[128];
java.util.Arrays.fill(next, -1);
int left = 0;
for (int right = 0; right < text.length(); right++) {
    char unit = text.charAt(right);
    left = Math.max(left, next[unit]);
    next[unit] = right + 1;
}
System.out.println(left);
```

### B11 - Overlapping matches

```java
System.out.println("aaaa".indexOf("aa"));
System.out.println("aaaa".indexOf("aa", 1));
```

### B12 - Empty pattern

```java
System.out.println("abc".indexOf(""));
System.out.println("".indexOf(""));
```

## C. Debug the Code

1. **Foundation:** A traversal uses `index <= text.length()`. Correct the boundary and explain the exception.
2. **Foundation:** A content comparison uses `first == second`. Repair it for nullable values.
3. **Foundation:** Code calls `text.isEmpty()` before checking whether `text` is null. Repair the contract and order.
4. **Foundation:** A loop repeatedly executes `result = result + word`. Replace it without adding a trailing delimiter.
5. **Foundation:** A parser validates with `Character.isDigit(unit)` and converts using `unit - '0'`. Make the accepted grammar and conversion consistent.
6. **Interview Core:** A method reverses a `char[]` and claims full Unicode code-point support. Repair the implementation or narrow the contract.
7. **Interview Core:** `"a.b".split(".")` is expected to yield two tokens. Correct the delimiter.
8. **Interview Core:** Longest-unique code sets `left = last.get(unit) + 1` and moves left backward on `"abba"`. Repair the transition.
9. **Interview Core:** A frequency-map window decrements a count to zero but leaves the key in the map. Repair distinct-count semantics.
10. **Interview Core:** KMP increments the pattern-building index immediately after falling back on mismatch. Repair LPS construction.
11. **Interview Core:** Rolling hash returns a match whenever two hashes are equal. Preserve correctness under collision.
12. **SDE-2 Follow-up:** A service normalizes input and returns the resulting index as an index into the original string. Redesign the result contract or retain a mapping.

## D. Focused Coding Tasks

1. **Foundation:** Count occurrences of an ASCII unit in a non-null string.
2. **Foundation:** Return the first UTF-16 index where two nullable strings differ, with an explicit null ordering.
3. **Foundation:** Reverse words while collapsing ASCII spaces and producing no leading/trailing space.
4. **Foundation:** Validate the grammar `letter (letter | digit | '_')*` for ASCII identifiers.
5. **Foundation:** Parse a strict signed decimal `int` and detect overflow before arithmetic exceeds range.
6. **Foundation:** Return all code points and their starting UTF-16 indexes.
7. **Interview Core:** Check an ASCII phrase palindrome while ignoring punctuation and case.
8. **Interview Core:** Decide whether a UTF-16 string can become a palindrome after deleting at most one unit.
9. **Interview Core:** Group lowercase English anagrams while preserving first-group and input order.
10. **Interview Core:** Return the longest common prefix without sorting or modifying input.
11. **Interview Core:** Encode consecutive runs and write a decoder for the explicitly defined grammar.
12. **Interview Core:** Find all lowercase anagram starting indexes with a rolling frequency difference.
13. **Interview Core:** Return the earliest longest ASCII substring with no repeated unit.
14. **Interview Core:** Count substrings with exactly `k` distinct UTF-16 units using `long`.
15. **Interview Core:** Return the earliest shortest ASCII window covering a target multiset.
16. **Interview Core:** Implement KMP first-match and all-match search, including overlaps.
17. **SDE-2 Follow-up:** Implement verified Rabin-Karp with safe modular arithmetic and adversarial collision tests.
18. **SDE-2 Follow-up:** Differential-test optimized minimum cover or KMP against a brute-force baseline with a fixed random seed.

## E. Interview Follow-ups

1. **Interview Core:** When would the simple `String.indexOf` call be preferable to KMP?
2. **Interview Core:** Compare sorting and frequency state for anagram detection.
3. **Interview Core:** Compare `char[]`, `StringBuilder`, and an `int[]` of code points for reversal.
4. **Interview Core:** Why does a sliding-window solution require monotonic repair rather than merely two pointers?
5. **SDE-2 Follow-up:** How would result positions and state change for code-point matching?
6. **SDE-2 Follow-up:** How would you search one pattern across decoded stream chunks without retaining the full text?
7. **SDE-2 Follow-up:** What input, output, logging, regex, and cache limits belong around a public text API?
8. **SDE-2 Follow-up:** When do many-pattern or approximate-search workloads require a different data structure or algorithm family?

## F. Cumulative Assessments

### F1 - SDE-1 Foundation Gate

In 30 minutes, implement a text utility with `isBlankOrNull`, literal delimiter splitting that preserves trailing fields, strict integer parsing, and code-point-safe reversal. Explain immutability, equality, every failure policy, and the output of five boundary tests.

### F2 - Interview Core Pattern Gate

In 40 minutes, solve longest unique substring and minimum covering substring. For each, give a brute-force baseline, interval convention, invariant, dry run, complete complexity, and a test that breaks a common incorrect solution.

### F3 - Search and Unicode Gate

In 45 minutes, derive LPS for `ababaca`, implement KMP all-match search, and explain how normalization and code-point matching would affect original UTF-16 result indexes. Add a deterministic differential test against naive search.

## G. Final Readiness Assessment

**SDE-2 Readiness:** Design and implement a bounded text-search component that accepts one pattern and many text chunks. The contract must define charset decoding, malformed bytes, case/normalization, empty pattern, match position units, overlapping matches, maximum retained matches, and error behavior. Provide a correct in-memory reference implementation, a streaming-state design, invariant and complexity proof, failure-focused tests, observability without sensitive-text logging, and trade-offs among naive search, KMP, rolling hash, and a prebuilt index.
