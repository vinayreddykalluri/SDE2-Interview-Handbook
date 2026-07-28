# SDE-2 String Interview Playbook

An SDE-2 string answer is more than a correct loop. It exposes the text contract, derives the algorithm from constraints, proves the state transition, uses Java precisely, tests failures, and explains how production boundaries would change the design.

This chapter turns the previous mechanics into a repeatable interview workflow.

![String pattern decision map](content/volumes/07-strings-and-string-patterns/assets/10-string-pattern-decision-map.png)

## 7.1 The seven-question contract

Before coding, ask or state:

1. **Absence:** Can input be null? Is an empty pattern/input valid?
2. **Text unit:** ASCII, lowercase English letters, UTF-16 units, code points, grapheme clusters, or encoded bytes?
3. **Equality:** Case-sensitive? Locale-sensitive? Normalized? Are punctuation and whitespace significant?
4. **Output:** Value, UTF-16 indexes, code-point positions, count, boolean, or all matches?
5. **Mutation/ownership:** May input be copied or normalized? Must original spelling be preserved?
6. **Scale:** Maximum lengths, alphabet size, number of queries, streaming, and memory limits?
7. **Tie and failure policy:** Earliest/shortest/lexicographically smallest? Reject invalid input or return a sentinel?

If the interviewer says "assume lowercase English letters," use that fact. It justifies a 26-slot array and avoids pretending a `char` algorithm is fully Unicode-aware.

## 7.2 Recognition map

| Wording or constraint | Baseline | Candidate pattern | Proof obligation |
|---|---|---|---|
| reverse/mirror/palindrome | create reverse | opposing pointers | outside range already matches |
| contiguous substring of size `k` | recompute every range | fixed window | entering/leaving update is exact |
| longest/shortest valid substring | start at every position | variable window | validity has monotonic repair |
| same letters, any order | sort | frequency state | signature matches equality contract |
| first/all pattern occurrences | align and compare | KMP/Z/hash | preprocessing never skips a match |
| many repeated prefixes | compare independently | trie/index | workload repays retained structure |
| build transformed output | repeated `+` | builder or output array | output order and escaping are correct |
| parse structured text | split | direct scanner/state machine | grammar and failure position are defined |

Start with the simplest baseline that makes correctness visible. Optimize because constraints eliminate it, not because a pattern name appears familiar.

## 7.3 A five-part solution explanation

Use this structure while coding:

1. **Contract:** "The input is non-null ASCII; indexes are UTF-16 indexes, equivalent here to ASCII positions."
2. **Baseline:** "Starting from every index and rebuilding frequency costs quadratic time."
3. **State/invariant:** "The current half-open window has no duplicate unit; the last-position table gives the next valid left boundary."
4. **Transitions/progress:** "Right advances once per unit. Left only moves forward."
5. **Cost and trade-offs:** "Time is linear, auxiliary state is fixed for ASCII, and returning a substring adds output allocation."

This is more convincing than naming "sliding window" without defining what slides or why.

## 7.4 Weak and strong Java

Weak code hides the contract and performs repeated work:

```java
String f(String s) {
    String x = "";
    for (int i = s.length() - 1; i >= 0; i--) {
        x += s.charAt(i);
    }
    return x;
}
```

Improved under an explicit UTF-16-unit contract:

```java
static String reverseUtf16Units(String text) {
    if (text == null) {
        throw new IllegalArgumentException("text must not be null");
    }
    char[] units = text.toCharArray();
    for (int left = 0, right = units.length - 1; left < right; left++, right--) {
        char temporary = units[left];
        units[left] = units[right];
        units[right] = temporary;
    }
    return new String(units);
}
```

The improved version names the method, declares null behavior, avoids growing concatenation, and states its limitation. If the requirement is code-point reversal, use the Chapter 2 implementation instead.

## 7.5 Invariant templates worth knowing

Do not memorize full solutions. Memorize the questions that produce invariants:

### Opposing pointers

```text
Everything outside [left, right] has been verified or placed correctly.
```

### Fixed window

```text
State describes exactly the k units ending at the current right boundary.
```

### Variable valid window

```text
After repair, [left, right] satisfies the predicate.
Every removed start can no longer produce a better valid candidate for this right.
```

### Frequency difference

```text
Each slot equals required count minus current-window count.
```

### KMP

```text
pattern[0..matched) equals the matched suffix immediately before textIndex.
```

An invariant must be true initially, preserved by each transition, and strong enough to imply the result at termination.

## 7.6 Complexity language that survives follow-ups

Avoid incomplete claims:

Weak:

```text
This is O(n).
```

Strong:

```text
Let n be text UTF-16 units and m be pattern units. KMP preprocessing is O(m),
search is O(n), and the LPS array is O(m) auxiliary space. Returning all k
matches adds O(k) output space. No input copy or normalization is performed.
```

For maps, say expected complexity unless the data structure provides a deterministic guarantee. For builders, describe amortized construction. For normalization, encoding, substring output, and code-point arrays, include allocation.

## 7.7 Failure-focused tests

Create tests by attacking assumptions:

| Dimension | Cases |
|---|---|
| size | null if allowed, empty, one unit, very long |
| relation | no match, full match, prefix, suffix, overlapping matches |
| multiplicity | all same, all distinct, one count short, surplus counts |
| boundaries | answer at index 0, at final alignment, entire input |
| text | spaces, punctuation, digits, mixed case, supplementary code point, combining mark |
| numeric | number of substrings beyond `int`, parser min/max, overflow by one |
| ownership | original text retained, normalized copy, returned builder not leaked |
| performance | repetitive prefixes, large alphabet, many outputs, collision candidate |

Randomized differential testing is powerful. Compare an optimized window or search method against a small brute-force baseline over thousands of generated short inputs.

```java
static void verifyKmpAgainstNaive(java.util.Random random) {
    for (int test = 0; test < 5_000; test++) {
        String text = randomLowercase(random, random.nextInt(20));
        String pattern = randomLowercase(random, random.nextInt(8));
        int expected = naiveSearch(text, pattern);
        int actual = kmpSearch(text, pattern);
        if (expected != actual) {
            throw new AssertionError(text + " / " + pattern);
        }
    }
}
```

Use a fixed seed so failure is reproducible.

## 7.8 Java traps interviewers revisit

### `==` versus `.equals()`

Identity is not content. Pool behavior does not change the rule.

### Ignored immutable result

```java
String text = " java ";
text.strip();
System.out.println(text); // still includes spaces
```

Assign the returned value when transformation is required.

### `split` and regex

`split("|")`, `split(".")`, and `split("+")` do not mean literal delimiters. Quote or escape them.

### Index unit mismatch

`charAt`, `substring`, and `indexOf` use UTF-16 indexes. An `int[]` from `codePoints()` uses code-point positions.

### Builder ownership

`StringBuilder` is mutable and not thread-safe. Returning it lets callers change later state. Publish an immutable String unless mutability is intentionally part of the API.

### Comparator and case

Lexicographic `compareTo`, case-insensitive comparison, locale collation, and normalized equality are different contracts. Do not substitute one for another.

### Hash and equality

String keys are immutable and provide value-based equality/hash behavior. A custom wrapper key must preserve the same stable contract. The Hashing book covers it in depth.

## 7.9 Production follow-ups

Interviewers at SDE-2 may ask how the in-memory method changes in a service.

### Input limits

Linear work can still be a denial-of-service risk for unbounded strings or unbounded result sets. Limit payload length, token count, pattern length, and collected matches.

### Sensitive text

Do not log authentication tokens, full request bodies, or personal information while debugging parsing failures. Redact at the boundary and keep error messages useful without echoing secrets.

### Unicode security

Visually similar code points, mixed scripts, normalization, and case folding can create identifier confusion. Security-sensitive identity rules need a deliberate policy and often dedicated libraries. "Lowercase it" is not a security design.

### Regex safety

Complex regexes can exhibit expensive backtracking. Prefer bounded direct parsing for simple grammars, precompile repeated patterns, and treat untrusted patterns as executable work.

### Streaming

A huge file should not become one String merely to search it. Decode bytes incrementally, preserve incomplete byte sequences at chunk boundaries, carry algorithm state, and define positions in bytes or decoded units.

### Caching

Caching normalized or preprocessed text trades memory and invalidation complexity for repeated-query speed. Measure the workload and bound the cache. Do not retain arbitrary user input indefinitely.

## 7.10 Advanced topics deliberately deferred

This book establishes the language and core pattern foundation. It intentionally cross-references:

- hash table internals, equality contracts, and prefix counts -> **Hashing**;
- tries and suffix-related tree structures -> **Trees and Advanced DSA**;
- edit distance, interleaving, and longest common subsequence -> **Dynamic Programming**;
- recursive generation of partitions/permutations -> **Recursion and Backtracking**;
- regex engines, streams, collectors, and advanced text APIs -> **Advanced Java**;
- charsets, decoders, channels, and files -> **Java I/O and NIO**;
- memory layout and compact-string implementation details -> **JVM Internals**; and
- distributed search, indexes, and caching -> **System Design**.

Cross-reference instead of duplicating those books. You should now be prepared to read them without a missing String foundation.

## 7.11 A 35-minute interview pacing model

| Time | Candidate action |
|---:|---|
| 0-4 min | clarify contract, examples, scale, and output |
| 4-8 min | state baseline and why constraints reject or accept it |
| 8-12 min | derive state, invariant, and interval convention |
| 12-24 min | implement readable Java while narrating boundaries |
| 24-29 min | dry-run normal and adversarial examples |
| 29-33 min | give complete time/space/output analysis |
| 33-35 min | answer production and alternative-design follow-ups |

Do not rush into KMP or a complex window in minute one. A correct, clear baseline plus justified optimization is stronger than unexplained advanced code.

## 7.12 Readiness checklist

You are ready to move beyond this volume when you can:

- explain immutability, references, pooling, equality, and pass-by-value accurately;
- distinguish null, empty, and blank;
- use core String and builder APIs without indexing or regex mistakes;
- choose UTF-16, code-point, grapheme, or byte units deliberately;
- implement and prove palindrome, frequency, fixed-window, variable-window, and minimum-cover patterns;
- derive and implement KMP after building its prefix table by hand;
- compare standard API, baseline, KMP, hash, and Z choices;
- state complete complexity including preprocessing, output, maps, and allocations;
- construct failure-focused and differential tests; and
- discuss input limits, Unicode policy, logging, regex, streaming, and ownership.

## 7.13 Final local practice

**Foundation:** Explain `String` immutability to a beginner using a variable/reference/object diagram and a runnable example.

**Interview Core:** Solve minimum covering substring from the baseline, state the validity invariant, and dry-run it on a case with duplicate required units.

**SDE-2 Follow-up:** Compare a KMP streaming matcher, a rolling-hash document scan, and a prebuilt search index for three different workloads. State consistency, memory, result-position, Unicode, and operational trade-offs.
