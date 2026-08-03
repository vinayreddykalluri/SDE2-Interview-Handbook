# 57. The Module System, JPMS, and Strong Encapsulation

## Learning objectives

By the end of this chapter, you should be able to:

- explain what a module declares and why `public` stopped meaning "accessible to everyone";
- distinguish the module path from the class path, and named, automatic, and unnamed modules;
- read and write a `module-info.java` including `requires transitive`, `exports ... to`, and `opens`;
- diagnose `InaccessibleObjectException` and split-package errors from the message alone; and
- decide whether modularizing a given service is worth the cost.

## Why this matters at SDE-2

Most Java services are not modularized, and many engineers conclude the module system is therefore irrelevant. It is not, for two reasons.

First, the JDK itself is modular and has been since Java 9. Strong encapsulation of JDK internals became the default in Java 16 and was made final in Java 17, so reflective access into `java.base` now throws rather than warns. When a serialization library, a mocking framework, or an ORM fails with `InaccessibleObjectException` after a JDK upgrade, that is JPMS enforcing a boundary - and someone has to know what `--add-opens` actually does before pasting it into a startup script.

Second, the vocabulary appears in interviews as a proxy for how well you understand accessibility, class loading, and dependency hygiene. "Why does `public` no longer mean accessible?" is a question about encapsulation design, not about syntax.

## First-principles model

Before Java 9, the unit of reuse was the JAR and the unit of accessibility was the package. Neither was enforced meaningfully. Any JAR on the class path could read any `public` type in any other JAR; the class path was a flat, ordered search list with no notion of dependencies; two JARs could both contain `com.example.util` and the loader would silently take whichever came first.

A **module** adds a name, an explicit dependency list, and an explicit export list to a set of packages. Three consequences follow directly:

1. **Accessibility becomes two-dimensional.** A type is accessible only if it is `public` *and* its package is exported by its module *and* the reading module requires that module. `public` became necessary but no longer sufficient.
2. **Dependencies become declared and verified.** The module graph is resolved at startup. A missing module is an error at launch rather than a `NoClassDefFoundError` on the unlucky code path six hours in.
3. **Packages become unique.** A package may belong to only one module in a configuration. Split packages, previously silent, are now a startup failure.

> **Specification boundary:** Java specifies module declarations, the readability and accessibility rules, and resolution at startup. It does not specify versions. `module-info.java` has no version constraint syntax and the module system performs no version selection or conflict resolution - that remains entirely the job of Maven, Gradle, or whatever builds your module path.

## Core terminology

- **Named module:** has a `module-info.class`; declares its own name, requires, and exports.
- **Automatic module:** a plain JAR placed on the module path; gets a name, reads everything, exports everything.
- **Unnamed module:** everything loaded from the class path; reads all modules, exports all its packages.
- **Module path:** `--module-path` / `-p`; entries are resolved as modules.
- **Class path:** `-cp`; entries land in the unnamed module with legacy behavior.
- **requires:** this module reads another.
- **requires transitive:** readers of this module also read that one - implied readability.
- **requires static:** needed at compile time, optional at run time.
- **exports:** package is accessible at compile time and run time.
- **opens:** package is available for deep reflection at run time only.
- **uses / provides:** the `ServiceLoader` declaration pair.
- **Strong encapsulation:** non-exported packages are inaccessible even to reflection.

## Detailed mechanics

### Reading a module declaration

`module-info.java` sits at the source root and compiles to `module-info.class`.

```java
module com.example.orders {
    requires java.sql;                       // I use JDBC types internally
    requires transitive com.example.model;   // my API signatures expose model types
    requires static com.example.codegen;     // compile-time only annotation processor

    exports com.example.orders.api;                       // public API
    exports com.example.orders.spi to com.example.admin;  // qualified: one consumer only

    opens com.example.orders.entity;         // deep reflection for the ORM

    uses com.example.orders.spi.PricingRule;                    // I consume this service
    provides com.example.orders.spi.PricingRule
            with com.example.orders.internal.StandardPricing;   // I supply this implementation
}
```

Everything not listed is inaccessible. `com.example.orders.internal` is not exported, so no other module can reference it - the compiler rejects the import and reflection throws at run time.

### requires transitive and API leakage

`requires transitive` exists for one situation: when your public API's signatures mention types from another module.

```java
// In com.example.orders
public Order lookup(CustomerId id);   // CustomerId comes from com.example.model
```

Any caller of `lookup` must be able to name `CustomerId`. Without `requires transitive com.example.model`, every consumer would have to add its own `requires com.example.model` - and would be baffled as to why. The rule is mechanical: if a type from module M appears in your exported signatures, `requires transitive M`. Otherwise use plain `requires`, which keeps the dependency an implementation detail you can change later.

This makes API leakage visible in a way the class path never did. A module needing ten `requires transitive` entries is telling you its public surface depends on ten other modules.

### exports versus opens

This distinction is the one that actually bites in production.

- `exports` grants **compile-time and run-time access to public types**. It does not grant reflective access to non-public members. `setAccessible(true)` on a private field in an exported-but-not-opened package throws.
- `opens` grants **run-time deep reflection** into all members, including private ones, but grants nothing at compile time.

Frameworks that populate fields reflectively - JPA providers, Jackson, Spring, most mocking libraries - need `opens`, not `exports`. Entity packages are the canonical case: you rarely want application code importing entity internals, but Hibernate must reach private fields.

```java
opens com.example.orders.entity;                        // to everyone
opens com.example.orders.entity to org.hibernate.orm.core;  // qualified, preferred
```

`open module com.example.orders { ... }` opens every package at once. It is the pragmatic escape hatch when migrating a large codebase, and it discards most of the encapsulation benefit, so treat it as a transition state rather than a destination.

### The three kinds of module

Understanding migration requires understanding what happens to code that has no `module-info`.

**Unnamed module** - everything on the class path. It reads every other module, and all its packages are exported. This is why an unmodularized application still runs on a modern JDK: the unnamed module is deliberately permissive. But no named module can `requires` the unnamed module, because it has no name. That asymmetry is the entire difficulty of incremental migration: you must modularize bottom-up, dependencies first.

**Automatic module** - a plain JAR on the *module* path. It gets a name (from `Automatic-Module-Name` in the manifest, or derived from the filename), reads every other module including the unnamed one, and exports all its packages. It is the bridge that lets a named module depend on a not-yet-modularized library.

Deriving a module name from a filename is fragile - the name changes if the artifact is renamed - so a library that has not modularized should at minimum publish `Automatic-Module-Name` in its manifest. That is a one-line manifest addition and it stabilizes the name for every downstream consumer.

**Named module** - has `module-info.class`, enforces its declaration.

### Strong encapsulation of the JDK

The change most engineers actually encounter. JDK internals such as `sun.misc.Unsafe` and much of `java.lang` reflection were accessible on Java 8, warned about in 9 through 15, and denied from 16 onward.

```text
java.lang.reflect.InaccessibleObjectException: Unable to make
field private final java.lang.String java.lang.String.value accessible:
module java.base does not "opens java.lang" to unnamed module @1b6d3586
```

The message is precise and worth reading closely: it names the field, the owning module (`java.base`), the missing directive (`opens java.lang`), and the requesting module (`unnamed`, so the caller is on the class path). The corresponding flag mirrors that structure exactly:

```bash
java --add-opens java.base/java.lang=ALL-UNNAMED -jar app.jar
```

`--add-opens module/package=target` grants deep reflection; `--add-exports` grants only public access. `ALL-UNNAMED` targets everything on the class path.

Two warnings. First, these flags are a compatibility bridge, not a fix - they belong in a build file with a comment naming the library that needs them and the ticket to remove them. Second, they must be present at every launch, so a flag that works locally and is missing from the container entrypoint produces a failure that appears only in deployment.

### Split packages

A package may exist in only one module on a given path. Two JARs both containing `com.example.util` fail at startup rather than silently shadowing:

```text
Error occurred during initialization of boot layer
java.lang.LayerInstantiationException: Package com.example.util in both
module lib.b and module lib.a
```

This surfaces genuine problems - shaded JARs, forked libraries, an old `javax.annotation` split - that the class path had been hiding.

### Services

JPMS makes `ServiceLoader` a first-class, declared relationship. The consumer declares `uses`, the provider declares `provides ... with`, and resolution binds them without either module referencing the other's implementation package.

```java
// consumer module
uses com.example.orders.spi.PricingRule;

// provider module, in a different artifact
provides com.example.orders.spi.PricingRule with com.example.pricing.RegionalPricing;
```

The implementation class need not be in an exported package. This is genuinely better than the `META-INF/services` file it replaces: it is compile-checked, and a typo is a compilation error rather than a silent empty iterator.

### jlink and runtime images

Because dependencies are declared, the module graph can be computed and a custom runtime image containing only the reachable modules can be produced.

```bash
jlink --add-modules com.example.orders --output runtime --strip-debug --no-man-pages
```

For a service using a modest slice of the JDK this can cut a runtime image from a few hundred megabytes to under 60 - a real container-image win. `jlink` requires every module in the graph to be a named module, which in practice is the strongest concrete incentive to modularize.

## Worked Java example

A two-module layout where the boundary is the point.

```java
// ---------- module com.example.model ----------
// src/com.example.model/module-info.java
module com.example.model {
    exports com.example.model.api;
    // com.example.model.internal is NOT exported
}

// src/com.example.model/com/example/model/api/CustomerId.java
package com.example.model.api;

public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("customer id required");
        }
    }
}

// src/com.example.model/com/example/model/internal/IdCodec.java
package com.example.model.internal;

public final class IdCodec {           // public, but unreachable outside the module
    public static String encode(String raw) { return raw.strip().toUpperCase(); }
}
```

```java
// ---------- module com.example.orders ----------
// src/com.example.orders/module-info.java
module com.example.orders {
    requires transitive com.example.model;   // CustomerId appears in my API
    exports com.example.orders.api;
}

// src/com.example.orders/com/example/orders/api/OrderService.java
package com.example.orders.api;

import com.example.model.api.CustomerId;
// import com.example.model.internal.IdCodec;  // compile error: not exported

public final class OrderService {
    public String describe(CustomerId id) {
        return "orders for " + id.value();
    }

    public static void main(String[] args) {
        System.out.println(new OrderService().describe(new CustomerId("c-100")));
        // orders for c-100
    }
}
```

Compile and run both modules:

```bash
javac -d out --module-source-path src $(find src -name '*.java')
java --module-path out --module com.example.orders/com.example.orders.api.OrderService
```

Uncommenting the `IdCodec` import produces a compile error naming the reason:

```text
error: package com.example.model.internal is not visible
  (package com.example.model.internal is declared in module
   com.example.model, which does not export it)
```

`IdCodec` is `public`. It is still unreachable. That is the whole idea in one error message.

## Execution or memory walkthrough

At launch, the module system resolves the graph before any application class loads. It reads the root modules named by `--module` or `--add-modules`, transitively reads their `requires` edges, and fails immediately on a missing module, a duplicate module name, a split package, or a cycle. Resolution produces a **configuration**, which is instantiated as the boot **layer**.

Each module in the layer is assigned to a class loader - the built-in application loader for the module path - and the loader records which packages belong to which module. This is why a split package is detectable at all: the mapping from package to module must be a function.

At link time, a reference from `com.example.orders` to `CustomerId` is checked twice. The compiler verifies that `com.example.orders` reads `com.example.model` and that the package is exported. The JVM verifies the same facts again during resolution, because a module path can differ at run time from the one used at compile time.

An `IdCodec` reference would fail the second check with `IllegalAccessError` even if it somehow passed the first - which is the meaningful difference from the class path, where the check did not exist.

Reflection consults the same tables. `setAccessible(true)` asks whether the target's package is *open* to the caller's module; the answer is a table lookup, and a negative answer throws `InaccessibleObjectException`. No bytecode is rewritten and no proxy is generated; the enforcement is a runtime check in `AccessibleObject`.

## Complexity and performance

Resolution is roughly linear in the number of modules and edges, and it happens once. For a typical service the cost is a few milliseconds against a JVM startup measured in hundreds.

Access checks are performed at resolution or first access and then cached in the constant pool, so steady-state throughput is unaffected. There is no per-call module check.

The measurable wins are startup and image size. A `jlink` image containing only reachable modules loads a smaller class-data-sharing archive and produces a much smaller container image. The measurable cost is build complexity, which is where modularization is usually paid for.

> **HotSpot note:** module boundaries do not create optimization barriers. The JIT inlines across modules exactly as it does within one, because accessibility is resolved before compilation. Modules are a correctness and packaging mechanism, not a performance one.

## Edge cases and common mistakes

- Assuming `public` still means accessible. It requires `public` plus `exports` plus `requires`.
- Using `exports` where a framework needs `opens`, then debugging `InaccessibleObjectException`.
- Using `opens` where `exports` was meant, giving reflective access but no compile-time access.
- Omitting `requires transitive` when an exported signature mentions another module's type, forcing every consumer to add a dependency it cannot explain.
- Expecting `module-info.java` to express versions. It cannot; version selection belongs to the build tool.
- Expecting a named module to `requires` the unnamed module. It cannot - migration must go bottom-up.
- Relying on a filename-derived automatic module name, which changes when the artifact is renamed.
- Split packages from shaded or forked JARs, now a hard startup failure.
- Adding `--add-opens` at compile time only and omitting it from the runtime launch command.
- Putting the same JAR on both the class path and the module path, producing two copies with different identities.
- Treating `open module` as the finished state rather than a migration step.
- Forgetting that `requires static` supplies compile-time visibility only; the type must be absent-safe at run time.
- Assuming `jlink` works with automatic modules. It does not - every module in the graph must be named.

## Production engineering notes

Be honest about whether modularization pays. For a Spring Boot service deployed as a fat JAR, the answer is usually no: the framework does extensive reflection, many dependencies are unmodularized, and you gain little over a well-structured build with enforced package rules. For a published library, a CLI, or anything shipping a runtime image, the answer is often yes.

If you do not modularize, still add `Automatic-Module-Name` to any JAR you publish. It costs one manifest line and spares every downstream consumer a name that changes when you rename a file.

Record every `--add-opens` and `--add-exports` in the build with a comment naming the library that requires it and the condition for removing it. These flags accumulate silently and become a list nobody dares touch. Re-test them on each JDK and library upgrade - most eventually become unnecessary.

Put the flags where every launch sees them. `MANIFEST.MF` supports `Add-Opens` for executable JARs, and the `JDK_JAVA_OPTIONS` environment variable is honored by the `java` launcher. A flag configured only in a local IDE run configuration is a production incident waiting for a deploy.

When a JDK upgrade breaks reflective access, prefer upgrading the offending library over adding a flag. `InaccessibleObjectException` is usually a library reaching into internals that have a supported alternative, and the maintainers have generally already fixed it.

## Interview questions and model answers

**Why is a `public` class not always accessible in Java 9 and later?**

Accessibility now has two dimensions. The type must be `public`, its package must be exported by its module, and the calling module must read that module. A `public` class in a non-exported package is visible to its own module only, which is what makes internal APIs genuinely internal.

**What is the difference between `exports` and `opens`?**

`exports` grants compile-time and run-time access to public members. `opens` grants run-time deep reflection into all members, including private ones, and grants nothing at compile time. Frameworks that populate private fields need `opens`; ordinary API consumers need `exports`.

**When do you need `requires transitive`?**

When a type from the required module appears in your own exported signatures. Consumers must be able to name those types, and `requires transitive` gives them implied readability instead of forcing each one to declare a dependency it does not obviously use.

**What is an automatic module?**

A plain JAR on the module path. It takes its name from `Automatic-Module-Name` or the filename, reads every other module including the unnamed one, and exports all its packages. It exists so a named module can depend on a library that has not modularized yet.

**How do you fix `InaccessibleObjectException`?**

Read the message - it names the module, the package, and the caller. Then either add `opens` to your own module declaration if you own the code, or pass `--add-opens module/package=ALL-UNNAMED` at launch. Preferably upgrade the library instead, since the exception usually means it is reaching into internals that now have a supported replacement.

**Why does the module system reject split packages?**

Each package must map to exactly one module so that accessibility and class loading are well defined. On the class path, duplicate packages silently resolved by search order, which produced defects that depended on artifact ordering. JPMS makes it a startup error.

## Exercises

1. Build the two-module example, then uncomment the `IdCodec` import and read the compiler error carefully.
2. Add a third module that requires `com.example.orders` and uses `CustomerId`. Remove `transitive` and explain the resulting error.
3. Write a class that calls `setAccessible(true)` on a private field of a non-opened package, observe the exception, and fix it with `opens` and again with `--add-opens`.
4. Create two JARs sharing a package name, put both on the module path, and read the `LayerInstantiationException`.
5. Convert a small library to a named module and run `jlink`, comparing the runtime image size to a full JDK.
6. Take a JAR with no `Automatic-Module-Name`, place it on the module path, and print its derived name with `java --list-modules`. Rename the file and print again.
7. Declare a `uses`/`provides` service pair across two modules and confirm `ServiceLoader` finds the implementation without the consumer importing it.

## Chapter summary

The module system adds a name, an explicit dependency list, and an explicit export list to a group of packages, which changes accessibility from a one-dimensional `public` check into a three-part test: public, exported, and required. `requires transitive` propagates readability for types that appear in your API; `exports` grants ordinary access while `opens` grants deep reflection, and confusing the two is the most common practical failure. Unnamed and automatic modules keep legacy code working and make bottom-up migration possible, at the cost of an asymmetry - named modules can never require the unnamed one. Strong encapsulation of JDK internals since Java 16 is where most engineers first meet JPMS, through `InaccessibleObjectException` and the `--add-opens` flag that answers it. The module system verifies structure, not versions, and modularizing is a judgment call: often unnecessary for a fat-JAR service, often worthwhile for a published library or a `jlink` runtime image.

## Revision checklist

- [ ] I can state the three conditions required for a type to be accessible.
- [ ] I can explain `exports` versus `opens` and which one an ORM needs.
- [ ] I know when `requires transitive` is mandatory and why.
- [ ] I can describe named, automatic, and unnamed modules and how each behaves.
- [ ] I know why a named module cannot require the unnamed module.
- [ ] I can read an `InaccessibleObjectException` and derive the exact `--add-opens` flag.
- [ ] I can explain why split packages are now a startup failure.
- [ ] I know `module-info.java` carries no version information.
- [ ] I can declare a `uses`/`provides` service pair.
- [ ] I can argue both sides of whether a given service should be modularized.
