# Realistic Stack, Queue, and Deque Interview Rounds

## Round 1: daily temperatures

### Prompt

For each day's temperature, return how many days must pass until a strictly warmer temperature. Return zero when none exists.

### Candidate derivation

Brute force scans right from every day: O(n squared). Instead, keep indexes of unresolved days in decreasing temperature order. A warmer current day resolves every colder index at the top.

```java
static int[] daysUntilWarmer(int[] temperatures) {
    int[] answer = new int[temperatures.length];
    Deque<Integer> unresolved = new ArrayDeque<>();
    for (int day = 0; day < temperatures.length; day++) {
        while (!unresolved.isEmpty()
                && temperatures[day] > temperatures[unresolved.peek()]) {
            int earlier = unresolved.pop();
            answer[earlier] = day - earlier;
        }
        unresolved.push(day);
    }
    return answer;
}
```

**Why indexes?** The output is a distance. The temperature can be recovered from the array.

**Why strictly greater?** Equal temperature does not satisfy "warmer," so equality stays unresolved.

**Complexity?** O(n) time by push/pop aggregate analysis and O(n) auxiliary space.

## Round 2: sliding-window maximum

### Prompt and clarification

Return the maximum for every contiguous window of size `k`. Require `1 <= k <= n`.

### Model answer

```java
static int[] windowMaximum(int[] values, int k) {
    if (k < 1 || k > values.length) {
        throw new IllegalArgumentException("invalid window");
    }
    int[] answer = new int[values.length - k + 1];
    Deque<Integer> candidates = new ArrayDeque<>();
    for (int right = 0; right < values.length; right++) {
        int left = right - k + 1;
        while (!candidates.isEmpty() && candidates.peekFirst() < left) {
            candidates.removeFirst();
        }
        while (!candidates.isEmpty()
                && values[candidates.peekLast()] <= values[right]) {
            candidates.removeLast();
        }
        candidates.addLast(right);
        if (left >= 0) {
            answer[left] = values[candidates.peekFirst()];
        }
    }
    return answer;
}
```

Invariant: candidate indexes are inside the active window, increase from front to back, and their values strictly decrease. Any removed-back index is dominated by the newer index for all future windows containing both.

### Follow-up answers

**Could a heap work?** Yes, O(n log n) with lazy expiry or indexed deletion. The deque exploits ordered window expiry and dominance for O(n).

**Streaming?** Yes. Emit a maximum after receiving the first `k` values. Retain indexes or sequence numbers for expiry.

## Round 3: queue using two stacks

### Prompt

Implement FIFO queue operations with two stacks and explain amortized cost.

### Candidate answer

Push new elements onto `incoming`. Pop or peek from `outgoing`. When `outgoing` is empty, move every incoming element to it, reversing the order once.

```java
static final class TwoStackQueue<T> {
    private final Deque<T> incoming = new ArrayDeque<>();
    private final Deque<T> outgoing = new ArrayDeque<>();

    void add(T value) {
        incoming.push(Objects.requireNonNull(value));
    }

    T remove() {
        transferIfNeeded();
        return outgoing.pop();
    }

    T peek() {
        transferIfNeeded();
        return outgoing.peek();
    }

    boolean isEmpty() {
        return incoming.isEmpty() && outgoing.isEmpty();
    }

    private void transferIfNeeded() {
        if (outgoing.isEmpty()) {
            while (!incoming.isEmpty()) {
                outgoing.push(incoming.pop());
            }
        }
    }
}
```

Each element is pushed to incoming, moved at most once, and popped from outgoing. A single remove can cost O(n), but a sequence of operations costs O(1) amortized per operation.

### SDE-2 follow-ups

**Can it be bounded?** Track total size and reject or block adds at capacity. Blocking adds introduce concurrency, interruption, and condition signaling beyond the DSA contract.

**Is it thread-safe?** No. Thread safety requires a synchronization design; choosing concurrent collections is a separate production decision.

## Closing answer pattern

Name the access policy, the exact deque ends, the stored state, the invariant, empty behavior, amortized reasoning, and one API or concurrency boundary.
