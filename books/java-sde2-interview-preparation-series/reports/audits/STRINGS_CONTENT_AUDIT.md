# Strings and String Patterns Content Audit

## Scope and canonical sources

This audit covers Volume 07, `Strings and String Problem-Solving Patterns`. The existing ReportLab publishing pipeline, cover system, typography, navigation, author profile, and stable output filename remain canonical.

Before enhancement, Volume 07 produced a 32-page PDF from selected portions of broad master chapters plus one 4,502-word native SDE-2 chapter. The source was mostly accurate, and it included palindrome, anagram, builder, longest-unique-window, KMP, rolling hash, Z, and parsing examples. Its central weakness was sequence: a reader encountered Unicode choices and advanced search patterns before receiving a complete String foundation. It had only seven embedded exercises, no separated solution guide, no focused diagram set, and no standalone Java 21 validation contract.

## Previous chapter inventory

| Area | Previous depth | Existing strength | Previous limitation |
|---|---|---|---|
| String identity/value/pool | Adequate mapped | Correctly separated identity and content | Shared with Arrays and lacked a from-zero reference/immutability path |
| Core String API | Too shallow | Selected operations appeared in examples | No sequential API toolkit, null/empty/blank model, substring contract, or output questions |
| Unicode | Adequate compressed | Distinguished char/code point/byte/user-visible character | Needed diagrams, index-conversion guidance, normalization, locale, and output-boundary warnings |
| StringBuilder | Adequate | Correctly warned about repeated concatenation | Needed first-use API, capacity/length, ownership, delimiter, and char-array comparisons |
| Parsing and splitting | Adequate | Strict parser was valuable | Regex split traps, trailing fields, grammar design, digit-policy consistency, and charset boundary were too thin |
| Palindrome/anagram | Adequate | Correct core implementations | Needed baselines, invariants, one-deletion variation, grouping, signatures, and dry runs |
| Sliding windows | Strong but compressed | Longest unique pattern present | Needed fixed windows first, at-most/exactly K, minimum cover, anagram windows, replacement state, and invalid-window criteria |
| Pattern search | Adequate | KMP, rolling hash, and Z were introduced | Needed naive derivation, LPS walkthrough, all/overlapping matches, collision verification, and workload choice |
| Interview engineering | Too shallow | Some production notes existed | Needed a repeatable contract/baseline/invariant/test/complexity playbook |
| Practice and solutions | Too shallow | Seven exercises | No knowledge/output/debug banks, cumulative gates, or separated reasoning solutions |
| Validation | Missing | Snippets were plausible | No warnings-as-errors companion, deterministic output, or differential search test |

## Content-quality matrix

| Topic | Previous quality | Beginner clarity | Interview relevance | Java accuracy | Recommended action | Final action |
|---|---|---|---|---|---|---|
| References and immutability | Adequate mapped | Medium | Core | Accurate | Create focused first chapter | Added object/reference diagram, reassignment, `final`, pass-by-value, and traps |
| Literals, pool, `new String` | Adequate | Medium | Core | Accurate | Separate identity from value | Added runnable identity/value examples and explicit non-reliance rule |
| Core methods and indexes | Too shallow | Low | Core | Accurate | Teach before patterns | Added length, charAt, substring, search, replacement, strip, conversion, and complexity |
| Null/empty/blank | Missing | Low | Core | N/A | Add contract model | Added decision diagram, guard ordering, and policy examples |
| Equality and ordering | Adequate | Medium | Core | Accurate | Add null and compare sign rules | Added `Objects.equals`, `compareTo`, case, locale, and value policy |
| UTF-16 and code points | Adequate compressed | Medium | High | Accurate | Visualize unit differences | Added four-unit diagram, traversals, index conversion, and code-point result contracts |
| Grapheme/normalization/locale | Too shallow | Low | SDE-2 | Accurate | Add bounded introduction | Added NFC, combining sequences, `Locale.ROOT`, limitations, and cross-references |
| Byte/charset boundary | Too shallow mapped | Low | High | Accurate | Add explicit round trip | Added UTF-8 example, default-charset warning, and streaming boundary |
| Builder and output | Adequate | Medium | Core | Accurate | Add API/ownership details | Added construction diagram, capacity, delimiters, char arrays, and amortized cost |
| Regex split and parsing | Adequate compressed | Medium | Core | Accurate | Expand real traps | Added quoting, trailing fields, grammar consistency, strict min/max parsing, and overflow derivation |
| Palindrome/two pointers | Adequate | Medium | Core | Accurate | Add invariant and variations | Added copy baseline, exact/normalized/one-deletion methods and figure |
| Frequency/anagrams | Adequate | Medium | Core | Accurate | Add alphabet choices | Added sort baseline, fixed array, code-point map, grouping signature, and figure |
| Core transformations | Too shallow | Low | Core | Accurate | Add prefix/runs | Added longest common prefix and run encoding with grammar boundaries |
| Fixed windows | Missing | Low | Core | N/A | Teach before variable window | Added exact-size initialization, rolling update, validation, and cost |
| Longest unique window | Strong compressed | Medium | Core | Accurate | Add two formulations and dry run | Added count and last-position forms, result tie policy, figure, and Unicode version |
| At-most/exactly K | Missing | Low | High | N/A | Add SDE-2 state derivation | Added map repair invariant and counting identity using long |
| Anagram/minimum/replacement windows | Too shallow | Low | High | Accurate | Add complete patterns | Added nonzero-count, satisfied-types, stale-max proof boundary, and diagrams |
| Naive search | Too shallow | Low | Core | Accurate | Establish baseline | Added last alignment, empty pattern, proof, and complexity |
| KMP | Adequate compressed | Medium | High | Accurate | Derive LPS and overlaps | Added full prefix walkthrough, invariants, first/all match, and figure |
| Rolling hash/Z | Adequate | Medium | SDE-2 | Mostly accurate | Strengthen trade-offs | Added safe long arithmetic, collision verification, separator warning, and choice table |
| Production text boundaries | Too shallow | Low | SDE-2 | Accurate | Add operational follow-ups | Added limits, redaction, regex safety, Unicode security, streaming, and caching |
| Practice/solutions | Too shallow | Low | High | Accurate | Separate progressive banks | Added 78 lab items, chapter drills, rubrics, and reasoning solutions |
| Executable validation | Missing | N/A | High | N/A | Add Java 21 companion | Added 50 warning-free deterministic checks and seeded differential test |

## Priority findings

### Critical

- No single catastrophic factual error dominated the prior book; the critical publication defect was that SDE-2 material arrived before the SDE-1 mechanics required to reason about it.
- A physical String index, code-point position, byte offset, and grapheme position could be discussed without enough guidance to preserve result-index correctness.
- The canonical volume had no executable evidence for its complete examples.
- One dense native chapter was difficult to navigate, revise, and validate.

### High value

- Establish immutability, references, equality, core APIs, null/empty/blank, indexes, builders, parsing, and Unicode units before algorithm patterns.
- Derive each pointer, frequency, window, and search technique from a baseline and invariant.
- Label alphabet and Unicode assumptions on every fixed-array or `char` implementation.
- Add fixed windows before variable windows and naive search before KMP.
- Separate practice and solutions and include output prediction, debugging, coding, follow-ups, cumulative gates, and a readiness design.
- Provide visual state models for the mechanics most readers struggle to dry-run.

### Nice to improve

- The website can later add interactive pointer and LPS traces while keeping the printable diagrams canonical.
- Company-tagged timed problem sets can be added as separate practice without turning this prerequisite volume into an uncurated question dump.
- Locale collation and full grapheme segmentation remain appropriate for Advanced Java or a focused international-text contribution.

## Final chapter inventory

| Order | Chapter | Main dependency | Purpose |
|---:|---|---|---|
| 1 | String Foundations from Zero | Java Fundamentals | Establish values, references, immutability, equality, indexes, core APIs, and contracts |
| 2 | Traversal, Unicode, and Text Boundaries | Chapter 1 | Choose UTF-16, code point, grapheme, or byte units safely |
| 3 | Building, Parsing, and Conversion | Chapters 1-2 | Build output, split, parse, validate, encode, and format without hidden defects |
| 4 | Two Pointers, Frequency, and Core String Patterns | Chapters 1-3 | Derive palindrome, anagram, grouping, prefix, and run patterns |
| 5 | Sliding Windows and Substring State | Chapter 4, Loop/Array foundations | Progress from fixed windows to unique, K-distinct, anagram, cover, and replacement state |
| 6 | Pattern Matching and Search | Chapters 1-5, Complexity | Derive naive, KMP, rolling hash, and Z choices |
| 7 | SDE-2 String Interview Playbook | All teaching chapters | Integrate contracts, invariants, Java quality, tests, complexity, and production boundaries |
| 8 | Strings Practice Lab | All teaching chapters | Retrieval, output, debugging, coding, follow-ups, and readiness |
| 9 | Practice Solutions | Practice Lab | Explain mechanics, corrections, trade-offs, and rubrics |
| 10 | 50 Executable String Checks | Core chapters | Provide a warning-free behavioral reference |

## Audit conclusion

The previous volume was a useful SDE-2 outline but not a publishable beginner-to-SDE-2 learning path. The enhanced canonical volume now contains 15,657 words across seven focused teaching chapters, a separated 78-item lab and solution guide, ten reproducible diagrams, and one strict Java 21 companion. It preserves the existing publishing system and stable PDF identity while closing the foundational, algorithmic, practice, and validation gaps.

## 2026-08-02 depth pass

The new contract/interview chapter distinguishes UTF-16 units, code points, and grapheme clusters before choosing an algorithm. It adds negative-accumulation integer parsing, a full KMP prefix-state dry run, normalization/locale boundaries, a ten-case failure matrix, six live interviews, and twelve rapid model answers. The added companion differential-tests parsing and KMP behavior and verifies that code-point reversal preserves a supplementary character.
