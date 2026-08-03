# Values, Variables, Types, and Literals

This is where Java starts to feel predictable. A variable has a type, the type limits which values it can hold, and the spelling of a literal determines how Java begins evaluating an expression.

> **A note from Vinay:** When a candidate says, "I knew the algorithm, but Java gave the wrong number," the cause is often in this chapter: an uninitialized local, the wrong primitive, or arithmetic that overflowed before it reached a `long`.

## Start with one value

```java
int solvedProblems = 12;
```

Read this from right to left:

- `12` is an integer literal.
- `int` is the declared type.
- `solvedProblems` is the variable name.
- `=` initializes the variable with the value.

Later assignment changes the value stored in that variable:

```java
solvedProblems = 13;
```

Use a name that carries meaning. `solvedProblems` is easier to review than `x` unless the variable is a conventional short-lived index.

## The eight primitive types

| Type | Width | Value model | Common interview use |
|---|---:|---|---|
| `byte` | 8 bits | signed integer | encoded data; rarely arithmetic |
| `short` | 16 bits | signed integer | uncommon in DSA code |
| `int` | 32 bits | signed integer | indexes, counts, ordinary values |
| `long` | 64 bits | signed integer | sums, products, timestamps, wide ranges |
| `float` | 32 bits | binary floating point | uncommon in interview algorithms |
| `double` | 64 bits | binary floating point | averages and approximate measurements |
| `char` | 16 bits | UTF-16 code unit | basic character processing |
| `boolean` | JVM-defined storage, language values `true`/`false` | logical state | predicates and flags |

Important ranges:

```text
byte  : -128 to 127
short : -32,768 to 32,767
int   : -2^31 to 2^31 - 1
long  : -2^63 to 2^63 - 1
char  : 0 to 65,535
```

Java specifies these value ranges. It does not promise that every local variable occupies exactly its primitive width in a physical stack frame.

## Primitive values and references are different

```text
int score = 90;                  score stores the value 90
String name = new String("Ada"); name stores a reference value
```

A reference can designate an object or be `null`. It does not contain the entire object. Copying a primitive copies its value. Copying a reference copies the reference value, so both variables can designate the same object.

## Declaration, initialization, assignment, and scope

```java
public class ScopeExample {
    private int completed;          // field: default value 0

    int next(boolean includeBonus) {
        int bonus;                  // local: no automatic usable default
        if (includeBonus) {
            bonus = 1;
        } else {
            bonus = 0;
        }
        int result = completed + bonus;
        return result;
    }
}
```

Fields and array elements receive default values. Local variables must be definitely assigned on every path before Java allows a read.

Scope is the region where a name is available. Prefer the smallest useful scope; a loop counter normally belongs inside the loop declaration.

## Literals you must recognize

```java
int decimal = 42;
long distance = 3_000_000_000L;
double ratio = 0.75;
float sample = 0.75F;
char grade = 'A';
String topic = "arrays";
boolean ready = true;
int binary = 0b101010;
int hexadecimal = 0x2A;
```

Uppercase `L` and `F` make the literal type visible. Underscores improve readability but do not change the value.

## Why overflow can happen before assignment

```java
long incorrect = 100_000 * 100_000;
long correct = 100_000L * 100_000;
```

In the first line both operands are `int`, so Java performs 32-bit multiplication first. The wrapped `int` result is widened only afterward. In the second line one operand is `long`; numeric promotion makes the multiplication itself 64-bit.

## Complete example

File: `ValuesAndTypesExample.java`

```java
public final class ValuesAndTypesExample {
    public static void main(String[] args) {
        int solvedProblems = 12;
        long safeProduct = 100_000L * 100_000;
        char digitCharacter = '7';
        boolean ready = solvedProblems >= 10;

        System.out.println(solvedProblems);
        System.out.println(safeProduct);
        System.out.println(digitCharacter);
        System.out.println(ready);
    }
}
```

Expected output:

```text
12
10000000000
7
true
```

The matching executable companion also asserts minimum and maximum primitive values and the difference between a field default and local definite assignment.

## Edge-case matrix

| Case | What Java does | Interview response |
|---|---|---|
| unassigned local is read | compile-time error | initialize every reachable path |
| field is not explicitly assigned | receives a specified default | do not confuse this with local variables |
| integer result exceeds range | wraps without an exception | widen before arithmetic or validate |
| `final` reference points to mutable object | reference cannot change; object might | `final` is not deep immutability |
| reference is `null` | no object is designated | validate before dereference |
| `char` holds an emoji | many emoji need two code units | do not promise one visible character per `char` |

## Interview room

**Interviewer:** Why does assigning an expression to `long` not guarantee safe arithmetic?

**Model answer:** Assignment conversion occurs after the expression is evaluated. If every operand is `int`, operations such as multiplication happen as `int` and may overflow first. I make an operand `long`, for example `left * (long) right`, when the operation needs a 64-bit range.

**Follow-up:** Do local variables receive zero by default?

**Model answer:** No. Fields and array elements are default-initialized. A local variable must be definitely assigned before it is read, or compilation fails.

## Practice

1. **Foundation:** Declare one variable of every primitive type and print it.
2. **Foundation:** Predict the value of `long result = Integer.MAX_VALUE + 1;`.
3. **Interview Core:** Repair an `int` overflow in the area of a large rectangle.
4. **Debugging:** Explain why `int value; System.out.println(value);` does not compile inside a method.
5. **SDE-2 Follow-up:** Define the input range that makes `int` safe for a count and explain when you would still choose `long`.

## Chapter takeaway

Choose a type from the value domain, not habit. Remember that expression types determine arithmetic before assignment, locals need definite assignment, and references designate objects rather than containing them.
