# Traversal, Unicode, and Text Boundaries

ASCII-only interview inputs make a Java `char` look like a complete character. Real text breaks that shortcut. Java indexes a `String` in UTF-16 code units; Unicode code points may occupy one or two units, and a user-perceived character may contain multiple code points.

You do not need to memorize the Unicode specification. You do need to choose the correct unit before counting, reversing, slicing, or returning indexes.

## 2.1 Four useful units

| Unit | Java representation | Typical use |
|---|---|---|
| UTF-16 code unit | `char`, `charAt`, `length` | APIs and problems explicitly defined in Java indexes |
| Unicode code point | `int`, `codePoints()` | process abstract Unicode scalar values safely |
| encoded byte | `byte`, `getBytes(charset)` | network, file, hash, and storage boundaries |
| grapheme cluster | needs a text-boundary algorithm/library | cursor movement and user-visible character operations |

![UTF-16 units, code points, graphemes, and encoded bytes](content/volumes/dsa/DSA-07-strings-and-string-patterns/assets/02-unicode-units-and-boundaries.png)

For a supplementary code point such as U+1F642, UTF-16 uses a surrogate pair. Therefore:

```java
String text = "A\uD83D\uDE42";
System.out.println(text.length());
System.out.println(text.codePointCount(0, text.length()));
```

Expected output:

```text
3
2
```

The string has three `char` units but two code points.

## 2.2 `char` is a UTF-16 code unit

`char` is an unsigned 16-bit integral primitive. It can represent values from `\u0000` through `\uFFFF`. A Basic Multilingual Plane code point often fits in one `char`; a supplementary code point uses two.

```java
char letter = 'A';
System.out.println((int) letter); // 65

char high = '\uD83D';
char low = '\uDE42';
System.out.println(Character.isSurrogatePair(high, low)); // true
```

Never describe one `char` as universally equal to one visible character. It is still the right unit when the input contract is limited to ASCII, lowercase English letters, or UTF-16 indexes.

## 2.3 Safe code-point traversal

The clearest traversal for many counting tasks is an `IntStream`:

```java
static long countCodePoints(String text) {
    if (text == null) {
        throw new IllegalArgumentException("text must not be null");
    }
    return text.codePoints().count();
}
```

When the algorithm needs the UTF-16 index as well, advance by `Character.charCount(codePoint)`:

```java
static void printCodePoints(String text) {
    for (int index = 0; index < text.length();) {
        int codePoint = text.codePointAt(index);
        System.out.printf("index=%d U+%04X%n", index, codePoint);
        index += Character.charCount(codePoint);
    }
}
```

Dry run for `"A\uD83D\uDE42B"`:

| UTF-16 index | code point | units consumed | next index |
|---:|---|---:|---:|
| 0 | U+0041 | 1 | 1 |
| 1 | U+1F642 | 2 | 3 |
| 3 | U+0042 | 1 | 4 |

The progress measure is the remaining UTF-16 suffix. The loop must advance by one or two units so it never lands inside the surrogate pair.

## 2.4 Index conversions are explicit

Java's `offsetByCodePoints` converts a code-point distance into a UTF-16 index:

```java
String text = "A\uD83D\uDE42B";
int secondCodePointIndex = text.offsetByCodePoints(0, 1);
int thirdCodePointIndex = text.offsetByCodePoints(0, 2);

System.out.println(secondCodePointIndex); // 1
System.out.println(thirdCodePointIndex);  // 3
```

Returning `substring(left, right)` requires UTF-16 indexes. If an algorithm first converts the entire string to `int[] codePoints`, its array positions are not String indexes. Preserve a mapping or return a result defined in code-point positions.

This is a common SDE-2 boundary defect: the matching logic is Unicode-aware, but the output slices the original string with incompatible indexes.

## 2.5 Reversing text: define the contract

`new StringBuilder(text).reverse()` preserves valid surrogate pairs in its documented behavior, making it safer than manually swapping arbitrary `char` units. It still reverses code points, not grapheme clusters.

```java
static String reverseCodePoints(String text) {
    int[] codePoints = text.codePoints().toArray();
    StringBuilder result = new StringBuilder(text.length());
    for (int index = codePoints.length - 1; index >= 0; index--) {
        result.appendCodePoint(codePoints[index]);
    }
    return result.toString();
}
```

A combining mark can expose the remaining limitation. The sequence `e` plus U+0301 COMBINING ACUTE ACCENT is two code points but often displayed as one grapheme. Reversing the two code points independently may detach the mark.

For interview questions, state one of these contracts:

- input contains ASCII only;
- reverse UTF-16 code units;
- reverse Unicode code points; or
- reverse user-perceived characters using a boundary iterator designed for grapheme segmentation.

Do not silently claim the strongest contract.

## 2.6 Normalization

Unicode can represent visually similar text using different code-point sequences. For example, an accented letter may be precomposed or represented as a base letter plus a combining mark.

```java
import java.text.Normalizer;

String composed = "\u00E9";
String decomposed = "e\u0301";

System.out.println(composed.equals(decomposed)); // false
System.out.println(
        Normalizer.normalize(composed, Normalizer.Form.NFC)
                .equals(Normalizer.normalize(decomposed, Normalizer.Form.NFC))
); // true
```

Normalization is a contract decision. NFC is common for stable comparison, but changing stored identifiers or security-sensitive strings after the fact can be dangerous. Normalize at a clearly owned boundary and document whether the original spelling must be preserved.

Normalization costs linear work in the size of the processed text and may allocate another string. Include it in complexity and ownership discussions.

## 2.7 Case and locale

Case conversion can change length and can depend on locale. Avoid relying on the machine's default locale for protocol identifiers.

```java
import java.util.Locale;

String normalizedCommand = command.toLowerCase(Locale.ROOT);
```

`Locale.ROOT` is useful for language-neutral tokens such as keywords and header names. Human-language presentation may require the user's locale. `equalsIgnoreCase` has its own Unicode-aware case comparison rules but is not a substitute for every locale-specific or security policy.

Never reduce a string to lowercase merely to solve a case-sensitive problem. Clarify the contract first.

## 2.8 Characters, digits, and ASCII

ASCII is a subset of Unicode. For an input contract restricted to ASCII decimal digits, arithmetic is simple:

```java
static int asciiDigit(char unit) {
    if (unit < '0' || unit > '9') {
        throw new IllegalArgumentException("ASCII digit required");
    }
    return unit - '0';
}
```

For Unicode digit classification, use `Character` methods and define the accepted radix:

```java
static int decimalDigitValue(int codePoint) {
    int value = Character.digit(codePoint, 10);
    if (value < 0) {
        throw new IllegalArgumentException("decimal digit required");
    }
    return value;
}
```

`Character.isDigit` may accept digits outside ASCII. That can be helpful or surprising. Do not mix a broad validation rule with an ASCII-only conversion formula.

## 2.9 Bytes and charset boundaries

Strings are characters, while files and network messages are bytes. Conversion requires a charset:

```java
import java.nio.charset.StandardCharsets;

String text = "caf\u00E9";
byte[] encoded = text.getBytes(StandardCharsets.UTF_8);
String decoded = new String(encoded, StandardCharsets.UTF_8);

System.out.println(text.equals(decoded)); // true
```

Never rely on the platform default charset for a durable protocol or file format. Encode and decode with the same explicit charset. A byte index is not a UTF-16 index or code-point position.

Malformed external bytes require a policy: report, replace, or ignore. The Java I/O and NIO book covers streaming decoders and error actions; this volume establishes the boundary only.

## 2.10 Grapheme clusters and display text

A grapheme cluster approximates one user-perceived character. It may contain multiple code points, such as a base plus combining marks or an emoji sequence joined by special code points.

Java's standard `BreakIterator.getCharacterInstance(locale)` provides character boundary iteration, but exact behavior depends on Unicode support and the required segmentation standard. Production UI work may choose a specialized Unicode library. Interview code should not implement grapheme segmentation by guessing around combining marks.

The practical hierarchy is:

```text
ASCII-only coding problem      -> char and fixed arrays are often ideal
general Unicode symbol problem -> code points are usually the right unit
display cursor/truncation      -> grapheme-aware boundary service
file/network/storage           -> explicit encoded bytes
```

## 2.11 Complexity and memory

Let `u` be UTF-16 units, `p` code points, `g` grapheme clusters, and `b` encoded bytes. These counts can differ.

| Task | Time | Auxiliary/output space |
|---|---:|---:|
| traverse with `charAt` | `O(u)` | `O(1)` auxiliary |
| `codePoints().toArray()` | `O(u)` | `O(p)` output |
| code-point count | `O(u)` | `O(1)` or stream machinery |
| normalization | `O(u)` model | result allocation may be `O(u)` |
| UTF-8 encode | `O(u)` model | `O(b)` output |
| reverse into a builder | `O(u)` | `O(u)` output |

The exact constants and internal representation are runtime matters. Complexity should name the unit so `O(n)` is not ambiguous.

## 2.12 Common failures

- assuming `length()` counts visible characters;
- indexing each `char` while claiming code-point correctness;
- slicing a string with indexes from an `int[]` code-point array;
- reversing a combining sequence and calling it grapheme-safe;
- lowercasing with the default locale for a protocol key;
- normalizing without permission to change identity;
- accepting broad Unicode digits but subtracting `'0'`;
- encoding and decoding with different or implicit charsets; and
- reporting `O(n)` without defining whether `n` is units, points, or bytes.

## 2.13 Quick check

1. Why can `length()` exceed `codePointCount(...)`?
2. What does `charAt` return when a code point uses a surrogate pair?
3. Why must a code-point traversal advance by `Character.charCount`?
4. When can code-point indexes not be passed to `substring`?
5. What problem does Unicode normalization address?
6. Why is `Locale.ROOT` appropriate for protocol tokens but not all display text?
7. Which boundary requires an explicit charset?

## 2.14 Practice

**Foundation:** Print every code point in a string as `U+XXXX` and its starting UTF-16 index.

**Interview Core:** Reverse a string by code point and test ASCII, an empty string, a supplementary emoji, and a combining sequence. Document the grapheme limitation.

**SDE-2 Follow-up:** Design a substring-search API for Unicode input. State its normalization policy and whether result positions use bytes, UTF-16 indexes, code-point positions, or grapheme positions.
