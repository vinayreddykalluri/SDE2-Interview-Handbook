# 5. Implicit Graphs and State-Space Search

## Why this chapter exists

The hardest graph interview questions usually do not mention graphs. Word ladder, open the lock, minimum genetic mutation, sliding puzzle, jump game with teleports, the water-jug problem, minimum knight moves - none of these hands you a vertex list. Each is a graph where **the vertices are configurations and the edges are legal moves**, and the entire difficulty is seeing that.

Once you see it, the algorithm is one you already know. BFS finds the fewest moves. Dijkstra finds the cheapest when moves have different costs. The work moves from choosing an algorithm to **modelling the state and controlling its size**.

Candidates who have practised traversal on adjacency lists but never on implicit graphs tend to freeze on these, then write a bespoke recursive search that is exponential and wrong. Recognizing the shape is the skill.

## First-principles model

An implicit graph is defined by a function rather than a data structure:

```text
explicit graph:   adj[u] is stored          -> read the neighbours
implicit graph:   neighbours(u) computed    -> generate the neighbours
```

Everything else about traversal is unchanged. The universal BFS invariant from chapter 1 still holds: **when a state is dequeued, its recorded distance is final**, provided every edge has the same cost.

Four questions replace the usual "build the adjacency list" step:

1. **What is a state?** The minimal information that determines the future. Not the path taken - only what constrains what happens next.
2. **What is the start, and what is the goal?** A goal may be a single state or a predicate.
3. **What are the legal moves?** The neighbour function.
4. **How large is the state space?** This is the feasibility check, and skipping it is the most common failure.

That fourth question is the one to answer out loud. `|V|` for a 4-dial lock is `10^4`; for word ladder it is the dictionary size; for an 8-puzzle it is `9!/2 = 181,440`; for a 15-puzzle it is about `10^13`, which is why the 15-puzzle needs A* and heuristics rather than plain BFS.

> **Specification boundary:** nothing here is a new algorithm. It is BFS, Dijkstra, and 0-1 BFS applied to a graph you generate rather than store. If your state space is too large to enumerate, no amount of BFS tuning saves you - the answer is a better state encoding, a heuristic search, or a different formulation.

## Designing the state

Getting the state wrong is the expensive error, and it fails in two directions.

**Too much state** blows up the space. Recording the full path taken multiplies the state count by the number of paths, which is usually exponential. If two different routes arrive at the same configuration, they must be the *same* state - that collapsing is what makes BFS polynomial instead of exponential.

**Too little state** makes the answer wrong. If a move's legality depends on something you did not record, BFS will happily revisit a configuration in a context where the move is no longer legal.

```text
Word ladder     state = current word                    (path not included)
Open the lock   state = 4-digit string                  (turn count not included)
Sliding puzzle  state = board configuration             (moves so far not included)
Keys and doors  state = (position, bitmask of keys held) (keys DO matter)
Bus routes      state = current route or stop           (choose carefully)
```

The keys-and-doors row is the instructive one. Position alone is insufficient because standing at a door with a key differs from standing at it without one. The key set is part of the state, and with `k <= 6` keys a bitmask makes `|V| = rows * cols * 2^k`, which is enumerable. This is the bitmask technique from the DP volume appearing as a graph state.

**Distance is never part of the state.** BFS tracks it separately. Folding it in makes every state unique and defeats the visited set entirely - a subtle bug that turns a linear search exponential while still producing correct answers on small inputs.

## The visited set is a correctness control, not an optimization

In an explicit graph, forgetting `visited` costs time. In an implicit graph with cycles, it costs termination.

**Mark visited when enqueuing, not when dequeuing.** Marking on dequeue lets the same state enter the queue several times before it is first processed, which inflates the queue and can be quadratic.

```java
// Correct: the state can never be enqueued twice.
if (visited.add(next)) {
    queue.add(next);
}
```

`Set.add` returning a boolean makes this a single operation, which is both faster and harder to get wrong than a separate `contains` check followed by an `add`.

The set's element type matters more than it looks. States are often strings or arrays; **arrays do not implement value equality**, so a `HashSet<int[]>` silently never matches and the search never terminates. Encode to a `String`, a `record`, or a packed `long` - anything with meaningful `equals` and `hashCode`.

## Worked example: open the lock

*A 4-dial lock starts at "0000". Each move turns one dial one step in either direction, with wraparound. Some codes are dead ends. Return the fewest moves to reach a target, or -1.*

**The four questions:** a state is the 4-character code; start is "0000"; the goal is the target string; moves are the eight one-dial turns; the space is `10^4 = 10,000` states with 8 edges each - trivially enumerable, so BFS is right.

```java
import java.util.*;

public final class OpenTheLock {

    static int openLock(String[] deadends, String target) {
        Set<String> blocked = new HashSet<>(Arrays.asList(deadends));
        if (blocked.contains("0000")) {
            return -1;                       // cannot even start
        }
        if (target.equals("0000")) {
            return 0;                        // check before the search, not inside it
        }

        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add("0000");
        visited.add("0000");
        int turns = 0;

        while (!queue.isEmpty()) {
            turns++;                          // one full level = one more move
            for (int level = queue.size(); level > 0; level--) {
                String code = queue.poll();
                for (String next : neighbours(code)) {
                    if (blocked.contains(next) || !visited.add(next)) {
                        continue;
                    }
                    if (next.equals(target)) {
                        return turns;
                    }
                    queue.add(next);
                }
            }
        }
        return -1;
    }

    /** The eight codes reachable by turning one dial one step. */
    private static List<String> neighbours(String code) {
        List<String> out = new ArrayList<>(8);
        char[] digits = code.toCharArray();
        for (int i = 0; i < 4; i++) {
            char original = digits[i];
            digits[i] = original == '9' ? '0' : (char) (original + 1);
            out.add(new String(digits));
            digits[i] = original == '0' ? '9' : (char) (original - 1);
            out.add(new String(digits));
            digits[i] = original;             // restore before the next dial
        }
        return out;
    }

    public static void main(String[] args) {
        System.out.println(openLock(
                new String[]{"0201", "0101", "0102", "1212", "2002"}, "0202")); // 6
        System.out.println(openLock(new String[]{"8888"}, "0009"));             // 1
        System.out.println(openLock(
                new String[]{"8887","8889","8878","8898","8788","8988","7888","9888"},
                "8888"));                                                       // -1
        System.out.println(openLock(new String[]{"0000"}, "8888"));             // -1
    }
}
```

Four details worth naming:

**The level-by-level loop.** Capturing `queue.size()` before the inner loop processes exactly one BFS level, so `turns` is the distance. Incrementing per *state* instead of per level is a classic wrong answer.

**Restoring `digits[i]`** before moving to the next dial. Without it, changes accumulate and the neighbour function generates codes two or three dials away - a bug that produces plausible but too-small answers.

**Checking the target on generation** rather than on dequeue returns one level earlier. Both are correct; this one is slightly faster and reads well.

**The `target.equals("0000")` guard.** Zero-move cases are the standard edge case, and handling them before the loop is cleaner than special-casing inside it.

## Bidirectional BFS

When both start and goal are known and the branching factor `b` is uniform, searching from both ends and meeting in the middle reduces the frontier from `O(b^d)` to `O(b^(d/2))`.

```text
one-directional:   b^d          e.g. 8^6  = 262,144
bidirectional:   2 * b^(d/2)    e.g. 2*8^3 =   1,024
```

The technique: keep two frontier sets, always expand the smaller one, and stop when a generated state appears in the other frontier.

It applies only when the goal is a *specific state* you can search backward from. A goal expressed as a predicate - "any configuration where all lights are off" - has no single backward start, and bidirectional search does not apply. Naming that condition is the interview signal; reciting the speedup is not.

For word ladder and open-the-lock it is a genuine and large improvement, and it is the standard follow-up once the plain BFS is working.

## When the moves have costs

If every move costs the same, BFS is correct and O(V + E). Once costs differ, the dequeue-is-final invariant breaks and you need the right tool:

| Move costs | Algorithm | Note |
|---|---|---|
| All equal | BFS | Queue; distance is level number |
| Only 0 and 1 | 0-1 BFS | Deque; push-front on cost 0, push-back on cost 1 |
| Arbitrary nonnegative | Dijkstra | Priority queue over states |
| Some negative | Bellman-Ford | Rare in state-space problems |
| Equal cost + good heuristic | A* | Needed once the space is too large to enumerate |

0-1 BFS is worth remembering here because it appears naturally in grid problems where some moves are free - "minimum obstacles to remove", "minimum cost to make a path" - and it is O(V + E) rather than Dijkstra's O(E log V) with no heap at all.

## Edge cases and common mistakes

- Not recognizing the problem as a graph and writing a bespoke exponential recursion.
- Including the path or the move count in the state, so no two states ever collapse.
- Omitting information a move depends on - keys held, fuel remaining - so the search is wrong.
- Never computing the state-space size, then running BFS over something unenumerable.
- Using `HashSet<int[]>`; arrays have identity equality, so nothing ever matches and BFS never terminates.
- Marking visited on dequeue instead of on enqueue, inflating the queue.
- Incrementing the distance per state rather than per BFS level.
- Forgetting to restore mutated scratch state inside the neighbour function.
- Not handling start equal to goal, or a blocked start.
- Applying bidirectional BFS when the goal is a predicate rather than a specific state.
- Using BFS when move costs differ; the dequeue-is-final invariant no longer holds.
- Generating neighbours that leave the legal domain - off-grid, out-of-range, malformed - and relying on the visited set to absorb them.

## Interview questions and model answers

**How do you recognize a state-space graph problem?**

The prompt asks for the fewest moves, steps, or transformations between configurations, and never supplies a vertex or edge list. Vertices are configurations, edges are legal moves, and the neighbour function replaces the adjacency list. Word ladder, open the lock, sliding puzzles, and minimum knight moves are all this shape.

**How do you choose the state?**

The minimal information that determines what may happen next. Not the path, since two routes reaching the same configuration must collapse to one state - that collapsing is what keeps BFS polynomial. But everything a move's legality depends on must be included, which is why keys-and-doors needs position plus a bitmask of keys held. Then compute the resulting space size to check the search is feasible at all.

**Why must the visited set be marked on enqueue?**

Marking on dequeue lets the same state be enqueued many times before it is first processed, inflating the queue and the work. Marking on enqueue guarantees each state enters once. `visited.add(next)` returning a boolean does the check and the mark in one operation.

**Your BFS over board states never terminates. What would you check first?**

The visited set's element type. If states are `int[]`, a `HashSet<int[]>` compares by identity, so no state is ever recognized as seen and cycles re-enter forever. Encode to a string, a record, or a packed long with real value equality.

**When does bidirectional BFS help, and when does it not?**

It helps when both endpoints are specific known states and the branching factor is roughly uniform, reducing the frontier from `b^d` to about `2*b^(d/2)`. It does not apply when the goal is a predicate rather than a single state, because there is nothing to search backward from.

**The moves now have different costs. What changes?**

BFS's invariant that a dequeued state has its final distance only holds for uniform costs. With costs of only 0 and 1, use 0-1 BFS on a deque - push-front for 0, push-back for 1 - which stays O(V + E). With arbitrary nonnegative costs, use Dijkstra with a priority queue over states.

## Exercises

1. **Foundation:** For word ladder, open the lock, and the 8-puzzle, write the state, start, goal, moves, and state-space size.
2. **Foundation:** Explain why including the path in the state makes BFS exponential, with a small concrete example.
3. **Interview Core:** Implement open the lock. Then remove the `digits[i] = original` restore line and describe the wrong answers produced.
4. **Interview Core:** Implement word ladder with a wildcard-bucket neighbour function, and state the complexity in terms of word length and dictionary size.
5. **Interview Core:** Solve the keys-and-doors grid with a `(position, keyMask)` state. Compute the state-space size for a 30x30 grid with 6 keys.
6. **Interview Core:** Store board states in a `HashSet<int[]>`, observe non-termination, then fix it with three different encodings and compare their speed.
7. **SDE-2 Follow-up:** Add bidirectional BFS to your open-the-lock solution and measure states expanded on a distance-6 target.
8. **SDE-2 Follow-up:** Solve "minimum obstacles to remove to reach the corner" with 0-1 BFS, then with Dijkstra, and compare runtimes.
9. **SDE-2 Follow-up:** Take a BFS that marks visited on dequeue and construct the input where the queue grows quadratically.
10. **Challenge:** Solve the 8-puzzle with BFS, report states expanded, then explain with numbers why the 15-puzzle needs A*.

## Chapter summary

The hardest graph problems in interviews do not look like graph problems: vertices are configurations, edges are legal moves, and the adjacency list is replaced by a neighbour function you write. The algorithm is unchanged - BFS for uniform move costs, 0-1 BFS when costs are only zero and one, Dijkstra when they are arbitrary - so the work moves entirely into modelling. State must contain everything a move's legality depends on and nothing else: never the path or the step count, because two routes reaching one configuration have to collapse for the search to stay polynomial, and always the extras a move needs, such as a bitmask of keys held. Compute the state-space size before writing anything, because that number decides whether BFS is viable or whether the problem needs A*. The visited set is a termination control rather than an optimization: mark on enqueue, and make sure the element type has real value equality, since a `HashSet<int[]>` compares identities and lets a cyclic search run forever.

## Revision checklist

- [ ] I recognize the implicit-graph shape from a "fewest moves" prompt with no edge list.
- [ ] I can answer the four modelling questions before writing code.
- [ ] I always compute the state-space size and use it to justify the algorithm.
- [ ] I exclude path and distance from state, and include everything a move depends on.
- [ ] I can explain why keys-and-doors needs a bitmask in the state.
- [ ] I mark visited on enqueue and know why dequeue is worse.
- [ ] I never put a raw array in a hash set, and can name three working encodings.
- [ ] I increment distance per BFS level, not per state.
- [ ] I can state when bidirectional BFS applies and when it cannot.
- [ ] I can pick between BFS, 0-1 BFS, and Dijkstra from the move-cost structure.
