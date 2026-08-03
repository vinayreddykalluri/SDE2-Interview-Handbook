# Operators, Conversions, and Expressions

An expression is a small program: Java evaluates operands, applies conversions, performs the operation, and produces a value. Interview output questions become straightforward when you trace those four steps.

## Arithmetic without surprises

```java
int quotient = 7 / 2;       // 3
double precise = 7 / 2.0;   // 3.5
int remainder = -7 % 3;     // -1
```

Integer division truncates toward zero. Integral division by zero throws `ArithmeticException`; floating-point division by zero can produce infinity or NaN.

`+`, `-`, `*`, `/`, and `%` do not automatically check integral overflow.

## Numeric promotion

Arithmetic on `byte`, `short`, and `char` normally promotes operands to `int`:

```java
byte left = 10;
byte right = 20;
int sum = left + right;
// byte invalid = left + right; // compile-time error: the expression is int
```

Binary numeric promotion chooses `double`, then `float`, then `long`, otherwise `int`, based on the operands.

## Widening and narrowing

```java
int count = 10;
long total = count;          // widening

double measurement = 10.8;
int truncated = (int) measurement; // narrowing: 10
```

Widening usually needs no cast, but widening to floating point can lose precision. Narrowing needs a cast and may discard bits, truncate a fraction, or change the mathematical value.

The order of a cast matters:

```java
long correct = (long) Integer.MAX_VALUE + 1;
long incorrect = (long) (Integer.MAX_VALUE + 1);
```

The inner addition in the second line already overflowed.

## Comparison and equality

`<`, `<=`, `>`, and `>=` compare numeric values. `==` and `!=` compare primitive values or reference identity.

```java
String first = new String("java");
String second = new String("java");

System.out.println(first == second);      // false: distinct objects
System.out.println(first.equals(second)); // true: same content
```

Use `equals` or `Objects.equals` for object value comparison when the type defines that meaning.

## Logical operators and short-circuiting

```java
if (text != null && !text.isEmpty()) {
    System.out.println(text);
}
```

If `text` is null, `&&` skips the right operand, preventing a dereference. `&` on booleans evaluates both operands. Similarly, `||` can skip its right operand while `|` does not.

## Prefix and postfix

```java
int value = 5;
int before = value++; // before=5, value=6
int after = ++value;  // value=7, after=7
```

Both mutate the variable. Postfix produces the old value; prefix produces the updated value. Keep increments out of larger expressions when clarity matters.

## Compound assignment hides a cast

```java
short value = 10;
value += 5;
```

This behaves roughly like:

```java
value = (short) (value + 5);
```

The left side is evaluated only once, but the implicit narrowing can hide overflow.

## Ternary expressions

```java
int absolute = number >= 0 ? number : -number;
```

Use a ternary when it clearly selects one of two values. Nested ternaries usually make interview code harder to discuss. Remember that negating `Integer.MIN_VALUE` still overflows.

## Precedence worth remembering

| Higher to lower | Operators |
|---|---|
| unary | `!`, unary `+`, unary `-`, `++`, `--` |
| multiplicative | `*`, `/`, `%` |
| additive | `+`, `-` |
| comparison | `<`, `<=`, `>`, `>=` |
| equality | `==`, `!=` |
| conditional AND/OR | `&&`, `||` |
| ternary | `?:` |
| assignment | `=`, `+=`, `-=`, and others |

Use parentheses when the grouping is not obvious. Interviewers value readable intent more than memorization of the complete precedence table.

## Complete example

File: `OperatorsAndConversionsExample.java`

```java
public final class OperatorsAndConversionsExample {
    public static void main(String[] args) {
        int maximum = Integer.MAX_VALUE;
        long correct = (long) maximum + 1;
        double average = (8 + 9) / 2.0;
        String text = null;
        boolean safe = text != null && !text.isEmpty();

        System.out.println(correct);
        System.out.println(average);
        System.out.println(safe);
    }
}
```

Expected output:

```text
2147483648
8.5
false
```

## Edge-case matrix

| Case | Failure | Correction |
|---|---|---|
| `5 / 2` expected as `2.5` | both operands are integral | make one operand floating point |
| cast after multiplication | overflow already happened | cast an operand before arithmetic |
| `value != null & value.isEmpty()` | right side still executes | use `&&` for a null guard |
| compare object contents with `==` | tests identity | use the type's value equality |
| comparator returns `left - right` | subtraction can overflow | use `Integer.compare(left, right)` |
| `Math.abs(Integer.MIN_VALUE)` | result remains negative | define a wider or exceptional contract |

## Interview room

**Interviewer:** Why does `short value = 10; value += 5;` compile while `value = value + 5;` does not?

**Model answer:** `value + 5` is promoted to `int`, so assigning it directly to `short` needs an explicit cast. Compound assignment includes an implicit conversion back to the left-side type and evaluates the left side once. That convenience can also hide narrowing.

**Follow-up:** What is the difference between `&&` and `&` for booleans?

**Model answer:** Both combine boolean values, but `&&` conditionally skips the right operand when the left is false. `&` always evaluates both operands. I use `&&` for guard conditions unless both evaluations are deliberately required.

## Practice

1. **Foundation:** Predict `-7 / 3` and `-7 % 3`.
2. **Foundation:** Convert `double 19.99` to `int` and explain the lost information.
3. **Interview Core:** Repair overflow in `long area = width * height` when both inputs are `int`.
4. **Predict the output:** Trace `int x = 3; int y = x++ + ++x;`, then rewrite it clearly.
5. **SDE-2 Follow-up:** Define how an API should react when an exact arithmetic result does not fit its return type.

## Chapter takeaway

Trace operand types before the operation, then the result, then assignment. That single habit explains promotion, integer division, overflow, casting, short-circuiting, and most output questions in this area.
