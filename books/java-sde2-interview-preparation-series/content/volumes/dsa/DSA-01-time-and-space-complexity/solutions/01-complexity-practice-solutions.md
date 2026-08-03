# Complexity Practice Solutions

Use these solutions after a complete attempt. Equivalent bounds are acceptable when assumptions are explicit and the proof is sound.

## How to review a solution

Do not compare only the final notation. For each answer, verify five parts:

1. **Dimension:** Did you name every independent input size rather than forcing everything into `n`?
2. **Execution:** Did you count reached work, including helper methods, library calls, copies, and generated output?
3. **Qualifier:** Did you label best, worst, expected, or amortized behavior when the distinction changes the claim?
4. **Space contract:** Did you separate auxiliary work, recursion depth, returned output, and mutation of caller data?
5. **Proof:** Can you justify the bound with a count, sum, recurrence, shrinking argument, or aggregate movement invariant?

If your final bound matches but the proof or qualifier is missing, mark the item partially correct. SDE-2 readiness means another engineer can audit the reasoning, not merely recognize the notation.

Keep an error log with four columns: snippet, incorrect assumption, corrected rule, and one new test. Reattempt every missed item after at least one day; delayed retrieval is more useful than rereading the answer immediately.

## A. Knowledge-check solutions

1. Big-O is an asymptotic upper bound on growth after constant factors; it is not elapsed time or an exact instruction count.
2. Independent inputs can grow separately. O(n + m) preserves that fact; replacing both by `n` can hide the dominant dimension.
3. Input space belongs to supplied data, auxiliary space is temporary working storage, and output space holds the required result.
4. Their counts add: `n + n = 2n`, which simplifies to O(n). Only independent nested repetitions multiply.
5. Repeatedly halving or doubling a bounded value commonly creates logarithmic iterations.
6. The address of an indexed array slot is computed directly from base, index, and fixed element layout; the program does not scan preceding entries.
7. When all or a constant fraction of unordered/ordered pairs are visited, the count is proportional to `n(n - 1)/2` or `n^2`.
8. The target is last or absent, so all `n` elements are inspected.
9. The unsimplified count shows whether work adds, multiplies, or depends on another dimension; dropping too soon invites a false bound.
10. Expected cost averages over a probability model. Amortized cost averages over an operation sequence even without random input.
11. The bound contains output size `k`, such as O(n + k) time or O(k) result space.
12. If each pointer advances only and at most `n` times over the whole execution, total movement is at most a constant multiple of `n`.
13. The call tree contains exponentially many calls over time, but only one root-to-leaf chain of O(n) frames is active at once.
14. O(h), where `h` is tree height; specialize to O(log n) only with a balance guarantee and O(n) for a worst-case skew.
15. Hash distribution, collisions, resizing, key contracts, and implementation state affect cost. Expected O(1) under sound hashing is the suitable basic claim.
16. Reaching an index or finding a value can require a linear traversal before the link update.
17. Occasional O(n) backing-array copies are spread across many cheap appends, producing constant average cost per append over the sequence.
18. A heap guarantees only the head. Its internal array maintains a partial order, not globally sorted iteration order.
19. Sorting can provide deterministic bounds, ordered output, lower representation overhead, or better locality, and may avoid reliance on hashing/equality.
20. Constants, allocation, locality, input distribution, JVM optimization, APIs, and hardware differ. Asymptotic growth intentionally abstracts these effects.

## B. Count-and-bound solutions

1. **B1:** Two indexed reads and one addition: O(1) time, O(1) auxiliary space, assuming a nonempty array.
2. **B2:** `n + m` visits for independent lengths: O(n + m) time and O(1) auxiliary space if `consume` is O(1).
3. **B3:** Exactly `n^2` visits: Theta(n squared) time and O(1) loop storage.
4. **B4:** `n(n - 1)/2` comparisons: Theta(n squared) time, O(1) auxiliary space.
5. **B5:** Floor/ceiling details depend on `n`, but the body runs about `log2(n)` times: O(log n).
6. **B6:** About `log2(n)` outer iterations, each with `n` inner visits: O(n log n).
7. **B7:** `1 + 2 + 4 + ...` up to `n`, less than `2n` for power-of-two framing: O(n), not O(n log n).
8. **B8:** `right` advances `n` times and `left` at most `n` times: O(n) control work by aggregate analysis.
9. **B9:** `n` queries times O(n) linear ArrayList membership: O(n squared) worst-case.
10. **B10:** Expected O(n) total with expected O(1) soundly hashed membership. Space for the existing set is not newly allocated by the loop.
11. **B11:** Sorting dominates the scan, ordinarily O(n log n) time for the relevant primitive overload plus O(n) scan. Exact worst-case and auxiliary-space details should name the overload/JDK contract.
12. **B12:** Each immutable concatenation can copy the growing prefix. `1 + 2 + ... + n` character work is O(n squared); intermediate strings also create allocation pressure. StringBuilder makes construction O(n) in appended characters under the ordinary model.
13. **B13:** Time is Theta(sum from i=1 to r of `c_i`) if each cell is consumed once. The rectangular shorthand O(rows times columns) is inappropriate without equal row lengths.
14. **B14:** `n + 1` calls: O(n) time and O(n) call-stack space.
15. **B15:** The recurrence is `T(n) = 2T(n - 1) + O(1)`, so time is O(2^n) as a simple tight-order description; maximum stack depth is O(n).
16. **B16:** O(n) time, O(1) auxiliary working variables, and O(k) result space. If result storage is included in space, report O(k).
17. **B17:** `n` amortized O(1) end removals: O(n) total. The existing queue occupies O(n) input/state space; loop variables are O(1).
18. **B18:** Removal sizes descend from n; each costs O(log currentSize), totaling O(n log n). The queue is mutated.
19. **B19:** A useful sorted-range model is O(log n + k): locate a boundary then visit `k` entries. Exact view-creation and iteration semantics should follow the implementation contract.
20. **B20:** Theta(sum from i=1 to t of `n_i`). Writing O(t times max `n_i`) is an upper bound but loses the tighter total-input description.

## C. Debug-the-analysis solutions

1. Consecutive loops add rather than multiply. Three full scans are O(3n) = O(n).
2. Indexing one element is O(1); array length describes input storage, not lookup work.
3. Early return affects best case. If the match can be absent/last, worst-case time is O(n).
4. O(n) describes a growth class after constants and lower-order work, not exact machine instructions.
5. Use O(n + m) for two independently sized scans.
6. Analyze total movement. Two forward-only pointers can perform at most 2n advances, yielding O(n), assuming constant-time predicates.
7. Say expected O(1) under sound hashing and ordinary assumptions; do not promise it for every state/input.
8. Updating neighboring links may be O(1) after position is known, but reaching the middle by index is O(n), so total is O(n).
9. Only repeated `poll`/`remove` returns priority order. Iteration exposes heap-array order and takes O(n) to visit all entries.
10. Recursive calls consume stack frames. Space is proportional to maximum depth plus growing allocations.
11. Time counts all call-tree nodes; stack counts one active path. Naive Fibonacci has O(n) depth.
12. The constructor traverses and copies membership into new storage: O(n) time and O(n) logical slots.
13. Auxiliary space may exclude the required result, but the copied array still consumes O(n) result storage and should be reported separately.
14. For sufficiently large `n` under comparable constants, lower growth wins, but a particular O(n log n) implementation can be faster for relevant sizes or provide better guarantees/semantics.
15. Measurements compare executions in an environment; they do not prove behavior for arbitrarily growing `n`. Derivation establishes asymptotic complexity.

## D. Coding-task solution guidance

### D1. Linear search with inspection count

Return a small record such as `SearchResult(int index, int inspections)`. Increment before each equality check. Empty/absent input produces `(-1, n)`; a first-element match produces `(0, 1)`. See examples 02 and 03 in the companion.

### D2. Rectangular cell visits

Use nested enhanced-for loops and increment one counter per cell. Time is O(rows times columns) only if rectangular; a general implementation naturally handles jagged rows in O(total cells).

### D3. Halving steps

Repeatedly divide a positive value by two while it exceeds one. Define behavior for zero and negative input. Each step removes one binary magnitude level, so time is O(log n) and space O(1).

### D4. Duplicate detection

The nested-loop version compares pairs: worst-case O(n squared), O(1) auxiliary space, no mutation. The HashSet version stops when `add` returns false: expected O(n) time, O(u) space, boxing for `int`, and dependency on sound equality/hash behavior.

### D5. Frequency map

Use `counts.merge(value, 1, Integer::sum)` or `getOrDefault`. With n inputs and u distinct keys: expected O(n) time and O(u) result space. State boxing and hash assumptions.

### D6. Two reverse contracts

The in-place two-pointer version is O(n) time/O(1) auxiliary space and mutates input. The safe version copies first, O(n) extra result storage, then reverses the copy. Neither is categorically superior; the API contract decides.

### D7. Queue and stack

For a queue, use `offerLast` with `removeFirst`/`pollFirst`. For a stack, use `push`/`pop`/`peek`, or consistently use one end. Each end operation is amortized O(1); n operations are O(n) total.

### D8. Largest k values

Maintain a min-heap of at most `k`. Offer until size `k`; for each later value larger than the head, replace the head. Time is O(n log k), auxiliary space O(k). If sorted output is required, draining adds O(k log k).

### D9. Two-pointer proof

Identify a monotonic invariant. If `right` increments n times and `left` never retreats and increments at most n times, the total is at most 2n pointer moves. Include the predicate's cost.

### D10. Jagged sum

Loop through each row and then its entries. If row i has `c_i` cells, time is Theta(sum `c_i`) and auxiliary space O(1), excluding the input.

### D11. String construction

Use one StringBuilder and append each part. Define `C` as total characters appended; construction is O(C) in the ordinary amortized buffer-growth model and the result requires O(C) storage. Formatting/parsing inside the loop must be added separately.

### D12. Deterministic duplicate API

Copy and sort the primitive array, then scan adjacent entries: O(n log n) time and O(n) copy/result storage, with no caller mutation. If mutation is permitted, sort in place. A tree-based set offers O(n log n) and O(n) storage. Discuss how the chosen primitive sort's documented guarantees and memory behavior affect the exact claim.

## E. Interview follow-up model answers

1. Sorting avoids hash assumptions, can preserve ordered output, may use compact primitive arrays, and provides a predictable O(n log n) route; hashing uses extra structures but offers expected O(n).
2. Poor/adversarial distribution can increase collision work. Mutation of equality/hash fields can make an inserted key unreachable. Use sound immutable keys or choose an ordered/different representation.
3. Graph traversal is O(V + E) for adjacency-list representation because every vertex is processed and each stored edge is examined a constant number of times. An adjacency matrix changes edge-scan work to O(V squared).
4. Count aggregate pointer movement. If the inner pointer resets for every outer index, multiplication may be right; if it only advances across the entire method, O(n) may be right.
5. Returning all k matches prevents an early final answer and requires Omega(k) output work/storage. A typical scan becomes O(n) time and O(k) result space.
6. Amortization spreads rare expensive operations across a sequence. It does not cap individual request latency; one resize can still be O(n).
7. Always mention result/copy storage. Whether it is labeled auxiliary or output depends on the convention and contract, but capacity planning cannot pretend it is free.
8. DFS stack is O(h): O(log n) on a balanced tree and O(n) on a skewed tree. BFS is O(w), maximum width.
9. Name the probability source, such as sound hash distribution or randomized choices. Without a model, “average” is unsupported.
10. A benchmark can reveal constants, allocation, throughput, latency, and environment effects for tested sizes. It cannot establish asymptotic behavior over all larger inputs or repair a wrong cost model.
11. Mutation policy may require copies; memory limits may favor arrays/sorting; deterministic requirements may favor trees/sorting; lookup and ordering semantics select the implementation before micro-optimization.
12. First state the O(n squared) bottleneck, often repeated search. Introduce sorting or indexed membership, prove the new invariant/cost, report extra space/mutation/expected qualifiers, and validate representative edge cases.

## F. Cumulative-assessment rubric

### Assessment 1

A strong answer names n, derives O(1), O(n), O(log n), O(n), and O(n squared), and does not confuse source nesting with executed work.

### Assessment 2

A strong answer connects ArrayList to indexing, HashSet to expected membership, TreeSet to O(log n) sorted navigation, ArrayDeque to amortized O(1) ends, and PriorityQueue to O(1) peek/O(log n) update. It mentions order/equality and avoids universal guarantees.

### Assessment 3

A strong answer reports mutation and O(1) auxiliary space for in-place work, O(n) copy/result storage for defensive work, O(depth) recursive stack, and O(k) required output. It distinguishes shallow reference copying from deep copying.

### Assessment 4

A strong answer identifies O(n) copy, sorting cost, repeated linear membership, potentially quadratic immutable concatenation, and O(k) output. It changes representations only when the method's contract supports them and states the resulting bounds.

### Assessment 5

A strong explanation has a clear baseline, named input dimensions, bottleneck proof, improved invariant or data structure, qualified time and space, trade-off, and falsifying tests such as empty, smallest, worst-shape, duplicate-heavy, and maximum-size input.

## Final-readiness scoring

Score each of the eight readiness bullets from the practice chapter:

- `2`: correct, concise, and independently explained;
- `1`: correct only with prompting or missing an important qualifier;
- `0`: incorrect or not attempted.

`14-16` means proceed. `10-13` means proceed only after revisiting the lowest-scoring chapter. Below `10` means repeat the foundations and practice lab. The goal is defensible reasoning, not memorized notation.
