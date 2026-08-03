# Pattern Matching and Search

Substring search asks where a pattern occurs inside text. Java provides `indexOf`, and it is usually the correct production default when no algorithmic guarantee is requested. Interviews often ask candidates to derive a baseline and then remove repeated comparisons with KMP, rolling hash, or the Z algorithm.

The goal is not to memorize three templates. It is to understand what information each preprocessing method preserves.

## 6.1 Search contract

Before implementation, clarify:

- first match, all matches, or only existence;
- whether overlapping matches count;
- result unit: Java UTF-16 index or another position;
- behavior for an empty pattern;
- case and normalization policy;
- expected input size and repeated-query workload; and
- whether a deterministic worst-case bound is required.

This volume uses the common Java contract: return the first UTF-16 starting index, return `-1` when absent, and return `0` for an empty pattern.

## 6.2 Java API baseline

```java
static int findWithApi(String text, String pattern) {
    return text.indexOf(pattern);
}
```

This is readable and battle-tested. The Java API does not promise a particular search algorithm or universal worst-case complexity. Use explicit KMP when the interview requires you to demonstrate a linear worst-case bound.

## 6.3 Naive search

Align the pattern at every possible start and compare until mismatch:

```java
static int naiveSearch(String text, String pattern) {
    if (pattern.isEmpty()) {
        return 0;
    }
    if (pattern.length() > text.length()) {
        return -1;
    }
    int lastStart = text.length() - pattern.length();
    for (int start = 0; start <= lastStart; start++) {
        int matched = 0;
        while (matched < pattern.length()
                && text.charAt(start + matched) == pattern.charAt(matched)) {
            matched++;
        }
        if (matched == pattern.length()) {
            return start;
        }
    }
    return -1;
}
```

Correctness is direct: every feasible alignment is tested in order. Worst-case time is `O((n - m + 1)m)`, often shortened to `O(nm)`, and auxiliary space is `O(1)`.

The baseline is often enough for small inputs or short patterns. Constraints decide whether preprocessing is justified.

## 6.4 Why naive search repeats work

Consider a long text of `a` units and pattern `aaaaab`. Each alignment matches many `a` units before failing at `b`, then the next alignment compares much of the same prefix again.

KMP preserves the answer to this question:

> After matching a prefix of the pattern, what shorter prefix is already known to match the suffix ending here?

That overlap lets the pattern fall back without moving the text index backward.

## 6.5 The LPS or prefix table

For every pattern position `i`, `lps[i]` is the length of the longest **proper** prefix of `pattern[0..i]` that is also a suffix of that same range. Proper means shorter than the complete range.

For `ababaca`:

```text
index:   0 1 2 3 4 5 6
pattern: a b a b a c a
lps:     0 0 1 2 3 0 1
```

![KMP prefix table and fallback](content/volumes/dsa/DSA-07-strings-and-string-patterns/assets/09-kmp-prefix-fallback.png)

Build it with the same fallback idea used during search:

```java
static int[] buildLps(String pattern) {
    int[] lps = new int[pattern.length()];
    int prefixLength = 0;

    for (int index = 1; index < pattern.length();) {
        if (pattern.charAt(index) == pattern.charAt(prefixLength)) {
            lps[index++] = ++prefixLength;
        } else if (prefixLength > 0) {
            prefixLength = lps[prefixLength - 1];
        } else {
            lps[index++] = 0;
        }
    }
    return lps;
}
```

Important detail: after mismatch with a nonzero `prefixLength`, do not advance `index`. The shorter candidate prefix must be compared against the same current unit.

### LPS dry run around the mismatch

For `ababaca`, indexes 0 through 4 establish prefix length 3 at the `a` ending `ababa`. At index 5, `c` does not match the next expected `b`:

1. fall from length 3 to `lps[2] = 1`;
2. compare `c` with pattern index 1 (`b`), still mismatch;
3. fall from 1 to `lps[0] = 0`;
4. compare `c` with pattern index 0 (`a`), mismatch; and
5. record zero and advance.

No already proven prefix-suffix candidate is skipped.

## 6.6 KMP first-match search

```java
static int kmpSearch(String text, String pattern) {
    if (pattern.isEmpty()) {
        return 0;
    }
    int[] lps = buildLps(pattern);
    int textIndex = 0;
    int matched = 0;

    while (textIndex < text.length()) {
        if (text.charAt(textIndex) == pattern.charAt(matched)) {
            textIndex++;
            matched++;
            if (matched == pattern.length()) {
                return textIndex - matched;
            }
        } else if (matched > 0) {
            matched = lps[matched - 1];
        } else {
            textIndex++;
        }
    }
    return -1;
}
```

Invariant: `pattern[0..matched)` equals the `matched` UTF-16 units immediately before `textIndex`. On mismatch, LPS identifies the longest smaller prefix that could still be a suffix of those already matched text units.

Time is `O(n + m)` and auxiliary space `O(m)`. The text index never moves backward; `matched` fallbacks are amortized against earlier increases.

## 6.7 All matches, including overlaps

After a full match, record its start and fall back to allow overlap:

```java
static java.util.List<Integer> kmpAllMatches(String text, String pattern) {
    java.util.List<Integer> matches = new java.util.ArrayList<>();
    if (pattern.isEmpty()) {
        for (int index = 0; index <= text.length(); index++) {
            matches.add(index);
        }
        return matches;
    }

    int[] lps = buildLps(pattern);
    int textIndex = 0;
    int matched = 0;
    while (textIndex < text.length()) {
        if (text.charAt(textIndex) == pattern.charAt(matched)) {
            textIndex++;
            matched++;
            if (matched == pattern.length()) {
                matches.add(textIndex - matched);
                matched = lps[matched - 1];
            }
        } else if (matched > 0) {
            matched = lps[matched - 1];
        } else {
            textIndex++;
        }
    }
    return matches;
}
```

For text `aaaa` and pattern `aa`, the result is `[0, 1, 2]`. Resetting `matched` to zero after a match would lose overlaps.

The empty-pattern contract above returns every boundary `0..length`. That is defensible but potentially large. A production API might reject an empty pattern for all-match queries.

## 6.8 Rolling hash and Rabin-Karp

Rolling hash represents a fixed-length window numerically so the next window hash can be updated without rehashing every unit. Equal hashes are only candidates; collisions are possible.

```java
static int rabinKarpSearchAscii(String text, String pattern) {
    if (pattern.isEmpty()) {
        return 0;
    }
    if (pattern.length() > text.length()) {
        return -1;
    }
    final long base = 257;
    final long modulus = 1_000_000_007L;
    long highestPower = 1;
    long patternHash = 0;
    long windowHash = 0;

    for (int index = 0; index < pattern.length(); index++) {
        requireAscii(pattern.charAt(index));
        requireAscii(text.charAt(index));
        patternHash = (patternHash * base + pattern.charAt(index)) % modulus;
        windowHash = (windowHash * base + text.charAt(index)) % modulus;
        if (index + 1 < pattern.length()) {
            highestPower = highestPower * base % modulus;
        }
    }

    for (int start = 0; start <= text.length() - pattern.length(); start++) {
        if (windowHash == patternHash && text.regionMatches(start, pattern, 0, pattern.length())) {
            return start;
        }
        if (start < text.length() - pattern.length()) {
            long outgoing = text.charAt(start) * highestPower % modulus;
            windowHash = (windowHash - outgoing + modulus) % modulus;
            char incoming = requireAscii(text.charAt(start + pattern.length()));
            windowHash = (windowHash * base + incoming) % modulus;
        }
    }
    return -1;
}
```

The explicit `regionMatches` verification makes collisions affect performance, not correctness. Expected time is near `O(n + m)` under a well-distributed hash; worst case can be `O(nm)` if many windows collide and require verification. Auxiliary space is `O(1)`.

Use `long` for multiplication before the modulus. Even `long` can overflow for arbitrary parameters, so production hashing requires carefully selected arithmetic or library-quality implementation.

## 6.9 Z algorithm intuition

For a sequence `s`, `z[i]` is the length of the longest substring starting at `i` that matches a prefix of `s`. Pattern search can construct:

```text
pattern + separator-not-in-input + text
```

Any position with Z value at least `pattern.length()` begins a match. A maintained `[left, right]` Z-box reuses earlier prefix comparisons, yielding `O(n + m)` time and `O(n + m)` state for the combined input.

KMP asks, "How much matched prefix can survive a mismatch?" Z asks, "How much of the global prefix begins here?" Both reuse prefix structure. KMP is often easier for streaming text because it only stores pattern preprocessing. Z is useful when prefix-match lengths at many positions are themselves part of the problem.

Do not concatenate with an assumed separator unless its absence is guaranteed. A safer implementation can work over an integer sequence with a sentinel outside the input domain or handle pattern and text indices without a literal separator.

## 6.10 Search choice table

| Situation | Good starting choice | Reason |
|---|---|---|
| ordinary Java application, first match | `String.indexOf` | clear standard API |
| small constraints, explainable baseline | naive | minimal state and direct proof |
| deterministic linear worst case | KMP | `O(n + m)`, pattern-only preprocessing |
| many fixed-size window comparisons | rolling hash | constant-time hash update, verify collisions |
| prefix-match length needed at every position | Z | exposes full prefix-overlap array |
| many dictionary prefixes | trie or specialized index | different workload; see Trees/Advanced DSA |
| approximate/edit-distance match | dynamic programming or specialized search | exact-match preprocessing is insufficient |

## 6.11 Repeated queries and streaming

Workload changes the design:

- one pattern, one text: preprocessing may not repay for tiny input;
- one pattern, many text chunks: keep KMP's `matched` state across chunk boundaries;
- many patterns, one text: repeated KMP may be insufficient; a multi-pattern automaton is an advanced follow-up;
- many static documents: build an index outside the request path; and
- unbounded stream: cap retained output and avoid collecting every empty-pattern boundary.

SDE-2 reasoning includes lifecycle and memory, not only the core loop.

## 6.12 Unicode boundary

All implementations above return UTF-16 indexes because they use `charAt`. That matches `String.indexOf`. If equality is defined after normalization or over code points:

1. decide whether to transform the input;
2. retain an index map if original positions are needed;
3. state whether equivalent normalized forms are matches; and
4. define how case and locale behave.

Normalization can change length, so a normalized-match index is not automatically an original-string index.

## 6.13 Common failures

- leaving empty-pattern behavior undefined;
- using `start < text.length() - pattern.length()` and skipping the last alignment;
- advancing the LPS index after a mismatch that still has a fallback;
- setting KMP `matched` to zero and losing overlapping matches;
- saying rolling hash is collision-free;
- returning a hash match without verifying content;
- multiplying in `int` before assigning to `long`;
- choosing a separator that can occur in input;
- promising complexity for `indexOf` that the API does not guarantee; and
- returning transformed or code-point positions as original UTF-16 indexes.

## 6.14 Quick check and practice

1. What repeated work makes naive search quadratic in the worst case?
2. Define `lps[i]` precisely, including "proper."
3. Why does LPS construction sometimes change prefix length without advancing the index?
4. How does KMP preserve overlapping matches?
5. Why must rolling-hash matches be verified?
6. When is Z more informative than a first-match KMP result?

**Foundation:** Implement naive all-match search, including overlaps, and define the empty-pattern result.

**Interview Core:** Implement KMP from memory after deriving the LPS array for `aabaaab` by hand.

**SDE-2 Follow-up:** Design a service that searches one pattern across streamed UTF-8 chunks. Explain decoder boundaries, retained KMP state, result position units, normalization, backpressure, and maximum output.
