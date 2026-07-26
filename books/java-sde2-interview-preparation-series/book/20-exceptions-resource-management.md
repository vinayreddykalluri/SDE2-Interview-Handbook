# 20. Exceptions and Resource Management

## Learning objectives

By the end of this chapter, you should be able to:

- distinguish checked exceptions, unchecked exceptions, and errors by contract;
- trace matching, propagation, rethrow, `finally`, and suppressed exceptions;
- use try-with-resources with correct ownership and close order;
- translate failures without losing cause or actionable context; and
- design retryable, observable, and non-leaking production error boundaries.

## Why this matters at SDE-2

Failure handling determines whether a service preserves data and recovers predictably. A lost cause obscures incidents, an overbroad catch turns corruption into a fake success, and a leaked connection can exhaust a pool. SDE-2 engineers must design failure semantics, not merely make the compiler accept `throws` clauses.

Interviews commonly ask about checked versus unchecked exceptions, `finally` behavior, close ordering, and what happens when both work and cleanup fail. The strongest answers connect the mechanics to API ownership and operational policy.

## First-principles model

An exception is an object representing abrupt completion. `throw` transfers control up dynamically nested execution until a compatible `catch` is found. If no application frame catches it, the thread terminates after its uncaught-exception handling.

Java divides `Throwable` into `Error` and `Exception`. `RuntimeException` and its subclasses are unchecked, as are `Error` subclasses. Other `Exception` subclasses are checked: a potentially escaping checked exception must be caught or declared.

A resource is something with a lifecycle that must be closed regardless of normal or abrupt completion. Try-with-resources gives that lifecycle structured scope. Ownership answers who closes; it is separate from which variable can access the resource.

> **Specification boundary:** Java specifies exception search, `finally`, try-with-resources translation, close order, and suppression. It does not guarantee that a process can recover from every `Error`, that every termination path runs cleanup, or that stack-trace collection has a fixed cost.

## Core terminology

- **Checked exception:** non-`RuntimeException` subclass of `Exception`, enforced by compile-time handling rules.
- **Unchecked exception:** `RuntimeException`, `Error`, or subclass.
- **Cause:** lower-level failure wrapped by another exception.
- **Stack trace:** recorded execution frames associated with a throwable.
- **Suppressed exception:** secondary failure retained when another failure is primary.
- **Try-with-resources:** statement that closes `AutoCloseable` resources automatically.
- **Exception translation:** mapping an implementation failure to a boundary-appropriate abstraction.
- **Precise rethrow:** compiler infers narrower exceptions when rethrowing an effectively final catch parameter.
- **Idempotency:** repeated operation has an effect compatible with safe retry.
- **Ownership:** responsibility for closing or releasing a resource.

## Detailed mechanics

### Throwing and declaring

`throw` requires a `Throwable` reference; throwing null causes `NullPointerException`. A `throws` clause declares possible checked failures for callers but does not itself throw anything. Unchecked failures may be documented without being declared.

Checked exceptions can communicate a recoverable condition a caller is expected to consider. Unchecked exceptions commonly represent violated preconditions, programming defects, or failures handled at a wider boundary. This is an API design choice, not a guarantee that checked failures are recoverable or unchecked failures are fatal.

```java
static int parsePort(String text) {
    int port;
    try {
        port = Integer.parseInt(text);
    } catch (NumberFormatException ex) {
        throw new IllegalArgumentException("port is not numeric: " + text, ex);
    }
    if (port < 1 || port > 65_535) {
        throw new IllegalArgumentException("port out of range: " + port);
    }
    return port;
}
```

Translation keeps the original exception as cause and adds domain context. Avoid putting secrets in messages.

### Catch selection and propagation

Catch clauses are tested in source order, and the first assignment-compatible type handles the throwable. Therefore a subtype catch must precede a supertype catch or the latter makes it unreachable. After the catch runs normally, execution continues after the whole try statement.

Multi-catch uses alternatives such as `catch (IOException | SQLException ex)`. Alternatives cannot be related by subclassing, and the catch parameter is implicitly final because assigning it would make its type unclear.

Precise rethrow lets this pattern declare the specific possible checked types rather than broad `Exception` when the caught parameter is not reassigned:

```java
static void invoke() throws java.io.IOException, java.sql.SQLException {
    try {
        callThatThrowsEither();
    } catch (Exception ex) {
        audit(ex);
        throw ex;
    }
}
```

This should not justify catching broad types routinely; it is useful at cross-cutting boundaries that must observe and rethrow unchanged.

### Finally and abrupt completion

A `finally` block executes after its associated try/catch completes normally or abruptly, before control continues outward. If `finally` itself returns or throws, it replaces the pending return value or exception. That can silently lose the true failure.

```java
static int misleading() {
    try {
        return 1;
    } finally {
        return 2; // replaces the pending return; avoid this
    }
}
```

Cleanup may not run if the process halts, is externally killed, the JVM crashes, or execution cannot proceed. Resource safety is scoped, not a substitute for durable recovery protocols.

### Try-with-resources

A resource type implements `AutoCloseable`; `Closeable` is a narrower I/O-oriented subtype whose `close` declares `IOException`. Resources initialize left to right and close in reverse order.

```java
try (var input = java.nio.file.Files.newBufferedReader(path);
     var output = java.nio.file.Files.newBufferedWriter(target)) {
    output.write(input.readLine());
}
```

If initialization of a later resource fails, already initialized earlier resources are closed. If the body throws and closing also throws, the body exception remains primary and close failures are attached through `getSuppressed()`. If the body succeeds but close fails, the close exception propagates. Multiple close failures retain the first propagating close failure as primary and later ones as suppressed according to reverse close order.

Since Java 9, an existing final or effectively final resource variable can appear directly in the resource specification:

```java
var reader = java.nio.file.Files.newBufferedReader(path);
try (reader) {
    System.out.println(reader.readLine());
}
```

The statement assumes ownership and closes it. The variable remains in scope afterward but denotes a closed resource; do not use it.

The conceptual compiler translation uses nested try/finally logic and `addSuppressed`. Writing that translation manually is error-prone, so use try-with-resources whenever ownership is lexical.

### Rethrow, wrapping, and interruption

Use `throw ex;` to rethrow the same object and preserve its trace. Wrapping with `new DomainException(message, ex)` creates an abstraction boundary with a cause. `throw new DomainException(ex.getMessage())` discards the cause and should usually be rejected in review.

Do not catch `Throwable` to continue normal service; it includes serious `Error` conditions. Catching `Exception` at a request or job boundary can be appropriate to log, translate, and isolate one unit of work, provided fatal conditions and cancellation semantics remain intact.

`InterruptedException` communicates cooperative cancellation. Code that cannot propagate it should normally restore the status with `Thread.currentThread().interrupt()` and stop its work. Swallowing it can make shutdown and timeouts fail.

## Worked Java example

This example demonstrates primary and suppressed failures with two resources.

```java
public class SuppressionDemo {
    static final class Resource implements AutoCloseable {
        private final String name;

        Resource(String name) {
            this.name = name;
            System.out.println("open " + name);
        }

        @Override
        public void close() {
            System.out.println("close " + name);
            throw new IllegalStateException("close failed: " + name);
        }
    }

    public static void main(String[] args) {
        try (Resource first = new Resource("first");
             Resource second = new Resource("second")) {
            throw new IllegalArgumentException("body failed");
        } catch (Exception ex) {
            System.out.println("primary: " + ex.getMessage());
            for (Throwable suppressed : ex.getSuppressed()) {
                System.out.println("suppressed: " + suppressed.getMessage());
            }
        }
    }
}
```

The body failure stays primary, preserving the operation's causal event. Both cleanup failures remain discoverable for diagnosis.

## Execution or memory walkthrough

`first` initializes, then `second`. The body constructs and throws `IllegalArgumentException`. Before catch selection, resources close in reverse order. Closing `second` throws; because a body exception is already pending, this failure is added to the body's suppressed list. Closing `first` also throws and is added next.

The `catch (Exception ex)` matches the primary `IllegalArgumentException`. It prints `body failed`, followed by `close failed: second` and `close failed: first`. The close exceptions are not causes; they are independent secondary failures during cleanup.

Had resource construction for `second` thrown, the body would never begin, `first` would still close, and any close failure would be suppressed under the second-construction failure.

## Complexity and performance

Normal try blocks do not inherently change algorithmic complexity. Throwing and capturing a stack trace can be expensive relative to ordinary branches, and exceptions should not represent expected high-volume control flow. Catch matching scales with propagation depth and handlers but is not usually the dominant production concern.

Try-with-resources adds close work that was required anyway. Its safety far outweighs tiny structural overhead. Stack traces and retained causes consume memory, especially if exceptions are accumulated rather than logged and released.

> **HotSpot note:** HotSpot optimizes normal paths through exception regions and may omit or change some diagnostic detail for certain repeated VM-thrown exceptions. Do not depend on such implementation behavior; preserve useful application context explicitly.

## Edge cases and common mistakes

- Catching a superclass before its subclass is unreachable code.
- Throwing from `finally` masks a pending exception; returning from `finally` masks both exceptions and returns.
- A `throws Exception` declaration exports implementation uncertainty and burdens every caller.
- Logging and rethrowing at every layer produces duplicate noise; log where context and ownership meet.
- Wrapping without a cause destroys the diagnostic chain.
- Catching and ignoring `InterruptedException` breaks cancellation.
- Retrying after an unknown partial side effect can duplicate writes.
- `AutoCloseable.close` is allowed to throw broad `Exception` and is not universally idempotent.
- Resource variables close in reverse declaration order.
- A caller-supplied stream should not be closed by a callee unless ownership transfer is explicit.
- `getCause()` and `getSuppressed()` represent different relationships.
- An exception message can leak tokens, SQL, customer data, or filesystem paths.

## Production engineering notes

Define exceptions at architectural boundaries. Translate driver errors into repository failures and domain failures into stable API responses, while retaining the original cause internally. Give clients machine-readable error codes rather than parsing messages.

Retries require classification, idempotency, deadlines, bounded attempts, backoff, and jitter. Never classify only by broad exception type when the provider exposes more specific status. Preserve interruption and cancellation.

Use try-with-resources for files, JDBC statements/results, streams, locks represented by closeable guards, and telemetry scopes. Declare ownership in method names or documentation. At top-level request and worker boundaries, record exception class, safe context, trace or correlation ID, and suppressed failures without logging secrets.

## Interview questions and model answers

**What is the difference between checked and unchecked exceptions?**

Checked exceptions are `Exception` subclasses other than `RuntimeException`; Java requires callers to catch or declare them. Runtime exceptions and errors are unchecked. The hierarchy affects compile-time handling, not an absolute recoverability classification.

**What happens if both a try body and close throw?**

In try-with-resources, the body exception remains primary. Close exceptions are attached as suppressed throwables in reverse close order. This preserves both the causal operation failure and cleanup evidence.

**Why should you avoid return in finally?**

It replaces a pending return or exception, silently changing behavior and losing diagnostics. `finally` should perform limited cleanup that does not determine the method outcome.

**When should an exception be translated?**

At an abstraction boundary where the lower-level type is not part of the caller's contract. The translated exception should add safe, actionable context and retain the original as its cause.

**How should InterruptedException be handled?**

Prefer propagating it so an owning layer can cancel. If an API cannot declare it, restore interrupt status, stop the current operation, and translate only if the boundary requires it. Do not swallow and continue.

## Exercises

1. Predict primary and suppressed exceptions for three resources whose body and close methods fail.
2. Refactor manual file cleanup into try-with-resources and test initialization failure.
3. Design repository exception translation that preserves SQL cause without exposing SQL to clients.
4. Find all ways a return or throw in `finally` can alter a pending outcome.
5. Implement a retry loop that stops on interruption, deadline, permanent failure, and maximum attempts.
6. Document ownership for a method accepting an `InputStream`, then offer borrowing and ownership-transfer variants.

## Chapter summary

Exceptions represent abrupt completion and propagate to the first compatible handler. Checked status is a compile-time contract choice. `finally` participates in abrupt completion and can dangerously replace outcomes. Try-with-resources initializes left to right, closes right to left, and preserves secondary failures through suppression. Production failure handling retains causes, respects cancellation and ownership, translates at abstraction boundaries, and retries only with bounded, idempotent policy.

## Revision checklist

- [ ] I can classify checked, runtime, and error types.
- [ ] I can trace catch selection and propagation.
- [ ] I distinguish causes from suppressed exceptions.
- [ ] I know try-with-resources initialization and reverse close order.
- [ ] I never return from `finally` and avoid throwing from cleanup.
- [ ] I preserve causes when translating failures.
- [ ] I propagate or restore interruption.
- [ ] I make resource ownership, retries, and client error contracts explicit.
