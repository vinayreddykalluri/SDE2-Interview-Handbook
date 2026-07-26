# Time and Space Complexity Topic Coverage

Final PDF: `series/dist/Java-SDE2-DSA-02-Time-and-Space-Complexity.pdf`

| Topic | Required depth | Previous state | Final state | Chapter | Examples/practice | Validation |
|---|---|---|---|---:|---|---|
| Complexity meaning | Foundation | Too shallow | Concrete count-first model | 1 | companion 01-03; A/B | Source + PDF |
| Input dimensions | Interview Core | Adequate | `n`, `m`, rows/columns, total cells, `V/E`, `k`, sums | 1-2 | B2, B13, B16, B20 | Source + Practice |
| Growth families | Foundation | Missing runway | O(1), log n, n, n log n, n squared, exponential/factorial | 1 | companion 01-09 | Companion + Practice |
| Big-O/Omega/Theta | Interview Core | Dense | Gentle definition with case qualification | 1 | A1, A8-A10 | Practice |
| Sequential/nested/dependent loops | Interview Core | Too few examples | Exact counts, sums, triangular and geometric work | 2 | companion 04-09; B2-B7 | Companion + Practice |
| Aggregate pointer movement | Interview Core | Adequate | Forward-only proof and invariant | 2, 5 | companion 10; B8, D9 | Companion + Practice |
| Hidden Java/API work | Interview Core | Too shallow | methods, contains, strings, copies, conversion, sort | 2 | B9-B12; C12 | Source + Practice |
| Matrices/jagged input | Foundation | Missing | rectangular versus total-cell model | 2 | companion 12-13; B13, D10 | Companion + Practice |
| Multiple test cases | Interview Core | Missing | sum of case sizes | 2 | B20 | Practice |
| Input/auxiliary/output space | Foundation | Too shallow | Explicit three-part contract | 3 | A3; B16; Assessment 3 | Source + Practice |
| Peak live/retained storage | SDE-2 | Missing | lifetime and batch-retention reasoning | 3 | quick checks/follow-ups | Source + Practice |
| Recursion space | Interview Core | Adequate | linear, logarithmic, branching, tree height | 3 | companion 16-17; B14-B15 | Companion + Practice |
| In-place/copy/ownership | Interview Core | Missing trade-off | mutation, defensive copy, views, shallow references | 3 | companion 15; D6/D12 | Companion + Practice |
| ArrayList/LinkedList | Interview Core | Reference only | usage-context cost and amortization | 4, 6 | A16-A17; C8 | Source + Practice |
| HashMap/HashSet | Interview Core | Overcompressed | expected cost, equality/hash, mutable keys, boxing | 4, 6 | companion 20-21; D4-D5 | Companion + Practice |
| Ordered/sorted collections | Interview Core | Reference only | LinkedHash and tree semantics/cost | 4, 6 | companion 22; D12 | Companion + Practice |
| ArrayDeque/PriorityQueue | Interview Core | Reference only | end/heap costs and iteration trap | 4, 6 | companion 23-24; B17-B18 | Companion + Practice |
| Expected/amortized/output-sensitive | SDE-2 | Strong but early | Preserved after prerequisites | 4-5 | A10-A11; E2/E5/E6 | Source + Practice |
| Constraints/lower bounds | SDE-2 | Strong | Preserved and better sequenced | 1, 5 | assessments 4-5 | Source + Practice |
| Optimization method | SDE-2 | Strong | Baseline, bottleneck, invariant, proof, tests, trade-off | 5 | E12; Assessment 5 | Source + Practice |
| Practice/solutions/readiness | SDE-2 | Too shallow | 85 mixed items with separated reasoning solutions | 7-8 | 20 K, 20 analysis, 15 debug, 12 coding, 12 follow-up, 6 assessments | Counted + PDF QA |
| Executable Java | Interview Core | Missing | 24 lint-clean deterministic checks | 9 | examples 01-24 | Compile + Run |

## Deliberate boundaries

- Numeric overflow/base algorithms: Number Systems.
- Full loop patterns: Loop Mastery.
- Complete array/string/hash/recursion/tree/graph/DP patterns: their dedicated DSA volumes.
- Benchmark design and JMH: Advanced Java E.
- JVM object layout and GC: Advanced Java A/E.

No required complexity prerequisite is deferred; only deeper domain catalogs are cross-referenced.
