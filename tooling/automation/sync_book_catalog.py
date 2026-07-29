#!/usr/bin/env python3
"""Generate the web book catalog from the canonical publishing manifest."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[2]
BOOK_ROOT = ROOT / "books" / "java-sde2-interview-preparation-series"
SERIES_SPEC = BOOK_ROOT / "publishing" / "series.json"
ARTIFACT_MANIFEST = BOOK_ROOT / "dist" / "manifest.json"
OUTPUT = ROOT / "apps" / "portal" / "content" / "books.json"
REPOSITORY = "https://github.com/vinayreddykalluri/SDE2-Interview-Handbook"
BOOK_REPOSITORY_PATH = "books/java-sde2-interview-preparation-series"
JAVA_FENCE = re.compile(r"^```java(?:\s|$)", re.MULTILINE | re.IGNORECASE)
WORD = re.compile(r"\b[\w'-]+\b")

# A focused PDF may span more than one concise website module. Keep these routes
# explicit so a reader lands on a useful lesson instead of a generic catalog page.
# Chapter names and counts are still derived from each volume's canonical Markdown.
WEB_READING_PATHS: dict[str, list[tuple[str, str]]] = {
    "03": [("Start Java fundamentals", "docs/coding-foundations/01-java-runtime/")],
    "02": [("Read complexity lessons", "docs/coding-foundations/02-complexity/")],
    "01": [("Read math foundations", "docs/coding-foundations/03-math/")],
    "01B": [("Practice math foundations", "docs/coding-foundations/03-math/02-interview-deep-dive/")],
    "04": [("Read bit manipulation", "docs/coding-foundations/06-bit-manipulation/")],
    "05": [
        ("Start loop reasoning", "docs/coding-foundations/04-loop-reasoning/"),
        ("Continue with index safety", "docs/coding-foundations/05-indexing/"),
    ],
    "06": [("Read array lessons", "docs/coding-foundations/07-arrays/")],
    "07": [("Read string lessons", "docs/coding-foundations/08-strings/")],
    "08": [("Read hashing lessons", "docs/coding-foundations/09-hashing/")],
    "09": [("Read recursion lessons", "docs/coding-foundations/14-stacks-recursion/")],
    "10": [("Read linked-list lessons", "docs/coding-foundations/16-linked-lists/")],
    "11": [
        ("Start stack lessons", "docs/coding-foundations/14-stacks-recursion/"),
        ("Continue with queues and deques", "docs/coding-foundations/15-queues-deques/"),
    ],
    "12": [("Read binary-search lessons", "docs/coding-foundations/13-binary-search/")],
    "13": [("Read tree lessons", "docs/coding-foundations/17-trees/")],
    "15": [("Read graph lessons", "docs/coding-foundations/18-graphs/")],
    "17": [("Read dynamic-programming lessons", "docs/coding-foundations/19-dynamic-programming/")],
    "18A": [("Read Java runtime lessons", "docs/coding-foundations/01-java-runtime/")],
    "18B": [("Read Java language lessons", "docs/coding-foundations/01-java-runtime/")],
    "18C": [("Read collections foundations", "docs/coding-foundations/01-java-runtime/15-collections-overview/")],
    "18E": [("Read production engineering", "docs/backend-interview/07-production-engineering/")],
    "18F": [
        ("Read low-level design", "docs/backend-interview/02-low-level-design/"),
        ("Continue with production engineering", "docs/backend-interview/07-production-engineering/"),
    ],
    "18G": [("Open interview practice", "docs/backend-interview/10-practice/")],
    "18H": [("Read API and service design", "docs/backend-interview/04-api-service-design/")],
    "18I": [("Read data and storage", "docs/backend-interview/05-data-storage/")],
    "18J": [("Read distributed systems", "docs/backend-interview/06-distributed-systems/")],
}


def read_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def source_href(volume: dict[str, Any]) -> str:
    first_source = Path(volume["sources"][0]["path"])
    source_path = first_source.parent if first_source.suffix else first_source
    return f"{REPOSITORY}/tree/master/{BOOK_REPOSITORY_PATH}/{source_path.as_posix()}"


def source_file_href(source_path: Path) -> str:
    return f"{REPOSITORY}/blob/master/{BOOK_REPOSITORY_PATH}/{source_path.as_posix()}"


def full_book_href(volume: dict[str, Any]) -> str:
    return f"books/{str(volume['path_label']).lower()}-{volume['slug']}/"


def markdown_title(path: Path) -> str:
    if path.is_file():
        for line in path.read_text(encoding="utf-8").splitlines():
            if line.startswith("# "):
                return line[2:].strip()
    return path.stem.replace("-", " ").title()


def chapter_preview(volume: dict[str, Any]) -> tuple[int, int, list[dict[str, str]]]:
    markdown_sources = [
        Path(source["path"])
        for source in volume.get("sources", [])
        if Path(source["path"]).suffix.lower() == ".md"
    ]
    chapter_sources = [
        source_path
        for source_path in markdown_sources
        if "chapters" in source_path.parts
    ] or markdown_sources
    preview = []
    for source_path in chapter_sources[:3]:
        preview.append(
            {
                "title": markdown_title(BOOK_ROOT / source_path),
                "sourceHref": source_file_href(source_path),
            }
        )
    supporting_source_count = len(markdown_sources) - len(chapter_sources)
    return len(chapter_sources), supporting_source_count, preview


def source_metrics(volume: dict[str, Any]) -> tuple[int, int, int]:
    markdown_sources = [
        Path(source["path"])
        for source in volume.get("sources", [])
        if Path(source["path"]).suffix.lower() == ".md"
    ]
    word_count = 0
    code_entries = 0
    for source_path in markdown_sources:
        text = (BOOK_ROOT / source_path).read_text(encoding="utf-8")
        word_count += len(WORD.findall(text))
        code_entries += len(JAVA_FENCE.findall(text))
    if volume.get("code_companion"):
        code_entries += 1
    return len(markdown_sources), word_count, code_entries


def web_reads(volume_id: str) -> list[dict[str, str]]:
    return [
        {"label": label, "href": href}
        for label, href in WEB_READING_PATHS.get(volume_id, [])
    ]


def build_catalog() -> dict[str, Any]:
    spec = read_json(SERIES_SPEC)
    artifact_manifest = read_json(ARTIFACT_MANIFEST)
    volumes_by_id = {str(volume["id"]): volume for volume in spec["volumes"]}
    artifacts_by_id = {
        str(volume["id"]): volume for volume in artifact_manifest["volumes"]
    }
    release_tag = str(spec["release_tag"])
    release_url = f"{REPOSITORY}/releases/tag/{release_tag}"
    download_root = f"{REPOSITORY}/releases/download/{release_tag}"
    current_download_root = f"{REPOSITORY}/raw/refs/heads/master/{BOOK_REPOSITORY_PATH}/dist"
    segment_by_book: dict[str, dict[str, Any]] = {}
    segments: list[dict[str, Any]] = []
    for segment in spec["segments"]:
        segment_books = [str(item) for item in segment["books"]]
        segment_entry = {
            "id": segment["id"],
            "code": segment["code"],
            "title": segment["title"],
            "shortTitle": segment["short_title"],
            "description": segment["description"],
            "bookCount": len(segment_books),
            "startBookId": segment_books[0],
        }
        first_volume = volumes_by_id[segment_books[0]]
        first_volume = {**first_volume, "path_label": spec["path_labels"][segment_books[0]]}
        segment_entry["startHref"] = full_book_href(first_volume)
        segments.append(segment_entry)
        for segment_position, volume_id in enumerate(segment_books, start=1):
            segment_by_book[volume_id] = {
                **segment_entry,
                "segmentPosition": segment_position,
            }

    books: list[dict[str, Any]] = []
    for position, volume_id in enumerate(spec["learning_order"], start=1):
        volume = dict(volumes_by_id[str(volume_id)])
        volume["path_label"] = str(spec["path_labels"][str(volume_id)])
        artifact = artifacts_by_id[str(volume_id)]
        segment = segment_by_book[str(volume_id)]
        source_chapter_count, supporting_source_count, source_chapter_preview = chapter_preview(volume)
        web_document_count, word_count, code_example_count = source_metrics(volume)
        book_href = full_book_href(volume)
        books.append(
            {
                "order": position,
                "bookPosition": position,
                "step": volume["path_label"],
                "pathLabel": volume["path_label"],
                "id": str(volume["id"]),
                "track": segment["title"],
                "segmentId": segment["id"],
                "segmentTitle": segment["title"],
                "segmentShortTitle": segment["shortTitle"],
                "segmentCode": f"{segment['code']} {segment['segmentPosition']:02d}",
                "segmentPosition": segment["segmentPosition"],
                "segmentBookCount": segment["bookCount"],
                "publicationStatus": volume.get("publication_status", "published"),
                "title": volume["title"],
                "shortTitle": volume["short_title"],
                "subtitle": volume["subtitle"],
                "purpose": volume["purpose"],
                "filename": volume["output_name"],
                "pageCount": int(artifact["page_count"]),
                "pdfHref": f"{current_download_root}/{volume['output_name']}",
                "releasePdfHref": f"{download_root}/{volume['output_name']}",
                "sourceHref": source_href(volume),
                "fullBookHref": book_href,
                "codeHref": f"{book_href}code/",
                "webDocumentCount": web_document_count,
                "wordCount": word_count,
                "codeExampleCount": code_example_count,
                "sourceChapterCount": source_chapter_count,
                "supportingSourceCount": supporting_source_count,
                "chapterPreview": source_chapter_preview,
                "outcomes": list(volume.get("outcomes", []))[:3],
                "webReads": web_reads(str(volume["id"])),
            }
        )

    master = spec["master_artifact"]
    index = artifact_manifest["index"]
    focused_pages = sum(book["pageCount"] for book in books)
    total_pages = focused_pages + int(index["page_count"]) + int(master["page_count"])
    return {
        "schemaVersion": 4,
        "generatedFrom": f"{BOOK_REPOSITORY_PATH}/publishing/series.json",
        "release": {
            "tag": release_tag,
            "date": spec["release_date"],
            "url": release_url,
            "focusedBookCount": len(books),
            "focusedPageCount": focused_pages,
            "indexPageCount": int(index["page_count"]),
            "masterPageCount": int(master["page_count"]),
            "totalPdfCount": len(books) + 2,
            "totalPageCount": total_pages,
            "index": {
                "title": "Java SDE-2 Interview Preparation Series Index",
                "filename": index["file"],
                "pdfHref": f"{current_download_root}/{index['file']}",
                "releasePdfHref": f"{download_root}/{index['file']}",
            },
            "master": {
                "title": master["title"],
                "filename": master["file"],
                "pdfHref": f"{current_download_root}/{master['file']}",
                "releasePdfHref": f"{download_root}/{master['file']}",
            },
        },
        "segments": segments,
        "books": books,
    }


def serialized_catalog() -> str:
    return json.dumps(build_catalog(), indent=2, ensure_ascii=False) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--check",
        action="store_true",
        help="Fail when apps/portal/content/books.json is not synchronized.",
    )
    args = parser.parse_args()
    expected = serialized_catalog()

    if args.check:
        if not OUTPUT.is_file() or OUTPUT.read_text(encoding="utf-8") != expected:
            print(
                "Book catalog is stale. Run: make sync-book-catalog",
                file=sys.stderr,
            )
            return 1
        print("Book catalog is synchronized with publishing/series.json")
        return 0

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(expected, encoding="utf-8")
    print(f"Updated {OUTPUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
