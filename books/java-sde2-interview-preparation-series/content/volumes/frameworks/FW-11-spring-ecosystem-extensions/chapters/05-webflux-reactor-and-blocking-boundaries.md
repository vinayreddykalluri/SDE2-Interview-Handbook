# Spring WebFlux: Demand, Event Loops, and Blocking Boundaries

WebFlux is not “MVC but faster.” It is a reactive HTTP stack whose useful scaling model depends on asynchronous, non-blocking work across the entire hot path.

## Begin with the Reactive Streams contract

A publisher does not push an unlimited number of items at a subscriber. The subscriber receives a subscription and requests demand.

```text
Subscriber -> subscribe -> Publisher
Subscriber <- onSubscribe(Subscription)
Subscriber -> request(n)
Subscriber <- onNext(item) at most n times
Subscriber <- onComplete OR onError
Subscriber -> cancel() when no longer interested
```

Backpressure is the rule that production should respect downstream demand. It is not the same as a business queue, a broker, or unlimited buffering.

- `Mono<T>` represents zero or one asynchronous result.
- `Flux<T>` represents zero to many asynchronous results.
- A publisher is usually **lazy**: constructing a pipeline does not execute it.
- Operators describe behavior; subscription activates it.
- Errors are terminal signals unless transformed into a new publisher.

## WebFlux request flow

```text
socket event
   |
reactive HTTP server / event-loop thread
   |
WebFilter chain
   |
DispatcherHandler
   |
handler mapping -> controller/function
   |
reactive service -> non-blocking client/driver
   |
signals return as data becomes ready
```

An event loop can serve many in-flight requests because it does not dedicate a waiting thread to each socket. If application code blocks that event-loop thread, unrelated requests sharing it stop making progress.

## A simple composition

```java
Mono<OrderView> findOrder(String orderId) {
    return orderRepository.findById(orderId)
            .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
            .flatMap(order -> paymentClient.findStatus(order.paymentId()))
            .map(status -> OrderView.from(orderId, status));
}
```

Read it as a dependency graph:

1. Subscribe to order lookup.
2. If it completes empty, produce a not-found error.
3. When an order arrives, start the dependent payment call.
4. When status arrives, map to a view.

`map` transforms a value synchronously. `flatMap` composes another asynchronous publisher. Returning `null` from either callback violates the expected contract.

## Concurrency is not parallelism

Reactive pipelines can interleave many operations without running CPU work in parallel. Scheduler choice controls where work executes. Operator placement controls which part moves.

```java
Mono<Result> result = Mono.fromCallable(() -> blockingLegacyCall())
        .subscribeOn(Schedulers.boundedElastic())
        .timeout(Duration.ofMillis(300));
```

This isolates a small unavoidable blocking call on a bounded worker pool. It does not make the dependency non-blocking. Under load, the pool and its queue can still saturate. Measure and bound the bridge; prefer a truly asynchronous driver for sustained hot paths.

Never call `block()` in a reactive request handler. It can deadlock or exhaust event-loop capacity and discards cancellation/context advantages.

## `flatMap`, ordering, and concurrency

```java
Flux.fromIterable(orderIds)
        .flatMap(this::loadOrder, 16)
```

This allows up to 16 inner subscriptions. Results can arrive out of input order. Use `concatMap` when sequential ordering is required, or an ordered variant when concurrency and ordering are both justified. Do not choose unbounded concurrency for a downstream service with finite capacity.

## Error, timeout, retry, and cancellation

```text
source -> timeout -> retry classifier -> fallback/translation -> subscriber
```

Operator order changes semantics. Retrying outside a timeout may create a timeout per attempt. A timeout outside retry may cap the entire operation. Decide intentionally.

`onErrorResume` is not a universal “keep going.” A fallback must preserve the API’s business meaning. Turning payment failure into `PAID=false` may incorrectly imply an authoritative unpaid state.

Cancellation is cooperative. An HTTP client may cancel an in-flight exchange, but an already committed external side effect is not undone. Cleanup belongs in resource-aware operators and the underlying client/driver contract.

## Reactor context and security/trace context

Thread-local reasoning is unreliable because signals can execute on different threads. Reactor `Context` is associated with a subscription, not a mutable global map. Framework integrations can propagate security and observation context through supported operators.

Keep tenant/authorization decisions explicit. A value captured before a context switch is clearer than reading an ambient value deep inside business logic.

## Hot, cold, and accidental duplicate work

A cold publisher commonly performs its work per subscription. Two subscriptions can cause two HTTP calls:

```java
Mono<Quote> quote = quoteClient.fetch();
quote.subscribe(audit::record);
quote.subscribe(response::send); // potentially a second fetch
```

Sharing/caching changes lifetime, replay, error, and memory semantics. Do not add `cache()` merely to stop duplicates; decide who owns the result and for how long.

## Failure and edge-case matrix

| Scenario | What surprises candidates | Better design |
|---|---|---|
| JDBC call inside handler | Return type is reactive, work still blocks | Use MVC for blocking stack or isolate/migrate explicitly |
| `block()` on event loop | Thread waits for work that needs same capacity | Compose publishers end to end |
| Unbounded `flatMap` | Downstream and memory flood | Set concurrency/prefetch from capacity |
| Two subscriptions | Source executes twice | One ownership path or deliberate share/replay policy |
| `onErrorReturn(empty)` | Failure becomes valid empty data | Preserve degradation/error semantics |
| Timeout followed by retry | Side effect may duplicate | Idempotency plus remaining-deadline policy |
| `publishOn` placed late | Earlier blocking stage stays on event loop | Move/isolate the source correctly |
| Client disconnect | Server work assumed cancelled/rolled back | Observe cancellation; side effects need own consistency |
| Infinite stream with fast producer | Buffer grows despite reactive API | Demand-aware source and bounded policy |
| Shared mutable accumulator | Interleaved subscribers corrupt state | Per-subscription immutable/reduced state |

## MVC or WebFlux?

Choose MVC when the dependency stack is blocking, concurrency is moderate, and the team benefits from straightforward thread-per-request reasoning. Choose WebFlux when the hot path is genuinely non-blocking, many requests spend time waiting on I/O, streaming/backpressure is required, and the team can test reactive behavior.

Virtual threads may improve the scalability of blocking code, but they do not add backpressure, make remote calls cheaper, or remove database connection limits. Compare with measurements, not ideology.

## Quick check

1. What activates a cold reactive pipeline?
2. Why is `flatMap` not ordered by default?
3. What is the difference between moving a blocking call and making it non-blocking?
4. Can cancellation undo a committed remote side effect?
5. Why can a second subscription cause a second network call?

## Practice

- **Foundation:** Trace signals for a `Mono` that completes empty.
- **Interview Core:** Rewrite nested subscriptions as one composed pipeline.
- **Interview Core:** Place timeout and retry to enforce one total 700 ms deadline.
- **SDE-2 Follow-up:** Design and measure a bounded bridge to one blocking legacy SDK.
