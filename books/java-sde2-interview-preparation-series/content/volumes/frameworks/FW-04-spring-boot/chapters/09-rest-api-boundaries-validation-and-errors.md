# REST API Boundaries, Validation, and Errors

A Boot REST API is still a Spring MVC application. Boot configures the dispatcher, server, message converters, validation integration, and error infrastructure; the application must define a stable HTTP contract.

## Request flow

```text
socket -> embedded server -> filter chain -> DispatcherServlet
       -> handler mapping -> controller -> application service
       -> return value handler -> message converter -> HTTP response
```

Failures can happen before the controller, during binding/validation, inside business logic, or while serializing the response. Diagnose the phase before changing controller code.

## Narrow request and response models

```java
record CreateOrderRequest(
        @NotBlank String customerId,
        @NotEmpty List<@Valid OrderLineRequest> lines,
        @NotBlank String idempotencyKey) { }

record OrderLineRequest(
        @NotBlank String sku,
        @Positive int quantity) { }

record OrderResponse(UUID id, String status, Instant createdAt) { }
```

Do not bind HTTP JSON directly to a persistence entity. Request DTOs prevent over-posting, define validation, and let API and storage evolve independently.

## Controller boundary

```java
@RestController
@RequestMapping("/api/orders")
final class OrderController {
    private final OrderService service;

    OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = service.create(request);
        URI location = URI.create("/api/orders/" + created.id());
        return ResponseEntity.created(location).body(created);
    }
}
```

The controller translates HTTP into an application command, invokes one use case, and translates the result. It should not open database transactions manually or contain retry loops.

## Structural versus business validation

Bean Validation checks request shape: required text, numeric range, valid nested elements. Business validation may need current state: SKU exists, inventory can be reserved, idempotency key belongs to the same request. Database constraints protect concurrent invariants.

```text
JSON syntax -> binding -> structural validation -> authorization
            -> business invariant -> database constraint
```

Do not query the database from a custom field validator by default. It hides I/O and transaction semantics.

## Consistent errors

Spring supports RFC-style problem details. Centralize translation:

```java
@RestControllerAdvice
final class ApiExceptionHandler {
    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail notFound(OrderNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Order not found");
        problem.setDetail(exception.getMessage());
        return problem;
    }
}
```

Do not include stack traces, class names, SQL, credentials, or internal hostnames. Add a stable application error code and trace/correlation identifier where the API contract needs them.

## Status decisions

| Situation | Typical status |
|---|---:|
| created resource | 201 |
| invalid request syntax/shape | 400 |
| unauthenticated | 401 |
| authenticated but forbidden | 403 |
| missing resource | 404 |
| idempotency conflict/state conflict | 409 |
| semantic validation where contract uses it | 422 |
| unexpected server failure | 500 |
| temporary dependency unavailable | 503 |

The exact choice is an API contract. Keep it consistent and test response bodies as well as codes.

## Common mistakes

- Returning 200 for every outcome.
- Catching `Exception` in every controller.
- Leaking entity fields into JSON.
- Treating validation as authorization.
- Returning a raw exception message from a remote system.
- Forgetting validation on nested collection elements.
- Writing an error handler that converts programming defects into 400 responses.

## Interview angle

**Interviewer:** Where do you validate an order request?

**Strong answer:** MVC binding and Bean Validation reject malformed structure; authorization verifies the caller; the application service enforces state-dependent business invariants; and the database protects concurrent uniqueness. The controller maps failures to a stable problem contract without leaking internals.

## Quick check

1. Why use DTOs instead of entities?
2. What is structural validation?
3. Which phase runs before the controller?
4. When is 409 appropriate?
5. What must never appear in an error response?

## Practice

- **Foundation:** Build a validated create-order endpoint.
- **Foundation:** Add validation for each nested line.
- **Interview Core:** Define problem responses for four failures.
- **Interview Core:** Test that internal exception text is redacted.
- **SDE-2 Follow-up:** Design idempotent POST behavior under two concurrent requests.
