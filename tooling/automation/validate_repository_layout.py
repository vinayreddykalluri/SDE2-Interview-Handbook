#!/usr/bin/env python3
"""Enforce the single-root repository and ordered backend curriculum layout."""

from __future__ import annotations

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
BACKEND_TRACK = ROOT / "docs" / "backend-interview"
BOOK_ROOT = ROOT / "books" / "java-sde2-interview-preparation-series"

REQUIRED_ROOT_FILES = {
    ".gitattributes",
    ".gitignore",
    "LICENSE",
    "LICENSE-CONTENT.md",
    "Makefile",
    "README.md",
    "mkdocs.yml",
}

REQUIRED_ROOT_DIRECTORIES = {
    ".github",
    "apps",
    "books",
    "docs",
    "examples",
    "tooling",
}

REQUIRED_COMMUNITY_FILES = {
    ".github/CODE_OF_CONDUCT.md",
    ".github/CONTRIBUTING.md",
    ".github/SECURITY.md",
    ".github/SUPPORT.md",
    "docs/community/authors.md",
    "docs/community/governance.md",
}

REQUIRED_PROJECT_FILES = {
    "docs/project/deployment.md",
    "docs/project/local-development.md",
    "docs/project/roadmap.md",
}

REQUIRED_TOOLING_PATHS = {
    "tooling/automation/build_site.py",
    "tooling/automation/validate_repository_layout.py",
    "tooling/mkdocs-overrides/main.html",
    "tooling/publishing-templates/reference.docx",
    "tooling/requirements/authoring.txt",
    "tooling/requirements/portal.txt",
}

FORBIDDEN_ROOT_ENTRIES = {
    "AUTHORS.md",
    "CODE_OF_CONDUCT.md",
    "CONTRIBUTING.md",
    "DEPLOYMENT.md",
    "GOVERNANCE.md",
    "LOCAL_DEVELOPMENT.md",
    "ROADMAP.md",
    "SECURITY.md",
    "SUPPORT.md",
    "overrides",
    "requirements-web.txt",
    "requirements.txt",
    "scripts",
    "templates",
    "web",
}

BACKEND_MODULES = [
    "01-programming-problem-solving",
    "02-low-level-design",
    "03-high-level-system-design",
    "04-api-service-design",
    "05-data-storage",
    "06-distributed-systems",
    "07-production-engineering",
    "08-cloud-platform",
    "09-leadership-behavioral",
    "10-practice",
]

BOOK_DIRECTORIES = {
    "assets",
    "content",
    "dist",
    "docs",
    "examples",
    "publishing",
    "reports",
    "scripts",
}

FORBIDDEN_BOOK_DIRECTORIES = {
    "book",
    "code-examples",
    "diagrams",
    "series",
}


def main() -> int:
    errors: list[str] = []

    forbidden_nested_root = ROOT / "SDE2-Interview-Handbook"
    if forbidden_nested_root.exists():
        errors.append(
            "Nested SDE2-Interview-Handbook/ directory found; project files must live at the Git root"
        )

    for name in sorted(REQUIRED_ROOT_FILES):
        if not (ROOT / name).is_file():
            errors.append(f"Missing required root file: {name}")

    for name in sorted(REQUIRED_ROOT_DIRECTORIES):
        if not (ROOT / name).is_dir():
            errors.append(f"Missing required root directory: {name}/")

    for relative in sorted(
        REQUIRED_COMMUNITY_FILES | REQUIRED_PROJECT_FILES | REQUIRED_TOOLING_PATHS
    ):
        if not (ROOT / relative).is_file():
            errors.append(f"Missing canonical repository file: {relative}")

    for name in sorted(FORBIDDEN_ROOT_ENTRIES):
        if (ROOT / name).exists():
            errors.append(f"Legacy root entry must not return: {name}")

    for page in [
        "index.md",
        "roadmap.md",
        "readiness-matrix.md",
        "revision-system.md",
        "review-log.md",
    ]:
        if not (BACKEND_TRACK / page).is_file():
            errors.append(f"Missing backend track page: docs/backend-interview/{page}")

    for module in BACKEND_MODULES:
        module_path = BACKEND_TRACK / module
        if not module_path.is_dir():
            errors.append(f"Missing ordered backend module: {module}")
            continue
        if not (module_path / "index.md").is_file():
            errors.append(f"Missing module overview: {module}/index.md")
        if not (module_path / "advanced-review.md").is_file():
            errors.append(f"Missing advanced review material: {module}/advanced-review.md")
        if len(list(module_path.glob("*.md"))) < 3:
            errors.append(f"Backend module needs overview plus detail pages: {module}")

    backend_page_count = len(list(BACKEND_TRACK.rglob("*.md")))
    if backend_page_count < 42:
        errors.append(
            f"Backend track must retain at least 42 curriculum pages; found {backend_page_count}"
        )

    for name in sorted(BOOK_DIRECTORIES):
        if not (BOOK_ROOT / name).is_dir():
            errors.append(f"Missing canonical book directory: {name}/")

    for name in sorted(FORBIDDEN_BOOK_DIRECTORIES):
        if (BOOK_ROOT / name).exists():
            errors.append(f"Legacy book directory must not return: {name}/")

    for path in [
        BOOK_ROOT / "publishing" / "series.json",
        BOOK_ROOT / "content" / "README.md",
        BOOK_ROOT / "dist" / "manifest.json",
        ROOT / "apps" / "portal" / "content" / "books.json",
    ]:
        if not path.is_file():
            errors.append(f"Missing synchronized publication file: {path.relative_to(ROOT)}")

    allowed_book_markdown = {"CHANGELOG.md", "README.md"}
    unexpected_book_markdown = sorted(
        path.name for path in BOOK_ROOT.glob("*.md") if path.name not in allowed_book_markdown
    )
    if unexpected_book_markdown:
        errors.append(
            "Book workspace root contains misplaced Markdown: "
            + ", ".join(unexpected_book_markdown)
        )

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print(
        "Repository layout passed: six intentional root sections, "
        f"{len(BACKEND_MODULES)} backend modules, {backend_page_count} curriculum pages, "
        f"{len(BOOK_DIRECTORIES)} canonical book sections"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
