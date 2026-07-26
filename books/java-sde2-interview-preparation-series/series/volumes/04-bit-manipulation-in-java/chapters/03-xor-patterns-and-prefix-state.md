# XOR Patterns and Prefix State

## Begin with the contract, not the trick

XOR is powerful because equal bit patterns cancel:

```text
x ^ x = 0
x ^ 0 = x
```

It is also associative and commutative, so the order and grouping of a reduction do not matter. These facts solve a problem only when the occurrence counts or prefix structure match them.

Before choosing XOR, state:

- which values are repeated;
- how many times they repeat;
- how many exceptional values exist;
- whether input values are fixed-width Java integers; and
- whether an answer must be interpreted using signed or unsigned order.

## Learning objectives

After this chapter, you should be able to:

- prove pair cancellation with a loop invariant;
- solve one-single, two-single, and triple-occurrence families;
- use XOR to find a missing value under a precise range contract;
- derive prefix XOR for range queries;
- use the four-value cycle for XOR from zero through `n`;
- count subarrays whose XOR equals a target;
- recognize when XOR cannot validate the input promise; and
- avoid swap, signed-order, and duplicate-contract traps.

## 3.1 The cancellation invariant

Consider:

```java
int xor = 0;
for (int value : values) {
    xor ^= value;
}
```

Loop invariant: after processing the first `i` elements, `xor` equals the XOR of exactly those elements.

At termination, algebra may simplify the reduction. If every ordinary value occurs twice and one value occurs once, every pair becomes zero and the exceptional value remains.

XOR does not prove the occurrence contract. For example, four copies also cancel, and three copies reduce to one copy. If validation is required, use counting or a set even though it costs extra space.

## 3.2 Single Number I: one value among pairs

Contract: the array is nonempty, exactly one value appears once, and every other value appears exactly twice.

```java
static int singleAmongPairs(int[] values) {
    if (values == null || values.length == 0) {
        throw new IllegalArgumentException("values must not be empty");
    }
    int answer = 0;
    for (int value : values) {
        answer ^= value;
    }
    return answer;
}
```

Dry run for `[4, 1, 2, 1, 2]`:

| Read | Accumulator |
|---:|---:|
| start | `0` |
| `4` | `4` |
| `1` | `4 ^ 1 = 5` |
| `2` | `5 ^ 2 = 7` |
| `1` | `7 ^ 1 = 6` |
| `2` | `6 ^ 2 = 4` |

Time is `O(n)` and auxiliary space is `O(1)`. A `HashSet` alternative can validate and generalize counts, but it uses `O(n)` space.

### Interview follow-up: what if values appear four times?

Four identical values cancel too. The code still returns an XOR result, but the original semantic promise has changed. Say whether counts divisible by two are acceptable or whether exactly twice must be validated.

## 3.3 Missing value from the range 0 through n

Contract: an array of length `n` contains distinct values from `0..n` with exactly one missing.

XOR every expected value and every observed value. Present pairs cancel.

```java
static int missingFromZeroThroughN(int[] values) {
    if (values == null) {
        throw new IllegalArgumentException("values must not be null");
    }
    int answer = values.length;
    for (int index = 0; index < values.length; index++) {
        answer ^= index;
        answer ^= values[index];
    }
    return answer;
}
```

This avoids the overflow risk of summing `0..n`, but it does not validate distinctness or range membership. A malformed array may produce a plausible answer.

Complexity is `O(n)` time and `O(1)` auxiliary space.

## 3.4 Single Number III: two values among pairs

Contract: exactly two distinct values appear once; every other value appears twice.

Let the exceptional values be `a` and `b`.

1. XOR everything: `combined = a ^ b`.
2. Since `a != b`, `combined` is nonzero.
3. Select one set bit in `combined`; `a` and `b` differ there.
4. Partition every input by that bit.
5. Pairs remain in the same partition and cancel.

```java
static int[] twoSinglesAmongPairs(int[] values) {
    if (values == null || values.length < 2) {
        throw new IllegalArgumentException("at least two values required");
    }
    int combined = 0;
    for (int value : values) {
        combined ^= value;
    }
    if (combined == 0) {
        throw new IllegalArgumentException("two distinct singles required");
    }

    int distinguishingBit = combined & -combined;
    int first = 0;
    int second = 0;
    for (int value : values) {
        if ((value & distinguishingBit) == 0) {
            first ^= value;
        } else {
            second ^= value;
        }
    }
    return first <= second
            ? new int[] {first, second}
            : new int[] {second, first};
}
```

### Dry run

Input: `[1, 2, 1, 3, 2, 5]`

```text
combined = 3 ^ 5 = 0110
lowest selected mask = 0010

mask clear group: 1, 1, 5 -> 5
mask set group:   2, 3, 2 -> 3
```

The selected mask may be `Integer.MIN_VALUE`. That is valid: it represents the sign-bit position on which the exceptional values differ. Do not call `Math.abs` on it.

## 3.5 Single Number II: one value among triples

Contract: one value occurs once and every other value occurs exactly three times.

### Clear baseline: count each bit modulo three

```java
static int singleAmongTriplesByCount(int[] values) {
    if (values == null || values.length == 0) {
        throw new IllegalArgumentException("values must not be empty");
    }
    int answer = 0;
    for (int bit = 0; bit < Integer.SIZE; bit++) {
        int count = 0;
        for (int value : values) {
            count += (value >>> bit) & 1;
        }
        if (count % 3 != 0) {
            answer |= 1 << bit;
        }
    }
    return answer;
}
```

Bit 31 is reconstructed just like every other bit, so negative exceptional values work.

Time is `O(32n)` and auxiliary space is `O(1)`. Under a fixed-width model, this is `O(n)`, but `O(32n)` shows the actual structure.

### Compact state-machine version

For every bit independently, track whether its count is one or two modulo three:

```text
count state: 0 -> 1 -> 2 -> 0
mask state: 00 -> 01 -> 10 -> 00
```

```java
static int singleAmongTriples(int[] values) {
    int ones = 0;
    int twos = 0;
    for (int value : values) {
        ones = (ones ^ value) & ~twos;
        twos = (twos ^ value) & ~ones;
    }
    return ones;
}
```

Invariant: after each input, no bit is present in both `ones` and `twos`; their two-bit state represents the count modulo three.

During an interview, begin with per-bit counts unless you can derive the state machine confidently. A compact answer with no explanation is weaker than a clear correct baseline.

## 3.6 Prefix XOR for repeated range queries

Prefix sums subtract a repeated prefix. Prefix XOR cancels it.

Define a half-open prefix:

```text
prefix[0] = 0
prefix[i + 1] = values[0] ^ ... ^ values[i]
```

Then the inclusive range `[left, right]` is:

```text
prefix[right + 1] ^ prefix[left]
```

Every value before `left` appears twice and cancels.

```java
static int[] buildPrefixXor(int[] values) {
    if (values == null) {
        throw new IllegalArgumentException("values must not be null");
    }
    int[] prefix = new int[values.length + 1];
    for (int index = 0; index < values.length; index++) {
        prefix[index + 1] = prefix[index] ^ values[index];
    }
    return prefix;
}

static int rangeXor(int[] prefix, int left, int right) {
    if (prefix == null || left < 0 || right < left
            || right + 1 >= prefix.length) {
        throw new IllegalArgumentException("invalid range");
    }
    return prefix[right + 1] ^ prefix[left];
}
```

Build: `O(n)` time and `O(n)` space. Query: `O(1)` time.

For one query, a direct loop may be simpler and avoids prefix storage. Prefix state pays off when the array is unchanged and there are many queries.

### Dry run

Values: `[4, 2, 7, 2]`

| Prefix length | Prefix XOR |
|---:|---:|
| 0 | 0 |
| 1 | 4 |
| 2 | `4 ^ 2 = 6` |
| 3 | `6 ^ 7 = 1` |
| 4 | `1 ^ 2 = 3` |

Range `[1, 3]` is `prefix[4] ^ prefix[1] = 3 ^ 4 = 7`, equal to `2 ^ 7 ^ 2`.

## 3.7 XOR from zero through n in constant time

For nonnegative `n`, the XOR `0 ^ 1 ^ ... ^ n` repeats by `n mod 4`:

| `n & 3` | Result |
|---:|---:|
| 0 | `n` |
| 1 | `1` |
| 2 | `n + 1` |
| 3 | `0` |

```java
static int xorZeroThrough(int n) {
    if (n < 0) {
        throw new IllegalArgumentException("n must be nonnegative");
    }
    return switch (n & 3) {
        case 0 -> n;
        case 1 -> 1;
        case 2 -> n + 1;
        default -> 0;
    };
}
```

### Derivation

Group four consecutive values. For `4k` through `4k + 3`, the XOR is zero. The incomplete tail determines the result. Verify the first eight prefix results:

```text
n:      0 1 2 3 4 5 6 7
prefix: 0 1 3 0 4 1 7 0
```

Inclusive range `[left, right]` for nonnegative integers is:

```java
static int xorRange(int left, int right) {
    if (left < 0 || right < left) {
        throw new IllegalArgumentException("invalid nonnegative range");
    }
    return xorZeroThrough(right)
            ^ (left == 0 ? 0 : xorZeroThrough(left - 1));
}
```

State the nonnegative-domain precondition. Do not casually extend this cycle across signed overflow.

## 3.8 Count subarrays whose XOR equals a target

This is the XOR analogue of counting subarrays with a target sum.

Let `prefix` be the XOR through the current position. A prior prefix `previous` creates target XOR when:

```text
previous ^ prefix = target
previous = prefix ^ target
```

Store how often each prior prefix has appeared.

```java
import java.util.HashMap;
import java.util.Map;

static long countSubarraysWithXor(int[] values, int target) {
    if (values == null) {
        throw new IllegalArgumentException("values must not be null");
    }
    Map<Integer, Integer> frequency = new HashMap<>();
    frequency.put(0, 1);
    int prefix = 0;
    long count = 0;
    for (int value : values) {
        prefix ^= value;
        count += frequency.getOrDefault(prefix ^ target, 0);
        frequency.merge(prefix, 1, Integer::sum);
    }
    return count;
}
```

The initial frequency of zero counts subarrays starting at index zero. Use `long` for the number of subarrays because there can be `n(n + 1)/2` matches.

Expected time is `O(n)` with `HashMap`; auxiliary space is `O(n)`. If hash behavior or adversarial keys matter, qualify the cost model rather than claiming an unconditional guarantee.

### Invariant

Before processing the next element, the map contains frequencies of all prefix XORs ending before that element. After updating `prefix`, every matching prior prefix identifies one distinct subarray ending at the current position.

## 3.9 XOR swap: a trick to recognize, not recommend

```java
first ^= second;
second ^= first;
first ^= second;
```

This swaps two distinct variables without a temporary value. It is inferior interview and production Java in most situations:

- it is harder to read;
- it fails conceptually when both names refer to the same storage location, such as swapping an array element with itself;
- the JVM can optimize an ordinary temporary-variable swap; and
- it distracts from the actual algorithm.

Use:

```java
int temporary = first;
first = second;
second = temporary;
```

Knowing the XOR version helps answer a trivia follow-up. Choosing clarity demonstrates better engineering judgment.

## 3.10 XOR decision map

| Input promise or query | Pattern | Main warning |
|---|---|---|
| one value once, all others twice | XOR reduction | does not validate counts |
| two values once, all others twice | combined XOR plus partition | combined must be nonzero |
| one value once, all others three times | per-bit modulo count | include sign bit |
| one value missing from distinct `0..n` | expected XOR observed | validate range only if required |
| many immutable range-XOR queries | prefix XOR | define endpoints consistently |
| XOR of integer range | four-value cycle | nonnegative domain |
| count subarrays with target XOR | prefix-frequency map | initialize zero prefix |
| maximize XOR partner | high-to-low trie | signed versus unsigned objective |

## 3.11 Predict, debug, and explain

### Predict

```java
int x = 9;
System.out.println(x ^ x);
System.out.println(x ^ 0);
System.out.println(x ^ -1);
```

Output:

```text
0
9
-10
```

`x ^ -1` flips every position and therefore equals `~x`.

### Debug

```java
static int rangeXor(int[] prefix, int left, int right) {
    return prefix[right] ^ prefix[left];
}
```

With a half-open prefix array, the correct inclusive query is `prefix[right + 1] ^ prefix[left]`. Name the prefix convention before writing the formula.

### Explain

1. Why does the two-single partition keep every duplicate pair together?
2. Why does XOR avoid arithmetic overflow in the missing-number problem?
3. Why can malformed input still produce a plausible XOR answer?
4. Why is `long` appropriate for a subarray count even though prefix state is `int`?
5. What changes if the objective compares XOR values as unsigned integers?

## Chapter summary

- XOR cancellation is algebra applied under an occurrence contract.
- One exceptional value needs one reduction; two need a distinguishing-bit partition.
- Triple occurrences can be handled with per-bit counts modulo three before attempting a compact state machine.
- Prefix XOR gives constant-time immutable range queries.
- Prefix frequency counts target-XOR subarrays in expected linear time.
- The `0..n` XOR cycle is a derived four-case pattern for nonnegative ranges.
- XOR swap is interview trivia, not preferred Java style.

## Readiness checkpoint

Without notes, solve the one-single, two-single, missing-number, range-XOR, and target-subarray-XOR problems. State the exact input promise, loop invariant, time, auxiliary space, and one malformed-input limitation for each.
