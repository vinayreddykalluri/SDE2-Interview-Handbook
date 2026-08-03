# GitHub Actions (re-enabled)

This directory previously held `build-books.yml` and `deploy-pages.yml` while
the handbook was validated locally. Both are now live under
`.github/workflows/`, and this directory is kept only for the note below.

## Why they were re-enabled

Building artifacts on a laptop and committing them by hand is what put 163 MB
of PDFs into git history. Automation had to come back before that could be
unwound, so the release path is now:

| Workflow | Trigger | Responsibility |
|---|---|---|
| `validate-books.yml` | PR and push to `master` | Source validation, catalog reconciliation, committed-PDF/manifest agreement, Java compilation |
| `build-books.yml` | PR, push to `master`, manual | Full series rebuild, PDF integrity check, artifact upload |
| `deploy-pages.yml` | Push to `master` touching docs, portal, or book sources | Build and deploy the unified site to GitHub Pages |

## Before editing `build-books.yml`

`scripts/build_book.py` consumes pandoc's native JSON AST and unpacks a Table
node as a 6-tuple. That shape arrived in pandoc-types 1.21, which ships with
pandoc 2.10. Older pandoc emits a 5-field Table and the build dies with:

```
ValueError: not enough values to unpack (expected 6, got 5)
```

Ubuntu 22.04 still packages pandoc 2.9.2.1, so a bare `apt-get install -y
pandoc` reintroduces this whenever the runner image changes. The workflow pins
a pandoc `.deb` by version for that reason, and `build_book.py` now checks the
version up front and fails with an explanation rather than a tuple-unpacking
error deep inside the render.

## Fonts

The build reads only the fonts vendored in
`books/java-sde2-interview-preparation-series/assets/fonts/` and never falls
back to system fonts. Font metrics determine pagination, pagination is
recorded in `dist/manifest.json`, and the manifest is asserted by
`scripts/validate_pdfs.py` — so a host-dependent font would make CI and local
builds disagree about page counts. See that directory's README for the
rationale and for how to change a font safely.
