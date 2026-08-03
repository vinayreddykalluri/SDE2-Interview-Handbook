# PDF Library Organization

The publishing workspace keeps stable release filenames while organizing repository artifacts by learning segment.

## Canonical artifacts

`dist/` contains one copy of every PDF under its stable filename, grouped into canonical folders. Repository links and the web catalog use the folder plus filename; tagged GitHub release assets remain flat because release uploads do not preserve directories.

Open `dist/00-start-here/README.md` or the series-index PDF first. Choose one segment:

```text
Java Engineering (JAVA 01-09)
Data Structures and Algorithms (DSA 01-17)
Frameworks, Data, and Messaging (FW 01-12)
System Design (SD 01-02)
```

Every focused PDF displays its segment code and local book position. Previous/next navigation stays inside the current segment. The full roadmap lists all four segments so a reader can switch intentionally.

## Generated folder library

Validate the committed organization, or copy it to a reader-owned location:

```bash
python3 scripts/organize_pdf_library.py
```

The command creates:

```text
output/reader-library/
|-- 00-start-here/
|-- 01-java/
|-- 02-dsa/
|-- 03-frameworks/
|-- 04-system-design/
+-- README.md
```

The generated `output/` tree is ignored by Git because it contains byte-for-byte copies of reviewed files from the canonical `dist/` folders.

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

Do not commit generated copies from `output/reader-library/`. When moving a repository PDF, preserve the filename, update the manifest-owned artifact folder, and retain web-route redirects.
