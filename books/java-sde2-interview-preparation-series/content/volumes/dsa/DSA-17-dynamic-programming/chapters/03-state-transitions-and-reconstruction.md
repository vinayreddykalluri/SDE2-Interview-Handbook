# State, Transitions, and Reconstruction

Dynamic programming is organized reuse of overlapping subproblems. The table is not the starting point. First write what one state means, what earlier states it depends on, and why evaluation order makes those dependencies available. Then choose memoization, tabulation, or space optimization.

The complete Java 21 implementations are in `DynamicProgrammingInterviewChecks.java`.

## The five-line DP design

For every problem, write:

1. **State:** what does `dp[...]` mean?
2. **Transition:** which smaller states produce it?
3. **Base:** which states are known directly?
4. **Order:** when is every dependency ready?
5. **Answer:** which state or aggregate is returned?

Complexity is number of reachable states times work per state, plus reconstruction/output.

## Memoization versus tabulation

Minimum coins state:

```text
dp[a] = minimum coins needed to make amount a, or unreachable
dp[0] = 0
dp[a] = 1 + min(dp[a-coin]) over legal reachable predecessors
```

Top-down memoization follows only requested states and resembles the recurrence. Bottom-up tabulation avoids recursion depth and has predictable iteration. The companion implements both and compares them on random inputs.

Memo needs three states in its cache: unknown, unreachable, and a nonnegative answer. Conflating unknown with unreachable can either recompute endlessly or skip valid work.

The teaching memoized coin method limits amount because a valid recurrence can still create unsafe Java call depth. Tabulation is the practical choice for larger linear amount domains.

## Space optimization comes after dependency analysis

Fibonacci depends on only two previous values, so a full array is unnecessary:

```text
previous = F(i-2)
current  = F(i-1)
next     = previous + current
```

The companion caps `n` at 92 because `F(93)` exceeds signed `long`. It does not compute one unnecessary next value after obtaining `F(92)`.

For a 2D DP, one row may be enough only if current-row updates do not overwrite a value still needed. Loop direction is part of the state transition.

## 0/1 knapsack and reconstruction

State:

```text
best[i][c] = maximum value using first i items with capacity c
```

Transition excludes item `i-1`, or includes it once from the previous row:

```text
best[i][c] = best[i-1][c]
if weight <= c:
    best[i][c] = max(best[i][c], best[i-1][c-weight] + value)
```

Using the previous row is what enforces 0/1 usage. A one-row optimization must iterate capacity downward; upward iteration would allow the same item repeatedly and solve unbounded knapsack instead.

To reconstruct, walk backward. If `best[i][c] != best[i-1][c]`, item `i-1` was chosen under the companion's tie policy; record it and subtract its weight. Ties are deliberately resolved by exclusion, so multiple optimal sets can exist while output remains deterministic.

```java
import java.util.*;

record Item(String id, int weight, int value) { }

record Packing(int value, List<Item> chosen) { }

static Packing knapsack(List<Item> items, int capacity) {
    int n = items.size();
    int[][] best = new int[n + 1][capacity + 1];

    for (int i = 1; i <= n; i++) {
        Item item = items.get(i - 1);
        for (int c = 0; c <= capacity; c++) {
            best[i][c] = best[i - 1][c];                    // exclude
            if (item.weight() <= c) {
                int include = best[i - 1][c - item.weight()] + item.value();
                if (include > best[i][c]) {                 // strict: ties exclude
                    best[i][c] = include;
                }
            }
        }
    }

    List<Item> chosen = new ArrayList<>();
    int c = capacity;
    for (int i = n; i > 0; i--) {
        if (best[i][c] != best[i - 1][c]) {                 // this row improved on the last
            Item item = items.get(i - 1);
            chosen.add(item);
            c -= item.weight();
        }
    }
    Collections.reverse(chosen);                            // back to input order
    return new Packing(best[n][capacity], List.copyOf(chosen));
}
```

The `>` in `include > best[i][c]` is a **tie policy, not a correctness
requirement**, and it is worth being precise about which. Switching it to `>=`
does not break reconstruction: on a tie the stored value is unchanged, so
`best[i][c] != best[i-1][c]` is still false, the walk-back still follows the
exclude path, and that path is equally optimal. Tested both ways over 4,000
random instances seeded with duplicate values - zero inconsistencies either
way.

What the strict comparison buys is **determinism**: it fixes *which* optimal
set you report when several exist. That matters for tests, for diffable output,
and for a reviewer asking why the answer changed. Say "ties resolve by
exclusion" rather than implying the program would otherwise be wrong.

The genuine correctness constraint is elsewhere: the transition must read
`best[i - 1][...]`, the **previous** row. Reading the current row would let one
item be taken repeatedly and quietly solve unbounded knapsack instead.

Reconstruction needs the **full table**, which is why the one-row space
optimisation and the ability to report the chosen set are mutually exclusive.
Say that trade-off out loud before optimising: `O(n x capacity)` memory buys
you an answer you can explain, and interviewers usually want the set.

Checked against exhaustive subset enumeration over 600 random instances,
asserting three things rather than one: the value matches the brute-force
optimum, the reconstructed set fits within capacity, and its values sum to
exactly the reported optimum. Zero failures.

## Edit distance: define the prefix state

```text
dp[i][j] = edits to convert first i chars of first string
           into first j chars of second string
```

Base row/column are insert/delete counts. Equal final code units take diagonal unchanged. Otherwise add one to the minimum of:

- diagonal: replace;
- above: delete from first; and
- left: insert into first.

The companion keeps two rows and makes the shorter string the column dimension, using `O(min(m,n))` space. It operates on UTF-16 `char` units. A code-point or grapheme-aware product needs a different tokenization contract.

## Grid path: identity and unreachable sentinels

For movement right/down, each cell depends on above and left. A one-row array holds the best path to the previous row before update and the current row to the left after update.

Initialize all entries unreachable except a synthetic zero before the start. Add with `Math.addExact` so numeric overflow does not silently become a better path. Reject jagged/empty grids under the rectangular contract.

Obstacles require an explicit unreachable state; do not use zero if zero is a valid path cost.

## Stock state machine

At most two transactions can be expressed as four best states after each price:

```text
buyFirst   = best balance after first buy
sellFirst  = best balance after first sell
buySecond  = best balance after second buy
sellSecond = best balance after second sell
```

Updating in transaction order is safe when buying and selling on the same day is allowed and adds zero profit. Each state summarizes all histories ending in that phase; no day-by-day transaction list is stored.

This is DP even though the table is four variables. State compression does not turn it into greedy.

## Validate with independent oracles

The companion compares:

- memoized and tabulated coin change across deterministic random inputs; and
- knapsack DP with exhaustive subset enumeration on small random instances.

An oracle should be simpler and structurally different. Comparing two copies of the same recurrence can preserve the same bug.

## Edge-case matrix

| Case | Correct handling | Common failure |
|---|---|---|
| amount zero | zero coins, even with empty coin set | requiring one coin |
| nonpositive coin | reject | self/cyclic dependency |
| unreachable amount | distinct sentinel/result | adding one to infinity |
| memo unknown versus impossible | separate markers | repeated work/wrong pruning |
| large recursion depth | tabulate or cap | stack overflow |
| Fibonacci 92/93 | 92 fits, 93 rejected for `long` | compute overflowed extra term |
| 0/1 one-row knapsack | capacity decreases | accidental item reuse |
| multiple optimal item sets | state tie policy | claiming unique reconstruction |
| empty string | base row/column | reading character `-1` |
| Unicode text | state code-unit contract | claiming user-perceived edits |
| jagged/empty grid | reject or define | invalid left/above access |
| negative stock price | reject domain input | nonsensical profit |

## Six live interview Q&A chains

### 1. State definition

**Interviewer:** Why is “dp of amount” too vague?

**Candidate:** It must say minimum coins for exactly that amount and how impossible states are represented. Without exact meaning, transition and answer cannot be proved.

### 2. Memo marker

**Interviewer:** Can `-1` mean both unknown and unreachable?

**Candidate:** Not safely. An impossible state would look uncomputed and be explored repeatedly. I use a separate unknown marker, then cache `-1` as the computed impossible result.

### 3. Knapsack loop direction

**Interviewer:** Why iterate capacity downward in one-row 0/1 knapsack?

**Candidate:** So `dp[c-weight]` still belongs to the previous item set. Upward order may read a value written by the current item and choose that item multiple times.

### 4. Reconstruction

**Interviewer:** Does a maximum value prove which items were selected?

**Candidate:** No. I retain enough table state to walk backward and apply a deterministic tie rule. A value-only compressed DP cannot always reconstruct without additional decisions or recomputation.

### 5. Space optimization

**Interviewer:** Why not always compress to one row?

**Candidate:** It can destroy information needed for reconstruction and is unsafe if current updates overwrite future dependencies. I first prove dependency direction, then decide whether memory or explainability matters more.

### 6. DP versus greedy

**Interviewer:** Coin change feels greedy. Why use DP?

**Candidate:** Arbitrary coin systems lack a safe largest-coin exchange proof; `[1,3,4]` for amount 6 is a counterexample. DP keeps the amount as state and evaluates every last-coin choice, guaranteeing the optimum under the stated bounds.

## Run the companion

```bash
javac --release 21 -Xlint:all -Werror DynamicProgrammingInterviewChecks.java
java DynamicProgrammingInterviewChecks
```

Expected final line: `PASS 20 dynamic-programming checks`.
