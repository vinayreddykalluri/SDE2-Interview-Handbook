# 7. Extension Rounds, Follow-Ups, and Question Bank

## Learning objectives

By the end of this chapter, you should be able to:

- predict the extension an interviewer is likely to add to each common problem and design the seam in advance;
- execute an extension under time pressure by touching the fewest types;
- handle the concurrency, persistence, and scale follow-ups that close most rounds;
- apply the method from chapter 1 to two further common prompts, a library system and a board game; and
- score your own mock attempts against the rubric.

## Why this matters at SDE-2

The last 15 to 25 minutes decide most machine-coding rounds. By then almost every candidate has something running. The extension is where a well-factored design and a lucky one separate: one candidate adds a class and changes a constructor argument, the other edits five files and breaks the demo. The follow-up questions that come after - "what if two threads...", "where would this be stored...", "what changes at 100 times the load..." - are where SDE-2 judgement is scored.

## First-principles model

An extension tests one thing: **did you put the seam where the change is?** A seam is a place where behaviour can be replaced without editing the code around it - an interface, a strategy, a table of transitions, an injected clock.

Seams are not free. Every interface is indirection a reader must follow. So the skill is not "add seams everywhere" but "add seams exactly where change is predictable". The good news is that change *is* predictable in this round: interviewers draw extensions from a small, well-known set for each problem.

## Core terminology

- **Seam:** a point where behaviour can vary without editing surrounding code.
- **Blast radius:** the number of types a change touches.
- **Open/closed principle:** extend behaviour by adding code, not by modifying working code.
- **Follow-up:** a question after the implementation about concurrency, persistence, scale, or failure.
- **Read model:** a derived view of data optimised for queries, rebuilt from the source of truth.

## Detailed mechanics

### The extension catalogue

For each problem in this book, the extensions interviewers use most, and the seam that absorbs each one:

| Problem | Likely extension | Seam that should already exist |
|---|---|---|
| Parking lot | EV spots, reservations, dynamic pricing | `AllocationPolicy`, `PricingPolicy`, compatibility table |
| Vending machine | card payment, maintenance mode | explicit states, payment as a strategy |
| Elevator | multiple cars, express floors, maintenance | `DispatchPolicy`, transition table |
| LRU / LFU cache | TTL, thread safety, eviction listener | injected clock, eviction hook |
| Rate limiter | per-user tiers, distributed limits | `RateLimiter` interface, limiter per key |
| Split expenses | shares split, currencies, edit history | `SplitStrategy`, `Money` value, derived balances |
| Job scheduler | cron schedules, cancellation, priorities | transition table, ready queue comparator |
| Notification service | new channel, quiet hours, digest | `ChannelSender`, preference policy |

Read this table before every mock. When you model the domain in step 2 of the method, check each likely extension against your types and ask: how many would this touch?

### Executing an extension in five minutes

1. **Restate the requirement and its edge cases** in one sentence each. "EVs prefer EV spots; other cars may use them only when no compact spot is free."
2. **Name the seam** before typing. "This is an allocation policy plus a compatibility change."
3. **Write the new type**, not edits to old ones, wherever possible.
4. **Wire it** - usually one constructor argument in `main`.
5. **Demo it** with one new check that would have failed before.

If the extension genuinely needs a change to an existing type, say so, and say why the seam was not there: "I modelled compatibility as an order of sizes; EV spots are not larger or smaller, they are different, so compatibility becomes a table." That sentence scores better than pretending the change was anticipated.

### The follow-up questions that close the round

Almost every round ends with some of these. Prepare one crisp answer for each against every problem you practise.

**Concurrency.** "Two requests hit this at the same time." Name the invariant, name the object whose lock or atomic operation protects it, and point to the check-then-act you made atomic. For caches, remember that access-ordered reads are writes.

**Persistence.** "Where does this live when the process restarts?" Entities become rows; the source of truth is usually an append-only log of events (expenses, tickets, job transitions) with derived read models (balances, free-spot counts). The in-memory lock becomes a database constraint, a conditional update, or a lease.

**Scale.** "One hundred times the load." Identify the partition key that keeps each invariant local - lot floor, expense group, rate-limit key, job id - and shard by it. Invariants that span partitions are the expensive ones; name them.

**Failure.** "The payment provider or channel is down." Bounded retries with backoff, a fallback, a circuit breaker, and idempotency so that retries are safe.

**Testing.** "How do you know it works?" Boundary tables for pricing and time, a reference model for data structures, a concurrent test with count and distinctness checks, and invariant assertions after every operation.

### Worked prompt: library management system

Scope in writing: search the catalogue; borrow and return copies; place holds; charge late fees. Out of scope: payments, recommendations, inter-library loans.

```text
Book (entity)       isbn, title, authors          - the work
Copy (entity)       copyId, isbn, state           - a physical item: AVAILABLE, ON_LOAN, ON_HOLD_SHELF, LOST
Member (entity)     memberId, tier, loans, holds
Loan (entity)       copy, member, dueDate, returnedAt
Hold (entity)       isbn, member, placedAt        - queued per isbn, FIFO
LoanPolicy          maxLoans(tier), loanDays(tier)
FinePolicy          long fineCents(Loan, returnedAt)
```

The distinction the interviewer is looking for is **book versus copy**: a hold is placed on a *title*, a loan is of a *copy*. Candidates who merge them cannot answer "three copies, five holds - who gets the returned copy?" The answer: on return, if the title's hold queue is non-empty, the copy goes to `ON_HOLD_SHELF` for the first member in the queue with a pickup deadline; otherwise it becomes `AVAILABLE`.

Invariants: a copy has at most one open loan; a member's open loans never exceed `maxLoans(tier)`; fines use integer cents and a cap. Concurrency: borrowing the last available copy is check-then-act on the copy's state - a conditional update from `AVAILABLE` to `ON_LOAN`.

### Worked prompt: board game (snakes and ladders, tic-tac-toe)

These prompts test separation of *rules* from *state* from *players*.

```text
Board (value)        cells, jumps: Map<Integer, Integer>   (snakes and ladders are both jumps)
GameState (entity)   positions, currentPlayer, status
Player               id, and a MoveStrategy (human input, bot, scripted for tests)
Dice                 an interface - a seeded implementation makes games reproducible
Rules                applyMove(state, roll) -> new state; winner(state)
```

Two points win the round. First, **snakes and ladders are the same thing** - a jump from one cell to another - so one map models both, and a validator rejects a jump chain that loops. Second, **inject randomness**: a `Dice` interface with a seeded implementation turns a game into a deterministic test. For tic-tac-toe on an N x N board, check the winner incrementally from the last move - its row, column, and two diagonals - in O(N) instead of scanning the board, and say so.

### Scoring your own mock

After every timed attempt, score yourself on the five rubric dimensions from chapter 1, 0 to 2 each:

```text
                                   0                     1                        2
working critical path      nothing ran           ran, not checked         ran with checks
separation of concerns     one big class         some policies            entities/values/policies distinct
extensibility              extension = rewrite   touched 3+ types         one new type + wiring
concurrency                not considered        mentioned                owner named, check-then-act fixed
communication              silent                answered when asked      drove scope and trade-offs
```

Eight or more out of ten is a strong SDE-2 attempt. Keep the score sheets; the dimension that stays lowest across attempts is what to practise next.

## Question bank

Practise each prompt against the full method, timed at 90 minutes, and score it.

**Allocation and pricing**

1. Parking lot with floors, EV spots, and reservations.
2. Movie ticket booking with seat holds that expire after 10 minutes.
3. Meeting room scheduler: book, cancel, and find a free room for a time range.
4. Hotel booking with room types and overbooking limits.

**State machines**

5. Vending machine with coin inventory and card payment.
6. Elevator system with three cars and a dispatcher.
7. Traffic light controller with pedestrian requests and an emergency override.
8. ATM with cash dispensing by denomination and daily limits.

**Data structure plus policy**

9. LRU cache with TTL and an eviction listener.
10. LFU cache with O(1) operations.
11. Rate limiter with per-user tiers.
12. In-memory key-value store with transactions: `BEGIN`, `SET`, `ROLLBACK`, `COMMIT`.

**Domain invariants**

13. Split expenses with equal, exact, percentage, and shares splits.
14. Digital wallet with transfers, idempotency, and statement generation.
15. Library management with holds and fines.
16. Inventory management with reservations and stock that must never go negative.

**Lifecycle and scheduling**

17. Job scheduler with dependencies, retries, and cancellation.
18. Notification service with preferences, fallback, and deduplication.
19. Logger library with levels, multiple sinks, and asynchronous writing.
20. Pub/sub message broker with topics, consumer groups, and offsets.

**Games**

21. Tic-tac-toe on an N x N board with an O(N) winner check.
22. Snakes and ladders with a seeded dice and loop validation.
23. Chess move validation for a subset of pieces.

## Failure modes and common mistakes

- **Treating extensions as surprises.** They come from a known list; design for it.
- **Editing working code first.** Add a type; wire it; only then refactor if needed.
- **Hand-waving follow-ups.** "We would scale it" is not an answer; name the partition key.
- **Merging book and copy, or rules and state.** Most prompts have one distinction like this that decides the round.
- **Not scoring mocks.** Without a score, practice repeats strengths instead of fixing weaknesses.

## Interview questions and model answers

**Q: You have ten minutes left and I want EV charging spots. Go.**
A: EV spots are a new kind of spot, not a bigger one, so compatibility changes from an order to a table: EVs accept EV spots first and compact spots second; other cars accept EV spots only when an overflow flag is set. That is a change to `VehicleType.fits` and a new allocation policy that prefers EV spots for EVs. The lot, ticket, and pricing are untouched. I'll write the policy and add one check that an EV takes the EV spot.

**Q: How would you persist the split-expenses ledger?**
A: Expenses are the source of truth, stored append-only with an id. Balances are a read model derived from them and cached per group. Adding an expense and updating the cached balances happen in one transaction; if they ever disagree, balances are rebuilt from expenses. Settlements are stored with an idempotency key under a unique constraint.

**Q: The job scheduler now runs on five machines. What changes?**
A: Job state moves to a database. A worker claims a job with a conditional update from `READY` to `RUNNING` that sets an owner and a lease expiry; only one update can succeed. A job whose lease expires returns to `READY`. Because a job may then run twice, job side effects must be idempotent.

**Q: What is the one mistake you see most often in this round?**
A: Nothing running at the end. The fix is procedural: a written scope in the first eight minutes, and the critical path executing by minute 45 even if it is crude.

## Exercises

1. Pick three prompts from the question bank you have never attempted. Do each in 90 minutes and score it.
2. For the in-memory key-value store with transactions (prompt 12), design how nested transactions roll back.
3. For the movie booking prompt, write the seat-hold state machine and the concurrent test for two users selecting the same seat.

## Chapter summary

The extension round tests whether your seams are where change happens, and change in this round is predictable: use the extension catalogue. Execute extensions by adding types and wiring them, and say plainly when a seam was missing. Prepare crisp concurrency, persistence, scale, failure, and testing answers for every problem. Practise from the question bank against a timer, and score every attempt against the rubric so that practice targets your weakest dimension.

## Revision checklist

- [ ] For each problem in this book, I can name its likely extensions and the seam for each.
- [ ] I can execute an extension in five steps and demo it.
- [ ] I have one crisp answer each for concurrency, persistence, scale, failure, and testing.
- [ ] I can explain book versus copy, and rules versus state.
- [ ] I score every mock attempt out of ten.
