# Circular Deque and Monotonic Invariants

Stack, queue, and deque are access contracts over ordered state. A stack uses one end, a queue uses opposite ends, and a deque exposes both. Java's `ArrayDeque` is the right default for interview solutions, but implementing a circular deque once makes wraparound, resizing, and monotonic candidate structures much easier to reason about.

The complete primitive deque, monotonic algorithms, and executable checks are in `OrderingStructuresInterviewChecks.java`.

## One buffer, logical order, physical wraparound

The companion stores:

- `head`: physical index of the first logical value;
- `size`: number of logical values; and
- `elements.length`: capacity.

Logical offset `k` lives at:

```text
(head + k) % capacity
```

Example:

```text
physical indexes:  0   1   2   3   4   5   6   7
buffer:            C   D   .   .   .   A   B   .
head = 5, size = 4
logical order:     A, B, C, D
```

The array contents outside logical size are irrelevant. Empty and full cannot both be inferred from `head == tail`; storing `size` removes that ambiguity.

## Operations at both ends

- `addFirst`: decrement head with wraparound, write, increment size.
- `addLast`: write at logical offset `size`, increment size.
- `removeFirst`: read head, increment head, decrement size.
- `removeLast`: read logical offset `size - 1`, decrement size.

All are `O(1)` except a resize. On resize, copy logical offsets `0..size-1` into a larger array beginning at physical zero and reset `head = 0`. Copying physical indexes directly would scramble wrapped order.

Geometric doubling makes repeated end insertion amortized `O(1)`. The companion starts with nonzero capacity so doubling cannot remain zero.

## Stack and queue views

With `ArrayDeque<E>`:

```text
stack: push / pop / peek       operate at first end
queue: addLast / removeFirst   opposite ends
deque: add/remove at either end
```

Avoid legacy `Stack` for new interview code. Also remember `ArrayDeque` rejects null, allowing null-returning methods to represent emptiness unambiguously. Choose throwing versus sentinel APIs deliberately; the custom primitive deque throws `NoSuchElementException` on empty removal.

## Monotonic stack: unresolved candidates

For “days until a warmer temperature,” the stack stores indexes whose answer is unresolved. Temperatures at those indexes are nonincreasing from bottom to top. A warmer current day resolves every smaller top:

```text
temperatures: 73, 74, 75, 71, 69, 72
day 4 stack:  [75@2, 71@3, 69@4]
day 5 = 72:   pop 69 -> wait 1
              pop 71 -> wait 2
              stop below 75
```

Each index is pushed once and popped at most once, so the nested-looking while loop is `O(n)` total.

Store indexes when distance, boundaries, or original position matters. Store values only when identity and position truly do not matter.

## Monotonic deque: best candidate in a sliding window

For window maximum, candidate indexes are:

1. inside the current window; and
2. strictly decreasing by value from front to back.

Before adding `right`, remove expired indexes from the front and remove values no better than the new value from the back. The front is then the maximum.

Removing equal older values with `<=` is safe for maximum values: the newer equal value lasts longer. If the output needs the earliest maximum index, equality policy changes.

## Histogram area: boundaries arrive when height drops

An increasing stack delays a bar until a shorter bar reveals its right boundary. When height at index `i` is lower than stack top:

```text
height = popped bar height
right boundary = i (exclusive)
left smaller index = new stack top, or -1
width = i - leftSmaller - 1
area = (long) height * width
```

A virtual trailing height zero flushes remaining bars. Area uses `long`; two `Integer.MAX_VALUE` bars already exceed `int`.

## Postfix evaluation: operand order matters

For a binary operator, pop right operand first and left operand second:

```text
tokens: 7 3 -
right = 3, left = 7, result = 4
```

Reversing pop order is invisible for addition but wrong for subtraction and division. The companion validates stack arity, final stack size, numeric tokens, division by zero, and arithmetic overflow.

## Edge-case matrix

| Case | Correct handling | Common failure |
|---|---|---|
| empty deque removal | explicit exception/sentinel contract | reading stale buffer slot |
| wrapped resize | copy logical order, reset head | copy physical slices incorrectly |
| capacity one | grow before second insert | overwrite live value |
| size returns to zero | normalize or maintain valid head | confusing old indexes with live state |
| duplicate window maxima | choose equality/tie contract | wrong retained index |
| `k=1` | each value is its own maximum | off-by-one output size |
| `k>n` or zero | reject | negative allocation |
| equal histogram heights | consistent pop policy | double-count/wrong boundary |
| area overflow | multiply in `long` | overflow before assignment |
| postfix subtraction/division | pop right then left | reversed result |
| malformed expression | require two operands and one final result | accepting unused operands |
| mutable queued priority/state | use immutable snapshots or controlled updates | order silently stale |

## Six live interview Q&A chains

### 1. Full versus empty ring

**Interviewer:** Why keep `size`?

**Candidate:** With only equal head/tail indexes, full and empty can look identical unless one slot is sacrificed. `size` gives an explicit invariant and lets every capacity slot hold data.

### 2. Resize correctness

**Interviewer:** Can you call `Arrays.copyOf` on the wrapped buffer?

**Candidate:** It preserves physical order, not logical order. I copy `elements[(head+k)%oldCapacity]` to new index `k`, then set head to zero.

### 3. Nested monotonic loop

**Interviewer:** Why is the temperature solution not quadratic?

**Candidate:** Every index enters once and leaves at most once. Aggregate pops over the whole scan are at most `n`, so total work is linear.

### 4. Window equality

**Interviewer:** Why discard an older equal maximum?

**Candidate:** For a value-only answer, the newer equal index dominates: same value, later expiration. If the API requires earliest-max identity, I retain the older equal index by changing `<=` to `<`.

### 5. Histogram sentinel

**Interviewer:** Why append a zero-height bar?

**Candidate:** It supplies a right boundary for every still-open positive bar, letting one loop handle cleanup. I model it virtually so input is not modified.

### 6. Standard versus custom deque

**Interviewer:** Would you use your custom deque in the interview solution?

**Candidate:** Only if implementation is the question. For algorithm problems I use `ArrayDeque`, state the end operations clearly, and focus on the monotonic invariant. The custom version demonstrates that I understand the mechanics beneath the API.

## Run the companion

```bash
javac --release 21 -Xlint:all -Werror OrderingStructuresInterviewChecks.java
java OrderingStructuresInterviewChecks
```

Expected final line: `PASS 16 ordering-structure checks`.
