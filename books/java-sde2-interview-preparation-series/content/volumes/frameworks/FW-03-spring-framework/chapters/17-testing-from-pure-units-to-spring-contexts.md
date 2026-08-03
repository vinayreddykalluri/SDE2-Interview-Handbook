# Testing: From Pure Units to Spring Contexts

A Spring test is valuable only when Spring behavior is the subject. Most business logic should be tested as ordinary Java; container, proxy, transaction, and MVC contracts need focused integration tests.

## The test ladder

```text
many: pure Java unit tests
       |
       v
focused context tests for wiring/proxies/conversion
       |
       v
resource integration tests for database/HTTP/message behavior
       |
       v
few: end-to-end tests across deployed boundaries
```

More infrastructure means slower feedback and more failure causes. Use the lowest level that can disprove the risk.

## Pure unit test

```java
@Test
void rejectsNonPositiveQuantity() {
    OrderRepository repository = new InMemoryOrderRepository();
    OrderService service = new OrderService(repository, Clock.fixed(
            Instant.parse("2026-07-29T12:00:00Z"), ZoneOffset.UTC));

    assertThrows(IllegalArgumentException.class,
            () -> service.place("book", 0));
}
```

No context, annotations, or mocks are required if a small fake expresses the contract.

## Focused context test

```java
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
final class WiringTest {
    @Autowired
    OrderService service;

    @Test
    void contextWiresService() {
        assertNotNull(service);
    }
}
```

Use this to verify candidate resolution, lifecycle, profiles, conversion, events, or proxy presence. A test that merely checks `notNull` is weak unless wiring is the actual risk.

## Transaction tests must observe committed reality

Spring TestContext can run a test in a transaction that rolls back by default. That keeps data isolated but can hide production behavior:

- code under test may join the test transaction rather than create its normal boundary;
- flush/commit constraints may not appear until the test ends;
- lazy access remains possible because the test transaction is open;
- an after-commit listener will not run if the test rolls back.

For commit semantics, end/commit the test transaction deliberately or invoke through production boundaries and verify from a separate transaction/connection. Test both commit and rollback.

## Preemptive timeout trap

Spring binds test-managed transaction state to the current thread. A preemptive test timeout that runs the test body on another thread can execute database work outside that transaction, so changes may commit despite expected rollback. Prefer non-preemptive time assertions or explicit cleanup for integration tests.

## Context caching

The TestContext framework caches contexts by a key derived from configuration classes, profiles, property sources, customizers, and other inputs. Reusing the same configuration speeds a suite. Excess unique mocks/properties fragment the cache.

`@DirtiesContext` evicts a context and should be rare. Prefer resetting the mutated state you own. Spring Framework 7 can pause inactive cached contexts, but that does not excuse tests that leak threads or global state.

## Proxy contract tests

```java
@Test
void transactionalFailureRollsBackBothWrites() {
    assertThrows(RuntimeException.class,
            () -> service.place(failingCommand));

    assertEquals(0, countOrdersInNewTransaction());
    assertEquals(0, countReservationsInNewTransaction());
}
```

Call the injected service reference, not a manually constructed target. Assert durable outcomes. For AOP, include a matching call, a non-matching call, exception path, and advice order.

## MVC testing boundary

Use mock request infrastructure to test routing, conversion, validation, status, headers, JSON shape, and exception translation without starting a real server. Use an actual server or deployed test only for servlet container/network/TLS behavior. Spring Boot test slices are Boot features and belong in **SD 05**.

## Test doubles

- **Fake:** working simplified implementation, useful for domain contracts.
- **Stub:** returns predefined values.
- **Mock:** verifies interactions; use where interaction itself is the contract.
- **Spy:** wraps real behavior; can over-couple tests to implementation.

Do not mock value objects or every internal method. Verify observable state and boundary interaction.

## External resources

Use a real compatible database/broker/containerized service when vendor behavior matters. H2 is useful for small transaction demonstrations but cannot prove MySQL plans, locking, types, or SQL dialect. Keep fixtures deterministic, migrations real, and failure diagnostics accessible.

## Common mistakes

- Starting a full context for every domain unit test.
- Calling a target directly in a proxy behavior test.
- Trusting rollback-only tests to prove commit behavior.
- Hiding lazy-load bugs with a test transaction.
- Excessive `@DirtiesContext` and unique context configurations.
- Using H2 as proof of MySQL-specific semantics.

## Interview angle

**Interviewer:** How do you test `@Transactional`?

**Strong answer:** I load a focused context with the real transaction manager and database-compatible resource, invoke the proxied application service, force a qualifying failure after the first write, then inspect durable state from a separate transaction. I also test successful commit, checked/unchecked rollback rules where customized, and self-invocation or propagation risks. A test that itself rolls back is not enough evidence.

## Quick check

1. When is a pure unit test sufficient?
2. What does the TestContext cache key depend on?
3. How can a test transaction hide lazy-loading defects?
4. Why are preemptive timeouts dangerous with transactional tests?
5. Which database should prove vendor-specific locking?

## Predict and debug

**Predict:** An after-commit listener is tested inside a rollback-by-default test transaction. It may never execute.

**Debug:** The suite creates hundreds of contexts. Compare profiles, properties, mocks, and configuration keys; consolidate stable fixtures and remove unnecessary context dirtiness.

## Practice

- **Foundation:** Unit-test a constructor-injected service without Spring.
- **Foundation:** Write a focused context wiring test.
- **Interview Core:** Verify a proxy advice match and non-match.
- **Interview Core:** Prove rollback using state observed outside the failed transaction.
- **SDE-2 Follow-up:** Reduce a slow suite's context count while preserving isolation and production fidelity.

## Readiness checkpoint

Continue when each test names the risk it proves and uses the smallest environment that can faithfully expose that risk.
