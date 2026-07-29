# Learning Portal

The portal is a lightweight, responsive shell around the canonical handbook. It provides track selection, module discovery, progress state, and links into the searchable MkDocs site.

## Ownership

- `index.html` and `assets/` own portal presentation and behavior.
- `content/coding-foundations.json` owns compact discovery metadata for the 19 foundation modules.
- `content/books.json` is a generated catalog for the 28 focused PDFs, matching web reading routes, and canonical Markdown chapter previews.
- Canonical lesson prose remains under `docs/` and is not duplicated here.
- `tooling/automation/build_site.py` copies this shell to `site/` and mounts MkDocs at `site/docs/`.

The book catalog is derived from `books/java-sde2-interview-preparation-series/publishing/series.json`, the Markdown files listed by each volume, and `dist/manifest.json`. Update those canonical files, then run `make sync-book-catalog`; do not edit `content/books.json` by hand. `make validate-web` fails if the catalog is stale, a web-reading route has no Markdown page, or a chapter preview loses its source link.

Run `make validate-web` after portal changes and `make serve-web` to inspect the complete local experience at [http://127.0.0.1:8000/](http://127.0.0.1:8000/).

Primary download actions use the PDF committed on `master`, so the deployed website and its downloadable book update together without copying PDFs into the website bundle. The catalog also retains the versioned release URL for archival snapshots, and the toolbar links to the tagged release. Book cards prefer a concise website lesson when one exists, keep the current publication-depth PDF one click away, and expose canonical source Markdown for review and contribution.
