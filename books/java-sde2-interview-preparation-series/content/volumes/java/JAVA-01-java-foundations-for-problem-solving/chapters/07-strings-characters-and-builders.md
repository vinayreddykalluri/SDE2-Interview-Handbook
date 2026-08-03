# Strings, Characters, and StringBuilder

Strings appear in almost every interview, but three different ideas are easy to mix up: the `String` object, the content it represents, and the `char` code units used by many Java APIs.

## String values and immutability

```java
String language = "Java";
String updated = language.toUpperCase();
```

`String` is immutable. `toUpperCase` does not modify `language`; it returns a string result. Reassignment can make a variable refer to another string, but the original object does not change.

Immutability enables safe sharing and stable hashing. It does not mean every string operation is allocation-free.

## `==` versus `equals`

```java
String first = new String("java");
String second = new String("java");

System.out.println(first == second);      // false
System.out.println(first.equals(second)); // true
```

`==` compares reference identity. `equals` compares string content.

String literals may share pooled instances:

```java
String a = "pool";
String b = "pool";
System.out.println(a == b); // normally true by literal interning semantics
```

Do not turn that into an identity-comparison strategy. Runtime-created strings and explicit `new String(...)` do not have to be the same object as a pooled literal. Use `equals` for content.

## Essential String operations

| Operation | Purpose |
|---|---|
| `length()` | number of UTF-16 code units |
| `charAt(index)` | code unit at an index |
| `substring(start, end)` | half-open slice |
| `equals`, `equalsIgnoreCase` | content comparison |
| `compareTo` | lexicographic comparison |
| `contains`, `indexOf`, `lastIndexOf` | search |
| `startsWith`, `endsWith` | boundary test |
| `replace` | replacement result |
| `split` | regular-expression split |
| `trim`, `strip` | surrounding-space handling |
| `toCharArray` | mutable code-unit copy |

`split` accepts a regular expression. `text.split(".")` does not mean split on a literal dot; use an escaped expression such as `"\\."`. Trailing empty results are discarded by default; use a negative limit when they matter.

## Empty, blank, and null are different

```java
String empty = "";       // length zero
String blank = "   ";    // nonzero length, only whitespace
String missing = null;    // no String object
```

- `empty.isEmpty()` is true.
- `blank.isBlank()` is true on Java 11+.
- calling either method on `missing` throws `NullPointerException`.

Choose a domain contract rather than silently treating all three as equivalent.

## Why repeated concatenation can be expensive

```java
String result = "";
for (String word : words) {
    result = result + word;
}
```

Each result is a new immutable string and repeated copying can make total work quadratic in the final text size.

Use a builder for incremental assembly:

```java
StringBuilder builder = new StringBuilder();
for (String word : words) {
    builder.append(word);
}
String result = builder.toString();
```

Useful builder operations include `append`, `insert`, `delete`, `setCharAt`, `reverse`, and `toString`. `StringBuilder` is mutable and not thread-safe; normal interview code keeps it local to one method.

## `char` and digit conversion

A `char` is a UTF-16 code unit. For an ASCII decimal digit:

```java
char character = '7';
int digit = character - '0';
```

This is safe only after proving `character` is between `'0'` and `'9'`.

For a radix-aware library conversion:

```java
int digit = Character.digit(character, 10);
if (digit < 0) {
    throw new IllegalArgumentException("not a decimal digit");
}
```

`Character.isDigit`, `isLetter`, `isWhitespace`, `toLowerCase`, and `toUpperCase` are useful, but case conversion can be language-sensitive at whole-string boundaries.

## Code units, code points, and visible characters

Many common English letters use one `char`. Some Unicode code points, including many emoji, use a surrogate pair: two `char` values. A user-perceived character can contain more than one code point.

At the fundamentals level, remember:

- `String.length()` counts UTF-16 code units;
- `codePointCount` counts Unicode code points;
- neither automatically counts every user-perceived grapheme.

The Strings book owns algorithmic string patterns, and Advanced Java owns deeper Unicode boundary handling.

## Complete example

File: `StringsAndCharactersExample.java`

```java
public final class StringsAndCharactersExample {
    static String reverseAscii(String input) {
        StringBuilder result = new StringBuilder(input.length());
        for (int index = input.length() - 1; index >= 0; index--) {
            result.append(input.charAt(index));
        }
        return result.toString();
    }

    public static void main(String[] args) {
        String first = new String("java");
        String second = new String("java");
        char character = '7';

        System.out.println(first == second);
        System.out.println(first.equals(second));
        System.out.println(character - '0');
        System.out.println(reverseAscii("SDE2"));
    }
}
```

Expected output:

```text
false
true
7
2EDS
```

The method is deliberately named `reverseAscii`: reversing UTF-16 code units is not a universal Unicode grapheme reversal.

## Edge-case matrix

| Case | Risk | Required decision |
|---|---|---|
| null input | dereference | reject, accept, or return a defined result |
| empty input | assumes first character | return the natural empty result |
| blank input | confused with empty | use the domain's whitespace policy |
| comparison with `==` | identity, not content | use `equals` or null-safe equality |
| loop concatenation | repeated copying | use a local `StringBuilder` |
| `split` delimiter | regex metacharacter | escape and choose a limit |
| `char - '0'` | invalid non-digit input | validate the range first |
| supplementary code point | two `char` values | use code-point APIs when required |
| locale-sensitive case | wrong normalization rule | define locale/protocol requirements |

## Interview room

**Interviewer:** Why is `first == second` unreliable for string content?

**Model answer:** For reference operands, `==` asks whether both references designate the same object. Literals can share a pooled instance, so identity can appear to match. `new String` and runtime results can be different objects with equal content. `String.equals` is the content contract.

**Follow-up:** When is `StringBuilder` useful?

**Model answer:** When building text incrementally, especially in a loop. It maintains mutable storage and avoids creating an immutable intermediate string on each append. A single clear concatenation expression does not need a builder by ritual.

## Practice

1. **Foundation:** Compare two strings safely when either may be null.
2. **Foundation:** Count ASCII digits after validating each `char`.
3. **Predict:** Compare literals, `new String`, and a concatenated runtime string with both `==` and `equals`.
4. **Debugging:** Repair a loop that builds a large string using repeated `+`.
5. **Interview Core:** Normalize consecutive spaces without losing non-space characters.
6. **SDE-2 Follow-up:** Define whether an identifier policy works in bytes, code units, code points, or user-perceived characters.

## Chapter takeaway

Strings are immutable values reached through references. Compare their content, use a builder for incremental assembly, validate character arithmetic, and state whether your algorithm operates on code units, code points, or a narrower input alphabet.
