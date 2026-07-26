# Chapter 6: Runtime Data Areas

## Learning objectives

- Identify JVM runtime data areas and whether they are shared or per thread.
- Describe frames, local-variable arrays, operand stacks, program counters, heaps, runtime constant pools, and native method stacks.
- Place HotSpot Metaspace, code cache, and TLABs outside the strict JVMS abstraction.
- Trace references across stack frames, heap objects, and class metadata.
- Diagnose common exhaustion errors by region rather than treating all memory as heap.

## Why this matters at SDE-2

Memory incidents require a map. A heap dump cannot explain every native leak; increasing `-Xmx` does not fix stack overflow; lowering thread count can recover native memory without changing live heap; generated classes can exhaust metadata; and JIT compilation can fail when a code cache fills. The map also grounds pass-by-value, recursion, allocation, garbage collection, and concurrency.

Interview answers often misuse phrases such as "the object is on the stack" or "static variables live in Metaspace." An SDE-2 should distinguish an object, a reference value, specified logical storage, and one runtime's physical representation.

## First-principles model

Execution needs three broad kinds of state:

1. Shared program and object state available to multiple threads.
2. Per-thread control state describing active calls and next execution points.
3. Runtime implementation state supporting compilation, collection, and native integration.

```text
JVM process
|
+-- shared logical state
|   +-- heap: objects and arrays
|   +-- per-class structures and runtime constant pools
|
+-- thread T1
|   +-- pc
|   +-- JVM stack: frame -> frame -> frame
|   +-- native method execution support
|
+-- thread T2
|   +-- pc
|   +-- JVM stack: frame -> frame
|   +-- native method execution support
|
+-- implementation memory
    +-- generated code, compiler/GC data, native allocations
```

> **Specification boundary:** The JVMS defines logical runtime data areas and permitted failures. It deliberately allows implementation freedom over contiguous memory, exact location, allocation strategy, and whether some areas are fixed or dynamically expanded.

## Core terminology

- **Heap:** Shared runtime area from which storage for all class instances and arrays is allocated in the JVM model.
- **JVM stack:** Per-thread area containing frames for Java method invocations.
- **Frame:** Per-invocation state with local variables, operand stack, and dynamic linking/return support.
- **Local-variable array:** Indexed frame slots holding parameters and locals in the abstract execution model.
- **Operand stack:** LIFO workspace used by JVM instructions.
- **PC register:** Per-thread current instruction address/position for non-native execution in the abstract model.
- **Method area:** Shared logical area for per-class structures, method data/code, and runtime constant pools.
- **Runtime constant pool:** Per-class runtime representation derived from the class-file constant pool.
- **Native method stack:** Implementation support for methods outside JVM bytecode.
- **Metaspace:** HotSpot native-memory implementation for much class metadata.
- **TLAB:** HotSpot thread-local allocation buffer carved from shared heap space.

## Detailed mechanics

The heap stores arrays and class instances. A reference value in a local slot or field can designate a heap object or be `null`. The specification does not expose a raw address, and a collector can relocate an object while preserving reference semantics. Heap storage is shared, but reachability and synchronization determine whether threads can safely use an object.

Each started thread has a JVM stack. Invoking a Java method creates a new frame for that invocation; normal return or abrupt exception unwinding destroys it. A frame's size can be derived from class-file method metadata plus implementation needs. Excessive call depth can produce `StackOverflowError`. Failure to create or expand a stack can lead to `OutOfMemoryError` in permitted circumstances.

The local-variable array uses slots. Instance method slot 0 initially holds `this`; subsequent slots hold parameters. `long` and `double` have historical two-slot treatment in class-file frame representation, although implementations can optimize physical storage. A source local can share a slot with a later non-overlapping local. Names exist only if optional debug metadata preserves them.

The operand stack evaluates expressions and passes invocation arguments in bytecode. Instructions push constants or loaded locals, consume operands, and push results. Its maximum depth is stored in method class-file metadata. It is not the same thing as the entire JVM stack.

The PC register lets each independently executing thread track its current JVM instruction. For a native method its value is undefined by the abstract specification. A context switch saves enough host/runtime state to resume execution; the JLS does not prescribe OS scheduler mechanics.

The method area is a logical shared area holding per-class structures such as field/method information, method code, and runtime constant pools. The name does not imply a specific physical region. The runtime constant pool includes numeric/string constants and symbolic field/method/type references derived from the class file, with resolution state maintained at runtime.

Native methods use host ABI and implementation machinery, often involving native stacks. A deep or faulty native call can exhaust or corrupt native state independently of Java heap correctness.

> **HotSpot note:** Modern HotSpot stores substantial class metadata in Metaspace, backed by native memory. Generated machine code is placed in segmented code heaps commonly called the code cache. These do not redefine the JVMS method area; they are implementation structures satisfying and extending the abstract model.

TLABs improve allocation scalability. A thread receives a region within the heap and can often allocate by advancing a pointer without contending on a global allocator. The buffer is thread local as an allocation mechanism, but objects allocated there are ordinary heap objects and can immediately escape to other threads. Large allocations or exhausted TLABs take other paths.

## Worked Java example

```java
public final class AreaWalkthrough {
    private static int created;

    private final int id;

    AreaWalkthrough(int id) {
        this.id = id;
        created++;
    }

    static int score(AreaWalkthrough item, int bonus) {
        int base = item.id * 10;
        return base + bonus;
    }

    public static void main(String[] args) {
        AreaWalkthrough item = new AreaWalkthrough(7);
        int result = score(item, 3);
        System.out.println(result + ":" + created);
    }
}
```

The likely output is `73:1`. The important lesson is where values conceptually participate, not a claim that a particular JIT leaves each value in the named region.

## Execution or memory walkthrough

Before construction, initialization establishes class state including static `created`. In `main`:

```text
Shared heap/class state                   main frame
+-----------------------------+          +-----------------------+
| object #A                   |<---------| local: item reference |
|   id = 7                    |          | local: result = 73    |
+-----------------------------+          | operand stack ...     |
| class state: created = 1    |          +-----------------------+
+-----------------------------+
```

Step by step:

1. `new` obtains object storage, generally from the heap model, and default-initializes `id` to 0.
2. The constructor frame receives the new reference as `this` and integer 7 as a parameter.
3. `putfield`-style behavior assigns 7 to the object's `id`; static state `created` is read, incremented, and written.
4. The constructor frame returns. The reference remains in `main`'s local state.
5. Calling `score` creates a frame. Its locals include the copied reference value and copied integer 3.
6. Bytecodes load the reference, read `id`, multiply, store or retain `base`, add `bonus`, and return 73.
7. The `score` frame disappears; the integer result is stored in `main`'s frame.
8. After `main` returns, `item` no longer exists as a local root. The object becomes collectible unless another reference was stored elsewhere.

Under optimization, `score` can be inlined, `base` can remain in a register, and the object could potentially be scalar-replaced if escape analysis proves it unnecessary. The source-level semantics remain the same.

## Complexity and performance

`score` is O(1) time and space. Each non-inlined invocation consumes frame state proportional to method metadata and implementation needs. Recursion consumes O(depth) logical call state unless optimized in ways Java does not guarantee.

Allocation through a TLAB can be extremely cheap, often resembling pointer bump plus initialization. Cost is deferred rather than absent: the collector later processes live/dead objects, and TLAB refills and large objects require slower paths. Code-cache pressure can reduce compilation effectiveness; metadata pressure can trigger class unloading attempts; too many threads can consume large native stack reservations.

## Edge cases and common mistakes

- Saying local variables always physically live on a native stack. Optimized values can live in registers or disappear.
- Saying all objects always physically remain in heap memory. The JVM heap is the semantic allocation model, but scalar replacement may eliminate an allocation.
- Calling Metaspace the heap or saying static object values are stored "in Metaspace." Static state contains values/references associated with class runtime data; referenced objects remain ordinary objects.
- Confusing the operand stack with the per-thread JVM stack.
- Assuming thread-local allocation means thread-confined objects.
- Confusing the runtime constant pool with string interning.
- Tuning only `-Xmx` when native stacks or metadata cause process exhaustion.
- Expecting a heap dump to contain direct native memory or all JVM structures.

## Production engineering notes

Match symptom to evidence:

| Symptom | First regions/evidence to inspect |
|---|---|
| `Java heap space` | live-set growth, allocation rate, heap dump, GC behavior |
| `Metaspace` | loaded class counts, class loaders, generated classes |
| `StackOverflowError` | recursion/call cycle and per-thread stack sizing |
| unable to create native thread | thread count, stack size, process/container limits, native memory |
| code cache warnings | compilation logs/JFR and code-cache status |
| container OOM kill without Java OOME | total RSS, native buffers, stacks, JVM/native structures |

Do not react to reserved address space as if it were committed resident memory. OS virtual size, committed JVM memory, and RSS are different. For native growth in HotSpot, Native Memory Tracking can help if enabled with an acceptable overhead and the relevant allocations are tracked.

## Interview questions and model answers

**Which JVM areas are shared and which are per thread?**

The heap and method-area/runtime class structures are shared. Each thread has a PC, JVM stack with frames, and native execution support. Implementation structures such as a shared code cache and per-thread TLABs must be labeled separately.

**What is in a stack frame?**

The abstract frame contains local-variable slots, an operand stack, and information supporting dynamic linking, return, and exceptions. A JIT may inline or optimize physical frames, so a source invocation is not always a materialized native frame.

**Is a TLAB outside the heap?**

No. In HotSpot it is a thread-owned allocation slice of the heap. Ownership reduces allocation contention; it does not imply that allocated objects are forever thread local.

**What is the relationship between method area and Metaspace?**

The method area is a JVMS logical concept for per-class structures. Metaspace is HotSpot's native-memory implementation for much class metadata. They should not be treated as specification synonyms.

## Exercises

1. Draw frames and heap state for the worked example at each method call.
2. Use `javap -c -v` to find `max_stack` and `max_locals` for `score`.
3. Write bounded recursion and observe how failure changes with stack-size settings in a safe local process.
4. Create many dynamic proxy classes with isolated loaders and predict the affected region.
5. Build a complete memory budget including heap, stacks, metadata, code cache, direct buffers, and margin.

## Chapter summary

The runtime data-area model separates shared objects and class state from per-thread execution state. Frames use local slots and operand stacks; the PC tracks instruction progress; native calls require host support. HotSpot adds concrete structures such as Metaspace, code cache, and TLABs. Correct reasoning follows references and execution state while avoiding unsupported claims about exact physical placement.

## Revision checklist

- [ ] I can classify each runtime data area as shared or per thread.
- [ ] I can distinguish a JVM stack, frame, local array, and operand stack.
- [ ] I can trace a reference from a local slot to a heap object.
- [ ] I can explain method area versus Metaspace and heap versus TLAB.
- [ ] I know why process memory can exceed heap memory.
- [ ] I can map common exhaustion symptoms to the right evidence.

