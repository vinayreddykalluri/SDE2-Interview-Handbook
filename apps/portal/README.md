# Learning Portal

The portal is the responsive entry point to the canonical Java SDE-2 curriculum. It explains the single learning order, renders the 28-book catalog, and keeps web reading and PDF download choices together at every step.

The shared navigation contract is **Home · Study path · Practice · About · GitHub**. The numbered web books are the only curriculum route, and each book exposes its matching PDF as an offline format. Keep those labels and destinations consistent in the portal, the MkDocs theme override, the generated book reader, and the 404 recovery page. Page-level controls must remain contextual; do not add a second roadmap or competing global menu.

## Ownership

- `index.html` and `assets/` own portal presentation and behavior.
- `content/coding-foundations.json` owns compact discovery metadata for the 19 foundation modules.
- `content/books.json` is a generated catalog for the 28 focused books, complete web-reader and code routes, current PDFs, quick lessons, and canonical Markdown chapter previews.
- Canonical lesson prose remains under `docs/` and is not duplicated here.
- `tooling/automation/build_book_web_library.py` renders every Markdown source declared by every volume into `site/books/`, with a searchable contents tree and code index.
- `tooling/automation/build_site.py` copies this shell to `site/`, mounts the concise handbook at `site/docs/`, and builds the complete books at `site/books/`.

The book catalog is derived from `books/java-sde2-interview-preparation-series/publishing/series.json`, the Markdown files listed by each volume, optional Java companions, and `dist/manifest.json`. Update those canonical files, then run `make sync-book-catalog`; do not edit `content/books.json` by hand. `make validate-web` fails if the catalog is stale, a route has no Markdown page, a complete-book path is unsafe, or web document and code counts drift from the canonical sources.

Run `make validate-web` after portal changes and `make serve-web` to inspect the complete local experience at [http://127.0.0.1:8000/](http://127.0.0.1:8000/).

Primary download actions use the PDF committed on `master`, so the deployed website and its downloadable book update together without copying PDFs into the website bundle. The catalog also retains the versioned release URL for archival snapshots. Every card presents the complete web book first and its matching PDF second; code and source links remain contextual supporting actions. The web reader is generated—not hand-copied—so an approved Markdown or Java companion change reaches both publishing formats through the normal build.
