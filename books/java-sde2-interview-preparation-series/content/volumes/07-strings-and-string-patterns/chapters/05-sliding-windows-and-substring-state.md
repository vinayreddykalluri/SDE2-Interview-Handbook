# Sliding Windows and Substring State

A substring is contiguous. Neighboring substrings overlap. Sliding windows exploit that overlap by updating state when one unit enters or leaves instead of recomputing the whole range.

The technique is not "use two indexes." A correct variable window needs a validity rule whose repair direction is monotonic. This chapter builds fixed windows first, then variable windows, then SDE-2 frequency-state variations.

## 5.1 Vocabulary and interval convention

Use a half-open window `[left, right)` whenever practical:

```text
contained indexes: left, left + 1, ..., right - 1
length:            right - left
empty window:      left == right
```

Some loops process an entering unit at inclusive `right`. Both forms are valid, but the code and invariant must agree. This chapter names the interval beside each implementation.

## 5.2 Baseline before optimization

For the longest substring without repeated units, a baseline starts at every index and extends until a repeat:

```java
static int longestUniqueQuadratic(String text) {
    int best = 0;
    for (int start = 0; start < text.length(); start++) {
        boolean[] seen = new boolean[128];
        for (int end = start; end < text.length(); end++) {
            char unit = text.charAt(end);
            if (unit >= 128) {
                throw new IllegalArgumentException("ASCII required");
            }
            if (seen[unit]) {
                break;
            }
            seen[unit] = true;
            best = Math.max(best, end - start + 1);
        }
    }
    return best;
}
```

Worst-case time is `O(n^2)`: a distinct suffix can be rescanned from many starts. The baseline establishes the exact validity condition: no unit occurs twice inside the candidate.

## 5.3 Fixed-size window

Question: return the maximum number of vowels in any substring of exactly `k` ASCII units.

```java
static int maximumVowels(String text, int k) {
    if (k < 0 || k > text.length()) {
        throw new IllegalArgumentException("k must be in [0, length]");
    }
    int current = 0;
    for (int index = 0; index < k; index++) {
        current += isAsciiVowel(text.charAt(index)) ? 1 : 0;
    }
    int best = current;
    for (int right = k; right < text.length(); right++) {
        current += isAsciiVowel(text.charAt(right)) ? 1 : 0;
        current -= isAsciiVowel(text.charAt(right - k)) ? 1 : 0;
        best = Math.max(best, current);
    }
    return best;
}

static boolean isAsciiVowel(char unit) {
    return unit == 'a' || unit == 'e' || unit == 'i'
            || unit == 'o' || unit == 'u';
}
```

The first loop initializes `[0, k)`. At each later `right`, one unit enters and the unit at `right - k` leaves. Each unit is processed a constant number of times: `O(n)` time and `O(1)` auxiliary space.

Edge contracts matter: this implementation returns zero for `k == 0` and rejects a window larger than the text.

## 5.4 Longest unique window with counts

Use an inclusive-right loop and an ASCII count array:

```java
static int longestUniqueAscii(String text) {
    int[] frequency = new int[128];
    int left = 0;
    int best = 0;

    for (int right = 0; right < text.length(); right++) {
        char entered = text.charAt(right);
        if (entered >= 128) {
            throw new IllegalArgumentException("ASCII required");
        }
        frequency[entered]++;

        while (frequency[entered] > 1) {
            frequency[text.charAt(left)]--;
            left++;
        }
        best = Math.max(best, right - left + 1);
    }
    return best;
}
```

Invariant after the repair loop: substring `[left, right]` contains no duplicate ASCII unit. Only the newly entered unit can have become duplicated, so repairing until its count is one is sufficient.

Although a `while` appears inside a `for`, work is `O(n)`, not `O(n^2)`. `right` moves forward `n` times; `left` also moves forward at most `n` times.

## 5.5 Longest unique window with last positions

Store the next valid left boundary after each observed unit:

```java
static int longestUniqueAsciiWithJumps(String text) {
    int[] nextAfterLast = new int[128];
    java.util.Arrays.fill(nextAfterLast, -1);
    int left = 0;
    int best = 0;

    for (int right = 0; right < text.length(); right++) {
        char unit = text.charAt(right);
        if (unit >= 128) {
            throw new IllegalArgumentException("ASCII required");
        }
        left = Math.max(left, nextAfterLast[unit]);
        best = Math.max(best, right - left + 1);
        nextAfterLast[unit] = right + 1;
    }
    return best;
}
```

![Longest unique substring window and boundary jump](content/volumes/07-strings-and-string-patterns/assets/07-longest-unique-window.png)

Why `Math.max`? A previous occurrence may be outside the current window. Moving `left` backward would reintroduce invalid state.

For `"abba"`:

| `right` | unit | previous next boundary | `left` | current length | best |
|---:|---|---:|---:|---:|---:|
| 0 | a | -1 | 0 | 1 | 1 |
| 1 | b | -1 | 0 | 2 | 2 |
| 2 | b | 2 | 2 | 1 | 2 |
| 3 | a | 1 | 2, not 1 | 2 | 2 |

## 5.6 Returning the substring, not only its length

Preserve the best boundary:

```java
static String longestUniqueSubstringAscii(String text) {
    int[] nextAfterLast = new int[128];
    java.util.Arrays.fill(nextAfterLast, -1);
    int left = 0;
    int bestStart = 0;
    int bestLength = 0;

    for (int right = 0; right < text.length(); right++) {
        char unit = text.charAt(right);
        if (unit >= 128) {
            throw new IllegalArgumentException("ASCII required");
        }
        left = Math.max(left, nextAfterLast[unit]);
        int length = right - left + 1;
        if (length > bestLength) {
            bestStart = left;
            bestLength = length;
        }
        nextAfterLast[unit] = right + 1;
    }
    return text.substring(bestStart, bestStart + bestLength);
}
```

This tie policy keeps the earliest maximum because it updates only for a strictly larger length. Say that aloud. Returning a substring adds `O(bestLength)` output allocation on current Java.

## 5.7 At most `k` distinct units

```java
static int longestWithAtMostKDistinct(String text, int k) {
    if (k < 0) {
        throw new IllegalArgumentException("k must be nonnegative");
    }
    java.util.Map<Character, Integer> frequency = new java.util.HashMap<>();
    int left = 0;
    int best = 0;

    for (int right = 0; right < text.length(); right++) {
        char entered = text.charAt(right);
        frequency.merge(entered, 1, Integer::sum);

        while (frequency.size() > k) {
            char removed = text.charAt(left++);
            int next = frequency.get(removed) - 1;
            if (next == 0) {
                frequency.remove(removed);
            } else {
                frequency.put(removed, next);
            }
        }
        best = Math.max(best, right - left + 1);
    }
    return best;
}
```

Invariant: the current window contains at most `k` distinct units after repair. Removing from the left can never increase the number of distinct units, so the repair direction is monotonic.

Expected time is `O(n)` with hash-map operations; auxiliary space is `O(min(n, alphabetSize))`. If the alphabet is known and bounded, an array can reduce overhead.

## 5.8 Exactly `k` distinct: two formulations

For **counting substrings**, a useful identity is:

```text
exactly(k) = atMost(k) - atMost(k - 1)
```

Each right endpoint contributes the number of valid starts for the at-most window. Use `long` because the number of substrings can be `n(n + 1)/2`.

```java
static long countSubstringsWithExactlyKDistinct(String text, int k) {
    if (k <= 0) {
        return 0;
    }
    return countAtMostDistinct(text, k) - countAtMostDistinct(text, k - 1);
}

static long countAtMostDistinct(String text, int k) {
    java.util.Map<Character, Integer> frequency = new java.util.HashMap<>();
    int left = 0;
    long count = 0;
    for (int right = 0; right < text.length(); right++) {
        frequency.merge(text.charAt(right), 1, Integer::sum);
        while (frequency.size() > k) {
            char removed = text.charAt(left++);
            frequency.compute(removed, (ignored, old) -> old == 1 ? null : old - 1);
        }
        count += right - left + 1L;
    }
    return count;
}
```

Why add `right - left + 1`? Every substring ending at `right` and starting from `left` through `right` also has at most `k` distinct units.

For **longest substring with exactly `k`**, an at-most window can update the answer only when its distinct count equals `k`.

## 5.9 Find all anagram starts

For lowercase English letters, compare rolling difference state instead of recomputing a signature for each window. Track how many of the 26 slots are nonzero:

```java
static java.util.List<Integer> findLowercaseAnagrams(String text, String pattern) {
    java.util.List<Integer> starts = new java.util.ArrayList<>();
    if (pattern.isEmpty() || pattern.length() > text.length()) {
        return starts;
    }
    int[] difference = new int[26];
    int nonZero = 0;

    for (int index = 0; index < pattern.length(); index++) {
        nonZero = adjust(difference, requireLower(pattern.charAt(index)), 1, nonZero);
        nonZero = adjust(difference, requireLower(text.charAt(index)), -1, nonZero);
    }
    if (nonZero == 0) {
        starts.add(0);
    }

    for (int right = pattern.length(); right < text.length(); right++) {
        int outgoing = requireLower(text.charAt(right - pattern.length()));
        int incoming = requireLower(text.charAt(right));
        nonZero = adjust(difference, outgoing, 1, nonZero);
        nonZero = adjust(difference, incoming, -1, nonZero);
        if (nonZero == 0) {
            starts.add(right - pattern.length() + 1);
        }
    }
    return starts;
}

static int adjust(int[] values, int index, int delta, int nonZero) {
    if (values[index] != 0) {
        nonZero--;
    }
    values[index] += delta;
    if (values[index] != 0) {
        nonZero++;
    }
    return nonZero;
}

static int requireLower(char unit) {
    if (unit < 'a' || unit > 'z') {
        throw new IllegalArgumentException("lowercase English letters required");
    }
    return unit - 'a';
}
```

Each boundary update changes two slots in constant time. Time is `O(textLength + patternLength)`, auxiliary state is fixed, and output space is proportional to the number of matches.

## 5.10 Minimum covering window

Question: find the shortest substring of `text` containing every unit of `target` with multiplicity. A valid window may contain surplus units.

```java
static String minimumCoveringAsciiWindow(String text, String target) {
    if (target.isEmpty()) {
        return "";
    }
    int[] need = new int[128];
    int requiredTypes = 0;
    for (int index = 0; index < target.length(); index++) {
        char unit = requireAscii(target.charAt(index));
        if (need[unit]++ == 0) {
            requiredTypes++;
        }
    }

    int[] have = new int[128];
    int formedTypes = 0;
    int left = 0;
    int bestStart = 0;
    int bestLength = Integer.MAX_VALUE;

    for (int right = 0; right < text.length(); right++) {
        char entered = requireAscii(text.charAt(right));
        have[entered]++;
        if (need[entered] > 0 && have[entered] == need[entered]) {
            formedTypes++;
        }

        while (formedTypes == requiredTypes) {
            int length = right - left + 1;
            if (length < bestLength) {
                bestStart = left;
                bestLength = length;
            }
            char removed = text.charAt(left++);
            if (need[removed] > 0 && have[removed] == need[removed]) {
                formedTypes--;
            }
            have[removed]--;
        }
    }
    return bestLength == Integer.MAX_VALUE
            ? ""
            : text.substring(bestStart, bestStart + bestLength);
}

static char requireAscii(char unit) {
    if (unit >= 128) {
        throw new IllegalArgumentException("ASCII required");
    }
    return unit;
}
```

![Minimum covering window state transitions](content/volumes/07-strings-and-string-patterns/assets/08-minimum-cover-window.png)

`formedTypes` counts required units whose current frequency reaches the target frequency. Surplus copies do not create extra formed types.

### Dry run highlights for `ADOBECODEBANC`, target `ABC`

| event | window | valid? | action |
|---|---|---|---|
| enter C at 5 | `ADOBEC` | yes | record length 6, then shrink |
| shrink past A | `DOBEC` | no | expand again |
| enter A at 10 | `CODEBA` | no C after later shrink? | continue state repair |
| enter C at 12 | `ODEBANC` | yes | shrink to `BANC`, length 4 |

The final answer is `BANC`. Each unit enters once and leaves at most once: `O(n + m)` time and `O(1)` state for ASCII.

## 5.11 Replacement budget window

Question: longest uppercase ASCII substring that can become one repeated letter with at most `k` replacements.

```java
static int longestRepeatedAfterReplacement(String text, int k) {
    if (k < 0) {
        throw new IllegalArgumentException("k must be nonnegative");
    }
    int[] frequency = new int[26];
    int left = 0;
    int maximumFrequencySeen = 0;
    int best = 0;

    for (int right = 0; right < text.length(); right++) {
        char unit = text.charAt(right);
        if (unit < 'A' || unit > 'Z') {
            throw new IllegalArgumentException("uppercase English letters required");
        }
        maximumFrequencySeen = Math.max(
                maximumFrequencySeen, ++frequency[unit - 'A']);

        while (right - left + 1 - maximumFrequencySeen > k) {
            frequency[text.charAt(left++) - 'A']--;
        }
        best = Math.max(best, right - left + 1);
    }
    return best;
}
```

The cached maximum frequency may be stale after shrinking. That is safe for the maximum-length objective: it can delay shrinking, but it does not manufacture a longer achievable answer than one supported when that maximum was observed. If asked to return the exact current window or prove a different property, recompute or maintain stronger state. This distinction is a valuable SDE-2 follow-up.

## 5.12 When a sliding window is invalid

Sliding windows depend on a one-direction repair rule. For sum thresholds, positive numbers often provide monotonicity: removing left items decreases the sum. With negative values, removing an item can increase or decrease the sum, so a standard variable window can miss candidates.

For strings, failure examples include:

- a constraint depending on a non-monotonic score of the complete substring;
- a requirement over noncontiguous subsequences;
- output needing all matches when state discards information; and
- Unicode-aware state paired with incompatible UTF-16 output indexes.

Use prefix state, dynamic programming, search preprocessing, or another dedicated volume when the invariant does not support boundary-only repair.

## 5.13 Code-point windows

A correctness-first option converts to code points:

```java
static int longestUniqueCodePoints(String text) {
    int[] points = text.codePoints().toArray();
    java.util.Map<Integer, Integer> nextAfterLast = new java.util.HashMap<>();
    int left = 0;
    int best = 0;
    for (int right = 0; right < points.length; right++) {
        left = Math.max(left, nextAfterLast.getOrDefault(points[right], -1));
        best = Math.max(best, right - left + 1);
        nextAfterLast.put(points[right], right + 1);
    }
    return best;
}
```

This returns a length in code points and uses `O(p)` output/state space. If the caller needs the original substring, maintain code-point-position to UTF-16-index boundaries.

## 5.14 Window debugging checklist

1. Is the target a contiguous range?
2. Is the interval inclusive or half-open?
3. What state changes when the right boundary enters?
4. What exact predicate means valid?
5. Can removing the left unit repair invalidity monotonically?
6. Is the answer updated before or after repair?
7. Are counts removed when they become zero?
8. Does `left` ever move backward?
9. Does the count of answers require `long`?
10. Are state and returned indexes expressed in the same text unit?

## 5.15 Quick check and practice

1. Why can a nested `while` still yield linear total work?
2. Why does `left = previous + 1` fail without `Math.max`?
3. How many valid at-most-`k` substrings end at `right` after repair?
4. Why does exactly-`k` counting use two at-most counts?
5. What does `formedTypes` mean in minimum cover?
6. Why may a stale maximum frequency be safe for one objective but not another?

**Foundation:** Implement maximum occurrences of a target unit in a window of length `k`.

**Interview Core:** Implement longest substring with at most two distinct lowercase letters, then generalize to `k`.

**SDE-2 Follow-up:** Return the shortest Unicode code-point window containing a target multiset and also return UTF-16 indexes. Explain the mapping, normalization policy, expected map cost, and output tie rule.
