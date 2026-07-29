# Git and GitHub Build Report

## Publication result

| Item | Result |
|---|---|
| publication status | Published |
| canonical sources | 17 Markdown chapters plus one Java 21 companion |
| previous PDF | 10-page roadmap edition |
| final PDF | 127-page publication edition |
| final size | 3,987,511 bytes |
| SHA-256 | `3f79bd292437f1dfcc45b4393d9ec33db22beda22cb53b82405bd53ece12445f` |
| final path | `dist/Java-SDE2-JAVA-02-Git-and-GitHub.pdf` |

## Content result

- Chapters audited: 1 previous roadmap source.
- Chapters rewritten or replaced: 1 roadmap replaced by 17 instructional chapters.
- Topics added: local state, object graph, collaboration, conflicts, rewrite, recovery, Java hygiene, governance, CI, security, releases, scale, incidents, interviews, practice, and solutions.
- Accuracy boundaries corrected: branch/reference identity, remote-tracking freshness, rebase identity, reset/revert distinction, merge-revert ancestry, secret rotation, workflow trust, release identity, and semantic integration.
- Fenced teaching blocks: 145.
- Structured practice tasks: 146, plus 14 full interview rounds.
- Complex production scenarios: 16.
- Executable Git lab scenarios: 7.
- Java companions added: 1.

## Existing build command

```bash
cd books/java-sde2-interview-preparation-series
python3 scripts/build_series.py --volume GIT --skip-index
```

Result: successful, existing publishing pipeline preserved.

## Java and Git validation

```bash
python3 scripts/validate_series.py --source-only
```

- Git scenarios: 7 passed, 0 failed.
- New Java classes: 1 compiled and executed, 0 failed.
- Full focused Java set: 41 compiled and executed, 0 failed.
- Remaining compiler warnings: 0 under `-Xlint:all -Werror`.

## Web result

Commands:

```bash
python3 tooling/automation/sync_book_catalog.py
python3 tooling/automation/build_site.py
```

The portal now marks JAVA 02 as published and exposes 127 pages, 17 canonical chapter documents, 23,848 indexed words, six indexed Java examples, the code page, source links, and the stable PDF download. The complete web library contains 40 books, 239 documents, and 961 code entries.

## PDF QA

Semantic command:

```bash
python3 scripts/qa_semantic_layout.py \
  --include 'GIT' \
  --output tmp/pdfs/git-book-publication-qa-final \
  --fail-level error
```

Result: 127 pages, 0 errors, 1 reviewed warning. The warning is the correct repeated header on the standard two-page series-roadmap table.

Visual command:

```bash
python3 scripts/qa_render.py \
  dist/Java-SDE2-JAVA-02-Git-and-GitHub.pdf \
  --output tmp/pdfs/git-book-render-final \
  --dpi 110 --columns 4 --rows 4
```

All 127 rendered pages were inspected through eight contact sheets, with high-resolution review of the cover, contents, state diagrams, command tables, YAML, conflict/recovery, governance/security, interview workbook, solutions, Java continuation, roadmap continuation, author, and copyright pages.

Visual result:

- blank candidates: 0;
- edge candidates: 0;
- dark-page candidates: 0;
- clipped code or tables: 0;
- orphan chapter headings: 0;
- content-induced layout repairs: 2 (workbook opener and command-table pipe);
- remaining warning: one approved standard roadmap continuation.

## Library totals after publication

- Focused books: 40 PDFs and 2,386 pages.
- Series index: 17 pages.
- Master book: 616 pages.
- Complete library: 42 PDFs and 3,019 pages.
