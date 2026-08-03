# Building, Parsing, and Conversion

Many string solutions fail outside the central algorithm. They build output with repeated copying, split with an unintended regular expression, accept malformed numbers, or cross a byte boundary without a charset. This chapter builds a safe transformation toolkit before later pattern chapters depend on it.

## 3.1 Why repeated concatenation can become quadratic

Strings are immutable. Appending to a growing String must produce another value:

```java
String result = "";
for (String word : words) {
    result = result + word;
}
```

Suppose each of `n` words contributes one unit. The created results have approximate lengths `1, 2, 3, ..., n`. Copying that growing prefix costs:

```text
1 + 2 + 3 + ... + n = n(n + 1) / 2 = O(n^2)
```

The code may be acceptable for a few fixed pieces, and the compiler can optimize a single concatenation expression. Do not generalize that optimization to repeated assignment across loop iterations.

## 3.2 `StringBuilder` from first use

`StringBuilder` is a mutable sequence designed for construction:

```java
static String joinWords(String[] words) {
    StringBuilder result = new StringBuilder();
    for (int index = 0; index < words.length; index++) {
        if (index > 0) {
            result.append(' ');
        }
        result.append(words[index]);
    }
    return result.toString();
}
```

For `{"clear", "Java", "code"}`, the method returns `"clear Java code"`. The separator is added *between* items, so there is no trailing-space repair.

![StringBuilder construction pipeline](content/volumes/dsa/DSA-07-strings-and-string-patterns/assets/04-string-builder-pipeline.png)

Core methods:

```java
StringBuilder builder = new StringBuilder("Java");
builder.append(' ');
builder.append(21);
builder.insert(0, "Learn ");
builder.delete(0, 6);
builder.setCharAt(0, 'j');
builder.reverse();
String value = builder.toString();
```

All index operations use UTF-16 units. Reversing a builder is not a promise to reverse grapheme clusters.

## 3.3 Length, capacity, and ownership

`length()` is the number of units currently stored. `capacity()` is the buffer size available before the builder must grow. Capacity growth is an implementation strategy; reason about it as amortized construction rather than depending on a formula.

```java
StringBuilder builder = new StringBuilder(128);
System.out.println(builder.length());   // 0
System.out.println(builder.capacity()); // at least 128 here
```

Pre-sizing is useful when a defensible output bound is known. It is not required for correctness, and an enormous speculative capacity wastes memory.

`StringBuilder` is not thread-safe. Local interview-method ownership is ideal: create, mutate, convert once, and do not leak the builder. `StringBuffer` synchronizes many operations but is not automatically the correct production design. Prefer explicit ownership and only choose synchronization when a shared mutable buffer is actually required.

## 3.4 Building without accidental delimiters

Three common patterns are reliable:

```java
// 1. Add before every item except the first.
if (builder.length() > 0) {
    builder.append(',');
}
builder.append(item);

// 2. Use StringJoiner for prefix, delimiter, and suffix.
java.util.StringJoiner joiner = new java.util.StringJoiner(", ", "[", "]");
joiner.add("A").add("B");

// 3. Join an already available collection of text.
String csv = String.join(",", values);
```

Avoid building a trailing delimiter and deleting it unless the empty-input branch is handled correctly.

## 3.5 Character arrays as a transformation boundary

When an algorithm needs arbitrary in-place swaps, convert explicitly:

```java
static String reverseAscii(String text) {
    char[] units = text.toCharArray();
    for (int left = 0, right = units.length - 1; left < right; left++, right--) {
        char temporary = units[left];
        units[left] = units[right];
        units[right] = temporary;
    }
    return new String(units);
}
```

This code is correct under an ASCII or UTF-16-code-unit reversal contract. It can split surrogate pairs, so it must not claim general code-point behavior.

The conversion costs `O(n)` time and `O(n)` storage. Count the output separately when an immutable result is required anyway.

## 3.6 `split` uses a regular expression

The delimiter passed to `String.split` is a regex:

```java
String[] wrong = "a.b.c".split(".");
String[] correct = "a.b.c".split("\\.");
```

In regex, `.` matches almost any character. The Java source must also escape the backslash, producing `"\\."`.

For a literal delimiter not known until runtime:

```java
import java.util.regex.Pattern;

String[] pieces = input.split(Pattern.quote(delimiter), -1);
```

The limit `-1` preserves trailing empty fields:

```java
System.out.println("a,b,".split(",").length);     // 2
System.out.println("a,b,".split(",", -1).length); // 3
```

If repeated parsing performance matters, precompile a `Pattern` or use a direct scanner for a simple delimiter. Do not reach for regex when the grammar is easier and safer to parse explicitly.

## 3.7 Strict integer parsing by contract

`Integer.parseInt` is correct for ordinary signed decimal input and rejects invalid or overflowing values. Interviewers may ask for manual parsing to test boundary reasoning.

Requirements for a strict parser:

- reject null and empty input;
- accept an optional leading `+` or `-`;
- require at least one digit;
- reject any non-ASCII digit when that is the stated grammar;
- detect overflow before it occurs; and
- handle `Integer.MIN_VALUE`, whose magnitude is one larger than `Integer.MAX_VALUE`.

Accumulating as a negative number avoids the asymmetric positive range:

```java
static int parseIntStrict(String text) {
    if (text == null || text.isEmpty()) {
        throw new NumberFormatException("nonempty integer required");
    }

    int index = 0;
    boolean negative = false;
    char first = text.charAt(0);
    if (first == '-' || first == '+') {
        negative = first == '-';
        index++;
    }
    if (index == text.length()) {
        throw new NumberFormatException("digit required");
    }

    int limit = negative ? Integer.MIN_VALUE : -Integer.MAX_VALUE;
    int multiplicationLimit = limit / 10;
    int result = 0;

    while (index < text.length()) {
        char unit = text.charAt(index++);
        if (unit < '0' || unit > '9') {
            throw new NumberFormatException("invalid digit");
        }
        int digit = unit - '0';
        if (result < multiplicationLimit) {
            throw new NumberFormatException("overflow");
        }
        result *= 10;
        if (result < limit + digit) {
            throw new NumberFormatException("overflow");
        }
        result -= digit;
    }
    return negative ? result : -result;
}
```

### Dry run for `"-214"`

| unit | digit | result before | after multiply/subtract |
|---|---:|---:|---:|
| `2` | 2 | 0 | -2 |
| `1` | 1 | -2 | -21 |
| `4` | 4 | -21 | -214 |

Negative accumulation keeps every intermediate within the allowed negative range. Every boundary comparison occurs before the dangerous arithmetic.

## 3.8 Parsing tokens and preserving the grammar

Parsing is not the same as validation. Start with a grammar:

```text
identifier := ASCII letter (ASCII letter | digit | '_')*
```

Then encode it directly:

```java
static boolean isAsciiIdentifier(String text) {
    if (text == null || text.isEmpty() || !isAsciiLetter(text.charAt(0))) {
        return false;
    }
    for (int index = 1; index < text.length(); index++) {
        char unit = text.charAt(index);
        if (!isAsciiLetter(unit) && !isAsciiDigit(unit) && unit != '_') {
            return false;
        }
    }
    return true;
}

static boolean isAsciiLetter(char unit) {
    return (unit >= 'A' && unit <= 'Z') || (unit >= 'a' && unit <= 'z');
}

static boolean isAsciiDigit(char unit) {
    return unit >= '0' && unit <= '9';
}
```

`Character.isLetterOrDigit` would implement a broader Unicode grammar. Neither choice is universally better; the stated language decides.

## 3.9 Encoding and decoding

At a byte boundary, name the charset:

```java
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

byte[] bytes = "Java".getBytes(StandardCharsets.UTF_8);
String hex = HexFormat.of().formatHex(bytes);
String restored = new String(bytes, StandardCharsets.UTF_8);
```

Expected values:

```text
hex      = 4a617661
restored = Java
```

`new String(bytes)` and `text.getBytes()` use the default charset. Avoid those overloads for stable data. The I/O volume covers decoders, malformed sequences, streaming, and file ownership.

## 3.10 Formatting and output safety

Formatting methods improve readability:

```java
String summary = "processed=%d rejected=%d".formatted(processed, rejected);
```

But do not build SQL, shell commands, HTML, JSON, or log records by casual concatenation. Each context has its own escaping, parameterization, and injection rules. In production follow-ups:

- use prepared statements for database values;
- use a JSON library for JSON;
- keep secrets and personal data out of logs;
- avoid constructing shell commands from untrusted text; and
- place maximum-length limits before retaining attacker-controlled input.

These are boundary contracts, not advanced algorithms.

## 3.11 Complexity model

Let `n` be input UTF-16 units and `m` be output units.

| Operation | Time | Space note |
|---|---:|---|
| append all pieces to one builder | `O(m)` amortized | builder/output capacity `O(m)` |
| insert/delete near front of builder | `O(m)` | suffix shifts |
| `toCharArray` and `new String(char[])` | `O(n)` each | independent copy/result |
| regex split | depends on regex and input | array plus token strings |
| strict decimal parse | `O(n)` | `O(1)` auxiliary |
| encode/decode | linear in processed input/output | new byte/String result |

Do not say every builder append is worst-case `O(1)`. Most appends are constant work, while occasional growth copies the buffer; the sequence is amortized linear.

## 3.12 Common failures

- concatenating a growing result in a loop;
- confusing builder length with capacity;
- leaking one mutable builder across owners or threads;
- splitting on `.` or `|` without regex escaping;
- losing trailing empty fields;
- parsing with `result = result * 10 + digit` before checking overflow;
- accepting a sign with no following digit;
- using a broad character predicate with a narrow conversion formula;
- relying on the default charset; and
- hand-building structured formats without their escaping rules.

## 3.13 Quick check and practice

1. Why can growing concatenation cost quadratic time?
2. When is a pre-sized builder valuable?
3. Why does `split(".")` not split literal dots?
4. What does a negative split limit preserve?
5. Why does the strict parser accumulate negatively?
6. Which overloads avoid the platform default charset?

**Foundation:** Build a comma-separated string with no leading or trailing comma. Define null element behavior.

**Interview Core:** Implement strict parsing for a signed `long`, including both extreme values.

**SDE-2 Follow-up:** Design a parser for `key=value` records where delimiters may be escaped. Compare direct scanning with regex and state maximum-input and error-reporting policies.
