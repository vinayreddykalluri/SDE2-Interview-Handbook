# Learning Portal

The portal is a lightweight, responsive shell around the canonical handbook. It provides track selection, module discovery, progress state, and links into the searchable MkDocs site.

## Ownership

- `index.html` and `assets/` own portal presentation and behavior.
- `content/coding-foundations.json` owns compact discovery metadata for the 19 foundation modules.
- `content/books.json` is a generated catalog for the 28 focused PDFs and the complete release.
- Canonical lesson prose remains under `docs/` and is not duplicated here.
- `tooling/automation/build_site.py` copies this shell to `site/` and mounts MkDocs at `site/docs/`.

The book catalog is derived from `books/java-sde2-interview-preparation-series/publishing/series.json` and `dist/manifest.json`. Update those canonical files, then run `make sync-book-catalog`; do not edit `content/books.json` by hand. `make validate-web` fails if the catalog is stale.

Run `make validate-web` after portal changes and `make serve-web` to inspect the complete local experience at [http://127.0.0.1:8000/](http://127.0.0.1:8000/).

The catalog uses stable GitHub release URLs, so readers can download books from local previews and deployed builds without copying PDFs into the website bundle.
