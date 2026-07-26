# Tooling

Shared engineering infrastructure is grouped here so the repository root remains focused on product areas and standard entry points.

| Section | Responsibility |
|---|---|
| [`automation/`](automation/) | Repository-wide build, synchronization, and validation commands |
| [`mkdocs-overrides/`](mkdocs-overrides/) | Shared MkDocs Material page chrome |
| [`publishing-templates/`](publishing-templates/) | PDF and DOCX presentation inputs for the root handbook |
| [`requirements/`](requirements/) | Pinned Python environments for authoring and portal deployment |

Prefer stable `make` targets from the repository root. The book package deliberately retains its own `scripts/` and `requirements.txt` because it is an independently buildable publication workspace.
