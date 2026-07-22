#!/usr/bin/env python3
"""Validate curriculum hierarchy and every Markdown path referenced by MkDocs."""

from __future__ import annotations

import re
import sys
from pathlib import Path
from typing import Any

import yaml


ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
FOUNDATIONS = DOCS / "coding-foundations"
BACKEND = DOCS / "backend-interview"
MKDOCS = ROOT / "mkdocs.yml"

EXPECTED_FOUNDATION_DIRS = [
    "01-java-runtime",
    "02-complexity",
    "03-math",
    "04-loop-reasoning",
    "05-indexing",
    "06-bit-manipulation",
    "07-arrays",
    "08-strings",
    "09-hashing",
    "10-sliding-window",
    "11-two-pointers",
    "12-prefix-sum",
    "13-binary-search",
    "14-stacks-recursion",
    "15-queues-deques",
    "16-linked-lists",
    "17-trees",
    "18-graphs",
    "19-dynamic-programming",
]
NUMBERED_DIR = re.compile(r"^(\d{2})-[a-z0-9]+(?:-[a-z0-9]+)*$")
NUMBERED_CHAPTER = re.compile(r"^\d{2}-[a-z0-9]+(?:-[a-z0-9]+)*\.md$")


class MkDocsConfigLoader(yaml.SafeLoader):
    """Safe YAML loader that recognizes MkDocs callable references as opaque names."""


def construct_python_name(
    loader: MkDocsConfigLoader,
    tag_suffix: str,
    node: yaml.Node,
) -> str:
    loader.construct_scalar(node)
    return tag_suffix


MkDocsConfigLoader.add_multi_constructor(
    "tag:yaml.org,2002:python/name:",
    construct_python_name,
)


def nav_paths(node: Any) -> list[str]:
    paths: list[str] = []
    if isinstance(node, str):
        if node.endswith(".md"):
            paths.append(node)
    elif isinstance(node, list):
        for item in node:
            paths.extend(nav_paths(item))
    elif isinstance(node, dict):
        for value in node.values():
            paths.extend(nav_paths(value))
    return paths


def validate_foundations(errors: list[str]) -> tuple[int, int]:
    if not FOUNDATIONS.is_dir():
        errors.append("Missing docs/coding-foundations/")
        return 0, 0

    actual_dirs = sorted(
        path.name for path in FOUNDATIONS.iterdir() if path.is_dir() and not path.name.startswith(".")
    )
    if actual_dirs != EXPECTED_FOUNDATION_DIRS:
        errors.append(
            "Coding-foundation directories must be the ordered canonical set; "
            f"found {actual_dirs}"
        )

    module_count = 0
    chapter_count = 0
    for directory_name in EXPECTED_FOUNDATION_DIRS:
        module_dir = FOUNDATIONS / directory_name
        if not module_dir.is_dir():
            continue
        module_count += 1
        if not (module_dir / "index.md").is_file():
            errors.append(f"Missing module overview: {module_dir.relative_to(ROOT)}/index.md")

        chapters = sorted(
            path for path in module_dir.glob("*.md") if NUMBERED_CHAPTER.fullmatch(path.name)
        )
        if not chapters:
            errors.append(f"No numbered chapters in {module_dir.relative_to(ROOT)}")
        chapter_count += len(chapters)

    if chapter_count != 36:
        errors.append(f"Expected 36 coding-foundation chapters; found {chapter_count}")

    legacy_dirs = sorted(path.name for path in DOCS.glob("volume-*") if path.is_dir())
    if legacy_dirs:
        errors.append(f"Legacy top-level curriculum directories remain: {legacy_dirs}")

    return module_count, chapter_count


def validate_backend(errors: list[str]) -> int:
    if not (BACKEND / "index.md").is_file():
        errors.append("Missing docs/backend-interview/index.md")
        return 0

    module_dirs = sorted(
        path for path in BACKEND.iterdir() if path.is_dir() and NUMBERED_DIR.fullmatch(path.name)
    )
    ids = [int(path.name[:2]) for path in module_dirs]
    if ids != list(range(1, len(ids) + 1)):
        errors.append(f"Backend module numbering must be contiguous from 01; found {ids}")

    required_signals = {
        "programming": any("programming-problem-solving" in path.name for path in module_dirs),
        "LLD": any("low-level-design" in path.name for path in module_dirs),
        "HLD/system design": any(
            "high-level" in path.name or "system-design" in path.name for path in module_dirs
        ),
    }
    for signal, present in required_signals.items():
        if not present:
            errors.append(f"Backend track is missing required {signal} coverage")

    for module_dir in module_dirs:
        if not (module_dir / "index.md").is_file():
            errors.append(f"Missing backend module overview: {module_dir.relative_to(ROOT)}/index.md")

    return len(module_dirs)


def validate_navigation(errors: list[str]) -> int:
    try:
        config = yaml.load(
            MKDOCS.read_text(encoding="utf-8"),
            Loader=MkDocsConfigLoader,
        )
    except (OSError, yaml.YAMLError) as error:
        errors.append(f"Cannot parse mkdocs.yml: {error}")
        return 0

    paths = nav_paths(config.get("nav", []))
    duplicates = sorted({path for path in paths if paths.count(path) > 1})
    if duplicates:
        errors.append(f"Duplicate MkDocs navigation targets: {duplicates}")

    for relative in paths:
        if not (DOCS / relative).is_file():
            errors.append(f"Missing MkDocs navigation target: docs/{relative}")

    return len(paths)


def main() -> int:
    errors: list[str] = []
    module_count, chapter_count = validate_foundations(errors)
    backend_count = validate_backend(errors)
    nav_count = validate_navigation(errors)

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print(
        "Structure validation passed: "
        f"{module_count} coding-foundation modules, "
        f"{chapter_count} chapters, "
        f"{backend_count} backend modules, "
        f"{nav_count} navigation targets"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
