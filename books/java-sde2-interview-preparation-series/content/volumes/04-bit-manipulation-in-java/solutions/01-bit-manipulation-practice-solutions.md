# Practice Solutions and Reasoning

## How to review

Mark an answer correct only when its reasoning and contract are correct. A lucky output prediction or copied expression is not mastery. For coding tasks, compare invariants and edge cases before comparing syntax.

## Part A - Knowledge-check solutions

**K01.** Bit position 6 contributes `2^6 = 64` when selected.

**K02.** Position zero has weight `2^0 = 1`. Shifting the value one left by position `i` therefore produces the mask for weight `2^i`.

**K03.** A pattern is the fixed sequence of zeros and ones. Signed and unsigned rules assign different numeric meanings to the same pattern. For example, all 32 one bits represent `-1` as a signed `int` and `4_294_967_295` as an unsigned magnitude.

**K04.** AND produces one only when both input bits are one. It can test a selected bit, retain a field, or clear positions by ANDing with an inverted mask.

**K05.** OR produces one when either bit is one. It sets selected flags or combines disjoint flag groups.

**K06.** XOR produces one when bits differ. It toggles selected positions, measures differences, and cancels equal fixed-width values under a matching occurrence contract.

**K07.** Java inverts all 32 `int` positions. In two's complement, `~x == -x - 1`, so `~5 == -6`.

**K08.** Both shift right. `>>` copies the sign bit into high positions; `>>>` fills them with zero. They match for nonnegative inputs but differ for negative values.

**K09.** Both ordinary `int` operations wrap, but a shift discards bits and uses a masked distance. More importantly, replacing arithmetic with a shift can violate a problem contract involving checked overflow, negative rounding, a variable distance, or clarity. Use a shift when bit structure is the intent.

**K10.** Java applies unary or binary numeric promotion to `byte`, `short`, and `char`, normally producing an `int` before the bitwise operation.

**K11.** With `mask = 1L << i`: test with `(x & mask) != 0`; set with `x | mask`; clear with `x & ~mask`; toggle with `x ^ mask`. Each follows directly from the one-bit truth table.

**K12.** The retained value equals the mask, not necessarily one. If bit 5 is selected, the nonzero result is 32.

**K13.** Subtracting one clears the lowest one and turns every lower zero into one. AND preserves the unchanged high prefix and clears the changed suffix, removing exactly that one.

**K14.** `-x` equals `~x + 1`. At the lowest one of `x`, the carry makes the corresponding bit in `-x` one; lower bits are zero in `x`, and higher common ones do not survive the construction. AND leaves only the lowest one.

**K15.** Zero satisfies the raw identity, and `Integer.MIN_VALUE` has one selected representation bit. The usual problem defines positive mathematical powers of two, so `x > 0` is required.

**K16.** It returns 32 because `-1` is all ones in the 32-bit two's-complement representation.

**K17.** `prefix[i]` represents exactly the first `i` elements. The inclusive range `[l, r]` is therefore `prefix[r + 1] ^ prefix[l]`, avoiding a special case at `l = 0`.

**K18.** The input is nonempty, exactly one value occurs once, and every other value occurs exactly twice. If validation is not implemented, the method relies on that promise.

**K19.** If `a != b`, then `a ^ b` has at least one one bit. At that position one exceptional value has zero and the other has one. Equal pairs have equal bits and enter the same group, where they cancel.

**K20.** A negative exceptional value is identified partly by bit 31. Omitting it reconstructs a nonnegative value with the wrong representation.

**K21.** Two masks may select different indexes containing equal values. They are distinct index subsets but can produce identical value lists.

**K22.** There are `2^n` subset objects and `Theta(n * 2^n)` selected-element occurrences across them. Working variables may be small, but the required result is not.

**K23.** Each of the `k` selected positions may be absent or present in a submask, giving `2^k` choices including zero.

**K24.** Each universe position is outside the mask, inside the mask but outside the submask, or inside both. Three independent choices across `n` positions give `3^n` relationships.

**K25.** Prefer `EnumSet` when flags are named Java enum values inside one process and a fixed wire representation is unnecessary. It provides type safety and intent while remaining compact.

**K26.** The expression is read-compute-write. Two threads can read the same old value, set different bits, then have the later write erase the earlier change.

**K27.** Version logical width, position meanings, reserved and required bits, default values, unknown-bit policy, signedness where relevant, and serialized byte order.

**K28.** In binary numeric ordering, a one at a higher position outweighs every combination of lower positions. A trie therefore chooses the opposite branch at the highest available position before optimizing lower bits.

**K29.** When the range crosses a bit boundary, that bit is zero for at least one value and is cleared by the total AND. Only the high prefix shared by both endpoints never changes across the range.

**K30.** OR can only add one bits as a range grows, so every new distinct state must add a position and there are at most `w` such changes. XOR can turn a bit on or off repeatedly and has no monotonic frontier.

## Part B - Output solutions

**O01.** `2`, `14`, `12`. In four bits: `1010 & 0110 = 0010`, OR is `1110`, and XOR is `1100`.

**O02.** `-1`, `-8`. The identity is `~x = -x - 1`.

**O03.** `32`, `1`, `4294967296`. The `int` shift distance 32 becomes zero; the `long` shift retains distance 32.

**O04.** `-1`, `2147483647`. Arithmetic shift repeats one; logical shift inserts zero.

**O05.** `-6`, `250`. The byte promotes to `int`; `& 0xFF` retains only the low eight inverted positions.

**O06.** `true`, `2`. Bit 3 is selected in `1010`; toggling it produces `0010`.

**O07.** `10100000`, `10000`. The first removes the lowest one; the second isolates it. `toBinaryString` omits leading zeros.

**O08.** `0`, `32`, `32`. The last value is a zero-input sentinel, not a valid selected position.

**O09.** `true`, `false`. The positive condition rejects zero.

**O10.** `9`. The two fours and two ones cancel.

**O11.** `7`. Equal pairs cancel regardless of order.

**O12.** `0 1 3 0 4 1 7 0 `, including the trailing space printed by the loop.

**O13.** `7`. The mask has three ones, so it has `2^3 - 1` nonzero submasks.

**O14.** `0 1 3 2 `. These are the first four Gray codes.

**O15.** A negative number, a positive number, then `4294967295`. Exact comparison returns are specified only by sign, not necessarily `-1` and `1`; current methods return `-1` and `1`, but tests should rely on sign. Unsigned `-1` is the largest 32-bit magnitude.

**O16.** `101`, `2`. Logical length is highest selected index plus one; cardinality counts selected positions.

**O17.** `8`. XOR holds sum without carry and shifted AND holds the carry until none remains.

**O18.** `8`, `3`. The highest selected position of 13 is index 3 and its mask is 8.

**O19.** `24`. The common prefix of 26 and 30 is restored with trailing zeros.

**O20.** Both lines print `-2147483648` and `true`, respectively. The sign-bit mask is negative, and the raw one-bit identity holds. This is why positive power-of-two detection requires `value > 0`.

## Part C - Debugging solutions

**D01.** Comparing with one fails for every selected bit except bit zero. Use `(value & (1 << index)) != 0` after validating `0..31`.

**D02.** The mask is calculated as an `int`; index 40 becomes effective distance 8. Use `1L << index` and validate `0..63`.

**D03.** Java silently masks invalid shift distances. Reject indexes outside `0..31` before shifting.

**D04.** XOR toggles and will set an already-clear bit. Use `value & ~(1 << index)`.

**D05.** `~value` is a promoted 32-bit result. For unsigned inverted byte content, return `(~value) & 0xFF`.

**D06.** Zero and `Integer.MIN_VALUE` pass the raw one-bit expression. Use `value > 0 && (value & (value - 1)) == 0`.

**D07.** `while (value > 0)` skips every negative input. Scan exactly 32 positions with `>>>`, or use `while (value != 0) value &= value - 1`.

**D08.** The expected range includes `n = values.length`, which is never XORed. Initialize `answer = values.length` or XOR it separately.

**D09.** For a half-open prefix, the inclusive range is `prefix[right + 1] ^ prefix[left]`. Add bounds validation.

**D10.** The isolated sign-bit mask is `Integer.MIN_VALUE`; `Math.abs` cannot make it positive. Use `combined & -combined` directly as a mask.

**D11.** Bit 31 is missing, so a negative exceptional result cannot be reconstructed. Loop through `bit < Integer.SIZE`.

**D12.** `1 << length` is calculated as `int` before widening. Use `1L << length` and reject infeasible or width-invalid lengths.

**D13.** `~` flips all 32 positions. For a nonnegative significant-width complement, XOR with an all-one mask through the highest selected bit; define zero as a special case.

**D14.** After `sub` reaches zero, the update returns `mask`, creating a cycle. Loop while `sub != 0` and process zero after the loop, or break immediately after processing zero.

**D15.** The map is missing the empty prefix. Add `frequency.put(0, 1)` before the loop so subarrays beginning at index zero are counted.

**D16.** OR does not clear the old field and does not validate width. Build the range mask, verify the new value fits, clear old positions, then OR the shifted value.

**D17.** `volatile` provides visibility and ordering for the variable but not atomicity for the read-modify-write workflow. Use a lock or `AtomicLong.getAndUpdate(current -> current | mask)`.

**D18.** `BitSet.and` mutates `first`. If inputs must be preserved, clone first, mutate the clone, and return it.

**D19.** The answer may exceed `int`. Return `long` and widen intermediate multiplication before it overflows.

**D20.** Scanning only bits 30..0 ignores the sign bit, and `Math.max` uses signed order. Either reject negatives, or scan all 32 bits and define comparison using `Integer.compareUnsigned` or the intended signed objective.

## Part D - Coding-task solution guidance

### C01 - Padded formatter

```java
static String bits32(int value) {
    StringBuilder result = new StringBuilder(Integer.SIZE);
    for (int bit = Integer.SIZE - 1; bit >= 0; bit--) {
        result.append((value >>> bit) & 1);
    }
    return result.toString();
}
```

Exactly 32 iterations preserve leading zeros and negative representations.

### C02 - Validated operations

Centralize validation in a helper returning `1L << index`, then use AND, OR, AND-NOT, and XOR. Test indexes 0 and 63 plus -1 and 64.

### C03 - Low-width mask

Return zero for width zero, `-1L` for width 64, and `(1L << width) - 1` otherwise. Reject widths outside `0..64`.

### C04 - Field extraction and replacement

Validate `offset >= 0`, `width >= 1`, and `offset <= 64 - width`. Extract with `>>> offset` and a low mask. Replace by validating `fieldValue`, clearing the shifted range, and ORing the shifted value.

### C05 - Two bit counts

The scan always uses 32 iterations. Kernighan uses `p` iterations for `p` one bits. Both return the same count for negative values when the scan uses `>>>` or direct mask tests.

### C06 - Powers

Power of two: positive plus one-bit test. Power of four: power of two plus intersection with `0x5555_5555`.

### C07 - Long Hamming distance

```java
return Long.bitCount(first ^ second);
```

XOR selects differing positions across the full 64-bit representation.

### C08 - Reverse bits

Repeat 32 times: shift result left, insert `value & 1`, and consume `value >>>= 1`. Compare randomly with `Integer.reverse`.

### C09 - One among pairs

XOR reduction is sufficient under the promise. State `O(n)` time, `O(1)` space, and no occurrence validation.

### C10 - Missing 0 through n

Initialize answer to `values.length`, then XOR each index and observed value. Test missing zero and missing `n`.

### C11 - Two singles

XOR all, isolate `combined & -combined`, partition, XOR within groups, then sort the two results. Do not use `Math.abs` on the mask.

### C12 - One among triples

For all 32 bits, sum `(value >>> bit) & 1`, reduce modulo three, and OR the remaining bit. Bit 31 support is mandatory.

### C13 - Prefix XOR

Build length `n + 1` with `prefix[i + 1] = prefix[i] ^ values[i]`. Query inclusive range with `prefix[r + 1] ^ prefix[l]` after validation.

### C14 - Integer range XOR

Use the `n & 3` cycle for `0..n`, then XOR the two prefixes. Handle `left == 0` without computing `left - 1`.

### C15 - Target subarrays

Start prefix frequency with zero once. At each element, add the frequency of `prefix ^ target` before recording the current prefix. Return `long`.

### C16 - Subsets

Use masks `0..(1 << n) - 1`, map positions to array indexes, and materialize immutable copies only if required. Report `Theta(n * 2^n)` output size.

### C17 - Submasks including zero

```java
int sub = mask;
while (true) {
    process(sub);
    if (sub == 0) {
        break;
    }
    sub = (sub - 1) & mask;
}
```

This handles `mask == 0` and includes zero exactly once.

### C18 - Gray codes

Generate `i ^ (i >>> 1)`. For adjacent values, assert `Integer.bitCount(previous ^ current) == 1`. Limit `n` before calculating `1 << n`.

### C19 - Maximum XOR differential test

For small random nonnegative arrays, compare the trie result with the maximum over all `i < j`. Include duplicates, zeros, one-bit values, and values near `Integer.MAX_VALUE`. A mismatch exposes trie or contract defects.

### C20 - Range AND alternatives

Common-prefix method shifts both endpoints until equal, then shifts back. Clearing method repeatedly applies `right &= right - 1` while `left < right`. Verify both against a direct small-range baseline.

### C21 - Total set bits

Use the highest-power block recurrence and a `long` result. Test against a direct `Long.bitCount` sum for many small `n` values.

### C22 - Minimum XOR pair

Clone to preserve ownership, reject or define negative semantics, sort, then inspect adjacent pairs. Complexity is `O(n log n)` time and `O(n)` copy space.

### C23 - Distinct subarray OR

For each ending position, build a set containing the value and every `prior | value`. Add the frontier to a global set. Explain the width-bounded frontier due to monotonic bit addition.

### C24 - Versioned permissions

A strong design includes a named enum or constants, one mapping from name to stable bit, a known-mask validator, explicit schema version, serialization byte order, reserved positions, and `AtomicLong.getAndUpdate` or a lock for compound updates. Tests cover unknown bits and concurrent independent enables.

## Part E - Follow-up model answers

**F01.** On fixed `int`, at most 32 positions are processed, so the bound is constant with respect to array length. For a `b`-bit arbitrary value, scanning costs `O(b)` and Kernighan costs `O(p)` selected positions, with `p <= b`.

**F02.** Reject both. Java would silently convert 64 to zero and -1 to 63 for a `long` shift, but those are not valid logical indexes.

**F03.** Zero makes the raw clear-lowest identity zero, and MIN_VALUE has exactly one representation bit. A positive mathematical-domain condition rejects both.

**F04.** Use a frequency map and verify exactly one count one and every other count two. That changes auxiliary space to `O(n)` and may be necessary at an untrusted boundary.

**F05.** The isolated mask is negative as an `int`, but AND partitioning still works. Treat it as a pattern and do not take its absolute value.

**F06.** A static prefix array no longer supports constant-time updates. Consider a Fenwick tree or segment tree with XOR as the associative operation, depending on required update/query contracts.

**F07.** At most `n(n + 1)/2` subarrays match, which exceeds `int` for sufficiently large `n`. Prefix state remains `int`; the count should be `long`.

**F08.** Materialization becomes impractical around low twenties for many object-heavy Java outputs, long before a 64-bit representation limit. The exact threshold depends on output form and resource budget.

**F09.** The mask omits order and any attributes not needed by future transitions. It is safe only when the future cost depends solely on the selected set and a derivable step such as `bitCount(mask)`.

**F10.** Object nodes are simplest but allocate heavily; primitive child arrays improve locality and memory; quadratic search uses no trie and can win for tiny input. State `O(nw)` trie cost versus `O(n^2)` baseline.

**F11.** AND is monotonic under expansion: bits only disappear. XOR bits can alternate, so neither a common-prefix result nor a width-bounded monotonic frontier follows.

**F12.** `BitSet` is mutable, and methods such as `and`, `or`, `xor`, and `andNot` modify the receiver. Clone when inputs must remain unchanged and avoid exposing mutable internal state.

**F13.** Assign stable meanings, reserve bits, encode a schema version, and define whether unknown bits are rejected, preserved, or cleared. Never reinterpret an old persisted bit for a new permission.

**F14.** Use a lock or `AtomicLong.getAndUpdate(current -> current | mask)`. `volatile` alone does not make the workflow atomic.

**F15.** Byte order defines how the eight bytes map to the 64-bit numeric value. Bit numbering defines meanings within that value. Document both and convert at the boundary with a specified `ByteOrder`.

## Part F - Assessment review guides

### Assessment 1

A passing answer shows the complete sign-extended 32-bit reasoning for `-13`, distinguishes arithmetic and logical right shift, validates both ends of the `long` index range, proves low-bit identities using subtraction and two's complement, and explains that `~byte` produces `int`.

### Assessment 2

Look for an explicit occurrence promise, one loop invariant per reduction, half-open prefix endpoints, initial zero prefix in frequency counting, zero-safe submask termination, and honest expected-hash versus worst-case qualifications.

### Assessment 3

A strong response starts each optimization from a baseline, defines signedness, makes ownership and mutation explicit, discusses trie allocation and primitive alternatives, separates mask representation from concurrency, and refuses infeasible subset output even when a wider primitive is available.

## Final correction checklist

If any answer was wrong, classify the cause:

- representation width;
- signed versus unsigned interpretation;
- invalid shift distance;
- wrong operator truth table;
- missing occurrence contract;
- incorrect prefix endpoints;
- output-space omission;
- exponential feasibility;
- mutation or ownership;
- concurrency; or
- schema/versioning.

Repeat the smallest matching example, then solve a different problem with the same failure mode. The goal is to correct the mental model, not memorize the answer key.
