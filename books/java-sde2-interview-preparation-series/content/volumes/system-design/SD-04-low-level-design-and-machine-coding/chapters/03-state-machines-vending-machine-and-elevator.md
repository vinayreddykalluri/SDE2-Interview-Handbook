# 3. State Machines: Vending Machine and Elevator

## Learning objectives

By the end of this chapter, you should be able to:

- model a problem as an explicit state machine and reject illegal transitions instead of ignoring them;
- place every refusal *before* any irreversible side effect;
- explain why greedy change-making fails with a limited coin inventory and implement the bounded alternative;
- state and test a conservation invariant for money;
- implement LOOK elevator scheduling and state precisely when it is, and is not, better than first-come-first-served; and
- extend a single-car design to a multi-car dispatcher.

## Why this matters at SDE-2

Both problems look like exercises in `enum` syntax. They are actually exercises in *ordering side effects*. A vending machine that dispenses the drink and then discovers it cannot make change has lost either the customer's money or the owner's product. An elevator that serves requests in arrival order is correct and spends more than twice the travel of one that sweeps. The interviewer is watching for whether you find these failure points yourself.

## First-principles model

A **state machine** is a set of states, a set of events, and a transition function. Designing one well means three things:

1. **Every state is named.** If behaviour depends on "whether money has been inserted", that is a state, not a boolean field checked in six places.
2. **Every transition is explicit, and every other transition is rejected.** Silently ignoring `select` in the idle state hides bugs; returning a clear refusal or throwing makes them visible.
3. **Irreversible actions come last.** Validate everything that can fail, then commit. Dispensing, charging a card, and moving a physical car are irreversible.

The vending machine is a *transactional* state machine: it must end every interaction with money conserved. The elevator is a *scheduling* state machine: its state is a position and a direction, and the interesting question is which transition to take next.

## Core terminology

- **State:** a named condition that determines which events are legal.
- **Transition:** a legal move from one state to another in response to an event.
- **Guard:** a condition checked before a transition is allowed.
- **Conservation invariant:** a quantity that must balance after every operation, such as inserted money equals money kept plus money returned plus current credit.
- **Bounded change-making:** making change using only coins actually in the machine.
- **FCFS:** first-come-first-served; serve requests in arrival order.
- **LOOK:** keep moving in the current direction while requests remain ahead, then reverse.
- **SCAN:** like LOOK, but travel to the physical end before reversing.

## Detailed mechanics

### The vending machine as a transactional state machine

```text
          insert(coin)                select(code): all guards pass
  IDLE ----------------> HAS_MONEY ---------------------------------> IDLE
                          |   ^  |                                  (dispense + change)
                          |   |  | insert(coin)
                          |   +--+
                          | cancel()
                          +--------------------------------------> IDLE (refund)

  any state --maintenance--> OUT_OF_SERVICE
```

`select` has four guards, and **all of them run before anything leaves the machine**:

```java
Vend select(String code) {
    if (state != VendingState.HAS_MONEY)                  return refuse("insert money first");
    if (product == null || stock.get(code) == 0)          return refuse("sold out");
    if (credit < product.priceCents())                    return refuse("insufficient credit");
    Optional<Map<Integer, Integer>> change = makeChange(credit - price, coins);
    if (change.isEmpty())                                 return refuse("exact change only");
    // only now: remove coins, decrement stock, record the sale
}
```

The fourth guard is the one most candidates miss. They dispense, then compute change, then discover the machine has no suitable coins. At that point there is no correct outcome.

### Why greedy change fails with a real coin box

With unlimited coins of 25, 10, 5, and 1 cents, greedy change - take the largest coin that fits, repeat - is optimal. That fact is well known, and it leads candidates to write greedy change for a vending machine. But a vending machine does not have unlimited coins.

The companion's example: the box holds one 25c and three 10c coins, and 30c change is due.

```text
change for 30c from {25x1, 10x3}: greedy FAILS, bounded DP {10=3}
```

Greedy takes the quarter, needs 5c, and has none. Three dimes would have worked. The failure is not a contrived corner: over 5,000 random inventories and amounts, greedy failed where change existed in 27 cases.

The fix is bounded change-making: a small dynamic program over the amount, treating each physical coin as a 0/1 item and minimising coin count. For amounts under a few dollars and a few dozen coins it is microseconds. The companion checks, over the same 5,000 trials, that the DP result always sums to the amount, never uses more coins than the box holds, and finds change whenever greedy does.

### Seeing the guard order matter

The companion replays a real sequence. The box holds one 25c and three 10c coins; water costs 65c.

```text
$1.00 for a 65c item with {25x1, 10x3} loaded -> dispensed {25=1, 10=1}
second $1.00 for the same item -> refused: exact change only
cancel refunds {100=1}, state IDLE
```

The first sale gives 35c as 25 + 10. The box now holds two dimes and the inserted dollar coin. The second customer needs 35c, which two dimes cannot make, so the machine refuses *before* dispensing, and cancel returns their dollar. This is the "exact change only" light on a real machine, and it exists because of exactly this ordering.

### The conservation invariant

After every operation the machine checks:

```text
inserted = taken for products + returned as change or refund + current credit
```

It is one line, and it catches every bug of the "money appeared or vanished" class: forgetting to reset credit after a sale, refunding twice, or counting a refused sale as taken. Writing it and asserting it after the demo is a strong SDE-2 signal because it shows you think in invariants, not only in happy paths.

### Elevator: state, direction, and the next stop

A single elevator car has a floor, a direction (`UP`, `DOWN`, `IDLE`), and a set of pending stops. With a `TreeSet<Integer>` of stops, LOOK is three lines:

```java
Integer next = direction == Direction.DOWN ? stops.floor(floor) : stops.ceiling(floor);
if (next == null) {
    direction = direction == Direction.DOWN ? Direction.UP : Direction.DOWN;  // reverse
}
```

`ceiling` and `floor` are O(log n) and give "the nearest stop ahead" directly. This is the one place in these problems where choosing a sorted collection is the whole algorithm.

### How much better is LOOK, exactly?

Start at floor 5 with requests `[1, 9, 2, 8, 3]` known up front:

```text
FCFS travels 30 floors
LOOK starting UP   visits [8, 9, 3, 2, 1], travels 12 floors
LOOK starting DOWN visits [3, 2, 1, 8, 9], travels 12 floors
```

FCFS zig-zags across the building: 4 + 8 + 7 + 6 + 5. LOOK sweeps once each way.

When I first wrote the companion check for this, I asserted that LOOK never travels further than FCFS. The randomized test refuted it: with the car always starting upward, LOOK lost to FCFS in 3 of 2,000 random batches. The smallest case is easy to see:

```text
start 5, requests [4, 20]: FCFS 17, LOOK up-first 31, LOOK down-first 17
```

Going up first travels to 20 and back down to 4 - 15 + 16 floors - while FCFS happens to visit the near stop first. The correct claim is narrower: **for a batch of requests known up front, LOOK started in the better direction is optimal**, because any complete tour on a line must reach both extremes, and the cheapest way is to go to the nearer extreme first and then sweep. The companion checks this against the closed-form optimum over the 2,000 batches: best-direction LOOK is optimal in all 2,000 and averages 18.8 floors against FCFS's 52.7.

That correction is worth saying in an interview. Real elevators are *online*: requests arrive while the car moves, and a car already heading up is committed to that direction. LOOK is used because it is fair and bounded - no request waits more than one round trip - not because it is optimal in every instance.

### Multiple cars: dispatch is a policy

With several cars, a dispatcher assigns each hall call to one car. It is an allocation policy, exactly like parking-spot allocation:

```java
interface DispatchPolicy {
    Car choose(List<Car> cars, int floor, Direction requested);
}
```

A reasonable default cost function: a car already moving toward the call in the requested direction costs its distance; an idle car costs its distance; a car moving away costs the distance to its last stop plus the distance back. Choose the minimum. Say that production systems also weigh load and use destination dispatch (passengers enter their floor in the lobby), and that both are new policies, not changes to the car.

### States the elevator must reject

The car should refuse to move with doors open, refuse to open doors between floors, and enter a maintenance state that ignores new calls but finishes delivering current passengers. These are the transitions interviewers add in the extension round, and an explicit transition table absorbs them as new rows.

## Failure modes and common mistakes

- **Booleans instead of states.** `hasMoney`, `isDispensing`, `isBroken` checked everywhere; illegal combinations become reachable.
- **Dispense before change.** The canonical vending bug. Guard first, commit last.
- **Greedy change with finite coins.** Correct for unlimited coins, wrong for a coin box.
- **Money in `double`.** Credit of 0.1 + 0.2 is not 0.3.
- **No conservation check.** Money bugs are invisible without one.
- **Overclaiming the elevator algorithm.** LOOK is not always better than FCFS; state the conditions.
- **Treating direction as derived.** Direction is state; a car heading up with no stops above must decide explicitly to reverse or go idle.

## Interview questions and model answers

**Q: A customer inserts $1 for a 65c item. When do you check that change is available?**
A: Before dispensing, as the last guard in `select`. If change cannot be made from the coins actually in the box, the sale is refused with "exact change only" and the customer can cancel for a refund. Once the product drops, there is no correct way to recover.

**Q: Isn't greedy change optimal for US coins?**
A: For unlimited coins, yes. A vending machine has a finite coin box. With one quarter and three dimes, greedy fails to make 30c while three dimes succeed. I use a small bounded dynamic program instead; it is cheap at these sizes, and in a randomized test greedy failed in 27 of 5,000 inventories where change existed.

**Q: How do you know no money is ever lost?**
A: The machine tracks four totals and checks `inserted == taken + returned + credit` after every operation. It catches missing credit resets, double refunds, and refused sales counted as taken.

**Q: Why LOOK over FCFS for the elevator?**
A: It sweeps instead of zig-zagging, and no request can starve: each is reached within one round trip of the car. On a known batch it is optimal if started in the better direction - it travelled 12 floors against FCFS's 30 in the example. It is not always better with a fixed starting direction: from floor 5 with requests at 4 and 20, going up first travels 31 floors against FCFS's 17. Real cars are online and committed to a direction, so the justification is fairness and bounded waiting, not optimality.

**Q: How would you add a second elevator?**
A: A dispatch policy that assigns each hall call to one car using a cost function - distance for a car coming toward the call in the right direction, distance to its turnaround plus back for a car moving away. The car class does not change; dispatch is a separate policy.

## Exercises

1. Add a `MAINTENANCE` state to the vending machine that allows coin collection and restocking but rejects sales. Write the transition table.
2. Make the vending machine accept card payment. Which guard changes, and where is the new irreversible step?
3. Implement SCAN, which travels to the physical end before reversing, and compare its travel with LOOK on the example batch.
4. Write the dispatch cost function for three cars and test it with a call that one car is passing.

## Chapter summary

State machines are about naming states and ordering side effects. Put every refusal before every irreversible action, and check a conservation invariant after each operation. Greedy change-making fails with a finite coin box; bounded change-making does not. For the elevator, LOOK on a sorted set of stops is simple and fair, and it is optimal for a known batch only when started in the better direction - a claim the companion's randomized test forced me to narrow. Multi-car dispatch is an allocation policy.

## Revision checklist

- [ ] I can draw the vending state machine and list the four `select` guards in order.
- [ ] I can give the 30c counterexample to greedy change and name the fix.
- [ ] I can write the money conservation invariant.
- [ ] I can implement LOOK with `TreeSet.ceiling` and `floor`.
- [ ] I can state precisely when LOOK is optimal, with the [4, 20] counterexample.
