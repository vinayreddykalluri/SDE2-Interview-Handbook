# Low-Level Design and Machine-Coding Drills: Worked Solutions

These are defensible answers, not the only ones. Every number below was computed by a program rather than by hand. Where a solution depends on a business rule the drill does not state, it says which rule it assumed.

## Foundation

### D1. Price the boundaries

Started hours for a positive number of minutes `m` is `(m + 59) / 60` in integer arithmetic.

```text
   10 min ->  $0.00   grace is inclusive
   11 min ->  $4.00   first started hour
   60 min ->  $4.00   exactly one hour
   61 min ->  $6.50   4.00 + 1 x 2.50
  180 min ->  $9.00   4.00 + 2 x 2.50
  600 min -> $25.00   10 hours would be $26.50; capped
1,440 min -> $25.00   exactly one day
1,441 min -> $29.00   one day ($25.00) plus a started hour ($4.00)
2,000 min -> $50.00   one day plus 560 minutes: 10 started hours, $26.50, capped at $25.00
```

**Rule assumed:** the cap applies per 24-hour period and the first-hour rate restarts each day. If the operator wants day two billed at $2.50 per hour from the start, that is a one-line change to the policy - and a question worth asking.

### D2. Allocation changes who is rejected

```text
first fit:  car -> L-1   motorcycle -> C-1   truck -> REJECTED   car -> REJECTED
best fit:   car -> C-1   motorcycle -> S-1   truck -> L-1        car -> REJECTED
```

First fit wastes both larger spots on smaller vehicles and rejects the truck and the second car. Best fit admits three of four. The last car is rejected under both, because only three spots exist. Best fit can still reject a car when only large spots are held back for trucks that never come - which is why the policy is behind an interface.

### D3. Name the concurrency owner

The bug:

```java
Seat seat = show.seat("F7");
if (seat.state() == SeatState.AVAILABLE) {        // both users see AVAILABLE
    seat.hold(userId, now.plusMinutes(10));       // both succeed
}
```

The fix makes the check and the change one atomic step, owned by the **show**, because the invariant "a seat has at most one active hold or booking" is per show. In memory, a `synchronized` `hold(seatId, user)` on `Show`, or a `compareAndSet` on the seat's state. In a database, `UPDATE seat SET state = 'HELD', holder = ? WHERE show_id = ? AND seat_id = ? AND state = 'AVAILABLE'`, then check the updated row count is 1.

The test: N threads all try to hold F7 at once. Assert exactly one success, exactly N - 1 failures, and that the seat's holder is the successful user.

## State machines

### D4. Break greedy change

Greedy takes the 25c coin, still owes 15c, takes one 10c coin, owes 5c, and has no 5c coin: **it fails**. Bounded change-making returns **four 10c coins**. The check runs as the last guard in `select`, before stock is decremented or anything is dispensed; if it fails, the machine refuses with "exact change only".

### D5. Find the money bug

The missing step is **resetting `credit` to zero**. Invariant: `inserted == taken + returned + credit`. After the sale, `credit` still holds the old amount, so the left side is smaller than the right by the price plus change and the check fails at once. The symptom: the next customer finds credit already on the machine and gets a free product.

### D6. Schedule an elevator

```text
FCFS                     10 -> 12 -> 3 -> 15 -> 8       2 + 9 + 12 + 7 = 30 floors
LOOK starting up         visits [12, 15, 8, 3]         5 + 12           = 17 floors
LOOK starting down       visits [8, 3, 12, 15]         7 + 12           = 19 floors
optimal (closed form)    min((10-3)+(15-3), (15-10)+(15-3)) = min(19, 17) = 17
```

The nearer extreme is 15 (five floors away) rather than 3 (seven away), so starting up is optimal. LOOK in the better direction matches the optimum, as the companion shows over 2,000 random batches.

## Data structure plus policy

### D7. Trace LRU and LFU

```text
op        LRU                         LFU (count; ties by least recent)
put a     -                           -
put b     -                           -
put c     -                           -
get a     hit                         hit (a=2)
put d     evicts b                    evicts b (b=1, older than c=1)
get b     miss                        miss
get c     hit                         hit (c=2)
get c     hit                         hit (c=3)
put e     evicts a                    evicts d (d=1 is the only count-1 key)
get a     miss                        hit (a=3)
get d     hit                         miss

final     {c, d, e}                   {a, c, e}
```

The two policies diverge at `put e`. LRU evicts `a`, last used four operations earlier. LFU evicts `d`, used once, and keeps `a`, used twice already.

### D8. Compare two limiters

```text
t (s):          0   10  20  50  55  59  60  61  65  90  119  120
fixed window    Y   Y   Y   Y   Y   N   Y   Y   Y   Y   Y    Y     total 11
sliding log     Y   Y   Y   Y   Y   N   Y   N   N   Y   Y    Y     total 9
```

The fixed window resets at t = 60, so 60, 61, 65, 90, and 119 all pass in the second window. The sliding log at t = 61 still counts 10, 20, 50, 55, and 60 - five within the last 60 seconds - and rejects it. Between t = 50 and t = 65 the fixed window admits 5 requests in 15 seconds (50, 55, 60, 61, 65); the sliding log admits 3 (50, 55, 60).

### D9. Make a cache thread-safe

In an access-ordered LRU, `get` moves the entry to the most-recent position, so it modifies the list. Two readers holding a shared read lock would relink nodes concurrently and corrupt the structure.

Two designs that allow concurrent reads:

- **Sharding.** N independent LRU caches chosen by key hash, each with its own lock. Reads on different shards run in parallel. Cost: eviction is LRU per shard, not global, and hot keys still contend on their shard.
- **Buffered recency.** Reads look up the value in a concurrent map and append the access to a lock-free buffer; a single thread drains the buffer and reorders the list in batches (the Caffeine design). Cost: eviction order lags reads slightly, and the implementation is substantially more complex.

## Domain invariants

### D10. Split a bill by percentage

```text
exact:   9,999 x 3,333 / 10,000 = 3,332.6667 cents   (twice)
         9,999 x 3,334 / 10,000 = 3,333.6666 cents
floors:  3,332 + 3,332 + 3,333 = 9,997   -> 2 cents left over
largest remainders: .6667, .6667, .6666 -> the first two get one cent each
result:  3,333 / 3,333 / 3,333 = 9,999   (each pays $33.33)
```

Everyone pays $33.33. The largest percentage produced the *smallest* fractional remainder, so the two leftover cents went to the others. That is the method working as designed: it minimises the total rounding error, not the error for the largest share. If the product must guarantee that a larger percentage never pays less, that is a different rule - and it should be written down.

### D11. Settle a group

Greedy, in dollars:

```text
S -> P  30     (P 40 is the largest creditor, S -30 the largest debtor)
R -> Q  20     (Q 30; R and T tie at -20, alphabetical picks R)
T -> P  10
T -> Q  10
4 transfers
```

The minimum is **3**: {Q, S} is a zero-sum group (+30, -30) needing one transfer, and {P, R, T} (+40, -20, -20) needs two - `S -> Q 30`, `R -> P 20`, `T -> P 20`. Five non-zero balances in two zero-sum groups need 5 - 2 = 3 transfers.

## Lifecycle and scheduling

### D12. Order a build pipeline

```text
order: docs, build, lint, test, package, deploy, notify
```

Initially ready: `docs` [3], `build` [2], `lint` [1]. `docs` runs first by priority. After `build`, `test` becomes ready with priority 1 - tied with `lint`, so the name decides: `lint`, then `test`. `notify` has the highest priority of all and still runs last, because it waits for `deploy`.

Cancelling `test` cancels `{deploy, notify, package, test}`. `build`, `lint`, and `docs` are unaffected.

```text
backoff (base 250 ms, cap 4 s), six attempts: 250, 500, 1000, 2000, 4000, 4000   (11.75 s in total)
```

In production, add jitter to each wait.

### D13. Deduplicate notifications

- **Key (event, user, channel):** the first delivery records `(shipped, u1, EMAIL)`. The second delivery tries push first; `(shipped, u1, PUSH)` is a new key and push now works, so the user gets **two** notifications.
- **Key (event, user):** the second delivery finds `(shipped, u1)` already delivered and suppresses it; the user gets **one**.

Use (event, user). The channel is *how* the notification was delivered, not *which* notification it was.

## Challenge

### D14. Full mock: seat booking with holds

**Scope.** In: list available seats for a show, hold up to N seats for 10 minutes, confirm a hold into a booking, and release expired holds. Out: payments (confirmation assumes payment succeeded), pricing tiers, and multiple cinemas. Say where each would plug in.

**Model.**

```text
Show (entity)          showId, seats: Map<SeatId, Seat>   - owns the per-show lock
Seat (entity)          seatId, state, holdId, holdExpiresAt
SeatState (enum)       AVAILABLE, HELD, BOOKED
Hold (entity)          holdId, userId, seatIds, expiresAt
Booking (entity)       bookingId, holdId, userId, seatIds
Clock                  injected LongSupplier for expiry tests
```

**Seat state machine.**

```text
AVAILABLE -> HELD      hold(), all requested seats available (all or nothing)
HELD      -> BOOKED    confirm() by the holder, before expiry
HELD      -> AVAILABLE expiry or explicit release
BOOKED    -> (terminal for this design; cancellation would be an extension)
```

**Critical decisions to say aloud.**

- Holding several seats is **all or nothing**: check every seat and then mark every seat inside one lock. A partial hold leaves the user with seats they did not want.
- **Expiry is lazy plus swept.** A `HELD` seat past its deadline counts as available on read, and a periodic sweep releases stale holds so that listings are accurate.
- `confirm` checks the hold's owner and expiry inside the same lock, so a hold that expires during confirmation cannot also be re-held by someone else.
- A hold id is returned to the client and makes `confirm` idempotent: confirming the same hold twice returns the same booking.

**Concurrent test.** Twenty threads call `hold(show, [F7, F8])` at once. Assert exactly one success, nineteen failures, and that F7 and F8 carry the successful hold id. A second test advances the injected clock by 10 minutes and asserts that the seats are holdable again.

**Rubric self-score.** Score honestly out of ten against chapter 7. Typical first attempts lose points on all-or-nothing holds, which are often implemented seat by seat, and on the expiry race in `confirm`.
