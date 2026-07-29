# PDF Library Organization

The publishing workspace separates stable artifact identity from reader navigation.

## Canonical artifacts

`dist/` contains one copy of every PDF under its stable filename. Repository links, the web catalog, checksums, and external bookmarks use those names. Existing files are not renamed merely to improve the curriculum order.

Open `dist/00-START-HERE.md` or the series-index PDF first. Choose one segment:

```text
Java Engineering (JAVA 01-09)
Data Structures and Algorithms (DSA 01-17)
System Design and Backend (SD 01-14)
```

Every focused PDF displays its segment code and local book position. Previous/next navigation stays inside the current segment. The full roadmap lists all three segments so a reader can switch intentionally.

## Generated folder library

Create a locally organized library without moving or renaming committed artifacts:

```bash
python3 scripts/organize_pdf_library.py
```

The command creates:

```text
output/reader-library/
|-- 00-start-here/
|-- 01-java/
|-- 02-dsa/
|-- 03-system-design/
+-- README.md
```

Focused copies use prefixes such as `JAVA-01`, `DSA-01`, and `SD-01`; the stable canonical filename remains visible after that prefix. The generated `output/` tree is ignored by Git because it contains byte-for-byte copies of reviewed files from `dist/`.

Validate all assignments without copying:

```bash
python3 scripts/organize_pdf_library.py --check
```

The check fails if a focused book is missing, duplicated, absent from a segment, out of order, or missing its PDF. It also verifies the series index and master book.

## Maintainer rule

When a book is added or renamed:

1. update `publishing/series.json`;
2. assign the book to exactly one segment;
3. preserve `output_name` after publication;
4. rebuild the series index and artifact manifest;
5. run the organization check; and
6. synchronize and rebuild the web catalog.

Do not commit generated copies from `output/reader-library/` and do not move stable PDFs without a migration plan for published links.
