# Book Content

This directory contains the canonical editable prose for the Java SDE-2 Interview Preparation Series.

## Ownership

```text
content/
|-- master/     Complete Java foundations-to-advanced book chapters and appendices
+-- volumes/    Focused module chapters, exercises, solutions, code companions, and local assets
```

`master/` supports the complete 616-page edition. `volumes/` owns focused material that does not belong in the master source or needs a dedicated beginner-first treatment. The focused builder may also select complete chapters or named sections from `master/`; the exact mapping is defined in [`../publishing/series.json`](../publishing/series.json) and summarized in [`SOURCE_MAP.md`](SOURCE_MAP.md).

The physical PDF numbers are stable identifiers. Reader order is defined separately by `learning_order`: Java Foundations (03), Time and Space Complexity (02), Number Systems (01 and 01B), Bit Manipulation (04), Loop Mastery (05), remaining DSA volumes, and Advanced Java (18A-18J).

## Editing rules

- Update an existing canonical source instead of creating a parallel Markdown copy.
- Put focused exercises, solutions, and standalone companions inside the owning volume.
- Keep website curriculum under the repository-level `docs/`; link or adapt material intentionally instead of silently duplicating it.
- Update `publishing/series.json` when a source is added, removed, or moved.
- Update the relevant audit, coverage, validation, or build report under `../reports/`.
- Rebuild and inspect every affected PDF before publication.

## Build and release

```bash
python3 scripts/build_series.py
python3 scripts/build_series.py --volume 01 --skip-index
python3 scripts/build_series.py --volume 18A --skip-index
```

Published PDFs and their integrity manifest live in [`../dist/`](../dist/). Regenerable assembly and visual-QA workspaces remain ignored.
