# Java APIs, Production Choices, and Rapid Revision

## Bits are a representation decision

An interview solution may fit in one `int`. A production design must also communicate names, ownership, concurrency, compatibility, and validation. This chapter helps you choose the simplest correct representation and defend that choice at SDE-2 depth.

## Learning objectives

After this chapter, you should be able to:

- choose among `int`, `long`, `BitSet`, `EnumSet`, `BigInteger`, arrays, and sets;
- use Java's built-in bit APIs accurately;
- design a named and versioned packed-field API;
- reason about concurrent updates and serialization boundaries;
- test signed, width, and occurrence-count edge cases;
- identify common interview traps quickly; and
- use a concise decision guide during timed interviews.

## 6.1 Representation decision table

| Representation | Best fit | Strengths | Main costs or risks |
|---|---|---|---|
| `int` mask | at most 32 fixed positions | primitive, compact, fast copying | sign/shift traps, unnamed positions |
| `long` mask | at most 64 fixed positions | larger primitive universe | `1L` and width-64 cases matter |
| `EnumSet<E>` | named enum flags inside Java | type safe, expressive, compact implementation | not a stable external binary schema |
| `BitSet` | dynamic dense nonnegative indexes | grows beyond 64, bulk operations | mutable, index mapping still needs meaning |
| `BigInteger` | immutable arbitrary-width bit pattern | immutable, signed numeric and bit APIs | allocations and signed semantics require care |
| `boolean[]` | bounded flags with direct indexing | obvious operations | more space, copying and equality policy |
| `HashSet<K>` | sparse or unbounded identifiers | expressive, supports arbitrary keys | allocation and expected hash costs |

Choose based on semantics first. A primitive mask is not automatically the best design merely because it is compact.

## 6.2 Essential `Integer` and `Long` APIs

| Question | Java method |
|---|---|
| How many one bits? | `Integer.bitCount(x)` |
| Where is the lowest one? | `Integer.lowestOneBit(x)` |
| Where is the highest one? | `Integer.highestOneBit(x)` |
| How many zeros before the highest one? | `Integer.numberOfLeadingZeros(x)` |
| How many zeros after the lowest one? | `Integer.numberOfTrailingZeros(x)` |
| Reverse all positions? | `Integer.reverse(x)` |
| Reverse byte order? | `Integer.reverseBytes(x)` |
| Rotate without losing bits? | `Integer.rotateLeft(x, d)` / `rotateRight` |
| Compare unsigned? | `Integer.compareUnsigned(a, b)` |
| Widen unsigned `int`? | `Integer.toUnsignedLong(x)` |
| Show unsigned decimal? | `Integer.toUnsignedString(x)` |

`reverse` and `reverseBytes` are different:

```text
reverse:      bit 0 exchanges with bit 31, bit 1 with bit 30, ...
reverseBytes: byte 0 exchanges with byte 3, preserving bit order inside a byte
```

Prefer named library methods in production code. In an interview, be ready to derive the underlying loop when that is the skill being tested.

## 6.3 `BitSet` basics

`BitSet` represents a growable set of nonnegative integer positions.

```java
BitSet available = new BitSet();
available.set(2);
available.set(5);
available.flip(5);
boolean hasTwo = available.get(2);
int selected = available.cardinality();
int first = available.nextSetBit(0);
```

Bulk operations mutate the receiver:

```java
BitSet intersection = (BitSet) firstSet.clone();
intersection.and(secondSet);

BitSet union = (BitSet) firstSet.clone();
union.or(secondSet);
```

Do not mutate an input accidentally. Clone when ownership requires preservation.

Important behaviors:

- logical length is highest selected index plus one, not allocated capacity;
- `size()` reports internal storage capacity and is not the number of selected bits;
- `cardinality()` counts selected positions;
- negative indexes throw `IndexOutOfBoundsException`; and
- equality compares logical selected bits, not spare capacity.

For dense sets much larger than 64, `BitSet` can be clearer and more efficient than a boxed `HashSet<Integer>`.

## 6.4 `EnumSet` for named flags

```java
enum Permission {
    READ,
    WRITE,
    EXPORT,
    ADMIN
}

EnumSet<Permission> permissions = EnumSet.of(
        Permission.READ, Permission.EXPORT);

if (permissions.contains(Permission.EXPORT)) {
    // named capability
}
```

`EnumSet` communicates intent and rejects flags from the wrong enum type. It is usually better than scattering raw bit positions through application code.

Do not persist `EnumSet` by assuming its internal bit mapping is a permanent protocol. Enum ordering can change, and implementation details are not an external schema. Define explicit wire codes when data crosses process or storage boundaries.

## 6.5 `BigInteger` for arbitrary-width immutable bits

Useful methods include:

```java
BigInteger value = BigInteger.ZERO.setBit(100);
boolean selected = value.testBit(100);
BigInteger cleared = value.clearBit(100);
BigInteger toggled = value.flipBit(7);
int count = value.bitCount();
```

`BigInteger` is immutable, so each update returns a new value. It also represents signed mathematical integers using two's-complement-like infinite sign extension for bit operations. For a finite unsigned protocol, define and enforce the logical width rather than assuming `not()` creates a bounded complement.

## 6.6 A named 64-bit flag API

Avoid magic positions at call sites:

```java
enum AccountFlag {
    VERIFIED(0),
    PREMIUM(1),
    EXPORT_ALLOWED(2),
    REVIEW_REQUIRED(3);

    private final long mask;

    AccountFlag(int index) {
        this.mask = 1L << index;
    }

    long mask() {
        return mask;
    }
}

static boolean hasFlag(long flags, AccountFlag flag) {
    Objects.requireNonNull(flag, "flag");
    return (flags & flag.mask()) != 0;
}

static long withFlag(long flags, AccountFlag flag) {
    Objects.requireNonNull(flag, "flag");
    return flags | flag.mask();
}
```

The API gives the bit a domain name. A production design should also specify:

- logical width;
- reserved positions;
- meaning of zero;
- unknown-bit policy;
- persistence and network byte order;
- versioning and migration policy; and
- whether callers may combine arbitrary raw masks.

## 6.7 Packed fields require validation

Suppose a 64-bit word contains a 3-bit status field beginning at offset 8. Values from 0 through 7 fit. A value of 8 must be rejected rather than truncated to zero.

```java
static long packUnsignedField(
        long word, int offset, int width, long fieldValue) {
    if (offset < 0 || width < 1 || width > Long.SIZE
            || offset > Long.SIZE - width) {
        throw new IllegalArgumentException("invalid field bounds");
    }
    long lowMask = width == Long.SIZE ? -1L : (1L << width) - 1;
    if ((fieldValue & ~lowMask) != 0) {
        throw new IllegalArgumentException("field value does not fit");
    }
    if (width == Long.SIZE) {
        return fieldValue;
    }
    long shiftedMask = lowMask << offset;
    return (word & ~shiftedMask) | (fieldValue << offset);
}
```

If negative field values are allowed, define signed-field encoding separately. The unsigned validation above deliberately rejects them.

## 6.8 Concurrency: primitive does not mean compound update is atomic

Two threads execute:

```java
flags = flags | mask;
```

Each operation reads the old value, computes a new value, and writes it. Two updates can race and one may overwrite the other even though each individual primitive read or write has defined atomicity properties.

Use a lock or an atomic read-modify-write operation:

```java
AtomicLong flags = new AtomicLong();

static void enable(AtomicLong flags, long mask) {
    flags.getAndUpdate(current -> current | mask);
}
```

An SDE-2 answer should distinguish representation from synchronization. Compact state does not automatically create a correct concurrent API.

## 6.9 Serialization and endianness

Bit numbering inside a numeric value and byte order in a serialized stream are related but distinct choices.

For a `ByteBuffer`:

```java
ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES)
        .order(ByteOrder.BIG_ENDIAN);
buffer.putLong(flags);
```

The protocol must specify byte order. It must also specify which numeric bit position represents each field. Do not use `Integer.reverseBytes` or `Long.reverseBytes` unless converting an explicitly defined byte order.

## 6.10 Trust-boundary validation

When flags come from a client or stored record:

```java
long knownMask = AccountFlag.VERIFIED.mask()
        | AccountFlag.PREMIUM.mask()
        | AccountFlag.EXPORT_ALLOWED.mask()
        | AccountFlag.REVIEW_REQUIRED.mask();
long unknown = incomingFlags & ~knownMask;
```

Choose an explicit policy:

- reject unknown bits;
- preserve unknown optional bits when forwarding;
- clear unknown bits; or
- accept them only under a newer schema version.

Never grant authorization merely because an unrecognized bit is set. Translate raw values into named, validated capabilities at the boundary.

## 6.11 Testing matrix for bit code

Test more than ordinary positive examples.

| Category | Representative cases |
|---|---|
| zero | no selected bits, empty subset, zero prefix |
| one-bit values | bit 0, middle bit, highest legal bit |
| all ones | `-1`, logical-width all-one mask |
| sign boundary | `Integer.MIN_VALUE`, `Long.MIN_VALUE` |
| range boundaries | index `0`, `31`, `63`, and invalid neighbors |
| occurrence contracts | negative single, malformed duplicates, equal exceptions |
| output size | `n = 0`, small maximum, rejected large subset input |
| fields | width 1, full width, value just too large |
| prefix queries | one element, whole array, invalid endpoints |
| signed policy | compare same bits using signed and unsigned APIs |

Property checks are especially valuable:

```text
clear(set(x, i), i) has bit i clear
toggle(toggle(x, i), i) == x
bitCount(x) == bitCount(x & (x - 1)) + 1 for x != 0
rangeXor(prefix, i, i) == values[i]
gray(i) and gray(i + 1) differ in one bit
```

## 6.12 Mandatory Java traps

1. `1 << 40` is an `int` shift with effective distance 8.
2. `1L << 64` has effective distance zero.
3. `~(byte) 0` is a 32-bit `int` result.
4. `(value & mask) == 1` only works for mask one.
5. `x & (x - 1) == 0` needs parentheses and a positive contract.
6. `>>` and `/ 2` differ for negative odd values.
7. `>>` sign-extends while `>>>` zero-fills.
8. `~mask` flips positions outside a logical subset universe.
9. `x & -x` can produce a negative sign-bit mask.
10. `Integer.numberOfTrailingZeros(0)` returns 32.
11. XOR cancellation does not validate duplicate counts.
12. XOR result ordering may require unsigned comparison.
13. `1 << n` does not make large subset enumeration safe.
14. output-sized subset storage is not `O(1)`.
15. a read-modify-write flag update can lose concurrent changes.
16. `BitSet.size()` is not selected-bit count.
17. `BitSet.and` and `or` mutate the receiver.
18. `BigInteger.not()` is not a finite-width complement.
19. `final` prevents reference reassignment; it does not make a mutable `BitSet` immutable.
20. byte order must be explicit when packed values cross systems.

## 6.13 The interview decision guide

| If the prompt says... | Ask... | First candidate |
|---|---|---|
| flags, permissions, chosen items | Is the universe small, fixed, and dense? | primitive mask or `EnumSet` |
| one bit differs or flips | Is it a comparison of two patterns? | XOR then `bitCount` |
| paired duplicates | What is the exact multiplicity contract? | XOR cancellation |
| two exceptional values | Are they guaranteed distinct? | XOR partition by low bit |
| range XOR queries | Is the array immutable? | prefix XOR |
| target XOR subarrays | Do I need counts of prior prefixes? | prefix-frequency map |
| all subsets | Is `2^n` feasible and output required? | mask enumeration |
| all submasks | How many bits are selected? | `(sub - 1) & mask` |
| maximum XOR | What signed/unsigned order is intended? | high-to-low trie |
| range AND | Which high prefix stays constant? | shift to common prefix |
| many range OR/AND states | Does monotonicity bound distinct states? | compressed frontier |

## 6.14 A five-minute rapid revision sheet

```text
test bit i:       (x & (1L << i)) != 0
set bit i:        x | (1L << i)
clear bit i:      x & ~(1L << i)
toggle bit i:     x ^ (1L << i)
lowest one:       x & -x
remove lowest:    x & (x - 1)
positive power 2: x > 0 && (x & (x - 1)) == 0
Hamming distance: bitCount(a ^ b)
low width mask:   (1L << width) - 1, except width 64
range XOR:        prefix[r + 1] ^ prefix[l]
XOR 0..n:         n, 1, n + 1, 0 by n mod 4
next submask:     (sub - 1) & mask
Gray code:        i ^ (i >>> 1)
```

For every line, remember the width and contract. A sheet is useful for retrieval only after you can derive it.

## 6.15 How to communicate a bit solution

Use this compact script:

1. "I will treat each value as a 32-bit Java pattern. Negative values are allowed/not allowed."
2. "The input promise is ..."
3. "The baseline is ..."
4. "The useful identity or monotonic property is ... because ..."
5. "My loop invariant is ..."
6. "Time is ... and auxiliary/output space is ..."
7. "I will test zero, the highest position, negative or unsigned behavior, and malformed contract cases."
8. "In production I would use the named Java API or representation ..."

That is more convincing than saying "this is a known bit trick."

## Chapter summary

- Primitive masks suit small fixed universes; `EnumSet`, `BitSet`, `BigInteger`, arrays, and sets cover different semantics.
- Java provides clear, optimized bit operations that should be preferred when allowed.
- Packed state is a schema requiring names, bounds, versioning, byte order, and unknown-bit policy.
- Compound concurrent updates need synchronization or atomic read-modify-write operations.
- Tests must cover zero, sign bits, full width, invalid indexes, occurrence contracts, and output limits.
- The best interview technique begins with the contract and derivation, then uses a concise implementation.

## Final learning checkpoint before practice

You are ready for the practice lab when you can use the decision guide without confusing signed and unsigned behavior, explain at least ten traps, and choose a Java representation based on semantics rather than cleverness.
