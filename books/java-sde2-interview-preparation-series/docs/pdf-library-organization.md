# PDF Library Organization

The publishing workspace separates artifact identity from reader navigation.

## Canonical reviewed artifacts

`dist/` contains one copy of every reviewed PDF under its stable release filename. These names are used by repository links, the web catalog, release assets, checksums, and external bookmarks. They are not renamed merely to change reading order.

Open `dist/00-START-HERE.md` first. It groups all individual PDFs and numbers them in the prerequisite-correct route:

```text
Java Foundations
  -> Time and Space Complexity
  -> Number Systems and workbook
  -> Bit Manipulation
  -> Loop Mastery
  -> Arrays
  -> Strings
  -> remaining DSA modules
  -> Advanced Java and backend engineering
```

The physical IDs in filenames are stable technical IDs. The public `Study Step` codes are the reader sequence: `01`, `02`, `03A`, `03B`, `04` through `17`, and `18A` through `18J`. The website, PDF covers, roadmap, and organized folder names all use these same codes.

## Generated folder library

Readers who want actual grouped folders can generate a local library without duplicating committed binaries:

```bash
python3 scripts/organize_pdf_library.py
```

The command creates:

```text
output/reader-library/
|-- 00-start-here/
|-- 01-foundations/
|-- 02-core-dsa/
|-- 03-algorithm-strategies/
|-- 04-advanced-java-backend/
+-- README.md
```

Every focused PDF receives its canonical Study Step prefix in the generated library. Suffixes distinguish the Number Systems book/workbook and the ten advanced books without inventing a second order. The canonical filename remains visible after that prefix. The generated `output/` tree is intentionally ignored by Git because it contains byte-for-byte copies of reviewed `dist/` artifacts.

Validate the mapping without copying:

```bash
python3 scripts/organize_pdf_library.py --check
```

The check fails if a focused volume is absent from the learning order, assigned to no group, duplicated, or missing its PDF. It also verifies the series index and master book.

## Maintainer rule

When a volume is added or renamed:

1. update `publishing/series.json`;
2. keep its `output_name` stable after publication;
3. add the volume ID to exactly one group in `scripts/organize_pdf_library.py`;
4. regenerate `dist/00-START-HERE.md` during the editorial change;
5. run the check command; and
6. update the web catalog through the existing catalog synchronization command.

Do not commit generated copies from `output/reader-library/` and do not move stable PDFs into new directories without a migration plan for every published link.
