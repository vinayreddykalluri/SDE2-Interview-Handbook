# Focused Series Source Map

## Mapping policy

The focused series preserves the master guide rather than cloning or deleting its material. `series/series.json` is the canonical, machine-readable mapping. A source entry may include a named section selection when one master chapter covers several DSA patterns; this avoids repeating unrelated sections in multiple PDFs.

## Series-native material

Stage 1 uses 16 new chapters under `series/volumes/01-number-systems-and-math-foundations/chapters/`. Part A contains Chapters 1-13; Part B contains Chapters 14-16. Together they supply the prerequisites that were missing from the master: digit traversal, base conversion, large-number strings, interview divisibility, factors and primes, GCD/LCM, modular arithmetic, powers and roots, numeric traps, a 30-problem catalog, and rapid revision.

The comprehensive audit added focused series-native chapters for Stages 4-17. Each provides a topic-specific recognition map, first-principles invariant or proof, one complete assertion-tested Java 21 companion, dry runs, complexity, boundary failures, exercises with model checkpoints, and SDE-2 production follow-ups. This replaces broad excerpts that previously caused repeated or off-topic material across the DSA PDFs.

Parts 18H-18J are also series-native. They form the backend specialist track: Spring Boot and REST; SQL, transactions, JPA/Hibernate, and caching; then capacity, consistency, Kafka, resilience, sagas, observability, and system design. Dependency-requiring framework sketches are labeled explicitly, while each volume includes a separate dependency-free Java 21 model that is compiled and executed by validation.

## Master-source grouping

| Focused stage | Principal master sources |
|---:|---|
| 2 | Chapter 42, Appendix B, and measurement context from Chapter 39 |
| 3 | Chapters 13-15, 20, 25, 30, 48, and Appendix A |
| 4 | Fixed-width numeric semantics, the Stage 1 bridge, and a focused bit-pattern chapter |
| 5 | Focused series-native loop and index patterns |
| 6 | Master array/list/sort semantics plus focused array interview patterns |
| 7 | Master string and text semantics plus focused string interview patterns |
| 8 | Master equality/collection/hash semantics plus focused hashing and prefix-state patterns |
| 9 | Focused recursion and backtracking chapter |
| 10 | Focused linked-list pointer chapter |
| 11 | Focused stack, queue, deque, and monotonic-pattern chapter |
| 12 | Focused binary-search chapter |
| 13 | Focused tree, BST, and trie chapter |
| 14 | Focused heap, priority-queue, selection, and Top-K chapter |
| 15 | Focused graph-modeling and graph-algorithm chapter |
| 16 | Focused greedy recognition and proof chapter |
| 17 | Focused dynamic-programming derivation chapter |
| 18A | Master Chapters 1-10 |
| 18B | Master Chapters 16-24 and Appendix D |
| 18C | Master Chapters 25-32 and Appendix B |
| 18D | Master Chapters 11 and 33-38 |
| 18E | Master Chapters 39-41 and Appendix C |
| 18F | Master Chapters 49-52 |
| 18G | Master Chapters 48, 53-54 and Appendices E-G |
| 18H | Four series-native Spring Boot and REST chapters |
| 18I | Four series-native persistence, SQL, JPA, and caching chapters |
| 18J | Four series-native distributed-systems and system-design chapters |

## Cross-reference policy

Master chapter numbers retained in extracted prose are labeled as `Master Chapter` or `Master Chapters`. Focused chapter numbers are local to each PDF. The master book remains the stable, comprehensive reference, while focused PDFs provide shorter learning and revision paths.

No valuable master source is deleted. Consolidation happens through selection at build time, and future edits should continue to land in the canonical master source unless a topic is explicitly series-native.
