#!/usr/bin/env python3
"""Enforce the single-root repository and ordered backend curriculum layout."""

from __future__ import annotations

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
BACKEND_TRACK = ROOT / "docs" / "backend-interview"

REQUIRED_ROOT_FILES = {
    ".gitignore",
    "CONTRIBUTING.md",
    "LICENSE",
    "LICENSE-CONTENT.md",
    "Makefile",
    "README.md",
    "mkdocs.yml",
    "requirements.txt",
}

REQUIRED_ROOT_DIRECTORIES = {
    ".github",
    "docs",
    "examples",
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

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print(
        "Repository layout passed: single Git root, "
        f"{len(BACKEND_MODULES)} backend modules, {backend_page_count} curriculum pages"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
