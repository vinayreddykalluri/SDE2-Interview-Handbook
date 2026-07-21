#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path
import sys
from urllib.parse import unquote, urlparse

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
LINK_RE = re.compile(r"\[[^\]]+\]\(([^)]+)\)")
REPOSITORY_BLOB_PREFIX = (
    "https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/blob/master/"
)


def check_links() -> bool:
    errors = []
    for p in DOCS.rglob("*.md"):
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
    return True


if __name__ == "__main__":
    if not check_links():
        sys.exit(1)
