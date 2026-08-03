# Downloads and Printing

The repository provides a versioned Java SDE-2 PDF series and can also generate printable handbook output from the website's Markdown.

## Download the published book series

Open the [latest GitHub release](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/releases/latest) for a versioned snapshot. Every focused module is attached as an individually named PDF, together with the complete master PDF and integrity manifest.

If you are deciding what to read next, use the [interactive catalog](https://vinayreddykalluri.github.io/SDE2-Interview-Handbook/#books) or open the [complete web book library](https://vinayreddykalluri.github.io/SDE2-Interview-Handbook/books/). Every focused book is available as searchable canonical chapters with exercises, solutions, a code index, and next-page navigation. Its card also downloads the current PDF committed on `master`, so the web reader, source, and PDF advance together when an approved update is merged.

The canonical published files are also versioned under [`books/java-sde2-interview-preparation-series/`](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/tree/master/books/java-sde2-interview-preparation-series).

Choose Java Engineering, Data Structures and Algorithms, Frameworks/Data/Messaging, or System Design, then follow the books inside that segment in order. The canonical `dist/` library groups all 43 PDFs under `00-start-here`, `01-java`, `02-dsa`, `03-frameworks`, and `04-system-design` without changing stable release filenames.

## Build locally

For the website-derived handbook, install Pandoc and XeLaTeX in addition to the normal site prerequisites.

```bash
make install
make build-all
```

Outputs:

- `output/pdf/` contains one PDF per module.
- `output/docx/` contains one DOCX per module.
- `output/combined/` contains the complete handbook in both formats.

## Print settings

- Paper: US Letter
- Scale: 100 percent
- Two-sided: long-edge binding when supported
- Color: color or grayscale
- Browser headers and footers: disabled when printing the website
- Background graphics: enabled for callouts and code shading

Inspect code blocks, wide tables, and diagrams in print preview before producing a large physical copy.

## Website printing

For a single chapter, use the browser's print command on that chapter. The custom stylesheet applies print margins, heading page-break rules, table handling, and monochrome-safe callouts.

## Artifact trust

Use artifacts from successful runs on `master` or from a tagged release. The workflow builds from committed source and applies the same structural, link, and Java checks used for contributions.

Published book assets include `manifest.json` with page counts, byte sizes, and SHA-256 hashes for integrity checks.
