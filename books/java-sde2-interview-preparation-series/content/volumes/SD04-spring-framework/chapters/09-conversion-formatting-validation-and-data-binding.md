# Conversion, Formatting, Validation, and Data Binding

External data begins as text, bytes, or loosely typed structures. A strong application converts it to typed values, validates structural constraints, then applies business invariants at a trusted boundary.

## Conversion versus validation

```text
"2026-08-01" --convert--> LocalDate
       |                         |
 invalid syntax             validate rule
                          deliveryDate >= today
```

Conversion answers "can this representation become this type?" Validation answers "is this value acceptable for this use?" Business logic answers "is this state transition allowed now?"

## `ConversionService`

Spring's type-conversion system supports standard conversions and custom converters.

```java
record OrderId(long value) {
    OrderId {
        if (value <= 0) {
            throw new IllegalArgumentException("order ID must be positive");
        }
    }
}

final class StringToOrderId implements Converter<String, OrderId> {
    @Override
    public OrderId convert(String source) {
        return new OrderId(Long.parseLong(source));
    }
}
```

Register converters centrally so controllers and configuration share consistent rules. Conversion exceptions should become safe boundary errors rather than internal stack traces returned to callers.

## Formatting is presentation-aware

Formatters convert text with locale context, commonly for numbers and dates. Keep canonical storage types independent of locale. `1,234.50` is not universally parsed the same way.

## Spring `Validator`

```java
record CreateOrderRequest(String requestKey, int quantity) { }

final class CreateOrderValidator implements Validator {
    @Override
    public boolean supports(Class<?> type) {
        return CreateOrderRequest.class.isAssignableFrom(type);
    }

    @Override
    public void validate(Object target, Errors errors) {
        CreateOrderRequest request = (CreateOrderRequest) target;
        ValidationUtils.rejectIfEmptyOrWhitespace(
                errors, "requestKey", "requestKey.required");
        if (request.quantity() <= 0) {
            errors.rejectValue("quantity", "quantity.positive");
        }
    }
}
```

Jakarta Bean Validation annotations such as `@NotBlank` and `@Positive` are also common when a provider is present. Spring integrates with that standard. Do not assume the annotation alone performs validation; a validation boundary must invoke it.

## Structural versus business validation

| Concern | Good location |
|---|---|
| required text, length, numeric range | request/command validation |
| ID syntax | conversion/binding boundary |
| order may transition from CREATED to PAID | domain/service invariant |
| request key unique under concurrency | database constraint plus service handling |
| caller may edit this tenant's order | authorization boundary |

An existence check followed by insert is not a concurrency-safe uniqueness guarantee. Keep the database constraint.

## Data binding risks

Data binding maps external property names to object properties. Binding directly to entities or broad mutable objects can allow **over-posting**: a client submits fields such as `role`, `tenantId`, `price`, or `status` that were never intended to be editable.

Use narrow request records/DTOs and explicit mapping:

```java
record UpdateAddressRequest(String line1, String city, String postalCode) { }

void updateAddress(long customerId, UpdateAddressRequest request) {
    Customer customer = repository.require(customerId);
    customer.changeAddress(request.line1(), request.city(), request.postalCode());
}
```

The entity controls allowed mutation. Authorization and persistence remain explicit.

## Error response design

Return stable error codes and field paths, not implementation details:

```json
{
  "code": "VALIDATION_FAILED",
  "errors": [
    {"field": "quantity", "code": "quantity.positive"}
  ]
}
```

Do not echo secret values, raw SQL, class names, or rejected payloads without redaction.

## SpEL boundary

The Spring Expression Language can navigate properties, call methods, and evaluate expressions in configuration and framework features. It is powerful enough to become a security and maintainability problem. Never evaluate untrusted user input as SpEL. Prefer ordinary Java for business rules and use small, reviewed expressions only where the hosting feature requires them.

## Common mistakes

- Treating conversion failure as business validation.
- Assuming Jakarta validation annotations execute automatically everywhere.
- Validating uniqueness only with a pre-query.
- Binding HTTP data directly to persistence entities.
- Returning rejected secret values in error details.
- Accepting user-authored SpEL.

## Interview angle

**Interviewer:** Where do you validate an order request?

**Strong answer:** I convert syntax at the boundary, validate structural fields on a narrow request type, authorize the caller, enforce domain state transitions in the domain/service, and preserve concurrency-sensitive invariants with database constraints or atomic operations. I return stable safe errors and never bind arbitrary client fields onto an entity.

## Quick check

1. How do conversion and validation differ?
2. What makes formatting locale-sensitive?
3. Why is a validation annotation not sufficient by itself?
4. What is over-posting?
5. Why can a uniqueness pre-check race?

## Predict and debug

**Predict:** A request passes `@NotBlank` but violates a unique key at commit. Structural validation cannot eliminate a concurrent database race.

**Debug:** A user changed `isAdmin` by adding a JSON field. Replace entity binding with an allowlisted request DTO, explicit mapping, authorization, and tests for rejected unknown/protected fields.

## Practice

- **Foundation:** Convert a string to a validated `OrderId`.
- **Foundation:** Validate required request key and positive quantity.
- **Interview Core:** Separate five rules into conversion, structural, domain, authorization, or database enforcement.
- **Interview Core:** Replace entity binding with an update request record.
- **SDE-2 Follow-up:** Design localized safe validation errors with stable machine codes and telemetry.

## Readiness checkpoint

Continue when you can trace untrusted text through conversion, structural validation, authorization, domain invariants, and durable constraints without merging those responsibilities.
