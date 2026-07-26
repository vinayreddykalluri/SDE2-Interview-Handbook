# Downloads and Printing

The repository provides a versioned Java SDE-2 PDF series and can also generate printable handbook output from the website's Markdown.

## Download the published book series

Open the [latest GitHub release](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/releases/latest). Every focused module is attached as an individually named PDF, together with the complete master PDF and integrity manifest.

The canonical published files are also versioned under [`books/java-sde2-interview-preparation-series/`](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/tree/master/books/java-sde2-interview-preparation-series).

Start with Java Foundations, then Time and Space Complexity, Number Systems, Bit Manipulation, and Loop Mastery before continuing to the remaining DSA modules.

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
