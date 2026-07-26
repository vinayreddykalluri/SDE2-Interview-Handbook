# Bit Manipulation Practice Lab

## How to use this lab

Keep a separate answer sheet. For every code question:

1. state the width and signed/unsigned interpretation;
2. draw only the relevant bit positions;
3. predict before running;
4. record the invariant or identity; and
5. state time, auxiliary space, and any output space.

Difficulty labels mean:

- **Foundation:** representation and operator fluency;
- **Interview Core:** reusable coding-interview patterns; and
- **SDE-2 Follow-up:** proof, boundary, API, or engineering depth.

Solutions are in the next chapter. Do not read them until you have written and tested an answer.

## Part A - Knowledge checks

### Foundation

**K01.** What decimal contribution does bit position 6 make when selected?

**K02.** Why does bit numbering begin at zero in the usual mask formula?

**K03.** Explain the difference between a bit pattern and its signed interpretation.

**K04.** What does AND do at one position? Give one use case.

**K05.** What does OR do at one position? Give one use case.

**K06.** What does XOR do at one position? Give one use case.

**K07.** Why is `~5` equal to `-6` rather than a small positive value?

**K08.** Compare `>>` and `>>>`.

**K09.** Why can `x << 1` differ from `x * 2` as a problem-solving replacement even though Java wraparound results match for ordinary `int` arithmetic?

**K10.** Why does `byte` bitwise arithmetic normally produce an `int`?

### Interview Core

**K11.** Derive test, set, clear, and toggle for bit `i`.

**K12.** Why must a bit test compare with nonzero instead of one?

**K13.** Prove that `x & (x - 1)` removes the lowest set bit.

**K14.** Prove that `x & -x` isolates the lowest set bit.

**K15.** Why does a power-of-two test require `x > 0`?

**K16.** What does `Integer.bitCount(-1)` return and why?

**K17.** Why does a half-open prefix XOR make inclusive queries convenient?

**K18.** State the exact input promise for one exceptional value among pairs.

**K19.** Why does one distinguishing bit separate two exceptional values?

**K20.** Why must the sign bit be included when reconstructing one value among triples?

**K21.** What is the difference between index subsets and unique value subsets?

**K22.** Why is materializing every subset not constant auxiliary/output space?

**K23.** Derive the number of submasks of a mask with `k` set bits.

**K24.** Explain the three roles per position behind the `O(3^n)` mask-submask bound.

### SDE-2 Follow-up

**K25.** When should an API use `EnumSet` instead of a raw primitive mask?

**K26.** Why can `flags = flags | mask` lose a concurrent update?

**K27.** What must be versioned when a packed word is persisted or sent over a network?

**K28.** Why is maximum XOR greedy from high bits to low bits?

**K29.** Why does range AND retain only the common high prefix?

**K30.** Explain why a growing OR frontier is width-bounded while a growing XOR frontier is not monotonic.

## Part B - Predict the output

Assume Java 21. Do not run the snippets until after predicting.

### Foundation

**O01.**

```java
System.out.println(10 & 6);
System.out.println(10 | 6);
System.out.println(10 ^ 6);
```

**O02.**

```java
System.out.println(~0);
System.out.println(~7);
```

**O03.**

```java
System.out.println(1 << 5);
System.out.println(1 << 32);
System.out.println(1L << 32);
```

**O04.**

```java
System.out.println(-1 >> 1);
System.out.println(-1 >>> 1);
```

**O05.**

```java
byte value = 5;
System.out.println(~value);
System.out.println((~value) & 0xFF);
```

**O06.**

```java
int value = 0b1010;
int mask = 1 << 3;
System.out.println((value & mask) != 0);
System.out.println(value ^ mask);
```

### Interview Core

**O07.**

```java
int x = 0b10110000;
System.out.println(Integer.toBinaryString(x & (x - 1)));
System.out.println(Integer.toBinaryString(x & -x));
```

**O08.**

```java
System.out.println(Integer.bitCount(0));
System.out.println(Integer.bitCount(-1));
System.out.println(Integer.numberOfTrailingZeros(0));
```

**O09.**

```java
int x = 8;
System.out.println(x > 0 && (x & (x - 1)) == 0);
x = 0;
System.out.println(x > 0 && (x & (x - 1)) == 0);
```

**O10.**

```java
int result = 0;
for (int value : new int[] {4, 1, 4, 9, 1}) {
    result ^= value;
}
System.out.println(result);
```

**O11.**

```java
int prefix = 0;
for (int value : new int[] {3, 3, 5, 5, 7}) {
    prefix ^= value;
}
System.out.println(prefix);
```

**O12.**

```java
for (int n = 0; n < 8; n++) {
    System.out.print(xorZeroThrough(n) + " ");
}
```

Use the four-case method from Chapter 3.

**O13.**

```java
int mask = 0b10110;
int count = 0;
for (int sub = mask; sub != 0; sub = (sub - 1) & mask) {
    count++;
}
System.out.println(count);
```

**O14.**

```java
for (int i = 0; i < 4; i++) {
    System.out.print((i ^ (i >>> 1)) + " ");
}
```

### SDE-2 Follow-up

**O15.**

```java
System.out.println(Integer.compare(-1, 1));
System.out.println(Integer.compareUnsigned(-1, 1));
System.out.println(Integer.toUnsignedLong(-1));
```

**O16.**

```java
BitSet bits = new BitSet();
bits.set(2);
bits.set(100);
System.out.println(bits.length());
System.out.println(bits.cardinality());
```

**O17.**

```java
int first = 3;
int second = 5;
while (second != 0) {
    int carry = (first & second) << 1;
    first ^= second;
    second = carry;
}
System.out.println(first);
```

**O18.**

```java
System.out.println(Integer.highestOneBit(13));
System.out.println(31 - Integer.numberOfLeadingZeros(13));
```

**O19.**

```java
int left = 26;
int right = 30;
int shifts = 0;
while (left != right) {
    left >>>= 1;
    right >>>= 1;
    shifts++;
}
System.out.println(left << shifts);
```

**O20.**

```java
int value = Integer.MIN_VALUE;
System.out.println(value & -value);
System.out.println((value & (value - 1)) == 0);
```

## Part C - Debug the code

For each task, identify the defect, give a failing input, and repair the implementation.

### Foundation

**D01. Check bit**

```java
static boolean isSet(int value, int index) {
    return (value & (1 << index)) == 1;
}
```

**D02. Long mask**

```java
static long set(long value, int index) {
    return value | (1 << index);
}
```

**D03. Index validation**

```java
static int mask(int index) {
    return 1 << index;
}
```

**D04. Clear bit**

```java
static int clear(int value, int index) {
    return value ^ (1 << index);
}
```

**D05. Unsigned byte inversion**

```java
static int invertByte(byte value) {
    return ~value;
}
```

### Interview Core

**D06. Power of two**

```java
static boolean isPowerOfTwo(int value) {
    return (value & (value - 1)) == 0;
}
```

**D07. Count bits of a negative value**

```java
static int countBits(int value) {
    int count = 0;
    while (value > 0) {
        count += value & 1;
        value >>= 1;
    }
    return count;
}
```

**D08. Missing value**

```java
static int missing(int[] values) {
    int answer = 0;
    for (int index = 0; index < values.length; index++) {
        answer ^= index ^ values[index];
    }
    return answer;
}
```

**D09. Prefix range**

```java
static int rangeXor(int[] prefix, int left, int right) {
    return prefix[right] ^ prefix[left];
}
```

**D10. Two exceptional values**

```java
int distinguishing = Math.abs(combined & -combined);
```

**D11. Triple occurrence reconstruction**

```java
for (int bit = 0; bit < 31; bit++) {
    // count and reconstruct
}
```

**D12. Subset bound**

```java
long total = 1 << values.length;
```

**D13. Logical complement**

```java
static int complement(int value) {
    return ~value;
}
```

**D14. Submask loop**

```java
for (int sub = mask; ; sub = (sub - 1) & mask) {
    process(sub);
}
```

**D15. Target XOR count**

```java
Map<Integer, Integer> frequency = new HashMap<>();
int prefix = 0;
long count = 0;
for (int value : values) {
    prefix ^= value;
    count += frequency.getOrDefault(prefix ^ target, 0);
    frequency.merge(prefix, 1, Integer::sum);
}
```

### SDE-2 Follow-up

**D16. Packed field**

```java
long updated = word | (fieldValue << offset);
```

**D17. Concurrent flags**

```java
volatile long flags;

void enable(long mask) {
    flags = flags | mask;
}
```

**D18. `BitSet` intersection**

```java
static BitSet intersection(BitSet first, BitSet second) {
    first.and(second);
    return first;
}
```

**D19. Total set-bit count type**

```java
static int totalSetBitsThrough(int n) {
    // mathematically correct recurrence returning int
}
```

**D20. Maximum XOR with negative inputs**

```java
// Trie scans bits 30..0 and returns Math.max of int XOR results.
```

## Part D - Small coding tasks

### Foundation

**C01.** Implement a padded 32-bit binary formatter without using `String.format`.

**C02.** Implement validated `long` helpers for test, set, clear, and toggle.

**C03.** Return a mask containing the lowest `width` bits for every width from 0 through 64.

**C04.** Extract and replace an unsigned field in a `long`.

**C05.** Count set bits with both a position scan and Kernighan's method. Compare iteration counts.

**C06.** Implement positive power-of-two and power-of-four checks.

**C07.** Return the Hamming distance between two `long` values.

**C08.** Reverse all 32 bits of an `int` without `Integer.reverse`.

### Interview Core

**C09.** Find one exceptional value among pairs.

**C10.** Find the missing value from distinct `0..n` input.

**C11.** Find two exceptional values among pairs and return them in sorted order.

**C12.** Find one value among triples using per-bit counts; support a negative answer.

**C13.** Build a prefix-XOR array and answer validated inclusive queries.

**C14.** Compute XOR for an inclusive nonnegative integer range in constant time.

**C15.** Count subarrays whose XOR equals a target; return `long`.

**C16.** Generate index subsets for at most 20 values and state output space.

**C17.** Enumerate every submask, including zero exactly once.

**C18.** Generate the first `2^n` Gray codes and assert consecutive Hamming distance one.

### SDE-2 Follow-up

**C19.** Implement maximum XOR pair for nonnegative values using both `O(n^2)` and a trie; differential-test them.

**C20.** Implement range bitwise AND using both common-prefix shifts and lowest-bit clearing.

**C21.** Count all set bits from one through `n` using `long` output.

**C22.** Find the minimum XOR pair after cloning and sorting the input.

**C23.** Count distinct subarray OR results using a frontier set.

**C24.** Design a versioned permission value with named flags, unknown-bit validation, and atomic updates.

## Part E - Interview follow-up chains

**F01.** You solved set-bit count. How do fixed-width and arbitrary-precision complexity models differ?

**F02.** You used `1L << index`. What should happen for index 64 or -1?

**F03.** You solved power of two. How do zero and `Integer.MIN_VALUE` expose missing contracts?

**F04.** You found a single value with XOR. How would you validate the occurrence promise?

**F05.** You found two singles. What if their distinguishing bit is the sign bit?

**F06.** You used prefix XOR. How would point updates change the data-structure choice?

**F07.** You counted target-XOR subarrays. What is the maximum answer and why is `long` useful?

**F08.** You generated subsets. At what `n` does output become the limiting factor before mask width?

**F09.** You used bitmask DP. What information is absent from the mask, and how do you know it is safe to omit?

**F10.** You built a trie. Compare object nodes, primitive arrays, and a quadratic baseline.

**F11.** You solved range AND. What property fails if the operator is XOR?

**F12.** You used `BitSet`. Discuss mutability and method side effects.

**F13.** You used a primitive permission mask. How do schema evolution and unknown bits work?

**F14.** Two threads set different bits. Make the operation linearizable.

**F15.** A network protocol provides eight bytes. Separate byte-order handling from numeric bit numbering.

## Part F - Cumulative assessments

### Assessment 1 - Foundation reconstruction

Time box: 35 minutes.

1. Draw `-13` as a 32-bit pattern using two's-complement reasoning.
2. Predict `-13 >> 2` and `-13 >>> 2`.
3. Implement all four one-bit operations for `long` with index validation.
4. Derive low-bit isolation and removal.
5. Explain promotion for `byte flags = 5; int x = ~flags;`.

Pass standard: four of five correct, with no confusion between `>>` and `>>>` or between `int` and `long` shift width.

### Assessment 2 - Pattern selection

Time box: 55 minutes.

Solve and explain:

1. one value among pairs;
2. two values among pairs;
3. inclusive range XOR queries;
4. target-XOR subarray count; and
5. every submask of a given mask.

Pass standard: correct contracts, invariants, boundaries, and complexity for at least four problems. No unexplained memorized expression.

### Assessment 3 - SDE-2 design and optimization

Time box: 70 minutes.

1. Give baseline and trie solutions for maximum XOR.
2. Derive range AND from common-prefix behavior.
3. Design a versioned 64-bit permissions API with atomic updates.
4. Compare `long`, `EnumSet`, `BitSet`, and `HashSet` for three workloads.
5. Review a colleague's subset generator for overflow, duplicate semantics, and output-space claims.

Pass standard: implementation correctness plus explicit trade-offs, mutation/ownership, signedness, and validation.

## Final readiness assessment

You are ready to continue to Loop Mastery and Arrays when all statements are true:

- [ ] I can derive operator results from truth tables.
- [ ] I validate Java shift indexes and choose `1` versus `1L` correctly.
- [ ] I can derive every one-bit mask operation.
- [ ] I can prove low-bit isolation and removal.
- [ ] I can solve the three main occurrence-count XOR families.
- [ ] I can build prefix XOR and a prefix-frequency solution.
- [ ] I report subset and submask complexity honestly.
- [ ] I can explain maximum XOR, range AND, and total set-bit patterns.
- [ ] I can identify at least fifteen Java bit traps.
- [ ] I choose representations based on semantics and constraints.
- [ ] I can discuss packed-schema versioning and atomic updates.
- [ ] I passed all three cumulative assessments without using the solution chapter.

Recommended threshold: at least 80 percent on knowledge and output questions, working implementations for C01-C18, and defensible solutions for at least four of C19-C24. Record every missed contract or width error in an interview error log and repeat the matching section after 48 hours.
