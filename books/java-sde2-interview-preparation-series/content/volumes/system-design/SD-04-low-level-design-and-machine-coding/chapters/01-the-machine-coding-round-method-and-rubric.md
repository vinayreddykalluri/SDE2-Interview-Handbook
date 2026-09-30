# 1. The Machine-Coding Round: Method and Rubric

## Learning objectives

By the end of this chapter, you should be able to:

- describe what a machine-coding or low-level design round is actually scoring, and why "it works" is necessary but nowhere near sufficient;
- run a fixed six-step method inside a 60 to 120 minute budget without losing the last 20 minutes to debugging;
- separate entities, value objects, policies, and state so that a new requirement lands in one place;
- decide where concurrency is owned before writing the first class; and
- recognise the three most common ways a strong engineer still fails this round.

## Why this matters at SDE-2

The low-level design round goes by several names - machine coding, object-oriented design, LLD - and it is the round where experienced engineers are most often surprised by the result. They produce code that runs and still get a "no hire" or a down-level to SDE-1.

The reason is that the round is not testing whether you can make a parking lot work. Almost everyone can. It is testing whether the code you produce under time pressure is the code a senior teammate would approve: responsibilities in the right places, rules that can change without a rewrite, invariants that cannot be violated from outside, and a clear answer to "what happens when two requests arrive at once?"

At SDE-1, working code with reasonable names is a pass. At SDE-2, the interviewer expects you to *drive*: to scope the problem yourself, to name the extension points before being asked, and to defend each boundary. A candidate who waits to be told what to build next is showing SDE-1 behaviour regardless of how clean the code is.

## First-principles model

A machine-coding problem is a small domain with three kinds of content, and good designs keep them separate:

**Things with identity that change over time (entities).** A parking spot, a ticket, a job, an elevator car. They have an id, a lifecycle, and state transitions. Two tickets with identical fields are still two tickets.

**Values that mean what they contain (value objects).** A money amount, a time range, a coin, a floor number. They are immutable and compared by content. In Java 21 they are almost always `record`s.

**Rules that are expected to change (policies).** How a spot is chosen, how a stay is priced, which cache entry is evicted, which channel a notification uses. Every problem in this book has at least one rule the interviewer will change in the last 20 minutes. If that rule is an `if` statement buried inside the entity, the change is a rewrite; if it is an interface with one implementation, the change is a new class.

On top of these sits one question that decides whether the design survives contact with reality: **who owns concurrency?** Pick the single object whose lock (or atomic operation) makes each invariant true, and say it out loud. "The lot's `park` method holds the lock across choosing and claiming a spot" is an answer. "We'll make things thread-safe" is not.

> **Specification boundary:** there is no standard for this round. Formats vary by company: some want a runnable program with a `main` demo, some want tests, some accept pseudo-code for the non-critical paths, and some let you use an IDE with autocomplete. Ask in the first minute. Everything in this book assumes the strictest common format - runnable Java 21, no frameworks, one to two hours - because a design that meets that bar also meets the looser ones.

## Core terminology

- **Machine coding:** a timed round where you design *and implement* a small system that must run, usually with a demo driver or tests.
- **Entity:** an object with identity and a lifecycle; equality by id.
- **Value object:** an immutable object compared by content; a `record` in modern Java.
- **Policy (strategy):** an interface isolating a rule that is expected to vary.
- **Invariant:** a condition that must hold after every public operation, such as "no spot is assigned to two vehicles".
- **State machine:** an explicit set of states and permitted transitions; illegal transitions are rejected, not silently ignored.
- **Extension round:** the part of the interview, usually the last 15 to 25 minutes, where a new requirement is added to your working code.
- **Critical path:** the one flow that must work end to end, such as park then exit then pay.

## Detailed mechanics

### What the rubric actually scores

Interviewers rarely share their rubric, but they converge on the same five dimensions. Ordered by how often each one decides the outcome:

| Dimension | What a pass looks like | What a fail looks like |
|---|---|---|
| Working critical path | The main flow runs end to end in a demo or test | Code that "would work" but was never executed |
| Separation of concerns | Entities, values, and policies in distinct types | One `ParkingLotService` with 600 lines and a `switch` |
| Extensibility | The new requirement lands in one new class | The new requirement touches five files |
| Correctness under concurrency | The lock owner is named and the check-then-act is atomic | `if (spot.isFree()) spot.occupy()` from two threads |
| Communication | Scope, trade-offs, and shortcuts are stated aloud | Silent typing for 40 minutes |

Two observations follow. First, "working" is the *entry ticket*, not the score. Second, three of the five dimensions are visible only if you narrate them. An interviewer cannot credit a trade-off you made silently.

### The six-step method

Use the same method every time. The point of a fixed method is that under pressure you stop deciding *what to do next* and spend all of your attention on the problem.

```text
step                           budget (of 90 min)   artifact
1. clarify and scope           5-8 min              written list of in-scope use cases and non-goals
2. model the domain            8-10 min             entities, values, policies, states - names only
3. define the interfaces       5 min                method signatures for the critical path
4. implement the critical path 30-35 min            running code, demo in main or a test
5. harden                      10-15 min            invariants, edge cases, concurrency owner
6. extend and discuss          15-20 min            the new requirement, then trade-offs
```

The budget is deliberately back-loaded. Candidates who spend 25 minutes on class diagrams run out of time to make anything run; candidates who start typing in minute two build the wrong thing and restructure in minute 50. The first 20 minutes produce no code, and that is correct.

**Step 1: clarify and scope.** Write down, in the shared editor, three to six use cases you will implement and two or three you explicitly will not. For a parking lot: *park a vehicle, exit and pay, show free spots by type* - in scope; *reservations, multiple entrances with separate displays, payment gateway integration* - out of scope, with a sentence on where each would plug in. This list is your contract with the interviewer and your defence when the clock runs out.

**Step 2: model the domain.** Nouns become candidate types; verbs become candidate methods. Then prune: a noun that has no behaviour and no identity is a value or a field, not a class. Mark which rules are likely to change - those become interfaces.

**Step 3: define the interfaces.** Write the public signatures for the critical path before any bodies. `Optional<Ticket> park(String plate, VehicleType type)` and `long exit(long ticketId)` tell the interviewer, and you, what "done" means.

**Step 4: implement the critical path.** One flow, end to end, runnable. Resist implementing the second use case until the first one prints the right answer. A stubbed pricing policy that returns a constant is fine at this stage; a lot that cannot park anything is not.

**Step 5: harden.** Add the invariant checks, the illegal-transition rejections, and the lock. This is where most of the SDE-2 signal is earned, because it is where you show what can go wrong.

**Step 6: extend and discuss.** The interviewer adds a requirement. If your policies are interfaces, you write a class and change one constructor argument. Then discuss what you would change at 100 times the scale.

### Modeling toolkit in Java 21

Java 21 gives you exactly the tools this round rewards, and using them is itself a signal:

```java
enum VehicleType {
    MOTORCYCLE(SpotSize.SMALL), CAR(SpotSize.COMPACT), TRUCK(SpotSize.LARGE);

    final SpotSize minimumSpot;

    VehicleType(SpotSize minimumSpot) {
        this.minimumSpot = minimumSpot;
    }

    boolean fits(SpotSize size) {
        return size.ordinal() >= minimumSpot.ordinal();
    }
}

record Spot(String id, int level, SpotSize size) { }

interface AllocationPolicy {
    Optional<Spot> choose(List<Spot> freeSpots, VehicleType vehicle);
}
```

- **`enum` with behaviour** for closed sets whose members carry rules (`VehicleType.fits`). Adding a vehicle type is then one line plus its rule, and the compiler finds every `switch` that needs updating.
- **`record`** for values. Equality, hashing, and immutability come free, and a reviewer immediately knows the type has no lifecycle.
- **`sealed interface` with `record` implementations** for closed sets of *shapes*, such as commands or events, where pattern-matching `switch` gives exhaustiveness checking.
- **Plain interfaces** for policies. One method is usually enough.
- **`Optional`** as the return type of operations that can legitimately find nothing (`park` when the lot is full). Exceptions are for broken contracts, such as exiting with an unknown ticket.

### Where concurrency is owned

Every problem in this book has a check-then-act sequence that is wrong under concurrency: check that a spot is free, then claim it; check the balance, then deduct; check the cache, then load. The fix is always the same shape - make the check and the act one atomic step - but *which object provides the atomicity* is a design decision you must make and state.

Three defensible options, in increasing order of complexity:

1. **One coarse lock on the aggregate.** `synchronized` on `ParkingLot.park` and `exit`. Correct, easy to reason about, and fast enough for any interview-scale workload. Start here.
2. **Per-partition locks.** A lock per floor or per spot size, when the coarse lock is shown to be contended. Requires that every invariant lives inside one partition.
3. **Lock-free claim.** A `ConcurrentLinkedQueue` of free spots, or `compareAndSet` on a spot's state. Fastest, but every multi-step invariant now needs a separate argument.

The companion model runs 8 threads making 8,000 park attempts against 500 spots with option 1, and checks the result: exactly 500 successes, 500 distinct spots, 0 free. A version that checked `isFree()` and then claimed outside the lock can assign one spot twice; the point of the test is that the correct version *cannot*.

### Anti-patterns that cost the round

**Pattern stuffing.** Naming five design patterns in the first ten minutes, then building a `VehicleFactoryFactory`. A pattern is justified by a rule that varies, not by the problem having a noun. The parking lot needs one strategy for allocation and one for pricing; it does not need an abstract factory, a visitor, and an observer.

**The god service.** One `XService` class that owns all state and all rules. It usually starts as a reasonable shortcut and becomes impossible to extend in step 6.

**Anemic entities with a smart service.** Entities that are only getters and setters, and a service that reaches into them to enforce every rule. The invariant "a ticket can be closed once" belongs to whatever owns tickets, not to every caller.

**Premature generality.** An interface for every class "in case". An interface with one implementation is justified only when you can name the second implementation the interviewer is likely to ask for.

**Silent shortcuts.** Using `double` for money because it is faster to type, without saying so. The shortcut itself is sometimes fine; failing to name it reads as not knowing.

## Failure modes and common mistakes

- **Running out of time with nothing running.** The most common failure. Enforce the step budget and get the critical path executing by minute 45 even if it is ugly.
- **Asking no clarifying questions.** The prompt is deliberately underspecified. Building the wrong scope is scored as a judgement failure, not bad luck.
- **Over-clarifying.** Ten minutes of questions with no written scope. Clarify, write it down, move on.
- **Untested demo.** Printing "parked" without checking which spot was taken. Assert the result, even if only with a `check(condition, message)` helper.
- **Treating the extension as a surprise.** It is not. For every problem in this book, the likely extensions are listed; design the seam before being asked.
- **Defending a bad decision.** When the interviewer points at a flaw, the strongest response is to agree, state the fix and its cost, and decide whether to apply it now or note it.

## Interview questions and model answers

**Q: "Design a parking lot." What is your first move?**
A: Scope it in writing. I would propose: park, exit with pricing, and free-spot counts by size are in scope; reservations, payment integration, and multiple entry displays are out, and I will say where each would plug in. I would also ask whether they want a runnable program or tests, and how many entrances there are, because concurrent entry changes the design.

**Q: Why an interface for pricing when there is only one pricing rule?**
A: Because pricing is the rule most likely to change - weekend rates, event pricing, per-vehicle-type rates - and I can name the second implementation now. Allocation is the same. I would not add an interface for `Ticket`, because there is no second kind of ticket I can name.

**Q: Where does thread safety live in your design?**
A: In the aggregate that owns the invariant. The lot's `park` method holds its lock across choosing and removing the spot from the free list, so two entrances can never receive the same spot. I start with one coarse lock because it is correct and simple, and would partition it by floor only if measurements showed contention.

**Q: You used `double` for price. Is that a problem?**
A: Yes, and I should have said so. Binary floating point cannot represent most decimal cents, so sums drift - 100 divided by 3 rounded to cents is 33.33, and three of those make 99.99. I would store money as `long` cents, or `BigDecimal` with an explicit scale and rounding mode if fractional cents are meaningful.

**Q: How do you know your design is extensible?**
A: I test it against the extensions I expect. For each likely requirement I check how many types it touches. If a new pricing rule needs one new class and a constructor change, the seam is right. If it needs edits in the lot, the ticket, and the display, it is not.

## Exercises

1. Take any problem from chapters 2 to 6 and write only steps 1 to 3 - scope, domain model, and signatures - in 15 minutes. Compare with the chapter.
2. List three rules in a vending machine that are likely to change. For each, write the interface you would introduce and name its second implementation.
3. Write a check-then-act bug for a seat-booking system, then fix it in two different ways, and state which object owns concurrency in each fix.

## Chapter summary

The machine-coding round scores working code as the entry requirement and then grades separation of concerns, extensibility, concurrency, and communication. A fixed six-step method with a back-loaded time budget prevents the two classic failures: nothing running, and the wrong thing running. Model entities, values, and policies as separate kinds of type; name the concurrency owner before writing code; and treat the extension round as predictable rather than as a surprise.

## Revision checklist

- [ ] I can list the five scoring dimensions and which three depend on narration.
- [ ] I can run the six-step method with a written scope inside eight minutes.
- [ ] I can classify any noun in a prompt as entity, value, policy, or field.
- [ ] I name the concurrency owner for every invariant before implementing.
- [ ] I state shortcuts aloud, including the ones I would not ship.
