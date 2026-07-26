# Chapter 4: Octal and Hexadecimal Essentials

Octal and hexadecimal are compact ways to write binary-aligned values. Octal groups binary digits in sets of three because `8 = 2^3`. Hexadecimal groups them in sets of four because `16 = 2^4`.

Most coding interviews do not require deep octal knowledge. They do expect you to recognize Java literals, read hexadecimal values during debugging, and use a radix-aware conversion API correctly.

## Learning objectives

By the end of this chapter, you should be able to:

- read octal and hexadecimal place values;
- use Java literal and parsing syntax without confusing source text and input text;
- convert small values to and from decimal;
- explain practical uses for hexadecimal and limited uses for octal; and
- validate and format bounded values with clear width rules.

## Place values and digit sets

Octal uses digits `0` through `7`. Hexadecimal uses `0` through `9` followed by `A` through `F`, where `A` represents 10 and `F` represents 15.

| Decimal | Binary | Octal | Hexadecimal |
|---:|---:|---:|---:|
| 0 | 0000 | 0 | 0 |
| 1 | 0001 | 1 | 1 |
| 7 | 0111 | 7 | 7 |
| 8 | 1000 | 10 | 8 |
| 10 | 1010 | 12 | A |
| 15 | 1111 | 17 | F |
| 16 | 10000 | 20 | 10 |

Hexadecimal `2F` means `2 * 16 + 15`, or decimal 47. Octal `755` means `7 * 64 + 5 * 8 + 5`, or decimal 493.

The general positional accumulation rule still applies:

```text
next = current * base + digit
```

## Recognition signals

Hexadecimal commonly appears in:

- debugger and heap-dump views;
- byte values and protocol fields;
- masks and packed flags;
- Unicode code-point notation such as `U+1F600`;
- color values such as `#336699`;
- hash displays and identifiers; and
- low-level logs that show unsigned bit patterns.

Octal appears less often. Common examples are Unix-style permission notation, compact legacy data, and source literals in existing Java code. In mainstream DSA interviews, know the representation and APIs, then move on.

## Java source literals

Java integer literals use prefixes:

```java
int decimal = 47;
int binary = 0b0010_1111;
int octal = 057;
int hexadecimal = 0x2F;
```

All four variables contain the same mathematical value. The prefix is source syntax, not stored type information.

The leading-zero octal form is a common review trap:

```java
int month = 010; // decimal 8, not decimal 10
```

Avoid padding decimal integer literals with a leading zero. If fixed-width formatting matters, keep the value numeric and format it when producing text.

Hex digits are case-insensitive in literals and parsers: `0x2f` and `0x2F` have the same value. Uppercase is often easier to scan in documentation, but consistency matters more than case.

## Source literals are not runtime parsing

`Integer.parseInt` does not infer octal from a leading zero or hexadecimal from `0x` when called without a radix:

```java
Integer.parseInt("010");      // decimal 10
Integer.parseInt("10", 8);   // decimal 8
Integer.parseInt("2F", 16);  // decimal 47
```

The radix-specific form expects digits without a source prefix. `Integer.parseInt("0x2F", 16)` fails because `x` is not a hexadecimal digit.

`Integer.decode` recognizes prefixes such as `0x`, `#`, and a leading zero, but implicit prefix rules can be surprising at input boundaries. Prefer an explicit contract and explicit radix unless decoding Java-style configuration text is the actual requirement.

## A bounded conversion example

This complete Java class parses and formats byte-sized hexadecimal values and parses three-digit octal permissions. The methods state their accepted ranges instead of silently truncating.

```java
public final class RadixEssentials {
    private RadixEssentials() {}

    public static int parseHexByte(String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("hex byte must be nonempty");
        }
        int value;
        try {
            value = Integer.parseInt(text, 16);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid hexadecimal byte", exception);
        }
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("hex byte must be 00 through FF");
        }
        return value;
    }

    public static String formatHexByte(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("byte value out of range");
        }
        return "%02X".formatted(value);
    }

    public static int parseOctalPermissions(String text) {
        if (text == null || !text.matches("[0-7]{3}")) {
            throw new IllegalArgumentException("expected three octal digits");
        }
        return Integer.parseInt(text, 8);
    }

    public static void main(String[] args) {
        System.out.println(parseHexByte("2f"));       // 47
        System.out.println(formatHexByte(47));        // 2F
        System.out.println(parseOctalPermissions("755")); // 493
    }
}
```

For an interview platform, a regex may be unnecessary overhead for three characters; a loop with `Character.digit(current, 8)` gives exact error positions and avoids a regular-expression dependency. The example uses the concise form because the octal contract is small and fixed.

### Dry run: hexadecimal `2F`

Start with zero. Reading `2` gives `0 * 16 + 2 = 2`. Reading `F` gives digit 15, so the next value is `2 * 16 + 15 = 47`.

### Dry run: octal `755`

Start with zero:

```text
read 7: 0 * 8 + 7 = 7
read 5: 7 * 8 + 5 = 61
read 5: 61 * 8 + 5 = 493
```

For k input digits, parsing is O(k) time and O(1) auxiliary space for a fixed-width primitive result. Formatting produces O(k) output.

## Binary grouping intuition

Hex is readable because one hex digit maps exactly to four bits:

```text
2       F
0010    1111
```

Thus `0x2F` corresponds to binary `0010 1111`. Octal digit 7 maps to three bits `111`, so octal `57` corresponds to binary `101 111`.

Grouping is a reasoning aid, not a requirement to build intermediate binary strings. Direct positional accumulation is simpler for general conversion code.

## Signed values and bit-pattern displays

`Integer.toHexString(-1)` returns `"ffffffff"`, the unsigned textual form of the 32-bit pattern. It does not return `"-1"`. In contrast, `Integer.toString(-1, 16)` returns `"-1"`, a signed mathematical representation.

This distinction is important in logs:

```java
String signed = Integer.toString(-1, 16); // "-1"
String bits = Integer.toHexString(-1);    // "ffffffff"
```

Choose the operation that matches the meaning. A signed quantity and a raw bit pattern should not share an ambiguous formatter.

## Character conversion APIs

For bases through 36, Java maps letters to digit values:

```java
int digit = Character.digit('F', 16); // 15
char symbol = Character.forDigit(15, 16); // 'f'
```

`Character.digit` returns `-1` when the character is not valid in the requested radix. Always check that result. `Character.forDigit` returns a lowercase letter for values above nine and returns the null character for invalid inputs; validate the digit and radix first.

## Common candidate mistakes

- Reading a leading-zero Java literal as decimal.
- Passing a `0x` prefix to `parseInt(text, 16)` without removing or allowing it by contract.
- Accepting digits `8` or `9` in octal.
- Treating hexadecimal text as case-sensitive without a reason.
- Confusing signed formatting with a fixed-width bit-pattern display.
- Truncating a value with `& 0xFF` when the contract requires range validation.
- Using octal where decimal would be clearer to a reviewer.
- Claiming a color or Unicode example proves how Java stores every related object internally.

## Interview follow-up questions

An interviewer may ask you to support an optional prefix, preserve an exact width, parse a 64-bit unsigned pattern, reject lowercase digits, stream a large hex value modulo a constant, or convert directly between binary and hex. Clarify whether the input represents a signed quantity or raw bits before choosing an API.

## Chapter summary

Octal groups binary positions in threes; hexadecimal groups them in fours. Java source literals use `0` and `0x` prefixes, while runtime parsing should normally use an explicit radix and prefix contract. Hexadecimal is useful for compact bit-aligned displays; octal has narrower uses. Keep signed quantities separate from raw fixed-width patterns.

## Quick Check

1. What decimal value does hexadecimal `2F` represent?
2. Why is Java literal `010` a maintenance risk?
3. Does `Integer.parseInt("010")` parse octal by default?
4. What is the difference between `Integer.toString(-1, 16)` and `Integer.toHexString(-1)`?
5. What does `Character.digit('G', 16)` return?

## Coding Practice

1. **Foundation:** Convert a two-digit hexadecimal byte string to decimal with validation.
2. **Foundation:** Format values 0 through 255 as exactly two uppercase hex digits.
3. **Interview Core:** Parse an octal string manually without `parseInt`.
4. **Interview Core:** Convert a hexadecimal string to decimal `long` with overflow checks.
5. **SDE-2 Follow-up:** Parse an RGB color in `#RRGGBB` form into three integer components.
6. **Challenge:** Convert a huge hexadecimal string to its remainder modulo a positive integer.

## Debugging Task

**Interview Core:** Identify the prefix, validation, and range assumptions that make this method unreliable.

```java
static int parseHex(String text) {
    return Integer.parseInt(text.substring(2), 16);
}
```

Test null, empty input, missing prefix, lowercase prefix, signs, invalid digits, and values above `Integer.MAX_VALUE`.

## Interview Extension

**SDE-2 Follow-up:** Define separate APIs for a signed hexadecimal number and an exact 32-bit hexadecimal pattern. Specify prefix, width, case, leading-zero, sign, and overflow behavior, then explain why one combined parser would make the contract harder to reason about.
