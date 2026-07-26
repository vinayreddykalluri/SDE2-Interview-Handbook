# Appendix E - Exercise Hints and Selected Solutions

## E.1 How to use this appendix

These are not scripts to memorize. Each solution demonstrates an interview method:

1. restate the contract and assumptions;
2. identify the invariant;
3. choose a representation;
4. derive correctness before complexity;
5. name edge cases and failure behavior; and
6. test with a small dry run.

For each exercise, stop after the hint and attempt a solution. Compare reasoning before comparing syntax. A different solution is valid when it satisfies the stated contract and you can defend its costs. Code targets Java 17 unless a Java 21 feature is explicitly identified.

The self-check criteria are intentionally strict. An interview answer is not complete merely because the sample returns the expected output. It must state why the result generalizes and what the code promises at production boundaries.

## E.2 JVM exercise: class initialization order

**Problem.** Predict the output. Explain which events are guaranteed by the Java language and which physical loading or compilation details remain implementation-specific.

```java
public final class InitializationOrderExercise {
    static class Parent {
        static int parentValue = mark("Parent field");

        static {
            System.out.println("Parent block");
        }

        static int mark(String text) {
            System.out.println(text);
            return 9;
        }
    }

    static class Child extends Parent {
        static final int CONSTANT = 7;
        static Integer boxed = mark("Child field");

        static {
            System.out.println("Child block");
        }
    }

    public static void main(String[] args) {
        System.out.println(Child.CONSTANT);
        System.out.println(Child.boxed);
    }
}
```

**Hint.** Separate loading/linking from initialization. Then ask whether each field read is an active use and whether the field is a compile-time constant variable.

**Selected solution.** The expected output is:

```text
7
Parent field
Parent block
Child field
Child block
9
```

`Child.CONSTANT` is a `static final` primitive initialized by a constant expression. Its value can be embedded in the caller, so reading it does not trigger `Child` initialization. Reading `Child.boxed` is an active use of `Child`; before a class initializes, its superclass initializes. Within each class, static field initializers and static blocks execute in textual order. Therefore the parent field and block precede the child field and block.

The JVM may have loaded or verified either nested class earlier, and HotSpot may interpret or compile methods at any appropriate time. Those details do not change the specified initialization output. In a real system, avoid important side effects in static initialization: failure produces `ExceptionInInitializerError`, and later active use can fail because initialization did not complete.

**Self-check.** You should be able to:

- distinguish load, link, and initialize;
- identify a compile-time constant without running the code;
- state superclass-before-subclass initialization; and
- avoid claiming when JIT compilation occurs.

## E.3 Language exercise: pass-by-value and aliasing

**Problem.** Explain why `swap` fails to swap the caller's variables while `rename` changes the shared object. Then repair the design so the caller can obtain swapped values without hidden mutation.

```java
public final class PassByValueExercise {
    static final class User {
        private String name;

        User(String name) {
            this.name = name;
        }

        void rename(String newName) {
            name = newName;
        }

        String name() {
            return name;
        }
    }

    record Pair<T>(T first, T second) {}

    static void swap(User left, User right) {
        User temporary = left;
        left = right;
        right = temporary;
    }

    static void rename(User user) {
        user.rename("renamed");
    }

    static <T> Pair<T> swapped(T left, T right) {
        return new Pair<>(right, left);
    }

    public static void main(String[] args) {
        User first = new User("first");
        User second = new User("second");

        swap(first, second);
        System.out.println(first.name() + "," + second.name());

        rename(first);
        System.out.println(first.name());

        Pair<User> result = swapped(first, second);
        first = result.first();
        second = result.second();
        System.out.println(first.name() + "," + second.name());
    }
}
```

**Hint.** Draw caller variables and parameter variables as separate boxes. Copy references into parameter boxes. Reassignment changes one box; mutation follows a reference to an object.

**Selected solution.** Java always passes argument values. For reference expressions, the value is a reference. `swap` receives copies of the two references and only reassigns its local copies, so the caller prints `first,second`. `rename` follows its copied reference to the same `User`, so the caller observes `renamed`. Returning a `Pair` communicates new reference placement explicitly; the final line is `second,renamed`.

The phrase "objects are passed by reference" is misleading because it predicts that parameter reassignment can update caller variables. It cannot. The language does not expose the physical address representation of a reference.

**Self-check.** Your diagram should contain four variable boxes before `swap`: two caller variables and two parameters. You should predict every output and distinguish reassignment, mutation, and immutable-result design.

## E.4 Generics exercise: a type-safe copy operation

**Problem.** Implement a method that copies values from a producer collection into a consumer collection. It must allow `List<Integer>` to be copied into `List<Number>` without raw types or casts.

```java
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class GenericCopyExercise {
    static <T> void copy(Collection<? extends T> source,
            Collection<? super T> destination) {
        for (T value : source) {
            destination.add(value);
        }
    }

    public static void main(String[] args) {
        List<Integer> integers = List.of(1, 2, 3);
        List<Number> numbers = new ArrayList<>();
        copy(integers, numbers);
        System.out.println(numbers);
    }
}
```

**Hint.** Use PECS: producer extends, consumer super. Ask what can be safely read and what can be safely written.

**Selected solution.** `source` has an unknown element type that is a subtype of `T`, so every value read from `? extends T` is assignable to `T`. `destination` can consume `T`, so `? super T` is safe for adding a `T`. Reading an arbitrary value from the destination only yields `Object` because its actual element type might be any supertype of `T`.

`List<Integer>` is not a subtype of `List<Number>`; generic types are invariant. If it were, a caller could add a `Double` through the `List<Number>` view and corrupt the integer list. Wildcards express use-site variance without permitting that write.

**Self-check.** Compile calls for `Integer -> Number`, `Integer -> Object`, and `Number -> Object`. Confirm that `Number -> Integer` is rejected. Explain the rejection without saying "the compiler is confused."

## E.5 Collections exercise: stable frequency ranking

**Problem.** Count words case-insensitively and return entries ordered by descending frequency, then ascending normalized word. Do not depend on `HashMap` encounter order.

```java
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FrequencyRankingExercise {
    record Count(String word, int occurrences) {}

    static List<Count> rank(List<String> words) {
        Map<String, Integer> counts = new HashMap<>();
        for (String word : words) {
            if (word == null) {
                throw new IllegalArgumentException("null word");
            }
            String normalized = word.toLowerCase(Locale.ROOT);
            counts.merge(normalized, 1, Integer::sum);
        }

        ArrayList<Count> result = new ArrayList<>(counts.size());
        counts.forEach((word, count) -> result.add(new Count(word, count)));
        result.sort(Comparator.comparingInt(Count::occurrences).reversed()
                .thenComparing(Count::word));
        return List.copyOf(result);
    }

    public static void main(String[] args) {
        System.out.println(rank(List.of("Java", "map", "JAVA", "Map", "java")));
    }
}
```

**Hint.** Separate aggregation order from presentation order. Add a deterministic tie-breaker.

**Selected solution.** The hash map provides expected constant-time aggregation. The comparator completely defines presentation: higher counts first, then lexical word. The result is `[Count[word=java, occurrences=3], Count[word=map, occurrences=2]]`. `Locale.ROOT` avoids environment-dependent case mapping for protocol-like tokens.

Let `C` be the total character count and `u` the number of unique normalized words. Expected aggregation is `O(C)` because normalization, hashing, and equality inspect characters. Result creation is `O(u)`. Sorting performs `O(u log u)` comparisons, with lexical comparison cost proportional to the compared prefixes; it is only `O(u log u)` under a constant-length-word assumption. Space is `O(C)` in the worst case for retained normalized keys. If words are human-language text, normalization and collation require a domain-specific Unicode/locale policy beyond lowercase.

**Self-check.** Test empty input, ties, repeated case variants, and a null. Explain why stable sorting alone would not make tied output deterministic if the input to the sort came from an unordered map.

## E.6 Collections design exercise: bounded LRU cache

**Problem.** Implement a small least-recently-used cache. State what the implementation does not guarantee.

```java
import java.util.LinkedHashMap;
import java.util.Map;

public final class LruCacheExercise<K, V> {
    private final int capacity;
    private final LinkedHashMap<K, V> entries;

    public LruCacheExercise(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LruCacheExercise.this.capacity;
            }
        };
    }

    public V get(K key) {
        return entries.get(key);
    }

    public void put(K key, V value) {
        entries.put(key, value);
    }

    public int size() {
        return entries.size();
    }

    public static void main(String[] args) {
        LruCacheExercise<String, Integer> cache = new LruCacheExercise<>(2);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.get("a");
        cache.put("c", 3);
        System.out.println(cache.get("a")); // 1
        System.out.println(cache.get("b")); // null
    }
}
```

**Hint.** `LinkedHashMap` can maintain access order. Evict only after insertion makes size exceed capacity.

**Selected solution.** Accessing `a` moves it behind `b` in access order. Adding `c` makes `b` eldest, so the override removes `b`. Basic map operations are expected `O(1)`.

This is not thread-safe, does not distinguish an absent key from a cached null, uses entry count rather than byte weight, has no expiration, does not load values atomically, and is not distributed. Production cache design needs an admission/eviction policy, metrics, concurrency, stampede control, and failure semantics. `LinkedHashMap` access-order behavior is an API contract; its node layout is not.

**Self-check.** Verify recency changes on both `get` and replacement. Explain how you would add synchronization without returning iterators that escape the lock. Define whether null keys/values are valid.

## E.7 Concurrency exercise: safe lazy initialization

**Problem.** Replace a racy lazy singleton without explicit locking on every read. Explain its happens-before argument.

```java
public final class LazyInitializationExercise {
    private LazyInitializationExercise() {}

    private static final class Holder {
        static final LazyInitializationExercise INSTANCE =
                new LazyInitializationExercise();
    }

    public static LazyInitializationExercise instance() {
        return Holder.INSTANCE;
    }

    public static void main(String[] args) throws InterruptedException {
        LazyInitializationExercise[] observed = new LazyInitializationExercise[2];
        Thread first = new Thread(() -> observed[0] = instance());
        Thread second = new Thread(() -> observed[1] = instance());
        first.start();
        second.start();
        first.join();
        second.join();
        System.out.println(observed[0] == observed[1]);
    }
}
```

**Hint.** Class initialization is synchronized by the JVM and happens-before later use of that class by any thread.

**Selected solution.** `Holder` is not initialized merely because the outer class initializes. The first active read of `Holder.INSTANCE` triggers `Holder` initialization. The initialization procedure permits one thread to perform it while other threads wait, and successful class initialization happens-before subsequent active use. Final-field semantics add safety for immutable constructed state, but the key publication edge here is class initialization.

The program prints `true`. `join` also gives the main thread a happens-before edge from each worker's completed actions, so the array reads are visible. Without `join` or another synchronization edge, reading the array from main would race.

An enum singleton is another simple solution when its serialization and initialization semantics fit. Double-checked locking requires a `volatile` field and a correct local/read pattern; the holder idiom is harder to get wrong.

**Self-check.** Name both happens-before edges: class initialization to active use, and worker actions to successful `join` return. Explain why "the object was constructed first" is not alone a memory-visibility proof.

## E.8 Concurrency exercise: bounded task fan-out and cancellation

**Problem.** Run several tasks with a total timeout, cancel unfinished work, preserve interruption, and avoid leaking the executor.

```java
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class BoundedFanOutExercise {
    static <T> List<T> run(List<? extends Callable<T>> tasks,
            int parallelism, Duration timeout) throws Exception {
        if (parallelism < 1 || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("invalid limits");
        }

        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        try {
            List<Future<T>> futures;
            try {
                futures = executor.invokeAll(
                        tasks, timeout.toNanos(), TimeUnit.NANOSECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            }

            ArrayList<T> results = new ArrayList<>(futures.size());
            for (Future<T> future : futures) {
                if (future.isCancelled()) {
                    throw new TimeoutException("fan-out deadline exceeded");
                }
                try {
                    results.add(future.get());
                } catch (ExecutionException failed) {
                    Throwable cause = failed.getCause();
                    if (cause instanceof Exception exception) {
                        throw exception;
                    }
                    throw (Error) cause;
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw interrupted;
                }
            }
            return List.copyOf(results);
        } finally {
            executor.shutdownNow();
        }
    }

    public static void main(String[] args) throws Exception {
        List<Callable<String>> tasks = List.of(
                () -> "alpha",
                () -> "beta",
                () -> "gamma");
        System.out.println(run(tasks, 2, Duration.ofSeconds(1)));
    }
}
```

**Hint.** Use the timed bulk operation, but still inspect cancellation. Treat interrupt as cancellation, not a retryable ordinary exception.

**Selected solution.** `invokeAll` submits all tasks, waits no longer than the timeout, and cancels unfinished futures on return. A fixed pool bounds simultaneous work, not the submitted task list; callers must also bound list size. Results retain input order because the returned future list corresponds to task order, not completion order.

`shutdownNow` requests interruption but cannot force code that ignores interruption to stop. The task contract must honor cancellation and bound its own I/O. Before timed `invokeAll` returns, each task has either completed or been canceled because the timeout expired; throwing while inspecting the first failed future does not undo work that already completed. A production design must decide fail-fast versus collect-all outcomes, attach multiple failures if needed, and use bounded `awaitTermination` handling when executor termination matters to lifecycle safety.

Java 21 virtual threads can simplify blocking task-per-request code, but scarce downstream connections still require a semaphore, pool, or admission bound. Virtual threads remove thread scarcity, not resource limits.

**Self-check.** Test one slow task, one throwing task, and caller interruption. Confirm no test waits forever. State which order the returned results use and why cancellation is cooperative.

## E.9 Performance exercise: repair a misleading benchmark

**Problem.** A candidate presents this result as proof that string parsing takes two nanoseconds:

```java
long start = System.nanoTime();
for (int i = 0; i < 100_000_000; i++) {
    Integer.parseInt("123");
}
long elapsed = System.nanoTime() - start;
System.out.println(elapsed / 100_000_000.0);
```

Identify at least six validity problems and design a JMH experiment.

**Hint.** Ask whether the result is observable, the input is constant, compilation is warm, process history is independent, setup matches production, and uncertainty is reported.

**Selected solution.** Problems include:

1. The result is unused, so the optimizer can eliminate work.
2. The string is constant, so parsing can be folded or specialized.
3. One process mixes warmup and measurement.
4. One aggregate timing reports no iteration/fork variance.
5. The loop and division are part of a homemade harness.
6. There is no baseline for harness overhead.
7. One tiny valid input does not represent lengths, signs, failures, or varied digits.
8. Environment, JDK, flags, CPU limits, and thermal/background state are missing.
9. A microbenchmark does not establish endpoint impact.

A defensible JMH design uses `@Param` for representative strings prepared in trial state, returns the parsed value or consumes it in a `Blackhole`, uses several warmup and measurement iterations in multiple forks, and reports configuration and uncertainty. Invalid-input parsing should be a separate benchmark because exception construction changes the operation. Add allocation or compiler profiling in separate runs, then confirm through a production profile that parsing is hot enough to matter.

Do not make input unpredictability artificial. If production repeatedly parses a small fixed vocabulary, model that vocabulary. If values vary per request, prepare an array of varied inputs and advance an index without putting random generation in the timed operation.

**Self-check.** Your design must state the exact question, mode, inputs, state scope, setup level, result consumption, forks, warmup, measurement, and the higher-level validation needed.

## E.10 DSA exercise: longest substring without repeating characters

**Problem.** Given an ASCII string, return the length of its longest substring containing no repeated character. Target `O(n)` time.

```java
import java.util.Arrays;

public final class LongestUniqueWindowExercise {
    static int longestUnique(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text is required");
        }
        int[] lastSeen = new int[128];
        Arrays.fill(lastSeen, -1);

        int left = 0;
        int best = 0;
        for (int right = 0; right < text.length(); right++) {
            char current = text.charAt(right);
            if (current >= 128) {
                throw new IllegalArgumentException("ASCII input required");
            }
            left = Math.max(left, lastSeen[current] + 1);
            lastSeen[current] = right;
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    public static void main(String[] args) {
        System.out.println(longestUnique("abba"));   // 2
        System.out.println(longestUnique("abcabcbb")); // 3
    }
}
```

**Hint.** Maintain a half-open or closed window invariant and jump the left boundary past the previous occurrence. Never move left backward.

**Selected solution.** Before processing `right`, the window from `left` through `right - 1` contains no duplicate. If the new character was seen inside that window, `lastSeen[current] + 1` moves `left` past it. `Math.max` prevents an older occurrence outside the window from moving `left` backward.

For `abba`, windows evolve as `a`, `ab`, then the second `b` moves left from 0 to 2, producing `b`; the final `a` has an old index 0 outside the current window, so left remains 2 and `ba` has length 2.

Each index advances once and table operations are constant, so time is `O(n)` and auxiliary space is `O(1)` for the fixed ASCII alphabet. Java `char` represents UTF-16 code units, not every Unicode code point. A Unicode-code-point contract should iterate `text.codePoints()` and use a map or appropriately sized structure.

**Self-check.** Test empty, one-character, all-equal, all-unique, and `abba`. State the window invariant before writing complexity.

## E.11 DSA exercise: deterministic topological ordering

**Problem.** Given directed edges `prerequisite -> dependent`, return a deterministic topological order or reject a cycle.

```java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public final class TopologicalOrderExercise {
    record Edge(String prerequisite, String dependent) {}

    static List<String> order(Set<String> nodes, List<Edge> edges) {
        Map<String, List<String>> outgoing = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        for (String node : nodes) {
            outgoing.put(node, new ArrayList<>());
            indegree.put(node, 0);
        }

        Set<Edge> uniqueEdges = new HashSet<>();
        for (Edge edge : edges) {
            if (!nodes.contains(edge.prerequisite())
                    || !nodes.contains(edge.dependent())) {
                throw new IllegalArgumentException("edge contains unknown node");
            }
            if (uniqueEdges.add(edge)) {
                outgoing.get(edge.prerequisite()).add(edge.dependent());
                indegree.merge(edge.dependent(), 1, Integer::sum);
            }
        }

        PriorityQueue<String> ready = new PriorityQueue<>();
        indegree.forEach((node, degree) -> {
            if (degree == 0) ready.offer(node);
        });

        ArrayList<String> result = new ArrayList<>(nodes.size());
        while (!ready.isEmpty()) {
            String node = ready.poll();
            result.add(node);
            for (String dependent : outgoing.get(node)) {
                int remaining = indegree.merge(dependent, -1, Integer::sum);
                if (remaining == 0) ready.offer(dependent);
            }
        }

        if (result.size() != nodes.size()) {
            throw new IllegalArgumentException("graph contains a cycle");
        }
        return List.copyOf(result);
    }

    public static void main(String[] args) {
        Set<String> nodes = Set.of("compile", "test", "package", "deploy");
        List<Edge> edges = List.of(
                new Edge("compile", "test"),
                new Edge("test", "package"),
                new Edge("package", "deploy"));
        System.out.println(order(nodes, edges));
    }
}
```

**Hint.** Kahn's algorithm repeatedly removes a zero-indegree node. If nodes remain but none is ready, those remaining dependencies contain a cycle.

**Selected solution.** Indegree counts unmet prerequisites. Removing a ready node conceptually removes all its outgoing edges; a dependent becomes ready exactly when its count reaches zero. The priority queue provides lexical determinism among simultaneously ready nodes. Duplicate edges are removed so they do not inflate indegree.

With `V` nodes and `E` unique edges, map construction and edge processing are expected `O(V + E)`. Each node enters the heap once, adding `O(V log V)` for deterministic selection; total is `O(E + V log V)` and space `O(V + E)`. A simple deque gives `O(V + E)` but its order depends on insertion/encounter policy.

**Self-check.** Test disconnected nodes, two valid choices, duplicate edges, a self-loop, a longer cycle, and an unknown endpoint. Prove that every emitted node has all prerequisites emitted first.

## E.12 DSA exercise: top-k values without sorting everything

**Problem.** Return the largest `k` integers in descending order. Use `O(k)` auxiliary storage when `k` is much smaller than `n`.

```java
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public final class TopKExercise {
    static List<Integer> largest(List<Integer> values, int k) {
        if (k < 0 || k > values.size()) {
            throw new IllegalArgumentException("k out of range");
        }
        PriorityQueue<Integer> retained = new PriorityQueue<>(Math.max(1, k));
        for (Integer value : values) {
            if (value == null) throw new IllegalArgumentException("null value");
            if (retained.size() < k) {
                retained.offer(value);
            } else if (k > 0 && value > retained.peek()) {
                retained.poll();
                retained.offer(value);
            }
        }
        ArrayList<Integer> result = new ArrayList<>(retained);
        result.sort(Comparator.reverseOrder());
        return List.copyOf(result);
    }

    public static void main(String[] args) {
        System.out.println(largest(List.of(4, 1, 9, 2, 9, 7), 3));
    }
}
```

**Hint.** Keep the worst currently retained answer at the heap head. It can be replaced in logarithmic time.

**Selected solution.** A min-heap of size `k` stores the largest values seen so far. Once full, a value no greater than the root cannot belong to a strictly better top-k multiset; a larger value replaces the root. Duplicate values are retained because the problem asks for values, not unique values.

For `1 <= k <= n`, scanning costs `O(n log k)`, sorting the result costs `O(k log k)`, and auxiliary space is `O(k)`. For `k = 0`, this implementation still validates all inputs and therefore costs `O(n)` time and `O(1)` auxiliary space. For `k` close to `n`, sorting a copy may be simpler and faster. Priority-queue iteration is not sorted, so the final explicit sort is necessary.

**Self-check.** Test `k = 0`, `k = n`, duplicates, negative values, and invalid `k`. Explain why arbitrary priority-queue iteration cannot be returned directly.

## E.13 Backend design exercise: idempotent order plus outbox

**Problem.** Design `POST /orders` so a client can safely retry after a timeout. The service must create one logical order and eventually emit `OrderCreated` without a distributed transaction.

**Hint.** Separate client command identity, local atomic state, relay delivery, and consumer effect identity.

**Selected solution.** Require an idempotency key scoped to authenticated customer and operation. In one database transaction:

1. use a database-tested atomic insert-if-absent or upsert for an idempotency row containing `(customer, operation, key)`, a canonical request fingerprint, status `IN_PROGRESS`, and a generated order ID under a unique constraint;
2. when that operation reports an existing row, lock/read it where the database permits the read in the same transaction; if a uniqueness error aborts the transaction, roll it back and read in a new transaction. Reject a different fingerprint, return the stored completed response, or report/internally coordinate an in-progress attempt;
3. insert the order using the reserved order ID;
4. insert a versioned outbox event with a stable event ID and aggregate ID;
5. store the completed response/status in the idempotency row; and
6. commit.

A relay claims committed outbox rows, publishes, and records progress. If it crashes after broker acceptance but before progress commit, it republishes. Consumers therefore record event ID in an inbox or use a naturally idempotent conditional update in the same transaction as their effect. This is recoverable at-least-once delivery; it does not promise one physical delivery.

The HTTP request has an end-to-end deadline. A timeout is an unknown outcome, so the client retries the same key or queries order status. The key record's retention exceeds the maximum retry window. Cleanup never removes an in-progress record without reconciliation. Metrics include key conflicts, in-progress age, outbox age, publish attempts, poison events, and consumer duplicates.

Security rules include binding the key to customer identity, limiting key length and cardinality, comparing request fingerprints, redacting payloads, and authorizing status lookup. Reliability rules include a unique database constraint as the concurrent truth, bounded relay batches, leases, backoff, and dead-letter/repair workflow.

**Self-check.** Your answer must identify these crash windows: before commit, after commit before response, after publish before relay acknowledgement, and during consumer effect. For each, state the durable record that permits recovery and the stable identity that prevents duplicate logical effects.

## E.14 Low-level design exercise: notification delivery

**Problem.** Design a notification component supporting email and SMS today, another channel later, templates, user preferences, retries, and audit. Do not build a pattern catalog for its own sake.

**Hint.** Separate application orchestration, policy, rendering, channel adapter, durable command state, and delivery attempts.

**Selected solution.** A compact domain API might be:

```java
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

enum Channel { EMAIL, SMS }

record NotificationCommand(String commandId, String userId,
        String templateId, Map<String, String> parameters, Instant createdAt) {
    public NotificationCommand {
        parameters = Map.copyOf(parameters);
    }
}

record Recipient(String address, Locale locale) {}
record RenderedMessage(String subject, String body) {}
record DeliveryResult(String providerMessageId, Instant acceptedAt) {}

interface PreferencePolicy {
    java.util.List<Channel> allowedChannels(String userId);
}

interface TemplateRenderer {
    RenderedMessage render(String templateId, Locale locale,
            Map<String, String> parameters);
}

interface ChannelSender {
    Channel channel();
    DeliveryResult send(String idempotencyKey, Recipient recipient,
            RenderedMessage message) throws DeliveryException;
}

final class DeliveryException extends Exception {
    private final boolean retryable;

    DeliveryException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    boolean retryable() {
        return retryable;
    }
}
```

`NotificationService` validates and durably stores the command before asynchronous processing. `PreferencePolicy` is policy; `TemplateRenderer` owns safe template expansion; each `ChannelSender` is a Strategy and vendor adapter. A factory or map selects a sender by its declared channel. Metrics can be a Decorator, but retry belongs in orchestration because it needs durable attempt state and idempotency.

The command ID is the logical idempotency identity. A delivery table records channel, recipient version, template version, attempt, next eligible time, status, and safe provider response. Workers claim bounded batches with a lease. Retryable errors receive jittered backoff under an attempt/deadline policy; permanent address or authorization errors do not retry. Provider timeouts are unknown outcomes, so the sender passes a stable idempotency key when supported or reconciles provider status.

Preferences and destination addresses can change. Decide whether the command snapshots them at acceptance or resolves them at delivery; that business rule affects audit and privacy. Templates require versioning so a retry does not silently change content. Audit records exclude secrets and sensitive body content unless retention policy explicitly permits it.

Adding push notification introduces one `Channel`, sender, address policy, and integration tests without changing existing senders. This is Open/Closed Principle applied at an observed variation boundary. The system still needs rate limits per tenant/channel, provider bulkheads, queue bounds, graceful shutdown, and deletion/redaction workflows.

**Self-check.** State ownership, lifecycle, idempotency, retry classification, timeout outcome, template/preference version, provider isolation, audit sensitivity, and extension path. If your design sends directly in the request transaction, explain how it recovers from every crash window.

## E.15 Final self-assessment rubric

Score each selected solution from 0 to 2 on each dimension:

A score below 10 suggests revisiting the chapter before adding more problems. A score of 12 or more is interview-ready only if you can reproduce the reasoning without seeing the solution. Practice explaining each answer in three layers: a 30-second summary, a two-minute model, and a detailed defense with code and edge cases.

| Dimension | 0 | 1 | 2 |
|---|---|---|---|
| Contract | assumptions missing | partial contract | inputs, outputs, failure, ownership explicit |
| Invariant | absent | named but not used | drives representation and proof |
| Correctness | example only | plausible explanation | general argument plus dry run |
| Complexity | missing/wrong | time only | time, space, assumptions, constants discussed |
| Edge cases | none | common cases | boundaries, invalid input, overflow/null/concurrency |
| Production | ignored | generic note | limits, observability, lifecycle, failure policy |
| Communication | code dump | understandable | answer-first, structured, trade-offs explicit |
