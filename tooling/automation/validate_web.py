#!/usr/bin/env python3
"""Validate portal metadata, assets, documentation, and Java source alignment."""

from __future__ import annotations

import argparse
import hashlib
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
ARTIFACT_MANIFEST = BOOK_DIST / "manifest.json"
SERIES_SPEC = ROOT / "books" / "java-sde2-interview-preparation-series" / "publishing" / "series.json"
BOOK_WEB_BUILDER = ROOT / "tooling" / "automation" / "build_book_web_library.py"
BOOK_WEB_CSS = ROOT / "tooling" / "book-web" / "book-reader.css"
PORTAL_THEME_MANAGER = WEB / "assets" / "theme-manager.js"
DOCS_THEME_MANAGER = DOCS / "assets" / "javascripts" / "theme-manager.js"
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


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


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

    for page in [WEB / "index.html", WEB / "404.html"]:
        text = page.read_text(encoding="utf-8")
        if 'src="assets/theme-manager.js"' not in text or "data-theme-selector" not in text:
            fail(errors, f"Theme management is missing from {page.relative_to(ROOT)}")

    override = (ROOT / "tooling" / "mkdocs-overrides" / "main.html").read_text(encoding="utf-8")
    if "theme-manager.js" not in override or "data-theme-selector" not in override:
        fail(errors, "MkDocs pages must expose the shared theme manager and selector")

    if PORTAL_THEME_MANAGER.read_bytes() != DOCS_THEME_MANAGER.read_bytes():
        fail(errors, "Portal and MkDocs theme-manager assets must remain identical")


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


def validate_books(errors: list[str], *, verify_artifact_files: bool) -> None:
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
    segments = catalog.get("segments")
    release = catalog.get("release")
    if not isinstance(books, list) or not isinstance(segments, list) or not isinstance(release, dict):
        fail(errors, "apps/portal/content/books.json must contain segments, release metadata, and a books array")
        return
    if catalog.get("schemaVersion") != 5:
        fail(errors, "Book catalog schemaVersion must be 5 for canonical artifact paths")
    # The expected count is derived from publishing/series.json rather than
    # hardcoded. A literal here meant that adding a volume failed validation in
    # a place that had nothing to do with the change, and the fix was to edit
    # the validator -- which is how a checked invariant quietly becomes a
    # number someone updates to make the build pass.
    series_spec = json.loads(SERIES_SPEC.read_text(encoding="utf-8"))
    expected_books = len(series_spec["volumes"])
    if len(books) != expected_books:
        fail(
            errors,
            f"Expected {expected_books} focused books from publishing/series.json; "
            f"found {len(books)} in the portal catalog. Run: make sync-book-catalog",
        )
    try:
        artifact_manifest = json.loads(ARTIFACT_MANIFEST.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        fail(errors, f"Cannot read {ARTIFACT_MANIFEST.relative_to(ROOT)}: {error}")
        return
    artifact_volumes = artifact_manifest.get("volumes")
    if not isinstance(artifact_volumes, list):
        fail(errors, "Book artifact manifest must contain a volumes array")
        return
    artifact_ids = [str(item.get("id", "")) for item in artifact_volumes]
    if not all(artifact_ids) or len(artifact_ids) != len(set(artifact_ids)):
        fail(errors, "Book artifact manifest contains a missing or duplicate volume ID")
        return
    expected_ids = [str(item) for item in series_spec["learning_order"]]
    catalog_ids = [str(item.get("id", "")) for item in books]
    if catalog_ids != expected_ids:
        fail(errors, "Book catalog volumes are not in canonical learning order")
    if artifact_ids != expected_ids:
        fail(errors, "Book artifact manifest volumes are not in canonical learning order")
    if artifact_manifest.get("physical_volumes") != len(expected_ids):
        fail(errors, "Book artifact manifest has an incorrect physical-volume count")
    artifacts_by_id = {
        str(item["id"]): item for item in artifact_volumes
    }
    if set(artifacts_by_id) != set(expected_ids):
        fail(errors, "Book artifact manifest does not contain every canonical volume exactly once")

    series_volumes = {
        str(volume["id"]): volume for volume in series_spec["volumes"]
    }
    expected_assignment: dict[str, tuple[dict[str, object], int]] = {}
    expected_global_position = {
        volume_id: position for position, volume_id in enumerate(expected_ids, start=1)
    }
    for segment in series_spec["segments"]:
        for segment_position, volume_id in enumerate(segment["books"], start=1):
            expected_assignment[str(volume_id)] = (segment, segment_position)
    if release.get("editionDate") != series_spec.get("edition_date", series_spec["release_date"]):
        fail(errors, "Book catalog edition date is not synchronized with the publishing manifest")
    expected_segments = [
        (segment["id"], segment["code"], len(segment["books"]))
        for segment in series_spec["segments"]
    ]
    actual_segments = [(item.get("id"), item.get("code"), item.get("bookCount")) for item in segments]
    if actual_segments != expected_segments:
        fail(errors, f"Book catalog segments are incorrect: {actual_segments}")
    if [book.get("id") for book in books[:6]] != ["03", "GIT", "BUILD", "18B", "18C", "18A"]:
        fail(errors, "Book catalog must begin with the ordered Java Engineering segment")
    focused_pages = sum(int(book.get("pageCount", 0)) for book in books)
    expected_total_pages = focused_pages + int(release.get("indexPageCount", 0)) + int(release.get("masterPageCount", 0))
    # Derived, not literal: every focused volume plus the series index and the
    # master book. A hardcoded 42 here made adding a volume fail in a validator
    # unrelated to the change.
    expected_pdf_count = expected_books + 2
    if release.get("totalPdfCount") != expected_pdf_count:
        fail(
            errors,
            f"Book catalog declares {release.get('totalPdfCount')} PDFs; "
            f"expected {expected_pdf_count} ({expected_books} focused + index + master)",
        )
    if int(release.get("totalPageCount", 0)) != expected_total_pages:
        fail(
            errors,
            f"Book catalog totalPageCount {release.get('totalPageCount')} does not equal "
            f"{expected_total_pages} (focused {focused_pages} + index + master)",
        )

    required_fields = {
        "order", "bookPosition", "step", "pathLabel", "id", "track", "title", "shortTitle", "subtitle",
        "purpose", "authorNote", "filename", "artifactPath", "pageCount", "pdfHref", "releasePdfHref", "sourceHref",
        "fullBookHref", "codeHref", "webDocumentCount", "wordCount", "codeExampleCount",
        "sourceChapterCount", "supportingSourceCount", "chapterPreview", "chapterContents", "outcomes", "webReads",
        "segmentId", "segmentTitle", "segmentShortTitle", "segmentCode", "segmentPosition",
        "segmentBookCount", "publicationStatus",
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
        expected_prefix = f"books/{str(book['pathLabel']).lower()}-"
        if not full_book_href.startswith(expected_prefix) or not full_book_href.endswith("/") or ".." in full_book_href:
            fail(errors, f"Book {book['id']} has an invalid complete web-book route")
        if code_href != f"{full_book_href}code/":
            fail(errors, f"Book {book['id']} code route must be nested under its complete web book")
        if str(book["step"]) != str(book["pathLabel"]) or int(book["bookPosition"]) != int(book["order"]):
            fail(errors, f"Book {book['id']} has inconsistent study numbering")
        expected_segment_code = next((segment["code"] for segment in segments if segment["id"] == book["segmentId"]), None)
        if not expected_segment_code or book["segmentCode"] != f"{expected_segment_code} {int(book['segmentPosition']):02d}":
            fail(errors, f"Book {book['id']} has inconsistent segment numbering")
        if book["publicationStatus"] not in {"published", "enhanced", "planned"}:
            fail(errors, f"Book {book['id']} has an invalid publication status")
        if int(book["webDocumentCount"]) != int(book["sourceChapterCount"]) + int(book["supportingSourceCount"]):
            fail(errors, f"Book {book['id']} web document count does not cover every Markdown source")
        if int(book["wordCount"]) <= 0 or int(book["codeExampleCount"]) < 0:
            fail(errors, f"Book {book['id']} has invalid web content metrics")
        if len(str(book["authorNote"]).strip()) < 40:
            fail(errors, f"Book {book['id']} is missing its topic-specific author note")
        artifact_path = str(book["artifactPath"])
        if Path(artifact_path).is_absolute() or ".." in Path(artifact_path).parts:
            fail(errors, f"Book {book['id']} has an invalid canonical artifact path")
        artifact_file = BOOK_DIST / artifact_path
        if verify_artifact_files and not artifact_file.is_file():
            fail(errors, f"Published book is missing from dist/: {artifact_path}")
        artifact = artifacts_by_id.get(str(book["id"]))
        volume = series_volumes.get(str(book["id"]))
        assignment = expected_assignment.get(str(book["id"]))
        if artifact is None or volume is None or assignment is None:
            fail(errors, f"Book {book['id']} has no complete publishing/artifact contract")
        else:
            segment, expected_segment_position = assignment
            expected_artifact_path = str(
                Path(str(segment["artifact_dir"])) / str(volume["output_name"])
            )
            required_artifact_fields = {
                "file", "page_count", "bytes", "sha256", "book_position",
                "segment_id", "segment_code", "segment_position",
                "path_label", "publication_status",
            }
            missing_artifact_fields = required_artifact_fields - set(artifact)
            if missing_artifact_fields:
                fail(
                    errors,
                    f"Book {book['id']} artifact record is missing fields: "
                    f"{sorted(missing_artifact_fields)}",
                )
            else:
                recorded_path = str(artifact["file"])
                recorded_filename = Path(recorded_path).name
                if artifact_path != expected_artifact_path or recorded_path != expected_artifact_path:
                    fail(
                        errors,
                        f"Book {book['id']} artifact path disagrees across catalog, "
                        "publishing manifest, and artifact manifest",
                    )
                if (
                    filename != str(volume["output_name"])
                    or recorded_filename != str(volume["output_name"])
                    or (
                        artifact.get("output_name") is not None
                        and str(artifact["output_name"]) != str(volume["output_name"])
                    )
                ):
                    fail(errors, f"Book {book['id']} filename disagrees with its artifact record")
                if (
                    str(book["segmentId"]) != str(segment["id"])
                    or str(artifact["segment_id"]) != str(segment["id"])
                ):
                    fail(errors, f"Book {book['id']} segment disagrees with its artifact record")
                expected_catalog_code = f"{segment['code']} {expected_segment_position:02d}"
                if (
                    str(book["segmentCode"]) != expected_catalog_code
                    or str(artifact["segment_code"]) != str(segment["code"])
                ):
                    fail(errors, f"Book {book['id']} segment code disagrees with its artifact record")
                try:
                    positions_match = (
                        int(book["bookPosition"])
                        == expected_global_position[str(book["id"])]
                        == int(artifact["book_position"])
                        and int(book["segmentPosition"]) == expected_segment_position
                        and int(artifact["segment_position"]) == expected_segment_position
                    )
                    pages_match = int(book["pageCount"]) == int(artifact["page_count"])
                except (TypeError, ValueError):
                    positions_match = False
                    pages_match = False
                if not positions_match:
                    fail(errors, f"Book {book['id']} position disagrees with its artifact record")
                if not pages_match:
                    fail(errors, f"Book {book['id']} page count disagrees with its artifact record")
                if (
                    str(book["pathLabel"]) != str(series_spec["path_labels"][str(book["id"])])
                    or str(artifact["path_label"])
                    != str(series_spec["path_labels"][str(book["id"])])
                ):
                    fail(errors, f"Book {book['id']} study code disagrees with its artifact record")
                expected_status = str(volume.get("publication_status", "published"))
                if (
                    str(book["publicationStatus"]) != expected_status
                    or str(artifact["publication_status"]) != expected_status
                ):
                    fail(errors, f"Book {book['id']} publication status disagrees with its artifact record")
                try:
                    recorded_bytes = int(artifact["bytes"])
                except (TypeError, ValueError):
                    recorded_bytes = -1
                if recorded_bytes <= 0:
                    fail(errors, f"Book {book['id']} artifact byte count is invalid")
                recorded_hash = str(artifact["sha256"]).casefold()
                if not re.fullmatch(r"[0-9a-f]{64}", recorded_hash):
                    fail(errors, f"Book {book['id']} artifact hash is not a SHA-256 digest")
                if verify_artifact_files and artifact_file.is_file():
                    if recorded_bytes != artifact_file.stat().st_size:
                        fail(errors, f"Book {book['id']} byte count disagrees with its artifact record")
                    if re.fullmatch(r"[0-9a-f]{64}", recorded_hash) and sha256(artifact_file) != recorded_hash:
                        fail(errors, f"Book {book['id']} PDF hash disagrees with its artifact record")
        if "/raw/refs/heads/master/" not in str(book["pdfHref"]):
            fail(errors, f"Book {book['id']} does not use the current master PDF URL")
        if "/releases/download/" not in str(book["releasePdfHref"]):
            fail(errors, f"Book {book['id']} does not retain a versioned release PDF URL")
        chapter_preview = book["chapterPreview"]
        chapter_contents = book["chapterContents"]
        if not isinstance(chapter_preview, list) or not chapter_preview:
            fail(errors, f"Book {book['id']} has no Markdown-derived chapter preview")
        elif int(book["sourceChapterCount"]) < len(chapter_preview):
            fail(errors, f"Book {book['id']} chapter preview exceeds its source count")
        else:
            for chapter in chapter_preview:
                if not chapter.get("title") or "/blob/master/" not in str(chapter.get("sourceHref", "")):
                    fail(errors, f"Book {book['id']} has an invalid source chapter entry")
        if not isinstance(chapter_contents, list) or len(chapter_contents) != int(book["webDocumentCount"]):
            fail(errors, f"Book {book['id']} does not expose every canonical web document")
        else:
            for chapter in chapter_contents:
                if not chapter.get("title") or not str(chapter.get("webHref", "")).startswith(full_book_href):
                    fail(errors, f"Book {book['id']} has an invalid web chapter contents entry")

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
    if total_documents < 173 or total_code_entries < 800:
        fail(errors, f"Complete web library is unexpectedly shallow: {total_documents} documents, {total_code_entries} code entries")


def validate_javascript(errors: list[str]) -> None:
    node = shutil.which("node")
    if not node:
        print("WARN: node is unavailable; skipped JavaScript syntax validation")
        return
    for script in [WEB / "assets" / "app.js", PORTAL_THEME_MANAGER, DOCS_THEME_MANAGER]:
        result = subprocess.run(
            [node, "--check", str(script)],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        if result.returncode:
            fail(errors, f"JavaScript syntax error in {script.relative_to(ROOT)}:\n{result.stderr.strip()}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--metadata-only",
        action="store_true",
        help=(
            "Validate the source, portal catalog, and artifact-manifest contract "
            "without requiring focused PDF files."
        ),
    )
    args = parser.parse_args()
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
        PORTAL_THEME_MANAGER,
        WEB / "assets" / "s2-mark.svg",
        DOCS_THEME_MANAGER,
        BOOK_WEB_BUILDER,
        BOOK_WEB_CSS,
        MODULES_FILE,
        BOOKS_FILE,
        ARTIFACT_MANIFEST,
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
    validate_books(errors, verify_artifact_files=not args.metadata_only)
    validate_javascript(errors)

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    # Report the counts that were actually checked rather than literals that
    # drift out of date the first time a volume is added.
    focused = len(json.loads(SERIES_SPEC.read_text(encoding="utf-8"))["volumes"])
    total_pdfs = focused + 2  # every focused volume, plus the series index and the master book
    artifact_scope = (
        f"{focused} focused artifact records with reconciled {total_pdfs}-PDF release totals"
        if args.metadata_only
        else f"{focused} focused PDFs with matching records and reconciled {total_pdfs}-PDF release totals"
    )
    print(
        f"Web validation passed: {focused} books in 4 segments, at least 173 canonical documents, "
        "at least 800 book code entries, 19 learning modules, 69 foundation Java files, "
        f"and {artifact_scope}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
