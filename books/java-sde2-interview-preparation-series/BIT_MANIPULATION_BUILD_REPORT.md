# Bit Manipulation Build Report

## Outcome

The existing publishing system successfully rebuilt the enhanced canonical Bit Manipulation volume. The output filename, shared modern cover, fonts, margins, syntax highlighting, page numbers, bookmarks, navigation, author page, and PDF infrastructure were preserved.

## Canonical source files

- `series/series.json`
- `series/volumes/04-bit-manipulation-in-java/chapters/01-bits-and-java-operators-from-zero.md`
- `series/volumes/04-bit-manipulation-in-java/chapters/02-masks-core-techniques-and-shortcuts.md`
- `series/volumes/04-bit-manipulation-in-java/chapters/03-xor-patterns-and-prefix-state.md`
- `series/volumes/04-bit-manipulation-in-java/chapters/04-subsets-submasks-and-compact-state.md`
- `series/volumes/04-bit-manipulation-in-java/chapters/05-sde2-bit-interview-patterns.md`
- `series/volumes/04-bit-manipulation-in-java/chapters/06-java-apis-production-and-revision.md`
- `series/volumes/04-bit-manipulation-in-java/exercises/01-bit-manipulation-practice-lab.md`
- `series/volumes/04-bit-manipulation-in-java/solutions/01-bit-manipulation-practice-solutions.md`
- `series/volumes/04-bit-manipulation-in-java/code/BitManipulationExamples.java`
- `scripts/build_series.py`

## Chapters audited and changed

| Measure | Result |
|---|---:|
| Previous published chapters | 2 |
| Previous PDF pages | 28 |
| Final published chapters | 9 |
| Chapters substantially rewritten | 2 source concepts redistributed across 6 teaching chapters |
| New practice/solution chapters | 2 |
| New executable companion chapter | 1 |
| Final PDF pages | 109 |

## Topics added or substantially expanded

- bit positions, binary/hex reading, and fixed-width interpretation;
- truth-table derivation for all Java bitwise operators;
- signed and logical shifts, negative rounding, promotion, and shift masking;
- validated masks, range fields, low-bit identities, power tests, and Java utilities;
- Hamming distance, bit reversal, count-bits recurrence, and ceiling powers;
- missing number, XOR occurrence families, prefix XOR, integer range XOR, and target-XOR subarrays;
- duplicate subset semantics, streaming, submasks, `O(3^n)`, Gray code, and compact state;
- maximum-XOR trie, constrained queries, range AND, total bit counts, significant complement, minimum XOR pair, OR frontier, and bit addition;
- `BitSet`, `EnumSet`, `BigInteger`, atomic flags, packed schemas, trust boundaries, and byte order; and
- interview selection guides, traps, assessments, and correction workflow.

## Accuracy and boundary improvements

- `1` versus `1L` shift width is explicit in every relevant pattern.
- Invalid bit indexes are rejected instead of relying on Java distance masking.
- Width zero and full-width mask cases are handled separately.
- `>>` versus `>>>`, negative rounding, and small-type promotion are demonstrated.
- Positive power-of-two detection rejects zero and MIN_VALUE.
- Sign-bit masks remain valid and are never passed through `Math.abs`.
- XOR solutions state exact occurrence-count promises and validation limits.
- Triple reconstruction includes bit 31.
- Subset output space and submask termination are reported accurately.
- Signed versus unsigned maximum-XOR objectives are separated.
- Packed-field values, schema versions, unknown bits, and atomic updates are validated.

## Practice inventory

| Practice type | Count |
|---|---:|
| Numbered conceptual questions | 30 |
| Numbered output questions | 20 |
| Numbered debugging exercises | 20 |
| Numbered coding tasks | 24 |
| Numbered interview follow-up chains | 15 |
| Cumulative assessments | 3 |
| Final readiness assessment | 1 |
| Executable Java checks | 40 |

Solutions are separated into Chapter 8 and explain reasoning, contract, failing cases, and engineering trade-offs.

## Code validation

| Measure | Result |
|---|---:|
| Java Markdown fences | 125 |
| Complete standalone companions | 1 |
| Successfully compiled | 1 |
| Failed compilation | 0 |
| Executed companions | 1 |
| Behavioral checks passed | 40 |
| Behavioral checks failed | 0 |
| Output mismatches | 0 |

Observed output:

```text
PASS 40 Bit Manipulation checks
```

See `BIT_MANIPULATION_CODE_VALIDATION.md` for the validation policy and repository-wide warnings.

## Build command

Executed from `/Users/vinayreddykalluri/Documents/Java SDE 2 Interview Book`:

```bash
/Users/vinayreddykalluri/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 \
  scripts/build_series.py --volume 04 --skip-index
```

Observed result:

```text
04: /Users/vinayreddykalluri/Documents/Java SDE 2 Interview Book/series/dist/Java-SDE2-DSA-04-Bit-Manipulation-in-Java.pdf (109 pages)
```

## Final PDF

- Path: `/Users/vinayreddykalluri/Documents/Java SDE 2 Interview Book/series/dist/Java-SDE2-DSA-04-Bit-Manipulation-in-Java.pdf`
- Page count: **109**
- File size: **3,901,568 bytes**
- SHA-256: `bb05810581843a230158ff47a1d1d607b862c9c086243e9f47ce0035f1ea0908`
- Page size: US Letter, 612 x 792 points
- Metadata title: `Bit Manipulation in Java`
- Metadata subject: `From First Bits to SDE-2 Interview Techniques`

The artifact manifest matches the final page count, byte size, and SHA-256.

The final cover and footers identify this as **Learning Step 4 - Volume 4 of 18**, consistent with the series index. Number Systems Parts A and B are two physical PDFs within Learning Step 3.

## PDF pages inspected

Rendered with Poppler and visually inspected:

- page 1: cover and safe text area;
- page 3: contents and chapter navigation;
- page 15: beginner-to-core chapter transition;
- page 24: shortcut table and code blocks;
- page 49: maximum-XOR trie code split;
- page 70: practice-lab opener;
- pages 83-84: solution opener and continuation after heading repair;
- page 93: executable companion opener;
- page 106: practice handoff and sibling links; and
- page 108: author bio and profile links.

No clipped code, split table rows, overlapping cover decoration, missing headings, or unreadable text remained. The first render exposed an overlong Chapter 8 heading; it was shortened to `Practice Solutions and Reasoning`, rebuilt, and re-inspected successfully.

## PDF checks

| Check | Result |
|---|---|
| Rebuilt with existing toolchain | passed |
| Table of contents includes all nine chapters | passed |
| Section bookmarks | passed |
| Required content markers | 11 of 11 |
| Near-empty body pages | 0 |
| Cover overlap | none |
| Tables and code fit | passed on inspected pages |
| Author page and links | passed |
| Manifest integrity | passed |

## Remaining warnings and boundaries

- The repository-wide source validator retains an unrelated Number Systems chapter-count invariant and a Volume 02 duplicate-companion classification. Volume 04 passes its targeted canonical validator.
- XOR linear bases, full bitmask dynamic programming catalogs, compressed bitmap internals, and concurrency memory-model depth remain intentionally outside this focused module.
- No sibling PDF, shared cover asset, or Git state was modified by the targeted Volume 04 build.

## Final condition

The module now has publication-range depth and a clear foundation-to-SDE-2 sequence without unnecessary framework or mathematical detours. Source, Java, PDF, visual, navigation, and manifest checks pass for Volume 04.
