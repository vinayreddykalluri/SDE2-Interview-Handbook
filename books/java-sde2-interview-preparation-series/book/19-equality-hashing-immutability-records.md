# 19. Equality, Hashing, Immutability, and Records

## Learning objectives

By the end of this chapter, you should be able to:

- distinguish reference identity, logical equality, ordering, and domain identity;
- implement the `equals` and `hashCode` contracts together;
- explain why mutable hash keys and subclass equality are dangerous;
- design deeply immutable value objects with safe publication; and
- use records while understanding their generated members and shallow immutability.

## Why this matters at SDE-2

Equality is infrastructure for collections, caches, deduplication, persistence, tests, and distributed idempotency. A broken contract does not always fail immediately; it can make an entry unreachable in a map or create asymmetric behavior that changes with argument order.

Records reduce value-carrier boilerplate, but they do not choose a domain model for you. An SDE-2 engineer must decide which fields define equality, whether data is deeply immutable, and whether an ORM entity, request DTO, or domain value should be a record at all.

## First-principles model

Every object has identity. Reference `==` asks whether two reference values designate the same object, with null handled as an ordinary reference value. `Object.equals` initially implements identity equality, but classes can override it to define logical value equality.

Hashing compresses equality-relevant state into an integer used to choose candidate buckets. A hash is not a unique identifier. Equal objects must have equal hash codes; unequal objects may collide. Hash-based collections use both hash and equality.

Immutability means an object's observable state cannot change after construction. It requires control of the entire reachable representation, not only final top-level fields. A record is a transparent nominal product of its components with generated accessors and value-oriented members. Its component fields are final, but referenced objects may be mutable.

> **Specification boundary:** Java specifies the `Object.equals` and `hashCode` contracts and record member semantics. It does not specify a `HashMap` bucket layout here, a stable hash code across JVM runs, or that `Object.hashCode` is a memory address.

## Core terminology

- **Identity equality:** same runtime object, tested with reference `==`.
- **Logical equality:** class-defined equivalence, tested by `equals`.
- **Domain identity:** business identity, such as an immutable account identifier.
- **Value object:** object defined by its values rather than lifecycle identity.
- **Hash collision:** unequal values have the same hash code.
- **Consistency:** repeated equality or hash calls return compatible results while relevant state is unchanged.
- **Deep immutability:** no observable reachable mutable state can change through any alias.
- **Defensive copy:** independent representation used to prevent outside mutation.
- **Record component:** declared state item that generates a field, accessor, and participation in generated members.
- **Canonical constructor:** record constructor whose parameters correspond to all components.

## Detailed mechanics

### The equals contract

For non-null references, a correct equivalence relation is:

- reflexive: `x.equals(x)` is true;
- symmetric: `x.equals(y)` equals `y.equals(x)`;
- transitive: if x equals y and y equals z, x equals z;
- consistent while equality-relevant information is unchanged; and
- false for `x.equals(null)`.

An implementation normally checks identity, compatible type, and relevant fields:

```java
@Override
public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof Coordinate that)) return false;
    return x == that.x && y == that.y;
}
```

`getClass()` versus `instanceof` is a design choice. Exact-class equality is easier to keep symmetric in extensible hierarchies but means a base and subclass never compare equal. `instanceof` can support equality across implementations only if every participant shares the same semantics. Making value classes final or records avoids many subclass traps.

### The hashCode contract

If `x.equals(y)`, then `x.hashCode() == y.hashCode()` must hold. The reverse is not required. The result must be consistent while equality state is unchanged. Overriding one of `equals` and `hashCode` without the other is almost always a defect.

```java
@Override
public int hashCode() {
    int result = Integer.hashCode(x);
    result = 31 * result + Integer.hashCode(y);
    return result;
}
```

`Objects.hash(x, y)` is concise but uses varargs and boxing, which can matter on a hot path. Hand-written accumulation or generated code avoids that allocation. Quality matters because clustered hashes degrade hash-table performance, but correctness still relies on equality.

Never base logical hash solely on a mutable field if objects can be keys. If a key changes after insertion, lookup uses its new hash or equality state while the entry remains located according to its old state.

### Equality across common types

Arrays retain identity `equals`; use `Arrays.equals` or `deepEquals`. `BigDecimal.equals` considers value and scale, so `1.0` is not equal to `1.00`; `compareTo` considers them numerically equal. This makes `BigDecimal` behavior differ between hash-based and sorted collections if the chosen semantics are not normalized.

Enums use identity safely because each declared constant is a canonical instance in its defining class loader context, and `Enum.equals` is final. Collections define content equality, usually including iteration order for lists and membership for sets.

Entity equality is difficult when database-generated IDs begin as null and proxies introduce subclasses. Do not mechanically include every mutable field. Choose a stable business key, controlled persistent identity policy, or explicit reference identity based on lifecycle requirements.

### Building immutable classes

A typical immutable class is final, has private final fields, validates fully during construction, exposes no mutators, and copies mutable inputs and outputs. Final-field semantics aid safe publication when construction is correct, but final references alone do not freeze mutable referents.

```java
public final class Tags {
    private final java.util.List<String> values;

    public Tags(java.util.Collection<String> values) {
        this.values = java.util.List.copyOf(values);
    }

    public java.util.List<String> values() {
        return values;
    }
}
```

`List.copyOf` rejects nulls and returns an unmodifiable snapshot when needed. If elements are mutable, copy or transform them too for deep immutability. Unmodifiable wrappers alone are views: another alias may still mutate the backing collection.

Avoid letting `this` escape during construction. Safe final-field visibility assumes the object is not observed before constructor completion.

### Records

Records became permanent in Java 16 and are available in Java 17 and 21. A declaration such as:

```java
record Coordinate(int x, int y) {}
```

implicitly defines a final class extending `java.lang.Record`, private final component fields, public component accessors `x()` and `y()`, a canonical constructor, and generated `equals`, `hashCode`, and `toString`. It can implement interfaces and declare static or instance methods, static fields, and additional constructors, but cannot declare extra instance fields or extend another class.

Generated record equality uses the same record class and component values. Its exact hash combination should not be treated as a stable serialization or partitioning algorithm. Record `toString` is convenient for diagnostics but can expose sensitive components if logged.

A compact canonical constructor validates or normalizes components. Assignments to component fields occur implicitly after its body using the possibly reassigned parameter values.

```java
record EmailAddress(String value) {
    EmailAddress {
        value = java.util.Objects.requireNonNull(value).strip();
        if (!value.contains("@")) {
            throw new IllegalArgumentException("invalid email");
        }
    }
}
```

Explicit canonical constructors cannot reduce accessibility below the record's accessibility. Records are not automatically deeply immutable, and their accessors expose component references directly.

## Worked Java example

This record normalizes permission spelling and takes an immutable snapshot, producing stable equality and hashing.

```java
import java.util.Set;
import java.util.TreeSet;

public record PermissionSet(String principalId, Set<String> permissions) {
    public PermissionSet {
        if (principalId == null || principalId.isBlank()) {
            throw new IllegalArgumentException("invalid principalId");
        }
        if (permissions == null) {
            throw new IllegalArgumentException("permissions is null");
        }
        TreeSet<String> normalized = new TreeSet<>();
        for (String permission : permissions) {
            if (permission == null || permission.isBlank()) {
                throw new IllegalArgumentException("invalid permission");
            }
            normalized.add(permission.strip().toLowerCase(java.util.Locale.ROOT));
        }
        permissions = Set.copyOf(normalized);
    }

    public boolean allows(String permission) {
        return permissions.contains(permission.toLowerCase(java.util.Locale.ROOT));
    }

    public static void main(String[] args) {
        PermissionSet a = new PermissionSet("u1", Set.of("READ", "write"));
        PermissionSet b = new PermissionSet("u1", Set.of("write", "read"));
        System.out.println(a.equals(b)); // true
        System.out.println(a.allows("READ")); // true
    }
}
```

Because set equality is order-independent and the canonical constructor normalizes content, different input order and case lead to the same value. Whether permission identifiers should be case-insensitive is a domain decision, not a universal rule.

## Execution or memory walkthrough

Construction receives copied references to the principal string and caller set. It validates the identifier, creates a new `TreeSet`, normalizes each permission, and then snapshots that set using `Set.copyOf`. Reassigning the constructor parameter changes the value that the compiler assigns to the final component field.

The caller's original set is not retained. The record is shallowly immutable by structure, and this particular representation is deeply immutable because strings are immutable and the set cannot be changed. Generated `equals` compares the same record type and then corresponding component values. Both instances contain equal strings and equal sets, so their hash codes must also match.

`allows` performs normalization without mutating the record. Passing null currently throws `NullPointerException`; a public API should document that or validate with a clearer message.

## Complexity and performance

`equals` is proportional to compared state until a difference is found. Hash computation is similarly proportional unless a safely cached hash is used. Hash-table lookup is expected O(1) with well-distributed hashes and controlled load, but collisions and adversarial inputs can change costs.

Immutable values enable sharing and memoization but defensive copies cost O(n) time and space. `PermissionSet` normalization is O(n log n) because of `TreeSet`; if sorted construction is unnecessary, a hash set can offer expected O(n). The final `Set.copyOf` may add another pass.

> **HotSpot note:** Generated record methods are ordinary methods that the JVM may inline. Object allocation or field reads may be optimized, but records are still reference objects and do not imply stack allocation or flattened storage.

## Edge cases and common mistakes

- `==` on references tests identity, not logical value.
- Equal objects must share a hash, but equal hashes do not imply equal objects.
- Mutable map keys can become unreachable after insertion.
- `getClass` and `instanceof` strategies can break symmetry when mixed across a hierarchy.
- Including an array field with `Objects.equals` compares array identity; use the matching `Arrays` helper.
- `BigDecimal.equals` includes scale while `compareTo` does not.
- Unmodifiable collection wrappers do not copy their backing collections.
- Final fields do not make mutable elements immutable.
- Records can contain arrays, lists, or date objects that mutate.
- A compact record constructor can reassign parameters for normalization but should not leak partially constructed state.
- Generated `toString` can leak credentials or personal data.
- Hash codes are not stable persistence keys, signatures, or security hashes.

## Production engineering notes

Write equality from domain identity, not from whatever fields happen to exist today. Value objects usually include all normalized value components. Entities need a lifecycle-aware identity policy that behaves before and after persistence and with framework proxies.

If a type is used as a cache or map key, make equality-relevant state immutable. Add contract tests for reflexivity, symmetry, transitivity, equal hashes, null, and collection behavior. Property-based tests can explore combinations better than a few examples.

Use records for transparent carriers when exposing components matches the API. Prefer a conventional class when representation must stay hidden, construction requires staged mutable machinery, or framework constraints conflict. Redact sensitive values explicitly rather than trusting generated output.

## Interview questions and model answers

**What is the relationship between equals and hashCode?**

If two objects are equal, their hash codes must be equal. Unequal objects may share a hash. Both results must stay consistent while equality state is unchanged. Therefore a class that overrides logical equality must provide a compatible hash implementation.

**Why is a mutable HashMap key dangerous?**

The map places it according to its hash at insertion. If equality-relevant state changes, lookup computes a different hash or comparison and may not find the existing entry. Immutability is the simplest key policy.

**Are records immutable?**

Their component fields are final and the record class is final, so component references cannot be reassigned after construction. The objects referenced by components can still mutate. Defensive copies are required for deep immutability.

**Should entity equality include every field?**

Usually not. Mutable fields make hashing unstable and two lifecycle instances may represent the same entity. Choose a stable business identity or a carefully specified persistent-identity strategy that handles transient instances and proxies.

**Can a record extend a base class?**

No. Every record implicitly extends `java.lang.Record` and is final. It may implement interfaces and define behavior.

## Exercises

1. Implement a final `Money` value with currency and minor units, including contract tests.
2. Demonstrate a map lookup failure after mutating a key, then repair the key design.
3. Compare `BigDecimal("1.0")` and `BigDecimal("1.00")` in `HashSet` and `TreeSet`; explain the result.
4. Make a record with a `byte[]` component deeply immutable, including accessor behavior.
5. Construct a base/subclass equality implementation that breaks symmetry, then redesign it.
6. Decide whether a JPA entity, API DTO, and domain identifier should each be a record, with reasons.

## Chapter summary

Identity and equality answer different questions. Logical equality must be an equivalence relation, and equal values must have equal hashes. Mutable equality state is unsafe for hash keys. Immutability requires control of reachable mutable data and careful publication. Records generate transparent value-oriented structure but remain shallowly immutable reference objects; domain normalization, validation, secrecy, and equality meaning are still design responsibilities.

## Revision checklist

- [ ] I can state every equals contract property.
- [ ] I implement equals and hashCode together from the same state.
- [ ] I do not treat hashes as unique or stable identifiers.
- [ ] I avoid mutable equality state in collection keys.
- [ ] I can explain exact-class versus cross-class equality trade-offs.
- [ ] I distinguish final fields, unmodifiable views, and deep immutability.
- [ ] I know what members records generate and what they do not guarantee.
- [ ] I validate, normalize, copy, and redact record components as required.
