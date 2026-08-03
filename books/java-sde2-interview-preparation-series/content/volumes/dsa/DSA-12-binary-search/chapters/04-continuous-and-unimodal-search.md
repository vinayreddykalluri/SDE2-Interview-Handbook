# 4. Searching Continuous and Unimodal Domains

## Why this chapter exists

Every search in this volume so far has been over an integer index range, terminating when `low` meets `high`. Two escalations break that assumption, and both appear once an interviewer moves past array problems.

**The domain is continuous.** "Find the square root to six decimal places", "find the minimum radius covering every point". Integers do not converge to a point, and the `low < high` termination condition never fires. Floating-point search needs a different stopping rule, and the naive one loops forever.

**The function is not monotonic.** Binary search requires a predicate that is false then true, exactly once. A function that rises then falls has no such predicate - but it still has a findable extremum, via ternary search. Recognizing that a problem is *unimodal rather than monotonic* is the whole insight.

## Part 1: search on real numbers

### Why integer termination fails

Integer binary search terminates because the range shrinks by at least one each step and integers are discrete. On reals, `(low + high) / 2` is strictly between them forever - or rather, until floating-point precision collapses the interval, which is an implementation accident rather than a designed stopping point.

Two correct stopping rules exist, and the second is the one to use.

**Absolute tolerance.** Stop when `high - low < epsilon`. Simple, and wrong for large magnitudes: an epsilon of `1e-9` is unreachable near `1e18`, because consecutive doubles there are further apart than that.

**Fixed iteration count.** Each step halves the interval, so `k` iterations reduce it by `2^-k`. One hundred iterations shrink any starting range below any representable precision, and the loop is guaranteed to terminate.

```java
static double squareRoot(double target) {
    if (target < 0) {
        throw new IllegalArgumentException("negative input");
    }
    double low = 0;
    double high = Math.max(1, target);      // sqrt(x) <= x only when x >= 1
    for (int i = 0; i < 100; i++) {         // fixed count: always terminates
        double mid = low + (high - low) / 2;
        if (mid * mid < target) {
            low = mid;
        } else {
            high = mid;
        }
    }
    return low;
}
```

Three details:

**`Math.max(1, target)` for the upper bound.** For `target < 1`, the square root is *larger* than the target - `sqrt(0.25) = 0.5` - so an upper bound of `target` excludes the answer. This is the most common bug in the problem, and it only shows on inputs below one.

**`low + (high - low) / 2` rather than `(low + high) / 2`.** On integers this avoids overflow; on doubles it avoids losing precision when both endpoints are large and similar. Same habit, different reason.

**A fixed 100 iterations, not a tolerance loop.** It cannot hang, it needs no epsilon tuning, and 100 halvings exceed the precision of a `double` many times over. Interviewers recognize this as the robust choice.

### Choosing the iteration count

Each iteration adds one bit of precision. A `double` has 52 mantissa bits, so around 60 to 70 iterations reach full precision for a starting range near 1. One hundred is a comfortable margin that costs nothing - the loop is 100 operations regardless of input size.

If the prompt asks for a specific tolerance, say six decimal places, you can compute it: from a range of width `W`, you need `log2(W / epsilon)` iterations. For `W = 1e18` and `epsilon = 1e-6`, that is about 80. Stating the arithmetic rather than guessing is the signal.

## Part 2: ternary search on unimodal functions

### Monotonic versus unimodal

Binary search needs a predicate that switches from false to true exactly once. Formally, the sequence must be **monotonic** in that predicate.

A **unimodal** function has a single peak (or single valley) - it rises then falls, or falls then rises. There is no false-then-true predicate available, because "is this greater than the value to its right?" is false, then true, and the *boundary* is the peak. Actually that does give a valid predicate, which is why peak-finding by binary search works and is covered in this volume's family 5.

Ternary search is the alternative when comparing to a neighbour is not possible or not meaningful - typically on a continuous function you can only *evaluate*, not differentiate.

```text
monotonic:   F F F F T T T T      -> binary search the boundary
unimodal:      /\                 -> ternary search the peak
              /  \
```

### The method

Pick two interior points, evaluate both, and discard the third of the range that cannot contain the extremum.

```java
/** Maximum of a unimodal function on [low, high]. */
static double ternarySearchMax(DoubleUnaryOperator f, double low, double high) {
    for (int i = 0; i < 200; i++) {          // ternary shrinks slower; use more
        double leftThird = low + (high - low) / 3;
        double rightThird = high - (high - low) / 3;
        if (f.applyAsDouble(leftThird) < f.applyAsDouble(rightThird)) {
            low = leftThird;                 // peak is right of leftThird
        } else {
            high = rightThird;               // peak is at or left of rightThird
        }
    }
    return low + (high - low) / 2;
}
```

**Why discarding is valid.** If `f(leftThird) < f(rightThird)`, the peak cannot lie left of `leftThird` - if it did, unimodality would force `f` to be decreasing across both points, making `f(leftThird) > f(rightThird)`. So `[low, leftThird]` is safe to discard. The symmetric argument covers the other case.

**Unimodality is a precondition, not something the search checks.** Run ternary search on a function with two peaks and it silently returns one of them, or neither. There is no error - just a wrong answer. Stating that precondition unprompted is the signal an interviewer wants.

**The convergence rate.** Each iteration keeps two thirds of the range, so `k` iterations shrink it by `(2/3)^k`. That is slower than binary search's `(1/2)^k`, and each iteration costs *two* function evaluations rather than one. Ternary search needs roughly 1.7 times as many iterations for the same precision, and about 3.4 times as many evaluations. Use it only when a monotonic predicate genuinely is not available.

### Integer ternary search, and why to avoid it

On integers the two-point comparison breaks down when the range shrinks to two or three elements - `leftThird` and `rightThird` can coincide, and the loop stops making progress.

The reliable integer approach is to compare **adjacent** elements instead:

```java
// Peak finding: compare a[mid] with a[mid + 1] - this IS monotonic in the
// predicate "is the peak at or before mid?", so plain binary search works.
while (low < high) {
    int mid = low + (high - low) / 2;
    if (values[mid] < values[mid + 1]) {
        low = mid + 1;                 // ascending: peak is to the right
    } else {
        high = mid;                    // descending or peak: it is here or left
    }
}
return low;
```

This is the peak-finding template from family 5, and it is worth recognizing that **it is binary search, not ternary** - the adjacent comparison manufactures the monotonic predicate that unimodality alone did not provide. On integers, prefer this. Reserve ternary search for continuous domains where neighbours do not exist.

## Part 3: binary search on the answer, on reals

The most valuable combination is applying real-valued binary search to a *feasibility* predicate - the "search the answer" pattern from family 3, but over a continuous parameter.

*Given n houses on a line and k heaters, find the minimum radius so every house is covered.*

The radius is continuous, and feasibility is monotonic in it: if radius `r` works, so does any larger radius. That monotonicity is what licenses binary search, and stating it is the load-bearing step.

```java
static double minimumRadius(double[] houses, double[] heaters) {
    double low = 0;
    double high = 1e9;                       // any radius certainly sufficient
    for (int i = 0; i < 100; i++) {
        double mid = low + (high - low) / 2;
        if (covers(houses, heaters, mid)) {
            high = mid;                      // feasible: try smaller
        } else {
            low = mid;                       // infeasible: need larger
        }
    }
    return high;                             // return the feasible side
}
```

**Return `high`, not `low`.** `high` always holds a value known feasible; `low` always holds one known infeasible. On a minimisation the feasible side is the answer. Returning the wrong endpoint gives a result that fails the check by an epsilon - a bug that passes casual testing and fails an exact assertion.

This pattern generalizes widely: minimum time, minimum capacity, maximum minimum distance. The recipe is always the same - identify the continuous parameter, confirm feasibility is monotonic in it, write the feasibility check, and binary search a fixed number of iterations.

## Edge cases and common mistakes

- Using `while (low < high)` on doubles, which never terminates.
- Using an absolute epsilon at large magnitudes where consecutive doubles are further apart than the epsilon.
- Bounding `sqrt(x)` above by `x`, which excludes the answer for `x < 1`.
- Computing the midpoint as `(low + high) / 2` and losing precision on large endpoints.
- Returning `low` from a minimisation search when `high` holds the feasible value.
- Applying ternary search to a function that is not unimodal; it returns a wrong answer silently.
- Using ternary search on integers, where the two probes can coincide and stall.
- Reaching for ternary search when an adjacent comparison gives a monotonic predicate and plain binary search suffices.
- Forgetting that ternary search costs two evaluations per iteration and converges more slowly.
- Failing to state the monotonicity of the feasibility predicate before searching the answer.

## Interview questions and model answers

**Find a square root to six decimal places.**

Binary search on the real interval from 0 to `max(1, target)` - the upper bound must be at least 1 because for inputs below one the root is larger than the input. Run a fixed number of iterations, around 100, rather than looping on a tolerance: each iteration adds a bit of precision, the loop cannot hang, and no epsilon needs tuning.

**Why not loop until `high - low < epsilon`?**

It works for moderate magnitudes and fails for large ones, where consecutive representable doubles are further apart than the epsilon and the condition can never be satisfied. A fixed iteration count is bounded, precision-independent, and free.

**When would you use ternary search?**

On a continuous unimodal function where I can only evaluate, not compare neighbours - a single peak or valley with no monotonic predicate available. Each iteration keeps two thirds of the range and costs two evaluations, so it converges more slowly than binary search; I would only use it where a monotonic predicate genuinely does not exist.

**Find the peak in an integer array. Ternary search?**

No - binary search. Comparing `a[mid]` with `a[mid + 1]` manufactures a monotonic predicate: "is the peak at or before mid?" is false then true exactly once. Ternary search on integers also degenerates when the two probes coincide on a small range. Adjacent comparison is both correct and simpler.

**What happens if the function is not actually unimodal?**

Ternary search returns a wrong answer with no error. Unimodality is a precondition it does not verify, so it must be established from the problem rather than assumed.

**In binary search on the answer, which endpoint do you return?**

The one holding a known-feasible value - `high` for a minimisation, `low` for a maximisation. The other endpoint is known infeasible, so returning it produces a result that misses the requirement by an epsilon, which passes casual testing and fails an exact check.

## Exercises

1. **Foundation:** Write real-valued binary search for a square root, then run it on 0.25 with an upper bound of `target` and observe the failure.
2. **Foundation:** Compute the iterations needed to reach `1e-9` precision from a starting range of `1e18`.
3. **Interview Core:** Replace the fixed loop with `while (high - low > 1e-9)` and find a magnitude where it does not terminate.
4. **Interview Core:** Implement ternary search for the maximum of a unimodal function and verify against a fine-grained scan.
5. **Interview Core:** Run ternary search on a two-peaked function and show it returns silently wrong output.
6. **Interview Core:** Solve peak finding with adjacent-comparison binary search, then attempt ternary search on integers and find where the probes coincide.
7. **SDE-2 Follow-up:** Measure function evaluations for binary and ternary search to identical precision and confirm the ratio.
8. **SDE-2 Follow-up:** Solve the heater-radius problem and assert the returned radius covers every house exactly; then return `low` instead and watch the assertion fail.
9. **SDE-2 Follow-up:** Take a maximisation "search the answer" problem and state which endpoint holds the feasible value.
10. **Challenge:** Given a function that is unimodal only within an unknown subrange, describe how you would locate that subrange before searching it.

## Chapter summary

Two escalations break the integer search this volume has assumed. On a continuous domain the `low < high` termination never fires, and the robust replacement is a fixed iteration count rather than a tolerance loop - each iteration adds a bit of precision, around 100 exceeds a double's mantissa many times over, and it cannot hang or need epsilon tuning at large magnitudes where an absolute tolerance becomes unreachable. The recurring traps are bounding `sqrt(x)` above by `x`, which excludes the answer below one, and returning the infeasible endpoint from an answer search when only the feasible side satisfies the requirement. The second escalation is a function that is unimodal rather than monotonic: ternary search finds its extremum by discarding a third per iteration, but converges more slowly than binary search and costs two evaluations per step, so it belongs only on continuous domains where no monotonic predicate exists. On integers, comparing adjacent elements manufactures exactly that predicate, which is why peak finding is binary search rather than ternary - and unimodality remains a precondition that ternary search never checks and silently violates when it fails.

## Revision checklist

- [ ] I know why `low < high` cannot terminate on doubles.
- [ ] I use a fixed iteration count and can justify the number.
- [ ] I know why an absolute epsilon fails at large magnitudes.
- [ ] I bound `sqrt(x)` above by `max(1, x)` and know which inputs break otherwise.
- [ ] I compute midpoints as `low + (high - low) / 2` on reals as well as integers.
- [ ] I return the feasible endpoint from an answer search and know which one it is.
- [ ] I can distinguish monotonic from unimodal.
- [ ] I can justify why ternary search may discard a third of the range.
- [ ] I know ternary search costs two evaluations and converges more slowly.
- [ ] I use adjacent-comparison binary search for integer peak finding, not ternary.
