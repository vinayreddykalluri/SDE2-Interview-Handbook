# 4. Proving Greedy Correctness

## Why this chapter exists

Greedy is the only major algorithmic technique where **a plausible algorithm is usually wrong**, and where the difference between a passing and a failing interview answer is whether you can say *why* yours is right.

Dynamic programming considers every option, so a correct recurrence is correct by construction. Greedy commits to one choice and never reconsiders, so it needs an argument that the committed choice is safe. Without that argument you have a heuristic that happens to pass the examples.

The workshop chapter in this volume covers finding counterexamples - proving an algorithm *wrong*. This chapter covers the other direction: the two standard techniques for proving one *right*, and how to deploy them in the five minutes an interview allows.

## The two proof techniques

Almost every greedy correctness argument is one of these.

### Exchange argument

**Claim:** any optimal solution can be transformed, step by step, into the greedy solution without ever getting worse. Therefore greedy is at least as good as optimal, so it is optimal.

The structure is always the same:

1. Let `OPT` be an optimal solution and `G` the greedy one.
2. Find the first position where they differ.
3. Show that exchanging `OPT`'s choice for greedy's choice yields a solution that is still valid and no worse.
4. Repeat. Each exchange increases the agreement with `G` by one, so after finitely many steps `OPT` has become `G`.

**Worked example - interval scheduling.** Given intervals, select the maximum number that do not overlap. Greedy: sort by *earliest finish time*, take each interval compatible with those already chosen.

*Proof.* Let `OPT` be optimal, sorted by finish time, and `G` be greedy's selection. Suppose they agree on the first `k` intervals and differ at position `k+1`. Greedy chose `g` and `OPT` chose `o`. By greedy's rule, `g` has the earliest finish time among all intervals compatible with the first `k`, so `finish(g) <= finish(o)`.

Replace `o` with `g` in `OPT`. Is the result still valid? Every interval in `OPT` after position `k+1` starts at or after `finish(o) >= finish(g)`, so none of them conflicts with `g`. The count is unchanged, so the modified solution is still optimal and now agrees with `G` on `k+1` intervals.

Repeating drives the disagreement to zero. Greedy is optimal. `QED`

![The exchange step: greedy's earlier finish time never breaks a later interval](content/volumes/dsa/DSA-16-greedy-algorithms/assets/01-exchange-argument.png)

The load-bearing sentence is "every later interval starts after `finish(o)`, which is at least `finish(g)`." **That is where the sort key earns its correctness**, and it is the sentence an interviewer is waiting for. Sorting by start time, or by duration, has no such sentence available - which is exactly why those variants are wrong.

### Greedy stays ahead

**Claim:** after every step, greedy's partial solution is at least as good as any other solution's partial solution by some measure. Therefore it is at least as good at the end.

This suits problems where you can define a running quantity and show greedy dominates it at each step.

**Worked example - interval scheduling again, the other way.** Let `g1..gk` be greedy's choices and `o1..om` any other valid selection, both by finish time.

*Claim:* for every `i <= min(k, m)`, `finish(g_i) <= finish(o_i)`.

*Induction.* For `i = 1`, greedy picks the globally earliest finish, so it holds. Assume it holds for `i`. Then `o_{i+1}` starts at or after `finish(o_i) >= finish(g_i)`, so `o_{i+1}` was available to greedy at step `i+1`. Greedy picks the earliest finishing available interval, so `finish(g_{i+1}) <= finish(o_{i+1})`.

*Conclusion.* Suppose `m > k`. Then `o_{k+1}` exists and starts at or after `finish(o_k) >= finish(g_k)`, so it was compatible with greedy's selection - contradicting greedy having stopped. Hence `m <= k`. `QED`

### Which to use

| Use exchange when | Use stays-ahead when |
|---|---|
| The solution is a set or sequence of choices | There is a natural running measure |
| Swapping one choice for another is easy to justify | Progress is monotonic |
| Selection problems - scheduling, MST | Coverage problems - jump game, gas station |

Both work for interval scheduling, which is why it is the standard teaching example. When only one is available, it is usually obvious which.

## Applying this under time pressure

You will not write a formal proof in an interview. You will say two or three sentences, and they should be the load-bearing ones.

**The template that works:**

> "I'll sort by X. The exchange argument is: take any optimal solution that differs from mine at the first position; my choice has [property P]; swapping it in keeps the solution valid because [consequence of P]; so I never lose. Repeating makes optimal equal mine."

Filled in for interval scheduling:

> "Sort by finish time. Take an optimal solution differing at the first position; my interval finishes no later than theirs; swapping keeps validity because everything after starts later than their finish, which is later than mine; so I never lose."

Three sentences, and it is a complete argument. Practising this template on five problems is worth more than reading proofs of twenty.

**Say the sort key and its justification together.** "Sort by finish time" is a step. "Sort by finish time *because that leaves the most room for what follows*" is the beginning of a proof. Interviewers hear the difference immediately.

## When greedy fails, and how to tell fast

Before proving, spend thirty seconds trying to break it. Counterexample search is faster than proof, and it is the cheaper failure.

**The standard probes:**

- **Two candidates with the same greedy key but different consequences.** If your key does not distinguish them, the tie-break may be doing unjustified work.
- **A large item that blocks several small ones.** The classic 0/1 knapsack failure: greedy by value density takes an item that excludes two better ones.
- **A locally best move that removes a future option.** Coin change with `{1, 3, 4}` and target 6: greedy takes 4, then 1, 1 for three coins; optimal is 3 + 3 for two.
- **Boundary sizes.** Empty input, one element, all elements identical, all incompatible.

The coin-change example is worth memorizing because it is the shortest complete refutation of "greedy works for coin change", which candidates assert surprisingly often. Greedy is correct for *canonical* systems such as US coins, and the qualifier is the whole answer.

**A failed proof attempt is diagnostic.** If you cannot complete the exchange step - if swapping greedy's choice into optimal breaks validity - the place it breaks usually *is* the counterexample. Proof and refutation are the same search from opposite ends.

## When greedy is guaranteed: the matroid connection

A structural answer exists for why greedy works on some problems and not others, and naming it is a strong senior signal even without the full theory.

A **matroid** is a set system closed under subsets with an exchange property: if `A` and `B` are independent sets and `|A| < |B|`, then some element of `B` can be added to `A` keeping it independent. **On a matroid, the greedy algorithm is optimal for any linear weight function.** Kruskal's minimum spanning tree is the standard instance - forests of a graph form a matroid, which is why sorting edges by weight and taking whatever does not create a cycle is correct.

The practical use is not to prove matroid-hood in an interview. It is to know that "does this structure have the exchange property?" is the underlying question, and that when it does not - as in 0/1 knapsack, where taking a heavier item can exclude two lighter ones - greedy has no guarantee and dynamic programming is the fallback.

## Worked example: a proof from scratch

*Given jobs each with a deadline and a profit, each taking one unit of time, schedule to maximize profit. A job earns its profit only if it finishes by its deadline.*

**Greedy:** sort by profit descending; for each job, schedule it in the latest free slot at or before its deadline; skip if no slot is free.

**Exchange proof.** Let `OPT` be optimal and `G` greedy's schedule. Consider the highest-profit job where they differ.

*Case 1 - greedy scheduled job `j`, `OPT` did not.* `OPT` uses some slot at or before `deadline(j)` for a lower-profit job, or leaves it free. Greedy only scheduled `j` because a slot was free at that point in its own construction. Replacing that lower-profit job with `j` in `OPT` keeps validity - `j` meets its deadline - and does not decrease profit, since `j` has profit at least as high.

*Case 2 - `OPT` scheduled `j`, greedy did not.* Greedy skipped `j` only when every slot at or before `deadline(j)` was full, and by the profit-descending order every job in those slots has profit at least `profit(j)`. So `OPT` cannot do better by including `j`; removing it and keeping greedy's jobs is no worse.

Both cases move `OPT` one step toward `G` without losing profit. `QED`

**Why the latest free slot?** Scheduling as late as possible preserves earlier slots for jobs with tighter deadlines. Choosing the earliest free slot breaks the argument and the algorithm: a job with a late deadline occupies an early slot that a tighter job then needs. The scheduling rule and the proof stand or fall together, which is the general lesson.

## Edge cases and common mistakes

- Presenting greedy with no correctness argument at all.
- Stating the sort key without saying why that key makes exchange work.
- Sorting interval scheduling by start time or duration; neither supports the argument.
- Asserting greedy solves coin change without the canonical-system qualifier.
- Applying greedy to 0/1 knapsack by value density, where a large item blocks two better ones.
- Scheduling into the earliest free slot rather than the latest, breaking the deadline proof.
- Skipping counterexample search and proving something false.
- Abandoning a failed exchange step instead of reading the counterexample out of it.
- Claiming greedy is optimal because it passed the sample cases.
- Confusing "greedy is a good approximation" with "greedy is optimal" - set cover admits a logarithmic-factor greedy, not an exact one.
- Forgetting ties. Two candidates with equal keys may not be interchangeable, and the proof must handle it.

## Interview questions and model answers

**How do you know your greedy algorithm is correct?**

By an exchange argument or by greedy-stays-ahead. Exchange: take any optimal solution, find the first place it differs from mine, and show swapping my choice in keeps it valid and no worse - repeat until optimal becomes mine. Stays-ahead: show my partial solution dominates any other's by some measure at every step, by induction. Which applies depends on the problem; selection problems usually take exchange, coverage problems usually take stays-ahead.

**Prove that earliest-finish-time is optimal for interval scheduling.**

Exchange. Suppose an optimal solution agrees with greedy on the first `k` intervals and differs at `k+1`. Greedy's interval finishes no later than optimal's, since it picks the earliest finish among compatible intervals. Swap it in: every later interval in optimal starts after optimal's finish, which is at or after greedy's, so nothing conflicts. Count unchanged, still optimal, now agreeing on `k+1`. Repeat.

**Why not sort by start time or by duration?**

Because neither supports the exchange step. The argument needs "everything after starts later than the finish time I chose", and only finish time gives that. Concretely, one long interval starting earliest destroys start-time ordering, and two short intervals overlapping one medium one destroys duration ordering.

**Does greedy solve coin change?**

Only for canonical coin systems, US denominations among them. With `{1, 3, 4}` and target 6, greedy takes 4 then 1 and 1 for three coins, while the optimum is 3 + 3 for two. For arbitrary denominations the problem needs dynamic programming.

**When is greedy guaranteed to work?**

When the structure is a matroid - a set system closed under subsets with the exchange property that a smaller independent set can always absorb an element from a larger one. Greedy is then optimal for any linear weight function, which is why Kruskal's MST works. Practically the question is whether that exchange property holds; where it fails, as in 0/1 knapsack, greedy has no guarantee and DP is the fallback.

**Your exchange argument does not go through. What does that tell you?**

Usually that the algorithm is wrong, and that the step where the swap breaks validity is the counterexample. Proof and refutation are the same search from opposite ends, so a stuck proof is a strong hint about where to look for a failing input.

## Exercises

1. **Foundation:** Write the exchange argument for interval scheduling in three sentences, then compress it to one.
2. **Foundation:** Find counterexamples to interval scheduling sorted by start time and by duration.
3. **Interview Core:** Prove earliest-finish-time optimal by greedy-stays-ahead, and compare the two proofs' lengths.
4. **Interview Core:** Prove or refute: greedy by value density is optimal for fractional knapsack, then for 0/1 knapsack.
5. **Interview Core:** Prove the deadline-scheduling algorithm, then change to the earliest free slot and construct the failing input.
6. **Interview Core:** Show `{1, 3, 4}` breaks greedy coin change, then characterize a canonical system.
7. **SDE-2 Follow-up:** Prove Kruskal via the exchange argument, then state which matroid property it uses.
8. **SDE-2 Follow-up:** Take gas station and prove it by stays-ahead. Say why exchange is awkward here.
9. **SDE-2 Follow-up:** Take a greedy that fails, attempt the exchange proof, and extract the counterexample from where it breaks.
10. **Challenge:** For a greedy of your choice, write both proofs and state which you would give in an interview and why.

## Chapter summary

Greedy is the technique where a plausible algorithm is usually wrong, so the correctness argument is not an add-on - it is the answer. Two techniques cover nearly everything: the exchange argument transforms any optimal solution step by step into the greedy one without loss, and greedy-stays-ahead shows by induction that greedy's partial solution dominates any competitor's at every step. Selection problems usually take exchange, coverage problems usually take stays-ahead, and interval scheduling admits both. Under time pressure, three sentences suffice - state the sort key together with the property that makes the swap safe, because "sort by finish time" is a step while "sort by finish time because it leaves the most room for what follows" is the beginning of a proof. Before proving, spend thirty seconds trying to break it: coin change with `{1, 3, 4}` refutes the most commonly asserted false claim, and a proof that will not go through usually hands you the counterexample at the point it fails. The structural answer underneath is the matroid exchange property, which is why Kruskal works and why 0/1 knapsack does not.

## Revision checklist

- [ ] I can state both proof techniques and when each applies.
- [ ] I can give the interval-scheduling exchange argument in three sentences.
- [ ] I state the sort key together with the property that justifies it.
- [ ] I can refute start-time and duration ordering with concrete inputs.
- [ ] I search for a counterexample before attempting a proof.
- [ ] I know `{1, 3, 4}` breaks greedy coin change and what "canonical" means.
- [ ] I can prove deadline scheduling and say why the latest free slot is required.
- [ ] I know a stuck exchange step usually locates the counterexample.
- [ ] I can name the matroid exchange property and what it guarantees.
- [ ] I distinguish greedy being optimal from greedy being a bounded approximation.
