# Wrappers, Generics, Enums, Collections, I/O, and Utilities

## Learning objectives

This chapter supplies the library runway used in early DSA solutions: primitive wrappers, boxing, basic generics, enums, collection selection, console I/O, and high-value utility APIs. Internals and advanced type-system mechanics remain in later books.

## Wrapper classes and parsing

Generics and collections require reference types, so every primitive has a wrapper: `byte`/`Byte`, `short`/`Short`, `int`/`Integer`, `long`/`Long`, `float`/`Float`, `double`/`Double`, `char`/`Character`, and `boolean`/`Boolean`. Wrappers are immutable and provide parsing, comparison, conversion, constants, and utility methods.

```java
int count = Integer.parseInt("42");
long maximum = Long.MAX_VALUE;
int comparison = Integer.compare(2_000_000_000, -2_000_000_000);
System.out.println(count);       // 42
System.out.println(comparison);  // 1
```

`parseInt` returns a primitive and throws `NumberFormatException` for invalid or out-of-range input. `Integer.valueOf("42")` returns a wrapper. Use `Integer.compare(left, right)`, not `left - right`, because subtraction can overflow.

## Autoboxing and unboxing

Autoboxing converts a primitive to its wrapper where required; unboxing extracts the primitive value.

```java
java.util.List<Integer> values = new java.util.ArrayList<>();
values.add(7);       // boxes int to Integer
int first = values.get(0); // unboxes Integer to int
```

Unboxing `null` throws `NullPointerException`.

```java
Integer boxed = null;
// int value = boxed; // runtime NullPointerException if reached
```

Wrapper identity is not value equality:

```java
Integer first = 127;
Integer second = 127;
Integer third = 128;
Integer fourth = 128;
System.out.println(first == second);  // commonly true by required small cache
System.out.println(third == fourth);  // do not rely on this identity result
System.out.println(third.equals(fourth)); // true
```

The required cache makes some small values share instances, and implementations may cache more. Never turn that optimization into program logic; use `equals` or `Objects.equals` for wrapper values.

## Basic generics

A type parameter lets one declaration work safely with multiple reference types.

```java
final class Box<T> {
    private final T value;
    Box(T value) { this.value = value; }
    T value() { return value; }
}

Box<String> word = new Box<>("java");
String text = word.value();
```

`T` is a type parameter. `Box<String>` prevents inserting an `Integer` and removes a caller-side cast. The diamond `<>` asks the compiler to infer type arguments. Primitives cannot be generic arguments, so use `List<Integer>`, not `List<int>`.

A generic method can declare its own type parameter:

```java
static <T> T first(java.util.List<T> values) {
    if (values.isEmpty()) throw new IllegalArgumentException("empty list");
    return values.get(0);
}
```

A light bound says which operations are available: `<T extends Comparable<? super T>>`. Wildcards, PECS, capture, erasure, and heap pollution belong in Advanced Java.

## Enums model finite state

```java
enum OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
```

Enum constants are typed singleton instances. Enums may have fields, private constructors, and methods. They are safer than magic strings because the compiler checks names and `switch` can cover the declared states. Avoid persisting `ordinal()` as a durable external contract; declaration order can change.

## Collection selection for early DSA

Choose by semantics first.

| Need | Typical interface/implementation | Core calls |
|---|---|---|
| indexed sequence | `List` / `ArrayList` | `add`, `get`, `set`, `size` |
| membership or deduplication | `Set` / `HashSet` | `add`, `contains`, `remove` |
| key to value or frequency | `Map` / `HashMap` | `put`, `get`, `getOrDefault`, `merge` |
| FIFO queue | `Queue` / `ArrayDeque` | `offer`, `poll`, `peek` |
| LIFO stack | `Deque` / `ArrayDeque` | `push`, `pop`, `peek` |
| smallest/largest priority | `PriorityQueue` | `offer`, `poll`, `peek` |
| insertion order retained | `LinkedHashSet`, `LinkedHashMap` | set/map APIs |
| sorted keys/elements | `TreeMap`, `TreeSet` | map/set plus navigation |

`LinkedList` is useful when its deque semantics or iterator-position operations fit. It is not universally faster for insertion: locating a position and pointer-heavy memory behavior matter. Prefer `ArrayDeque` over legacy `Stack` in new interview code.

Frequency map:

```java
Map<String, Integer> frequency = new HashMap<>();
for (String word : List.of("java", "dsa", "java")) {
    frequency.merge(word, 1, Integer::sum);
}
System.out.println(frequency.get("java")); // 2
```

Deduplication while retaining encounter order:

```java
Set<Integer> unique = new LinkedHashSet<>(List.of(3, 1, 3, 2));
System.out.println(unique); // [3, 1, 2]
```

Queue and stack with `ArrayDeque`:

```java
Deque<Integer> deque = new ArrayDeque<>();
deque.offerLast(10);
deque.offerLast(20);
System.out.println(deque.pollFirst()); // queue removes 10

deque.push(30);
System.out.println(deque.pop());       // stack removes 30
```

Min-heap:

```java
PriorityQueue<Integer> minimums = new PriorityQueue<>();
minimums.offer(9);
minimums.offer(3);
minimums.offer(5);
System.out.println(minimums.poll()); // 3
```

Only repeated `poll` operations reveal priority order. Iteration is not sorted order. Hash-based operations are commonly expected constant time under suitable hashing, but the API does not promise universal O(1) behavior. Continue to the dedicated collection and DSA volumes for costs and internals.

## Input and output

Many interviews provide a method signature and do not require console parsing. When input code is required, choose convenience or throughput deliberately.

Scanner is convenient:

```java
Scanner scanner = new Scanner(System.in);
int length = scanner.nextInt();
int[] numbers = new int[length];
for (int index = 0; index < length; index++) {
    numbers[index] = scanner.nextInt();
}
```

`BufferedReader` plus parsing is more explicit and often faster for token-heavy input:

```java
BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
int rows = Integer.parseInt(reader.readLine().strip());
int[][] matrix = new int[rows][];
for (int row = 0; row < rows; row++) {
    StringTokenizer tokens = new StringTokenizer(reader.readLine());
    int columns = tokens.countTokens();
    matrix[row] = new int[columns];
    for (int column = 0; column < columns; column++) {
        matrix[row][column] = Integer.parseInt(tokens.nextToken());
    }
}
```

The second example permits jagged rows. In real code, validate token counts and define behavior for missing lines. Use `PrintWriter` to buffer output and flush at the ownership boundary. Try-with-resources is appropriate for resources your code owns; do not casually close `System.in` when another component owns it.

## Interview utility APIs

- `Math`: `min`, `max`, `abs`, `sqrt`, `pow`, `floor`, `ceil`, `addExact`, `subtractExact`, `multiplyExact`.
- `Arrays`: `sort`, `fill`, `copyOf`, `equals`, `deepEquals`, `binarySearch`.
- `Collections`: `sort`, `reverse`, `min`, `max`, `frequency`, `binarySearch`.
- `Objects`: `equals`, `hash`, `requireNonNull`.
- Wrappers: parsing, `compare`, min/max constants, `toString(value, radix)`, and unsigned/base helpers where needed.
- `Character`: `isDigit`, `isLetter`, `isWhitespace`, `toLowerCase`, `toUpperCase`, and `digit`.

Important caveats:

```java
List<String> fixed = Arrays.asList("a", "b");
fixed.set(0, "x");      // allowed
// fixed.add("c");      // UnsupportedOperationException

int[] primitive = {1, 2};
List<int[]> oneElement = Arrays.asList(primitive);
System.out.println(oneElement.size()); // 1, not 2
```

`List.of(...)` creates an unmodifiable list and rejects null elements. `Collections.unmodifiableList(existing)` creates an unmodifiable view; changes through another alias to `existing` remain visible. Label the exact contract instead of calling every such result "immutable."

## Common mistakes and interview angles

- Unboxing a nullable wrapper.
- Comparing wrappers with `==`.
- Using `List<int>` instead of `List<Integer>`.
- Using raw collections and deferring type errors to runtime.
- Using a string where an enum defines the finite state.
- Assuming `HashMap` order or guaranteed O(1) operations.
- Treating `PriorityQueue` iteration as sorted.
- Writing `left.priority - right.priority` in a comparator.
- Using legacy `Stack` for new code.
- Closing a stream the current method does not own.
- Assuming `Arrays.asList(intArray)` produces a list of boxed integers.

## Quick check and practice

1. What is the difference between parsing, boxing, and unboxing?
2. Why can `Integer` identity tests appear inconsistent?
3. Why are primitives unavailable as generic arguments?
4. Which collection represents FIFO without accepting `null` in its common implementation?
5. Why is a priority queue's iterator not a sorted traversal?

**Foundation:** Count character frequencies with `Map<Character, Integer>`.

**Interview Core:** Read a jagged integer matrix and print each row sum.

**SDE-2 Follow-up:** Compare an unmodifiable view with an independently copied unmodifiable list, including aliasing behavior.

## Cross-book boundary

Continue to Hashing, Stacks, Queues and Deques, Heaps, and Time and Space Complexity for algorithm patterns and defensible costs. Continue to Java Collections Internals for implementation mechanics, Advanced Java for generics and lambdas in depth, and the I/O volume for files, charsets, channels, and resource ownership.

## Chapter summary

Wrappers connect primitives to generic APIs but introduce null and identity traps. Generics provide compile-time type safety for reference types. Enums model finite state. Collections should be selected by contract, not folklore. Input code should match the platform boundary, and utility methods should be used with their exact aliasing, ordering, overflow, and mutability behavior understood.
