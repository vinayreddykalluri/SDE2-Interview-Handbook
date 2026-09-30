# Low-Level Design and Machine-Coding Drills

Work these in order. Each drill names the artifact to produce, because "think about it" is not practice. Write your own answer first, then compare with the worked solutions - the gap between the two is the lesson.

Money is always in integer cents. Where a drill asks for code, it should compile and run on Java 21 with no dependencies.

## Foundation: method and modeling

**D1. Price the boundaries.**
A lot charges nothing for the first 10 minutes, $4.00 for the first hour, $2.50 for each started hour after that, and at most $25.00 per 24 hours.
*Produce:* the charge for 10, 11, 60, 61, 180, 600, 1,440, 1,441, and 2,000 minutes, and the one-line formula for "started hours".

**D2. Allocation changes who is rejected.**
Spots, in declaration order: `L-1` (large), `C-1` (compact), `S-1` (small). Arrivals in order: car, motorcycle, truck, car.
*Produce:* the spot each arrival receives under first fit and under best fit, and one sentence on which vehicle best fit could still reject.

**D3. Name the concurrency owner.**
A cinema sells seats. Two users select seat F7 at the same moment.
*Produce:* the check-then-act bug in five lines, the fix, the object that owns the lock or atomic operation, and a concurrent test with its three assertions.

## State machines

**D4. Break greedy change.**
A vending machine holds one 25c coin and four 10c coins, and owes 40c change.
*Produce:* what greedy change does, what bounded change-making returns, and where in `select` the check must run.

**D5. Find the money bug.**
A vending machine records `inserted`, `taken`, and `returned`. After a successful sale it decrements stock, adds the price to `taken`, adds the change to `returned`, and sets the state to `IDLE`.
*Produce:* the missing step, the invariant that detects it, and the symptom a customer would see.

**D6. Schedule an elevator.**
The car is at floor 10. Requests known up front: 12, 3, 15, 8.
*Produce:* total travel for FCFS, for LOOK starting up, and for LOOK starting down; the visit order for each LOOK run; and the optimal tour length from the closed-form rule.

## Data structure plus policy

**D7. Trace LRU and LFU.**
Capacity 3. Operations: put a, put b, put c, get a, put d, get b, get c, get c, put e, get a, get d.
*Produce:* for both LRU and LFU (ties broken by least recent use), which key each `put` evicts, whether each `get` hits, and the final contents.

**D8. Compare two limiters.**
Limit: 5 requests per 60 seconds. Requests arrive at t = 0, 10, 20, 50, 55, 59, 60, 61, 65, 90, 119, 120 seconds.
*Produce:* which requests a fixed window (aligned to multiples of 60) admits, which a sliding log admits, and the totals.

**D9. Make a cache thread-safe.**
*Produce:* the reason a `ReadWriteLock` is wrong for an access-ordered LRU, and two designs that do allow concurrent reads, with the cost of each.

## Domain invariants

**D10. Split a bill by percentage.**
$99.99 is split 33.33% / 33.33% / 33.34%.
*Produce:* the exact share of each person in fractional cents, the largest-remainder result, and one sentence on why the person with the largest percentage does not pay the most.

**D11. Settle a group.**
Balances in dollars: P +40, Q +30, R -20, S -30, T -20.
*Produce:* the greedy settlement (largest creditor paid by largest debtor, ties alphabetical), its transfer count, the minimum transfer count, and the zero-sum groups that achieve it.

## Lifecycle and scheduling

**D12. Order a build pipeline.**
Jobs (priority in brackets): `build` [2], `lint` [1], `docs` [3], `test` [1] needs build, `package` [1] needs test and lint, `deploy` [1] needs package, `notify` [4] needs deploy and docs. Higher priority runs first among ready jobs; ties break by name.
*Produce:* the execution order, the set cancelled when `test` is cancelled, and the backoff schedule for six attempts with base 250 ms and cap 4 s.

**D13. Deduplicate notifications.**
An "order shipped" event is delivered twice. On the first delivery, push failed and email succeeded. On the second delivery, push works.
*Produce:* how many notifications the user receives with an idempotency key of (event, user, channel) and with (event, user), and which key you would use.

## Challenge

**D14. Full mock: seat booking with holds.**
Design and implement a cinema booking system in 90 minutes: list available seats for a show, hold seats for 10 minutes, confirm a hold into a booking, and release expired holds.
*Produce:* the written scope, the domain model, the seat state machine, a runnable critical path, the concurrent test for two users holding the same seat, and your rubric score out of ten.
