# Focused Java SDE-2 PDF Series

This directory contains the source, build workspace, validation artifacts, and distributable PDFs for the 18-stage Java SDE-2 Interview Preparation Series.

## Directory layout

```text
series/
  series.json       canonical stage and physical-volume manifest
  volumes/          series-native source, code, and diagrams
  build/            assembled per-volume Markdown and Pandoc AST files
  dist/             final PDFs, series index, and artifact manifest
  tmp/pdfs/         Poppler render output used for visual QA
```

The complete master sources remain under `book/`. The focused builder selects entire chapters or named sections from those sources, renumbers them locally, and adds series-specific front matter and navigation. Stable volume numbers remain unchanged, while `learning_order` in `series.json` defines the reader path: Java Foundations (03), Time and Space Complexity (02), Number Systems (01 and 01B), then Volumes 04-17 and the advanced collection.

## Build commands

```bash
python3 scripts/build_series.py
python3 scripts/build_series.py --volume 01 --skip-index
python3 scripts/build_series.py --volume 18A --skip-index
```

The build uses Java 21 examples, Pandoc JSON, ReportLab, syntax-aware code rendering, deterministic PDF metadata, US Letter pages, embedded bookmarks, and relative sibling-PDF links. Headings stay with their first figure/table/code unit, tables split only by whole rows, and long code listings use balanced continuation panels. Every focused volume has shared modern text-safe cover artwork, a compact readiness gate, local navigation, a complete roadmap, and an editorial-leadership page with LinkedIn, GitHub, and individual-credit guidance.

## Reading order

Use the root `SERIES_ROADMAP.md` or `dist/Java-SDE2-Interview-Preparation-Series-Index.pdf`. The absolute-beginner route is Java Foundations first, Complexity second, and Number Systems third. The roadmap contains 18 learning steps and 28 physical topic PDFs because Number Systems has two linked parts and the advanced step is divided into Parts A-J. Parts 18H-18J form a focused backend specialist track for Spring, persistence, messaging, distributed systems, and system design.

## Open-source publishing

Canonical source, code, diagrams, release PDFs, and `dist/manifest.json` are published in the repository. Regenerable `build/` and `tmp/` workspaces remain ignored. Building the series never stages, commits, pushes, merges, or opens a pull request automatically.

Book content is licensed under CC BY 4.0 and code under MIT. Individual authorship is recorded in the root `AUTHORS.md` and Git history. Vinay Reddy Kalluri serves as series creator, founding author, Editor-in-Chief, and Chief Auditor.
