#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
LINK_RE = re.compile(r"\[[^\]]+\]\(([^)]+)\)")


def check_links() -> bool:
    errors = []
    for p in DOCS.rglob("*.md"):
        text = p.read_text(encoding="utf-8")
        for link in LINK_RE.findall(text):
            if link.startswith("http://") or link.startswith("https://"):
                continue
            if link.startswith("#"):
                continue
            target = (p.parent / link).resolve()
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
