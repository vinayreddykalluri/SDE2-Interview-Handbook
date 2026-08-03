# Methods and Java's Pass-by-Value Rule

A method gives a useful operation a name. In interview code, a good helper makes an invariant visible and lets the main solution read like a sequence of decisions.

## The smallest useful method

```java
static int larger(int left, int right) {
    if (left >= right) {
        return left;
    }
    return right;
}
```

- `static` lets the method be called without creating an object; instance methods come later.
- `int` before the name is the return type.
- `left` and `right` are parameters.
- `return` supplies the result and exits the method.

At a call site, `larger(4, 9)`, the expressions `4` and `9` are arguments.

## Parameters, arguments, and local scope

Parameters are local variables initialized for one invocation. Variables declared inside the method are also local to that invocation.

```java
static int calculateSum(int[] numbers) {
    int sum = 0;
    for (int number : numbers) {
        sum += number;
    }
    return sum;
}
```

The method's contract should answer:

- Are null or empty inputs allowed?
- Can the method mutate the input?
- What does it return?
- Which failures can occur?
- What is its time and space cost?

## `void` methods

A `void` method produces no return value:

```java
static void printSeparator() {
    System.out.println("---");
}
```

It can still use `return;` to exit early. Prefer returning computed information rather than printing from every helper; returned values are easier to test and reuse.

## Java always passes argument values by value

This wording matters. Java has no pass-by-reference parameter mode.

### Primitive argument

```java
static void increment(int number) {
    number++;
}

int callerNumber = 7;
increment(callerNumber);
System.out.println(callerNumber); // 7
```

The parameter receives a copy of the primitive value. Updating that local copy cannot change the caller's variable.

### Reference argument and shared mutation

```java
static final class Counter {
    int value;
}

static void increment(Counter counter) {
    counter.value++;
}
```

The parameter receives a copy of the reference value. Caller and callee can use their reference copies to reach the same object, so mutation of that object is visible.

```text
caller variable ----+
                    +----> Counter{value=1}
parameter copy  ----+
```

### Reassigning the parameter

```java
static void replace(Counter counter) {
    counter = new Counter();
    counter.value = 99;
}
```

Reassignment changes only the callee's local parameter. It does not point the caller's variable at the new object.

## Arrays follow the same rule

An array variable holds a reference. Passing it copies that reference value:

```java
static void markFirst(int[] values) {
    values[0] = -1;       // mutates the shared array
    values = new int[0];  // reassigns only the parameter
}
```

Document mutation. A name such as `sortInPlace` is more honest than `sort` when the input changes.

## Method overloading

Overloading uses the same method name with a different parameter list:

```java
static int clamp(int value, int minimum, int maximum) { ... }
static long clamp(long value, long minimum, long maximum) { ... }
```

Return type alone cannot distinguish overloads. Overload selection occurs at compile time. At the fundamentals level, prefer overloads whose intent remains obvious; complex boxing/varargs resolution belongs in the Advanced Java Language book.

## Varargs as a convenient array

```java
static int sum(int... values) {
    int total = 0;
    for (int value : values) {
        total += value;
    }
    return total;
}
```

`int...` is represented as `int[]`. Only the final parameter may be variable-arity. The caller can supply separate arguments or an existing array. Treat the received array according to an explicit ownership contract.

## Recursion preview

```java
static long factorial(int number) {
    if (number < 0) {
        throw new IllegalArgumentException("number must be nonnegative");
    }
    if (number <= 1) {       // base case
        return 1;
    }
    return number * factorial(number - 1); // progress toward base case
}
```

Every recursive method needs a base case and progress. Each unfinished call consumes invocation state; excessive depth can throw `StackOverflowError`. Full recursive problem solving belongs in the Recursion book.

## Complete example

File: `MethodsAndPassByValueExample.java`

```java
public final class MethodsAndPassByValueExample {
    static final class Person {
        String name;

        Person(String name) {
            this.name = name;
        }
    }

    static void rename(Person person) {
        person.name = "Updated";
    }

    static void replace(Person person) {
        person = new Person("Replacement");
    }

    public static void main(String[] args) {
        Person person = new Person("Original");
        rename(person);
        System.out.println(person.name);
        replace(person);
        System.out.println(person.name);
    }
}
```

Expected output:

```text
Updated
Updated
```

The first call mutates the shared object. The second reassigns only its local parameter.

## Edge-case matrix

| Case | Mistake | Better contract |
|---|---|---|
| primitive parameter | expects caller value to change | return the new value |
| reference parameter | calls it pass-by-reference | say a reference value is copied |
| array helper | silently mutates input | name/document mutation or return a copy |
| return type only differs | tries to overload | change the parameter list or method name |
| varargs input | assumes a fresh array always exists | caller may pass an existing array |
| recursive call | no base case or no progress | state both before coding |
| deep recursion | ignores stack limit | use an iterative form when appropriate |

## Interview room

**Interviewer:** Is Java pass-by-reference for objects?

**Model answer:** No. Java always passes values by value. For an object argument, the value being copied is a reference. Both copies can designate the same object, which explains visible mutation, but reassigning the parameter cannot reassign the caller's variable.

**Follow-up:** Can a method swap two caller variables?

**Model answer:** Not by reassigning two parameters. It can mutate a shared container or return the swapped values, but Java does not pass the caller's variable locations.

## Practice

1. **Foundation:** Write a method with two parameters and an integer result.
2. **Predict:** Trace a primitive parameter, a mutable object parameter, and parameter reassignment.
3. **Debugging:** Repair a method that tries to swap two `String` variables by reassigning parameters.
4. **Interview Core:** Write `reverseCopy(int[])` without mutating the input.
5. **SDE-2 Follow-up:** Design a method contract stating nullability, mutation, result ownership, failure, and complexity.

## Chapter takeaway

Every parameter is initialized from a copied argument value. Separate mutation of a shared object from reassignment of a local parameter, and use small helpers whose contracts are easy to test and explain.
