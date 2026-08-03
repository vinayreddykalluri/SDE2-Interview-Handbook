# Live Bit Interviews and JDK Contract Comparisons

> **A note from Vinay:** A bit trick is useful only when you can explain the representation that makes it true. In an interview, say the invariant before the shortcut. That one sentence separates reasoning from memorization.

## 1. Manual first, JDK second

When an interviewer asks you to implement population count, parity, rotation, or a mask operation, derive the manual version first. Afterward, mention the JDK method you would normally use when the operation itself is not being tested.

| Concept under test | Interview-first implementation | JDK comparison |
|---|---|---|
| Count set bits | repeatedly clear `value & (value - 1)` | `Integer.bitCount` / `Long.bitCount` |
| Leading-zero/bit length | shift unsigned until zero | `Integer.numberOfLeadingZeros` |
| Rotation | combine opposite shifts with masked distance | `Integer.rotateLeft` / `rotateRight` |
| Reverse bits | move one bit per iteration | `Integer.reverse` |
| Lowest one bit | `value & -value` with two's-complement explanation | `Integer.lowestOneBit` |
| Sparse mutable bit set | explain word/index mapping | `BitSet` when its contract fits |

The library is often the best production answer because it is readable, tested, and may use platform intrinsics. The manual solution is still required when the interview is testing bit reasoning.

## 2. Signed values do not change the 32-bit pattern

An `int` always has 32 bits. Negative values have the high bit set in two's-complement representation.

```text
value = -1
bits  = 11111111 11111111 11111111 11111111
```

Therefore `Integer.bitCount(-1)` is `32`, not `1`. A loop that stops with `while (value > 0)` is wrong for negative inputs. Use either Brian Kernighan's clearing rule until the pattern becomes zero or an unsigned right shift `>>>`.

## 3. Shift distances are masked

For an `int`, Java uses only the low five bits of the shift distance. Shifting by `32` therefore behaves like shifting by `0`. For a `long`, Java uses the low six bits.

```java
System.out.println(1 << 32);  // 1, not 0
System.out.println(1L << 64); // 1, not 0
```

Do not use this as a clever shortcut. Normalize rotation distances explicitly so the intent is visible.

## 4. Rotation dry run

Rotate the 8-bit pattern `10110001` right by three positions:

```text
original:       10110001
right part:     00010110
wrapped part:   00100000
result:         00110110
```

For a 32-bit `int`, a manual right rotation is:

```java
int distance = requestedDistance & 31;
int rotated = (value >>> distance) | (value << ((32 - distance) & 31));
```

The second mask handles distance zero without relying on an unexplained shift by 32.

## 5. Edge and failure matrix

| Case | Failure | Correct handling |
|---|---|---|
| Negative input to popcount | `while (value > 0)` returns zero | clear set bits until zero or use `>>>` |
| Test bit 31 | `1 << 31` is negative | treat it as a bit pattern; use `!= 0` rather than `> 0` |
| Shift by 32/64 | assume all bits disappear | remember Java masks the distance |
| Signed right shift | use `>>` when zeros must enter | use `>>>` for logical shift |
| Set membership mask | use `int` for more than 32 states | use `long`, `BitSet`, or another representation |
| Subset count | calculate `1 << n` for large `n` | validate width and output-size feasibility |
| XOR duplicate trick | ignore frequency contract | state exactly which values occur once, twice, or another count |
| Prefix XOR count | store only first occurrence | store frequencies when counting all ranges |

## 6. Six live interview rounds

### Round 1 - Count set bits

**Interviewer:** Count the one bits without calling `Integer.bitCount`.

**Candidate opening:** I will repeatedly clear the lowest set bit with `value &= value - 1`; each iteration removes exactly one one-bit.

**Model answer:** The loop runs once per set bit, so time is `O(k)` for `k` set bits and `O(1)` space for a fixed-width integer. It also works for negative values because the loop tests `value != 0`, not `value > 0`.

**Follow-up:** In production, use `Integer.bitCount` unless the manual logic is itself required.

### Round 2 - Unique value with XOR

**Interviewer:** Every value appears twice except one. Find it in constant extra space.

**Model answer:** XOR is associative and commutative, and `x ^ x = 0`, so all pairs cancel and the remaining value is the answer. This relies on the exact “twice except once” contract; it does not solve arbitrary frequencies.

**Follow-up:** If every repeated value occurs three times, use per-bit counts modulo three or a two-mask state machine.

### Round 3 - Missing number and overflow

**Interviewer:** Values contain every number from zero through `n` except one.

**Model answer:** XOR the expected range and the actual values. Equal numbers cancel, avoiding the overflow risk of the arithmetic-sum formula. Time is `O(n)`, extra space is `O(1)`.

**Follow-up:** If the input contract permits duplicates, neither simple XOR nor the sum formula is sufficient.

### Round 4 - Power of two

**Interviewer:** Is `value & (value - 1) == 0` enough?

**Model answer:** No. Zero also satisfies the expression. The complete signed-`int` contract is `value > 0 && (value & (value - 1)) == 0`.

**Follow-up:** `Integer.MIN_VALUE` has one set bit but is not a positive power of two in the ordinary numeric contract.

### Round 5 - Prefix XOR ranges

**Interviewer:** Count subarrays whose XOR equals `target`.

**Model answer:** If the current prefix is `p`, an earlier prefix must equal `p ^ target`. Store prefix frequencies, seed frequency `0 -> 1`, add the compatible frequency, and then record `p`. Expected time is `O(n)` and space is `O(u)` for distinct prefix values.

**Follow-up:** Use a `long` answer because the number of subarrays can exceed `int`.

### Round 6 - Compact state design

**Interviewer:** Would you always replace a set with a bit mask?

**Model answer:** No. A primitive mask is excellent for a small, fixed, well-defined domain. It becomes unclear or incorrect when the domain exceeds the bit width, identifiers are sparse, or dynamic growth matters. I would then choose `BitSet`, a boolean array, or a set according to the contract.

## 7. Rapid interviewer questions

1. **`>>` versus `>>>`?** `>>` propagates the sign bit; `>>>` inserts zeros.
2. **Why does `x & -x` isolate the lowest set bit?** Two's-complement negation flips bits above that position and adds one, leaving only that shared bit.
3. **Can XOR detect two unique values among pairs?** Yes: XOR all values, split by one differing bit, then XOR within each group.
4. **What does `x ^ 0` produce?** `x`.
5. **What does `x & 0` produce?** Zero.
6. **What does `x | 0` produce?** `x`.
7. **How do you test bit `i`?** `(value & (1L << i)) != 0` after validating the width.
8. **How do you clear bit `i`?** `value & ~(1L << i)`.
9. **How do you toggle bit `i`?** `value ^ (1L << i)`.
10. **Why can subset enumeration overflow?** `2^n` outputs and the shift used to express that count can exceed the chosen primitive width.
11. **Is `BitSet` thread-safe?** No; coordinate concurrent mutation externally.
12. **What should accompany every bit shortcut?** The input contract, representation invariant, width/sign policy, and a boundary test.

## 8. Executable comparison

`BitContractChecks.java` differential-tests manual population count and rotation against the matching JDK operations over fixed boundary values and deterministic pseudo-random values. The JDK call acts as an oracle; the manual implementation remains the teaching target.
