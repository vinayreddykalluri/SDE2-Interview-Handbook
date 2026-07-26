# Forty Java Interview Traps

These are behavior questions, not trivia contests. For each one: predict before running, identify the language or API rule, repair the code, and state the boundary an interviewer could change.

## 1. `==` versus `equals`

```java
String left = new String("java");
String right = new String("java");
System.out.println(left == right);
System.out.println(left.equals(right));
```

Predicted/actual output: `false`, then `true`. `==` compares reference identity; `equals` compares string content. Correct value test: `Objects.equals(left, right)` when either value may be null. Follow-up: what contract must a custom key's `equals` satisfy?

## 2. String-pool assumptions

```java
String first = "sde2";
String second = "sde" + "2";
System.out.println(first == second);
```

Actual output is `true` because this constant expression is folded and the literal may share the pooled instance. Do not infer that all equal strings have one object. Correct comparison: `first.equals(second)`. Follow-up: what changes if part of the concatenation is a variable?

## 3. `new String(...)`

```java
String pooled = "java";
String explicit = new String("java");
System.out.println(pooled == explicit);
```

Actual output: `false`. The constructor creates a distinct `String` object even though it is content-equal to the literal. Correct value test: `pooled.equals(explicit)`. Follow-up: what does `explicit.intern()` return?

## 4. Java pass-by-value

```java
static void replace(StringBuilder value) {
    value = new StringBuilder("new");
}
StringBuilder caller = new StringBuilder("old");
replace(caller);
System.out.println(caller);
```

Actual output: `old`. The parameter receives a copy of the reference value; reassigning that copy does not reassign `caller`. Mutation such as `value.append("!")` would affect the shared object. Follow-up: draw both variables before and after reassignment.

## 5. Primitive overflow

```java
int maximum = Integer.MAX_VALUE;
System.out.println(maximum + 1);
```

Actual output: `-2147483648`. `int` arithmetic wraps to the defined 32-bit two's-complement result; it does not widen automatically or throw. Correct when overflow is invalid: `Math.addExact(maximum, 1)`. Follow-up: when is modular wraparound intentional?

## 6. Overflow before assignment to `long`

```java
long wrong = 100_000 * 100_000;
long correct = 100_000L * 100_000;
System.out.println(wrong + " " + correct);
```

Actual output: `1410065408 10000000000`. Both operands of the first product are `int`, so overflow happens before assignment. Widen an operand before the operation. Follow-up: why is `(long) (a * b)` still too late?

## 7. Integer division

```java
System.out.println(5 / 2);
System.out.println(5 / 2.0);
```

Actual output: `2`, then `2.5`. Integer division truncates toward zero. Correct an average by widening before division: `(double) sum / count`. Follow-up: what is `-5 / 2`?

## 8. Floating-point equality

```java
System.out.println(0.1 + 0.2 == 0.3);
```

Actual output: usually `false` by specified binary floating-point evaluation of these literals. Compare within a domain-defined tolerance, or use exact decimal/integer units when the domain requires them. Follow-up: why is one universal epsilon incorrect?

## 9. Narrowing conversion

```java
double value = 10.8;
int whole = (int) value;
System.out.println(whole);
```

Actual output: `10`. The cast truncates toward zero; it does not round. Correct rounding may use `Math.round` with explicit range handling. Follow-up: what happens for a value outside the `int` range?

## 10. Prefix versus postfix increment

```java
int value = 5;
int first = value++;
int second = ++value;
System.out.println(value + " " + first + " " + second);
```

Actual output: `7 5 7`. Postfix yields the old value; prefix increments before yielding. Correct interview style separates increments from larger expressions. Follow-up: predict `numbers[index++] = index` and explain evaluation order.

## 11. Short-circuit evaluation

```java
String text = null;
System.out.println(text != null && !text.isEmpty());
```

Actual output: `false`; the second operand is not evaluated. Replacing `&&` with boolean `&` evaluates both sides and throws. Correct guard: cheapest/safest prerequisite first. Follow-up: how can short-circuiting avoid an out-of-bounds read?

## 12. Null unboxing

```java
Integer count = null;
// int value = count;
```

If reached, the assignment throws `NullPointerException` during unboxing. Correct by rejecting null, defining a default, or keeping absence explicit. Follow-up: why can `map.get(key) == 0` throw?

## 13. Integer wrapper caching

```java
Integer a = 127, b = 127;
Integer c = 128, d = 128;
System.out.println(a == b);
System.out.println(c == d);
```

The first is `true` for the required cache; the second identity result must not be used as a value contract and is commonly `false`. Correct: `a.equals(b)` and `c.equals(d)`. Follow-up: why may a larger implementation cache not improve correctness?

## 14. `Math.abs(Integer.MIN_VALUE)`

```java
System.out.println(Math.abs(Integer.MIN_VALUE));
```

Actual output remains `-2147483648`: the positive magnitude is not representable as `int`. Correct: widen first with `Math.abs((long) value)` or use `Math.absExact` when overflow must throw. Follow-up: can `Math.abs` be used safely as a hash bucket index?

## 15. `Math.abs(Long.MIN_VALUE)`

```java
System.out.println(Math.abs(Long.MIN_VALUE));
```

Actual output remains negative for the same asymmetry at 64 bits. `Math.absExact(long)` throws on this input; `BigInteger` handles a larger exact magnitude. Follow-up: design a non-negative modulo-based index without `abs`.

## 16. Array aliasing

```java
int[] first = {1, 2};
int[] alias = first;
alias[0] = 9;
System.out.println(first[0]);
```

Actual output: `9`. Assignment copies the array reference, not its elements. Correct independent storage: `int[] copy = first.clone()`. Follow-up: does cloning a two-dimensional array copy nested rows?

## 17. Shallow copying

```java
int[][] original = {{1}, {2}};
int[][] copy = original.clone();
copy[0][0] = 9;
System.out.println(original[0][0]);
```

Actual output: `9`. Only the outer array was copied; row references are shared. Correct deep copy: clone each non-null row. Follow-up: what contract should define behavior for null rows?

## 18. `Arrays.asList` fixed-size behavior

```java
List<String> values = Arrays.asList("a", "b");
values.set(0, "x");
values.add("c");
```

`set` succeeds; `add` throws `UnsupportedOperationException`. The list is fixed-size and backed by an array. Correct resizable copy: `new ArrayList<>(Arrays.asList("a", "b"))`. Follow-up: are changes to the original array visible through the list?

## 19. Primitive arrays passed to `Arrays.asList`

```java
int[] numbers = {1, 2, 3};
List<int[]> list = Arrays.asList(numbers);
System.out.println(list.size());
```

Actual output: `1`. `int[]` is one reference element; generic varargs do not box its contents. Correct: loop into `List<Integer>` or use `Arrays.stream(numbers).boxed().toList()`. Follow-up: is the stream result mutable?

## 20. String immutability

```java
String text = "java";
text.toUpperCase();
System.out.println(text);
```

Actual output: `java`. String methods return results; the original object does not change. Correct: `text = text.toUpperCase(Locale.ROOT)`. Follow-up: why can immutable strings still be referenced by mutable variables?

## 21. Repeated string concatenation

```java
String result = "";
for (int i = 0; i < n; i++) result += i;
```

This may repeatedly copy a growing immutable string and become quadratic in produced characters. Correct repeated construction: `StringBuilder builder = new StringBuilder(); builder.append(i);`. Follow-up: when can one expression's `+` remain readable and efficient?

## 22. `char` arithmetic

```java
char digit = '7';
System.out.println(digit - '0');
System.out.println(digit + 1);
```

Actual output: `7`, then `56`; binary numeric promotion produces `int`. Correct next character: `(char) (digit + 1)`. Validate ASCII digits before `digit - '0'`; `Character.digit(digit, 10)` handles broader digit definitions. Follow-up: what type is `'a' + 'b'`?

## 23. Unicode limitations of `char`

```java
String symbol = "\uD83D\uDE00";
System.out.println(symbol.length());
```

Actual output: `2`. A Java `char` is a UTF-16 code unit; this supplementary code point uses a surrogate pair. Correct code-point count: `symbol.codePointCount(0, symbol.length())`. Follow-up: why is even a code point not always one user-perceived character?

## 24. Switch fall-through

```java
int value = 1;
switch (value) {
    case 1: System.out.print("one ");
    case 2: System.out.print("two");
}
```

Actual output: `one two`. Traditional colon cases fall through without `break`, `return`, or `throw`. Correct with breaks or a version-labeled switch expression. Follow-up: when is deliberate fall-through acceptable?

## 25. Missing default constructor

```java
class User { User(String name) {} }
// User user = new User();
```

The commented construction would not compile. Declaring any constructor prevents the compiler from supplying a no-argument constructor. Correct by calling the declared constructor or declaring an explicit no-argument constructor. Follow-up: what if the superclass lacks an accessible no-argument constructor?

## 26. Constructor chaining rules

```java
class Size {
    Size() {
        // System.out.println("start");
        // this(1); // illegal after another statement
    }
    Size(int value) {}
}
```

`this(...)` or `super(...)` must be the constructor's first statement. Correct: put `this(1);` first and move common work to the target constructor. Follow-up: can one constructor invoke both explicitly?

## 27. Static versus instance access

```java
class Counter {
    static int total;
    int local;
    static void reset() { total = 0; }
}
```

A static method has no receiver and cannot directly read `local`. Correct: pass a `Counter`, make the method an instance method, or operate only on class state. Follow-up: why is `instance.reset()` legal but misleading?

## 28. Overriding versus overloading

```java
class Base { String label(Object value) { return "base"; } }
class Child extends Base { String label(String value) { return "child"; } }
Base item = new Child();
System.out.println(item.label("x"));
```

Actual output: `base`. `label(String)` overloads rather than overrides `label(Object)`; compile-time selection uses `Base`'s visible signature. Correct override uses the identical parameter types and `@Override`. Follow-up: can return type alone overload a method?

## 29. Covariant return types

```java
class Parent { Number value() { return 1; } }
class Child extends Parent { @Override Integer value() { return 2; } }
```

This compiles: an overriding reference return type may be a subtype of the original return type. Primitive return types are not covariant. Follow-up: what return type is visible at a call site through a `Parent` reference?

## 30. Field hiding versus method overriding

```java
class Parent { int value = 1; int value() { return 1; } }
class Child extends Parent { int value = 2; @Override int value() { return 2; } }
Parent item = new Child();
System.out.println(item.value + " " + item.value());
```

Actual output: `1 2`. Fields use the declared reference type; instance methods dispatch to the runtime object. Correct design avoids same-name instance fields across a hierarchy. Follow-up: how do static methods behave?

## 31. Unsafe downcasting

```java
Object value = "java";
// Integer number = (Integer) value;
```

If reached, the cast throws `ClassCastException`. Correct: keep a stronger type or guard with `instanceof`. Follow-up: why can a cast compile yet fail at runtime?

## 32. Exception catch ordering

```java
try {
    Integer.parseInt("x");
} catch (RuntimeException exception) {
    System.out.println("runtime");
// } catch (NumberFormatException exception) { } // unreachable
}
```

A catch for a subtype after its supertype is a compile-time error because the earlier catch already handles it. Correct: most specific catches first. Follow-up: when is one broad boundary catch appropriate?

## 33. `finally` behavior

```java
static int result() {
    try { return 1; }
    finally { return 2; }
}
```

Actual result: `2`; the `finally` return suppresses the pending return and is dangerous. Correct: never return from `finally`; reserve it for cleanup that does not replace the primary outcome. Also, `finally` is not guaranteed after forced process termination or fatal host failure. Follow-up: how does try-with-resources preserve suppressed close failures?

## 34. Mutable keys in `HashMap`

```java
List<Integer> key = new ArrayList<>(List.of(1));
Map<List<Integer>, String> map = new HashMap<>();
map.put(key, "value");
key.add(2);
System.out.println(map.get(key));
```

The lookup is commonly `null` because the key's hash code changed while the entry stayed in its old bucket. Correct: use an immutable key whose equality fields never change. Follow-up: why may iterating still reveal the entry?

## 35. Comparator subtraction overflow

```java
Comparator<Integer> broken = (left, right) -> left - right;
System.out.println(broken.compare(Integer.MAX_VALUE, -1));
```

Subtraction overflows and can report the wrong sign. Correct: `Integer.compare(left, right)`. Follow-up: how do you add a deterministic tie-breaker?

## 36. PriorityQueue iteration is not sorted

```java
PriorityQueue<Integer> queue = new PriorityQueue<>(List.of(4, 1, 3, 2));
for (int value : queue) System.out.print(value + " ");
```

Only `peek`/`poll` guarantee the head according to priority; iterator order is unspecified. Correct sorted consumption repeatedly polls a copy if the original must be preserved. Follow-up: what is the cost and mutation trade-off?

## 37. Modifying during enhanced-for

```java
List<Integer> values = new ArrayList<>(List.of(1, 2, 3));
for (int value : values) {
    if (value == 2) values.remove(Integer.valueOf(value));
}
```

This can throw `ConcurrentModificationException`; fail-fast behavior is a bug detector, not a synchronization guarantee. Correct: use the iterator's `remove`, `removeIf`, or collect changes separately. Follow-up: why does the exact moment of failure not form an API guarantee?

## 38. Null handling

```java
String value = null;
// System.out.println(value.equals("x"));
```

The call would throw. Correct null-safe choices depend on contract: reject with `requireNonNull`, compare as `"x".equals(value)`, or use `Objects.equals`. Follow-up: why can silently treating null as empty lose information?

## 39. Off-by-one array errors

```java
int[] values = {2, 4, 6};
for (int index = 0; index <= values.length; index++) {
    System.out.println(values[index]);
}
```

The fourth access throws `ArrayIndexOutOfBoundsException`; valid indexes are `0` through `length - 1`. Correct half-open loop: `index < values.length`. Follow-up: state its loop invariant.

## 40. Incorrect loop termination

```java
int value = 10;
while (value != 0) {
    value -= 3;
}
```

This never reaches zero and continues through negative values until eventual integer wraparound. Correct the contract and progress rule, for example `while (value > 0) value -= 3;`. Follow-up: how would you prove termination for arbitrary step sizes?

## Rapid trap checklist

Before submitting, inspect equality, null, arithmetic type, overflow order, division, casts, aliasing, copy depth, loop bounds, comparator sign, collection mutation, constructor availability, dispatch kind, exception order, and resource ownership.

## Cross-book boundary

Numeric traps continue in Number Systems and Bit Manipulation. Loop termination continues in Loop Mastery. Aliasing and copying continue in Arrays. Hash keys continue in Hashing. Priority ordering continues in Heaps. Deep exception and JVM failure behavior continue in Advanced Java and JVM.

## Chapter summary

Each trap has the same repair method: state the type and object contract, predict evaluation order, identify shared state, label the failure stage, and test a boundary that distinguishes the wrong mental model from the correct one.
