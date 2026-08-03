# String Contracts, Live Interviews, and Edge Cases

> **A note from Vinay:** Before solving a string problem, ask what one “character” means. A Java `char`, a Unicode code point, and what a user sees on screen are not always the same unit. The correct unit changes both the algorithm and its indexes.

## 1. Choose the text unit before the data structure

| Contract | Java representation | Typical interview use |
|---|---|---|
| ASCII-only symbol | `char` or bounded array | explicit ASCII problems |
| UTF-16 code unit | `char` | APIs whose indexes are Java string indexes |
| Unicode code point | `int` from `codePoints()` | emoji and supplementary characters |
| User-perceived character | grapheme-cluster boundary logic | UI editing, display truncation, human text |

Do not silently promise Unicode correctness while iterating `char` values. Conversely, do not add grapheme-cluster machinery to a stated lowercase-English interview problem.

## 2. Manual integer parsing contract

A robust parser must define:

- whether leading/trailing whitespace is accepted;
- whether `+` and `-` are accepted;
- whether an empty digit sequence is invalid;
- how overflow is reported;
- whether non-ASCII decimal digits are accepted.

Accumulating the result as a negative number makes `Integer.MIN_VALUE` representable during parsing. Before appending digit `d`, check whether `result < (limit + d) / 10`, using the negative limit for the selected sign. The companion demonstrates the complete rule and differential-tests accepted inputs against `Integer.parseInt`.

## 3. KMP prefix table as reusable state

For pattern `ababaca`, the prefix table is:

```text
index:   0 1 2 3 4 5 6
char:    a b a b a c a
prefix:  0 0 1 2 3 0 1
```

At a mismatch after matching `matched` characters, KMP does not restart from zero. It reuses the longest proper prefix that is also a suffix: `matched = prefix[matched - 1]`. The text index does not move backward.

The invariant is: before comparing the next text character, `matched` is the length of the pattern prefix equal to the suffix ending immediately before that text position.

## 4. Equality, normalization, and locale

`equals` compares the stored character sequence. It does not perform Unicode normalization or locale-aware human collation.

```text
"é"                 one code point U+00E9
"e" + combining mark two code points U+0065 U+0301
```

They can render similarly while `equals` is false. Normalize only when the application contract requires it. For identifiers and protocol tokens, use a defined locale-independent rule; avoid relying on the machine's default locale.

## 5. Edge and failure matrix

| Case | Typical bug | Correct policy |
|---|---|---|
| `null` | call `text.isEmpty()` | reject, return optional/boolean, or define null semantics |
| empty pattern | inconsistent search answer | state whether it matches at index zero |
| supplementary code point | split surrogate pair with `charAt` | iterate code points when contract requires it |
| combining marks | treat code point as visible character | use grapheme logic for user-facing boundaries |
| case conversion | use default locale for identifiers | use `Locale.ROOT` or explicit contract |
| parsing `MIN_VALUE` | accumulate positive magnitude in `int` | negative accumulation or widened arithmetic |
| repeated concatenation | build immutable prefixes in a loop | use `StringBuilder` when accumulating |
| `split` | forget regex semantics | quote literal delimiters or parse manually |
| substring/window index | mix code-point and UTF-16 indexes | document and convert the index unit |
| rolling hash | treat matching hashes as proof | verify candidate content or use collision-free logic |

## 6. Six live interview rounds

### Round 1 - Valid palindrome

**Interviewer:** Ignore non-alphanumeric characters and case.

**Candidate opening:** I will clarify whether the input is ASCII or general Unicode and whether the returned positions matter.

**Model answer:** Under an ASCII contract, two UTF-16 indexes and `Character` predicates are sufficient. For general code points, move by `codePointAt` and `codePointBefore`, normalize case with a defined policy, and compare code points. Time is `O(n)` in code units and auxiliary space is `O(1)` if no normalized copy is required.

### Round 2 - Parse a signed integer

**Model answer:** Validate optional sign and at least one digit, accumulate while checking the boundary before multiplication/addition, and reject trailing invalid data according to the contract. Do not parse into `long` unless the problem explicitly permits using a wider type as the strategy.

**Follow-up:** `Integer.MIN_VALUE` has no positive `int` magnitude, so negative accumulation avoids a special overflow hole.

### Round 3 - Longest substring without repeats

**Candidate opening:** The window contains no repeated symbol, and `left` is the smallest legal start after processing each right endpoint.

**Model answer:** Store the next legal index after the last occurrence. Update `left = max(left, lastSeen + 1)` so an old occurrence behind the current window never moves `left` backward. Time is expected `O(n)` with a map and space is `O(u)`.

### Round 4 - Minimum covering window

**Model answer:** Track required frequencies, window frequencies, and how many distinct requirements are currently satisfied. Expand until legal, then shrink while preserving legality. Duplicates in the target matter; a set is insufficient.

### Round 5 - KMP search

**Candidate opening:** I will preprocess how much valid prefix state survives a mismatch, so the text pointer never moves backward.

**Model answer:** Prefix construction is `O(m)` and scanning is `O(n)`, giving `O(n + m)` time and `O(m)` space. Empty-pattern behavior must be stated explicitly.

### Round 6 - Production text boundary

**Interviewer:** Can `text.length()` be used as the number of displayed characters?

**Model answer:** Not generally. It returns UTF-16 code units. `codePointCount` handles supplementary code points but still does not count grapheme clusters. UI truncation should use a grapheme-aware boundary mechanism; interview ASCII problems can retain the simpler contract when stated.

## 7. Rapid interviewer questions

1. **`==` versus `equals` for strings?** Identity versus content.
2. **Does every equal literal imply one universal string object?** No; literals may share pooled instances, but do not base value logic on identity.
3. **Why is `String` immutable?** Its observable value cannot be changed; operations return another string.
4. **Is `StringBuilder` thread-safe?** No.
5. **When is `StringBuffer` relevant?** Only when its synchronized mutable-buffer contract is specifically useful; it is not an automatic default.
6. **What does `substring` cost?** Use the selected Java version's behavior; modern Java creates an independent result rather than retaining the original backing array.
7. **Why can `toLowerCase()` change length?** Unicode case mappings are not always one code unit to one code unit.
8. **Why can comparator subtraction fail?** The subtraction can overflow; use `Integer.compare` or a proper comparator chain.
9. **Why seed prefix-state frequency?** It represents the empty prefix and counts ranges beginning at index zero.
10. **Can a rolling-hash match be accepted without checking?** Only if collision risk is part of the accepted contract; otherwise verify.
11. **What is the cost of `StringBuilder.toString()`?** It creates the immutable result and should be counted when output size matters.
12. **What should a string answer state?** Null policy, text unit, case/normalization policy, output-index unit, and complexity including created substrings.

## 8. Executable evidence

`StringContractChecks.java` implements overflow-safe signed parsing, KMP search, and code-point reversal. It compares the manual parser and search behavior against matching JDK contracts over deterministic cases while keeping intentionally different contracts explicit.
