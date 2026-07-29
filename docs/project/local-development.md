# Local development

This repository supports four outputs from one source tree:

- The web portal at `/`
- The searchable MkDocs handbook at `/docs/`
- The complete generated web-book library at `/books/`
- Printable PDF and editable DOCX books under `output/`

## One-command macOS setup

```bash
make bootstrap
```

The bootstrap checks Homebrew and Apple command-line tools, installs any missing
Python, Java 21, Node.js, GitHub CLI, Pandoc, and Tectonic commands, creates `.venv`,
and installs the pinned Python requirements. It is safe to run again.

Inspect the environment without changing it:

```bash
make doctor
```

## Validate and build everything

```bash
make validate
make build-all
```

`make validate` checks repository layout, Markdown structure and links, all Java
examples, and portal metadata. `make build-all` creates the portal, documentation
site, individual and combined PDFs, and individual and combined DOCX files.

PDF generation automatically prefers `xelatex` when installed and otherwise uses
the lightweight `tectonic` engine. To select an installed engine explicitly:

```bash
PDF_ENGINE=tectonic make build-pdf
```

## Open the complete local web experience

```bash
make serve-web
```

Open these addresses after the server starts:

- Portal: <http://127.0.0.1:8000/>
- Searchable docs: <http://127.0.0.1:8000/docs/>
- Backend SDE-2 track: <http://127.0.0.1:8000/docs/backend-interview/>
- Downloads: <http://127.0.0.1:8000/downloads/>
- Published book catalog: <http://127.0.0.1:8000/#books>
- Complete web books and code indexes: <http://127.0.0.1:8000/books/>

Stop the server with `Ctrl+C`.

## Output flow

```mermaid
flowchart LR
    A["Handbook Markdown"] --> B["Handbook MkDocs build"]
    J["Canonical book Markdown + Java"] --> K["Complete web-book build"]
    C["Web portal source"] --> D["Unified site directory"]
    B --> D
    K --> D
    J --> E["Pandoc"]
    E --> F["Tectonic or XeLaTeX"]
    F --> G["Printable PDFs"]
    E --> H["Editable DOCX books"]
    D --> I["Local HTTP server"]
```

## GitHub Actions status

Published-book source and Java validation runs in `.github/workflows/validate-books.yml`.
Website deployment and the legacy root artifact workflow remain under
`.github/workflows-disabled/` until they receive separate approval.
