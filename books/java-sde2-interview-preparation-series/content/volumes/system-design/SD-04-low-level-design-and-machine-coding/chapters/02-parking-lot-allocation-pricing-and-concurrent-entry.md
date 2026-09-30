# 2. Parking Lot: Allocation, Pricing, and Concurrent Entry

## Learning objectives

By the end of this chapter, you should be able to:

- model a parking lot with the compatibility rule on the vehicle, not scattered through `if` statements;
- explain why the choice of allocation policy is a correctness decision, not only an optimisation;
- implement pricing as a policy with a grace period, started-hour rounding, and a daily cap, and test its boundaries;
- make concurrent entry correct by construction and prove it with a test; and
- handle the standard extensions: multiple floors, reservations, electric-vehicle spots, and dynamic pricing.

## Why this matters at SDE-2

The parking lot is the most-asked LLD problem, which is exactly why it discriminates well. Every candidate has seen a solution. The interviewer is therefore not asking whether you can produce classes named `Spot` and `Ticket`; they are watching for whether you see the two places where the problem is genuinely hard - allocation under constraints and concurrent entry - and whether your pricing survives its boundary cases.

## First-principles model

A parking lot is a **constrained resource allocator with a billing side effect**.

- The **resource** is a set of spots, each with a size.
- The **constraint** is compatibility: a vehicle may use a spot of at least its minimum size.
- The **allocator** chooses among compatible free spots - and different choices lead to different future rejections.
- The **billing** is a function of time, computed when the vehicle leaves.
- The **invariant** is that no spot is ever held by two vehicles and no ticket is closed twice.

Everything else - displays, gates, payment methods - is presentation around that core.

## Core terminology

- **Compatibility rule:** which vehicle types may use which spot sizes.
- **Allocation policy:** the rule that chooses one spot among the compatible free spots.
- **First fit:** take the first compatible spot in some fixed order.
- **Best fit:** take the smallest compatible spot, preserving large spots for large vehicles.
- **Grace period:** a short initial duration that is free.
- **Started hour:** billing that rounds any partial hour up.
- **Daily cap:** the maximum charged for one 24-hour period.

## Detailed mechanics

### The domain model

```text
VehicleType (enum)     MOTORCYCLE, CAR, TRUCK - each knows its minimum SpotSize
SpotSize (enum)        SMALL < COMPACT < LARGE
Spot (record)          id, level, size
Ticket (record)        ticketId, plate, spot, entryMinute
AllocationPolicy       Optional<Spot> choose(List<Spot> free, VehicleType v)
PricingPolicy          long priceCents(long minutesParked)
ParkingLot             owns free spots, active tickets, both policies, and the lock
```

Note what is *not* a class: there is no `Vehicle` hierarchy with `Car extends Vehicle`. The only behaviour that varies by vehicle is compatibility, and an `enum` with a field expresses that in three lines. Introduce a class hierarchy only when vehicle types differ in *behaviour* - which in this problem they do not.

The compatibility rule lives on `VehicleType`:

```java
boolean fits(SpotSize size) {
    return size.ordinal() >= minimumSpot.ordinal();
}
```

Relying on `ordinal()` encodes "sizes are totally ordered" in the declaration order of `SpotSize`. That is acceptable and worth one sentence aloud. If the interviewer adds a spot type that is not simply larger - an electric-vehicle spot, a disabled-access spot - the rule stops being an order, and `fits` becomes a lookup table from vehicle to the set of acceptable spot types.

### Allocation is a correctness decision

It is tempting to treat spot selection as an optimisation to discuss later. It is not: the policy changes *which future vehicles are rejected*.

Take a lot with one large spot `L-1` declared first and one compact spot `C-1`. A car arrives, then a truck. The companion model runs both policies:

```text
FirstFit car -> L-1, truck -> REJECTED
BestFit  car -> C-1, truck -> L-1
```

First fit gave the car the only spot a truck could use, and the truck was turned away from a lot with a free spot. Best fit takes the smallest compatible spot, keeping large spots for large vehicles.

Best fit is not universally correct either. If trucks are rare and cars queue at peak, holding large spots empty can reject cars that could have parked. The defensible answer is: best fit as the default, the policy behind an interface, and a note that a real operator would choose based on the arrival mix.

### Pricing as a policy with boundaries

Pricing is where off-by-one errors hide. The companion uses a common real-world rule: 15 minutes free, $3.00 for the first hour, $2.00 for each started hour after that, capped at $20.00 per 24 hours.

```java
record HourlyPricing(long graceMinutes, long firstHourCents, long perHourCents, long dailyCapCents)
        implements PricingPolicy {
    public long priceCents(long minutesParked) {
        if (minutesParked <= graceMinutes) {
            return 0;
        }
        long fullDays = minutesParked / (24 * 60);
        long remainder = minutesParked % (24 * 60);
        long partial = 0;
        if (remainder > 0) {
            long startedHours = (remainder + 59) / 60;
            partial = Math.min(dailyCapCents, firstHourCents + (startedHours - 1) * perHourCents);
        }
        return fullDays * dailyCapCents + partial;
    }
}
```

The boundary table is the test, and every row is checked by the companion:

```text
   10 min -> $0.00     inside the grace period
   15 min -> $0.00     grace is inclusive
   16 min -> $3.00     first started hour
   60 min -> $3.00     exactly one hour
   61 min -> $5.00     one minute into the second hour
  300 min -> $11.00    5 hours: 300 + 4 x 200
  720 min -> $20.00    12 hours would be $25.00; capped
1,440 min -> $20.00    exactly one day
1,500 min -> $23.00    one day plus a started hour ($20 + $3)
```

Three decisions are embedded here, and each should be said aloud:

- **Money is `long` cents.** Never `double`. Chapter 5 shows the drift.
- **`(remainder + 59) / 60`** is ceiling division for positive integers. It avoids `Math.ceil` on a `double`.
- **The cap applies per 24-hour period**, so a second day restarts the first-hour rate. Whether day two should restart at $3.00 or continue at $2.00 per hour is a business rule; state your choice and make it one line to change.

### Tickets and the close-once invariant

`exit(ticketId)` removes the ticket from the active map and returns the spot to the free list in one synchronized step. Exiting with the same ticket twice throws `IllegalArgumentException`. The companion checks both: a 61-minute stay charges $5.00, and the second exit is rejected.

Why an exception rather than returning zero? A second exit is a contract violation - a lost or duplicated ticket, a replayed request - and must be visible. Returning zero would silently hide a fraud path.

### Concurrent entry, correct by construction

With multiple entrance gates, `park` is called concurrently. The bug every interviewer probes for:

```java
Optional<Spot> spot = allocation.choose(free, vehicle);   // thread A and B both see C-7
spot.ifPresent(free::remove);                             // both "claim" it
```

Two threads choose the same spot because the choice and the removal are separate steps. The fix is to make them one step. The companion's `ParkingLot.park` is `synchronized`, so choosing and removing happen under one lock, and the ticket is issued inside the same critical section.

The test that proves it runs 8 threads, each making 1,000 park attempts against 500 compact spots:

```text
8 threads x 1,000 attempts on 500 spots -> 500 parked, 500 distinct spots, 0 free
```

Three independent checks: the number of successful parks equals the number of spots, the set of occupied spot ids has no duplicates, and the free list is empty. Any one of them would catch a double assignment.

**Is one lock too slow?** Choosing a spot is a scan over a list in memory. Even with thousands of spots, the critical section is microseconds, while a physical gate processes a car every few seconds. Say this, and offer the partitioned design - one lock per floor, with a dispatcher that tries floors in order - only as the answer to "what if this were a 50,000-spot stadium lot with 40 gates?"

### Standard extensions

| Extension | Where it lands |
|---|---|
| Multiple floors | `Spot.level` already exists; a `NearestLevelFirst` allocation policy |
| Electric-vehicle charging spots | Compatibility becomes a table; add `SpotType.EV_COMPACT`; EV spots accept EVs first, others only when a flag allows |
| Reservations | A `Reservation` entity holding a spot for a time window; allocation excludes reserved spots; a no-show releases after a grace period |
| Weekend or event pricing | A new `PricingPolicy`, or a `CompositePricing` choosing by time |
| Lost ticket | A fixed penalty policy plus a lookup by plate - which means `plate` must be indexed |
| Display boards | An observer notified on park and exit; counts by `SpotSize` are maintained incrementally |

## Failure modes and common mistakes

- **Vehicle class hierarchy with no behaviour.** `Car extends Vehicle` adds types without adding meaning. Use an enum until behaviour differs.
- **Compatibility spread across the code.** `if (vehicle == TRUCK && spot.size != LARGE)` in three places. One `fits` method.
- **Pricing with `double` and `Math.ceil`.** Works for the demo and fails on the boundary tests.
- **Grace period off-by-one.** Is 15 minutes free or charged? Decide, test both sides of the boundary.
- **Check-then-act on spot allocation.** The central concurrency bug of this problem.
- **Ticket closed twice.** Silently returning zero hides duplicate exits.

## Interview questions and model answers

**Q: Why not a `Vehicle` abstract class?**
A: Because the only thing that varies by vehicle type is which spots it fits, and that is data, not behaviour. An enum with a minimum spot size says it in one line and keeps the switch exhaustive. If the requirements added behaviour that varies - different entry procedures, different billing - I would introduce a type then.

**Q: How do you choose a spot?**
A: Through an allocation policy. My default is best fit - the smallest compatible spot - because first fit can put a car in the only large spot and reject a truck from a lot with free space. I would note that best fit is not always right: if large spots sit idle while cars queue, the operator might prefer to overflow cars into them.

**Q: Two cars arrive at two gates at the same instant. What happens?**
A: Both call `park`. The method holds the lot's lock across choosing and removing the spot, so the second call sees the first car's spot already gone. I have a test that runs 8 threads against 500 spots and checks exactly 500 succeed with 500 distinct spots.

**Q: The lot has 50,000 spots and 40 gates. Is the single lock still acceptable?**
A: Probably yes, because the critical section is a short in-memory scan and gates are slow. If profiling showed contention, I would partition by floor, give each floor its own lock and free-spot index, and have the entry gate try floors nearest-first. Each invariant is then per floor, which holds because a spot belongs to exactly one floor.

**Q: How would you add hourly pricing that varies by time of day?**
A: A new pricing policy that splits the stay into intervals at rate boundaries and sums each interval at its rate, with the daily cap applied last. The lot does not change; the constructor receives the new policy.

## Exercises

1. Add EV spots: EVs prefer EV spots, other cars may use them only when no compact spot is free. Write the compatibility table and the allocation policy.
2. Write the pricing policy for "first hour free on weekends" without changing `HourlyPricing`.
3. Replace the `synchronized` lot with per-floor locks. State the invariant each lock protects and write the concurrent test for it.
4. Add reservations. What happens to a reserved spot when the reserving driver is 30 minutes late?

## Chapter summary

The parking lot is a constrained allocator with billing. Put compatibility on the vehicle type, allocation and pricing behind policies, and money in integer cents. Allocation policy affects which vehicles are rejected, so it is a correctness choice. Make choosing and claiming a spot one atomic step, and prove it with a concurrent test that checks counts and distinctness. Every standard extension should land in a new policy or a small new entity.

## Revision checklist

- [ ] I can explain why first fit rejects a truck that best fit admits.
- [ ] I can recite and test the pricing boundaries: grace edge, exact hour, hour plus one minute, cap, day plus one hour.
- [ ] I can write the check-then-act bug and its one-lock fix from memory.
- [ ] I can say where EV spots, reservations, and dynamic pricing plug in.
