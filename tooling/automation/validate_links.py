#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path
import sys
from urllib.parse import unquote, urlparse

ROOT = Path(__file__).resolve().parents[2]
DOCS = ROOT / "docs"
BOOK_README = ROOT / "books" / "java-sde2-interview-preparation-series" / "README.md"
LINK_RE = re.compile(r"\[[^\]]+\]\(([^)]+)\)")
REPOSITORY_BLOB_PREFIX = (
    "https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/"
)


def navigation_markdown() -> list[Path]:
    paths = list(DOCS.rglob("*.md"))
    paths.extend(
        [
            ROOT / "README.md",
            ROOT / ".github" / "CODE_OF_CONDUCT.md",
            ROOT / ".github" / "CONTRIBUTING.md",
            ROOT / ".github" / "SECURITY.md",
            ROOT / ".github" / "SUPPORT.md",
            ROOT / "apps" / "README.md",
            ROOT / "apps" / "portal" / "README.md",
            ROOT / "tooling" / "README.md",
            ROOT / "tooling" / "automation" / "README.md",
            ROOT / "tooling" / "mkdocs-overrides" / "README.md",
            ROOT / "tooling" / "publishing-templates" / "README.md",
            ROOT / "tooling" / "requirements" / "README.md",
            BOOK_README,
        ]
    )
    return sorted(set(paths))


def check_links() -> bool:
    errors = []
    paths = navigation_markdown()
    for p in paths:
        if not p.is_file():
            errors.append(f"missing navigation document: {p.relative_to(ROOT)}")
            continue
        text = p.read_text(encoding="utf-8")
        for link in LINK_RE.findall(text):
            if link.startswith(REPOSITORY_BLOB_PREFIX):
                repository_path = unquote(
                    link.removeprefix(REPOSITORY_BLOB_PREFIX).split("#", 1)[0].split("?", 1)[0]
                )
                target = ROOT / repository_path
                if not target.exists():
                    errors.append(f"{p}: missing repository source {repository_path}")
                continue
            parsed = urlparse(link)
            if parsed.scheme in {"http", "https", "mailto", "tel"}:
                continue
            if link.startswith("#"):
                continue
            clean_link = unquote(link.split("#", 1)[0].split("?", 1)[0])
            if not clean_link:
                continue
            target = (p.parent / clean_link).resolve()
            if not target.exists():
                errors.append(f"{p}: missing {link}")
    if errors:
        print("Broken links:")
        for e in errors:
            print(e)
        return False
    print(f"Link validation passed: {len(paths)} navigation and contributor documents")
    return True


if __name__ == "__main__":
    if not check_links():
        sys.exit(1)
