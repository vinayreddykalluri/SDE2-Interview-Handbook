# Two Pointers, Frequency, and Core String Patterns

The first reusable string patterns do not need advanced machinery. They arise from two simple observations:

- positions at opposite ends can be related by a mirror or ordering rule; and
- when order does not matter, multiplicity can be represented as frequency state.

This chapter derives those patterns from contracts and invariants, then shows where they stop being valid.

## 4.1 Pattern selection starts with the question

Ask what the result depends on:

| Signal | First baseline | Likely optimized state |
|---|---|---|
| mirrored ends | compare symmetric positions | opposing pointers |
| remove/compact selected units | build another result | read/write pointers or builder |
| same symbols in any order | sort both values | frequency array or map |
| group by equal composition | compare every pair | canonical signature in a map |
| common beginning | compare candidates | bounded prefix scan |

Do not use a frequency table when order matters. Do not use a 26-element array unless the input contract guarantees lowercase English letters.

## 4.2 Palindrome from the baseline

A direct baseline creates a reverse and compares it:

```java
static boolean isAsciiPalindromeWithCopy(String text) {
    return text.equals(new StringBuilder(text).reverse().toString());
}
```

This is readable and `O(n)` time, but it allocates an `O(n)` result. Opposing pointers avoid the copy:

```java
static boolean isExactPalindrome(String text) {
    if (text == null) {
        throw new IllegalArgumentException("text must not be null");
    }
    int left = 0;
    int right = text.length() - 1;
    while (left < right) {
        if (text.charAt(left) != text.charAt(right)) {
            return false;
        }
        left++;
        right--;
    }
    return true;
}
```

![Opposing pointers and the palindrome invariant](content/volumes/dsa/DSA-07-strings-and-string-patterns/assets/05-palindrome-two-pointers.png)

Invariant: before each comparison, every position outside `[left, right]` has already been paired with an equal mirror position. A mismatch proves false. When the pointers meet or cross, no unverified pair remains.

Time is `O(n)` and auxiliary space is `O(1)` under a UTF-16-unit contract.

## 4.3 Normalized palindrome without a copy

A common interview variation ignores non-alphanumeric units and case. For ASCII input:

```java
static boolean isAsciiPhrasePalindrome(String text) {
    if (text == null) {
        return false;
    }
    int left = 0;
    int right = text.length() - 1;

    while (left < right) {
        while (left < right && !isAsciiLetterOrDigit(text.charAt(left))) {
            left++;
        }
        while (left < right && !isAsciiLetterOrDigit(text.charAt(right))) {
            right--;
        }
        char lowLeft = asciiLower(text.charAt(left));
        char lowRight = asciiLower(text.charAt(right));
        if (lowLeft != lowRight) {
            return false;
        }
        left++;
        right--;
    }
    return true;
}

static boolean isAsciiLetterOrDigit(char unit) {
    return (unit >= 'A' && unit <= 'Z')
            || (unit >= 'a' && unit <= 'z')
            || (unit >= '0' && unit <= '9');
}

static char asciiLower(char unit) {
    return unit >= 'A' && unit <= 'Z' ? (char) (unit + ('a' - 'A')) : unit;
}
```

Progress matters: both skipping loops and the comparison branch eventually move a pointer. The ASCII policy is explicit. Replacing it with `Character.isLetterOrDigit` while keeping `char` indexing would broaden classification without providing complete code-point handling.

## 4.4 Valid palindrome after deleting at most one unit

At the first mismatch, any valid answer must delete either the left unit or the right unit. That creates exactly two remaining candidates:

```java
static boolean canBePalindromeAfterOneDeletion(String text) {
    int left = 0;
    int right = text.length() - 1;
    while (left < right && text.charAt(left) == text.charAt(right)) {
        left++;
        right--;
    }
    return left >= right
            || isPalindromeRange(text, left + 1, right)
            || isPalindromeRange(text, left, right - 1);
}

static boolean isPalindromeRange(String text, int left, int rightInclusive) {
    while (left < rightInclusive) {
        if (text.charAt(left++) != text.charAt(rightInclusive--)) {
            return false;
        }
    }
    return true;
}
```

The two checks do not create exponential recursion because branching occurs only once, at the first mismatch. Worst-case time remains `O(n)` and auxiliary space `O(1)`.

## 4.5 Anagrams: sorting baseline

Two values are anagrams under a chosen unit and normalization policy when they contain the same multiplicity of every unit.

The baseline is easy to verify:

```java
static boolean areAnagramsBySorting(String first, String second) {
    if (first == null || second == null || first.length() != second.length()) {
        return false;
    }
    char[] left = first.toCharArray();
    char[] right = second.toCharArray();
    java.util.Arrays.sort(left);
    java.util.Arrays.sort(right);
    return java.util.Arrays.equals(left, right);
}
```

Time is `O(n log n)` and auxiliary/output storage is `O(n)` for the two arrays. This baseline supports any UTF-16 units, though it still does not define normalization or grapheme behavior.

## 4.6 Frequency-array optimization

If the contract guarantees lowercase English letters, a 26-element array is ideal:

```java
static boolean areLowercaseAnagrams(String first, String second) {
    if (first == null || second == null || first.length() != second.length()) {
        return false;
    }
    int[] difference = new int[26];
    for (int index = 0; index < first.length(); index++) {
        char left = first.charAt(index);
        char right = second.charAt(index);
        if (left < 'a' || left > 'z' || right < 'a' || right > 'z') {
            throw new IllegalArgumentException("lowercase English letters required");
        }
        difference[left - 'a']++;
        difference[right - 'a']--;
    }
    for (int value : difference) {
        if (value != 0) {
            return false;
        }
    }
    return true;
}
```

![Frequency state and the anagram contract](content/volumes/dsa/DSA-07-strings-and-string-patterns/assets/06-frequency-and-anagram-state.png)

Invariant: after processing prefix `[0, index]`, each slot equals count in `first` minus count in `second` for that prefix. All final differences are zero exactly when multiplicities match.

Time is `O(n + alphabetSize)`, usually written `O(n)` because alphabet size is the fixed constant 26. Auxiliary space is `O(1)` under that fixed alphabet.

## 4.7 Map frequencies for a broader alphabet

For code points without a small bounded alphabet:

```java
static java.util.Map<Integer, Integer> codePointFrequency(String text) {
    java.util.Map<Integer, Integer> frequency = new java.util.HashMap<>();
    text.codePoints().forEach(codePoint ->
            frequency.merge(codePoint, 1, Integer::sum));
    return frequency;
}

static boolean areCodePointAnagrams(String first, String second) {
    return codePointFrequency(first).equals(codePointFrequency(second));
}
```

This is expected `O(p)` time for `p` code points under ordinary `HashMap` assumptions and `O(k)` auxiliary space for `k` distinct code points. State expected rather than guaranteed constant-time map operations. Normalize first only if the contract requires canonical equivalence.

The Hashing volume develops equality, keys, collision behavior, capacity, and prefix-state patterns in depth.

## 4.8 Grouping anagrams with a signature

The pairwise approach compares every word against every group and can become expensive. Instead, map each word to a canonical signature.

For lowercase English words, serialize all 26 counts with separators:

```java
static String lowercaseSignature(String word) {
    int[] frequency = new int[26];
    for (int index = 0; index < word.length(); index++) {
        char unit = word.charAt(index);
        if (unit < 'a' || unit > 'z') {
            throw new IllegalArgumentException("lowercase English letters required");
        }
        frequency[unit - 'a']++;
    }
    StringBuilder signature = new StringBuilder();
    for (int count : frequency) {
        signature.append('#').append(count);
    }
    return signature.toString();
}
```

Separators prevent ambiguous signatures. For example, the unseparated counts `1, 11` and `11, 1` could both produce `111`.

```java
static java.util.List<java.util.List<String>> groupLowercaseAnagrams(
        java.util.List<String> words) {
    java.util.Map<String, java.util.List<String>> groups = new java.util.LinkedHashMap<>();
    for (String word : words) {
        groups.computeIfAbsent(lowercaseSignature(word), ignored -> new java.util.ArrayList<>())
                .add(word);
    }
    return new java.util.ArrayList<>(groups.values());
}
```

`LinkedHashMap` makes output-group order follow first appearance. That is an output contract choice, not an algorithm requirement.

## 4.9 Longest common prefix

For a nonempty array, start with the first string as the maximum possible prefix and shorten it against each remaining string:

```java
static String longestCommonPrefix(String[] words) {
    if (words == null || words.length == 0) {
        return "";
    }
    if (words[0] == null) {
        throw new IllegalArgumentException("words must not contain null");
    }

    int prefixLength = words[0].length();
    for (int wordIndex = 1; wordIndex < words.length; wordIndex++) {
        String word = java.util.Objects.requireNonNull(words[wordIndex]);
        prefixLength = Math.min(prefixLength, word.length());
        int index = 0;
        while (index < prefixLength
                && words[0].charAt(index) == word.charAt(index)) {
            index++;
        }
        prefixLength = index;
        if (prefixLength == 0) {
            return "";
        }
    }
    return words[0].substring(0, prefixLength);
}
```

The prefix length never increases. Total comparisons are bounded by the total input size in the ordinary analysis, and the final substring allocation is proportional to the returned prefix.

## 4.10 Run-length encoding

Run-length encoding compresses consecutive equal units, not total frequency:

```java
static String encodeRuns(String text) {
    if (text.isEmpty()) {
        return "";
    }
    StringBuilder result = new StringBuilder();
    int runStart = 0;
    for (int index = 1; index <= text.length(); index++) {
        if (index == text.length()
                || text.charAt(index) != text.charAt(runStart)) {
            result.append(text.charAt(runStart));
            result.append(index - runStart);
            runStart = index;
        }
    }
    return result.toString();
}
```

The condition tests `index == length` first, so short-circuiting prevents an invalid `charAt(length)`. For `"aaabbc"`, output is `"a3b2c1"`.

An actual compression API must define how digits and delimiters are escaped, whether only shorter results are accepted, how runs beyond one digit are decoded, and which text unit is compressed.

## 4.11 Pattern boundaries

Two pointers and frequency state are not interchangeable:

- palindrome depends on relative positions, so counts alone are insufficient;
- anagram depends on counts, so preserving positions is unnecessary;
- sorted-order pointers require sortedness or a monotonic relation;
- frequency arrays require a bounded alphabet with a safe index mapping; and
- grapheme-aware problems need a stronger text boundary than `char`.

## 4.12 Complexity comparison

| Problem | Baseline | Improved | Space | Required property |
|---|---:|---:|---:|---|
| exact palindrome | reverse `O(n)` | pointers `O(n)` | `O(1)` auxiliary | mirror relation |
| one deletion palindrome | try every deletion `O(n^2)` | branch at first mismatch `O(n)` | `O(1)` | at most one deletion |
| anagram | sort `O(n log n)` | frequency `O(n)` | `O(A)` | bounded or mapped alphabet |
| group anagrams | pairwise comparisons | signature mapping | depends on signature/map | canonical equality key |
| longest common prefix | compare candidates | shrinking prefix | result only | common prefix cannot grow |
| run encoding | repeated substring work | one traversal | `O(m)` output | consecutive runs |

## 4.13 Quick check and practice

1. State the exact palindrome pointer invariant.
2. Why is only one two-way branch needed for one-deletion palindrome?
3. What input contract justifies a 26-element frequency array?
4. Why does a serialized signature need delimiters?
5. Why is run-length encoding different from a frequency map?
6. What output-order decision does `LinkedHashMap` make in grouping?

**Foundation:** Implement first non-repeated lowercase letter with a frequency pass followed by an order-preserving pass.

**Interview Core:** Return all starting indexes where a lowercase pattern's anagrams occur in a lowercase text. Begin with recomputed counts, then prepare the rolling-window improvement for Chapter 5.

**SDE-2 Follow-up:** Extend anagram grouping to normalized Unicode code points. Define normalization, case, output order, memory limits, and protection against adversarially large distinct alphabets.
