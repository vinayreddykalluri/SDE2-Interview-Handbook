# 4. Subsequence and Palindrome DP

## Why this chapter exists

Subsequence problems are the densest cluster in the dynamic-programming interview space. Longest increasing subsequence, longest common subsequence, longest palindromic subsequence, palindromic substring counting, and their many disguises account for a large share of DP prompts, and they share a small number of state shapes.

They also contain the two most common derivation errors. The first is conflating **subsequence** with **substring**: one allows gaps, the other does not, and the state definitions are not interchangeable. The second is reaching for the O(n log n) longest-increasing-subsequence algorithm without being able to say what its auxiliary array actually holds - which collapses the moment an interviewer asks for the subsequence itself rather than its length.

This chapter derives each from the six-question protocol in chapter 1, and pays particular attention to reconstruction, because "return the answer, not just its size" is the standard follow-up.

## Subsequence versus substring

State the distinction before writing any state, because every later decision depends on it.

```text
"interview"
  substring    (contiguous)       "terv"   yes
                                  "itv"    no
  subsequence  (order preserved)  "itv"    yes
                                  "vti"    no
```

The consequence for DP is structural:

- **Substring** state is usually an interval `[i, j]` or a run ending at `i`. Extending means adding one adjacent character, so transitions look at `i-1` or `j+1`.
- **Subsequence** state is usually a pair of prefix lengths, or a position plus a comparison predicate. Extending means *choosing whether to include* the next character, so transitions branch on take or skip.

If a candidate writes `dp[i]` = "longest palindromic substring ending at i", they have already lost, because palindromes are not built by extension at one end. The correct substring state is the interval.

## Longest common subsequence, and what it generalizes

LCS is the archetype. Both inputs are consumed from the front, and each step either matches both characters or discards one.

**The six questions:**

1. *State:* `dp[i][j]` = length of the LCS of `a[0,i)` and `b[0,j)`. Prefix lengths, not indices - this is what makes the base case free.
2. *Transition:* if `a[i-1] == b[j-1]` then `dp[i][j] = 1 + dp[i-1][j-1]`, else `max(dp[i-1][j], dp[i][j-1])`.
3. *Base:* `dp[0][j] = dp[i][0] = 0`. An empty prefix shares nothing.
4. *Order:* increasing `i`, then increasing `j`. Each cell reads only smaller indices.
5. *Answer:* `dp[n][m]`.
6. *Complexity:* O(n*m) time, O(n*m) space, reducible to O(min(n, m)) if reconstruction is not required.

The half-open prefix convention matters. With `dp[i][j]` meaning "first `i` and first `j` characters", index `0` is the empty prefix and no special-casing is needed. Defining state as "ending at index i" forces an awkward base row and is the usual source of off-by-one bugs.

**Reconstruction** walks backward from `(n, m)`:

```text
while i > 0 and j > 0:
    if a[i-1] == b[j-1]:  emit a[i-1]; i--; j--
    elif dp[i-1][j] >= dp[i][j-1]:  i--
    else:  j--
reverse the emitted characters
```

The tie-break on the `elif` decides *which* LCS you return when several are optimal. Interviewers sometimes ask for a specific one - lexicographically smallest, say - and the honest answer is that the tie-break policy, not the recurrence, controls it.

### What LCS generalizes to

| Problem | Reduction |
|---|---|
| Longest palindromic subsequence | LCS of `s` and `reverse(s)` |
| Shortest common supersequence | `n + m - LCS(a, b)` |
| Minimum deletions to make equal | `(n - LCS) + (m - LCS)` |
| Edit distance (no substitution) | Same as minimum deletions |
| Diff / patch output | LCS reconstruction with the non-matching parts emitted |

The palindromic-subsequence reduction is worth internalizing because it converts an unfamiliar problem into one you have already derived. It is correct because a subsequence of `s` that is also a subsequence of `reverse(s)` reads the same in both directions.

## Longest palindromic subsequence, derived directly

The reduction above works, but interviewers often want the interval formulation, and it is the one that extends to counting.

1. *State:* `dp[i][j]` = length of the longest palindromic subsequence within `s[i..j]` inclusive.
2. *Transition:* if `s[i] == s[j]` then `dp[i][j] = 2 + dp[i+1][j-1]`, else `max(dp[i+1][j], dp[i][j-1])`.
3. *Base:* `dp[i][i] = 1`; every single character is a palindrome of length one.
4. *Order:* **by increasing interval length**, not by increasing `i`. `dp[i][j]` reads `dp[i+1][j-1]`, which is a shorter interval.
5. *Answer:* `dp[0][n-1]`.
6. *Complexity:* O(n^2) time and space.

Step 4 is the one candidates get wrong. A naive nested loop over `i` ascending and `j` ascending reads cells that have not been computed. Two correct orders exist: iterate over interval length, or iterate `i` **descending** and `j` ascending. Both guarantee that `i+1` and `j-1` are already final.

```text
length = 1:  [0,0] [1,1] [2,2] [3,3]      all base
length = 2:  [0,1] [1,2] [2,3]            reads length-0
length = 3:  [0,2] [1,3]                  reads length-1
length = 4:  [0,3]                        reads length-2
```

## Palindromic substrings: expand versus DP

Counting palindromic *substrings* has two standard solutions, and knowing why the simpler one is usually better is the interview signal.

**Interval DP.** `isPal[i][j]` is true when `s[i] == s[j]` and (`j - i < 2` or `isPal[i+1][j-1]`). O(n^2) time and O(n^2) space.

**Expand around centres.** Every palindrome has a centre - a character for odd lengths, a gap for even. There are `2n - 1` centres; expand outward from each while the characters match. O(n^2) time and **O(1) space**.

```java
int countPalindromicSubstrings(String s) {
    int total = 0;
    for (int centre = 0; centre < 2 * s.length() - 1; centre++) {
        int left = centre / 2;
        int right = left + centre % 2;          // odd centre: right == left
        while (left >= 0 && right < s.length()
                && s.charAt(left) == s.charAt(right)) {
            total++;
            left--;
            right++;
        }
    }
    return total;
}
```

Same asymptotic time, quadratically less space, and considerably less code. Reach for the DP table only when you need the `isPal` relation itself for a *later* DP - palindrome partitioning being the standard case, where `isPal` is precomputed and then a second DP minimizes cuts.

> **When O(n) is required:** Manacher's algorithm finds the longest palindromic substring in linear time. It is rarely expected at SDE-2 and is a poor use of interview minutes unless asked for explicitly. Knowing it exists, and that it works by reusing mirror information around a rightmost boundary, is usually sufficient.

## Longest increasing subsequence: both algorithms, honestly

### The O(n^2) formulation

1. *State:* `dp[i]` = length of the longest increasing subsequence **ending at index i**.
2. *Transition:* `dp[i] = 1 + max(dp[j])` over all `j < i` with `a[j] < a[i]`; `1` if none.
3. *Base:* implicit - every element alone is a subsequence of length one.
4. *Order:* increasing `i`.
5. *Answer:* `max(dp)` - note **not** `dp[n-1]`, because the LIS need not end at the last element.
6. *Complexity:* O(n^2) time, O(n) space.

Reconstruction stores a predecessor index whenever `dp[i]` improves, then follows predecessors back from the argmax. This version reconstructs easily, which is why it deserves to be derived first.

### The O(n log n) formulation, and what the array actually holds

This is where candidates lose points by reciting an algorithm they cannot explain.

Maintain an array `tails`, where **`tails[k]` is the smallest possible tail value of an increasing subsequence of length `k+1`** seen so far. It is not the subsequence. It is not sorted input. It is a set of best-case endings, and it is provably increasing.

```java
int lengthOfLIS(int[] values) {
    int[] tails = new int[values.length];
    int size = 0;
    for (int value : values) {
        int position = Arrays.binarySearch(tails, 0, size, value);
        if (position < 0) {
            position = -(position + 1);         // insertion point
        }
        tails[position] = value;                // extend, or improve a tail
        if (position == size) {
            size++;
        }
    }
    return size;
}
```

Two facts make this correct:

- `tails` is strictly increasing, so binary search is valid. A shorter subsequence can always end no higher than a longer one.
- Overwriting `tails[position]` never shortens the answer. It records a subsequence of the same length ending at a smaller value, which can only make future extensions easier.

**The critical honesty:** `tails` is not itself a valid subsequence. Printing it as the answer is wrong, and interviewers ask exactly this. To reconstruct, record for each input element the index it was placed at, plus the predecessor - the element then at `tails[position - 1]` - and walk back from the last element that reached the maximum length.

**Strict versus non-decreasing** is controlled by the search, not the recurrence. `binarySearch` for the leftmost position gives strictly increasing; searching for the rightmost insertion point (upper bound) allows equal values. Getting this backwards silently changes the answer, and it is a common follow-up.

## Worked example: longest palindromic subsequence with reconstruction

```java
import java.util.Arrays;

public final class PalindromicSubsequence {

    /** Length of the longest palindromic subsequence of s. */
    static int length(String s) {
        int n = s.length();
        if (n == 0) {
            return 0;
        }
        int[][] dp = new int[n][n];
        for (int i = 0; i < n; i++) {
            dp[i][i] = 1;
        }
        // Interval length ascending: dp[i][j] reads the shorter dp[i+1][j-1].
        for (int span = 2; span <= n; span++) {
            for (int i = 0; i + span - 1 < n; i++) {
                int j = i + span - 1;
                dp[i][j] = s.charAt(i) == s.charAt(j)
                        ? 2 + (span == 2 ? 0 : dp[i + 1][j - 1])
                        : Math.max(dp[i + 1][j], dp[i][j - 1]);
            }
        }
        return dp[0][n - 1];
    }

    /** One longest palindromic subsequence, rebuilt from the same table. */
    static String reconstruct(String s) {
        int n = s.length();
        if (n == 0) {
            return "";
        }
        int[][] dp = new int[n][n];
        for (int i = 0; i < n; i++) {
            dp[i][i] = 1;
        }
        for (int span = 2; span <= n; span++) {
            for (int i = 0; i + span - 1 < n; i++) {
                int j = i + span - 1;
                dp[i][j] = s.charAt(i) == s.charAt(j)
                        ? 2 + (span == 2 ? 0 : dp[i + 1][j - 1])
                        : Math.max(dp[i + 1][j], dp[i][j - 1]);
            }
        }

        StringBuilder head = new StringBuilder();
        int i = 0;
        int j = n - 1;
        String middle = "";
        while (i <= j) {
            if (i == j) {
                middle = String.valueOf(s.charAt(i));
                break;
            }
            if (s.charAt(i) == s.charAt(j)) {
                head.append(s.charAt(i));
                i++;
                j--;
            } else if (dp[i + 1][j] >= dp[i][j - 1]) {
                i++;                              // tie-break: prefer moving i
            } else {
                j--;
            }
        }
        // Built in three explicit steps. Writing this as
        // `head + middle + head.reverse().toString()` also works, but only
        // because Java evaluates operands left to right and stringifies the
        // first `head` before `reverse()` mutates it. Correctness that depends
        // on evaluation order next to in-place mutation is not worth the line
        // it saves.
        String firstHalf = head.toString();
        String secondHalf = new StringBuilder(firstHalf).reverse().toString();
        return firstHalf + middle + secondHalf;
    }

    public static void main(String[] args) {
        String s = "bbbab";
        System.out.println(length(s));        // 4
        System.out.println(reconstruct(s));   // bbbb
        System.out.println(length("cbbd"));   // 2
        System.out.println(reconstruct("cbbd")); // bb
        System.out.println(length(""));       // 0
    }
}
```

Note the `span == 2` guard. Without it, `dp[i+1][j-1]` on a two-character interval indexes `dp[i+1][i]`, which is below the diagonal and holds a meaningless zero. It happens to be zero here so the code would still work, but relying on that is exactly the kind of accident that breaks when the base case changes.

The reconstruction returns the first half, the optional middle character, then the reversed first half - which is why `head` is reversed in place at the end.

## Edge cases and common mistakes

- Defining a substring state as "ending at i" for palindromes, which does not extend.
- Iterating `i` ascending for interval DP, reading cells not yet computed. Use interval length, or `i` descending.
- Returning `dp[n-1]` for LIS instead of `max(dp)`.
- Presenting the `tails` array as the longest increasing subsequence. It is not one.
- Confusing strict and non-decreasing LIS by using the wrong binary-search boundary.
- Reaching for O(n log n) LIS when the follow-up needs reconstruction, then being unable to produce it.
- Building an O(n^2) `isPal` table when expand-around-centre solves it in O(1) space.
- Forgetting that the palindromic-subsequence-to-LCS reduction needs `reverse(s)`, not a sort.
- Off-by-one from mixing inclusive `[i..j]` interval state with half-open `[0,i)` prefix state in the same solution. Pick one convention per problem and say which.
- Returning any optimal answer when the prompt asked for a specific tie-break, without noting that the policy is a choice.

## Interview questions and model answers

**What is the difference between a subsequence and a substring, and why does it change the DP?**

A substring is contiguous; a subsequence preserves order but allows gaps. Substring state is typically an interval or a run ending at an index, because extension adds an adjacent character. Subsequence state is typically a pair of prefix lengths, because each step chooses to include or skip. The state shapes are not interchangeable, and most derivation errors start by picking the wrong one.

**Derive the longest palindromic subsequence.**

`dp[i][j]` over the inclusive interval `s[i..j]`. Matching ends contribute two plus the inner interval; otherwise take the better of dropping either end. Base is one for every single character. Evaluate by increasing interval length, because the transition reads a shorter interval - iterating `i` ascending reads uncomputed cells. Alternatively, it is exactly `LCS(s, reverse(s))`.

**What does the `tails` array in O(n log n) LIS actually contain?**

`tails[k]` is the smallest tail value among all increasing subsequences of length `k+1` found so far. It is strictly increasing, which is what makes binary search valid, but it is not itself a subsequence of the input. Reconstructing the actual subsequence requires recording each element's placement index and predecessor separately.

**Count palindromic substrings. Which approach and why?**

Expand around each of the `2n-1` centres. Same O(n^2) time as the interval DP but O(1) space and far less code. I would only build the `isPal` table if a later DP consumes it - palindrome partitioning, for instance, where you precompute `isPal` then minimize cuts.

**How do you return the subsequence rather than its length?**

For O(n^2) LIS, store a predecessor index whenever `dp[i]` improves, then follow predecessors from the argmax. For LCS, walk backward from `dp[n][m]` taking a diagonal on a match and otherwise moving toward the larger neighbour. In both cases the tie-break policy determines which optimal answer you return, and that is a decision to state rather than leave implicit.

**Your LIS returns the wrong answer for equal adjacent values. What happened?**

The binary-search boundary. Searching for the leftmost insertion point enforces strictly increasing; searching for the upper bound permits equal values and yields the non-decreasing variant. The recurrence is unchanged - only the search boundary moves - which is why the bug is easy to introduce and hard to spot.

## Exercises

1. **Foundation:** For `"character"`, list three subsequences that are not substrings and one substring that is not a palindrome.
2. **Foundation:** Write the six-question derivation for longest common subsequence before writing any code.
3. **Interview Core:** Implement LCS with reconstruction. Then change the tie-break and show a pair of inputs where the returned subsequence differs while the length does not.
4. **Interview Core:** Implement longest palindromic subsequence twice - once as an interval DP, once as `LCS(s, reverse(s))` - and verify both agree over a few hundred random strings.
5. **Interview Core:** Implement the interval DP with `i` ascending and demonstrate the wrong answer it produces. Explain exactly which cell was read before it was written.
6. **Interview Core:** Count palindromic substrings by expanding around centres, then by interval DP, and compare peak memory on a 10,000-character input.
7. **Interview Core:** Implement O(n^2) LIS with reconstruction, then O(n log n) LIS with reconstruction. State what extra bookkeeping the second needs.
8. **SDE-2 Follow-up:** Convert your LIS to the non-decreasing variant by changing only the search boundary. Write the test that distinguishes them.
9. **SDE-2 Follow-up:** Solve palindrome partitioning with minimum cuts using a precomputed `isPal` table, and justify why the table earns its space here when it did not for counting.
10. **Challenge:** Given the LCS table for two strings, count how many distinct longest common subsequences exist. Explain why this is not simply the number of backward paths.

## Chapter summary

Subsequence problems share a small set of state shapes, and picking the wrong one is the first and most expensive error - substring state extends at an end, subsequence state chooses take-or-skip over prefixes. LCS with half-open prefix state is the archetype, and it generalizes to longest palindromic subsequence, shortest common supersequence, and minimum deletions by reduction rather than by new derivation. Interval DP for palindromes must be evaluated by increasing interval length, because the transition reads a shorter interval; iterating `i` ascending reads uncomputed cells and is the standard bug. For counting palindromic substrings, expanding around the `2n-1` centres matches the DP's time in constant space, so the table only earns its place when a later DP consumes it. Longest increasing subsequence deserves both derivations: the O(n^2) version reconstructs naturally, while the O(n log n) version depends on knowing that `tails[k]` holds the smallest tail of a length-`k+1` subsequence rather than a subsequence itself - and on knowing that the strict versus non-decreasing distinction lives entirely in the binary-search boundary.

## Revision checklist

- [ ] I can state the substring/subsequence distinction and its effect on state shape.
- [ ] I can derive LCS with half-open prefix state and reconstruct from the table.
- [ ] I know the four problems LCS reduces to.
- [ ] I can derive longest palindromic subsequence as interval DP and as an LCS reduction.
- [ ] I know why interval DP must iterate by span, and which cell breaks if it does not.
- [ ] I can count palindromic substrings in O(1) space and say when the table is worth it.
- [ ] I can implement O(n^2) LIS with reconstruction from predecessors.
- [ ] I can explain precisely what `tails[k]` holds and why binary search is valid.
- [ ] I can reconstruct the actual subsequence from the O(n log n) algorithm.
- [ ] I can switch between strict and non-decreasing LIS and test the difference.
