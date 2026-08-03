# Focused Book Workspaces

Book-specific source now has four obvious shelves:

- `java/` — JAVA 01-09
- `dsa/` — DSA 01-17
- `frameworks/` — FW 01-12
- `system-design/` — SD 01-02

The folders are ordered with the same public codes used by the website, PDF covers, and download library. `publishing/series.json` is the authoritative mapping from a book to its Markdown, Java companions, PDF name, and segment position.

Use `JAVA-*`, `DSA-*`, `FW-*`, and `SD-*` codes in reader-facing prose and issue titles. The manifest also contains stable internal IDs retained for build and artifact compatibility; those keys are not a second study order. When a script requires `--volume <id>`, translate from the public code through `path_labels` instead of exposing the internal ID as a study step.

`content/master/` is a shared compendium source layer, not a fifth learning segment. A focused book may reference a master chapter when that material has one canonical owner; book-specific explanations, practice, solutions, and labs belong in the appropriate workspace above.

Never edit `build/`, `tmp/`, `site/`, or generated catalog files as the source of a content change. Update canonical Markdown or code, validate it, and regenerate the web and PDF artifacts through the existing publishing commands.
