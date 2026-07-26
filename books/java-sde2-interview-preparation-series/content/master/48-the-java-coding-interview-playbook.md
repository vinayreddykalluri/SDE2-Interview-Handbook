# 48. The Java Coding Interview Playbook

## Learning objectives

By the end of this chapter, you should be able to:

- run a repeatable control loop from clarification through final complexity;
- convert examples and constraints into a baseline, pattern, invariant, and proof;
- communicate while coding without narrating every keystroke;
- write compact, compilable Java 21 with deliberate numeric and collection choices;
- test with a boundary matrix and recover systematically from defects; and
- handle optimization, follow-ups, and production questions at SDE-2 depth.

## Why this matters at SDE-2

Knowledge does not automatically become interview performance. Time pressure causes candidates to skip the contract, silently assume input properties, start coding a half-remembered pattern, and discover an off-by-one error near the end. A playbook preserves reasoning bandwidth.

The interviewer is evaluating several channels at once: correctness, problem decomposition, complexity, communication, code quality, testing, and response to feedback. A candidate who reaches code slightly later but keeps the solution controlled often performs better than one who types immediately and debugs by instinct.

This chapter is not a script to recite. It is a feedback loop. Each artifact - contract, example, baseline, invariant, dry run, and complexity statement - makes the next decision safer and gives the interviewer a chance to correct a misunderstanding early.

> **Focused practice path:** The 18-stage companion series turns this playbook into shorter topic loops. Begin with the first volume whose completion check is not yet automatic, and use Stage 18G for question-bank, mock-loop, and final revision work.

## First-principles model

A coding interview is a constrained engineering session. The problem statement defines an incomplete contract. Input bounds create a performance budget. The solution is a claim that an implementation satisfies the clarified contract within that budget. Evidence consists of an invariant or recurrence, a dry run, tests, and complexity analysis.

Treat the session as a control system:

```text
observe requirements -> propose model -> receive feedback -> implement
         ^                                         |
         +----------- trace and verify <-----------+
```

At every stage, maintain a usable fallback. The brute-force baseline is not wasted time: it validates the target behavior, reveals repeated work, and can become a test oracle. If optimization fails, a correct partial solution with honest analysis is better than an unverified sophisticated one.

> **Specification boundary:** Java guarantees defined language and library behavior, but not the judge's memory limit, stack size, default input constraints, or expected method signature. Those belong to the interview contract and must be clarified or stated as assumptions.

## Core terminology

- **Clarifying question:** resolves an ambiguity that can change correctness or design.
- **Constraint pressure:** reason the baseline fails at stated input scale.
- **Pattern signal:** structural property suggesting a technique, not proof by keyword.
- **Invariant:** statement connecting variables to the processed and unprocessed state.
- **Oracle:** trusted implementation or property used to verify another result.
- **Boundary matrix:** compact set of tests covering shape, value, and behavioral extremes.
- **Counterexample:** input disproving a proposed rule or invariant.
- **Follow-up:** changed constraint requiring adaptation or trade-off discussion.
- **Recovery loop:** reproduce, localize invariant violation, repair, and retrace.
- **Closeout:** final proof, complexity, mutation, edge cases, and alternative summary.

## Detailed mechanics

### A 45-minute operating rhythm

Exact interview lengths vary, but a time budget prevents uncontrolled drift:

| Phase | Approximate time | Deliverable |
|---|---:|---|
| Clarify and examples | 4-6 minutes | Contract and expected cases |
| Baseline and optimization | 5-8 minutes | Costed alternatives and selected approach |
| Invariant and pseudocode | 3-5 minutes | Proof skeleton |
| Java implementation | 15-20 minutes | Compilable solution |
| Dry run and tests | 5-7 minutes | Corrected boundary behavior |
| Follow-ups and close | Remaining | Complexity and trade-offs |

Do not obey the table mechanically. A familiar problem can move faster; a modeling-heavy graph problem deserves more clarification. The important discipline is noticing when ten silent minutes have passed without a verified artifact.

### Step 1: clarify the contract

Ask questions that can change the algorithm:

- Can input be null or empty?
- What are n and value ranges?
- Are values sorted, unique, positive, or immutable?
- Do duplicates represent separate items?
- May the method mutate or reorder input?
- Which output is required when several are valid?
- Is there always a solution? What represents absence?
- Are indexes, values, paths, counts, or only an optimum required?
- What does "character," interval overlap, or distance mean?

Avoid spending time on irrelevant possibilities. If the interviewer says to choose reasonable assumptions, state them aloud and continue: "I will treat input as non-null, allow an empty array, and return -1 when infeasible."

Restate the contract in one sentence. This creates an early synchronization point.

### Step 2: construct examples

Use one normal example, one smallest case, and one adversarial case tied to a likely mistake. For a window, include a case that shrinks multiple times. For a BST, include a deep ancestor violation. For Dijkstra, include a stale priority-queue entry. For duplicates, include equal values on both sides of a boundary.

Calculate expected output manually before coding. If that is difficult, the contract is not yet understood. Do not rely exclusively on the sample supplied by the interviewer; samples often avoid exactly the boundary that breaks a memorized template.

### Step 3: establish and pressure-test a baseline

Describe the direct correct approach in enough detail to cost it. For example: enumerate every subarray, calculate each sum incrementally, and track the best, O(n squared) time and O(1) space. Then compare with constraints. At n = 200, it may be fine. At n = 200,000, it is not.

Name eliminated work:

- repeated membership test -> hash set;
- repeated range aggregate -> prefix sum;
- repeated valid-range rescan -> sliding window;
- repeated minimum selection -> heap;
- repeated subproblem -> memoization/DP;
- exhaustive ordered boundary -> binary search;
- repeated connectivity traversal -> DSU.

This explanation is stronger than saying "I know this pattern." It shows why the representation pays for itself.

### Step 4: select a pattern and state its preconditions

Use a decision map, then check its proof obligation:

| Need | Candidate | Must be true |
|---|---|---|
| Complement/frequency/history | Hashing | Equality and memory acceptable |
| Eliminate ordered candidates | Two pointers | Pointer movement is monotone and safe |
| Maintain contiguous valid region | Window | State updates and validity permit shrinking |
| Ordered boundary/answer | Binary search | Predicate is monotone |
| Nearest in edge count | BFS | Edges have equal effective weight |
| Next greater/smaller | Monotonic stack | Popped candidate is permanently dominated |
| Best k/frontier minimum | Heap | Only priority head is required |
| Repeated optimal states | DP | State is sufficient; transitions complete |

If a precondition fails, do not force the pattern. Negative numbers can invalidate a sum window. Negative edges invalidate Dijkstra. A non-monotone feasibility test invalidates binary search.

### Step 5: articulate the invariant before code

An interview invariant should be operational, not philosophical:

- "At loop start, indexes below `low` are infeasible and indexes at or above `high` are feasible."
- "The map counts prefix sums strictly before the current prefix."
- "The deque contains unexpired indexes in increasing index and decreasing value order."
- "`dp[a]` is the minimum number of coins for exact amount a, or impossible."

Also state progress: right advances, candidate interval shrinks, one node moves from suffix to prefix, or every recursive call reduces remaining work.

Write short pseudocode when it helps validate phase order. For mutation-heavy logic, draw state. For recursion, write the return contract and base case before the method body.

### Step 6: implement interview-grade Java

Prefer Java that makes the proof visible:

- semantic names such as `left`, `rightExclusive`, `indegree`, and `prefixFrequency`;
- half-open intervals where they simplify boundaries;
- `long` before potentially overflowing arithmetic;
- `ArrayDeque` for stacks and queues;
- `PriorityQueue` with `Integer.compare`, `Long.compare`, or comparator factories;
- records for small immutable state pairs;
- arrays for dense integer domains and maps for sparse domains;
- helper methods where they isolate a predicate or traversal contract.

Do not build a production framework during a 40-minute exercise. Avoid streams when a stateful loop is easier to trace. Avoid clever one-liners, raw types, mutable static fields, and subtraction comparators. A local nested record is often clearer than parallel arrays, but check what the interview environment supports.

Compilation is a separate correctness layer. Scan imports, generic types, return paths, method names, and static context. If no IDE is available, mentally compile one declaration at a time.

### Step 7: dry run by invariant

Do not narrate only values. At each iteration, verify the invariant and boundary updates. Use a small table with the variables that prove correctness. For recursive code, draw the first few frames and show state restoration. For a heap or deque, show contents in logical order, not internal library array order.

Select a case that activates the difficult branch. Testing a sliding window that never shrinks or a graph without a cycle proves little.

### Step 8: use a boundary matrix

Cover categories rather than random anecdotes:

| Dimension | Cases |
|---|---|
| Size | empty, one, two, typical, maximum shape |
| Values | zero, negative, duplicates, min/max numeric |
| Position | answer at start, middle, end, absent |
| Structure | sorted/reversed, skewed tree, disconnected graph, cycle |
| Behavior | impossible, multiple valid answers, all qualify, none qualify |

Trace two or three high-yield cases aloud. Mention additional cases you would automate. If time permits, compare optimized output against the baseline on small generated inputs; this property-based oracle catches many pointer and DP defects.

### Step 9: recover from a bug

Do not patch randomly. Use this loop:

1. Freeze a minimal failing input.
2. Identify the first step where actual state violates the invariant.
3. Classify the cause: initialization, update order, boundary, stale state, overflow, or contract mismatch.
4. Make the smallest coherent correction.
5. Restart the trace from initialization.
6. Recheck complexity and neighboring boundary cases.

Say what you found. "I am expiring the deque after reading its front, so an old maximum leaks into the answer. I will expire before reporting." This demonstrates control rather than panic.

### Step 10: close and handle follow-ups

End with:

- time in named dimensions and any expected/amortized qualifier;
- auxiliary, recursion-stack, and output space;
- whether input is mutated;
- the critical precondition and invariant;
- one alternative and when it is preferable; and
- production constraints if asked.

When a follow-up changes one constraint, identify which proof breaks before changing code. If input becomes streaming, random access disappears. If memory becomes O(1), hashing may no longer fit. If negatives are allowed, window monotonicity can fail. This approach prevents cargo-cult adaptation.

## Worked Java example

Problem: each positive workload takes `ceil(work / rate)` hours at integer processing rate. Workloads are handled sequentially, and each nonempty workload consumes at least one hour. Return the minimum rate that completes all work within `hourLimit`, or -1 when impossible.

```java
public final class MinimumProcessingRate {
    public static int minimumRate(int[] workloads, int hourLimit) {
        if (workloads == null) throw new IllegalArgumentException("null workloads");
        if (workloads.length == 0) return 0;
        if (hourLimit < workloads.length) return -1;

        int high = 0;
        for (int workload : workloads) {
            if (workload <= 0) {
                throw new IllegalArgumentException("workloads must be positive");
            }
            high = Math.max(high, workload);
        }

        int low = 1;
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (finishesWithin(workloads, hourLimit, mid)) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    private static boolean finishesWithin(
            int[] workloads, int hourLimit, int rate) {
        long hours = 0;
        for (int workload : workloads) {
            hours += (workload + (long) rate - 1) / rate;
            if (hours > hourLimit) return false;
        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println(minimumRate(new int[] {3, 6, 7, 11}, 8)); // 4
        System.out.println(minimumRate(new int[] {5}, 1));           // 5
        System.out.println(minimumRate(new int[] {2, 3}, 1));        // -1
        System.out.println(minimumRate(new int[] {}, 0));            // 0
    }
}
```

The feasibility predicate is monotone: if rate r finishes in time, every larger rate also finishes in time. The answer is therefore the first true rate in `[1, maxWorkload]`.

## Execution or memory walkthrough

For workloads `[3,6,7,11]` and limit 8, initialize candidate range `[1,11]` with both endpoints represented by `low` and `high`. Rate 11 is feasible because each workload takes one hour.

| Step | `low` | `high` | `mid` | Hours needed | Update |
|---|---:|---:|---:|---:|---|
| 1 | 1 | 11 | 6 | 6 | `high = 6` |
| 2 | 1 | 6 | 3 | 10 | `low = 4` |
| 3 | 4 | 6 | 5 | 8 | `high = 5` |
| 4 | 4 | 5 | 4 | 8 | `high = 4` |
| End | 4 | 4 | - | - | Return 4 |

Invariant: every rate below `low` is infeasible, and `high` is feasible; the first feasible rate remains in `[low, high]`. A feasible mid can still be larger than minimum, so retain it by setting `high = mid`. An infeasible mid is excluded with `low = mid + 1`. The interval strictly shrinks.

The ceiling formula promotes before addition to avoid `int` overflow. Early exit in the predicate preserves correctness because hours only increase. The empty-input behavior and impossible case are explicit contract choices rather than accidental loop results.

## Complexity and performance

Let n be workload count and M the maximum workload. Feasibility is O(n) time and O(1) space. Binary search performs O(log M) predicate calls, so total time is O(n log M) and auxiliary space is O(1). The value-domain logarithm is distinct from O(log n).

A brute-force scan of rates is O(nM) and is a useful conceptual oracle for small M. Sorting does not help because every feasibility check needs the aggregate work across all workloads. Precomputing cannot remove dependence on rate without a more specialized constraint set.

| Interview choice | Benefit | Cost or risk |
|---|---|---|
| Baseline first | Correctness anchor and oracle | Uses a few minutes |
| Helper predicate | Isolates monotonic proof | Extra method boundary |
| `long` aggregate | Prevents realistic overflow | Must promote before arithmetic |
| Early predicate exit | Less work on infeasible rates | Does not change worst-case bound |
| Half-open/closed convention | Prevents boundary drift | Must remain consistent |

> **HotSpot note:** HotSpot may inline the feasibility helper and optimize the primitive loop. Such optimization changes constants, not the O(n log M) algorithm or the need for overflow-safe arithmetic.

## Edge cases and common mistakes

- Coding before deciding null, empty, impossible, and mutation behavior.
- Asking many questions that cannot affect the algorithm while missing value ranges.
- Quoting a pattern without stating its preconditions.
- Giving an optimized idea without a correct baseline or proof.
- Using sample input as the only test.
- Mixing `int` and `long` so overflow occurs before widening.
- Writing ceiling division as floating point and introducing precision or conversion issues.
- Failing to establish that the binary-search predicate is monotone.
- Mixing first-true updates with any-match loop conditions.
- Narrating syntax instead of decisions and invariants.
- Going silent after finding a bug and applying unexplained patches.
- Claiming success without tracing the branch that mutates or shrinks state.
- Reporting O(log n) when the search dimension is a value M.
- Forgetting stack or output space.
- Overengineering interfaces and classes that do not help solve or verify the problem.
- Treating interviewer feedback as a command to patch rather than evidence to re-evaluate the model.

## Production engineering notes

Interview code optimizes for a short, isolated demonstration. Production code needs input limits, observability, cancellation, API error types, test suites, documentation, and ownership. Do not paste a coding solution directly into a service boundary.

Numeric models deserve special attention. A workload might exceed `int`, processing might be parallel rather than sequential, or hour limits might use time zones and partial units. Clarify units and use domain types where appropriate. The worked predicate assumes deterministic integer work and no setup cost.

For large inputs, benchmark the actual representation. A map-heavy expected O(n) solution may exceed memory. A recursive DFS may overflow a worker stack. A returned list may be the dominant allocation. State these constraints in an SDE-2 follow-up without derailing the initial interview solution.

Interview practice should be measurable. Track failure categories: contract, recognition, proof, coding, boundary, complexity, and communication. Re-solving ten problems without classifying mistakes is less useful than repairing one repeated invariant failure.

## Interview questions and model answers

**Should I start coding immediately if I recognize the problem?**

No. Spend a short, bounded period confirming the contract, constraints, and expected output. Then state the approach and invariant. Recognition accelerates reasoning but does not replace proof or boundary checks.

**How much should I talk while coding?**

Explain decisions, invariant-changing updates, and trade-offs. Do not narrate punctuation. Brief silence while implementing a stated plan is fine; resynchronize when entering a tricky branch or after changing the plan.

**What if I cannot find the optimal solution?**

Present a correct baseline, analyze its bottleneck, and explore one optimization at a time. Ask for a constraint hint if appropriate. A verified suboptimal solution plus clear progress is stronger than speculative code.

**What should I do when the interviewer finds a counterexample?**

Restate the failing case, find the first invariant violation, explain which assumption failed, repair the model, and retrace. Do not defend the old approach or patch only the observed output.

**How do I know binary search on the answer is valid?**

Define a totally ordered candidate domain and a feasibility predicate. Prove all candidates on one side are false and all on the other side true. Then search explicitly for the first true or last false boundary.

**What distinguishes an SDE-2 closeout?**

It includes named dimensions, qualified time, auxiliary/stack/output space, mutation, critical invariant, edge cases, and a credible alternative. It can also discuss scale, overflow, concurrency, or API contracts when prompted.

## Exercises

1. Run the ten-step playbook on two-sum, recording no more than one sentence per step.
2. Solve a variable-window problem and design a test that forces several left-pointer moves in one iteration.
3. Use an O(n squared) oracle to property-test an O(n) prefix-hash solution on small random arrays.
4. Conduct a 45-minute graph mock and record time spent in each phase.
5. Deliberately inject an off-by-one error into lower bound, then practice the recovery loop aloud.
6. Adapt `minimumRate` when workloads may be zero and when work can be split across workers; identify which assumptions change.
7. Take one recent failed problem and classify the root cause as contract, model, invariant, implementation, or testing.
8. Practice three follow-ups: forbid extra space, make input streaming, and require all valid outputs.

## Chapter summary

A reliable coding interview is a controlled engineering loop. Clarify the contract, compute examples, establish a costed baseline, name eliminated work, verify pattern preconditions, state an invariant and progress measure, implement readable Java, trace a difficult branch, test a boundary matrix, and close with complete complexity. When a defect appears, localize the first invariant violation rather than patching randomly. Follow-ups change constraints; identify which proof breaks before adapting code.

## Revision checklist

- [ ] I can restate null, empty, duplicate, mutation, and absence behavior.
- [ ] I create a normal, smallest, and adversarial example before coding.
- [ ] I state a correct baseline and the repeated work being eliminated.
- [ ] I verify pattern preconditions rather than matching keywords.
- [ ] I articulate an operational invariant and progress measure.
- [ ] I use semantic Java names, safe arithmetic, and appropriate collections.
- [ ] I trace the difficult branch and use a boundary matrix.
- [ ] I recover by finding the first invariant violation.
- [ ] I report time, auxiliary space, stack, output, and mutation.
- [ ] I identify which assumption a follow-up changes before editing code.
