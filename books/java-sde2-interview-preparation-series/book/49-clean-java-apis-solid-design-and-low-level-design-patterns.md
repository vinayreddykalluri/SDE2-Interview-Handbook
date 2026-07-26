# 49. Clean Java APIs, SOLID Design, and Low-Level Design Patterns

## Learning objectives

By the end of this chapter, you should be able to:

- derive classes and interfaces from domain invariants, use cases, and ownership;
- design small Java APIs with explicit null, mutation, failure, ordering, blocking, and concurrency contracts;
- apply SOLID principles as trade-off questions rather than slogans;
- recognize and implement Strategy, Factory, Adapter, Decorator, Builder, State, Command, and Observer where justified;
- evaluate substitutability, cohesion, coupling, and extension cost; and
- communicate a low-level design from requirements through objects, flows, edge cases, and tests.

## Why this matters at SDE-2

SDE-2 design interviews test whether you can turn ambiguous behavior into a maintainable object model. Production code tests the same skill over years: callers depend on names, defaults, failures, side effects, and timing as much as on types. A clean implementation behind an unclear API still spreads defects.

Good design is not the maximum number of interfaces or patterns. It places volatile decisions behind narrow boundaries, keeps invariants close to state, and makes the common call correct by construction. It also acknowledges operational behavior: an innocent-looking getter that performs network I/O is not clean simply because its name is short.

## First-principles model

Start with behavior and invariants:

```text
actors and use cases
    -> commands and queries
    -> state and invariants
    -> ownership and lifecycle
    -> failure and concurrency policy
    -> interfaces at real variation boundaries
```

An object is useful when it combines state with operations that preserve that state's valid region. If callers must remember five checks before every mutation, the abstraction has leaked its invariant.

For every public operation, define:

```text
preconditions -> state transition or result -> postconditions
              -> failure mode -> side effects -> ownership
```

Prefer a small number of domain concepts with high cohesion. Coupling is unavoidable; the goal is to couple stable policy to explicit abstractions and keep unstable infrastructure at replaceable edges.

> **Specification boundary:** Java types express some constraints, such as access, generic relationships, and checked exceptions, but they do not express nullness, units, thread safety, blocking, ownership, idempotency, or most value invariants. These are application API contracts and must be encoded through types, validation, tests, and documentation.

## Core terminology

- **Invariant:** Condition that must hold for every externally observable valid object state.
- **Precondition:** Requirement a caller must satisfy before an operation.
- **Postcondition:** Guarantee after successful completion.
- **Cohesion:** Degree to which a component's responsibilities belong together.
- **Coupling:** Dependencies between components or knowledge of one another's details.
- **Encapsulation:** Protecting a model by controlling access to representation and transitions.
- **Composition:** Building behavior by containing and delegating to collaborators.
- **Policy:** Business decision or rule.
- **Mechanism:** Technical means used to carry out policy.
- **Dependency inversion:** High-level policy depends on an abstraction rather than a low-level detail.
- **Behavioral subtyping:** A subtype can replace its supertype without violating caller expectations.
- **Pattern:** Named reusable structure for a recurring design pressure, not a mandatory template.
- **Seam:** Boundary where behavior can vary or be tested independently.

## Detailed mechanics

### Clean API contracts

Names should communicate units and effects: `timeout(Duration)` is stronger than `timeout(int)`, and `loadFromDatabase` is more honest than `get` when I/O occurs. Prefer domain types such as `OrderId`, `Money`, or `PageToken` when primitives would be confused or invalid states are common.

Validate at construction and mutation boundaries. Fail fast with the most specific useful exception when a programming precondition is violated. Domain rejection may deserve a domain result or exception rather than `IllegalArgumentException`. Do not partially mutate an object and then validate.

Return empty collections instead of null for zero results. Use `Optional` for a maybe-one return when absence is normal, not for every field or parameter. State whether collections preserve order, permit duplicates/nulls, are snapshots, or are live views. `List.copyOf` protects membership but does not make mutable elements immutable.

Keep command-query separation as a useful default: queries report state without changing domain state; commands perform transitions and return the minimum needed result. It is acceptable for a command to return an identifier or outcome. Avoid setters that bypass rules. `order.cancel(reason, now)` communicates and enforces more than `setStatus(CANCELED)`.

Checked versus unchecked exceptions is an API decision. Checked exceptions can force handling of recoverable boundary conditions but propagate coupling. Unchecked exceptions suit violated programming contracts and failures callers cannot meaningfully recover from locally. Never discard context; wrap with causal chains and safe domain metadata.

### SOLID as five review questions

**Single Responsibility Principle:** Does this unit have one cohesive reason to change? "One method" is not the rule. A pricing component can contain several methods if all preserve pricing policy. A class mixing pricing, SQL, JSON, retries, and email has unrelated change drivers.

**Open/Closed Principle:** Can a likely new variant be added behind a deliberate seam without editing a fragile conditional everywhere? Do not create extension points for imagined futures. A sealed hierarchy can intentionally close variants; openness is not universally desirable.

**Liskov Substitution Principle:** Can every implementation honor the abstraction's preconditions, postconditions, invariants, and failure semantics? A subtype that rejects inputs the base accepts, returns weaker results, or changes a nonblocking call into unbounded blocking is not substitutable even if signatures match.

**Interface Segregation Principle:** Do clients depend only on capabilities they use? Split read, write, administration, and lifecycle interfaces when those capabilities differ. Avoid one giant service interface, but also avoid dozens of one-method interfaces with no independent variation.

**Dependency Inversion Principle:** Does high-level policy own the abstraction it needs, while adapters translate database, HTTP, clock, or vendor details? Depending on interfaces is not sufficient if the interface merely mirrors one vendor SDK.

### Composition and inheritance

Prefer composition for optional or combinable behavior. Inheritance exposes protected state, constructor ordering, override interactions, and a strong "is-a" behavioral promise. Use it for genuine substitutable families with stable shared semantics. Mark classes or methods final when extension would bypass invariants.

Records suit transparent immutable data aggregates, not automatically rich entities. Their components are shallowly final; contained collections and objects may mutate. Sealed types model a known set of variants and enable exhaustive reasoning. Interfaces model capabilities across unrelated implementations.

> **HotSpot note:** Whether a composed call, interface call, or small value object allocates at runtime depends on profiling, inlining, and escape analysis. Do not distort an API solely to predict HotSpot optimization. Measure a confirmed hot path on the target JDK.

### Patterns and their design pressure

| Pattern | Pressure it addresses | Common misuse |
|---|---|---|
| Strategy | Select one interchangeable policy | interface for behavior that never varies |
| Factory | Centralize valid creation and implementation selection | hiding arbitrary service location |
| Adapter | Translate an external or legacy contract | leaking vendor types through the adapter |
| Decorator | Add composable behavior around one contract | changing core semantics unexpectedly |
| Builder | Construct many optional or staged parameters | mutable builder reused across threads |
| State | Behavior changes with explicit lifecycle state | replacing a small clear switch with many classes |
| Command | Represent an action for queueing, logging, undo, or dispatch | turning every method call into an object |
| Observer | Notify multiple dependents | unclear lifetime, ordering, and failure isolation |
| Template Method | Share an algorithm skeleton through inheritance | fragile override hooks |

Patterns can combine. A factory creates a strategy; decorators add metrics or retries; an adapter implements the strategy using a vendor. Each layer must preserve the underlying contract. Retry, for example, is safe only for idempotent operations or when an idempotency mechanism exists.

### Low-level design interview method

Clarify scale, actors, operations, consistency, concurrency, and out-of-scope requirements. Identify nouns but validate them through behavior; not every noun needs a class. State invariants and lifecycle transitions. Sketch public APIs and one critical sequence. Then examine extension points, storage boundaries, failure, thread safety, and test seams.

Discuss trade-offs explicitly. A map-based in-memory design may be correct for one-process scope but not durable or distributed. Do not smuggle distributed guarantees into a class diagram. Keep the initial model runnable and explain how boundaries evolve.

## Worked Java example

This pricing design keeps money invariants in a value object and discount variation behind a Strategy:

```java
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

record Money(BigDecimal amount, String currency) {
    public Money {
        if (amount == null || currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("money fields are required");
        }
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
    }

    Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    Money multiply(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("negative quantity");
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("currency mismatch");
        }
    }
}

record LineItem(String sku, int quantity, Money unitPrice) {
    public LineItem {
        if (sku == null || sku.isBlank() || quantity <= 0 || unitPrice == null) {
            throw new IllegalArgumentException("invalid line item");
        }
    }
}

record Order(List<LineItem> items, String customerTier) {
    public Order {
        items = List.copyOf(items);
        if (items.isEmpty() || customerTier == null) {
            throw new IllegalArgumentException("invalid order");
        }
    }
}
```

The policy contract and pricing service build on those value objects:

```java

interface DiscountPolicy {
    Money discountFor(Order order, Money subtotal);
}

final class TierDiscountPolicy implements DiscountPolicy {
    @Override
    public Money discountFor(Order order, Money subtotal) {
        BigDecimal rate = switch (order.customerTier()) {
            case "GOLD" -> new BigDecimal("0.10");
            case "SILVER" -> new BigDecimal("0.05");
            default -> BigDecimal.ZERO;
        };
        BigDecimal amount = subtotal.amount().multiply(rate)
                .setScale(2, RoundingMode.HALF_EVEN);
        return new Money(amount, subtotal.currency());
    }
}

record Quote(Money subtotal, Money discount, Money total) {}

final class PricingService {
    private final DiscountPolicy discountPolicy;

    PricingService(DiscountPolicy discountPolicy) {
        this.discountPolicy = java.util.Objects.requireNonNull(discountPolicy);
    }

    Quote quote(Order order) {
        String currency = order.items().get(0).unitPrice().currency();
        Money subtotal = new Money(BigDecimal.ZERO.setScale(2), currency);
        for (LineItem item : order.items()) {
            subtotal = subtotal.add(item.unitPrice().multiply(item.quantity()));
        }
        Money discount = discountPolicy.discountFor(order, subtotal);
        if (!discount.currency().equals(currency)
                || discount.amount().compareTo(subtotal.amount()) > 0) {
            throw new IllegalStateException("discount policy violated its contract");
        }
        return new Quote(subtotal, discount, subtotal.subtract(discount));
    }
}
```

The service owns orchestration; the policy owns discount variation; `Money` owns currency and nonnegative-value rules. A factory could select a policy from configuration, and a decorator could measure policy latency without changing `PricingService`.

## Execution or memory walkthrough

For two USD items priced `10.00 x 2` and `5.00 x 1`, `PricingService` starts with USD zero, produces subtotals `20.00` and then `25.00`, and asks the policy for a discount. A GOLD order yields `2.50`, so the quote total is `22.50`.

Construction copies the item list, preventing later membership changes through the caller's list. `LineItem` and `Money` are immutable records, so this particular object graph is stable. If a component were a mutable object, `List.copyOf` alone would not deep-copy it.

The service checks the strategy's postcondition: same currency and discount no greater than subtotal. That defensive boundary makes a broken implementation fail near the cause rather than creating negative totals downstream. It does not need to know how the strategy chose the discount.

## Complexity and performance

For `n` line items, quote computation is `O(n)` time and `O(1)` additional domain storage beyond immutable result objects. `BigDecimal` arithmetic cost depends on precision and magnitude, so the simple `O(n)` model treats money operation size as bounded.

Abstraction cost should be considered at the design level first:

| Choice | Benefit | Cost to evaluate |
|---|---|---|
| immutable values | safe sharing, simple invariants | allocation and copying |
| interface seam | substitution and testing | more concepts and contract surface |
| defensive copy | ownership isolation | `O(n)` time and references |
| decorator | composable cross-cutting behavior | call depth, ordering interactions |
| builder | readable optional construction | extra mutable lifecycle |

Avoid making every field a wrapper or every class an interface in the name of cleanliness. Complexity of understanding is a performance constraint on the engineering organization. Measure runtime concerns only after profiling identifies a hot path.

## Edge cases and common mistakes

- Starting from patterns instead of use cases and invariants.
- Creating interfaces for every class with no independent implementation or test seam.
- Using setters that permit invalid intermediate states.
- Hiding blocking I/O or mutation behind query-like names.
- Returning internal mutable collections or mutable builder state.
- Treating record components as deeply immutable.
- Applying LSP only to method signatures and ignoring failure, timing, and side effects.
- Building a base class with protected fields and many override hooks.
- Adding retry decorators to non-idempotent operations.
- Letting an adapter expose vendor DTOs, exceptions, or configuration throughout the domain.
- Using a service locator or static singleton and calling it dependency inversion.
- Splitting cohesive behavior across so many classes that one change requires navigating a graph.
- Using `double` for money or omitting currency and rounding contracts.
- Optimizing dispatch based on guessed JVM behavior.

## Production engineering notes

Treat public APIs as versioned products. Remove ambiguity about nulls, blocking, timeout, idempotency, thread safety, collection ownership, ordering, and exceptions. Favor additive evolution and migration adapters over silently changing semantics.

Keep domain policy free of framework annotations and vendor DTOs where practical. Translate at controllers, messaging adapters, and repositories. This makes business behavior testable and limits dependency upgrades to boundary code. Do not create duplicate models mechanically when one stable representation genuinely serves both layers.

Lifecycle is part of design. Observers need registration handles; executors and clients need closure; caches need bounds; decorators need ordering; factories need validated configuration. Expose health and metrics through separate operational interfaces rather than mixing administrative mutation with core use cases.

Run architecture checks for forbidden dependency directions, but use reviews to evaluate semantics. Static package rules cannot prove substitutability or a useful abstraction. Record significant design decisions, alternatives, and reversal cost.

## Interview questions and model answers

**What makes an API clean?**

It has cohesive operations, explicit invariants and ownership, unsurprising names, minimal invalid states, and documented failure, blocking, concurrency, null, and ordering behavior. It is easy to use correctly and hard to misuse.

**Explain dependency inversion with an example.**

Pricing policy depends on a small exchange-rate abstraction owned by the domain, not a vendor HTTP client. An adapter implements that abstraction using the vendor. Policy tests use a deterministic implementation, and vendor changes stay at the edge.

**How do you decide between composition and inheritance?**

Use inheritance only for a true behavioral subtype with stable shared semantics. Use composition for optional, replaceable, or combinable behavior because it exposes fewer override interactions and preserves encapsulation.

**Is SOLID always good?**

The principles are review heuristics with trade-offs. Premature interfaces and tiny classes can increase cognitive load. Apply them at observed change boundaries and preserve cohesion.

**How do Strategy and Decorator differ?**

Strategy selects the core policy implementation. Decorator wraps the same contract to add behavior such as metrics, caching, or authorization while delegating. A decorator must preserve the wrapped contract.

**How would you approach a low-level design interview?**

Clarify use cases and scale, state invariants, model lifecycle and ownership, sketch the public API and critical sequence, then cover extension, concurrency, failure, persistence boundaries, complexity, and tests.

## Exercises

1. Replace a `setStatus` order API with valid transition methods and specify preconditions and failures.
2. Design a retry decorator contract that is safe only for idempotent commands. Show how callers communicate idempotency.
3. Refactor a report class that queries SQL, calculates totals, formats JSON, and emails output into cohesive boundaries.
4. Add a promotion-composition policy to the worked example. Decide whether discounts stack, cap, or select the best.
5. Model a vending machine with either a State pattern or an enum switch. Compare extension and readability.
6. Review an interface whose implementations disagree on null, timeout, and exception behavior. Write a substitutable contract.

## Chapter summary

Clean Java design starts with use cases, invariants, ownership, and observable contracts. SOLID principles expose change, substitutability, capability, and dependency questions; they are not a class-count target. Patterns name structures that address real variation or lifecycle pressure, and composition is usually the safest default. A strong low-level design remains runnable, states trade-offs, isolates infrastructure, and makes invalid state and accidental side effects difficult.

## Revision checklist

- [ ] I derive types and methods from behavior and invariants rather than nouns alone.
- [ ] I document null, mutation, order, failure, blocking, idempotency, lifecycle, and thread safety.
- [ ] I can apply all five SOLID principles as concrete review questions.
- [ ] I distinguish behavioral subtyping from signature compatibility.
- [ ] I prefer composition unless a genuine stable is-a relationship exists.
- [ ] I can select and critique common low-level design patterns.
- [ ] I keep policy independent from vendor and framework details at deliberate seams.
- [ ] I can present requirements, APIs, sequence, edge cases, complexity, and tests in a design interview.
