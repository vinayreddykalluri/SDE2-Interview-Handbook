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
# Release automation is now live. This was previously an allowlist of one that
# asserted build and deploy workflows stayed disabled, which made sense while
# publication was hand-driven -- but hand-driven publication is exactly what
# put 163 MB of PDFs into git history. The check is inverted: these three must
# be PRESENT, and anything else appearing here should be a deliberate decision.
REQUIRED_WORKFLOWS = {
    "validate-books.yml",
    "build-books.yml",
    "deploy-pages.yml",
}
REQUIRED_IGNORES = {
    ".github/",
    ".venv/",
    "site/",
    "tooling/publishing-templates/",
    "books/java-sde2-interview-preparation-series/dist/*",
    "!books/java-sde2-interview-preparation-series/dist/manifest.json",
    "books/java-sde2-interview-preparation-series/reports/",
    "books/java-sde2-interview-preparation-series/scripts/",
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
    if "books/" in ignored:
        errors.append(".vercelignore must retain canonical book Markdown and Java inputs for the complete web library")

    workflows_dir = ROOT / ".github" / "workflows"
    active_workflows = (
        {path.name for path in workflows_dir.glob("*.yml")}
        | {path.name for path in workflows_dir.glob("*.yaml")}
        if workflows_dir.exists()
        else set()
    )
    missing_workflows = sorted(REQUIRED_WORKFLOWS - active_workflows)
    if missing_workflows:
        errors.append(
            "Release automation must stay enabled; missing active workflows: "
            f"{missing_workflows}. PDFs and the published site are only "
            "reproducible when CI produces them."
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
