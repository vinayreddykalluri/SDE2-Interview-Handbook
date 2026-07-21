# SDE2 Interview Handbook

A production-ready, printable handbook for interview preparation in Java, JVM internals, and SDE-2 level algorithmic design.

## Purpose

This repository is a documentation platform with:

- Structured Markdown-first sources.
- Searchable MkDocs website.
- Automated PDF and DOCX artifact generation.
- Consistent chapter structure for all content.
- Practical Java and DSA interview preparation at the SDE-2 level.

## Repository structure

- `docs/` – source Markdown and assets.
- `scripts/` – automation for building and validation.
- `templates/` – PDF and DOCX templates.
- `output/` – generated books.
- `.github/workflows/` – GitHub Actions automation.

## Installation

1. `cd SDE2-Interview-Handbook`
2. `make install`

## Start the local documentation site

- `make serve`

Then open `http://127.0.0.1:8000`.

## Generate print artifacts

- `make build-pdf` builds per-volume and combined PDFs.
- `make build-docx` builds per-volume and combined DOCX.
- `make build-all` runs both PDF and DOCX generation.

Artifacts are produced under:

- `output/pdf/`
- `output/docx/`
- `output/combined/`

## Add a chapter

1. Add the Markdown file in the appropriate `docs/volume-xx-.../` folder.
2. Use the chapter template in `docs/chapter-template.md`.
3. Add the chapter path to `mkdocs.yml` under the volume section.
4. Ensure all required headings exist.

## Add a diagram

1. Add Mermaid code block using ```mermaid.
2. Use clear labels and keep complexity visible.
3. Keep one major diagram per conceptual section when possible.

## Print the final book

1. `make build-pdf`
2. Open `output/combined/SDE2-Interview-Handbook.pdf`.
3. Confirm US Letter margins and page breaks in generated output.

## Troubleshooting

- Missing plugin: verify `make install` completed.
- Mermaid not rendering in PDF: check mermaid converter tool availability in your environment and verify `build_pdf.py` logs.
- Broken links: run `make validate` and fix markdown links before committing.
- Large generated files: clear outputs with `make clean` and rebuild.

## macOS instructions

```bash
brew install python pandoc
make install
make build-all
```

## Linux instructions

```bash
sudo apt-get update
sudo apt-get install -y python3 python3-pip pandoc
make install
make build-all
```

## Known current scope

- Volume 1 is fully written.
- Volumes 2 through 19 have placeholder indices and structure for expansion.
