# Chapter 7: Object Creation and Memory Layout

## Learning objectives

- Trace a `new` expression through class readiness, allocation, zeroing, and constructor execution.
- Distinguish object semantics from HotSpot headers, alignment, TLABs, and compressed references.
- Explain default values, reference assignment, and constructor failure.
- Reason about escape analysis, allocation elimination, and the "stack allocation" misconception.
- Distinguish shallow size, deep size, and retained size.

## Why this matters at SDE-2

Allocation rate, not just live heap, drives garbage-collection work. Object layout affects cache locality and capacity planning. Unsafe publication can expose constructor-related correctness bugs. Heap analysis depends on understanding that an object's retained size is graph-dependent, not simply its field total.

Interviewers also use object creation to see whether a candidate can separate layers. Java guarantees default initialization and constructor semantics. It does not guarantee a mark word, an 8-byte alignment boundary, compressed ordinary object pointers, or that every syntactic allocation becomes a heap allocation in optimized machine code.

## First-principles model

An object combines identity, class membership, and instance state. Evaluating a class instance creation expression conceptually:

1. Ensures the target class is loaded, linked, and initialized as required.
2. Obtains storage for a new instance.
3. Assigns default values to every instance field.
4. Evaluates constructor arguments left to right.
5. Invokes the selected constructor, beginning with a superclass-constructor chain.
6. Executes instance field initializers and initializer blocks in specified order for each class level.
7. Produces a reference to the initialized object if construction completes normally.

```text
new Account("A-1", 100)
      |
      +-- allocate identity and default state
      |      id=null, balance=0
      +-- Object constructor
      +-- Account field initializers / initializer blocks
      +-- Account constructor body
      v
reference value to constructed Account
```

If construction throws, the expression produces no normal result. The allocated object can still escape if constructor code published `this` before failure, which is one reason early escape is dangerous.

## Core terminology

- **Allocation:** Obtaining storage and runtime identity for an object or array.
- **Default initialization:** Setting reference fields to `null`, numeric fields to zero, `boolean` to `false`, and `char` to zero before Java initializer code.
- **Object header:** Implementation metadata preceding or accompanying instance fields.
- **Mark word:** HotSpot header word commonly encoding identity hash, lock/age/GC state in a layout that varies by mode and release.
- **Klass pointer:** HotSpot metadata reference identifying the runtime class.
- **Alignment:** Placement at address multiples chosen by the VM, potentially requiring padding.
- **Compressed oops:** HotSpot encoding that represents many object references in fewer bits under supported heap/addressing conditions.
- **Escape analysis:** Compiler analysis of whether a reference escapes a method, thread, or analysis scope.
- **Scalar replacement:** Replacing an aggregate allocation with individual scalar values when identity/escape constraints allow.
- **Shallow size:** Storage directly occupied by one object, excluding referenced objects.
- **Retained size:** Storage that becomes unreachable if a particular object is removed from the reachability graph.

## Detailed mechanics

The bytecode instruction `new` names a class through the runtime constant pool and creates an uninitialized reference used under verifier-enforced rules. `dup` commonly preserves a reference while constructor arguments are prepared, and `invokespecial <init>` invokes the constructor. A constructor is not an ordinary dynamically dispatched method and has no return type, not even `void`.

Before constructor code observes fields, allocation supplies their default values. Source field initializers are compiled into constructors after the superclass constructor invocation and before the rest of that constructor's body. If one constructor delegates to another using `this(...)`, the delegated constructor performs initialization once; Java requires a `this(...)` or `super(...)` invocation to be the constructor's first statement under the applicable language rules.

A JVM must preserve object semantics, but it need not use one allocation algorithm. A general allocator could find a free block. A compacted region supports fast pointer-bump allocation: return the current top and advance it by aligned object size. Concurrent threads require coordination.

> **Specification boundary:** The JLS defines evaluation, initialization, constructor, field, identity, and reference semantics; the JVMS defines object/array allocation instructions and verification constraints. Neither specification fixes an object header, byte offset, field order, alignment, TLAB policy, or compressed-reference encoding.

> **HotSpot note:** HotSpot usually gives allocating threads TLABs in the shared heap. A fast-path allocation can advance a thread-local pointer, reducing contention. TLAB refill, large-object handling, slow-path allocation, and collector-specific regions vary by collector and release.

Physical object layout is implementation-specific. A common HotSpot mental model is:

```text
+---------------------------+
| mark word                 | implementation metadata
+---------------------------+
| class/Klass metadata ref  | possibly compressed
+---------------------------+
| instance fields           | superclass fields included
+---------------------------+
| internal/tail padding     | alignment-dependent
+---------------------------+
```

Field declaration order is not a portable layout contract. A JVM may arrange fields to reduce padding while honoring semantic constraints. Object size depends on VM bitness, compressed-reference modes, field mix, header scheme, and alignment. Arrays add length metadata and repeated element storage. A `boolean` field has language values `true` and `false`; its exact physical bit/byte size inside an object is not specified.

Compressed ordinary object pointers encode references using a base and scale or related addressing mode, allowing a 32-bit encoded value to address a larger aligned heap. Compressed class pointers can separately reduce the metadata reference. Whether these modes activate and their address range are HotSpot/version/configuration details.

Assignment copies a reference value; it does not copy the object:

```java
Account second = first;
```

Afterward, both variables can designate the same object. Reassigning `second` changes only that variable. Mutating through either reference changes shared object state.

Escape analysis allows optimization beyond the source model. If an object never escapes an analyzed compilation scope and identity is not observably required, a JIT may replace its fields with scalar values, eliminate synchronization, or eliminate the allocation. This is often loosely described as "putting the object on the stack," but scalar replacement can mean no object exists at all in generated code. Java does not guarantee stack allocation, and an allocation can be materialized during deoptimization.

Size terminology needs graph precision. Deep size recursively adds a chosen object's reachable graph but must define how shared objects are counted. Retained size uses dominators: if every path from a GC root to object B passes through A, A dominates B, so B contributes to A's retained set. Removing one ordinary reference to A does not necessarily collect A if another root path exists.

## Worked Java example

```java
public final class Account {
    private static int nextSequence;

    private final int sequence = ++nextSequence;
    private final String id;
    private long cents;
    private boolean active;

    Account(String id, long openingCents) {
        if (id == null || openingCents < 0) {
            throw new IllegalArgumentException();
        }
        this.id = id;
        this.cents = openingCents;
        this.active = true;
    }

    long balance() {
        return cents;
    }

    public static void main(String[] args) {
        Account first = new Account("A-1", 10_000);
        Account alias = first;
        alias.cents += 500;
        alias = new Account("A-2", 2_000);
        System.out.println(first.balance() + ":" + alias.sequence);
    }
}
```

The output is `10500:2` in this single-threaded program. The first assignment aliases; the second `new` plus reassignment does not change what `first` references.

## Execution or memory walkthrough

For the first construction:

1. `Account` initialization establishes `nextSequence` at 0.
2. Storage is obtained. Fields initially contain `sequence=0`, `id=null`, `cents=0`, and `active=false`.
3. `Object` construction completes.
4. The instance initializer expression `++nextSequence` writes 1 to class state and 1 to `sequence`.
5. Constructor validation executes. `id`, `cents`, and `active` receive their explicit values.
6. The resulting reference is stored in `first`.
7. `alias = first` copies the same reference. Mutation through `alias` changes the one object to 10,500 cents.
8. The second construction increments class state to 2 and produces another object. Reassigning `alias` changes the local reference graph only.

```text
main locals                  heap graph
first ---------------------> Account #1
                               sequence=1
                               id ---------> String "A-1"
                               cents=10500
alias ---------------------> Account #2
                               sequence=2
                               id ---------> String "A-2"
                               cents=2000
```

The shallow size of Account #1 includes its header, field encodings, and padding, not the `String` object's storage. Its retained size may include the `String` only if no other root path retains that string and Account #1 dominates it.

## Complexity and performance

Fast-path allocation is often amortized O(1), while initialization time is at least proportional to storage that must be zeroed or otherwise made semantically zero. Collector work accounts for allocation rate and survival. Constructor body complexity is arbitrary.

Reducing object count can improve locality and lower GC metadata/tracing work, but manual pooling is often harmful. Pools retain objects, add synchronization and reset complexity, and defeat generational collection's strength with short-lived objects. Pool scarce external resources such as connections, not ordinary DTOs, unless measurement proves a need.

Field layout can create padding and cache effects, but changing domain design solely to save a few bytes is premature until a heap histogram shows multiplicity makes it material. Measure the deployed JVM configuration with a trustworthy layout tool; never hard-code an assumed shallow size into correctness logic.

## Edge cases and common mistakes

- Publishing `this` from a constructor via listeners, static collections, callbacks, or starting a thread.
- Calling an overridable method from a constructor; the subclass override can observe uninitialized subclass fields.
- Believing a `final` reference makes the referenced object immutable.
- Believing reference assignment clones an object.
- Treating header size or field order as a Java guarantee.
- Equating escape analysis with guaranteed stack allocation.
- Using shallow size to estimate the memory released by clearing a cache.
- Forgetting arrays are objects with header/length/alignment overhead.
- Assuming a failed constructor means the object could never have escaped.

## Production engineering notes

Use allocation profiling to find hot allocation sites and heap histograms/dominator analysis to find retention. These are different questions. A high allocation rate with stable live set can cause GC CPU but not a leak. A slowly growing retained graph can leak with a modest allocation rate.

Prefer immutable objects with constructor validation and no `this` escape. Final-field visibility has useful JMM guarantees only when construction and publication rules are respected. For memory estimates, include backing arrays, object graphs, duplicate strings, alignment, and load factors in collections.

> **HotSpot note:** Header formats have evolved, including locking changes and optional compact-object-header work in some releases/configurations. Always label diagrams with exact JDK, VM, architecture, alignment, and compression settings when numbers matter.

## Interview questions and model answers

**What happens when Java executes `new`?**

The runtime ensures the class is ready, obtains and default-initializes object storage, then constructor invocation runs the superclass chain, instance initializers, and constructor body. A conforming JVM preserves those effects, while TLAB allocation and header layout are implementation choices.

**Are Java objects allocated on the stack or heap?**

Semantically, object and array storage comes from the JVM heap. An optimizing JIT may prove an allocation non-escaping and scalar-replace it, so the physical object may not exist. Java provides no general stack-allocation guarantee.

**What are mark word and Klass pointer?**

They are HotSpot object-header concepts. The mark word can encode GC, locking, age, and hash-related state; the class metadata reference identifies the runtime type. Their exact format is not defined by Java specifications.

**Shallow size versus retained size?**

Shallow size is direct object storage. Retained size is the size of objects that would become unreachable if this object were removed, determined by root paths and dominance. It is a graph property, not a sum of field types.

## Exercises

1. Draw default and post-constructor state for `Account`.
2. Inspect constructor bytecode and identify `new`, `dup`, and `invokespecial` at a call site.
3. Create a constructor that leaks `this`; explain possible observations without relying on a reproducible failure.
4. Estimate the shallow size of a sample object under stated assumptions, then list why the result is non-portable.
5. Given a heap graph with shared children, compute shallow, deep-under-a-stated-rule, and retained sizes.

## Chapter summary

Object creation combines allocation, mandatory default state, a superclass/initializer/constructor sequence, and reference production. Java specifies semantic effects but delegates headers, field placement, alignment, TLABs, and compressed references to implementations. Escape analysis may eliminate allocations without creating a portable stack-allocation feature. Memory analysis must distinguish allocation rate, shallow storage, and graph-based retention.

## Revision checklist

- [ ] I can trace `new` through default initialization and constructors.
- [ ] I can separate Java guarantees from HotSpot layout details.
- [ ] I can explain TLABs without calling their objects thread local.
- [ ] I understand reference aliasing and reassignment.
- [ ] I can explain escape analysis and scalar replacement precisely.
- [ ] I can distinguish shallow, deep, and retained size.
