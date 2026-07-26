# Repository Structure

This guide answers two contributor questions: where a change belongs and which other layers must stay synchronized.

## Canonical Hierarchy

```text
.
|-- .github/                   Community health, ownership, issue forms, and CI
|-- apps/
|   +-- portal/               Static learning portal, assets, and generated catalog
|-- books/
|   +-- java-sde2-interview-preparation-series/
|       |-- content/           Master and focused canonical Markdown
|       |-- assets/            Covers and educational diagrams
|       |-- examples/java/     Master-book Maven examples
|       |-- publishing/        Series metadata and shared artwork
|       |-- reports/           Audit, coverage, validation, and build evidence
|       |-- docs/              Series editorial standard and roadmap
|       |-- scripts/           Book-specific publishing and QA
|       +-- dist/              Reviewed PDFs and artifact manifest
|-- docs/
|   |-- backend-interview/     Backend interview curriculum
|   |-- coding-foundations/    Java and DSA learning sequence
|   |-- community/             Architecture, authorship, and governance
|   |-- project/               Roadmap, deployment, and local setup
|   |-- examples/              Documentation-side code index
|   +-- assets/                Documentation diagrams and styles
|-- examples/
|   +-- java/src/              Runnable handbook Java and smoke checks
+-- tooling/
    |-- automation/            Repository-wide build and validation commands
    |-- mkdocs-overrides/      MkDocs Material presentation overrides
    |-- publishing-templates/  Root PDF and DOCX rendering inputs
    +-- requirements/          Authoring and portal dependency manifests
```

## Change-Routing Matrix

| Change | Primary location | Usually update too |
|---|---|---|
| Backend interview theory | `docs/backend-interview/NN-topic/` | `mkdocs.yml` and a Java example when implementation is central |
| Algorithm or Java foundation | `docs/coding-foundations/NN-topic/` | `apps/portal/content/coding-foundations.json` and semantic Java package |
| Runnable implementation | `examples/java/src/main/java/...` | Linked documentation and smoke tests |
| Navigation | `mkdocs.yml` | Track overview and internal links |
| Portal behavior or appearance | `apps/portal/` | `tooling/automation/validate_web.py` when a contract changes |
| MkDocs page chrome | `tooling/mkdocs-overrides/` | Light/dark and mobile behavior |
| Build behavior | `tooling/automation/` | `Makefile` and `docs/project/local-development.md` |
| Hosted static delivery | `vercel.json` and `docs/project/deployment.md` | Portal requirements and deployment validation |
| Printable styling | `tooling/publishing-templates/` | Local PDF and DOCX inspection |
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
3. Its record in `apps/portal/content/coding-foundations.json`.
4. Its semantic package under `examples/java/.../codingfoundations/`.
5. Relevant source links in `docs/examples/README.md`.
6. Validators if the metadata contract itself changes.

When adding a backend module, add its ordered directory, `index.md`, detailed pages, navigation entries, and any runnable examples needed to support the claims.

The book catalog has a separate synchronization rule: edit `books/java-sde2-interview-preparation-series/publishing/series.json` or rebuild `dist/manifest.json`, then run `make sync-book-catalog`. Never hand-edit `apps/portal/content/books.json`.

## Java Source Ownership

Three Java trees are intentional and have different consumers:

1. `examples/java/` supports the searchable handbook and coding-foundation modules.
2. `books/java-sde2-interview-preparation-series/examples/java/` validates complete examples from the master book.
3. `books/java-sde2-interview-preparation-series/content/volumes/<volume>/code/` contains a small, standalone companion for a focused PDF.

Prefer links over copies. Add code to more than one tree only when the examples have genuinely different educational contracts.

## Intentional Root Files

The root is limited to repository entry points (`README.md`, licenses, and `CITATION.cff`) plus tool-discovered configuration (`Makefile`, `mkdocs.yml`, and `vercel.json`). GitHub recognizes contribution, conduct, security, and support policies under `.github/`; longer project and stewardship material belongs under `docs/`. The layout validator prevents legacy root clutter from returning.

## Before Proposing a Change

```bash
make validate
make build-site
```

For changes to printable content, also run `make build-pdf` and `make build-docx`. Do not commit generated output.
