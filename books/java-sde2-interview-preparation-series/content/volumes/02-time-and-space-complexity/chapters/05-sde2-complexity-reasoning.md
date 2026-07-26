# SDE-2 Complexity Reasoning and the Optimization Method

## Learning objectives

By the end of this chapter, you should be able to:

- derive time and space complexity from executed work rather than loop appearance;
- distinguish worst-case, expected, amortized, and output-sensitive costs;
- state preconditions, postconditions, loop invariants, and progress measures;
- move from a correct baseline to an optimized algorithm systematically; and
- communicate a complete SDE-2 solution, including trade-offs, proof, tests, and production limits.

## Why this matters at SDE-2

Coding interviews at this level do not reward pattern recall alone. The interviewer is looking for controlled reasoning under incomplete requirements. Can you expose an ambiguity before it becomes a bug? Can you justify discarding part of the search space? Can you separate an algorithmic bound from Java implementation costs? Can you recover when the first approach is too slow?

The same skill appears in production. A service incident might require estimating whether a loop is linear in records, quadratic in tenant pairs, or bounded by output. A design review might hinge on whether memory is O(n) per request or O(n) for the whole process. Complexity vocabulary is useful only when tied to named inputs and an execution model.

> **Learning-path position:** This chapter converts the foundations from Chapters 1-4 into an SDE-2 explanation method. After completing this volume, continue to Number Systems Volume 01 for numeric representations, overflow boundaries, modular arithmetic, powers, roots, and the mathematical tools used by later DSA books.

## First-principles model

An algorithm transforms inputs satisfying preconditions into an output satisfying postconditions. Correctness has two parts: partial correctness, meaning the promised result holds if the algorithm terminates, and termination, meaning it eventually stops for every valid input.

An invariant is a statement true at a defined program point before and after each iteration or recursive expansion. A progress measure moves toward a bound. Together they turn intuition into a proof. For example, binary search does not work because it "checks the middle." It works because the answer remains inside a maintained interval and each comparison safely removes a region that cannot contain it.

Complexity describes resource growth as input dimensions grow. It ignores many machine constants to compare scalability, but it does not erase them from engineering. First derive the asymptotic model. Then discuss allocations, cache locality, hashing behavior, boxing, and expected data sizes.

> **Specification boundary:** Big-O is a mathematical bound, not a Java or JVM guarantee about elapsed time. Java specifies observable program behavior; it does not promise a particular instruction count, cache behavior, object size, or JIT optimization for a source-level operation.

## Core terminology

- **Input dimension:** independent size variable such as `n` records, `m` queries, or `V` vertices and `E` edges.
- **Big-O:** asymptotic upper bound.
- **Big-Omega:** asymptotic lower bound.
- **Big-Theta:** matching upper and lower bound.
- **Worst case:** maximum cost among inputs of a given size.
- **Expected case:** average over a stated randomness or input distribution.
- **Amortized cost:** total cost of an operation sequence divided across that sequence, without assuming random inputs.
- **Auxiliary space:** extra working storage excluding input and usually output.
- **Output-sensitive:** cost includes the size of the result, such as O(n + k) for k matches.
- **Invariant:** property preserved at a chosen control point.
- **Progress measure:** quantity that moves monotonically toward termination.
- **Baseline:** simplest clearly correct approach used to expose structure and constraints.

## Detailed mechanics

### A defensible complexity analysis

Start by naming dimensions. If one loop reads `users` and another reads `orders`, call the sizes `u` and `o`; do not collapse them into `n` unless the relationship is known. Count a meaningful dominant operation, form a sum, and simplify afterward.

Sequential phases add. Sorting and then scanning is O(n log n + n), which simplifies to O(n log n). Independent nested loops multiply: comparing every user with every role is O(ur). Dependent loops require a sum. This loop is O(n), not O(n squared), because `right` only advances:

```java
for (int left = 0, right = 0; right < values.length; right++) {
    while (left <= right && windowIsInvalid(left, right)) {
        left++;
    }
}
```

Across the whole execution, each pointer moves at most n times. Aggregate pointer movement is often the right unit for windows, monotonic structures, and union-find analyses.

Halving a remaining range produces logarithmic depth: after k halvings, size is approximately n / 2^k, so reaching one requires k near log2(n). A balanced divide-and-conquer algorithm that does linear work at each of log n levels is O(n log n). Recursive analysis must include both number of calls and work per call; `T(n) = 2T(n/2) + O(n)` differs from `T(n) = T(n/2) + O(1)`.

Space analysis separates several categories:

| Category | Question | Example |
|---|---|---|
| Input | What storage already exists? | Supplied `int[]` |
| Auxiliary | What new working state is needed? | Hash map of n entries |
| Call stack | How deep can recursion become? | O(h) tree height |
| Output | How large must the result be? | O(k) returned matches |
| Hidden library work | Does an API copy or box? | Sorting objects, `substring`, streams |

An "in-place" recursive algorithm may still use O(n) call-stack space. Returning all pairs cannot use less than O(k) output storage if k pairs are materialized.

### Worst, expected, and amortized claims

State the qualifier. Hash-table operations are commonly expected O(1) under suitable hashing and load, not an unconditional mathematical constant for every possible collision pattern. Dynamic-array append is amortized O(1): rare O(n) resize copies are paid for across many cheap appends. Randomized quickselect is expected O(n) with an appropriate pivot strategy but has a quadratic worst case.

Do not call amortized analysis "average case." Amortization holds across any operation sequence covered by its potential or aggregate proof; expected analysis depends on probability.

### The SDE-2 control loop

Use the following repeatable sequence:

1. **Contract:** restate inputs, outputs, mutability, null policy, duplicates, ordering, ranges, and failure behavior.
2. **Examples:** create a normal case and an adversarial boundary. Calculate expected output manually.
3. **Baseline:** describe a correct direct solution and its cost. This establishes a fallback.
4. **Constraint pressure:** compare baseline cost with input bounds. Name the repeated or unnecessary work.
5. **Representation:** choose the state needed to avoid that work: hash map, pointer boundary, prefix state, heap, graph frontier, or DP table.
6. **Invariant:** say what every variable or data structure means at a stable point.
7. **Progress:** show why each iteration or recursive call approaches completion.
8. **Implementation:** use names that encode the proof and keep the main path visible.
9. **Trace:** execute the code on a boundary and a representative case.
10. **Close:** state time, auxiliary space, output space, mutation, and relevant alternatives.

This is not ceremony. Each step catches a different failure class. The contract catches wrong problems, the baseline catches unjustified optimization, the invariant catches logic errors, and the trace catches boundary defects.

### Invariant proof template

For a loop, answer three questions:

- **Initialization:** why is the invariant true before the first iteration?
- **Maintenance:** assuming it is true, why does one iteration preserve it?
- **Termination:** when the loop stops, how does the invariant imply the postcondition?

Then give a variant or progress measure that is bounded and changes every continuing iteration. This proof style works for partitioning, two pointers, BFS layers, heap selection, and DP fill order.

### Binary search as a reasoning template

Binary search applies to a monotone predicate, not only to equality in a sorted array. Define a search interval consistently. A half-open interval `[low, high)` is often easiest because its size is `high - low`, empty means `low == high`, and `high` can be `n` for insertion at the end.

For lower bound, maintain:

- every index before `low` has value less than target;
- every index at or after `high` has value at least target; and
- the first qualifying index remains in `[low, high)`.

Use `mid = low + (high - low) / 2` and update one boundary to exclude `mid`; otherwise the interval may not shrink.

## Worked Java example

The following Java 21 program returns the first index whose value is at least the target. It returns `values.length` when no element qualifies.

```java
import java.util.Arrays;

public final class LowerBound {
    public static int lowerBound(int[] values, int target) {
        int low = 0;
        int high = values.length;

        while (low < high) {
            int mid = low + (high - low) / 2;
            if (values[mid] < target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private static void requireSorted(int[] values) {
        for (int i = 1; i < values.length; i++) {
            if (values[i - 1] > values[i]) {
                throw new IllegalArgumentException("array must be sorted");
            }
        }
    }

    public static void main(String[] args) {
        int[] values = {1, 3, 3, 7, 9};
        requireSorted(values);
        for (int target : new int[] {0, 3, 4, 10}) {
            System.out.printf("target=%d index=%d%n",
                    target, lowerBound(values, target));
        }
        System.out.println(Arrays.toString(values));
    }
}
```

The sortedness check is useful at an external boundary but would change a single search from O(log n) to O(n). In an interview, state whether sorted input is a trusted precondition instead of silently validating it.

## Execution or memory walkthrough

Trace `lowerBound([1, 3, 3, 7, 9], 3)`:

| Step | `low` | `high` | `mid` | `values[mid]` | Update |
|---|---:|---:|---:|---:|---|
| Start | 0 | 5 | - | - | Candidate interval `[0, 5)` |
| 1 | 0 | 5 | 2 | 3 | `high = 2` |
| 2 | 0 | 2 | 1 | 3 | `high = 1` |
| 3 | 0 | 1 | 0 | 1 | `low = 1` |
| End | 1 | 1 | - | - | Return 1 |

At each start, indices below `low` are proven too small, indices at or above `high` are proven qualifying, and the boundary lies between them. Each iteration strictly reduces `high - low`. At termination, no candidate interval remains; `low` is the unique partition point.

The method stores four primitive locals and uses no recursion, so auxiliary space is O(1). It does not mutate the input. The JVM may place locals in registers after compilation, but that does not change the source-level space analysis.

## Complexity and performance

| Pattern | Time | Auxiliary space | Important qualifier |
|---|---:|---:|---|
| Full scan | O(n) | O(1) | May stop early, worst case remains n |
| Compare all pairs | O(n^2) | O(1) | Output may itself be quadratic |
| Sort then scan | O(n log n) | Depends on sort | Mutation and stability matter |
| Binary search | O(log n) | O(1) iterative | Requires monotone search space |
| Hash lookup sequence | Expected O(n) | O(n) | Hash quality and boxing matter |
| Balanced recursion | Often O(log n) depth | O(log n) stack | Skew can make depth O(n) |
| Dynamic array append | Amortized O(1) | Capacity overhead | Individual resize is O(n) |

`lowerBound` is O(log n) time and O(1) auxiliary space when sortedness is a precondition. Calling `requireSorted` first makes the combined operation O(n). For one query on untrusted data, a linear scan may be simpler. For many queries, validating once and reusing the ordered representation can be worthwhile.

Constants re-enter after the asymptotic choice. A linear scan over a small contiguous primitive array can beat a more elaborate structure. Benchmark representative workloads rather than treating the notation as a stopwatch.

> **HotSpot note:** HotSpot may inline methods, eliminate range checks, vectorize loops, and compile hot branches using profile data. These optimizations can change constants but not the algorithm's asymptotic operation count or correctness proof.

## Edge cases and common mistakes

- Naming only `n` when the algorithm depends independently on two inputs.
- Multiplying every pair of nested loops without checking total pointer movement.
- Dropping output space or recursive stack space from the analysis.
- Claiming hash operations are worst-case O(1) without qualification.
- Confusing expected with amortized complexity.
- Optimizing before clarifying whether mutation, sorting, or extra memory is allowed.
- Choosing a pattern from keywords without proving its invariant.
- Mixing closed `[low, high]` and half-open `[low, high)` binary-search rules.
- Computing `(low + high) / 2` when indexes or search values could overflow.
- Updating `low = mid` in a loop where `mid` can equal `low`, causing nontermination.
- Giving average-case complexity when the interviewer asked for a service limit or worst-case bound.
- Hiding assumptions about integer overflow, Unicode, duplicates, or invalid input.

## Production engineering notes

Production inputs have distributions, limits, and adversaries. Record all three. Hash-flooding, enormous result sets, recursion depth, and a caller-controlled sort key can turn a textbook choice into a reliability problem. Put hard bounds around memory and output, use `long` for counts that may exceed `int`, and fail before partial mutation when possible.

Complexity is end-to-end. Reading n rows through an ORM may perform n extra queries; an O(n) Java loop can sit on top of O(n) network round trips. A theoretically better algorithm can lose if it allocates millions of wrappers or destroys locality. Instrument the actual bottleneck and preserve a clear correctness model while optimizing.

During review, demand qualifiers: n means what, hash time under what assumption, recursion depth under which shape, and whether output is included. This precision transfers directly from interviews to capacity planning.

## Interview questions and model answers

**Are two nested loops always O(n squared)?**

No. Derive total executions. In a sliding window, two nested-looking pointer loops may advance each pointer at most n times, yielding O(n). Independent full-range loops do produce a product.

**What is the difference between expected and amortized O(1)?**

Expected O(1) relies on a probability model, such as suitable hash distribution. Amortized O(1) bounds the average cost across an operation sequence, such as dynamic-array appends including occasional resizes, without assuming random inputs.

**How do you prove binary search correct?**

Define a monotone predicate and an interval invariant that retains the boundary or answer. Show initialization, show each comparison discards only impossible values, show the interval shrinks, and use the termination condition to derive the returned boundary.

**Why start with a brute-force baseline?**

It validates understanding, gives a correctness oracle for small tests, identifies repeated work, and provides a fallback. The optimized solution should be motivated by constraints, not pattern guessing.

**What space should be reported?**

State auxiliary working space, recursion stack, and output space separately. Also mention whether input is mutated. This avoids calling a recursive or output-heavy method "constant space."

**When can O(n) be worse than O(n log n)?**

Asymptotic notation describes growth, not every concrete runtime. The O(n) approach may have much larger constants, random memory access, boxing, or remote operations. For sufficiently large n its growth is better, but production decisions need representative measurements.

## Exercises

1. Analyze a loop with two pointers that each only move forward. Write the aggregate sum that proves O(n).
2. Implement upper bound, returning the first index whose value is greater than a target, using a half-open interval.
3. Analyze `T(n) = 2T(n/2) + n` with a recursion tree and account for stack depth separately.
4. Give a potential or aggregate argument for amortized dynamic-array append.
5. Take a quadratic duplicate-detection baseline and derive hashing and sorting alternatives, including mutation and worst-case qualifiers.
6. Write preconditions, postconditions, an invariant, and a progress measure for stable in-place compaction.
7. Estimate memory for one million boxed map entries, then explain why asymptotic O(n) alone is insufficient for a capacity decision.

## Chapter summary

Strong problem solving begins with a contract and ends with a proof, a cost model, and tests. Name independent input dimensions, count total work, qualify expected and amortized claims, and separate auxiliary, stack, and output space. Use a correct baseline to expose repeated work. Then choose state, state an invariant, prove progress, and trace boundaries. Binary search is the canonical example: its power comes from a monotone predicate and a shrinking candidate interval, not from memorized syntax.

## Revision checklist

- [ ] I name every independent input dimension before analyzing cost.
- [ ] I sum dependent work and multiply only independent repeated work.
- [ ] I distinguish O, Omega, Theta, worst, expected, and amortized bounds.
- [ ] I report auxiliary, call-stack, output, and mutation costs separately.
- [ ] I clarify the contract and produce a correct baseline before optimizing.
- [ ] I state invariants with initialization, maintenance, and termination.
- [ ] I identify a bounded progress measure.
- [ ] I can implement lower and upper bound without mixing interval conventions.
- [ ] I discuss constants and production constraints only after the asymptotic model is sound.
