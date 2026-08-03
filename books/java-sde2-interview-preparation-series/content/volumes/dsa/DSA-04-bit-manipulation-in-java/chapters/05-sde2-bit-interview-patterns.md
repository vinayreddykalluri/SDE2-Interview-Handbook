# SDE-2 Bit Interview Patterns

## Use this chapter after the foundations

The earlier chapters established representation, masks, XOR, and subset state. This chapter combines those tools into frequently asked interview patterns. Each technique is presented as:

1. recognition signal;
2. baseline;
3. key invariant or monotonic property;
4. optimized Java implementation;
5. boundary cases; and
6. SDE-2 follow-up.

Do not force these techniques onto every integer problem. The best answer begins with constraints and a clear baseline.

## Learning objectives

After this chapter, you should be able to:

- maximize XOR with a high-to-low trie;
- compute range AND by finding a common binary prefix;
- count all set bits from one through `n` by block structure;
- compute a significant-bit complement;
- prove why the minimum XOR pair is adjacent after sorting;
- use bounded distinct OR states for subarray reasoning;
- add integers with bit operations under Java overflow semantics; and
- compare bit solutions with simpler sorting, hashing, and library alternatives.

## 5.1 Maximum XOR pair with a bitwise trie

### Recognition signal

The problem asks for the maximum `a ^ b` among many values or repeated maximum-XOR queries.

### Baseline

Compare all pairs: `O(n^2)` time and `O(1)` space.

### Greedy bit insight

At the highest relevant bit, a differing pair produces one. That high one outweighs every possible combination of lower bits. Therefore, when querying for `value`, prefer a trie branch containing the opposite bit at each position.

For nonnegative `int` values, bits 30 through 0 determine ordinary signed order. If negative values are allowed, clarify whether the objective uses signed or unsigned comparison.

### Trie implementation for nonnegative inputs

```java
static final class BitNode {
    final BitNode[] next = new BitNode[2];
}

static int maximumXorPair(int[] values) {
    if (values == null || values.length < 2) {
        throw new IllegalArgumentException("at least two values required");
    }
    BitNode root = new BitNode();
    for (int value : values) {
        if (value < 0) {
            throw new IllegalArgumentException("nonnegative values required");
        }
        insert(root, value);
    }

    int best = 0;
    for (int value : values) {
        best = Math.max(best, bestPartnerXor(root, value));
    }
    return best;
}

static void insert(BitNode root, int value) {
    BitNode node = root;
    for (int bit = 30; bit >= 0; bit--) {
        int current = (value >>> bit) & 1;
        if (node.next[current] == null) {
            node.next[current] = new BitNode();
        }
        node = node.next[current];
    }
}

static int bestPartnerXor(BitNode root, int value) {
    BitNode node = root;
    int result = 0;
    for (int bit = 30; bit >= 0; bit--) {
        int current = (value >>> bit) & 1;
        int preferred = current ^ 1;
        if (node.next[preferred] != null) {
            result |= 1 << bit;
            node = node.next[preferred];
        } else {
            node = node.next[current];
        }
    }
    return result;
}
```

For width `w`, time is `O(nw)` and worst-case node space is `O(nw)`. With 31 fixed positions this is often summarized as linear, but keeping `w` visible explains the memory cost.

### SDE-2 follow-ups

- For online insert-and-query operations, insert incrementally and query before or after insertion according to whether self-pairing is allowed.
- For unsigned 32-bit maximization, include bit 31 and compare candidates with `Integer.compareUnsigned`.
- A node-per-bit object trie creates allocation overhead. Production implementations may use primitive arrays of child indexes.
- For static data and only a few values, the quadratic baseline may be simpler and faster in practice.

## 5.2 Maximum XOR under a limit

A common follow-up asks: for each query `(x, limit)`, maximize `x ^ value` using only stored values `<= limit`.

Technique:

1. sort input values;
2. sort queries by `limit`, remembering original positions;
3. insert eligible values into the trie as limits increase; and
4. query greedily.

This offline ordering turns repeated filtering into one monotonic scan. Complexity is `O((n + q) log(n + q) + (n + q)w)`. The Arrays and Sorting modules develop offline-query design further; the bit-specific part is the trie query.

## 5.3 Bitwise AND of every value in an inclusive range

### Recognition signal

Compute `left & (left + 1) & ... & right` without visiting the entire range.

### Baseline

Loop across the range. This can be enormous and may overflow the loop variable near `Integer.MAX_VALUE`.

### Common-prefix insight

Any bit that changes between `left` and `right` becomes zero somewhere in the range. Only the common high prefix survives.

```java
static int rangeBitwiseAnd(int left, int right) {
    if (left < 0 || right < left) {
        throw new IllegalArgumentException("0 <= left <= right required");
    }
    int shifts = 0;
    while (left != right) {
        left >>>= 1;
        right >>>= 1;
        shifts++;
    }
    return left << shifts;
}
```

Dry run for `[26, 30]`:

```text
26 = 11010
30 = 11110
common prefix is 11
remaining positions become zero
answer = 11000 = 24
```

The loop runs at most 31 times for the nonnegative `int` contract, so it is `O(w)`.

### Alternative: clear right's lowest one

```java
while (left < right) {
    right &= right - 1;
}
return right;
```

Each iteration removes a suffix-changing one bit from the upper bound. Use the common-prefix explanation first because its proof is easier to communicate.

## 5.4 Count all set bits from one through n

### Recognition signal

Return the total number of one bits in every nonnegative integer from `1` to `n`, where looping through all numbers is too slow.

### Highest-power block insight

Let `2^k` be the highest power of two not greater than `n`.

1. In values `0..2^k - 1`, each of the `k` positions is one exactly half the time: `k * 2^(k - 1)` total ones.
2. From `2^k..n`, the highest bit contributes `n - 2^k + 1` ones.
3. Lower bits in that suffix repeat the pattern `0..n - 2^k`.

```java
static long totalSetBitsThrough(int n) {
    if (n < 0) {
        throw new IllegalArgumentException("n must be nonnegative");
    }
    if (n == 0) {
        return 0;
    }
    int highestBit = 31 - Integer.numberOfLeadingZeros(n);
    int power = 1 << highestBit;
    long fullBlock = highestBit == 0
            ? 0
            : (long) highestBit * (power >>> 1);
    long highBitSuffix = (long) n - power + 1;
    return fullBlock + highBitSuffix
            + totalSetBitsThrough(n - power);
}
```

Use `long` for the answer. The count can exceed `Integer.MAX_VALUE` even when `n` is an `int`.

The recursion removes the highest selected bit, so depth is at most 31 and time is `O(log n)` under a numeric-value model.

### Dry run for n = 13

```text
highest power = 8, k = 3
ones in 0..7 = 3 * 4 = 12
high bit in 8..13 = 6
remaining pattern = total bits in 0..5 = 7
total = 12 + 6 + 7 = 25
```

## 5.5 Complement only the significant bits

Java's `~value` flips all 32 bits. Many interview problems define the complement only from bit zero through the highest set bit.

For positive `value`, construct an all-one mask covering that width:

```java
static int significantComplement(int value) {
    if (value < 0) {
        throw new IllegalArgumentException("nonnegative value required");
    }
    if (value == 0) {
        return 1;
    }
    int highest = Integer.highestOneBit(value);
    int mask = (highest << 1) - 1;
    return value ^ mask;
}
```

This implementation fails when `highest` is the sign bit, but the nonnegative contract limits the highest bit to position 30. For wider domains, use `long` and handle its positive limit deliberately.

Example: `5 = 101`, significant mask `111`, result `010 = 2`.

## 5.6 Minimum XOR pair after sorting

### Recognition signal

Find the pair with minimum XOR among nonnegative values.

### Baseline

Check every pair in `O(n^2)` time.

### Sorted-adjacency property

After sorting nonnegative values, a minimum-XOR pair appears among adjacent values.

High-level proof: if `a < b < c`, the first high bit on which `a` and `c` differ separates the ordered range. At least one adjacent boundary inside that range differs no earlier and therefore cannot have a larger most-significant XOR bit than the wider pair. Repeatedly narrowing yields an adjacent candidate.

```java
static int minimumXorPair(int[] input) {
    if (input == null || input.length < 2) {
        throw new IllegalArgumentException("at least two values required");
    }
    int[] values = input.clone();
    for (int value : values) {
        if (value < 0) {
            throw new IllegalArgumentException("nonnegative values required");
        }
    }
    Arrays.sort(values);
    int best = Integer.MAX_VALUE;
    for (int index = 1; index < values.length; index++) {
        best = Math.min(best, values[index - 1] ^ values[index]);
    }
    return best;
}
```

Time is `O(n log n)` and auxiliary space is `O(n)` here because input ownership is preserved by cloning. Sorting the input in place changes the space and mutation contract.

Negative values complicate signed order. Clarify the domain instead of applying the property without qualification.

## 5.7 Distinct bitwise OR values of all subarrays

### Recognition signal

The problem asks for all distinct OR results across contiguous subarrays.

For every ending index, keep the OR values of subarrays ending there. Extending with a new value can only turn bits on, never off. Therefore the number of distinct OR states ending at one position is bounded by the word width plus a small constant: each genuinely new value must add at least one bit.

```java
static int distinctSubarrayOrCount(int[] values) {
    if (values == null) {
        throw new IllegalArgumentException("values must not be null");
    }
    Set<Integer> all = new HashSet<>();
    Set<Integer> previous = Set.of();
    for (int value : values) {
        Set<Integer> current = new HashSet<>();
        current.add(value);
        for (int prior : previous) {
            current.add(prior | value);
        }
        all.addAll(current);
        previous = current;
    }
    return all.size();
}
```

For fixed-width integers, the per-ending frontier has `O(w)` distinct values, giving `O(nw)` expected set operations and up to `O(nw)` distinct results overall. This is a good SDE-2 discussion: the visible nested work is controlled by a monotonic bit property, not by the number of earlier indexes.

## 5.8 Add two integers without `+` or `-`

This problem tests decomposition into sum-without-carry and carry.

```text
partial sum without carry = a ^ b
carry positions           = (a & b) << 1
```

Repeat until no carry remains.

```java
static int addWithBits(int first, int second) {
    while (second != 0) {
        int carry = (first & second) << 1;
        first ^= second;
        second = carry;
    }
    return first;
}
```

The result follows Java `int` wraparound semantics, just like ordinary `+`. The loop terminates within the fixed width because carries move left until they leave the 32-bit representation.

This is an interview exercise, not a recommendation to replace `+` in normal code. If overflow must be detected, use `Math.addExact` or a wider checked representation.

## 5.9 Bitwise AND and OR monotonicity

As a subarray expands:

- its AND can only lose one bits; and
- its OR can only gain one bits.

This limits the number of distinct states at a fixed endpoint and can support compressed-frontier algorithms. XOR has no corresponding monotonicity: adding another value may flip positions in either direction.

Recognition shortcut:

| Operation across a growing range | State movement |
|---|---|
| AND | one bits only disappear |
| OR | one bits only appear |
| XOR | bits may flip either way |

When a problem asks about distinct range AND or OR values, look for a bounded frontier. When it asks about XOR, look instead for cancellation, prefixes, tries, or linear algebra.

## 5.10 Interview technique: baseline to optimization

Use this answer structure:

1. **Contract:** "Inputs are nonnegative `int` values, and the objective uses signed Java order."
2. **Baseline:** "All pairs cost `O(n^2)` and constant extra space."
3. **Signal:** "XOR is decided lexicographically from high bits to low bits."
4. **Data structure or identity:** "A binary trie lets me prefer the opposite bit at each level."
5. **Invariant:** "At bit `b`, the chosen prefix is the best achievable XOR prefix among stored values."
6. **Complexity:** "`O(nw)` time and up to `O(nw)` nodes for width `w`."
7. **Boundaries:** "I need two values, and signed versus unsigned behavior changes bit 31."
8. **Trade-off:** "For small inputs, the quadratic version is simpler and may allocate less."

That sequence demonstrates senior judgment. Starting with a memorized trie template does not.

## 5.11 Common traps

- Greedily maximizing low XOR bits before high bits.
- Ignoring the sign bit while allowing negative values.
- Claiming trie space is `O(n)` without acknowledging the width and object overhead.
- Looping from `left` to `right` near `Integer.MAX_VALUE` and overflowing the loop variable.
- Returning an `int` for the total number of set bits through a large `n`.
- Using `~value` when the problem asks for significant-bit complement.
- Sorting a caller-owned array without stating the mutation.
- Claiming all nested loops are quadratic when a bit frontier has at most `w` states.
- Replacing normal arithmetic with bit arithmetic when readability is more important.

## 5.12 SDE-2 follow-up questions

1. How would a primitive-array trie reduce allocation compared with object nodes?
2. How would you delete values or support duplicate counts in a dynamic trie?
3. What does maximum XOR mean for signed results, and how would unsigned comparison change it?
4. Why does range AND keep only a common prefix?
5. Prove the block formula for total set bits through `n`.
6. Why does OR produce a bounded frontier but XOR does not?
7. When would sorting be preferable to a trie for pair-XOR problems?
8. What overflow semantics does the bit-addition method implement?

## Chapter summary

- A maximum-XOR trie chooses opposite bits from high to low under a defined ordering contract.
- Range AND is the common binary prefix of its endpoints.
- Total set-bit counts decompose around the highest power-of-two block.
- Significant complement needs a logical-width mask.
- Minimum XOR among nonnegative values can be found among sorted neighbors.
- Growing AND and OR ranges have monotonic, width-bounded state frontiers.
- Bit addition separates XOR sum from shifted carry but still wraps like Java `int` addition.
- SDE-2 answers connect the baseline, invariant, constraints, proof, cost, and engineering trade-off.

## Readiness checkpoint

Choose any three patterns in this chapter. Derive them on a whiteboard without code, implement them from the derivation, and answer the signed-input, space-cost, and baseline-comparison follow-ups.
