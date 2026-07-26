# Loop Mastery Topic Coverage

| Topic | Required depth | Previous state | Final state | Chapter | Examples/figures | Exercises | Cross-reference | Validation |
|---|---|---|---|---:|---|---|---|---|
| Loop state model | Foundation | Missing | Strong | 1 | lifecycle figure and trace | A1-A3, B1-B3 | Java Foundations | Companion checks 1-10 |
| `for` execution | Foundation | Too shallow | Strong | 1 | forward/step examples | A1-A2, B1/B4 | Java Foundations | checks 1-5 |
| `while` and termination | Foundation | Too shallow | Strong | 1 | countdown and infinite-loop failure | A3/A7, B8, C3 | Complexity | checks 8-9 |
| `do-while` | Foundation | Missing | Strong | 1 | execute-once dry run | B3, C5 | Java Foundations | check 8 |
| enhanced-for | Foundation | Missing | Strong | 1/6 | desugaring figure, copies/references | A8-A9, B6, C4/C19 | Collections | checks 1/9 |
| control transfers | Interview Core | Too shallow | Strong | 1 | break/continue/return/labeled | A6-A7, B5/B8 | Java Foundations | check 9 |
| scope and bounds checks | Interview Core | Too shallow | Strong | 1/6 | access sequence and JIT qualification | C1-C2 | JVM | checks 1-7 |
| half-open ranges | Interview Core | Strong compressed | Strong | 2 | processed/remaining figure | A11, B10, C6 | Binary Search | checks 11-15 |
| closed ranges | Interview Core | Adequate | Strong | 2 | convention table | A12, C6 | Binary Search | pointer checks |
| invariants | SDE-2 | Strong compressed | Strong | 2/6 | sum proof and design contract | A13-A14, assessments | Complexity | all core methods |
| progress/termination | SDE-2 | Strong compressed | Strong | 2/6 | lifetime measures | A14/A22, C8 | Complexity | all loops terminate |
| fenceposts and counts | Interview Core | Too shallow | Strong | 2 | items/boundaries formulas | A4-A5, B19 | Number Systems | `long` count checks |
| midpoint safety | Interview Core | Adequate | Strong | 2 | domain-qualified formula | C7-C8 | Binary Search | checks 11-15 |
| lower/upper bounds | Interview Core | Exercise only | Strong | 2 | lower-bound visual trace | D7-D8, E9 | Binary Search | checks 11-15 |
| opposing pointers | Interview Core | Adequate | Strong | 3 | elimination figure/dry run | D9-D10, E1-E2 | Arrays | checks 16-19 |
| read/write compaction | Interview Core | Adequate | Strong | 3 | compaction figure/dry run | D11-D13, E3-E4 | Arrays | checks 20-22 |
| stable merge/intersection | Interview Core | Missing | Strong | 3 | full methods and invariant | D14-D15, E5 | Arrays/Sorting | checks 23-24 |
| partition regions | SDE-2 intro | Missing | Adequate | 3 | Dutch-flag region model | follow-up practice | Arrays | contextual review |
| fixed window | Interview Core | Adequate | Strong | 4 | state figure and trace | D16, E6 | Arrays | check 25 |
| variable window | Interview Core | Strong compressed | Strong | 4 | K-distinct dry run | D17-D19, E7-E8 | Arrays/Strings | checks 26-30 |
| monotonicity limits | SDE-2 | Adequate | Strong | 4 | negative-value counterexample | A20-A21, C14 | Prefix state | check 30 contract |
| aggregate movement | SDE-2 | Strong | Strong visual | 4/6 | movement figure and pair proof | A22, D20, E10 | Complexity | check 31 |
| rectangular/ragged Java arrays | Interview Core | Adequate | Strong | 5 | storage/validation examples | A25, D6/D22, E11 | Arrays | checks 34-40 |
| flatten/unflatten | Interview Core | Strong compressed | Strong visual | 5 | 3x4 mapping figure | D21, E12 | Number Systems | checks 32-33 |
| neighbor/diagonal traversal | Interview Core | Too shallow | Strong | 5 | delta and diagonal methods | D22 | Graphs | checks 36-38 |
| spiral boundaries | Interview Core | Adequate | Strong visual | 5 | shrinking-ring figure | D23, E13 | Arrays | check 39 |
| iterator mutation | SDE-2 Java | Missing | Strong | 6 | translation, invalid/correct removal | A27, B18, C19 | Collections | contextual validation |
| iteration order | SDE-2 Java | Missing | Strong | 6 | collection guarantee examples | A28, E14 | Collections/Heaps | contextual validation |
| numeric promotion in indexes | Interview Core | Adequate | Strong | 2/5/6 | overflow comparisons | A26, B12, C18 | Number Systems | checks 31-33 |
| testing and diagnostics | SDE-2 | Adequate | Strong | 6 | category matrix/properties | cumulative assessments | Testing | 40 checks |
| production loops | SDE-2 | Adequate | Strong | 4/5/6 | cancellation/output/ownership | D24, E15 | Advanced Java | solution sketch |
| Practice and solutions | Publication | Too shallow | Strong | 7-8 | 109 numbered items/chains | all categories | next-volume handoff | separated solutions |
| Executable companion | Publication | Embedded only | Strong | 9 | one Java 21 class | 40 checks | none | warning-free pass |

## Coverage conclusion

All core topics required to move from Java fundamentals into array and string problem solving are now covered in prerequisite order. Deep binary-search variants, array algorithm catalogs, Unicode algorithms, graph traversal, collection internals, and JVM bytecode remain intentionally cross-referenced rather than duplicated.
