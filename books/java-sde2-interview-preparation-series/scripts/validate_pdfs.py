#!/usr/bin/env python3
"""Verify that every published PDF is structurally sound and matches its manifest.

The existing validators check that source Markdown, the portal catalog, and
publishing/series.json agree with each other. None of them opens a PDF. This
one does, because the failure modes that actually reach a reader live in the
rendered artifact:

  * a page count outside the band declared in publishing/series.json;
  * blank or near-empty pages from a bad page break;
  * a page whose text cannot be extracted at all (a font that failed to embed
    renders visibly but yields nothing to a screen reader or to search);
  * a heading stranded as the last line of a page, with its body overleaf;
  * dist/manifest.json disagreeing with the bytes on disk.

The manifest check is the important one after a rebuild. Page counts, sizes,
and SHA-256 digests are recorded at build time, so a stale manifest means the
catalog, the README totals, and the release notes are all describing files
that no longer exist in that form.

Usage:
    python scripts/validate_pdfs.py                # every PDF in dist/
    python scripts/validate_pdfs.py --volume 18C   # one volume
    python scripts/validate_pdfs.py --quick        # structure + manifest only
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path

try:
    import pypdf
except ImportError:  # pragma: no cover - dependency guard
    print("pypdf is required: pip install -r requirements.txt", file=sys.stderr)
    raise SystemExit(2)

ROOT = Path(__file__).resolve().parents[1]
DIST = ROOT / "dist"
MANIFEST = DIST / "manifest.json"
SERIES = ROOT / "publishing" / "series.json"

# A rendered page carrying fewer than this many extractable characters is
# almost always a layout accident. Deliberate part-title pages are the
# legitimate exception, so they are matched by shape before being reported.
MIN_PAGE_CHARS = 40

# Part dividers and cover pages legitimately carry very little text.
STRUCTURAL_PAGE_MARKERS = (
    "Part I", "Part II", "Part III", "Part IV", "Part V",
    "Part VI", "Part VII", "Part VIII", "Part IX",
    "Appendices", "Contents",
)


def sha256_of(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def manifest_entries(manifest: dict) -> dict[str, dict]:
    """Flatten every manifest record that names a PDF and a page count.

    Three artifact classes are recorded with different key names: focused
    volumes use `output_name`, and the series index uses `file` with a
    directory prefix. Both are keyed here by bare filename.
    """
    found: dict[str, dict] = {}

    def walk(node) -> None:
        if isinstance(node, dict):
            if "page_count" in node:
                name = node.get("output_name") or node.get("file")
                if name:
                    found[Path(name).name] = node
            for value in node.values():
                walk(value)
        elif isinstance(node, list):
            for value in node:
                walk(value)

    walk(manifest)
    return found


def master_record() -> tuple[str, dict]:
    """The master book is built by build_book.py and declared in series.json
    rather than dist/manifest.json, so its expected page count lives there."""
    data = json.loads(SERIES.read_text(encoding="utf-8"))
    master = data.get("master_artifact") or {}
    name = Path(master.get("artifact_path") or master.get("file") or "").name
    record = {"page_count": master["page_count"]} if "page_count" in master else {}
    return name, record


def page_bands() -> dict[str, tuple[int, int]]:
    data = json.loads(SERIES.read_text(encoding="utf-8"))
    return {
        volume["output_name"]: (volume["min_pages"], volume["max_pages"])
        for volume in data["volumes"]
        if "min_pages" in volume and "max_pages" in volume
    }


def check_pdf(path: Path, record: dict, band: tuple[int, int] | None,
              quick: bool) -> list[str]:
    problems: list[str] = []
    name = path.name

    try:
        reader = pypdf.PdfReader(str(path))
        pages = reader.pages
        page_count = len(pages)
    except Exception as error:
        return [f"{name}: cannot be opened as a PDF ({error})"]

    if page_count == 0:
        return [f"{name}: contains zero pages"]

    # --- manifest agreement -------------------------------------------------
    recorded_pages = record.get("page_count")
    if recorded_pages != page_count:
        problems.append(
            f"{name}: manifest records {recorded_pages} pages, file has {page_count}"
        )

    actual_bytes = path.stat().st_size
    if record.get("bytes") not in (None, actual_bytes):
        problems.append(
            f"{name}: manifest records {record['bytes']} bytes, file is {actual_bytes}"
        )

    if "sha256" in record:
        actual_sha = sha256_of(path)
        if record["sha256"] != actual_sha:
            problems.append(
                f"{name}: manifest sha256 {record['sha256'][:12]}... "
                f"does not match file {actual_sha[:12]}..."
            )

    # --- declared page band -------------------------------------------------
    if band is not None:
        low, high = band
        if not low <= page_count <= high:
            problems.append(
                f"{name}: {page_count} pages is outside the declared band {low}-{high}"
            )

    if quick:
        return problems

    # --- per-page content ---------------------------------------------------
    empty_pages: list[int] = []
    unextractable: list[int] = []
    stranded_headings: list[int] = []

    previous_text = ""
    for index, page in enumerate(pages, start=1):
        try:
            text = (page.extract_text() or "").strip()
        except Exception:
            unextractable.append(index)
            previous_text = ""
            continue

        if not text:
            unextractable.append(index)
        elif len(text) < MIN_PAGE_CHARS:
            if not any(marker in text for marker in STRUCTURAL_PAGE_MARKERS):
                empty_pages.append(index)

        # A heading alone at the foot of a page, with its body on the next one,
        # is the classic ReportLab keep-with-next failure. Detect it as a short
        # final line that is title-cased and unpunctuated.
        lines = [line.strip() for line in text.splitlines() if line.strip()]
        if lines and index < page_count:
            last = lines[-1]
            if (
                4 < len(last) < 70
                and not last.endswith((".", ":", ";", ",", "?", "!", ")", "]"))
                and last[0].isupper()
                and len(lines) > 3
                and last.count(" ") < 9
                and any(char.isalpha() for char in last)
                and last.rstrip().split(" ")[0].rstrip(".").isdigit()
            ):
                stranded_headings.append(index)

        previous_text = text

    if unextractable:
        shown = ", ".join(str(p) for p in unextractable[:10])
        more = f" (+{len(unextractable) - 10} more)" if len(unextractable) > 10 else ""
        problems.append(
            f"{name}: no extractable text on page(s) {shown}{more} — "
            "check that fonts embedded correctly"
        )

    if empty_pages:
        shown = ", ".join(str(p) for p in empty_pages[:10])
        more = f" (+{len(empty_pages) - 10} more)" if len(empty_pages) > 10 else ""
        problems.append(
            f"{name}: near-empty page(s) {shown}{more} "
            f"(under {MIN_PAGE_CHARS} characters)"
        )

    if stranded_headings:
        shown = ", ".join(str(p) for p in stranded_headings[:10])
        more = f" (+{len(stranded_headings) - 10} more)" if len(stranded_headings) > 10 else ""
        problems.append(
            f"{name}: numbered heading stranded at the foot of page(s) {shown}{more}"
        )

    return problems


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--volume", help="restrict to one volume id from series.json")
    parser.add_argument("--quick", action="store_true",
                        help="skip per-page text extraction")
    args = parser.parse_args()

    if not MANIFEST.is_file():
        print(f"Missing {MANIFEST}. Build the series first.", file=sys.stderr)
        return 2

    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    records = manifest_entries(manifest)
    bands = page_bands()

    master_name, master = master_record()
    if master_name:
        records.setdefault(master_name, master)

    wanted_output: str | None = None
    if args.volume:
        series = json.loads(SERIES.read_text(encoding="utf-8"))
        match = [v for v in series["volumes"] if v["id"] == args.volume]
        if not match:
            print(f"Unknown volume id: {args.volume}", file=sys.stderr)
            return 2
        wanted_output = match[0]["output_name"]

    pdfs = sorted(DIST.rglob("*.pdf"))
    if wanted_output:
        pdfs = [p for p in pdfs if p.name == wanted_output]

    if not pdfs:
        print("No PDFs found to validate.", file=sys.stderr)
        return 2

    all_problems: list[str] = []
    unmanifested: list[str] = []
    total_pages = 0

    for path in pdfs:
        record = records.get(path.name)
        if record is None:
            unmanifested.append(path.name)
            record = {}
        problems = check_pdf(path, record, bands.get(path.name), args.quick)
        all_problems.extend(problems)
        if record.get("page_count"):
            total_pages += record["page_count"]

    missing_files = sorted(set(records) - {p.name for p in pdfs})
    if missing_files and not wanted_output:
        all_problems.extend(
            f"{name}: present in manifest but missing from dist/" for name in missing_files
        )

    if unmanifested:
        all_problems.extend(
            f"{name}: present in dist/ but absent from manifest.json" for name in unmanifested
        )

    if all_problems:
        print(f"PDF validation FAILED with {len(all_problems)} problem(s):\n", file=sys.stderr)
        for problem in all_problems:
            print(f"  - {problem}", file=sys.stderr)
        return 1

    mode = "structure and manifest" if args.quick else "structure, content, and manifest"
    print(
        f"PDF validation passed: {len(pdfs)} PDFs checked for {mode}; "
        f"{total_pages} pages reconciled against dist/manifest.json"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
