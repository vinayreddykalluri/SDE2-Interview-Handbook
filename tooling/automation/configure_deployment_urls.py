#!/usr/bin/env python3
"""Normalize canonical URLs in the generated site for hosted deployments.

The production site is https://sde-2-interview-handbook.vercel.app, but the
source keeps `SOURCE_ORIGIN` below as a placeholder and this script rewrites it
to whatever origin the deployment actually has. That indirection is the reason a
domain change is a Vercel setting rather than a repo-wide find-and-replace.

So there are two classes of file, and the rule is worth stating because the
instinct is to "fix" the first class:

* **Pages built into `site/`** - anything under `docs/`, the portal shell, the
  generated book library. These keep the placeholder origin. This script
  rewrites them at build time. Hardcoding the live domain here would silently
  opt those pages out of the rewrite.

* **Files GitHub renders directly and this script never sees** - `README.md`,
  `.github/SUPPORT.md`, and the other community-health files. These point at the
  live domain, because a reader on github.com gets the literal URL.
"""

from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path
from urllib.parse import urlsplit


ROOT = Path(__file__).resolve().parents[2]
SITE = ROOT / "site"
SOURCE_ORIGIN = "https://vinayreddykalluri.github.io/SDE2-Interview-Handbook"
TEXT_SUFFIXES = {".html", ".json", ".js", ".txt", ".webmanifest", ".xml"}


def deployment_origin() -> str | None:
    explicit = os.environ.get("PUBLIC_SITE_URL", "").strip()
    vercel_host = os.environ.get("VERCEL_PROJECT_PRODUCTION_URL", "").strip()
    candidate = explicit or (f"https://{vercel_host}" if vercel_host else "")
    if not candidate:
        return None

    candidate = candidate.rstrip("/")
    parsed = urlsplit(candidate)
    if parsed.scheme != "https" or not parsed.netloc or parsed.path:
        raise ValueError("Deployment origin must be an HTTPS origin without a path")
    return candidate


def normalize(origin: str, check_only: bool) -> tuple[int, int]:
    files_changed = 0
    replacements = 0
    for path in sorted(SITE.rglob("*")):
        if not path.is_file() or path.suffix.lower() not in TEXT_SUFFIXES:
            continue
        text = path.read_text(encoding="utf-8")
        count = text.count(SOURCE_ORIGIN)
        if not count:
            continue
        files_changed += 1
        replacements += count
        if not check_only:
            path.write_text(text.replace(SOURCE_ORIGIN, origin), encoding="utf-8")
    return files_changed, replacements


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="Report replacements without writing")
    args = parser.parse_args()

    if not SITE.is_dir():
        print("ERROR: site/ does not exist; run the site build first", file=sys.stderr)
        return 1

    try:
        origin = deployment_origin()
    except ValueError as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1

    if origin is None:
        print("Deployment URL normalization skipped: no hosted origin is configured")
        return 0

    files_changed, replacements = normalize(origin, args.check)
    action = "would update" if args.check else "updated"
    print(
        f"Deployment URL normalization {action} {files_changed} files "
        f"with {replacements} references to {origin}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
