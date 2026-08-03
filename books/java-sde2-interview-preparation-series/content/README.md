# Book Content

This directory contains the canonical editable prose for the Java SDE-2 Interview Preparation Series.

## Ownership

```text
content/
|-- master/     Complete Java foundations-to-advanced book chapters and appendices
+-- volumes/    Focused module chapters, exercises, solutions, code companions, and local assets
```

`master/` supports the complete 616-page edition. `volumes/` owns focused material that does not belong in the master source or needs a dedicated beginner-first treatment. The focused builder may also select complete chapters or named sections from `master/`; the exact mapping is defined in [`../publishing/series.json`](../publishing/series.json) and summarized in [`SOURCE_MAP.md`](SOURCE_MAP.md).

Readers navigate by the four public shelf codes: `JAVA-01` through `JAVA-09`, `DSA-01` through `DSA-17`, `FW-01` through `FW-12`, and `SD-01` through `SD-02`. Book order restarts at 01 inside each shelf; there is no longer one global numbered study path.

The manifest's `id` values are stable implementation keys retained for build compatibility. Some predate the public shelves—for example, internal ID `03` maps to `JAVA-01`, `01B` maps to `DSA-03`, and `18A` maps to `JAVA-06`. They are not reader-facing step numbers. Use `path_labels` and `segments[].books` in the manifest when translating an internal ID to a public code or determining publication order.

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
# --volume accepts the stable internal manifest ID.
# Public DSA-02 maps to internal ID 01.
python3 scripts/build_series.py --volume 01 --skip-index
# Public JAVA-06 maps to internal ID 18A.
python3 scripts/build_series.py --volume 18A --skip-index
```

Published PDFs and their integrity manifest live in [`../dist/`](../dist/). Regenerable assembly and visual-QA workspaces remain ignored.
