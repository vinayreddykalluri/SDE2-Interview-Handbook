# 4. Amortized Analysis

## Why this chapter exists

Several structures in this volume have an operation whose *worst case* is O(n) while the structure as a whole is still considered O(1) per operation. The monotonic stack pops an unbounded number of elements in one step. A queue built from two stacks moves the entire contents on one dequeue. `ArrayDeque` and `ArrayList` copy everything when they grow.

Candidates who say "the inner while loop makes this O(n^2)" about a monotonic stack are reading the code correctly and analysing it wrongly. **Amortized analysis is what makes the correct answer sayable**, and interviewers ask for it directly - "you have a nested loop there, what is the complexity?" is a test, not a trap.

Three techniques answer it, and the middle one is worth genuinely understanding rather than reciting.

## What amortized does and does not mean

**Amortized O(1)** means: any sequence of `n` operations costs O(n) total, so the average per operation is O(1). It is a guarantee about the *sequence*, and it holds for every sequence, including adversarial ones.

It is not average-case analysis. Average case assumes a distribution over inputs and can be defeated by a hostile one. Amortized makes no probabilistic assumption at all - it is a worst-case bound on the total.

That distinction is the one interviewers probe, and stating it correctly separates understanding from vocabulary:

| | Guarantee about | Defeated by an adversary? |
|---|---|---|
| Worst case per operation | one operation | no |
| **Amortized** | a sequence of operations | **no** |
| Average case | a distribution of inputs | yes |

The practical caveat: amortized bounds say nothing about **latency**. A single operation can still take O(n), and for a real-time or low-tail-latency system that spike matters. `ArrayList.add` is amortized O(1) and can still stall on a resize of a large list. Naming that caveat unprompted is a strong senior signal.

## Technique 1: aggregate analysis

Bound the total work over `n` operations directly, then divide.

**Monotonic stack.** Computing the next greater element pushes each index once and pops it at most once. The inner `while` may pop many elements in one iteration, but across the whole run **the total number of pops cannot exceed the total number of pushes, which is `n`**. Total work O(n), amortized O(1) per element.

```java
static int[] nextGreater(int[] values) {
    int[] answer = new int[values.length];
    Arrays.fill(answer, -1);
    Deque<Integer> stack = new ArrayDeque<>();          // holds indices
    for (int i = 0; i < values.length; i++) {
        while (!stack.isEmpty() && values[stack.peek()] < values[i]) {
            answer[stack.pop()] = values[i];            // each index pops once, ever
        }
        stack.push(i);
    }
    return answer;
}
```

The sentence that answers the interviewer is: *"the inner loop is bounded not by n per iteration but by the number of pushes remaining, and each index is pushed exactly once and popped at most once, so the total is O(n)."*

**Dynamic array growth.** Doubling on resize, `n` appends cost `1 + 2 + 4 + ... + n < 2n` copies. Total O(n), amortized O(1).

The doubling factor is what makes this work. **Growing by a constant amount instead is O(n^2)**: growing by 10 requires `n/10` resizes copying an average of `n/2` each. Any *multiplicative* factor gives amortized O(1) - the factor changes the constant and the memory overhead, not the class.

## Technique 2: the accounting method

Aggregate analysis needs you to see the total. The accounting method is more mechanical: **overcharge cheap operations and store the surplus as credit, then spend it on expensive ones.** If credit never goes negative, the charged rate is a valid amortized bound.

**Queue from two stacks.** `inbox` receives pushes; `outbox` serves pops. When `outbox` empties, everything transfers.

```java
public final class QueueFromStacks<T> {
    private final Deque<T> inbox = new ArrayDeque<>();
    private final Deque<T> outbox = new ArrayDeque<>();

    public void enqueue(T item) {
        inbox.push(item);
    }

    public T dequeue() {
        if (outbox.isEmpty()) {
            while (!inbox.isEmpty()) {
                outbox.push(inbox.pop());     // O(n) - but only once per element
            }
        }
        if (outbox.isEmpty()) {
            throw new NoSuchElementException("empty queue");
        }
        return outbox.pop();
    }
}
```

A single `dequeue` can be O(n). The accounting argument:

```text
charge enqueue 3 credits:  1 pays the push onto inbox
                           1 saved for the future pop from inbox
                           1 saved for the future push onto outbox
charge dequeue 1 credit:   pays the pop from outbox
```

Every element is moved at most once from `inbox` to `outbox`, and the two credits banked at enqueue pay for exactly that move. Credit never goes negative, so **enqueue is amortized O(1) and dequeue is amortized O(1)**, even though one dequeue can be O(n).

The transfer condition matters more than it first appears, and it is worth being exact about why.

Refilling only when `outbox` is empty is what guarantees each element moves once, which is what the accounting argument depends on. Transferring on *every* dequeue is the standard bug - and its first symptom is not slowness but **wrong output**. If `outbox` still holds older elements, newly transferred ones land on top of them and are dequeued first, so the queue stops being FIFO. Running both versions over twenty thousand mixed operations, the always-transfer variant produced incorrect ordering while moving barely more elements than the correct one.

So the guard is a correctness control that also happens to preserve the amortized bound. That ordering matters: a candidate who defends it purely on performance grounds has not noticed the more serious failure.

## Technique 3: the potential method

A more formal version: define a potential function over the structure's state, and let amortized cost be actual cost plus the change in potential.

```text
amortized(i) = actual(i) + potential(after) - potential(before)
```

For the two-stack queue, `potential = 2 * inbox.size()`. An enqueue adds one actual push and raises potential by 2, giving amortized 3. A dequeue that transfers `k` elements does `2k` actual work but drops potential by `2k`, giving amortized 1 plus the outbox pop.

This is the same argument as accounting, expressed as a function of state rather than as saved coins. It is worth naming - "the potential is the pending work stored in the inbox" - and rarely worth deriving formally in an interview. Accounting is easier to say out loud.

## The one to watch: amortization does not survive everything

Two situations break these bounds, and knowing them is the depth question.

**Repeated undo destroys amortization.** Amortized bounds assume operations accumulate credit. Consider an `ArrayList` at exactly its capacity boundary: adding triggers a resize, removing shrinks back, and alternating add/remove at that boundary pays O(n) *every time* if the implementation shrinks eagerly. Real implementations avoid this with hysteresis - shrinking only at, say, one quarter full rather than one half - so a single operation cannot flip the state back. That gap between the shrink and grow thresholds is exactly what preserves the amortized bound.

**Persistent or copied structures break it.** If a caller can save a snapshot and repeatedly invoke the expensive operation from the same saved state, the banked credit is spent many times. Amortization assumes a single linear sequence of operations, and immutable or versioned structures violate that assumption.

## Edge cases and common mistakes

- Calling a monotonic stack O(n^2) because of the inner `while`.
- Confusing amortized with average case; amortized holds against adversarial inputs.
- Assuming amortized O(1) bounds latency. A single operation can still take O(n).
- Growing a dynamic array by a constant rather than a factor, making appends O(n^2) overall.
- Transferring between the two stacks on every dequeue, which breaks FIFO order - newly moved elements sit on top of older ones.
- Forgetting the empty-queue check after the transfer, so an empty queue pops from an empty outbox.
- Shrinking a dynamic array at the same threshold it grows at, letting alternating add/remove cost O(n) each.
- Claiming amortized bounds hold for persistent structures where an expensive state can be replayed.
- Charging credit in the accounting method without checking it never goes negative.

## Interview questions and model answers

**Your monotonic stack has a nested while loop. Is it O(n^2)?**

No, O(n) total. The inner loop is bounded by the number of elements currently on the stack, and each index is pushed exactly once and popped at most once across the whole run. So total pops are at most total pushes, which is n. Amortized O(1) per element.

**What does amortized O(1) actually guarantee?**

That any sequence of n operations costs O(n) in total, so the average is constant. It is a worst-case bound on the sequence and holds against adversarial inputs, which is what distinguishes it from average-case analysis. It does not bound any individual operation, so latency spikes remain possible.

**Prove the two-stack queue is amortized O(1).**

Accounting method. Charge each enqueue three credits: one for the push onto the inbox, and two banked for the eventual pop from the inbox and push onto the outbox. Each element moves between the stacks at most once, and the banked credits pay for exactly that. Credit never goes negative, so both operations are amortized O(1) despite one dequeue costing O(n).

**Why must the transfer happen only when the outbox is empty?**

Primarily for correctness. If the outbox still holds older elements, transferring puts newer ones on top of them, so they dequeue first and the queue is no longer FIFO. The guard also preserves the amortized bound, since it is what makes each element move exactly once - but the ordering failure is the more serious one and the one to lead with.

**Why does doubling matter for dynamic arrays?**

The total copy cost across n appends is a geometric series bounded by 2n, giving amortized O(1). Growing by a constant amount instead needs n/c resizes copying an average of n/2 each, which is O(n^2) overall. Any multiplicative factor works; the specific factor trades memory overhead against resize frequency.

**When does an amortized bound stop holding?**

When the sequence assumption breaks. Alternating operations at a resize boundary can cost O(n) each if the shrink and grow thresholds coincide, which is why implementations leave hysteresis between them. And persistent or snapshot-able structures break it entirely, because a caller can replay the expensive operation from a saved state and spend the same credit repeatedly.

## Exercises

1. **Foundation:** Count total pushes and pops for `nextGreater` on a strictly decreasing array and on a strictly increasing one. Confirm both are O(n).
2. **Foundation:** Compute total copies for a thousand appends with doubling and with growth by ten. State both complexity classes.
3. **Interview Core:** Implement the two-stack queue and instrument it to report total element moves over ten thousand mixed operations. Compare against the operation count.
4. **Interview Core:** Change the transfer to happen on every dequeue, then check the output order against a reference queue before looking at the move count. Say which failure you would have caught first by reading the code.
5. **Interview Core:** Write the accounting argument for a dynamic array with doubling, stating the credit charged per append.
6. **Interview Core:** Build a stack with eager shrinking at half capacity and construct the alternating sequence that costs O(n) per operation.
7. **SDE-2 Follow-up:** Define the potential function for the two-stack queue and derive the amortized cost of both operations.
8. **SDE-2 Follow-up:** Measure worst single-operation latency for `ArrayList.add` over a million appends and reconcile it with the amortized bound.
9. **SDE-2 Follow-up:** Explain why a persistent version of the two-stack queue loses the amortized guarantee, with a concrete calling pattern.
10. **Challenge:** Design a stack supporting `push`, `pop`, and `getMin` in worst-case O(1) - not amortized - and state the extra memory it costs.

## Chapter summary

Several structures in this volume have an operation whose worst case is linear while the structure is still O(1) per operation overall, and amortized analysis is what makes that sayable. It guarantees that any sequence of n operations costs O(n) total, which is a worst-case bound on the sequence rather than an average over inputs - so it holds against adversarial data, and it says nothing about the latency of any single operation. Three techniques deliver it: aggregate analysis bounds the total directly, which is why a monotonic stack's inner loop is O(n) overall since each index is pushed once and popped at most once; the accounting method banks credit on cheap operations to pay for expensive ones, which proves the two-stack queue amortized O(1) provided the transfer happens only when the outbox empties - a guard that is first a correctness control, since transferring onto a non-empty outbox breaks FIFO order outright; and the potential method expresses the same argument as a function of state. The bounds break in two places worth knowing - resize thresholds without hysteresis, where alternating operations pay the full cost every time, and persistent structures, where a saved state lets the same banked credit be spent repeatedly.

## Revision checklist

- [ ] I can explain why a monotonic stack's nested loop is O(n) total.
- [ ] I can distinguish amortized from average case and say which an adversary defeats.
- [ ] I know amortized bounds do not bound latency.
- [ ] I can give the accounting proof for the two-stack queue.
- [ ] I know the transfer guard is a correctness control first and an amortization control second.
- [ ] I can explain why doubling gives O(1) and constant growth gives O(n^2).
- [ ] I can state the potential function for the two-stack queue.
- [ ] I know why shrink and grow thresholds must differ.
- [ ] I know persistent structures break amortized guarantees.
