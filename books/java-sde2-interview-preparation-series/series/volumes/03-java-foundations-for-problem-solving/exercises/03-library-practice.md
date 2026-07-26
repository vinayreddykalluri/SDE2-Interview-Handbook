# Practice Lab C - Wrappers, Generics, Collections, Exceptions, I/O

**Start here:** select an abstraction from the required behavior before naming an implementation. State ordering, duplicates, null policy, mutation, lookup direction, failure behavior, and resource ownership. Only then discuss expected cost or write code.

Answer K51-K75, predict O39-O57, compile every D39-D57 repair, and implement C39-C57 with one normal and two boundary tests. For exception and I/O tasks, mark the ownership boundary and decide whether the method catches, translates, or propagates each failure. Use Solution Studio C after attempting the work; a remembered API name without its contract does not count as mastery.

## Knowledge check

- **K51 [Foundation]** Map each primitive to its wrapper class.
- **K52 [Foundation]** Distinguish parsing, boxing, and unboxing.
- **K53 [Interview Core]** Why can null unboxing fail far from the source of null?
- **K54 [Interview Core]** Why is wrapper identity not a value contract?
- **K55 [Foundation]** What type safety does `List<String>` provide?
- **K56 [Foundation]** Why can primitives not be generic arguments?
- **K57 [Interview Core]** What does the diamond operator infer?
- **K58 [Interview Core]** What operation does a simple upper bound permit?
- **K59 [Foundation]** Why is an enum safer than magic strings for finite state?
- **K60 [Interview Core]** Why should enum ordinal rarely be persisted?
- **K61 [Foundation]** Choose List, Set, Map, Queue, Deque, or PriorityQueue by need.
- **K62 [Interview Core]** Why is `LinkedList` not always faster for insertion?
- **K63 [Interview Core]** Qualify the expected-cost claim for `HashMap`.
- **K64 [Foundation]** Distinguish `offer`/`poll`/`peek` from throwing queue methods.
- **K65 [Interview Core]** Why does PriorityQueue iteration not establish order?
- **K66 [Interview Core]** State a safe comparator sign contract.
- **K67 [Foundation]** Distinguish checked exceptions, unchecked exceptions, and errors.
- **K68 [Interview Core]** Distinguish `throw` from `throws`.
- **K69 [Interview Core]** What does try-with-resources guarantee about close order?
- **K70 [Interview Core]** Why must catch blocks go from specific to general?
- **K71 [Foundation]** When is `Scanner` sufficient?
- **K72 [Interview Core]** Why can `BufferedReader` plus token parsing be preferable?
- **K73 [Interview Core]** Who should close a resource?
- **K74 [Interview Core]** Distinguish fixed-size, unmodifiable, and immutable collections.
- **K75 [SDE-2 Follow-up]** Why is broad `catch (Exception)` dangerous inside core logic?

## Predict the output

- **O39 [Foundation]** Predict `Integer.parseInt("42") + 1`.
- **O40 [Interview Core]** Predict unboxing a null `Integer`.
- **O41 [Interview Core]** Predict `Integer.valueOf(127) == Integer.valueOf(127)`.
- **O42 [Interview Core]** Predict value equality for two boxed 128 values.
- **O43 [Foundation]** Predict `new Box<>("x").value()`.
- **O44 [Foundation]** Predict an enum switch result for `PAID`.
- **O45 [Foundation]** Predict a HashSet size after adding 2, 2, and 3.
- **O46 [Foundation]** Predict a frequency map count after merging three equal keys.
- **O47 [Foundation]** Predict FIFO removals from an ArrayDeque.
- **O48 [Foundation]** Predict LIFO removals using `push` and `pop`.
- **O49 [Interview Core]** Predict the first value polled from a min-heap containing 9, 3, 5.
- **O50 [Interview Core]** Predict comparator sign from `Integer.compare(MAX_VALUE, -1)`.
- **O51 [Foundation]** Predict `Arrays.asList("a","b").set(0,"x")` contents.
- **O52 [Interview Core]** Predict the result of adding to that fixed-size list.
- **O53 [Interview Core]** Predict the size of `Arrays.asList(new int[]{1,2})`.
- **O54 [Interview Core]** Predict which catch handles `NumberFormatException`.
- **O55 [Interview Core]** Predict resource close order for two declared resources.
- **O56 [Foundation]** Predict `Character.digit('A', 16)`.
- **O57 [SDE-2 Follow-up]** Predict `Math.addExact(Integer.MAX_VALUE,1)`.

## Debug the code

- **D39 [Foundation]** Repair `List<int>`.
- **D40 [Interview Core]** Repair a nullable wrapper used in arithmetic.
- **D41 [Interview Core]** Repair wrapper comparisons that use `==`.
- **D42 [Foundation]** Repair a raw `List` that permits mixed values.
- **D43 [Interview Core]** Repair a generic `first` method that accepts an empty list silently.
- **D44 [Foundation]** Replace string order states with an enum.
- **D45 [Interview Core]** Repair a switch that forgets a new enum state.
- **D46 [Foundation]** Repair a deduplication loop that still appends duplicates.
- **D47 [Interview Core]** Repair a frequency map that treats missing as null arithmetic.
- **D48 [Interview Core]** Replace legacy `Stack` with ArrayDeque operations.
- **D49 [Interview Core]** Repair a comparator implemented as subtraction.
- **D50 [Interview Core]** Repair code that assumes PriorityQueue iteration is sorted.
- **D51 [Interview Core]** Repair enhanced-for structural removal.
- **D52 [Interview Core]** Repair a mutable hash-map key.
- **D53 [Foundation]** Repair catch blocks ordered superclass before subclass.
- **D54 [Interview Core]** Repair a `finally` block that returns.
- **D55 [Interview Core]** Replace manual resource closing with try-with-resources.
- **D56 [Foundation]** Repair Scanner code that reads an empty token after mixed line/token calls.
- **D57 [SDE-2 Follow-up]** Repair library code that closes a caller-owned stream.

## Small coding tasks

- **C39 [Foundation]** Parse an integer and report invalid input without hiding other defects.
- **C40 [Interview Core]** Convert nullable boxed counts to a defined primitive total safely.
- **C41 [Foundation]** Implement a generic immutable `Box<T>`.
- **C42 [Interview Core]** Implement a generic `first` method with an empty-input contract.
- **C43 [Foundation]** Model order status with an enum method `isTerminal`.
- **C44 [Foundation]** Use ArrayList to build a result sequence.
- **C45 [Foundation]** Deduplicate integers with HashSet.
- **C46 [Interview Core]** Preserve first-seen order with LinkedHashSet.
- **C47 [Interview Core]** Count word frequencies with HashMap.merge.
- **C48 [Foundation]** Implement FIFO operations with ArrayDeque.
- **C49 [Foundation]** Implement LIFO operations with ArrayDeque.
- **C50 [Interview Core]** Find three smallest values with PriorityQueue.
- **C51 [Interview Core]** Sort records using `comparingInt` and a tie-breaker.
- **C52 [Interview Core]** Remove matching list elements safely.
- **C53 [Foundation]** Catch and explain NumberFormatException at an input boundary.
- **C54 [Interview Core]** Implement a checked file-reading helper with a clear throws contract.
- **C55 [Interview Core]** Use try-with-resources with BufferedReader.
- **C56 [Foundation]** Read an integer array with Scanner.
- **C57 [SDE-2 Follow-up]** Read and validate a jagged matrix with BufferedReader and StringTokenizer.

## Interview follow-ups

- **F27 [Interview Core]** When does boxing create hidden null or allocation risk?
- **F28 [SDE-2 Follow-up]** Why should a cache range never become business logic?
- **F29 [Interview Core]** What error does generics prevent before runtime?
- **F30 [Interview Core]** When should a generic bound stay out of a beginner API?
- **F31 [Interview Core]** How does enum evolution affect persistence and switches?
- **F32 [SDE-2 Follow-up]** Defend a collection choice using semantics before complexity.
- **F33 [Interview Core]** When is insertion order a correctness requirement?
- **F34 [Interview Core]** Why is ArrayDeque usually preferred to Stack?
- **F35 [SDE-2 Follow-up]** Explain PriorityQueue's head guarantee and iterator non-guarantee.
- **F36 [Interview Core]** When should an unchecked exception represent a violated precondition?
- **F37 [SDE-2 Follow-up]** Preserve a primary exception when resource closing also fails.
- **F38 [SDE-2 Follow-up]** Define resource ownership in a reusable parsing method.

## Cumulative assessment 3

**A03:** In 45 minutes, read tokens, build a frequency map, keep the first-seen order of unique values, and print the top three by frequency using a safe comparator. Define null, malformed-input, tie, and empty-input behavior.

## Cumulative assessment 4

**A04:** Review a 40-line solution that uses raw collections, wrapper identity, comparator subtraction, broad catches, and a caller-owned reader. Produce a behavior-preserving refactor and an evidence-based defect list.
