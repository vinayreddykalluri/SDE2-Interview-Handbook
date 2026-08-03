# Spring MVC Request Flow and HTTP Boundaries

Spring MVC is part of Spring Framework. Spring Boot can configure it conveniently, but the request pipeline belongs to Framework and should not be treated as hidden magic.

## Request flow

```text
HTTP client
    |
    v
Servlet container filters
    |
    v
DispatcherServlet
    |
    +-> HandlerMapping finds controller method
    +-> HandlerAdapter resolves arguments and invokes it
    |       +-> conversion / binding / validation
    |       +-> application service
    +-> return-value handling / message conversion
    +-> exception resolvers when needed
    |
    v
HTTP response
```

`DispatcherServlet` is the front controller. It delegates to strategies; it does not contain every application behavior itself.

## A small controller

```java
@RestController
@RequestMapping("/orders")
final class OrderController {
    private final OrderApplicationService orders;

    OrderController(OrderApplicationService orders) {
        this.orders = orders;
    }

    @PostMapping
    ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orders.create(request);
        URI location = URI.create("/orders/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    OrderResponse find(@PathVariable long id) {
        return orders.require(id);
    }
}
```

The controller translates HTTP into an application command/query and translates the result back. It should not own SQL, multi-step domain rules, or remote retry loops.

## Parameter sources are different contracts

- `@PathVariable`: identifies a resource in the route.
- `@RequestParam`: query/form parameter, often filtering or optional control.
- `@RequestHeader`: protocol metadata; avoid making core domain state header-only.
- `@RequestBody`: content decoded by an HTTP message converter.
- Framework-provided types: request context, locale, authentication principal when configured.

Never assume a missing value becomes a safe Java default. Define required/optional behavior and validate ranges.

## DTOs protect the boundary

```java
record CreateOrderRequest(
        @NotBlank String requestKey,
        @NotBlank String sku,
        @Positive int quantity) { }
```

Do not expose persistence entities as request/response models. DTOs avoid lazy-loading during serialization, over-posting, accidental internal fields, and schema coupling.

## HTTP semantics

Use status codes to express the contract:

| Situation | Typical response |
|---|---|
| created resource | `201 Created` plus `Location` |
| successful read | `200 OK` |
| no response body | `204 No Content` |
| malformed/invalid input | `400 Bad Request` |
| unauthenticated | `401 Unauthorized` |
| authenticated but forbidden | `403 Forbidden` |
| missing resource | `404 Not Found` |
| state/version/idempotency conflict | `409 Conflict` |
| unsupported media type | `415 Unsupported Media Type` |
| unexpected server failure | `500 Internal Server Error` |

The exact API contract can refine these choices. Do not return `200` with an error string for every outcome.

## Central exception translation

```java
@RestControllerAdvice
final class ApiExceptionHandler {
    @ExceptionHandler(OrderNotFound.class)
    ResponseEntity<ProblemDetail> notFound(OrderNotFound failure) {
        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setTitle("Order not found");
        problem.setProperty("code", "ORDER_NOT_FOUND");
        return ResponseEntity.status(404).body(problem);
    }
}
```

Map known application failures precisely. Log unexpected failures once at the owning boundary with a correlation/trace identifier, then return a safe generic problem. Never return stack traces, SQL, secrets, or internal class names.

## Filters, interceptors, and controller advice

| Mechanism | Boundary | Good use |
|---|---|---|
| Servlet filter | before/after Spring MVC, raw request/response | correlation ID, low-level protocol/security filter chain |
| `HandlerInterceptor` | mapped MVC handler execution | handler-aware timing or policy metadata |
| controller advice | controller binding/return/exception concerns | consistent API exception and binding responses |
| AOP service advice | Spring bean method | transaction, service policy, metrics |

Authentication and authorization should use Spring Security rather than custom filter inventions; full security is in **SD 10 - Spring Ecosystem Extensions**.

## Idempotent create endpoints

For retryable clients, accept an idempotency key, bind it to caller plus operation, store a payload fingerprint and result under a uniqueness constraint, and return the prior result for a true replay. Do not hold an HTTP request open inside a long database transaction unnecessarily.

## Common mistakes

- Returning entities directly.
- Putting business orchestration in controllers.
- Using `@ControllerAdvice` to swallow every exception as `200`.
- Treating filters, interceptors, and aspects as interchangeable.
- Logging full bodies or authorization headers.
- Assuming request validation prevents database conflicts.

## Interview angle

**Interviewer:** Walk through a Spring MVC request.

**Strong answer:** The servlet container runs filters and dispatches to `DispatcherServlet`. Handler mappings choose a controller method, a handler adapter resolves and converts arguments, validation runs when configured, the controller invokes an application boundary, return-value handlers and message converters produce the response, and exception resolvers translate failures. I keep DTO, status, auth, transaction, and error contracts explicit.

## Quick check

1. What role does `DispatcherServlet` play?
2. Why are entities poor API DTOs?
3. Which layer should own a database transaction?
4. How do a filter and interceptor differ?
5. What must a safe error response exclude?

## Predict and debug

**Predict:** `@Valid` is omitted from a body parameter. Bean Validation constraints on the DTO may not be invoked for that parameter.

**Debug:** Serializing an order triggers 100 queries. Map a bounded DTO inside the service transaction and fetch/project exactly the response shape; do not make all associations eager.

## Practice

- **Foundation:** Map create and find routes with appropriate statuses.
- **Foundation:** Replace an entity response with a record DTO.
- **Interview Core:** Translate validation, not-found, conflict, and unexpected errors.
- **Interview Core:** Place correlation, authorization, transaction, and exception logic at correct boundaries.
- **SDE-2 Follow-up:** Design an idempotent create endpoint with concurrent duplicate requests.

## Readiness checkpoint

Continue when you can trace one request from filter to response and explain why each concern belongs at its chosen boundary.
