# Downloads and Printing

The repository generates individual module books and a combined handbook from the same Markdown used by the website.

## Download without a local toolchain

1. Open the [Build Interview Handbook workflow](https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/actions/workflows/build-books.yml).
2. Select the newest successful run on the default branch.
3. Download the PDF, DOCX, or combined artifact bundle.
4. Extract the archive before opening the book.

GitHub requires a signed-in account to download workflow artifacts.

## Build locally

Install Pandoc and XeLaTeX in addition to the normal site prerequisites.

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
