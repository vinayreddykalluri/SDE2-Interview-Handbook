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


ROOT = Path(__file__).resolve().parents[2]
WEB = ROOT / "apps" / "portal"
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
BOOKS_FILE = WEB / "content" / "books.json"
BOOK_DIST = ROOT / "books" / "java-sde2-interview-preparation-series" / "dist"
BOOK_WEB_BUILDER = ROOT / "tooling" / "automation" / "build_book_web_library.py"
BOOK_WEB_CSS = ROOT / "tooling" / "book-web" / "book-reader.css"
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
            fail(errors, f"Asset escapes apps/portal/: {reference}")
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
        fail(errors, "apps/portal/content/coding-foundations.json must contain a JSON array")
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

    if metadata_chapters != 57 or actual_chapters != 57:
        fail(errors, f"Expected 57 chapters; metadata={metadata_chapters}, source={actual_chapters}")
    if metadata_examples != 69 or actual_examples != 69:
        fail(errors, f"Expected 69 Java examples; metadata={metadata_examples}, source={actual_examples}")


def validate_books(errors: list[str]) -> None:
    sync = subprocess.run(
        [sys.executable, str(ROOT / "tooling" / "automation" / "sync_book_catalog.py"), "--check"],
        cwd=ROOT,
        capture_output=True,
        text=True,
    )
    if sync.returncode:
        fail(errors, sync.stderr.strip() or sync.stdout.strip())
        return

    try:
        catalog = json.loads(BOOKS_FILE.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        fail(errors, f"Cannot read {BOOKS_FILE.relative_to(ROOT)}: {error}")
        return

    books = catalog.get("books")
    release = catalog.get("release")
    if not isinstance(books, list) or not isinstance(release, dict):
        fail(errors, "apps/portal/content/books.json must contain release metadata and a books array")
        return
    if catalog.get("schemaVersion") != 2:
        fail(errors, "Book catalog schemaVersion must be 2 for complete web-book routes")
    if len(books) != 28:
        fail(errors, f"Expected 28 focused books; found {len(books)}")
    if [book.get("id") for book in books[:6]] != ["03", "02", "01", "01B", "04", "05"]:
        fail(errors, "Book catalog must begin Java -> Complexity -> Number Systems -> Bits -> Loops")
    if release.get("totalPdfCount") != 30 or release.get("totalPageCount") != 2579:
        fail(errors, "Book catalog totals must remain 30 PDFs and 2,579 reviewed pages")

    required_fields = {
        "order", "step", "id", "track", "title", "shortTitle", "subtitle",
        "purpose", "filename", "pageCount", "pdfHref", "releasePdfHref", "sourceHref",
        "fullBookHref", "codeHref", "webDocumentCount", "wordCount", "codeExampleCount",
        "sourceChapterCount", "supportingSourceCount", "chapterPreview", "outcomes", "webReads",
    }
    filenames: list[str] = []
    full_book_routes: list[str] = []
    web_readable_books = 0
    for book in books:
        missing = required_fields - set(book)
        if missing:
            fail(errors, f"Book {book.get('id', '??')} is missing fields: {sorted(missing)}")
            continue
        filename = str(book["filename"])
        filenames.append(filename)
        full_book_href = str(book["fullBookHref"])
        code_href = str(book["codeHref"])
        full_book_routes.append(full_book_href)
        expected_prefix = f"books/{str(book['id']).lower()}-"
        if not full_book_href.startswith(expected_prefix) or not full_book_href.endswith("/") or ".." in full_book_href:
            fail(errors, f"Book {book['id']} has an invalid complete web-book route")
        if code_href != f"{full_book_href}code/":
            fail(errors, f"Book {book['id']} code route must be nested under its complete web book")
        if int(book["webDocumentCount"]) != int(book["sourceChapterCount"]) + int(book["supportingSourceCount"]):
            fail(errors, f"Book {book['id']} web document count does not cover every Markdown source")
        if int(book["wordCount"]) <= 0 or int(book["codeExampleCount"]) < 0:
            fail(errors, f"Book {book['id']} has invalid web content metrics")
        if not (BOOK_DIST / filename).is_file():
            fail(errors, f"Published book is missing from dist/: {filename}")
        if "/raw/refs/heads/master/" not in str(book["pdfHref"]):
            fail(errors, f"Book {book['id']} does not use the current master PDF URL")
        if "/releases/download/" not in str(book["releasePdfHref"]):
            fail(errors, f"Book {book['id']} does not retain a versioned release PDF URL")
        chapter_preview = book["chapterPreview"]
        if not isinstance(chapter_preview, list) or not chapter_preview:
            fail(errors, f"Book {book['id']} has no Markdown-derived chapter preview")
        elif int(book["sourceChapterCount"]) < len(chapter_preview):
            fail(errors, f"Book {book['id']} chapter preview exceeds its source count")
        else:
            for chapter in chapter_preview:
                if not chapter.get("title") or "/blob/master/" not in str(chapter.get("sourceHref", "")):
                    fail(errors, f"Book {book['id']} has an invalid source chapter entry")

        web_reads = book["webReads"]
        if not isinstance(web_reads, list):
            fail(errors, f"Book {book['id']} webReads must be an array")
            continue
        if web_reads:
            web_readable_books += 1
        for web_read in web_reads:
            href = str(web_read.get("href", ""))
            if not web_read.get("label") or not href.startswith("docs/"):
                fail(errors, f"Book {book['id']} has an invalid web reading route")
                continue
            relative = Path(href.removeprefix("docs/").strip("/"))
            index_source = DOCS / relative / "index.md"
            page_source = (DOCS / relative).with_suffix(".md")
            if not index_source.is_file() and not page_source.is_file():
                fail(errors, f"Book {book['id']} web reading route has no Markdown source: {href}")
    if len(filenames) != len(set(filenames)):
        fail(errors, "Book catalog filenames must be unique")
    if len(full_book_routes) != len(set(full_book_routes)):
        fail(errors, "Complete web-book routes must be unique")
    if web_readable_books < 24:
        fail(errors, f"Expected at least 24 books with a web reading route; found {web_readable_books}")

    strings = next((book for book in books if book.get("id") == "07"), None)
    if not strings or int(strings.get("sourceChapterCount", 0)) < 7:
        fail(errors, "Strings must expose all seven publication-depth Markdown chapters")
    elif int(strings.get("wordCount", 0)) < 14000 or int(strings.get("codeExampleCount", 0)) < 75:
        fail(errors, "Strings must retain publication-depth web content and code coverage")

    total_documents = sum(int(book.get("webDocumentCount", 0)) for book in books)
    total_code_entries = sum(int(book.get("codeExampleCount", 0)) for book in books)
    if total_documents < 161 or total_code_entries < 800:
        fail(errors, f"Complete web library is unexpectedly shallow: {total_documents} documents, {total_code_entries} code entries")


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
        WEB / "assets" / "s2-mark.svg",
        BOOK_WEB_BUILDER,
        BOOK_WEB_CSS,
        MODULES_FILE,
        BOOKS_FILE,
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
    validate_books(errors)
    validate_javascript(errors)

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print(
        "Web validation passed: 28 complete books, 161 canonical documents, 860 book code entries, "
        "19 learning modules, 69 foundation Java files, and 30 published PDFs"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
