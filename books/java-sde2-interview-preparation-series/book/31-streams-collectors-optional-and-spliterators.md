# 31. Streams, Collectors, Optional, and Spliterators

## Learning objectives

By the end of this chapter, you should be able to:

- explain lazy stream pipelines, encounter order, and single-use traversal;
- distinguish stateless, stateful, short-circuiting, intermediate, and terminal operations;
- write side-effect-free transformations and associative reductions;
- compose collectors and state their mutability, duplicate, null, and ordering behavior;
- use `Optional` at absence-return boundaries without turning it into a universal container;
- interpret spliterator characteristics and splitting; and
- evaluate parallel streams using correctness laws, source shape, workload, and deployment context.

## Why this matters at SDE-2

Streams compress collection processing into declarative pipelines, but concise syntax can hide multiple traversals, large buffers, unsafe side effects, and ambiguous merge behavior. At SDE-2, you should be able to review a pipeline as rigorously as a loop: identify its source, cardinality changes, ordering, terminal result, allocation, and parallel assumptions.

Collectors appear in grouping, aggregation, indexing, and reporting. Optional appears at repository and lookup boundaries. Spliterators connect collection data structures to sequential and parallel traversal. Understanding all three prevents accidental `O(n^2)` work, data races, and APIs that are harder to use than a simple null check or domain result type.

## First-principles model

A stream is not a container. It is a one-shot description of a traversal and transformation. A pipeline has:

```text
source -> intermediate operations -> terminal operation
```

Intermediate operations return another stream and are generally lazy. The terminal operation pulls elements through the pipeline. A fused sequential pipeline can process one element through several stages before requesting the next.

```text
input:  [1, 2, 3, 4, 5]
filter even -> map square -> findFirst

1 rejected
2 accepted -> 4 -> terminal completes
3, 4, 5 may never be requested
```

A collector is a mutable-reduction recipe with four conceptual functions:

1. supplier creates an accumulator;
2. accumulator incorporates one input;
3. combiner merges partial accumulators; and
4. finisher converts the accumulator to the result, unless identity finishing applies.

A spliterator is a traversal cursor that can also partition remaining work. Its characteristics describe properties a caller may exploit.

> **Specification boundary:** Stream semantics define laziness, operation contracts, encounter-order rules, reduction requirements, and single use. They do not guarantee stage fusion strategy, thread count, partition sizes, common-pool scheduling, vectorization, or that parallel execution will be faster.

## Core terminology

- **Pipeline:** Source, intermediate operations, and one terminal operation.
- **Lazy:** Work is deferred until demanded by a terminal operation.
- **Encounter order:** Order in which a stream logically presents elements when its source or operations define one.
- **Stateless operation:** Result for one element does not depend on previously seen elements, such as `map`.
- **Stateful operation:** May buffer or track elements, such as `sorted` or `distinct`.
- **Short-circuiting:** May complete without processing all input, such as `findFirst` or `limit`.
- **Non-interference:** Pipeline behavior does not modify or depend on unsafe mutation of its source during execution.
- **Associative:** Grouping operands differently produces an equivalent result.
- **Mutable reduction:** Accumulate into a mutable result container, as with `collect`.
- **Downstream collector:** Collector applied to values within each group or partition.
- **Characteristic:** Spliterator or collector property clients can use for execution decisions.
- **Primitive stream:** `IntStream`, `LongStream`, or `DoubleStream`, avoiding wrapper elements for many operations.

## Detailed mechanics

### Laziness, fusion, and single use

`filter`, `map`, `flatMap`, `peek`, `distinct`, `sorted`, `limit`, and `skip` are intermediate. `forEach`, `reduce`, `collect`, `count`, `findFirst`, and matching operations are terminal. Without a terminal operation, a pipeline performs no traversal.

A stream can be consumed once. Reusing it after a terminal operation throws `IllegalStateException`. Store a source or a `Supplier<Stream<T>>` when repeated traversal is intended.

Laziness enables fusion and short-circuiting, but stateful operations can create barriers. `sorted` usually needs all relevant input before emitting the first sorted result. `distinct` tracks seen values. On ordered parallel pipelines, `limit`, `skip`, and `distinct` can require coordination and buffering.

### Operation classes and ordering

`map` preserves cardinality one-for-one; `filter` can reduce it; `flatMap` maps one input to zero or more outputs and closes each nested stream after use. `mapMulti`, available in modern Java, can emit zero or more outputs without constructing a stream per source item.

Ordered sources include lists and sorted collections. Hash-based collections generally do not promise encounter order. `forEachOrdered` respects encounter order when one exists, even in a parallel pipeline, at a coordination cost. `findFirst` is order-sensitive; `findAny` permits more freedom and can be useful in parallel.

Calling `unordered()` does not randomly shuffle data. It removes the obligation to preserve encounter order, allowing optimizations for operations whose result does not require it.

### Side effects and non-interference

Behavioral parameters should be stateless and non-interfering:

```java
// Wrong for parallel use and difficult even sequentially.
List<String> output = new ArrayList<>();
input.parallelStream()
        .filter(this::valid)
        .forEach(output::add);

// Correct mutable reduction.
List<String> output = input.parallelStream()
        .filter(this::valid)
        .toList();
```

Concurrent mutation of the source during traversal may throw, produce weakly consistent observations for a concurrent source, or otherwise follow that source's contract. Streams do not add isolation.

`peek` exists mainly for debugging and carefully controlled observation. It may not execute for every conceptual element because short-circuiting and implementation optimizations can avoid traversal. Do not put required business effects in `peek`.

### Reduction laws

For `reduce(identity, accumulator, combiner)`, the identity must be neutral, the reduction must be associative, and accumulator/combiner behavior must be compatible. Integer addition satisfies this ignoring overflow as Java-defined wraparound grouping effects for fixed values; subtraction does not:

```text
(10 - 3) - 2 = 5
10 - (3 - 2) = 9
```

A sequential left fold with subtraction may look predictable, but parallel partitioning changes grouping. Floating-point addition is mathematically associative but not bitwise associative due to rounding, so parallel sums can differ slightly.

Do not mutate the identity object in the three-argument `reduce`. Multiple partitions may share assumptions incompatible with mutable state. Use `collect` for mutable containers.

### Collector composition

Common collectors include:

- `toList`, `toSet`, and `toCollection`;
- `joining` for character sequences;
- `counting`, `summingInt`, `averagingLong`, and `summarizingDouble`;
- `groupingBy` and `groupingByConcurrent`;
- `partitioningBy` for exactly two Boolean groups;
- `mapping`, `filtering`, and `flatMapping` downstream;
- `collectingAndThen` for a finishing transformation; and
- `teeing` to run two downstream aggregations and merge their results.

When collecting to a map, duplicate keys need policy:

```java
Map<String, User> byEmail = users.stream().collect(
        java.util.stream.Collectors.toMap(
                User::email,
                java.util.function.Function.identity(),
                (left, right) -> chooseNewer(left, right),
                java.util.LinkedHashMap::new));
```

Without a merge function, duplicate keys throw `IllegalStateException`. That can be the right invariant check. The map supplier determines a concrete result representation when an overload accepts it.

`Collectors.toList()` does not promise mutability, unmodifiability, or a concrete list type. `Stream.toList()` returns an unmodifiable list and may have different null behavior from `List.copyOf`; do not substitute methods solely because names look similar. `Collectors.toUnmodifiableList()` rejects null elements.

Collector characteristics include `CONCURRENT`, `UNORDERED`, and `IDENTITY_FINISH`. `CONCURRENT` does not mean every use updates one shared result concurrently; source ordering and collector characteristics influence the strategy. Custom collectors must make accumulator, combiner, and finisher laws correct before seeking performance.

### Optional as an absence result

`Optional<T>` models either one non-null value or absence. It works well as a return type for lookups:

```java
Optional<User> findById(UserId id)
```

Useful methods include `map`, `flatMap`, `filter`, `or`, `orElseGet`, `ifPresentOrElse`, and `stream`. `map` wraps a non-null mapping result and yields empty for null. `flatMap` is for functions already returning `Optional`.

`orElse(fallback())` evaluates `fallback()` eagerly. `orElseGet(this::fallback)` invokes it only when empty. `orElseThrow` is appropriate when absence violates the current layer's precondition.

Avoid `Optional` fields, collection elements, and method parameters by default. They often add a second wrapper state without improving the domain. A list already represents zero or more values; return an empty list instead of `Optional<List<T>>` unless absence and empty are genuinely different. `Optional` is value-based: do not synchronize on instances or rely on identity.

Specialized `OptionalInt`, `OptionalLong`, and `OptionalDouble` avoid boxing. Optional is not a substitute for a rich failure result containing reason, retryability, or validation errors.

### Spliterator mechanics

A spliterator supports `tryAdvance(action)` for one element, `forEachRemaining`, `trySplit`, `estimateSize`, and `characteristics`. Common characteristics are:

- `ORDERED`: defined encounter order;
- `DISTINCT`: elements are distinct under relevant equality;
- `SORTED`: encounter order is sorted and a comparator may be available;
- `SIZED`: exact remaining size is known;
- `SUBSIZED`: split children also report exact sizes;
- `NONNULL`: source guarantees non-null elements;
- `IMMUTABLE`: source cannot be structurally modified;
- `CONCURRENT`: source supports concurrent structural modification under its contract.

`trySplit` returns a prefix or partition and leaves remaining work in the original. Balanced, cheap splitting enables useful parallelism. Array and range sources split well. Pointer-chasing, unknown-size, or I/O-backed sources may split poorly.

Characteristics are promises, not hints. A custom spliterator that falsely reports `SORTED`, `SIZED`, or `DISTINCT` can cause wrong optimizations or contract violations. Late-binding and fail-fast behavior depend on the source implementation.

> **HotSpot note:** OpenJDK stream execution uses internal pipeline stages, fork-join tasks for many parallel pipelines, and heuristics for target partition sizes. These details and optimization shortcuts are version-sensitive.

### Parallel stream decision model

Parallel streams are most plausible when input is large, CPU work per element is substantial, operations are associative and independent, splitting is balanced, and no blocking or shared mutable state is involved. They are often poor for small inputs, ordered stateful stages, blocking I/O, request-thread latency isolation, or environments where the common pool is already busy.

Measure end-to-end behavior under concurrent service load. A microbenchmark showing one pipeline speedup does not prove better tail latency or capacity in a server.

## Worked Java example

The following aggregation produces immutable per-region statistics from completed orders:

```java
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

enum Status { PENDING, COMPLETED, CANCELED }
record Order(String id, String region, Status status, BigDecimal amount) {}
record RegionSummary(long count, BigDecimal total, List<String> orderIds) {}

final class OrderReport {
    static Map<String, RegionSummary> summarize(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.status() == Status.COMPLETED)
                .collect(Collectors.groupingBy(
                        Order::region,
                        java.util.TreeMap::new,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                OrderReport::summarizeGroup)));
    }

    private static RegionSummary summarizeGroup(List<Order> group) {
        BigDecimal total = group.stream()
                .map(Order::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> ids = group.stream()
                .sorted(Comparator.comparing(Order::id))
                .map(Order::id)
                .toList();

        return new RegionSummary(group.size(), total, ids);
    }
}
```

The outer `TreeMap` guarantees region order. Each list is an intermediate mutable accumulator local to collection, and the finisher converts it to an immutable summary. `BigDecimal.add` returns a new value, so there is no shared mutable numeric accumulator.

For a very large group, retaining its entire list may be unnecessary. A custom accumulator could track count, total, and IDs directly, but its combiner must merge all fields correctly. If sorted IDs are mandatory, memory proportional to group cardinality remains necessary unless output can stream to another sorted source.

## Execution or memory walkthrough

Given completed orders `east/e2/$5`, `west/w1/$7`, and `east/e1/$3`, plus one canceled east order:

1. `filter` rejects the canceled order.
2. `groupingBy` asks the `TreeMap` for an accumulator list for each region.
3. East's list receives `e2` and `e1`; west's receives `w1`.
4. The downstream finisher visits each group. East total becomes `0 + 5 + 3 = 8`; IDs sort to `[e1, e2]`.
5. The final map is sorted by region, but it remains mutable because `groupingBy` with `TreeMap::new` returns that mutable map. If an unmodifiable outer map is required, wrap the complete collector with `collectingAndThen` and an order-preserving unmodifiable snapshot strategy.

In a parallel execution, partitions can build separate maps and lists, then combiners merge them. Encounter order for downstream lists follows the collector and source contracts; code must not assume one shared accumulator. The example remains mathematically correct because filters and mappings are pure and `BigDecimal` addition is associative in value for these exact decimal inputs, subject to any chosen math context.

Memory includes group lists containing references to orders, final ID lists containing references to existing strings, summary records, and tree map entries. Streams themselves add pipeline objects and possibly tasks/buffers, but they do not automatically duplicate every input.

## Complexity and performance

Let `n` be input size, `g` number of regions, and `m_i` each group size. Filtering and grouping perform `O(n)` element work plus `TreeMap` group lookup `O(log g)` per accepted element, so `O(n log g)` under this chosen map. Summing is `O(n)`. Sorting IDs costs:

```text
sum over groups of O(m_i log m_i), bounded by O(n log n)
```

Space is `O(n + g)` because group lists and final ID lists retain per-order references during finishing. A one-pass custom collector could reduce temporary duplication but not the final list storage.

Pipeline complexity composes by stages. `map` and `filter` are normally linear; `sorted` is `O(n log n)`; `distinct` is expected `O(n)` with hashing but depends on source/order and equality cost; `limit(k)` can be `O(k)` sequentially with a favorable source but may coordinate more work in parallel.

Boxing matters in numerical pipelines. `stream.mapToLong(...).sum()` avoids one wrapper per mapped value. It does not fix an expensive source, I/O, or algorithm. Benchmark with JMH-style discipline and realistic cardinality before replacing a clear loop.

## Edge cases and common mistakes

- Reusing a stream after a terminal operation.
- Mutating a non-thread-safe collection from `parallel().forEach`.
- Using `peek` for required persistence or audit effects.
- Supplying a non-associative reduction and observing parallel differences.
- Mutating a shared identity in `reduce` instead of using `collect`.
- Forgetting duplicate-key behavior in `toMap`.
- Assuming collector-produced collection types, mutability, null policy, or order without a contract.
- Calling `orElse(expensive())` when lazy `orElseGet` is intended.
- Calling `optional.get()` without proving presence, recreating a less clear null check.
- Using `Optional<List<T>>` when empty list already expresses no results.
- Flat-mapping infinite streams without a termination argument.
- Placing `sorted` before a selective `filter` and sorting unnecessary elements.
- Parallelizing blocking I/O on the common pool.
- Reporting false characteristics in a custom spliterator.
- Depending on fail-fast traversal to make source mutation safe.
- Assuming `unordered()` shuffles or that parallel automatically ignores encounter order.

## Production engineering notes

Prefer pipelines whose stages read like a data-flow specification and whose cardinality remains bounded. Break a complex pipeline into named methods or intermediate results when it improves observability and debugging. A loop is often better when error handling, early exits, multiple stateful outputs, or checked resource lifetimes dominate.

Keep lambdas pure. Move database calls and remote requests outside element-wise stream operations; otherwise a compact line can hide N+1 I/O. Batch external work explicitly and use concurrency mechanisms with controlled executors, timeouts, and backpressure.

Define output map/list order and mutability. Use explicit collection suppliers when representation matters. Treat `groupingBy` on unbounded cardinality as a memory risk. Validate keys, cap results, and avoid `distinct` on attacker-controlled unbounded streams.

Use `Optional` for an immediate maybe-one return, then unwrap or transform near that boundary. Domain-specific sealed results are clearer when absence has multiple causes. Avoid Optional in serialization entities unless the serializer and schema deliberately support it.

For parallel pipelines, validate algebraic correctness sequentially first. Then benchmark using the target JDK, CPU quota, container limits, and concurrent traffic. Consider a dedicated executor or structured concurrency design when work needs isolation; the convenient common pool may couple unrelated requests.

## Interview questions and model answers

**Why are streams lazy?**

Laziness lets the terminal operation pull only needed elements, fuse stateless stages, and short-circuit. Stateful operations may still buffer substantial input.

**What is the difference between `map` and `flatMap`?**

`map` produces one result value per input. `flatMap` maps each input to a stream and flattens all nested elements, supporting zero-to-many output.

**What makes a reduction safe in parallel?**

Its operation is associative, its identity is neutral, accumulator and combiner are compatible, and behavioral functions do not share mutable state.

**Why should mutable reduction use `collect` rather than `reduce`?**

Collector contracts explicitly provide separate accumulators and a combiner for mutable containers. Mutating a reduce identity can share state incorrectly and violates reduction assumptions.

**When is `parallelStream` appropriate?**

For sufficiently large, splittable, CPU-bound data with independent operations and associative aggregation, after measurement under actual deployment load. It is not a general fix for blocking I/O.

**What does a spliterator contribute?**

It traverses elements, can split remaining work into partitions, estimates size, and reports characteristics such as ordering, size, or distinctness that stream execution can exploit.

**Why prefer `orElseGet` sometimes?**

`orElse` evaluates its argument eagerly even when the optional has a value. `orElseGet` evaluates its supplier only when empty.

## Exercises

1. Rewrite a parallel `forEach` that appends to an `ArrayList` as a safe collector. State order behavior.
2. Show that subtraction is not associative by partitioning four integers in two different ways.
3. Build a nested map of department to status to count using downstream collectors.
4. Implement a custom collector for count and `BigDecimal` total. Prove supplier, combiner, and finisher behavior.
5. Compare `findFirst` and `findAny` on ordered and unordered parallel sources.
6. Design a spliterator for an immutable array slice. State valid characteristics and a balanced `trySplit` rule.
7. Refactor code using `isPresent` plus `get` into `map`, `flatMap`, `or`, or `orElseThrow` where appropriate.
8. Analyze memory for `sorted().limit(10)` versus a size-10 heap on ten million records.

## Chapter summary

Streams are lazy, single-use traversal pipelines rather than containers. Correct pipelines preserve non-interference, encounter-order requirements, and associative reduction laws. Collectors formalize mutable reduction and composition but require explicit duplicate, null, order, and mutability policies. Optional is a focused maybe-one return type, not a universal field or parameter wrapper. Spliterators expose traversal and partition properties; parallel speed depends on honest characteristics, balanced splitting, pure CPU work, and deployment context.

## Revision checklist

- [ ] I can classify stream operations as intermediate, terminal, stateful, or short-circuiting.
- [ ] I understand encounter order, `forEachOrdered`, `findFirst`, and `findAny`.
- [ ] I keep stream lambdas non-interfering and side-effect-free.
- [ ] I can state associativity and identity requirements for reduction.
- [ ] I can compose grouping and downstream collectors with explicit map policies.
- [ ] I know the mutability and null guarantees I may and may not infer from collection terminals.
- [ ] I use Optional lazily and at appropriate absence boundaries.
- [ ] I can explain spliterator splitting and characteristics.
- [ ] I evaluate parallel streams with correctness, source shape, workload, pool contention, and measurement.
