#!/usr/bin/env python3
"""Validate portal metadata, assets, documentation, and Java source alignment."""

from __future__ import annotations

import json
import re
import shutil
import subprocess
import sys
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit


ROOT = Path(__file__).resolve().parents[1]
WEB = ROOT / "web"
DOCS = ROOT / "docs"
JAVA = (
    ROOT
    / "examples"
    / "java"
    / "src"
    / "main"
    / "java"
    / "io"
    / "github"
    / "vinayreddykalluri"
    / "interviewhandbook"
)
MODULES_FILE = WEB / "content" / "coding-foundations.json"
NUMBERED_CHAPTER = re.compile(r"^\d{2}-.+\.md$")
ROOT_RELATIVE_REFERENCE = re.compile(r"(?:href|src|fetch)\s*\(?(?:=\s*)?[\"']/")
SAFE_CODE_PACKAGE = re.compile(r"^codingfoundations/[a-z][a-z0-9]*$")


class AssetCollector(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.references: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        values = dict(attrs)
        if tag in {"img", "script", "source"} and values.get("src"):
            self.references.append(values["src"] or "")
        if tag == "link" and values.get("href"):
            rel = set((values.get("rel") or "").split())
            if rel & {"stylesheet", "icon", "manifest", "preload"}:
                self.references.append(values["href"] or "")


def fail(errors: list[str], message: str) -> None:
    errors.append(message)


def validate_local_assets(errors: list[str]) -> None:
    parser = AssetCollector()
    parser.feed((WEB / "index.html").read_text(encoding="utf-8"))

    for reference in parser.references:
        parsed = urlsplit(reference)
        if parsed.scheme or parsed.netloc or reference.startswith("//"):
            continue
        relative_path = unquote(parsed.path)
        if not relative_path:
            continue
        target = (WEB / relative_path).resolve()
        if WEB.resolve() not in target.parents and target != WEB.resolve():
            fail(errors, f"Asset escapes web/: {reference}")
        elif not target.is_file():
            fail(errors, f"Missing web asset: {reference}")

    for source in [WEB / "index.html", WEB / "404.html", WEB / "assets" / "app.js"]:
        text = source.read_text(encoding="utf-8")
        if ROOT_RELATIVE_REFERENCE.search(text):
            fail(errors, f"Root-relative URL breaks project Pages: {source.relative_to(ROOT)}")


def validate_modules(errors: list[str]) -> None:
    try:
        modules = json.loads(MODULES_FILE.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        fail(errors, f"Cannot read {MODULES_FILE.relative_to(ROOT)}: {error}")
        return

    if not isinstance(modules, list):
        fail(errors, "web/content/coding-foundations.json must contain a JSON array")
        return

    expected_ids = [f"{number:02d}" for number in range(1, 20)]
    ids = [str(module.get("id", "")) for module in modules]
    slugs = [str(module.get("slug", "")) for module in modules]
    code_packages = [str(module.get("codePackage", "")) for module in modules]
    if ids != expected_ids:
        fail(errors, f"Module IDs must be ordered 01-19; found {ids}")
    if len(slugs) != len(set(slugs)):
        fail(errors, "Module slugs must be unique")
    if len(code_packages) != len(set(code_packages)):
        fail(errors, "Module Java packages must be unique")

    required_fields = {
        "id", "roman", "title", "slug", "codePackage", "stage", "level",
        "duration", "chapters", "codeExamples", "summary", "keywords",
    }
    metadata_chapters = 0
    metadata_examples = 0
    actual_chapters = 0
    actual_examples = 0

    for module in modules:
        missing_fields = required_fields - set(module)
        module_id = str(module.get("id", "??"))
        if missing_fields:
            fail(errors, f"Module {module_id} is missing fields: {sorted(missing_fields)}")
            continue

        slug = str(module["slug"])
        code_package = str(module["codePackage"])
        if not slug.startswith(f"coding-foundations/{module_id}-"):
            fail(errors, f"Module {module_id} has noncanonical slug: {slug}")
        if not SAFE_CODE_PACKAGE.fullmatch(code_package):
            fail(errors, f"Module {module_id} has unsafe Java package path: {code_package}")

        metadata_chapters += int(module["chapters"])
        metadata_examples += int(module["codeExamples"])

        docs_dir = DOCS / slug
        chapter_count = sum(
            1 for path in docs_dir.glob("*.md") if NUMBERED_CHAPTER.fullmatch(path.name)
        )
        actual_chapters += chapter_count
        if chapter_count != int(module["chapters"]):
            fail(
                errors,
                f"Module {module_id} metadata says {module['chapters']} chapters; "
                f"found {chapter_count}",
            )

        java_dir = JAVA / code_package
        example_count = sum(1 for _ in java_dir.glob("*.java"))
        actual_examples += example_count
        if example_count != int(module["codeExamples"]):
            fail(
                errors,
                f"Module {module_id} metadata says {module['codeExamples']} examples; "
                f"found {example_count}",
            )

    if metadata_chapters != 36 or actual_chapters != 36:
        fail(errors, f"Expected 36 chapters; metadata={metadata_chapters}, source={actual_chapters}")
    if metadata_examples != 66 or actual_examples != 66:
        fail(errors, f"Expected 66 Java examples; metadata={metadata_examples}, source={actual_examples}")


def validate_javascript(errors: list[str]) -> None:
    node = shutil.which("node")
    if not node:
        print("WARN: node is unavailable; skipped JavaScript syntax validation")
        return
    result = subprocess.run(
        [node, "--check", str(WEB / "assets" / "app.js")],
        cwd=ROOT,
        capture_output=True,
        text=True,
    )
    if result.returncode:
        fail(errors, f"JavaScript syntax error:\n{result.stderr.strip()}")


def main() -> int:
    errors: list[str] = []
    required_files = [
        WEB / "index.html",
        WEB / "404.html",
        WEB / "robots.txt",
        WEB / "sitemap.xml",
        WEB / "manifest.webmanifest",
        WEB / ".nojekyll",
        WEB / "assets" / "styles.css",
        WEB / "assets" / "app.js",
        MODULES_FILE,
    ]
    for path in required_files:
        if not path.is_file():
            fail(errors, f"Missing required web file: {path.relative_to(ROOT)}")

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    validate_local_assets(errors)
    validate_modules(errors)
    validate_javascript(errors)

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print("Web validation passed: 19 modules, 36 chapters, 66 foundation Java examples")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
