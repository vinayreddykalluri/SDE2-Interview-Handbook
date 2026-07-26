#!/usr/bin/env python3
"""Generate the web book catalog from the canonical publishing manifest."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
BOOK_ROOT = ROOT / "books" / "java-sde2-interview-preparation-series"
SERIES_SPEC = BOOK_ROOT / "publishing" / "series.json"
ARTIFACT_MANIFEST = BOOK_ROOT / "dist" / "manifest.json"
OUTPUT = ROOT / "web" / "content" / "books.json"
REPOSITORY = "https://github.com/vinayreddykalluri/SDE2-Interview-Handbook"
BOOK_REPOSITORY_PATH = "books/java-sde2-interview-preparation-series"


def read_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def track_for(volume_id: str) -> str:
    if volume_id.startswith("18"):
        return "Advanced Java and Backend"
    if volume_id in {"03", "02", "01", "01B", "04", "05"}:
        return "Programming Foundations"
    return "Data Structures and Algorithms"


def source_href(volume: dict[str, Any]) -> str:
    first_source = Path(volume["sources"][0]["path"])
    source_path = first_source.parent if first_source.suffix else first_source
    return f"{REPOSITORY}/tree/master/{BOOK_REPOSITORY_PATH}/{source_path.as_posix()}"


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

    books: list[dict[str, Any]] = []
    for position, volume_id in enumerate(spec["learning_order"], start=1):
        volume = volumes_by_id[str(volume_id)]
        artifact = artifacts_by_id[str(volume_id)]
        books.append(
            {
                "order": position,
                "step": str(volume["stage"]),
                "id": str(volume["id"]),
                "track": track_for(str(volume["id"])),
                "title": volume["title"],
                "shortTitle": volume["short_title"],
                "subtitle": volume["subtitle"],
                "purpose": volume["purpose"],
                "filename": volume["output_name"],
                "pageCount": int(artifact["page_count"]),
                "pdfHref": f"{download_root}/{volume['output_name']}",
                "sourceHref": source_href(volume),
            }
        )

    master = spec["master_artifact"]
    index = artifact_manifest["index"]
    focused_pages = sum(book["pageCount"] for book in books)
    total_pages = focused_pages + int(index["page_count"]) + int(master["page_count"])
    return {
        "schemaVersion": 1,
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
                "pdfHref": f"{download_root}/{index['file']}",
            },
            "master": {
                "title": master["title"],
                "filename": master["file"],
                "pdfHref": f"{download_root}/{master['file']}",
            },
        },
        "books": books,
    }


def serialized_catalog() -> str:
    return json.dumps(build_catalog(), indent=2, ensure_ascii=False) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--check",
        action="store_true",
        help="Fail when web/content/books.json is not synchronized.",
    )
    args = parser.parse_args()
    expected = serialized_catalog()

    if args.check:
        if not OUTPUT.is_file() or OUTPUT.read_text(encoding="utf-8") != expected:
            print(
                "Book catalog is stale. Run: python3 scripts/sync_book_catalog.py",
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
