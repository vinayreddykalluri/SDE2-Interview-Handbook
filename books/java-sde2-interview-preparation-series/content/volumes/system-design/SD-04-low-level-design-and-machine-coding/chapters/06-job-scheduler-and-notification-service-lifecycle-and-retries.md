# 6. Job Scheduler and Notification Service: Lifecycle and Retries

## Learning objectives

By the end of this chapter, you should be able to:

- order jobs with dependencies using Kahn's algorithm, with deterministic tie-breaking and a useful cycle error;
- model a job lifecycle as a transition table that rejects illegal moves;
- compute what a cancellation actually cancels;
- design retries with capped exponential backoff and explain why jitter is needed;
- design a notification service with channel strategies, user preferences, templates, fallback, and idempotency; and
- say which parts of both designs change when they move from one process to many.

## Why this matters at SDE-2

The job scheduler and the notification service are the LLD problems closest to real backend work, and interviewers use them to see whether you have operated software, not only written it. Anyone can put jobs in a queue. The SDE-2 questions are: what happens when a job fails, when it fails forever, when it is cancelled while its dependents wait, and when the same message is delivered twice. Those are lifecycle questions, and this chapter treats them as the core of the design.

## First-principles model

Both systems are **work items moving through a lifecycle, with side effects that may fail and may be retried.**

- A **job** has dependencies, a priority, a state, and an attempt count. The scheduler's job is to run each one exactly when its dependencies have succeeded, and to make failure visible.
- A **notification** has a recipient, content, and a set of possible channels. The service's job is to deliver it once, through the best channel that works.

Three principles apply to both:

1. **State transitions are explicit.** A job that has succeeded cannot be cancelled; a notification already delivered is not sent again.
2. **Retries are bounded and spaced.** Unbounded retries turn a transient outage into a permanent overload.
3. **Side effects are idempotent.** At-least-once execution is the realistic guarantee, so repeating an action must be harmless.

## Core terminology

- **DAG:** directed acyclic graph; the shape of valid job dependencies.
- **Topological order:** an order in which every job comes after its dependencies.
- **Kahn's algorithm:** repeatedly run a job with no unfinished dependencies.
- **Transitive dependents:** every job that depends, directly or indirectly, on a given job.
- **Exponential backoff:** waiting `base x 2^attempt` between retries, up to a cap.
- **Jitter:** randomising each wait so that many clients do not retry in lockstep.
- **Idempotency key:** an identifier that makes a repeated request a no-op.
- **Fallback channel:** the next delivery channel to try when the preferred one fails.

## Detailed mechanics

### Ordering jobs: Kahn's algorithm with a priority queue

Kahn's algorithm keeps each job's count of unfinished dependencies. Jobs whose count is zero are *ready*. Running a ready job decrements its dependents' counts, which may make them ready.

The detail that matters in an interview is the data structure holding ready jobs. A plain queue gives *an* order. A `PriorityQueue` ordered by priority and then by id gives a *deterministic* order that honours priority among the jobs that are actually ready - and only among those.

The companion's pipeline: `schema` (priority 5) and `fetch` have no dependencies; `clean` needs `fetch`; `load` needs `clean` and `schema`; `index` and `report` (priority 3) need `load`.

```text
execution order [schema, fetch, clean, load, report, index]
```

`schema` runs first because it is ready and has the highest priority. `report` runs before `index` for the same reason. But priority never lets a job jump its dependencies: `report` has priority 3 and still waits for `load`, which has priority 1. Say this explicitly, because "priority scheduling" is often misread as "high priority first, always".

### Cycles must be reported, not waited on

If `fetch` is changed to depend on `index`, the graph has a cycle. Kahn's algorithm detects it for free: it finishes with jobs still unprocessed.

```text
fetch now depends on index -> dependency cycle; stuck jobs [clean, fetch, index, load, report]
```

Note what the stuck set contains. The cycle is `fetch -> clean -> load -> index -> fetch`; `report` is not on it, but it depends on `load`, so it can never run either. A good error names the stuck jobs; a better one also extracts the cycle itself with a depth-first search so that the user knows which edge to remove. The failure mode to avoid is a scheduler that silently waits forever for jobs that can never become ready.

Validate at submission time, not at run time. Adding an edge that creates a cycle should be rejected when the job is registered.

### The lifecycle as a transition table

```text
PENDING   -> READY, CANCELLED
READY     -> RUNNING, CANCELLED
RUNNING   -> SUCCEEDED, READY (retry), FAILED (retries exhausted)
SUCCEEDED -> (terminal)
FAILED    -> (terminal)
CANCELLED -> (terminal)
```

The companion stores this as an `EnumMap<JobState, Set<JobState>>` and rejects any move not in the table. Two consequences:

- A flaky job's history is readable as data:

  ```text
  flaky job, 3 attempts: [PENDING, READY, RUNNING, READY, RUNNING, READY, RUNNING, SUCCEEDED]
  ```

- Cancelling a job that already succeeded throws, instead of quietly relabelling completed work.

Whether `RUNNING -> CANCELLED` is allowed is a real design decision. Allowing it requires cooperative cancellation - the running code must check a flag or respond to interruption - and the side effects already performed are not undone. The table above does not allow it; state that choice and what it would take to change.

### Cancellation cascades

Cancelling a job means none of its transitive dependents can ever run. They must move to `CANCELLED` too, or they sit in `PENDING` forever:

```text
cancelling clean also cancels [clean, index, load, report]
```

`schema` and `fetch` are unaffected, because they do not depend on `clean`. The closure is a graph traversal over the reverse edges. The same traversal answers "if this job fails permanently, what else fails?", which is the question an on-call engineer asks first.

### Retries: capped exponential backoff, with jitter

Retrying immediately after a failure usually fails again, because whatever broke has not recovered. Exponential backoff spaces retries out:

```text
backoff base 100ms, cap 5s: [100, 200, 400, 800, 1600, 3200, 5000, 5000]
```

The cap stops the wait from growing without limit. The companion's schedule is deterministic so that it can be tested; production code adds **jitter** - for example, waiting a random duration between zero and the computed delay. Without jitter, a thousand jobs that failed together during an outage all retry at exactly the same instants, and each wave can knock the recovering dependency over again.

Two more rules turn retries from harmful to helpful:

- **Retry only what is retryable.** A timeout or a 503 is transient; a validation error or a 400 will fail identically forever. Classify failures.
- **Bound the attempts and make exhaustion visible.** After the last attempt the job goes to `FAILED`, and in a real system it goes to a dead-letter queue with its error, where someone can see it.

### Notification service: channels are strategies

```text
Channel (enum)          PUSH, EMAIL, SMS
ChannelSender           Channel channel(); boolean send(userId, body)
Notification (record)   eventId, userId, template, params
Preferences             userId -> ordered List<Channel>
NotificationService     render, choose channels, retry, fall back, deduplicate
```

Each channel is a `ChannelSender` implementation. Adding WhatsApp is one new class and one enum constant. The service never contains `if (channel == SMS)`.

### Preferences, retries, and fallback

A user's preferences are an ordered list: try push, then email. The service tries each channel a bounded number of times before falling back to the next one. The companion's scenario gives push a provider that fails five times and email one that fails once, with two attempts per channel:

```text
push fails 5x, email fails 1x, 2 attempts per channel -> sent via EMAIL after 4 attempts
```

Two push attempts, then two email attempts, the second of which succeeds. Real systems add per-channel rules: security codes go only by SMS, marketing respects quiet hours, and some notifications must never fall back to a more intrusive channel. Each rule is a policy that filters or reorders the channel list before sending.

### Idempotency: the same event twice

Upstream systems deliver events at least once. An "order shipped" event that is redelivered must not produce a second email. The service keys each delivery by `(eventId, userId)` and returns the recorded result for a key it has already delivered:

```text
same event delivered again -> duplicate suppressed, email outbox size 1
```

Two subtleties an interviewer may probe:

- **The key excludes the channel.** If the key included the channel, a redelivered event whose push attempt now succeeds would notify the user twice - once by email earlier, once by push now.
- **Record-then-send versus send-then-record.** If the service records the key and then crashes before sending, the notification is lost. If it sends and crashes before recording, a retry sends it twice. With external providers there is no way to make both atomic; you choose at-least-once (send first, occasional duplicates) or at-most-once (record first, occasional loss) per notification type. Password resets choose at-least-once; marketing chooses at-most-once.

### Templates fail early

Rendering fills template variables from parameters. A template with a variable that has no value must fail before anything is sent - "Hi {name}" reaching a customer is a visible defect. The companion's renderer throws on any unfilled template variable, and the check runs before the first channel is tried.

### From one process to many

Both designs change in predictable ways when scaled out:

- The ready queue and job states move to durable storage, and "claim a ready job" becomes an atomic update with a lease: set `RUNNING` with an owner and expiry only if the job is still `READY`. An expired lease returns the job to `READY`. This is check-then-act again, now in a database.
- The idempotency map becomes a table with a unique constraint on the key, or a key-value store with set-if-absent and a TTL.
- Delivery moves behind a queue per channel, so a slow SMS provider cannot block email.

## Failure modes and common mistakes

- **Plain FIFO for ready jobs.** Non-deterministic order across runs; priority ignored.
- **Priority that overrides dependencies.** A high-priority job must still wait for its dependencies.
- **Infinite wait on a cycle.** Detect cycles at submission; name the stuck jobs.
- **Cancelling one job, orphaning its dependents.** Cancel the transitive closure.
- **Immediate, unbounded retries.** Use capped exponential backoff with jitter and a dead-letter destination.
- **Retrying non-retryable failures.** Classify errors first.
- **Idempotency key that includes the channel.** Fallback then causes duplicates.
- **`if (channel == ...)` in the service.** Channels are strategies.

## Interview questions and model answers

**Q: How do you order jobs with dependencies and priorities?**
A: Kahn's algorithm, with ready jobs held in a priority queue ordered by priority and then id. Priority only chooses among jobs whose dependencies have all succeeded; it never lets a job skip a dependency. The id tie-break makes the order deterministic, which makes the scheduler testable.

**Q: What happens if someone submits a cycle?**
A: I reject it at submission. Kahn's algorithm finishes with unprocessed jobs when there is a cycle, and I report the stuck set, which includes the cycle and everything downstream of it. I would also extract the cycle with a DFS so that the error says which edge to remove.

**Q: A job fails. What does the scheduler do?**
A: If the error is retryable, it moves the job from `RUNNING` back to `READY` with the next backoff delay - exponential, capped, with jitter. After the last attempt it moves the job to `FAILED`, sends it to a dead-letter destination, and cancels its transitive dependents, which can no longer run.

**Q: How do you avoid sending the same notification twice?**
A: Every notification carries the event id. I key deliveries by event and user - not by channel, or fallback would cause duplicates - and a repeated key returns the earlier result. Across processes that key lives in a table with a unique constraint. Whether I record before or after sending depends on whether a duplicate or a loss is worse for that notification type.

**Q: Push is down. What happens?**
A: The user's preferences give an ordered channel list. After a bounded number of push attempts the service falls back to the next channel, subject to rules such as "security codes only by SMS". In a real deployment push failures would also trip a circuit breaker, so we stop trying push for everyone while it is down.

## Exercises

1. Extract and print the actual cycle, not just the stuck set, using DFS with an on-stack marker.
2. Add `RUNNING -> CANCELLED` with cooperative cancellation. What does a job's code have to do to honour it?
3. Add full jitter to the backoff and write a test that remains deterministic by injecting the random source.
4. Add quiet hours to the notification service as a policy that defers non-urgent notifications.

## Chapter summary

Job schedulers and notification services are lifecycle problems. Order jobs with Kahn's algorithm and a priority queue, reject cycles at submission, and name the stuck jobs. Keep the lifecycle in a transition table, cascade cancellation to transitive dependents, and retry with capped, jittered exponential backoff only for retryable errors. Treat channels as strategies, preferences as an ordered fallback list, and deduplicate deliveries by event and user.

## Revision checklist

- [ ] I can write Kahn's algorithm with a priority queue and explain what priority can and cannot do.
- [ ] I can explain why `report` is stuck by a cycle it is not part of.
- [ ] I can write the job transition table and defend the `RUNNING -> CANCELLED` decision.
- [ ] I can produce a capped backoff schedule and explain why jitter matters.
- [ ] I can explain why the notification idempotency key excludes the channel.
