# Testing Ladder, Contexts, Slices, and Testcontainers

The strongest Boot test suite uses the smallest boundary that proves each claim. Full-context tests are valuable, but they are expensive and often conceal which behavior failed.

## The ladder

```text
pure unit
  -> configuration binding / ApplicationContextRunner
  -> web or data slice
  -> full application context without server
  -> random-port process integration
  -> target dependency with Testcontainers
  -> deployment smoke / end-to-end
```

Each level adds realism and failure surface. Do not ask one level to prove what it cannot observe.

## Pure unit test

```java
@Test
void rejectsEmptyOrder() {
    OrderService service = new OrderService(repository, paymentGateway);
    assertThatThrownBy(() -> service.create(emptyCommand()))
            .isInstanceOf(InvalidOrderException.class);
}
```

No Boot context is needed for domain behavior constructed from plain Java dependencies.

## Auto-configuration test

`ApplicationContextRunner` starts a focused context and exposes rich assertions:

```java
runner.withPropertyValues("acme.audit.enabled=false")
      .run(context -> assertThat(context)
              .doesNotHaveBean(AuditSink.class));
```

This is the right tool for condition, back-off, and binding matrices.

## Test slices

A web slice loads controller-oriented infrastructure while replacing/importing collaborators deliberately. Data slices load data infrastructure for the selected technology. Boot 4 uses more focused test modules/starters, but the principle is unchanged: a slice is a restricted context, not a smaller production process.

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrderService service;
}
```

Test request binding, validation, status, JSON, and error translation. Do not mock MVC itself.

## Full context

```java
@SpringBootTest
class ApplicationWiringTest {
    @Test
    void contextStarts() { }
}
```

A single context-start test detects broken assembly but has weak diagnostics alone. Add assertions about important beans, properties, and conditions.

Random-port tests start the real embedded server and exercise serialization, filters, server configuration, and network behavior. They are slower and require reliable port/client cleanup.

## Testcontainers

Use the target database or broker when dialect, locking, migrations, transaction isolation, or driver behavior matters. Boot service connections can derive connection details from supported containers, reducing manual dynamic properties.

Still pin container versions, wait for readiness, isolate data, and record logs on failure. A container test is not production load testing.

## Context cache and test pollution

Spring caches compatible contexts. Excessive distinct property sets, profiles, mocks, and dirty contexts slow the suite. Mutable singleton state and leaked threads make order-dependent tests.

Measure context count and move behavior down the ladder. Use `@DirtiesContext` only when the test genuinely invalidates the context.

## Commit behavior

Rollback-by-default tests may not prove commit-time constraints, after-commit events, or behavior from a new transaction. Add tests that commit and verify from a separate transaction where durability matters.

## Common mistakes

- Using `@SpringBootTest` for every service method.
- Mocking repositories in a test claimed to prove SQL.
- Using H2 for MySQL-specific locking behavior.
- Sharing mutable container data across tests.
- Assuming context-start proves endpoint correctness.
- Making tests depend on execution order.
- Testing only rollback paths and never commit evidence.

## Interview angle

**Interviewer:** How do you choose a Boot test annotation?

**Strong answer:** I start from the claim. Pure logic gets a unit test; condition/binding behavior gets `ApplicationContextRunner`; MVC mapping gets a web slice; cross-bean assembly gets a full context; real server filters/serialization get random port; database-specific behavior gets a pinned target container. I minimize context variants and add commit evidence where required.

## Quick check

1. What does a web slice intentionally omit?
2. When is `ApplicationContextRunner` best?
3. What extra behavior does random port prove?
4. Why can rollback tests mislead?
5. What causes context-cache fragmentation?

## Practice

- **Foundation:** Move one service test from full context to pure Java.
- **Interview Core:** Write a four-case auto-configuration matrix.
- **Interview Core:** Test validation/error JSON with a web slice.
- **SDE-2 Follow-up:** Design the minimum test portfolio for an idempotent database-backed POST.
