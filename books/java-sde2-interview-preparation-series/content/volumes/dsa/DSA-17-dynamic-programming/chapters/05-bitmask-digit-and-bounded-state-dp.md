# 5. Bitmask, Digit, and Bounded-State DP

## Why this chapter exists

The DP problems covered so far all had a state that was obviously an index or a pair of indices. This chapter covers the cases where the state is a **set**, a **position within a numeral**, or a **budget** - shapes that are less obvious but appear regularly once an interviewer starts pushing past the standard catalogue.

They share one property worth naming immediately: each is a technique for making an exponential search space finite and enumerable. Bitmask DP does not make an NP-hard problem polynomial. It makes `n!` into `2^n * n`, which is a genuine and useful difference at `n = 20` and no help at all at `n = 60`. Being able to say that precisely is the interview signal.

## Bitmask DP: when the state is a subset

### Recognizing it

Reach for a bitmask when **all** of the following hold:

- The state must record *which* elements have been used, not merely how many.
- Order of selection does not matter for the state, only the set does.
- `n` is small - roughly 20 or fewer, occasionally 24 with tight memory.

That last condition is a hard gate. `2^20` is about a million states, which is comfortable; `2^25` is 33 million, which is usually not. If the prompt says `n <= 20`, the constraint is telling you the intended solution.

### The representation

An `int` holds the subset. Bit `k` set means element `k` is used.

```java
int full = (1 << n) - 1;          // all n elements used
boolean used = (mask & (1 << k)) != 0;
int with = mask | (1 << k);       // add element k
int without = mask & ~(1 << k);   // remove element k
int count = Integer.bitCount(mask);
```

`Integer.bitCount` is worth knowing by name: it compiles to a single `POPCNT` instruction on modern hardware, so using it as a loop counter is free.

### Worked derivation: minimum-cost assignment

*n tasks, n workers, `cost[worker][task]`. Assign each worker exactly one task, minimizing total cost.*

The naive search tries all `n!` assignments. The insight is that once you have assigned the first `k` workers, **which** tasks remain is all that matters - not the order they were assigned in. That collapses `n!` orderings into `2^n` subsets.

1. *State:* `dp[mask]` = minimum cost to assign tasks in `mask` to the first `bitCount(mask)` workers.
2. *Transition:* let `worker = bitCount(mask)`. For each unused task `t`, `dp[mask | (1<<t)] = min(that, dp[mask] + cost[worker][t])`.
3. *Base:* `dp[0] = 0`; no workers assigned, no cost.
4. *Order:* increasing `mask` as an integer. Adding a bit strictly increases the value, so every predecessor is already final.
5. *Answer:* `dp[full]`.
6. *Complexity:* O(2^n * n) time, O(2^n) space.

Step 4 is the elegant part and worth stating explicitly in an interview: iterating masks in increasing numeric order is a valid topological order, because `mask | (1<<t) > mask` whenever bit `t` was unset. No explicit ordering logic is needed.

```java
static int minimumAssignmentCost(int[][] cost) {
    int n = cost.length;
    int full = (1 << n) - 1;
    int[] dp = new int[1 << n];
    Arrays.fill(dp, Integer.MAX_VALUE);
    dp[0] = 0;

    for (int mask = 0; mask < full; mask++) {
        if (dp[mask] == Integer.MAX_VALUE) {
            continue;                     // unreachable, do not relax from it
        }
        int worker = Integer.bitCount(mask);
        for (int task = 0; task < n; task++) {
            if ((mask & (1 << task)) != 0) {
                continue;                 // already assigned
            }
            int next = mask | (1 << task);
            int candidate = dp[mask] + cost[worker][task];
            if (candidate < dp[next]) {
                dp[next] = candidate;
            }
        }
    }
    return dp[full];
}
```

The `dp[mask] == MAX_VALUE` guard is not decoration. Without it, `dp[mask] + cost` overflows to a negative number and silently poisons the table - the classic sentinel-arithmetic bug from chapter 3, appearing here in a new costume.

### Travelling salesman, and the honest complexity statement

The Held-Karp formulation adds "where am I now" to the state:

- *State:* `dp[mask][last]` = cheapest path visiting exactly the cities in `mask`, ending at `last`.
- *Transition:* `dp[mask | 1<<next][next] = min(that, dp[mask][last] + dist[last][next])`.
- *Complexity:* O(2^n * n^2) time, O(2^n * n) space.

At `n = 20` that is roughly 400 million operations and 20 million table entries - feasible but not comfortable. At `n = 25` it is out of reach. **This is still exponential.** Held-Karp is a large constant-factor and asymptotic improvement over `O(n!)` brute force, not a polynomial algorithm, and TSP remains NP-hard. Candidates who present bitmask DP as "solving" TSP are making a claim an interviewer will test.

### Subset-sum enumeration

Iterating every subset of a mask is a standard idiom worth recognizing:

```java
for (int sub = mask; sub > 0; sub = (sub - 1) & mask) {
    int complement = mask ^ sub;
    // ... consider splitting mask into sub and complement
}
```

Summed over all masks this is O(3^n), not O(4^n), because each element is in `sub`, in `complement`, or outside `mask` - three choices per element. That counting argument is a common follow-up question.

## Digit DP: when the state is a position in a numeral

### Recognizing it

Digit DP answers "how many integers in `[low, high]` satisfy property P?" when the range is far too large to enumerate - typically up to 10^18. The state walks the decimal representation left to right.

The standard reduction is `count(high) - count(low - 1)`, so only a "count up to N" routine is needed.

### The tight flag is the whole technique

Building a number digit by digit, at each position you may place any digit `0..9` - **unless** every previous digit exactly matched the corresponding digit of `N`, in which case you are bounded above by `N`'s digit at this position.

That single boolean is what makes the state finite:

- *State:* `(position, tight, ...problem-specific state)`.
- `position` - index into the digit string, 0 to 18.
- `tight` - whether the prefix so far equals `N`'s prefix.
- Extra state depends on the property: digit sum so far, last digit placed, count of a particular digit, remainder modulo k.

```java
static long countUpTo(String digits, int position, boolean tight,
                      int state, Long[][][] memo) {
    if (position == digits.length()) {
        return isAccepting(state) ? 1 : 0;
    }
    if (!tight && memo[position][state][0] != null) {
        return memo[position][state][0];   // only cacheable when not tight
    }
    int limit = tight ? digits.charAt(position) - '0' : 9;
    long total = 0;
    for (int digit = 0; digit <= limit; digit++) {
        total += countUpTo(digits, position + 1,
                           tight && digit == limit,
                           nextState(state, digit), memo);
    }
    if (!tight) {
        memo[position][state][0] = total;
    }
    return total;
}
```

**Memoize only when `tight` is false.** This is the detail candidates miss. A tight state depends on the specific prefix of `N` and is visited at most once per position anyway, so caching it is both unnecessary and wrong if the cache is shared across different bounds.

A third flag, `started`, is often needed to handle leading zeros - "count numbers whose digits are strictly increasing" must not treat `007` as having leading digits that break the property.

Digit DP appears at SDE-2 occasionally rather than routinely. Recognizing the shape and naming the tight flag is usually enough; a full implementation under time pressure is a stretch goal.

## Bounded-state DP: budgets, transactions, and cooldowns

The third shape adds a small bounded counter to an otherwise ordinary state. Stock problems are the canonical family, and they are worth working as a group because the progression shows how one extra dimension absorbs each new rule.

| Variant | State | Size |
|---|---|---|
| Unlimited transactions | `dp[day][holding]` | 2 |
| At most one transaction | `dp[day][holding]` with no re-buy | 2 |
| At most `k` transactions | `dp[day][used][holding]` | 2(k+1) |
| With cooldown | `dp[day][holding \| cooling]` | 3 |
| With per-transaction fee | `dp[day][holding]`, fee on sell | 2 |

The pattern to internalize: **a new rule usually becomes a new small dimension, not a new algorithm.** A candidate who has derived the two-state version can extend to `k` transactions in a sentence, and that fluency is what the interviewer is measuring.

```java
static int maxProfitWithCooldown(int[] prices) {
    if (prices.length == 0) {
        return 0;
    }
    int holding = Integer.MIN_VALUE / 2;   // half-range: safe to add to
    int free = 0;                          // sold earlier, may buy
    int cooling = Integer.MIN_VALUE / 2;   // sold today, may not buy tomorrow

    for (int price : prices) {
        int previousHolding = holding;
        int previousFree = free;
        int previousCooling = cooling;

        holding = Math.max(previousHolding, previousFree - price);
        cooling = previousHolding + price;
        free = Math.max(previousFree, previousCooling);
    }
    return Math.max(free, cooling);
}
```

Two details carry the correctness. **Snapshot the previous values** before updating any of them - updating in place makes a later transition read this day's value instead of yesterday's, which silently permits an extra same-day trade. And `Integer.MIN_VALUE / 2` rather than `MIN_VALUE` gives an unreachable sentinel that can still be added to without overflowing, the same discipline as the bitmask guard above.

## Edge cases and common mistakes

- Using a bitmask when `n` exceeds about 20, producing a table that will not allocate.
- Presenting bitmask DP as making TSP polynomial. It remains exponential and NP-hard.
- Relaxing from an unreachable state, so a sentinel participates in arithmetic and overflows.
- Using `Integer.MAX_VALUE` as an additive sentinel instead of a half-range value.
- Forgetting that increasing numeric mask order is already a valid topological order, and writing unnecessary ordering logic.
- Assuming subset-of-subset enumeration is O(4^n); it is O(3^n) by the three-choices argument.
- Memoizing tight states in digit DP, which is unnecessary and unsound across different bounds.
- Omitting the `started` flag in digit DP and mishandling leading zeros.
- Updating stock-state variables in place rather than from a snapshot, permitting an illegal same-day transaction.
- Treating each stock variant as a new problem instead of one more bounded dimension.
- Using `1 << k` with `k >= 31` on `int`; shift to `long` or the result is undefined for the sign bit.

## Interview questions and model answers

**When would you use a bitmask for DP state?**

When the state must record *which* elements are used rather than how many, when order does not matter to the state, and when `n` is small enough that `2^n` is enumerable - roughly 20. A constraint of `n <= 20` in a prompt is usually the setter telling you this is the intended approach.

**Does bitmask DP make TSP tractable?**

It reduces brute force from `O(n!)` to `O(2^n * n^2)` via Held-Karp, which is a real improvement and makes `n` around 20 feasible. It does not make it polynomial and TSP remains NP-hard. For larger instances you move to heuristics or approximation, not a better exact DP.

**Why is iterating masks in increasing numeric order correct?**

Adding an element sets a previously clear bit, which strictly increases the integer value. So every predecessor of a mask is numerically smaller and therefore already computed. The natural integer order is a topological order of the state DAG, which is why no explicit ordering logic is needed.

**What is the `tight` flag in digit DP?**

It records whether the digits placed so far exactly match the prefix of the bound. When tight, the current digit is capped by the bound's digit; when not, all ten digits are available. It is what makes the state space finite and small. Only non-tight states should be memoized, since tight states are bound-specific and visited once.

**Your stock DP with cooldown allows buying on the sell day. Why?**

The state variables were updated in place, so the buy transition read the value already updated this iteration rather than yesterday's. Snapshot the previous values first, then compute all three from the snapshot.

**How do you extend a two-state stock DP to at most k transactions?**

Add a bounded dimension counting transactions used: `dp[day][used][holding]`. The transitions are unchanged in shape; selling increments `used`. This is the general pattern - a new rule usually becomes a small extra dimension rather than a different algorithm.

## Exercises

1. **Foundation:** Write the four bit operations for testing, setting, clearing, and counting bits in a mask, and state what `Integer.bitCount` compiles to.
2. **Foundation:** For `n = 20` and `n = 25`, compute the table sizes for `dp[mask]` and `dp[mask][last]`. State which are feasible in 256 MB.
3. **Interview Core:** Implement minimum-cost assignment with bitmask DP. Then remove the unreachable-state guard and construct an input where the answer becomes negative.
4. **Interview Core:** Implement Held-Karp TSP. Report the wall time at `n = 12`, `16`, and `20`, and state where it stops being practical.
5. **Interview Core:** Prove that summing subset-of-subset enumeration over all masks is O(3^n), using the three-choices argument.
6. **Interview Core:** Implement digit DP counting integers in `[1, N]` whose digit sum is divisible by 7. Include the `tight` and `started` flags.
7. **Interview Core:** Memoize tight states in your digit DP and construct two different bounds that share a cache to show the wrong answer.
8. **SDE-2 Follow-up:** Implement all five stock variants from the table as one parameterized solution, and state which dimension each rule added.
9. **SDE-2 Follow-up:** Take the cooldown solution, update the variables in place rather than from a snapshot, and find the input where the profit becomes impossible.
10. **Challenge:** Given `n <= 18` items with weights and a target, count the number of distinct subsets summing to the target - first with bitmask enumeration, then with classic subset-sum DP. Compare complexities and say when each wins.

## Chapter summary

These three shapes cover the DP problems whose state is not an index. Bitmask DP encodes a subset in an integer and is gated hard by `n` around 20; its natural numeric iteration order is already a topological order, and its most common bug is relaxing from an unreachable sentinel that then overflows. It improves TSP from factorial to `O(2^n * n^2)` without making it polynomial, and saying so precisely matters more than the implementation. Digit DP walks a numeral left to right with a `tight` flag capping the current digit and a `started` flag handling leading zeros; only non-tight states may be memoized. Bounded-state DP adds a small counter - transactions, cooldown, budget - to an ordinary state, and the lesson from the stock family is that a new rule almost always becomes one more small dimension rather than a different algorithm, provided the transitions read a snapshot of the previous step rather than values already updated this one.

## Revision checklist

- [ ] I can state the three conditions that justify a bitmask state.
- [ ] I know the practical `n` ceiling and can compute the table size.
- [ ] I can explain why increasing numeric mask order is a valid evaluation order.
- [ ] I guard against relaxing from unreachable states and use half-range sentinels.
- [ ] I can state Held-Karp's complexity and that TSP remains NP-hard.
- [ ] I can justify the O(3^n) bound on subset-of-subset enumeration.
- [ ] I can explain the `tight` flag and why only non-tight states are memoized.
- [ ] I know why digit DP needs a `started` flag.
- [ ] I can extend a stock DP to k transactions, cooldown, and fees by adding dimensions.
- [ ] I snapshot previous state before updating multi-variable transitions.
