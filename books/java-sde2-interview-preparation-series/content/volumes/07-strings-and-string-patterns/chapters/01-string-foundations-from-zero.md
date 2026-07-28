# String Foundations from Zero

Strings appear in almost every interview: identifiers, logs, file paths, words, tokens, and serialized data are all text. The algorithms are rarely the only difficulty. A candidate must also use Java's immutable `String` API correctly, choose an equality contract, define empty and null behavior, and avoid accidental quadratic work.

This chapter starts at SDE-1 fundamentals. Later chapters add SDE-2 patterns only after the Java mechanics are dependable.

## 1.1 What a Java String is

A `String` is an object that represents an immutable sequence of UTF-16 code units. That sentence has two parts:

- **object:** a variable of type `String` stores a reference value or `null`; and
- **immutable sequence:** after a `String` object is created, its represented text does not change.

```java
String language = "Java";
String alias = language;
language = language.toUpperCase();

System.out.println(language); // JAVA
System.out.println(alias);    // Java
```

`toUpperCase()` did not edit the original object. It returned a `String` representing the result, and assignment changed only `language`. `alias` still refers to the earlier value.

![String references and immutable values](content/volumes/07-strings-and-string-patterns/assets/01-string-values-references-immutability.png)

Immutability is useful because a `String` value can be shared safely as a key, class name, path, or message without another caller changing its characters. Immutability does **not** make the variable final:

```java
String name = "Ada";
name = "Grace"; // legal: the variable now stores another reference value

final String fixedReference = "Ada";
// fixedReference = "Grace"; // compile-time error
```

`final` prevents reassignment of `fixedReference`. The referred `String` is already immutable regardless of `final`.

## 1.2 Creation: literals and constructors

Prefer a literal when the text is known in source code:

```java
String first = "interview";
String second = "interview";
String forcedCopy = new String("interview");
```

The JVM normally interns identical string literals used by the same runtime, so `first` and `second` commonly refer to the same pooled object. `new String(...)` requests a distinct object. These implementation-facing facts explain output questions, but they must not drive value comparison.

```java
System.out.println(first == second);       // true in this literal example
System.out.println(first == forcedCopy);   // false
System.out.println(first.equals(forcedCopy)); // true
```

The interview rule is stable:

| Question | Operation |
|---|---|
| Do these references identify the same object? | `first == second` |
| Do these strings contain the same sequence? | `first.equals(second)` |
| Can either value be null? | `Objects.equals(first, second)` |

Do not claim that every equal string exists only once. Runtime-created strings, constructor-created strings, and many API results need not share identity. `intern()` exposes the pool deliberately, but it is rarely appropriate in ordinary interview solutions.

## 1.3 Length, indexes, and the first traversal

`length()` returns the number of UTF-16 `char` units, not necessarily the number of visible symbols. For basic ASCII examples, one `char` corresponds to one character, so the first mental model is straightforward.

```java
static int countLetter(String text, char target) {
    int count = 0;
    for (int index = 0; index < text.length(); index++) {
        if (text.charAt(index) == target) {
            count++;
        }
    }
    return count;
}
```

For `countLetter("banana", 'a')`, indexes `0` through `5` are tested and the result is `3`. Valid indexes form `[0, text.length())`.

| `index` | `charAt(index)` | matches `a`? | `count` |
|---:|---|---|---:|
| 0 | `b` | no | 0 |
| 1 | `a` | yes | 1 |
| 2 | `n` | no | 1 |
| 3 | `a` | yes | 2 |
| 4 | `n` | no | 2 |
| 5 | `a` | yes | 3 |

An index equal to `length()` is outside the string and causes `StringIndexOutOfBoundsException`. A null reference causes `NullPointerException` before the index is considered.

## 1.4 The core read-only API

Learn a small, reliable toolkit before solving patterns:

```java
String text = "  Java Interview  ";

int units = text.length();
char firstUnit = text.charAt(0);
String piece = text.substring(2, 6);       // "Java"
boolean hasJava = text.contains("Java");
int firstView = text.indexOf("view");
int lastLetterA = text.lastIndexOf('a');
boolean beginsWithSpaces = text.startsWith("  ");
boolean endsWithSpaces = text.endsWith("  ");
String replaced = text.replace("Java", "SDE-2");
String stripped = text.strip();
char[] unitsCopy = text.toCharArray();
```

`substring(begin, end)` uses a half-open interval `[begin, end)`. The result length is `end - begin`. This convention matches loops, arrays, and most range algorithms.

Important distinctions:

- `contains` accepts a sequence; `indexOf` returns the first UTF-16 index or `-1`.
- `replace` treats its `CharSequence` arguments literally; `replaceAll` interprets a regular expression.
- `trim()` removes a historically limited set of leading/trailing characters; `strip()` uses Unicode-aware whitespace classification and is usually the clearer modern choice.
- `toCharArray()` creates a new mutable array of UTF-16 units. Mutating that array cannot mutate the original string.

## 1.5 Equality, ordering, and case

```java
String left = "Java";
String right = new String("Java");

System.out.println(left.equals(right));            // true
System.out.println(left.equalsIgnoreCase("JAVA")); // true
System.out.println("apple".compareTo("banana"));   // negative
```

`compareTo` performs lexicographic comparison over UTF-16 units and returns a negative, zero, or positive number. Do not promise exactly `-1`, `0`, or `1`; only the sign is contractual.

Case-insensitive comparison is a domain decision, not a universal cleanup step. Usernames, filesystem names, human-language words, and security identifiers may require different rules. `equalsIgnoreCase` is convenient for limited comparisons, but locale-sensitive transformation and Unicode normalization require an explicit policy introduced in Chapter 2.

## 1.6 Null, empty, and blank

These states are different:

```java
String absent = null; // no String object
String empty = "";    // zero UTF-16 units
String blank = " \t"; // nonempty, all whitespace
```

![Null, empty, and blank contracts](content/volumes/07-strings-and-string-patterns/assets/03-null-empty-blank-contract.png)

```java
static String requireVisibleText(String text) {
    if (text == null) {
        throw new IllegalArgumentException("text must not be null");
    }
    if (text.isBlank()) {
        throw new IllegalArgumentException("text must contain a non-whitespace character");
    }
    return text;
}
```

Choose one contract and state it. Returning `false`, returning an empty result, and rejecting null are all defensible in different APIs. Silently treating null as empty can erase information.

Use a safe order when null is permitted:

```java
if (text != null && !text.isEmpty()) {
    System.out.println(text.charAt(0));
}
```

Short-circuit `&&` prevents the method call when `text` is null.

## 1.7 Concatenation and conversion basics

The `+` operator creates a string result when either operand is a string:

```java
int attempts = 3;
String message = "attempts=" + attempts;
System.out.println(message); // attempts=3
```

Within one uncomplicated expression this is readable. Repeated `result = result + piece` inside a loop can repeatedly copy the growing prefix. Chapter 3 develops `StringBuilder` for that case.

Useful conversions:

```java
int number = Integer.parseInt("42");
String digits = String.valueOf(number);
String joined = String.join(",", "red", "green", "blue");
String formatted = "id=%d".formatted(number); // Java 15+
```

Parsing can throw `NumberFormatException`; it does not accept arbitrary whitespace or separators automatically. Define validation rather than assuming successful conversion.

## 1.8 What a String variable stores

The language-level model is enough for interviews:

```text
local variable -> a reference value or null
reference value -> may identify a String object
String object   -> immutable text value with Java-defined behavior
```

Do not claim that the variable contains the full object. Do not claim a universal object byte size. Layout, compression, headers, and internal representation are runtime concerns. The JVM book covers those implementation details.

Java is always pass-by-value. A string parameter receives a copy of a reference value:

```java
static void replace(String text) {
    text = "replacement";
}

String value = "original";
replace(value);
System.out.println(value); // original
```

Reassigning the local parameter does not reassign the caller's variable. Because strings are immutable, there is no operation that mutates the shared String object either.

## 1.9 Complexity baseline

State cost in terms of the operation's chosen text unit. For ordinary Java API discussion, `n` usually means UTF-16 units.

| Operation | Typical bound | Qualification |
|---|---:|---|
| `length()` | `O(1)` | returns UTF-16 unit count |
| `charAt(index)` | `O(1)` | valid UTF-16 index required |
| full traversal | `O(n)` | cost of processing each unit also matters |
| `equals` | `O(n)` worst case | may stop at first mismatch |
| `substring(begin, end)` | `O(k)` on current Java | result length `k`; do not depend on old shared-storage behavior |
| `indexOf(pattern)` | implementation-dependent search cost | use an explicit algorithm when a worst-case guarantee is required |
| concatenating total output length `m` once | `O(m)` | allocation and copying are real work |
| repeated growing concatenation | can be `O(m^2)` | depends on accumulated copied prefixes |

API complexity is a model, not a stopwatch reading. The Time and Space Complexity book explains how allocation, output, and amortized work are reported.

## 1.10 SDE-1 mistakes to eliminate

- comparing contents with `==`;
- calling a method before checking a nullable reference;
- using `<= text.length()` in a traversal;
- ignoring the return value of `replace`, `strip`, or `toUpperCase`;
- assuming `substring`'s end index is inclusive;
- assuming every equal value is the same pooled object;
- using `compareTo(...) == -1` instead of `< 0`;
- treating blank as the same state as empty;
- parsing without defining invalid-input behavior; and
- repeatedly concatenating a growing result inside a loop.

## 1.11 Interview angle

When asked a string question, state these before coding:

1. Can input be null or empty?
2. Is comparison case-sensitive?
3. Are spaces and punctuation significant?
4. Is ASCII guaranteed, or must Unicode be handled?
5. Are returned indexes UTF-16 indexes, code-point positions, or another unit?
6. May the input be normalized or copied?

A strong answer makes the text contract visible. The pattern comes afterward.

## 1.12 Quick check

1. Why does `toUpperCase()` not mutate its receiver?
2. What exactly does `==` compare for two `String` variables?
3. Why is `Objects.equals(a, b)` useful when either value may be null?
4. What range does `substring(2, 5)` select?
5. Why is `compareTo(other) < 0` safer than `== -1`?
6. How can a method receive a String reference yet remain pass-by-value?
7. Which distinction separates null, empty, and blank?

## 1.13 Foundation practice

**Foundation:** Implement `countOccurrences(String text, char target)` with a non-null contract. Test empty, one-unit, absent-target, and repeated-target cases.

**Interview Core:** Write `firstDifference(String first, String second)` that returns the first differing UTF-16 index, or the shorter length when one value is a prefix, or `-1` when equal. Define null behavior.

**SDE-2 Follow-up:** Design an API for comparing user identifiers. State whether case folding, normalization, whitespace, and locale belong inside the method or at a system boundary.
