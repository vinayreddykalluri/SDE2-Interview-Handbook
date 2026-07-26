# 15. Arrays, Strings, Text Blocks, and Unicode

## Learning objectives

By the end of this chapter, you should be able to:

- model Java arrays as fixed-length, reified reference objects;
- distinguish shallow copying, deep copying, aliasing, and covariance hazards;
- explain `String` immutability, pooling, concatenation, and comparison;
- write and reason about text blocks; and
- process Unicode code points instead of assuming one `char` equals one user-visible character.

## Why this matters at SDE-2

Arrays and strings sit on almost every backend boundary: network buffers, database values, identifiers, JSON, logs, and security checks. Errors here become data corruption, accidental quadratic work, or Unicode vulnerabilities. A senior engineer must recognize ownership and encoding assumptions as part of an API contract.

Interviews use arrays and strings heavily because their syntax is simple while their semantics expose fundamentals: references, mutability, indexing, complexity, equality, and representation.

## First-principles model

An array is an object with a runtime component type and immutable length. Its elements are mutable unless prevented by external design. A variable such as `int[] values` contains a reference, not the elements themselves. Multidimensional array syntax represents arrays whose elements are array references; rows may have different lengths or be null.

A `String` is an immutable sequence of UTF-16 code units. A `char` is one 16-bit code unit. Some Unicode code points use one code unit, others use a surrogate pair, and a visible grapheme can combine several code points. Therefore `length()`, code-point count, and what a user sees are different concepts.

A text block is source syntax for a `String`. It improves multiline readability but does not introduce a different runtime type.

> **Specification boundary:** Java specifies array bounds checks, runtime store checks, `String` behavior, and UTF-16-based APIs. It does not guarantee a particular internal `String` field layout or byte encoding used inside a JVM implementation.

## Core terminology

- **Component type:** type of an array's elements.
- **Reified:** runtime retains enough component-type information to check array stores.
- **Covariance:** `Sub[]` is a subtype of `Super[]` when `Sub` is a subtype of `Super`.
- **Aliasing:** multiple references reach the same mutable object.
- **Shallow copy:** copies top-level values or references, not referenced nested objects.
- **Interning:** canonicalizing equal strings in a JVM-managed pool.
- **Code unit:** unit used by an encoding; Java `char` is a UTF-16 code unit.
- **Code point:** Unicode numeric value such as U+1F680.
- **Grapheme cluster:** sequence perceived as one user-visible character.
- **Incidental indentation:** whitespace text blocks remove based on delimiter and content placement.

## Detailed mechanics

### Array creation and initialization

```java
int[] counts = new int[4];          // [0, 0, 0, 0]
String[] names = {"Ada", "Linus"};
int[][] matrix = new int[3][];
matrix[0] = new int[] {1, 2};
matrix[1] = new int[] {3};
// matrix[2] remains null
```

Every array element is default-initialized. Length is available through the final-like `length` field and cannot change. Indexes range from zero through `length - 1`; any other index throws `ArrayIndexOutOfBoundsException`.

Array declarations allow brackets after the type or name, but placing them with the type avoids mixed declarations. Generic array creation such as `new List<String>[10]` is illegal because erased generic element types cannot support the array's runtime store check.

Covariance is checked dynamically:

```java
Number[] numbers = new Integer[2];
numbers[0] = 10;
// numbers[1] = 2.5; // ArrayStoreException
```

The variable permits a `Double` by its static component type, but the actual object is `Integer[]`, so the store fails. Generic collections use invariant static checking and usually make this class of error impossible.

### Copying and comparison

Assignment copies an array reference. `clone`, `Arrays.copyOf`, and `System.arraycopy` create or populate top-level arrays, but nested references remain shared.

```java
int[][] original = {{1, 2}, {3, 4}};
int[][] shallow = original.clone();
shallow[0][0] = 99;
System.out.println(original[0][0]); // 99
```

Arrays inherit identity-based `equals` and `hashCode` from `Object`. Use `Arrays.equals` for one-dimensional content, `Arrays.deepEquals` for nested arrays, and matching hash helpers. Printing an array directly does not format its elements; use `Arrays.toString` or `deepToString`.

### String identity, value, and pool

String literals and constant string expressions are interned. Runtime-created strings need not share identity.

```java
String a = "java";
String b = "ja" + "va";        // compile-time constant
String part = "ja";
String c = part + "va";         // runtime concatenation
System.out.println(a == b);      // true
System.out.println(a == c);      // generally false; do not rely on identity
System.out.println(a.equals(c)); // true
```

Use `equals` for exact content and `equalsIgnoreCase` only when its locale-independent comparison matches the domain. For natural-language case conversion, specify a `Locale`. For protocol identifiers, often use `Locale.ROOT`. Unicode normalization is separate from case folding; visually equivalent strings can have different code-point sequences.

Because strings are immutable, operations such as `substring`, `replace`, and `toUpperCase` return a string and do not modify the receiver. `StringBuilder` provides a mutable sequence for single-threaded assembly. `StringBuffer` synchronizes individual operations but rarely solves a higher-level concurrency design.

Modern compilers use `invokedynamic`-based concatenation strategies for many `+` expressions, so simple one-shot concatenation is readable and efficient. Repeated `result += item` in a loop can still create work proportional to all accumulated content; use a builder or joining collector.

### Text blocks

Text blocks became permanent in Java 15 and are available in Java 17 and 21. The opening `"""` must be followed by a line terminator. The compiler removes incidental indentation, normalizes line terminators, and processes escape sequences.

```java
String json = """
        {
          "name": "Ada",
          "active": true
        }
        """;
```

This value normally ends with a newline because content ends before the closing delimiter. Place the closing delimiter immediately after content, or use a line-continuation escape, when no terminal newline is desired. The `\s` escape preserves a space explicitly. `String.stripIndent()` and `translateEscapes()` expose related runtime transformations, but a compiled text block has already undergone compiler processing.

Never use text blocks as a substitute for SQL parameter binding or HTML/JSON escaping. They improve source presentation, not data safety.

### Unicode processing

`String.length()` returns UTF-16 code units. `codePointCount` counts Unicode code points. Iterate safely with code-point-aware methods:

```java
String value = "A\uD83D\uDE80"; // A plus rocket
System.out.println(value.length()); // 3 code units
System.out.println(value.codePointCount(0, value.length())); // 2

value.codePoints().forEach(cp ->
        System.out.printf("U+%04X%n", cp));
```

`codePointAt` combines a valid surrogate pair. If input contains an unpaired surrogate, it returns that surrogate value; Java strings can contain ill-formed UTF-16 sequences. Encoders must decide whether to replace, report, or ignore malformed input.

Unicode escapes such as `\u000A` are processed very early in Java source translation, even in comments. A Unicode escape that becomes a line terminator can change tokenization or make source illegal. Prefer ordinary escapes like `\n` for controls inside literals.

## Worked Java example

This function reverses code points rather than UTF-16 code units and preserves supplementary characters.

```java
public class UnicodeReverse {
    static String reverseCodePoints(String input) {
        int[] points = input.codePoints().toArray();
        StringBuilder result = new StringBuilder(input.length());
        for (int i = points.length - 1; i >= 0; i--) {
            result.appendCodePoint(points[i]);
        }
        return result.toString();
    }

    public static void main(String[] args) {
        String input = "A\uD83D\uDE80B";
        System.out.println(reverseCodePoints(input)); // B, rocket, A
    }
}
```

This is code-point correct, not grapheme-cluster correct. Reversing combining marks separately can still produce surprising visible output. User-perceived text segmentation requires a boundary algorithm suitable for the Unicode version and locale.

## Execution or memory walkthrough

The input contains four UTF-16 code units: `A`, a high surrogate, a low surrogate, and `B`. `codePoints()` combines the surrogate pair and emits three `int` values. `toArray` allocates an `int[3]`.

The loop begins at index two and appends `B`. It then calls `appendCodePoint` for the rocket value, which emits two UTF-16 code units, followed by `A`. `toString` creates the immutable result. The original string is unchanged.

No bounds error occurs because the loop starts at `points.length - 1`, continues while nonnegative, and decrements. For an empty input it starts at -1 and performs zero iterations.

## Complexity and performance

Array indexing is O(1); traversal and copying are O(n). `System.arraycopy` is still O(n), though JVMs optimize its constant factors. A rectangular `r` by `c` traversal is O(rc); a jagged traversal is proportional to actual elements.

Most string searches and transformations are O(n) in code units, with algorithm-specific exceptions. Concatenating into an immutable accumulator inside a loop can be O(n squared) in total copied content. A pre-sized `StringBuilder` usually makes assembly O(n).

The reverse example is O(n) time and O(n) additional space. A more intricate backward code-point traversal can avoid the `int[]`, but clarity is often worth the allocation unless profiling identifies the path.

> **HotSpot note:** Current HotSpot implementations may store strings compactly using a byte array plus an encoding marker when contents permit. This is not a public `String` contract and should not influence correctness assumptions.

## Edge cases and common mistakes

- Array assignment aliases; it does not copy elements.
- `clone` on a nested or object array is shallow.
- Covariant array stores can fail at runtime.
- `Arrays.asList(primitiveArray)` creates a one-element list containing that array, not a list of boxed primitives.
- `array.length`, `string.length()`, and `collection.size()` use different syntax.
- `==` compares string references; `equals` compares content.
- Calling `toString` on a null reference fails; `String.valueOf` renders it as `"null"`, which may hide missing data.
- Splitting on a regex metacharacter requires regex escaping, and `split` discards trailing empty strings by default unless a negative limit is used.
- A supplementary code point occupies two `char` values.
- Code-point correctness does not guarantee grapheme or locale correctness.
- Default-charset conversions vary by environment; specify `StandardCharsets.UTF_8` at byte boundaries.
- Text blocks still process escapes and normally include a trailing newline based on delimiter placement.

## Production engineering notes

Specify ownership for arrays and mutable builders. Defensive-copy untrusted mutable input when retaining it, and do not expose internal arrays directly. For secrets, a mutable `char[]` can be cleared, but copies may still exist in libraries and memory; do not overstate that protection.

Define charset at every byte/text boundary. Validate malformed input intentionally. Normalize only when the domain requires it, and decide where canonicalization occurs so database keys, caches, signatures, and authorization checks agree.

Use locale-aware libraries for human language and locale-neutral rules for protocols. Length limits must state whether they mean bytes, UTF-16 units, code points, or grapheme clusters. Database column limits and API validators often count differently.

## Interview questions and model answers

**Why are arrays covariant while generics are invariant?**

Arrays predate generics and retain runtime component types, so Java permits covariance and protects stores dynamically. Generic type arguments are normally erased and checked statically; invariance prevents inserting the wrong subtype without waiting for a runtime failure.

**Does `String.length()` count characters?**

It counts UTF-16 code units. It may differ from Unicode code points and from user-perceived grapheme clusters. The correct metric depends on the requirement.

**Why is `String` immutable?**

Immutability enables safe sharing, stable hashing, pooling, and simpler security boundaries. It does not mean every string operation is allocation-free, and sensitive values can remain in memory beyond application control.

**What does a text block produce?**

An ordinary `String`. The compiler determines content through indentation removal, line-ending normalization, and escape processing. Delimiter placement controls whether a final newline remains.

**Is `StringBuilder` always required for concatenation?**

No. One expression using `+` is clear and compilers optimize it effectively. Use a builder when incrementally accumulating in loops or when capacity and append behavior need explicit control.

## Exercises

1. Deep-copy a jagged `int[][]` while preserving null rows.
2. Demonstrate an `ArrayStoreException`, then replace the API with a generic collection.
3. Write tests showing trailing-newline and indentation behavior for three text blocks.
4. Count code units, code points, and expected grapheme clusters in strings containing an emoji and a combining mark.
5. Implement UTF-8 encode/decode with a `CharsetEncoder` configured to report malformed input.
6. Replace quadratic loop concatenation with a pre-sized `StringBuilder` and justify the capacity estimate.

## Chapter summary

Arrays are fixed-length, reified, mutable objects; assignment aliases them, covariance can defer type errors, and top-level copy operations are shallow for reference elements. Strings are immutable UTF-16 code-unit sequences and require value comparison. Text blocks are readable source syntax for ordinary strings. Correct text handling separates bytes, code units, code points, graphemes, normalization, locale, and user-visible length.

## Revision checklist

- [ ] I can explain array reification, covariance, and store checks.
- [ ] I distinguish aliasing, shallow copying, and deep copying.
- [ ] I use content helpers to compare and print arrays.
- [ ] I compare strings by value and understand interning boundaries.
- [ ] I know when repeated concatenation becomes expensive.
- [ ] I can predict text-block indentation and final-newline behavior.
- [ ] I distinguish UTF-8 bytes, UTF-16 units, code points, and graphemes.
- [ ] I specify charset, locale, normalization, and length units at boundaries.
