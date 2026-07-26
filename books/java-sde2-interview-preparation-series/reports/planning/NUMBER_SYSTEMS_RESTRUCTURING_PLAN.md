# Number Systems Restructuring Plan

## Decision

Preserve the existing two-PDF Stage 1 architecture and stable filenames. Reorganize by adding focused 14A and 15A modules and expanding only the prerequisite chapters that own newly required material. Do not rebuild the series index or unrelated volumes.

| Original chapter/module | Problem found | New location | Action | Migration status |
|---|---|---|---|---|
| Chapter 1 | Reader could see signals but lacked an explicit restart route | Chapter 1 | Expand with dependency map, prerequisites, scope, and start guidance | Complete |
| Chapter 2 | Number/digit/sign vocabulary and zero-safe loop were implicit | Chapter 2 | Rewrite opening and add visual 5382 traversal | Complete |
| Chapter 3 | Binary foundation already strong | Chapter 3 | Keep | Complete |
| Chapter 4 | Octal/hex scope already appropriate | Chapter 4 | Keep | Complete |
| Chapter 5 | Base conversion already robust | Chapters 5 and 14A | Keep; add authoritative validation method to companion/index | Complete |
| Chapter 6 | BigInteger/BigDecimal and floating precision decision guidance too concise | Chapter 6 | Expand without duplicating full Java-type book | Complete |
| Chapter 7 | Overflow catalog already strong | Chapters 7 and 14A | Keep; add strict reverse implementation reference | Complete |
| Chapter 8 | Compare/add/modulo present; subtract/digit multiply and visual stream missing | Chapters 8 and 14A | Expand and add compiling methods | Complete |
| Chapter 9 | High-value divisibility rules complete | Chapter 9 | Keep | Complete |
| Chapter 10 | Sieve and factor sum missing; factor pairs lacked a diagram | Chapter 10 and 14A | Expand with factor-sum model, sieve, and two diagrams | Complete |
| Chapter 11 | Modular core complete; inverse intentionally absent | Chapter 14A | Keep core flow; add inverse as SDE-2 follow-up | Complete |
| Chapter 12 | Fast-power visualization and factorial metrics missing | Chapters 12 and 14A | Expand, diagram, and add compiling methods | Complete |
| Chapter 13 | Two's complement needed stronger visual support | Chapter 13 | Add diagram; retain bit-book boundary | Complete |
| Chapter 14 | Thirty patterns no longer covered expanded 52-method requirement | Chapters 14 and 14A | Keep high-frequency catalog; add complete reference and missing explanations | Complete |
| Chapter 15 | Java traps complete | Chapters 15 and 15A | Keep; add 30 retrieval questions in practice bank | Complete |
| Chapter 16 | Original counts were 30/20/20/5 rather than new 40/25/25/10 targets | Chapters 15A and 16 | Add bank and delayed checkpoints; preserve original solutions | Complete |
| Companion library | 30 catalog methods plus helpers, but not all 52 named obligations | `NumberSystemsAlgorithms.java` | Add 16 public methods and supporting extended-GCD record | Complete |
| Boundary suite | No tests for new implementations | `NumberSystemsAlgorithmsTest.java` | Add boundary and behavior coverage | Complete |
| Diagram set | 10 figures; seven requested concepts remained text-only | `assets/11` through `assets/17` | Add dependency, digits, two's complement, factors, sieve, fast power, huge string | Complete |
| Exercise guide | Reported old assessment counts | `exercises/README.md` | Update combined Part B counts and labels | Complete |
| Solution map | Listed only original 30 mappings | `solutions/SOLUTION_MAP.md` | Replace with exact 52-item method map | Complete |
| Volume manifest | Part B advertised only 30 algorithms and omitted new modules | `publishing/series.json` entries 01B only | Update Part B subtitle/outcomes and add two sources | Complete |
| Build/validation | Full-series validation was unnecessary for a targeted content update | Targeted commands | Build 01 and 01B with `--skip-index`; render only those PDFs | Complete |

## Target-topic mapping decision

The supplied fifty-chapter design is implemented as fifty ordered topic units mapped into eighteen canonical modules. This avoids fifty shallow files, repeated cover/transition overhead, and fragmented local contents. The exact mapping is in `NUMBER_SYSTEMS_TOPIC_COVERAGE_MATRIX.md`.

## Preservation and non-impact rules

- Keep `Java-SDE2-DSA-01-Number-Systems-and-Math-Foundations.pdf` and `Java-SDE2-DSA-01B-Number-Systems-Interview-Workbook.pdf` unchanged as filenames.
- Keep the Stage 1 public title stable so roadmaps in unrelated PDFs do not become stale.
- Do not rebuild the series index, master book, or Stages 2-18.
- Do not delete original chapters, diagrams, solutions, or exercises.
- Do not stage, commit, push, merge, or open a pull request.
