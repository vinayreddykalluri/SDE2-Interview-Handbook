# Bit Manipulation Content Changelog

## Canonical source changes

| Chapter | Original weakness | Change made | Examples added | Practice added | Accuracy or boundary improvement |
|---:|---|---|---|---|---|
| 1 | Broad numeric extract was not bit-focused | Replaced with bit-first Java foundation | conversions, truth tables, shifts, padded display, status byte | prediction, explanation, debugging | explicit pattern/interpretation split, promotion, shift masking |
| 2 | Core identities compressed into one pattern chapter | Created progressive mask and shortcut chapter | field masks, low bit, power four, flips, reverse, count DP | follow-ups and failure repairs | width 0/64, nonzero trailing-zero sentinel, positive contracts |
| 3 | XOR patterns lacked intermediate bridge problems | Expanded into a dedicated XOR sequence | missing number, prefix ranges, `0..n`, target subarrays | contracts, dry runs, debug tasks | sign-bit reconstruction and malformed-promise warnings |
| 4 | Subsets and submasks were brief | Added duplicate semantics, output accounting, Gray code, and compact state | subset streaming, submask loop, assignment-state DP | complexity proofs and follow-ups | zero-loop, logical complement, feasibility limits |
| 5 | Advanced patterns were listed rather than taught | Added baseline-to-optimization SDE-2 catalog | maximum XOR trie, range AND, total bit count, minimum XOR, OR frontier, bit addition | implementation and design challenges | signed/unsigned objectives, ownership, `long` result |
| 6 | Java representation choices were only mentioned | Added API and production decision chapter | `BitSet`, `EnumSet`, `BigInteger`, flags, atomic updates, byte order | twenty Java traps and rapid revision | mutability, schema, trust-boundary, concurrency rules |
| 7 | Practice ladder had thirteen prompts | Added separated structured lab | 20 output snippets, 20 debug snippets | 30 knowledge, 24 coding, 15 follow-ups, 3 assessments | every task requests width, contract, invariant, and cost |
| 8 | Solutions were embedded as checkpoints only | Added separate reasoning chapter | repaired snippets and model reasoning | guidance for every coded/follow-up task | answers emphasize causes rather than code alone |
| 9 | One embedded toolkit had no explicit validation count | Added dependency-free Java 21 companion | 40 executable checks | runnable retrieval reference | warnings-as-errors compile and exact output contract |

## Structural changes

- Removed the obsolete unreferenced single native chapter after its strong material was preserved and expanded.
- Replaced the generic numeric-source mapping with eight focused native sources.
- Added one canonical Java companion and updated Volume 04 metadata, outcomes, practice ladder, and publication range.
- Preserved the existing builder, cover system, output filename, navigation, fonts, margins, syntax highlighting, author page, and sibling modules.

## Quantified change

| Measure | Before | After |
|---|---:|---:|
| Published pages | 28 | 109 |
| Focused teaching/practice/solution sources | 1 native plus 1 extracted source | 8 native sources |
| Published chapters | 2 | 9 including code companion |
| Conceptual questions | limited embedded prompts | 30 numbered plus chapter checks |
| Output questions | none as a bank | 20 |
| Debugging exercises | none as a bank | 20 |
| Coding tasks | 13 mixed prompts | 24 numbered plus chapter exercises |
| SDE-2 follow-up chains | brief model questions | 15 numbered plus chapter follow-ups |
| Cumulative assessments | 0 | 3 plus final readiness assessment |
| Executable validation | one embedded smoke class | 40 named behavioral checks |

## Cross-book boundaries

- Binary conversion and two's-complement mathematics continue in Number Systems.
- Cost-model detail continues in Time and Space Complexity.
- Fenwick trees continue with range-query structures.
- Full state-design methodology continues in Dynamic Programming.
- Arrays, sorting, hashing, and offline-query families continue in their dedicated modules.
- Concurrency memory semantics and advanced bitmap internals remain outside this volume.
