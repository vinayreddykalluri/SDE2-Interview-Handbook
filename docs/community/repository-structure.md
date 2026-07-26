# Repository Structure

This guide answers two contributor questions: where a change belongs and which other layers must stay synchronized.

## Canonical Hierarchy

```text
docs/
|-- backend-interview/
|   |-- 01-programming-problem-solving/
|   |-- 02-.../
|   +-- NN-topic/
|-- coding-foundations/
|   |-- index.md
|   |-- 01-java-runtime/
|   +-- 19-dynamic-programming/
|-- community/
|-- examples/
+-- assets/

examples/
+-- java/
    +-- src/
        |-- main/java/io/github/vinayreddykalluri/interviewhandbook/
        |   |-- problemsolving/
        |   +-- codingfoundations/<topic>/
        +-- test/java/

books/
+-- java-sde2-interview-preparation-series/
    |-- content/
    |   |-- master/            Master-book chapters and appendices
    |   +-- volumes/           Focused canonical sources and companions
    |-- assets/                Covers and master-book diagrams
    |-- examples/java/         Master-book Maven examples
    |-- publishing/            Series manifest and shared cover artwork
    |-- reports/               Audits, validation, coverage, and build evidence
    |-- docs/                  Editorial standard and reader roadmap
    |-- scripts/               Book-specific build and validation tooling
    +-- dist/                  Reviewed PDFs and artifact manifest

web/
|-- assets/
|-- content/
+-- index.html
```

## Change-Routing Matrix

| Change | Primary location | Usually update too |
|---|---|---|
| Backend interview theory | `docs/backend-interview/NN-topic/` | `mkdocs.yml` and a Java example when implementation is central |
| Algorithm or Java foundation | `docs/coding-foundations/NN-topic/` | `web/content/coding-foundations.json` and semantic Java package |
| Runnable implementation | `examples/java/src/main/java/...` | Linked documentation and smoke tests |
| Navigation | `mkdocs.yml` | Track overview and internal links |
| Portal behavior or appearance | `web/` | `scripts/validate_web.py` when a contract changes |
| MkDocs page chrome | `overrides/` | Light/dark and mobile behavior |
| Build behavior | `scripts/` | `Makefile` and local-development documentation |
| Hosted static delivery | `vercel.json` and `DEPLOYMENT.md` | Web requirements and deployment validation |
| Printable styling | `templates/` | Local PDF and DOCX inspection |
| Published book content or release PDF | `books/java-sde2-interview-preparation-series/` | Book audit/coverage report, companion code, and affected PDF |
| Contribution policy | Root community files or `.github/` | Relevant community page |

## Naming Rules

- Documentation directories use `NN-lowercase-topic`.
- Numbering represents study order, not a release number.
- Java package names are lowercase semantic identifiers such as `codingfoundations.binarysearch`.
- Markdown chapter files use `NN-descriptive-topic.md`; every module has `index.md`.
- Portal metadata uses the documentation path as `slug` and the Java source path as `codePackage`.
- Assets use descriptive kebab-case names and live near the documentation system that owns them.
- Generated `site/`, root `output/`, `.venv/`, `__pycache__/`, `*.class`, and editor artifacts remain untracked. The PDFs in the published book series' `dist/` directory are intentional versioned release artifacts.

## Synchronization Contract

When adding or renaming a coding-foundation module, update all of these in one contribution:

1. The directory under `docs/coding-foundations/`.
2. Its navigation entry in `mkdocs.yml`.
3. Its record in `web/content/coding-foundations.json`.
4. Its semantic package under `examples/java/.../codingfoundations/`.
5. Relevant source links in `docs/examples/README.md`.
6. Validators if the metadata contract itself changes.

When adding a backend module, add its ordered directory, `index.md`, detailed pages, navigation entries, and any runnable examples needed to support the claims.

The book catalog has a separate synchronization rule: edit `books/java-sde2-interview-preparation-series/publishing/series.json` or rebuild `dist/manifest.json`, then run `make sync-book-catalog`. Never hand-edit `web/content/books.json`.

## Java Source Ownership

Three Java trees are intentional and have different consumers:

1. `examples/java/` supports the searchable handbook and coding-foundation modules.
2. `books/java-sde2-interview-preparation-series/examples/java/` validates complete examples from the master book.
3. `books/java-sde2-interview-preparation-series/content/volumes/<volume>/code/` contains a small, standalone companion for a focused PDF.

Prefer links over copies. Add code to more than one tree only when the examples have genuinely different educational contracts.

## Intentional Root Files

Files such as `README.md`, `LICENSE`, `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`, `CITATION.cff`, `Makefile`, and `mkdocs.yml` remain at repository root. Moving them into a generic configuration folder would make GitHub discovery and contributor onboarding worse.

## Before Proposing a Change

```bash
make validate
make build-site
```

For changes to printable content, also run `make build-pdf` and `make build-docx`. Do not commit generated output.
