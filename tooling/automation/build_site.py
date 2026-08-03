#!/usr/bin/env python3
"""Build the web portal and MkDocs handbook into one GitHub Pages artifact."""

from __future__ import annotations

import json
import re
import shutil
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote, urlsplit


ROOT = Path(__file__).resolve().parents[2]
WEB_SOURCE = ROOT / "apps" / "portal"
SITE_OUTPUT = ROOT / "site"
DOCS_OUTPUT = SITE_OUTPUT / "docs"
BOOKS_OUTPUT = SITE_OUTPUT / "books"
EXAMPLES_SOURCE = ROOT / "examples"
EXAMPLES_OUTPUT = SITE_OUTPUT / "examples"
MODULES_FILE = WEB_SOURCE / "content" / "coding-foundations.json"
BOOKS_FILE = WEB_SOURCE / "content" / "books.json"
BOOKS_BUILDER = ROOT / "tooling" / "automation" / "build_book_web_library.py"
LOCAL_REFERENCE = re.compile(r'(?:href|src)="([^"]+)"')
PAGES_PREFIX = "/SDE2-Interview-Handbook/"

# Runtime-only routes: served by the host, never present in site/.
#
# broken_book_links() below requires every local reference to resolve to a real
# file under site/, which is the right rule - it is what catches a renamed
# chapter before it ships. The Vercel Web Analytics script is the one reference
# that legitimately cannot satisfy it: Vercel serves it from the edge at this
# fixed path, so it exists at request time and never on disk.
#
# Kept as an exact-match set rather than a prefix or a pattern, so this stays a
# hole for one known path instead of a general escape hatch. validate_web.py
# carries the same allowance for the portal shell, under VERCEL_INSIGHTS_SCRIPT.
RUNTIME_ONLY_REFERENCES = frozenset({"/_vercel/insights/script.js"})


def broken_book_links() -> list[tuple[Path, str]]:
    broken: list[tuple[Path, str]] = []
    for page in BOOKS_OUTPUT.rglob("*.html"):
        if page.name == "404.html":
            continue
        for reference in LOCAL_REFERENCE.findall(page.read_text(encoding="utf-8")):
            parsed = urlsplit(reference)
            if parsed.scheme or parsed.netloc or reference.startswith(("#", "//")):
                continue
            if reference in RUNTIME_ONLY_REFERENCES:
                continue
            raw_path = unquote(parsed.path)
            if not raw_path:
                continue
            if raw_path.startswith(PAGES_PREFIX):
                target = SITE_OUTPUT / raw_path.removeprefix(PAGES_PREFIX)
            elif raw_path.startswith("/"):
                target = SITE_OUTPUT / raw_path.lstrip("/")
            else:
                target = page.parent / raw_path
            target = target.resolve()
            if target != SITE_OUTPUT.resolve() and SITE_OUTPUT.resolve() not in target.parents:
                broken.append((page.relative_to(ROOT), reference))
                continue
            candidates = [target] if target.suffix else [target, target / "index.html"]
            if not any(candidate.is_file() for candidate in candidates):
                broken.append((page.relative_to(ROOT), reference))
    return broken


def main() -> int:
    if SITE_OUTPUT.exists():
        shutil.rmtree(SITE_OUTPUT)

    subprocess.run(
        [
            sys.executable,
            "-m",
            "mkdocs",
            "build",
            "--strict",
            "--clean",
            "--site-dir",
            str(DOCS_OUTPUT),
        ],
        cwd=ROOT,
        check=True,
    )

    subprocess.run(
        [
            sys.executable,
            str(BOOKS_BUILDER),
            "--site-dir",
            str(BOOKS_OUTPUT),
        ],
        cwd=ROOT,
        check=True,
    )

    shutil.copytree(WEB_SOURCE, SITE_OUTPUT, dirs_exist_ok=True)
    shutil.copytree(EXAMPLES_SOURCE, EXAMPLES_OUTPUT, dirs_exist_ok=True)

    modules = json.loads(MODULES_FILE.read_text(encoding="utf-8"))
    catalog = json.loads(BOOKS_FILE.read_text(encoding="utf-8"))
    required_outputs = [
        SITE_OUTPUT / "index.html",
        SITE_OUTPUT / "assets" / "styles.css",
        SITE_OUTPUT / "assets" / "app.js",
        SITE_OUTPUT / "assets" / "theme-manager.js",
        SITE_OUTPUT / "content" / "coding-foundations.json",
        SITE_OUTPUT / "content" / "books.json",
        DOCS_OUTPUT / "index.html",
        DOCS_OUTPUT / "assets" / "javascripts" / "theme-manager.js",
        BOOKS_OUTPUT / "index.html",
        BOOKS_OUTPUT / "assets" / "theme-manager.js",
        BOOKS_OUTPUT / "manifest.json",
        EXAMPLES_OUTPUT
        / "java"
        / "src"
        / "main"
        / "java"
        / "io"
        / "github"
        / "vinayreddykalluri"
        / "interviewhandbook"
        / "problemsolving"
        / "MinimumSizeSubarraySum.java",
    ]
    required_outputs.extend(
        DOCS_OUTPUT / module["slug"] / "index.html" for module in modules
    )
    for book in catalog["books"]:
        required_outputs.extend(
            [
                SITE_OUTPUT / book["fullBookHref"] / "index.html",
                SITE_OUTPUT / book["codeHref"] / "index.html",
            ]
        )

    missing = [path.relative_to(ROOT) for path in required_outputs if not path.is_file()]
    if missing:
        print("Site build is incomplete:", file=sys.stderr)
        for path in missing:
            print(f"  - {path}", file=sys.stderr)
        return 1

    broken_links = broken_book_links()
    if broken_links:
        print("Complete web-book library has broken local links:", file=sys.stderr)
        for page, reference in broken_links[:50]:
            print(f"  - {page}: {reference}", file=sys.stderr)
        if len(broken_links) > 50:
            print(f"  - ... and {len(broken_links) - 50} more", file=sys.stderr)
        return 1

    print("Built unified GitHub Pages artifact:")
    print(f"  Portal:   {SITE_OUTPUT / 'index.html'}")
    print(f"  Handbook: {DOCS_OUTPUT / 'index.html'}")
    print(f"  Books:    {BOOKS_OUTPUT / 'index.html'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
