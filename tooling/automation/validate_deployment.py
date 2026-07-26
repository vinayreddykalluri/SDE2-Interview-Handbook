#!/usr/bin/env python3
"""Validate the static Vercel build contract without contacting Vercel."""

from __future__ import annotations

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CONFIG = ROOT / "vercel.json"
IGNORE = ROOT / ".vercelignore"
WEB_REQUIREMENTS = ROOT / "tooling" / "requirements" / "portal.txt"

EXPECTED = {
    "$schema": "https://openapi.vercel.sh/vercel.json",
    "installCommand": "python3 -m pip install --disable-pip-version-check -r tooling/requirements/portal.txt",
    "buildCommand": "python3 tooling/automation/build_site.py && python3 tooling/automation/configure_deployment_urls.py",
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
ACTIVE_WORKFLOW_ALLOWLIST = {"validate-books.yml"}
REQUIRED_IGNORES = {".github/", ".venv/", "books/", "site/", "tooling/publishing-templates/"}


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
        errors.append(f"Cannot read tooling/requirements/portal.txt: {error}")
        lines = []

    packages = {
        line.split("==", 1)[0].strip().lower()
        for line in lines
        if line.strip() and not line.lstrip().startswith("#")
    }
    missing = sorted(REQUIRED_PACKAGES - packages)
    if missing:
        errors.append(f"tooling/requirements/portal.txt is missing: {missing}")
    if any("==" not in line for line in lines if line.strip() and not line.startswith("#")):
        errors.append("Every hosted dependency must use an exact == pin")

    try:
        ignored = {
            line.strip()
            for line in IGNORE.read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.lstrip().startswith("#")
        }
    except OSError as error:
        errors.append(f"Cannot read .vercelignore: {error}")
        ignored = set()
    missing_ignores = sorted(REQUIRED_IGNORES - ignored)
    if missing_ignores:
        errors.append(f".vercelignore is missing non-deployment inputs: {missing_ignores}")
    if "examples/" in ignored:
        errors.append(".vercelignore must retain examples/ because build_site.py publishes it")

    workflows_dir = ROOT / ".github" / "workflows"
    active_workflows = (
        {path.name for path in workflows_dir.glob("*.yml")}
        | {path.name for path in workflows_dir.glob("*.yaml")}
        if workflows_dir.exists()
        else set()
    )
    unexpected_workflows = sorted(active_workflows - ACTIVE_WORKFLOW_ALLOWLIST)
    if unexpected_workflows:
        errors.append(
            "Website and deployment GitHub Actions must remain disabled; "
            f"unexpected active workflows: {unexpected_workflows}"
        )

    for relative in [
        "tooling/automation/build_site.py",
        "tooling/automation/configure_deployment_urls.py",
        "apps/portal/index.html",
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
