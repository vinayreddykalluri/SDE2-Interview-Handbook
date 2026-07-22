#!/usr/bin/env python3
"""Validate the static Vercel build contract without contacting Vercel."""

from __future__ import annotations

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / "vercel.json"
WEB_REQUIREMENTS = ROOT / "requirements-web.txt"

EXPECTED = {
    "$schema": "https://openapi.vercel.sh/vercel.json",
    "installCommand": "python3 -m pip install --disable-pip-version-check -r requirements-web.txt",
    "buildCommand": "python3 scripts/build_site.py && python3 scripts/configure_deployment_urls.py",
    "outputDirectory": "site",
}
REQUIRED_PACKAGES = {
    "mkdocs",
    "mkdocs-material",
    "mkdocs-mermaid2-plugin",
    "mkdocs-git-revision-date-localized-plugin",
    "pymdown-extensions",
    "pygments",
    "pyyaml",
}


def main() -> int:
    errors: list[str] = []
    try:
        config = json.loads(CONFIG.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        print(f"ERROR: Cannot read vercel.json: {error}", file=sys.stderr)
        return 1

    for key, expected in EXPECTED.items():
        if config.get(key) != expected:
            errors.append(f"vercel.json {key} must be {expected!r}")

    try:
        lines = WEB_REQUIREMENTS.read_text(encoding="utf-8").splitlines()
    except OSError as error:
        errors.append(f"Cannot read requirements-web.txt: {error}")
        lines = []

    packages = {
        line.split("==", 1)[0].strip().lower()
        for line in lines
        if line.strip() and not line.lstrip().startswith("#")
    }
    missing = sorted(REQUIRED_PACKAGES - packages)
    if missing:
        errors.append(f"requirements-web.txt is missing: {missing}")
    if any("==" not in line for line in lines if line.strip() and not line.startswith("#")):
        errors.append("Every hosted dependency must use an exact == pin")

    if (ROOT / ".github" / "workflows").exists():
        errors.append("GitHub Actions must remain disabled under .github/workflows-disabled/")

    for relative in [
        "scripts/build_site.py",
        "scripts/configure_deployment_urls.py",
        "web/index.html",
        "mkdocs.yml",
        ".vercelignore",
    ]:
        if not (ROOT / relative).is_file():
            errors.append(f"Missing deployment input: {relative}")

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print("Deployment validation passed: pinned static Vercel build publishes site/")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
