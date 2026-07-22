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
| Contribution policy | Root community files or `.github/` | Relevant community page |

## Naming Rules

- Documentation directories use `NN-lowercase-topic`.
- Numbering represents study order, not a release number.
- Java package names are lowercase semantic identifiers such as `codingfoundations.binarysearch`.
- Markdown chapter files use `NN-descriptive-topic.md`; every module has `index.md`.
- Portal metadata uses the documentation path as `slug` and the Java source path as `codePackage`.
- Assets use descriptive kebab-case names and live near the documentation system that owns them.
- Generated `site/`, `output/`, `.venv/`, `__pycache__/`, `*.class`, and editor artifacts remain untracked.

## Synchronization Contract

When adding or renaming a coding-foundation module, update all of these in one contribution:

1. The directory under `docs/coding-foundations/`.
2. Its navigation entry in `mkdocs.yml`.
3. Its record in `web/content/coding-foundations.json`.
4. Its semantic package under `examples/java/.../codingfoundations/`.
5. Relevant source links in `docs/examples/README.md`.
6. Validators if the metadata contract itself changes.

When adding a backend module, add its ordered directory, `index.md`, detailed pages, navigation entries, and any runnable examples needed to support the claims.

## Intentional Root Files

Files such as `README.md`, `LICENSE`, `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`, `CITATION.cff`, `Makefile`, and `mkdocs.yml` remain at repository root. Moving them into a generic configuration folder would make GitHub discovery and contributor onboarding worse.

## Before Proposing a Change

```bash
make validate
make build-site
```

For changes to printable content, also run `make build-pdf` and `make build-docx`. Do not commit generated output.
