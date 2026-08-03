# Transaction Abstraction and Declarative Boundaries

A transaction protects a business invariant across a set of resource operations. `@Transactional` describes a boundary; a `PlatformTransactionManager` implements begin, participation, commit, and rollback for a specific resource strategy.

## Start from the invariant

Placing an order writes an order and reserves inventory. The invariant is not "both repository methods have annotations." It is:

> Either the order and reservation both commit, or neither becomes durable.

```text
controller
    |
    v
transaction proxy
    |
    +-- BEGIN
    +-- target OrderApplicationService.place(...)
    |       +-- insert order
    |       +-- update inventory
    +-- COMMIT
    |
    +-- on qualifying failure: ROLLBACK
```

The application-service operation is usually the right boundary because it owns the full use case.

## Minimal declarative setup

```java
@Configuration
@EnableTransactionManagement
class TransactionConfiguration {
    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

```java
@Service
final class OrderApplicationService {
    private final OrderRepository orders;
    private final InventoryRepository inventory;

    @Transactional
    public long place(PlaceOrder command) {
        long orderId = orders.insert(command);
        inventory.reserve(command.sku(), command.quantity());
        return orderId;
    }
}
```

For JPA, use the appropriate transaction manager that coordinates the `EntityManager`. Do not combine transaction managers casually and assume one annotation creates atomicity across them.

## Runtime sequence

1. Caller invokes the Spring proxy.
2. Transaction interceptor resolves transaction attributes.
3. Manager opens or joins a transaction and binds resource state to the execution context, commonly the current thread for imperative transactions.
4. Target method executes.
5. Normal return leads to commit unless marked rollback-only.
6. A configured throwable leads to rollback.
7. Resources are cleaned up and unbound.

The database owns actual isolation, locks, constraints, and durability. Spring coordinates access; it does not redefine ACID.

## Declarative and programmatic styles

Declarative transactions keep ordinary service code focused:

```java
@Transactional
public void transfer(...) { ... }
```

`TransactionTemplate` makes boundaries explicit when one method needs multiple differently scoped units:

```java
long id = transactionTemplate.execute(status -> orders.create(command));
remoteClient.notify(id); // outside database transaction
```

Programmatic transaction code is not inherently superior. Use it when boundary shape truly requires explicit control, not to work around misunderstood proxies.

## Rollback defaults

By default, Spring declarative transactions roll back for unchecked `RuntimeException` and `Error`, not ordinary checked exceptions. Customize when a checked business/infrastructure exception must roll back:

```java
@Transactional(rollbackFor = PaymentFileException.class)
public void importPaymentFile(...) throws PaymentFileException { ... }
```

Do not add `rollbackFor = Exception.class` everywhere without examining which exceptions mean failure versus an expected alternate result.

## Catching exceptions changes the outcome

```java
@Transactional
public void place(PlaceOrder command) {
    try {
        inventory.reserve(command.sku(), command.quantity());
    } catch (RuntimeException failure) {
        log.warn("reservation failed", failure);
        // method returns normally: transaction may commit prior work
    }
}
```

If the exception is swallowed, the interceptor sees a normal return unless inner infrastructure already marked the transaction rollback-only. Either translate and rethrow according to contract, or explicitly mark rollback-only only when that coupling is justified.

## Transaction boundaries and remote calls

Holding a database transaction open during an HTTP call increases connection hold time, lock duration, latency variance, and failure coupling. Prefer:

```text
short DB transaction -> commit intent/outbox -> remote delivery -> idempotent status update
```

When a remote response must determine the database change, model a state machine and compensating/reconciliation behavior. Spring transactions cannot atomically commit an ordinary HTTP API.

## Common mistakes

- Annotating private helpers and expecting proxy advice.
- Spreading transaction boundaries across repository methods so one use case is not atomic.
- Catching failures and accidentally committing partial work.
- Keeping transactions open during slow remote calls.
- Assuming one transaction manager covers multiple databases or a broker.
- Testing only successful writes, never rollback.

## Interview angle

**Interviewer:** Where do you put `@Transactional`?

**Strong answer:** At an externally invoked application-service method that owns one business invariant and one intentionally bounded resource unit. Repositories participate in that boundary. I keep remote I/O outside, choose the correct manager, state rollback rules, and integration-test both durable commit and rollback rather than merely checking that the annotation exists.

## Quick check

1. Who owns actual database locks and isolation?
2. What are default rollback throwable categories?
3. Why can catching an exception cause partial commit?
4. When is `TransactionTemplate` useful?
5. Can a JDBC transaction atomically include an HTTP call?

## Predict and debug

**Predict:** A checked exception leaves the method under default rules. Unless configured otherwise or marked rollback-only, the transaction can commit.

**Debug:** Order row persists after inventory failure. Inspect call-through-proxy, manager choice, exception translation/catching, propagation, and an integration test against committed database state.

## Practice

- **Foundation:** Mark begin, statements, commit, and rollback on a service sequence.
- **Foundation:** Write a rollback integration test for two writes.
- **Interview Core:** Refactor repository-level transactions into one use-case boundary.
- **Interview Core:** Move one remote call outside a database transaction.
- **SDE-2 Follow-up:** Design an order/outbox workflow with idempotency and reconciliation.

## Readiness checkpoint

Continue when you can state invariant, proxy entry, manager, resource, rollback rule, and external side-effect boundary for one transaction.
