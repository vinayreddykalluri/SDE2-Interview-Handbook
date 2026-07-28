# Strings and String Patterns Content Changelog

| Chapter/area | Original weakness | Change made | Examples/figures added | Practice added | Accuracy or scope decision |
|---|---|---|---|---|---|
| Learning sequence | One dense SDE-2 chapter plus broad mapped fragments | Replaced with seven prerequisite-ordered teaching chapters, lab, and solutions | Learning-step metadata | Progressive difficulty labels | Preserved builder and output filename |
| String foundations | No dedicated start-from-zero path | Added references, immutability, literals, core API, equality, indexes, null/empty/blank, pass-by-value, and cost model | Two reference/contract diagrams | Foundation checks and tasks | Removed duplicated mapped master selections from Volume 07 |
| Unicode/text units | Correct ideas were compressed | Added UTF-16, code-point traversal, index conversion, reversal boundary, normalization, locale, grapheme, and charset sections | Four-unit figure and dry runs | Output/debug/design work | Avoided claiming char equals visible character |
| Builders/conversion | Builder and parser examples lacked a full toolkit | Added capacity/length, ownership, delimiters, char arrays, regex split, strict parsing, grammar, bytes, and formatting | Builder pipeline figure | Parser/split/debug tasks | Labeled default charset and regex hazards |
| Two pointers | Palindrome was an advanced example | Added copy baseline, exact and normalized variants, one-deletion proof, and reusable invariant | Palindrome pointer figure | Core coding/follow-ups | ASCII and UTF-16 contracts are explicit |
| Frequency patterns | Anagram coverage was narrow | Added sort baseline, fixed array, code-point map, grouping signatures, common prefix, and run encoding | Frequency-state figure | Anagram/grouping/run tasks | Map operations described as expected cost |
| Sliding windows | Began with longest-unique pattern | Added fixed window first, then count/jump unique variants, at-most/exactly K, anagram starts, minimum cover, replacement, and failure criteria | Unique and minimum-cover figures | Multiple window assessments | Monotonic repair is required and proved |
| Search | KMP/hash/Z arrived without full derivation | Added API and naive baselines, LPS walkthrough, first/all KMP, verified rolling hash, Z intuition, choice table, and streaming workload | KMP fallback figure | LPS/KMP/hash/differential tasks | No complexity guarantee attributed to `indexOf` |
| Interview quality | Production notes were scattered | Added contract, recognition map, invariant templates, complete complexity language, test matrix, pacing, and readiness checklist | Pattern decision map | Eight follow-ups and readiness design | Advanced domains cross-referenced instead of duplicated |
| Practice | Seven embedded exercises | Added 24 knowledge, 12 output, 12 debugging, 18 coding, 8 follow-up, 3 cumulative, and 1 final assessment | Runnable snippets | 78 lab items | Simple recall is not labeled SDE-2 |
| Solutions | No separated comprehensive guide | Added reasoning-first answers, repair explanations, coding guidance, and assessment rubrics | Reference reasoning | Delayed feedback | Solutions emphasize contracts/invariants, not only code |
| Code validation | No standalone contract | Added dependency-free Java 21 companion with seeded differential KMP validation | 50 deterministic checks | Executable review | Warnings treated as errors |
| PDF organization | Flat filenames sorted by physical ID | Added `00-START-HERE.md`, organization documentation, and generated grouped reader library | Step-prefixed local copies | Navigation, not content | Stable canonical artifacts and links remain intact |
| Visual learning | No focused String figure set | Added reproducible 2400x1450 print diagrams | Ten PNG figures | Visual dry runs | Generated through repository-owned script |

## Summary

The enhancement concentrates on SDE-1 fluency and SDE-2 reasoning without importing unrelated algorithms. Hash internals, tries, edit-distance DP, full regex and Unicode segmentation, streaming I/O internals, JVM layout, and system search design remain in dedicated volumes.
