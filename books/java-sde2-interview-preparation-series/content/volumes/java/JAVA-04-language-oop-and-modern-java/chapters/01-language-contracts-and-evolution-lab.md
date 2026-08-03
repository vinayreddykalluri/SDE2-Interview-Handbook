# Language Contracts and API Evolution Lab

Advanced Java interviews rarely reward a list of keywords. They present two legal implementations and ask which one preserves the contract when subtypes, generic callers, reflection, failure, or a new Java release enters the picture.

This lab gives you one reusable method:

```text
call site -> compile-time selection -> runtime dispatch -> state/failure contract
          -> compatibility risk -> test that exposes the edge
```

It assumes the Fundamentals book has already taught basic classes, interfaces, generics, records, enums, exceptions, and lambdas. Here we concentrate on the mechanism at their boundaries.

## Method selection happens in two stages

Given:

```java
class Parent {
    String convert(Number value) { return "parent-number"; }
    String describe() { return "parent"; }
    static String label() { return "parent-static"; }
}

class Child extends Parent {
    String convert(Integer value) { return "child-integer"; }
    @Override String describe() { return "child"; }
    static String label() { return "child-static"; }
}

Parent reference = new Child();
```

Predict these separately:

```java
reference.convert(Integer.valueOf(7));
reference.describe();
Parent.label();
```

### Stage 1: compile-time method selection

The compiler considers methods available from the declared receiver type and the compile-time argument types. `reference` is declared `Parent`, so `Child.convert(Integer)` is not an overload candidate through that expression. The selected signature is `Parent.convert(Number)`.

### Stage 2: runtime dispatch

For a selected overridable instance-method signature, the runtime object chooses the most specific override. `Child` does not override `convert(Number)`, so the parent implementation runs. It does override `describe()`, so `reference.describe()` returns `child`.

Static methods are selected using the qualifying type; they are hidden, not overridden. Fields follow a similar compile-time qualification rule and are not dynamically dispatched.

| Member form | Selected primarily by | Polymorphic at runtime? |
|---|---|---:|
| overload signature | compile-time types | no |
| overridden instance method | selected signature plus runtime receiver | yes |
| static method | qualifying/declared type | no |
| field | qualifying/declared type | no |
| private method | declaring class | no override relationship |

This matrix resolves many “trick” questions without memorizing outputs.

## An override is a behavioral promise

Java's compiler checks signature rules, but substitutability is broader:

- accepted inputs should not become unexpectedly narrower;
- promised outputs and invariants should remain valid;
- new unchecked failures should not surprise ordinary base-contract use;
- side effects, ordering, idempotency, and thread-safety claims must still hold.

A covariant return type can narrow the returned reference type in an override. Checked exceptions can be narrowed or removed, but not broadened beyond the parent declaration. Those language permissions do not automatically prove good API design.

Example: a `ReadOnlyRepository` subtype that throws `UnsupportedOperationException` for a base `save` operation is not a truthful subtype of a mutable repository, even if it compiles. Split the capabilities into smaller interfaces instead of making callers discover the mismatch at runtime.

## Equality across inheritance is a design decision

The `equals` contract requires reflexivity, symmetry, transitivity, consistency, and false for null. Inheritance can make these properties hard to preserve when a subtype adds value state.

Suppose `Point(x,y)` considers any `Point` coordinates, while `ColoredPoint(x,y,color)` also wants color to matter. If the parent accepts the child by coordinates but the child rejects the parent for missing color, symmetry fails. If the child ignores color, distinct domain values collapse.

Practical choices:

1. make a value class final;
2. use composition for additional attributes;
3. define equality at an intentionally shared abstraction and prohibit state that changes it;
4. use identity semantics when entities have lifecycle identity rather than value equality.

Equal objects must have equal hash codes. The reverse is not required. Fields participating in equality should normally remain stable while instances cross hash-based boundaries.

### Records are value-oriented, not deeply immutable

```java
record Snapshot(List<String> labels) {}

List<String> labels = new ArrayList<>(List.of("draft"));
Snapshot snapshot = new Snapshot(labels);
labels.set(0, "changed");
```

The component reference is final, but the referenced list remains mutable. A compact constructor can defensively copy:

```java
record Snapshot(List<String> labels) {
    Snapshot {
        labels = List.copyOf(labels);
    }
}
```

That freezes list membership through this record; it still does not deep-copy mutable elements.

## Generics: write the operation before choosing the wildcard

Ask what the method does with a parameter:

- it only produces `T` values for this method -> `? extends T`;
- it consumes `T` values from this method -> `? super T`;
- it both reads and writes the exact type -> often a named type parameter or invariant type;
- callers do not benefit from variance -> keep the signature simpler.

```java
static <T> void copy(List<? extends T> source,
                     List<? super T> target) {
    for (T value : source) {
        target.add(value);
    }
}
```

`List<Integer>` can produce `Number`; `List<Number>` can consume `Integer`. PECS is a reminder, not a substitute for describing the operation.

### Erasure and inserted casts

Java generally compiles parameterized types through erasure. Type arguments guide compile-time checks and can appear in signatures/metadata, but ordinary runtime objects do not become separate `ArrayList<String>` and `ArrayList<Integer>` classes.

A retrieval from `List<String>` can include a compiler-inserted cast to `String`. If raw code writes an `Integer` into that list, the unsafe write can appear to succeed and the later read fails at the inserted cast. This delayed failure is heap pollution.

```text
parameterized reference -> raw alias -> unchecked write
                                      -> polluted object
parameterized read -> compiler cast -> ClassCastException
```

Do not “fix” the exception by widening every type to `Object`. Remove the raw/unchecked boundary or validate it in one audited adapter.

### Reifiable boundaries

Due to erasure, these are illegal or restricted:

- `new T[10]` for an unconstrained type parameter;
- `value instanceof List<String>`;
- class literals such as `List<String>.class`.

Arrays carry a runtime component type and are covariant; generics are ordinarily invariant and erased. Combining generic varargs with mutation can produce heap pollution. Use `@SafeVarargs` only when the method body truly does not perform potentially unsafe operations on the varargs array or expose it for unsafe mutation.

### Bridge methods

An override involving erased generic signatures can require a synthetic bridge method so JVM-level dispatch still honors source-level polymorphism. `javap -v` can reveal `ACC_BRIDGE` and `ACC_SYNTHETIC`. Reflection clients should decide whether bridge/synthetic methods belong in their scanning contract instead of assuming every declared method came directly from source.

## Sealed hierarchies make a closed-world promise

```java
sealed interface Expression permits Literal, Add {}
record Literal(int value) implements Expression {}
record Add(Expression left, Expression right) implements Expression {}
```

In Java 21, pattern matching for switch can express exhaustive handling:

```java
static int evaluate(Expression expression) {
    return switch (expression) {
        case Literal literal -> literal.value();
        case Add add -> evaluate(add.left()) + evaluate(add.right());
    };
}
```

The benefit is not shorter syntax alone. The model states that the permitted family is controlled, and adding a new permitted subtype forces exhaustive consumers to be reconsidered.

Trade-off: a public extensibility point should not be sealed merely to make one switch convenient. Sealing is an API-evolution decision about who may add implementations.

Null handling is separate. A selector that may be null needs an explicit `case null`, a guard, or a contract that rejects null.

## Lambdas are behavior values, not guaranteed singleton objects

A lambda captures values from its lexical scope. Local variables must be final or effectively final because the captured state represents a stable value, not a shared mutable local slot.

Avoid relying on lambda identity:

```java
Supplier<String> first = () -> "ready";
Supplier<String> second = () -> "ready";
// first == second has no useful semantic contract
```

For retryable, parallel, or lazy pipelines, captured mutable state creates timing-dependent behavior. Make the side effect explicit at a boundary or return a value and combine it with a defined reduction.

Method references follow the same target-typing and overload rules as lambdas. If overload resolution becomes unreadable, use an explicitly typed lambda or helper method.

## Reflection crosses encapsulation and compatibility boundaries

Reflection sees runtime metadata, not source intent. Before building reflective code, define:

- which retention policy an annotation needs;
- whether inherited annotations matter;
- whether synthetic/bridge members are included;
- how private/module access is handled;
- how missing constructors/methods are reported;
- how results are cached without retaining disposable class loaders;
- whether generated code or method handles are a better repeated-call path.

`setAccessible(true)` is not a universal bypass in a strongly encapsulated modular runtime. Access can depend on module readability, exports, opens, lookup context, security policy, and launch configuration.

Reflection wraps failures at boundaries. Preserve the underlying cause from invocation wrappers when translating to a domain-specific error.

## Try-with-resources has a two-failure contract

When the body and `close()` both throw, Java keeps the body failure as primary and attaches the close failure as suppressed:

```text
primary: IllegalArgumentException("body failed")
  suppressed: IllegalStateException("close failed")
```

Resources close in reverse declaration order. A catch block that logs only `getMessage()` can hide suppressed evidence. In production diagnostics, record the whole throwable chain.

If construction of a later resource fails, already-created earlier resources are still closed. The code that creates the resource generally owns closing it; accepting a caller-owned stream changes that responsibility.

## API evolution matrix

| Change | Source compatibility | Binary/runtime risk | Better move |
|---|---|---|---|
| add overload | old source compiles, new calls may become ambiguous | old binaries usually target old descriptor | use distinct name when semantic difference is large |
| add abstract interface method | implementers break on recompilation | old implementations can fail at invocation | default method only with a truthful default |
| add default method | source conflicts possible with multiple interfaces | resolution conflicts possible | test implementer hierarchy |
| change equality fields | source unaffected | stored keys/caches/persistence semantics change | version/migrate contract explicitly |
| seal previously open type | external subclasses stop compiling | existing subclasses can fail linkage/access rules | treat as breaking API change |
| change record component | constructor/accessor/deconstruction shape changes | descriptor/serialization/framework impact | introduce new type/version boundary |
| broaden checked exception | callers must handle | override may be illegal | translate at boundary or add a new operation |

Compatibility has multiple dimensions: source, binary, behavior, serialization, reflection, and data. “It compiles” checks only one.

## Executable contract companion

`LanguageContractChecks.java` validates six high-value boundaries:

1. overload selection versus override dispatch and static hiding;
2. record value equality and shallow component mutability;
3. producer/consumer generic variance;
4. a controlled raw-type heap-pollution failure at the inserted cast;
5. Java 21 exhaustive pattern switch over a sealed hierarchy;
6. primary and suppressed failures in try-with-resources.

```bash
out=$(mktemp -d)
javac --release 21 -Xlint:all -Werror -d "$out" \
  content/volumes/java/JAVA-04-language-oop-and-modern-java/code/LanguageContractChecks.java
java -ea -cp "$out" LanguageContractChecks
```

Expected output:

```text
PASS 6 advanced language contract suites
```

The raw-type operation is isolated under a narrow `@SuppressWarnings` annotation so the full source still passes `-Werror`. The annotation documents a deliberate failure demonstration; it is not a license for raw types in application code.

## Interview room: worked answers

### Why does a child overload not run through a parent reference?

**Model answer:** Overload selection is compile-time and uses the declared receiver and argument types. Runtime dispatch happens only after a signature is selected, and it can choose an override of that signature. A child-only overload is not retroactively added to the parent's compile-time candidate set.

### Can a record be mutable?

**Model answer:** Record component fields are final references/values and generated accessors do not provide reassignment. But a component can refer to a mutable object. Without a defensive copy, mutation through an alias is visible through the record, so records are not automatically deeply immutable.

### Explain `? extends T` versus `? super T` without reciting PECS.

**Model answer:** If this method needs to read values as `T`, an extends-bound lets callers provide a source of a subtype. If it needs to insert `T`, a super-bound accepts a destination whose element type can hold T. If I need both exact reads and writes, I reconsider the signature rather than forcing a wildcard.

### Where does heap pollution fail?

**Model answer:** The unsafe operation can occur at a raw or unchecked write, but the runtime failure often appears later at a compiler-inserted cast on a parameterized read. I trace back to the unchecked boundary instead of blaming the retrieval.

### When should a hierarchy be sealed?

**Model answer:** When the domain genuinely owns and controls the complete family and exhaustive consumers are valuable. I would not seal a public extension point just for a concise switch, because sealing changes the API's evolution and third-party implementation contract.

### Are default methods always backward compatible?

**Model answer:** They can preserve a binary path for adding behavior, but multiple inherited defaults can create conflicts, and a default can violate implementer semantics. I test the real hierarchy and behavioral contract; “default” is not a blanket compatibility guarantee.

### What does `@SafeVarargs` guarantee?

**Model answer:** It is the author's assertion that the method's generic varargs use is safe from heap pollution according to the documented restrictions. The compiler cannot prove the whole claim. I apply it only to eligible methods after auditing that the array is neither unsafely written nor exposed.

### Why inspect suppressed exceptions?

**Model answer:** In try-with-resources, a close failure is suppressed when the body already failed. Ignoring suppressed exceptions can hide a second resource or durability problem. The primary failure drives control flow, while the suppressed chain remains diagnostic evidence.

## Exercises

1. **Predict:** Evaluate the three calls in the parent/child example and identify compile-time and runtime stages.
2. **Debugging:** Repair equality between a value superclass and a subtype with an extra value field.
3. **Interview Core:** Design a `copy` signature from `List<Integer>` into `List<Number>` and explain every wildcard.
4. **Debugging:** Locate the true unsafe boundary when a `List<String>` retrieval throws after raw code runs.
5. **SDE-2 Follow-up:** Decide whether a plugin interface should be sealed and defend the API-evolution choice.
6. **SDE-2 Follow-up:** Review reflective annotation scanning for retention, bridges, module access, caching, and loader leaks.
7. **Production:** Translate a body failure plus two close failures without losing causal or suppressed evidence.

## Worked solutions

1. `convert(Integer)` selects `Parent.convert(Number)` and runs it because there is no override of that descriptor. `describe()` selects the parent signature and dispatches to `Child`. `Parent.label()` selects the parent static method.
2. Prefer a final value type or composition. If cross-type equality is required, define one shared state contract that both sides implement symmetrically; do not use `instanceof` on only one side and exact-class checks on the other.
3. `<T> void copy(List<? extends T> source, List<? super T> target)`. With T inferred as `Number` or `Integer` as appropriate, source produces values accepted as T and target consumes them. Keep the simpler invariant signature if variance adds no caller value.
4. The raw alias and unchecked write polluted the list. The retrieval's cast merely detects it. Remove the raw path or isolate validation in a typed adapter; do not suppress the whole module.
5. Seal only if the domain owns the finite implementer set. A third-party plugin SPI is normally intentionally open; use versioned capabilities or a non-sealed extension point and keep exhaustive switches inside a closed internal model.
6. Require runtime retention, state inherited-annotation behavior, filter bridge/synthetic members intentionally, use permitted module access, preserve invocation causes, and key caches so disposable class loaders can be released.
7. Let try-with-resources preserve the body failure as primary and both close failures as suppressed in reverse close order. Translate once at the boundary with the entire original throwable as cause; logging only messages is insufficient.

## Final checklist

- I separate overload selection from runtime override dispatch.
- I test behavioral substitutability, not only legal signatures.
- I can defend equality semantics across inheritance and mutable components.
- I design wildcard bounds from producer/consumer operations.
- I trace heap pollution to the unchecked boundary.
- I treat sealed types, default methods, records, and reflection as API-evolution choices.
- I preserve primary, causal, and suppressed exception evidence.
