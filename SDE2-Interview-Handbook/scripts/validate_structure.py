#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path
from typing import Dict, List

import yaml

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
MKDOCS = ROOT / "mkdocs.yml"
PLACEHOLDER_PATTERNS = [r"TODO", r"TBD", r"Lorem ipsum", r"exercise number", r"question number", r"add content later"]
REQUIRED_PROJECT_FILES = [
    ROOT / "README.md",
    ROOT / "CONTRIBUTING.md",
    ROOT / "CODE_OF_CONDUCT.md",
    ROOT / "SECURITY.md",
    ROOT / "SUPPORT.md",
    ROOT / "GOVERNANCE.md",
    ROOT / "LICENSE",
    ROOT / "LICENSE-CONTENT.md",
    ROOT / "CITATION.cff",
]
REQUIRED_HEADINGS = [
    "Why This Matters",
    "Learning Objectives",
    "Core Concept",
    "Internal Working",
    "Architecture or Memory Diagram",
    "Code Example",
    "Step-by-Step Execution",
    "Interviewer Perspective",
    "Common Mistakes",
    "Production Perspective",
    "Must Know for DSA",
    "Interview Questions and Answers",
    "Practice Exercises",
    "Revision Checklist",
    "One-Page Summary",
]


def parse_nav() -> List[str]:
    data = yaml.safe_load(MKDOCS.read_text(encoding="utf-8"))
    out = []

    def walk(node):
        if isinstance(node, str):
            out.append(node)
        elif isinstance(node, dict):
            for v in node.values():
                walk(v)
        elif isinstance(node, list):
            for item in node:
                walk(item)

    walk(data.get("nav", []))
    return out


def resolve_ref(ref: str) -> Path:
    p = (DOCS / ref)
    return p


def check_navigation():
    missing = []
    for ref in parse_nav():
        if ref == "/":
            continue
        p = resolve_ref(ref)
        if ref and not p.exists():
            missing.append(ref)
    if missing:
        raise SystemExit(f"Missing navigation refs: {missing}")


def check_project_files():
    missing = [str(path.relative_to(ROOT)) for path in REQUIRED_PROJECT_FILES if not path.exists()]
    if missing:
        raise SystemExit(f"Missing open-source project files: {missing}")


def check_source_separation():
    embedded_java = sorted(DOCS.rglob("*.java"))
    if embedded_java:
        paths = [str(path.relative_to(ROOT)) for path in embedded_java]
        raise SystemExit("Java source must live under examples/java, not docs:\n" + "\n".join(paths))

    java_root = ROOT / "examples" / "java" / "src" / "main" / "java"
    if not any(java_root.rglob("*.java")):
        raise SystemExit(f"No canonical Java examples found under {java_root}")


def check_public_metadata():
    metadata = MKDOCS.read_text(encoding="utf-8")
    forbidden = ["example.com", "your-org"]
    found = [value for value in forbidden if value in metadata]
    if found:
        raise SystemExit(f"Placeholder public metadata found in mkdocs.yml: {found}")


def check_placeholders() -> bool:
    bad = []
    for p in DOCS.rglob("*.md"):
        text = p.read_text(encoding="utf-8").lower()
        for pat in PLACEHOLDER_PATTERNS:
            if re.search(pat.lower(), text):
                bad.append(f"{p}: {pat}")
    if bad:
        raise SystemExit("Placeholder text found: \n" + "\n".join(bad))


def check_required_sections(files: List[Path]):
    errors = []
    for p in files:
        text = p.read_text(encoding="utf-8")
        for heading in REQUIRED_HEADINGS:
            if f"## {heading}" not in text and f"## {heading.lower()}" not in text.lower():
                errors.append(f"{p}: missing heading '{heading}'")
    if errors:
        raise SystemExit("Missing required sections:\n" + "\n".join(errors))


def check_mermaid_blocks():
    for p in DOCS.rglob("*.md"):
        lines = p.read_text(encoding="utf-8").splitlines()
        stack = []
        for line in lines:
            stripped = line.strip()
            if not stripped.startswith("```"):
                continue
            if stripped.startswith("```mermaid"):
                stack.append("mermaid")
            elif stack and stripped == "```":
                stack.pop()
        if stack:
            raise SystemExit(f"Unbalanced mermaid fence in {p}")


def check_empty_chapters(files: List[Path]):
    small = [str(p) for p in files if len(p.read_text(encoding="utf-8").strip()) < 600]
    if small:
        raise SystemExit("Potentially empty/sparse chapters: \n" + "\n".join(small))


def check_duplicate_titles(files: List[Path]):
    seen: Dict[str, str] = {}
    dups = []
    for p in files:
        first = None
        for line in p.read_text(encoding="utf-8").splitlines():
            if line.startswith("# "):
                first = line[2:].strip()
                break
        if first:
            if first in seen:
                dups.append(f"{first}: {seen[first]} and {p}")
            else:
                seen[first] = str(p)
    if dups:
        raise SystemExit("Duplicate chapter titles:\n" + "\n".join(dups))


def main() -> None:
    if not MKDOCS.exists():
        raise SystemExit("mkdocs.yml missing")
    check_project_files()
    check_public_metadata()
    check_navigation()
    check_source_separation()
    all_md = [p for p in DOCS.rglob("*.md")]
    if not all_md:
        raise SystemExit("No markdown files found")
    check_placeholders()
    required = [
        p
        for p in all_md
        if p.parent.name.startswith("volume-") and p.name != "index.md"
    ]
    check_required_sections(required)
    check_mermaid_blocks()
    check_empty_chapters([p for p in required])
    check_duplicate_titles(required)


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"Structure validation failed: {exc}")
        sys.exit(1)
