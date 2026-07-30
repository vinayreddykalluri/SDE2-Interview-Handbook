# Constructor Injection and Dependency Resolution

Dependency injection is easy when exactly one bean matches. Interviews become interesting when dependencies are optional, repeated, ordered, lazy, or circular.

## Constructor injection as the default

```java
@Service
final class CheckoutService {
    private final PriceCalculator calculator;
    private final PaymentGateway gateway;

    CheckoutService(PriceCalculator calculator, PaymentGateway gateway) {
        this.calculator = Objects.requireNonNull(calculator);
        this.gateway = Objects.requireNonNull(gateway);
    }
}
```

For a single constructor, `@Autowired` is unnecessary. Constructor injection makes required dependencies visible, supports final fields, and makes plain unit construction natural.

Setter injection can express a genuinely optional or reconfigurable collaborator. Field injection hides construction requirements, prevents final fields, and makes non-container tests awkward; avoid it in application code.

## Resolution by type, then disambiguation

Given two implementations:

```java
@Component
final class StripeGateway implements PaymentGateway { }

@Component
final class OfflineGateway implements PaymentGateway { }
```

an unqualified `PaymentGateway` injection is ambiguous. Options:

### One real default with `@Primary`

```java
@Primary
@Component
final class StripeGateway implements PaymentGateway { }
```

Use `@Primary` only if one implementation is the application-wide default.

### Semantic selection with `@Qualifier`

```java
CheckoutService(@Qualifier("offlineGateway") PaymentGateway gateway) {
    this.gateway = gateway;
}
```

For durable semantics, define a custom qualifier instead of repeating strings:

```java
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
@interface OfflinePayments { }
```

### Inject all strategies

```java
FraudRouter(List<FraudRule> rules) {
    this.rules = List.copyOf(rules);
}
```

Spring supplies all matching beans. Use `@Order` or `Ordered` only when order is part of the contract; otherwise do not let registration order become behavior.

## Optional and deferred dependencies

```java
final class ExportService {
    private final ObjectProvider<AuditSink> auditSink;

    ExportService(ObjectProvider<AuditSink> auditSink) {
        this.auditSink = auditSink;
    }

    void export() {
        auditSink.ifAvailable(sink -> sink.record("export"));
    }
}
```

`ObjectProvider<T>` supports optional, lazy, or repeated resolution while keeping the dependency explicit. `Optional<T>` can represent simple absence. Do not make a required business collaborator optional merely to keep startup green.

## Generic types participate in matching

```java
interface Handler<T> {
    void handle(T command);
}

final class CreateOrderHandler implements Handler<CreateOrder> { }
```

Spring can use generic type information when resolving candidates. This makes typed strategy registries possible, but keep routing explicit and test ambiguous cases.

## Parameter names are not your primary contract

Spring may use an injection-point name as a fallback among candidates when parameter metadata is available. That is fragile as the main design. Prefer type, semantic qualifiers, or a deliberate collection/map.

## Circular dependencies are a design signal

```text
OrderService -> PricingService -> OrderService
```

Constructor injection makes the cycle fail clearly. Field/setter injection may permit some cycles through early references in some configurations, but the graph remains hard to construct, test, and reason about. Typical corrections:

- extract the shared policy into a third service;
- publish a domain/application event when coupling is temporal;
- move orchestration into a higher-level coordinator;
- merge classes only if they are truly one responsibility.

`@Lazy` can defer one side and break construction, but it often conceals a design issue. Use it only after naming why the cycle is legitimate.

## Resolution decision tree

```text
one injection point of type T
        |
        v
find candidate beans assignable to T
        |
        +-- none -> optional/provider? otherwise fail
        |
        +-- one -> inject it
        |
        +-- many -> qualifier/primary/fallback/name rules
                         |
                         +-- still ambiguous -> fail startup
```

Spring Framework 7 also documents `@Fallback` for candidates that should lose to regular beans. Treat it as version-sensitive; `@Primary` and explicit qualifiers remain common interview expectations.

## Common mistakes

- Using field injection because it is shorter.
- Adding `@Primary` to silence ambiguity without defining a real default.
- Injecting a `Map<String, Strategy>` and leaking bean names into domain behavior accidentally.
- Making every dependency optional.
- Applying `@Lazy` to hide a cycle without redesigning responsibilities.
- Expecting constructor injection to prevent mutable internal state.

## Interview angle

**Interviewer:** Why do you prefer constructor injection?

**Strong answer:** It makes required dependencies part of the type's construction contract, supports final references, fails an incomplete graph at startup, and allows pure Java tests. I do not claim it makes objects immutable or fixes too many responsibilities. Optional collaborators are modeled explicitly, and multiple candidates are selected by semantic qualifier or a deliberate strategy collection.

## Quick check

1. When is `@Autowired` optional on a constructor?
2. What is the difference between `@Primary` and `@Qualifier`?
3. When is `ObjectProvider` appropriate?
4. Why is collection ordering a contract decision?
5. What does a constructor cycle reveal?

## Predict and debug

**Predict:** Two `PaymentGateway` beans, neither qualified or primary, and one `PaymentGateway` parameter cause context refresh to fail with an ambiguity.

**Debug:** A developer changes the parameter name to match a bean and the error disappears. Replace the incidental name match with an explicit semantic qualifier.

## Practice

- **Foundation:** Refactor a field-injected service into constructor injection.
- **Foundation:** Register two `Clock` beans and select one with a qualifier.
- **Interview Core:** Inject and order three validation rules; write a test for their sequence.
- **Interview Core:** Model an optional audit sink without returning null.
- **SDE-2 Follow-up:** Redesign an `OrderService <-> InventoryService` cycle and defend the new boundary.

## Readiness checkpoint

Continue when you can predict zero, one, or many-candidate outcomes and can remove a circular dependency without hiding it behind lazy injection.
