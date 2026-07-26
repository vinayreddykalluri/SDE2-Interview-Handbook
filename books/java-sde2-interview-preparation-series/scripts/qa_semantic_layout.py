#!/usr/bin/env python3
"""Audit semantic PDF pagination and reader-navigation quality.

This complements the raster QA scripts.  It uses pypdf for document structure
and pdfplumber for positioned text and vector-table inspection, then writes a
machine-readable JSON report and a concise Markdown review report.

The auditor is deliberately read-only with respect to source documents and
PDFs.  Its only writes are the two requested report files.
"""

from __future__ import annotations

import argparse
import fnmatch
import html
import json
import re
import sys
from collections import Counter
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable, Sequence

try:
    import pdfplumber
    from pypdf import PdfReader
except ImportError as exc:  # pragma: no cover - dependency boundary
    raise SystemExit(
        "qa_semantic_layout.py requires pdfplumber and pypdf. "
        "Run it with the bundled Codex Python runtime or install requirements.txt."
    ) from exc


ROOT = Path(__file__).resolve().parents[1]
SERIES = ROOT / "series"
DEFAULT_DIST = Path("series/dist")
DEFAULT_SPEC = Path("series/series.json")
DEFAULT_MASTER = Path("java-sde2-interview-book.pdf")
DEFAULT_OUTPUT = Path("series/tmp/pdfs/semantic-layout-qa")
INDEX_NAME = "Java-SDE2-Interview-Preparation-Series-Index.pdf"

LANGUAGE_LABEL = re.compile(
    r"^(?:JAVA|TEXT|SQL|BASH|HTTP|JSON)(?:\s+\(continued\s+\d+/\d+\))?$",
    re.IGNORECASE,
)
CONTINUED_LANGUAGE_LABEL = re.compile(
    r"^(?:JAVA|TEXT|SQL|BASH|HTTP|JSON)\s+\(continued\s+(\d+)/(\d+)\)$",
    re.IGNORECASE,
)
BOLD_FONT = re.compile(r"(?:bold|semibold|demibold|demi|heavy|black)", re.IGNORECASE)
MONO_FONT = re.compile(
    r"(?:mono|menlo|courier|consolas|jetbrains|sourcecode|code[-_ ]?pro)",
    re.IGNORECASE,
)
WORD = re.compile(r"[A-Za-z0-9_]+")

GENERATED_SECTION_HEADINGS = {
    "Purpose",
    "Prerequisites",
    "Recognition signals",
    "Outcomes",
    "Readiness target",
    "Choose the route that fits today",
    "Recommended study loop",
    "Practice ladder",
    "Navigation",
    "Completion check",
    "Volume Position",
}

SEMANTIC_CATEGORIES: tuple[tuple[str, re.Pattern[str]], ...] = (
    ("pattern", re.compile(r"^(?:pattern|family)\s+\d+\b", re.IGNORECASE)),
    (
        "recognition",
        re.compile(r"(?:recognition|decision|choose|when to use|selection)", re.IGNORECASE),
    ),
    (
        "reasoning",
        re.compile(r"(?:invariant|proof|first-principles|why it works)", re.IGNORECASE),
    ),
    (
        "walkthrough",
        re.compile(r"(?:dry run|worked|walkthrough|trace)", re.IGNORECASE),
    ),
    (
        "warning",
        re.compile(r"(?:edge case|mistake|pitfall|failure|watch out|trap)", re.IGNORECASE),
    ),
    (
        "practice",
        re.compile(r"(?:exercise|checkpoint|completion|question|practice)", re.IGNORECASE),
    ),
    ("summary", re.compile(r"(?:summary|revision|recap|readiness)", re.IGNORECASE)),
)

SEVERITY_RANK = {"info": 0, "warning": 1, "error": 2}


@dataclass(frozen=True)
class Document:
    """One PDF artifact to inspect."""

    document_id: str
    kind: str
    path: Path
    title: str


@dataclass
class TextLine:
    """A visual line reconstructed from pdfplumber words."""

    text: str
    top: float
    bottom: float
    x0: float
    x1: float
    max_size: float
    fonts: set[str] = field(default_factory=set)
    words: list[dict[str, Any]] = field(default_factory=list)

    @property
    def is_bold(self) -> bool:
        return any(BOLD_FONT.search(font) for font in self.fonts)


@dataclass(frozen=True)
class TableSummary:
    """Minimal table information retained across consecutive pages."""

    page: int
    bbox: tuple[float, float, float, float]
    header: tuple[str, ...]
    header_key: str
    rows: int
    body_rows: int


def compact(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def heading_key(text: str) -> str:
    """Normalize Markdown/PDF heading text without depending on punctuation."""
    value = html.unescape(text)
    value = re.sub(r"`([^`]*)`", r"\1", value)
    value = re.sub(r"\[([^]]+)]\([^)]*\)", r"\1", value)
    value = re.sub(r"<[^>]+>", " ", value)
    value = re.sub(r"[*_~]", "", value)
    return compact(re.sub(r"[^0-9A-Za-z]+", " ", value)).casefold()


def semantic_category(text: str) -> str:
    for category, pattern in SEMANTIC_CATEGORIES:
        if pattern.search(text):
            return category
    return "section"


def collect_headings(root: Path) -> dict[str, dict[str, Any]]:
    """Collect source H2/H3 labels used to distinguish headings from bold prose."""
    result: dict[str, dict[str, Any]] = {}
    roots = (root / "book", root / "series" / "volumes")
    for source_root in roots:
        if not source_root.exists():
            continue
        for path in source_root.rglob("*.md"):
            for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
                match = re.match(r"^(#{2,3})\s+(.+?)\s*$", raw)
                if not match:
                    continue
                level = len(match.group(1))
                label = compact(match.group(2))
                key = heading_key(label)
                if key:
                    result.setdefault(
                        key,
                        {
                            "label": label,
                            "levels": [],
                            "category": semantic_category(label),
                        },
                    )
                    if level not in result[key]["levels"]:
                        result[key]["levels"].append(level)
    for label in GENERATED_SECTION_HEADINGS:
        key = heading_key(label)
        result.setdefault(
            key,
            {"label": label, "levels": [2], "category": semantic_category(label)},
        )
    return result


def reconstruct_lines(
    words: Sequence[dict[str, Any]], y_tolerance: float = 2.4
) -> list[TextLine]:
    """Group positioned words into visual lines while preserving font evidence."""
    groups: list[list[dict[str, Any]]] = []
    for word in sorted(words, key=lambda item: (float(item["top"]), float(item["x0"]))):
        top = float(word["top"])
        if groups:
            anchor = sum(float(item["top"]) for item in groups[-1]) / len(groups[-1])
            if abs(top - anchor) <= y_tolerance:
                groups[-1].append(word)
                continue
        groups.append([word])

    lines: list[TextLine] = []
    for group in groups:
        ordered = sorted(group, key=lambda item: float(item["x0"]))
        text = compact(" ".join(str(item.get("text", "")) for item in ordered))
        if not text:
            continue
        lines.append(
            TextLine(
                text=text,
                top=min(float(item["top"]) for item in ordered),
                bottom=max(float(item["bottom"]) for item in ordered),
                x0=min(float(item["x0"]) for item in ordered),
                x1=max(float(item["x1"]) for item in ordered),
                max_size=max(float(item.get("size") or 0) for item in ordered),
                fonts={str(item.get("fontname") or "") for item in ordered},
                words=list(ordered),
            )
        )
    return lines


def is_code_label(line: TextLine) -> bool:
    return bool(
        LANGUAGE_LABEL.fullmatch(compact(line.text))
        and line.max_size <= 10.0
        and line.is_bold
    )


def code_label_findings(
    document: Document,
    page_number: int,
    page_height: float,
    lines: Sequence[TextLine],
    words: Sequence[dict[str, Any]],
    content_top: float,
    content_bottom: float,
) -> tuple[list[dict[str, Any]], int]:
    """Find language labels that have no nearby code on the same page."""
    findings: list[dict[str, Any]] = []
    checked = 0
    lower_limit = page_height - content_bottom
    for line in lines:
        if not is_code_label(line):
            continue
        if line.top < content_top or line.bottom > lower_limit:
            continue
        checked += 1
        nearby_mono = [
            word
            for word in words
            if float(word["top"]) > line.bottom + 0.5
            and float(word["top"]) <= line.bottom + 38
            and float(word["bottom"]) < lower_limit
            and MONO_FONT.search(str(word.get("fontname") or ""))
            and re.search(r"\S", str(word.get("text") or ""))
        ]
        if nearby_mono:
            continue
        following_lines = [
            candidate.text
            for candidate in lines
            if candidate.top > line.bottom + 0.5 and candidate.bottom < lower_limit
        ][:3]
        findings.append(
            {
                "check": "orphan_code_label",
                "severity": "error",
                "document_id": document.document_id,
                "document": document.title,
                "path": str(document.path),
                "page": page_number,
                "message": (
                    f"Code label '{line.text}' has no nearby monospace code on the same page."
                ),
                "evidence": {
                    "label_top": round(line.top, 2),
                    "label_bottom": round(line.bottom, 2),
                    "following_lines": following_lines,
                },
            }
        )
    return findings, checked


def joined_heading_match(
    lines: Sequence[TextLine],
    start: int,
    known_headings: dict[str, dict[str, Any]],
    minimum_size: float,
) -> tuple[int, dict[str, Any]] | None:
    """Match one-to-three wrapped bold lines to a known source H2/H3."""
    for span in (3, 2, 1):
        end = start + span
        if end > len(lines):
            continue
        batch = lines[start:end]
        if any(line.max_size < minimum_size or not line.is_bold for line in batch):
            continue
        if any(
            batch[index + 1].top - batch[index].bottom > 9
            for index in range(len(batch) - 1)
        ):
            continue
        sizes = [line.max_size for line in batch]
        if max(sizes) - min(sizes) > 1.8:
            continue
        key = heading_key(" ".join(line.text for line in batch))
        if key in known_headings:
            return span, known_headings[key]
    return None


def bottom_heading_findings(
    document: Document,
    page_number: int,
    page_height: float,
    lines: Sequence[TextLine],
    images: Sequence[dict[str, Any]],
    known_headings: dict[str, dict[str, Any]],
    content_top: float,
    content_bottom: float,
    bottom_zone: float,
    minimum_size: float,
    minimum_following_lines: int,
    minimum_following_words: int,
) -> tuple[list[dict[str, Any]], int]:
    """Flag source H2/H3 headings stranded near the page bottom."""
    findings: list[dict[str, Any]] = []
    checked = 0
    lower_limit = page_height - content_bottom
    zone_start = lower_limit - bottom_zone
    index = 0
    while index < len(lines):
        line = lines[index]
        if line.top < content_top or line.bottom > lower_limit:
            index += 1
            continue
        match = joined_heading_match(lines, index, known_headings, minimum_size)
        if not match:
            index += 1
            continue
        span, metadata = match
        checked += 1
        batch = lines[index : index + span]
        heading_bottom = max(item.bottom for item in batch)
        heading_top = min(item.top for item in batch)
        label = " ".join(item.text for item in batch)

        following: list[TextLine] = []
        for candidate_index, candidate in enumerate(
            lines[index + span :], start=index + span
        ):
            if candidate.top <= heading_bottom + 0.5 or candidate.bottom >= lower_limit:
                continue
            if is_code_label(candidate):
                continue
            candidate_match = joined_heading_match(
                lines, candidate_index, known_headings, minimum_size
            )
            if candidate_match:
                continue
            following.append(candidate)

        following_words = sum(len(WORD.findall(item.text)) for item in following)
        visual_below = any(
            float(image.get("top") or 0) > heading_bottom + 1
            and float(image.get("bottom") or page_height) < lower_limit
            for image in images
        )
        meaningful = visual_below or (
            len(following) >= minimum_following_lines
            and following_words >= minimum_following_words
        )
        # A known H2/H3 that is literally the last body text on the page is an
        # orphan even when it appears above the usual bottom zone; a protected
        # figure, code panel, or table may have moved and left a large white
        # hole. Keep the narrower bottom-zone heuristic for pages that do have
        # a small amount of following prose.
        last_body_element = not following and not visual_below
        if heading_top < zone_start and not last_body_element:
            index += span
            continue
        if not meaningful:
            findings.append(
                {
                    "check": "orphan_semantic_heading",
                    "severity": "error" if last_body_element else "warning",
                    "document_id": document.document_id,
                    "document": document.title,
                    "path": str(document.path),
                    "page": page_number,
                    "message": (
                        f"H2/H3 heading '{label}' "
                        + (
                            "is the final body element on the page."
                            if last_body_element
                            else (
                                f"begins in the bottom {bottom_zone:g}pt without "
                                "enough meaningful following text."
                            )
                        )
                    ),
                    "evidence": {
                        "source_label": metadata["label"],
                        "source_levels": metadata["levels"],
                        "semantic_category": metadata["category"],
                        "heading_top": round(heading_top, 2),
                        "heading_bottom": round(heading_bottom, 2),
                        "following_line_count": len(following),
                        "following_word_count": following_words,
                        "visual_below": visual_below,
                        "following_lines": [item.text for item in following[:3]],
                    },
                }
            )
        index += span
    return findings, checked


def heading_continuity_candidates(
    lines: Sequence[TextLine],
    images: Sequence[dict[str, Any]],
    found_tables: Sequence[Any],
    known_headings: dict[str, dict[str, Any]],
    page_height: float,
    content_top: float,
    content_bottom: float,
    minimum_size: float,
    minimum_following_lines: int,
    minimum_following_words: int,
) -> list[dict[str, Any]]:
    """Return headings with only a thin prose lead-in and no visual payload."""
    lower_limit = page_height - content_bottom
    candidates: list[dict[str, Any]] = []
    index = 0
    while index < len(lines):
        line = lines[index]
        if line.top < content_top or line.bottom > lower_limit:
            index += 1
            continue
        match = joined_heading_match(lines, index, known_headings, minimum_size)
        if not match:
            index += 1
            continue
        span, metadata = match
        batch = lines[index : index + span]
        heading_bottom = max(item.bottom for item in batch)
        following: list[TextLine] = []
        for candidate_index, candidate in enumerate(
            lines[index + span :], start=index + span
        ):
            if candidate.top <= heading_bottom + 0.5 or candidate.bottom >= lower_limit:
                continue
            if is_code_label(candidate):
                continue
            if joined_heading_match(lines, candidate_index, known_headings, minimum_size):
                continue
            following.append(candidate)
        following_words = sum(len(WORD.findall(item.text)) for item in following)
        visual_below = any(
            float(image.get("top") or 0) > heading_bottom + 1
            and float(image.get("bottom") or page_height) < lower_limit
            for image in images
        ) or any(float(table.bbox[1]) > heading_bottom + 1 for table in found_tables)
        meaningful = visual_below or (
            len(following) >= minimum_following_lines
            and following_words >= minimum_following_words
        )
        # Zero-following-line cases are already emitted by
        # bottom_heading_findings. This list targets the harder pattern where
        # a one-line lead-in remains while its protected payload moves.
        if not meaningful and following:
            candidates.append(
                {
                    "label": " ".join(item.text for item in batch),
                    "source_label": metadata["label"],
                    "heading_top": min(item.top for item in batch),
                    "heading_bottom": heading_bottom,
                    "following_line_count": len(following),
                    "following_word_count": following_words,
                    "following_lines": [item.text for item in following[:3]],
                }
            )
        index += span
    return candidates


def page_start_descriptor(
    lines: Sequence[TextLine],
    images: Sequence[dict[str, Any]],
    found_tables: Sequence[Any],
    known_headings: dict[str, dict[str, Any]],
    page_height: float,
    content_top: float,
    minimum_size: float,
) -> dict[str, Any]:
    """Describe whether a page begins with code, a table, or a figure."""
    body_lines = [line for line in lines if line.top >= content_top]
    first = body_lines[0] if body_lines else None
    first_is_heading = bool(
        first
        and joined_heading_match(body_lines, 0, known_headings, minimum_size)
    )
    first_is_code = bool(
        first
        and (
            is_code_label(first)
            or any(MONO_FONT.search(font) for font in first.fonts)
        )
    )
    top_limit = content_top + 115
    starts_with_visual = any(
        float(image.get("top") or page_height) <= top_limit for image in images
    ) or any(float(table.bbox[1]) <= top_limit for table in found_tables)
    return {
        "protected": not first_is_heading and (first_is_code or starts_with_visual),
        "first_lines": [line.text for line in body_lines[:4]],
        "first_is_code": first_is_code,
        "starts_with_visual": starts_with_visual,
    }


def normalized_cell(value: Any) -> str:
    return compact(str(value or "").replace("\n", " "))


def code_continuation_findings(
    document: Document,
    page_number: int,
    found_tables: Sequence[Any],
) -> tuple[list[dict[str, Any]], int]:
    """Flag continuation panels that contain only a tiny code fragment."""
    findings: list[dict[str, Any]] = []
    checked = 0
    for table in found_tables:
        rows = table.extract() or []
        if len(rows) != 2 or any(len(row) != 1 for row in rows):
            continue
        label = compact(str(rows[0][0] or ""))
        match = CONTINUED_LANGUAGE_LABEL.fullmatch(label)
        if not match:
            continue
        checked += 1
        code_lines = [
            line for line in str(rows[1][0] or "").splitlines() if line.strip()
        ]
        line_count = len(code_lines)
        if line_count >= 10:
            continue
        severity = "error" if line_count < 6 else "warning"
        findings.append(
            {
                "check": "tiny_code_continuation",
                "severity": severity,
                "document_id": document.document_id,
                "document": document.title,
                "path": str(document.path),
                "page": page_number,
                "message": (
                    f"Code panel '{label}' contains only {line_count} nonblank "
                    "extracted lines."
                ),
                "evidence": {
                    "continuation_index": int(match.group(1)),
                    "continuation_total": int(match.group(2)),
                    "nonblank_code_lines": line_count,
                    "code_preview": code_lines[:4],
                },
            }
        )
    return findings, checked


def page_tables(
    page: Any,
    page_number: int,
    found_tables: Sequence[Any] | None = None,
) -> list[TableSummary]:
    """Extract multi-column vector tables, excluding one-cell code panels."""
    summaries: list[TableSummary] = []
    for table in found_tables if found_tables is not None else page.find_tables():
        rows = table.extract() or []
        if not rows:
            continue
        header = tuple(normalized_cell(cell) for cell in rows[0])
        nonempty = [cell for cell in header if cell]
        if len(nonempty) < 2:
            continue
        header_key = " || ".join(cell.casefold() for cell in header)
        if len(re.sub(r"\W", "", header_key)) < 6:
            continue
        bbox = tuple(float(value) for value in table.bbox)
        summaries.append(
            TableSummary(
                page=page_number,
                bbox=(bbox[0], bbox[1], bbox[2], bbox[3]),
                header=header,
                header_key=header_key,
                rows=len(rows),
                body_rows=max(0, len(rows) - 1),
            )
        )
    return summaries


def table_continuation_findings(
    document: Document,
    pages: Sequence[list[TableSummary]],
    page_heights: Sequence[float],
    bottom_proximity: float,
    continuation_top: float,
    minimum_fragment_rows: int,
) -> list[dict[str, Any]]:
    """Detect the same table header at the bottom and top of consecutive pages."""
    findings: list[dict[str, Any]] = []
    for page_index in range(len(pages) - 1):
        previous = pages[page_index]
        following = pages[page_index + 1]
        if not previous or not following:
            continue
        previous_height = page_heights[page_index]
        for left in previous:
            if left.bbox[3] < previous_height - bottom_proximity:
                continue
            for right in following:
                if right.bbox[1] > continuation_top:
                    continue
                if left.header_key != right.header_key:
                    continue
                fragment_minimum = min(left.body_rows, right.body_rows)
                severity = (
                    "error" if fragment_minimum <= 1 else "warning"
                )
                qualification = (
                    "one-row (or header-only) fragment"
                    if fragment_minimum <= 1
                    else (
                        f"fragment below the {minimum_fragment_rows}-row target"
                        if fragment_minimum < minimum_fragment_rows
                        else "multi-page continuation requiring review"
                    )
                )
                findings.append(
                    {
                        "check": "repeated_table_header_continuation",
                        "severity": severity,
                        "document_id": document.document_id,
                        "document": document.title,
                        "path": str(document.path),
                        "page": left.page,
                        "next_page": right.page,
                        "message": (
                            f"Repeated table header across pages {left.page}-{right.page}; "
                            f"{qualification}."
                        ),
                        "evidence": {
                            "header": list(left.header),
                            "first_fragment_body_rows": left.body_rows,
                            "continuation_body_rows": right.body_rows,
                            "first_bbox": [round(value, 2) for value in left.bbox],
                            "continuation_bbox": [round(value, 2) for value in right.bbox],
                        },
                    }
                )
    return findings


def outline_items(items: Iterable[Any]) -> Iterable[Any]:
    for item in items:
        if isinstance(item, list):
            yield from outline_items(item)
        else:
            yield item


def chapter_one_page(reader: PdfReader) -> int | None:
    """Resolve Chapter 1 from the PDF outline, avoiding a TOC-text false match."""
    try:
        for destination in outline_items(reader.outline):
            title = compact(str(getattr(destination, "title", "")))
            if re.match(r"^Chapter\s+1\s*(?:-|:|$)", title, flags=re.IGNORECASE):
                return reader.get_destination_page_number(destination) + 1
    except Exception:
        pass
    for page_number, page in enumerate(reader.pages, start=1):
        text = page.extract_text() or ""
        if "SDE-2 CORE CHAPTER" in text:
            return page_number
    return None


def finding_counts(findings: Sequence[dict[str, Any]]) -> dict[str, int]:
    counter = Counter(str(item["severity"]) for item in findings)
    return {severity: counter.get(severity, 0) for severity in ("error", "warning", "info")}


def scan_document(
    document: Document,
    known_headings: dict[str, dict[str, Any]],
    args: argparse.Namespace,
) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    """Run every applicable check for one PDF."""
    findings: list[dict[str, Any]] = []
    reader = PdfReader(str(document.path))
    page_count = len(reader.pages)
    core_start_page: int | None = None
    if document.kind == "focused":
        core_start_page = chapter_one_page(reader)
        if core_start_page is None:
            findings.append(
                {
                    "check": "focused_core_start",
                    "severity": "error",
                    "document_id": document.document_id,
                    "document": document.title,
                    "path": str(document.path),
                    "page": None,
                    "message": "Could not resolve the focused volume's Chapter 1 page.",
                    "evidence": {"target_page": args.core_start_max_page},
                }
            )
        elif core_start_page > args.core_start_max_page:
            findings.append(
                {
                    "check": "focused_core_start",
                    "severity": "error",
                    "document_id": document.document_id,
                    "document": document.title,
                    "path": str(document.path),
                    "page": core_start_page,
                    "message": (
                        f"Core learning begins on page {core_start_page}; target is page "
                        f"{args.core_start_max_page} or earlier."
                    ),
                    "evidence": {
                        "core_start_page": core_start_page,
                        "target_page": args.core_start_max_page,
                    },
                }
            )
    if document.kind == "index" and page_count > args.index_max_pages:
        findings.append(
            {
                "check": "index_page_count",
                "severity": "error",
                "document_id": document.document_id,
                "document": document.title,
                "path": str(document.path),
                "page": None,
                "message": (
                    f"Series index is {page_count} pages; target is at most "
                    f"{args.index_max_pages}."
                ),
                "evidence": {
                    "page_count": page_count,
                    "target_max_pages": args.index_max_pages,
                },
            }
        )

    tables_by_page: list[list[TableSummary]] = []
    page_heights: list[float] = []
    code_labels_checked = 0
    code_continuations_checked = 0
    headings_checked = 0
    tables_found = 0
    continuity_candidates_by_page: list[list[dict[str, Any]]] = []
    page_starts: list[dict[str, Any]] = []
    with pdfplumber.open(str(document.path)) as pdf:
        if len(pdf.pages) != page_count:
            raise RuntimeError(
                f"pypdf reports {page_count} pages but pdfplumber reports {len(pdf.pages)}"
            )
        for page_number, page in enumerate(pdf.pages, start=1):
            page_heights.append(float(page.height))
            words = page.extract_words(
                x_tolerance=1.5,
                y_tolerance=2.0,
                keep_blank_chars=False,
                extra_attrs=["fontname", "size"],
            )
            lines = reconstruct_lines(words)
            code_findings, code_checked = code_label_findings(
                document,
                page_number,
                float(page.height),
                lines,
                words,
                args.content_top,
                args.content_bottom,
            )
            findings.extend(code_findings)
            code_labels_checked += code_checked
            heading_findings, heading_checked = bottom_heading_findings(
                document,
                page_number,
                float(page.height),
                lines,
                page.images,
                known_headings,
                args.content_top,
                args.content_bottom,
                args.heading_bottom_zone,
                args.heading_min_size,
                args.min_following_lines,
                args.min_following_words,
            )
            findings.extend(heading_findings)
            headings_checked += heading_checked
            found_tables = page.find_tables()
            continuation_findings, continuation_checked = code_continuation_findings(
                document, page_number, found_tables
            )
            findings.extend(continuation_findings)
            code_continuations_checked += continuation_checked
            continuity_candidates_by_page.append(
                heading_continuity_candidates(
                    lines,
                    page.images,
                    found_tables,
                    known_headings,
                    float(page.height),
                    args.content_top,
                    args.content_bottom,
                    args.heading_min_size,
                    args.min_following_lines,
                    args.min_following_words,
                )
            )
            page_starts.append(
                page_start_descriptor(
                    lines,
                    page.images,
                    found_tables,
                    known_headings,
                    float(page.height),
                    args.content_top,
                    args.heading_min_size,
                )
            )
            if args.skip_table_check:
                tables_by_page.append([])
            else:
                summaries = page_tables(page, page_number, found_tables)
                tables_by_page.append(summaries)
                tables_found += len(summaries)
            page.close()

    for page_index, candidates in enumerate(continuity_candidates_by_page[:-1]):
        next_start = page_starts[page_index + 1]
        if not next_start["protected"]:
            continue
        for candidate in candidates:
            findings.append(
                {
                    "check": "cross_page_heading_payload",
                    "severity": "error",
                    "document_id": document.document_id,
                    "document": document.title,
                    "path": str(document.path),
                    "page": page_index + 1,
                    "next_page": page_index + 2,
                    "message": (
                        f"Heading '{candidate['label']}' has only a thin lead-in before "
                        "a protected payload starts the next page."
                    ),
                    "evidence": {
                        **candidate,
                        "next_page_first_lines": next_start["first_lines"],
                        "next_page_starts_with_code": next_start["first_is_code"],
                        "next_page_starts_with_visual": next_start["starts_with_visual"],
                    },
                }
            )

    if not args.skip_table_check:
        findings.extend(
            table_continuation_findings(
                document,
                tables_by_page,
                page_heights,
                args.table_bottom_proximity,
                args.table_continuation_top,
                args.table_min_fragment_rows,
            )
        )

    record = {
        "id": document.document_id,
        "kind": document.kind,
        "title": document.title,
        "path": str(document.path),
        "pages": page_count,
        "core_start_page": core_start_page,
        "code_labels_checked": code_labels_checked,
        "code_continuations_checked": code_continuations_checked,
        "source_headings_checked": headings_checked,
        "vector_tables_found": tables_found,
        "findings": finding_counts(findings),
    }
    return record, findings


def load_documents(
    root: Path,
    master: Path,
    dist: Path,
    spec_path: Path,
    include_patterns: Sequence[str],
) -> tuple[list[Document], list[str]]:
    """Discover the master and every PDF physically present in series/dist."""
    expected: dict[str, tuple[str, str]] = {}
    expected_order: list[str] = []
    warnings: list[str] = []
    if spec_path.exists():
        data = json.loads(spec_path.read_text(encoding="utf-8"))
        for volume in data.get("volumes", []):
            filename = str(volume["output_name"])
            expected[filename] = (str(volume["id"]), str(volume["title"]))
            expected_order.append(filename)
    else:
        warnings.append(f"Series specification not found: {spec_path}")

    documents: list[Document] = []
    if master.exists():
        documents.append(Document("MASTER", "master", master.resolve(), "Master Book"))
    else:
        warnings.append(f"Master PDF not found: {master}")

    discovered = {path.name: path for path in dist.glob("*.pdf")} if dist.exists() else {}
    for filename in sorted(set(expected) - set(discovered)):
        warnings.append(f"Expected focused PDF not found: {dist / filename}")
    if INDEX_NAME not in discovered:
        warnings.append(f"Series index PDF not found: {dist / INDEX_NAME}")
    ordered_names = [name for name in expected_order if name in discovered]
    if INDEX_NAME in discovered:
        ordered_names.append(INDEX_NAME)
    ordered_names.extend(sorted(set(discovered) - set(ordered_names)))
    for filename in ordered_names:
        path = discovered[filename]
        if filename == INDEX_NAME:
            documents.append(Document("INDEX", "index", path.resolve(), "Series Index"))
        elif filename in expected:
            document_id, title = expected[filename]
            documents.append(Document(document_id, "focused", path.resolve(), title))
        else:
            documents.append(Document(path.stem, "series-other", path.resolve(), path.stem))

    if include_patterns:
        documents = [
            document
            for document in documents
            if any(
                fnmatch.fnmatch(document.document_id, pattern)
                or fnmatch.fnmatch(document.path.name, pattern)
                for pattern in include_patterns
            )
        ]
    return documents, warnings


def markdown_escape(value: Any) -> str:
    return compact(str(value)).replace("|", "\\|")


def write_markdown_report(report: dict[str, Any], path: Path) -> None:
    findings = report["findings"]
    lines = [
        "# Semantic PDF Layout QA",
        "",
        f"Generated: {report['generated_at']}",
        "",
        f"Quality status: **{report['quality_status'].upper()}**",
        "",
        "## Summary",
        "",
        "| Documents | Pages | Errors | Warnings | Code labels | Continuations | Source headings | Tables |",
        "|---:|---:|---:|---:|---:|---:|---:|---:|",
        (
            f"| {report['summary']['documents']} | {report['summary']['pages']} | "
            f"{report['summary']['findings']['error']} | "
            f"{report['summary']['findings']['warning']} | "
            f"{report['summary']['code_labels_checked']} | "
            f"{report['summary']['code_continuations_checked']} | "
            f"{report['summary']['source_headings_checked']} | "
            f"{report['summary']['vector_tables_found']} |"
        ),
        "",
        "## Documents",
        "",
        "| ID | Kind | Pages | Core starts | Errors | Warnings |",
        "|---|---|---:|---:|---:|---:|",
    ]
    for document in report["documents"]:
        core = document["core_start_page"] if document["core_start_page"] is not None else "-"
        lines.append(
            f"| {markdown_escape(document['id'])} | {markdown_escape(document['kind'])} | "
            f"{document['pages']} | {core} | {document['findings']['error']} | "
            f"{document['findings']['warning']} |"
        )

    lines.extend(["", "## Findings", ""])
    if not findings:
        lines.append("No semantic-layout findings.")
    else:
        lines.extend(
            [
                "| Severity | Check | Document | Page | Finding |",
                "|---|---|---|---:|---|",
            ]
        )
        for finding in sorted(
            findings,
            key=lambda item: (
                -SEVERITY_RANK[str(item["severity"])],
                str(item["document_id"]),
                int(item.get("page") or 0),
                str(item["check"]),
            ),
        ):
            page = finding.get("page") or "-"
            if finding.get("next_page"):
                page = f"{page}-{finding['next_page']}"
            lines.append(
                f"| {finding['severity']} | {markdown_escape(finding['check'])} | "
                f"{markdown_escape(finding['document_id'])} | {page} | "
                f"{markdown_escape(finding['message'])} |"
            )

    if report.get("discovery_warnings"):
        lines.extend(["", "## Discovery warnings", ""])
        lines.extend(f"- {warning}" for warning in report["discovery_warnings"])
    path.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")


def quality_status(findings: Sequence[dict[str, Any]]) -> str:
    if any(item["severity"] == "error" for item in findings):
        return "fail"
    if any(item["severity"] == "warning" for item in findings):
        return "review"
    return "pass"


def should_fail(findings: Sequence[dict[str, Any]], fail_level: str) -> bool:
    if fail_level == "none":
        return False
    threshold = SEVERITY_RANK[fail_level]
    return any(SEVERITY_RANK[str(item["severity"])] >= threshold for item in findings)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Audit the master book and all series PDFs for semantic pagination, "
            "navigation, code-label, heading, and table-continuation defects."
        ),
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
        epilog=(
            "Examples:\n"
            "  python3 scripts/qa_semantic_layout.py\n"
            "  python3 scripts/qa_semantic_layout.py --include '12' --fail-level none\n"
            "  python3 scripts/qa_semantic_layout.py --skip-table-check --output /tmp/pdf-qa"
        ),
    )
    parser.add_argument("--root", type=Path, default=ROOT, help="Book project root")
    parser.add_argument("--master", type=Path, default=DEFAULT_MASTER, help="Master PDF")
    parser.add_argument("--dist", type=Path, default=DEFAULT_DIST, help="Focused PDF directory")
    parser.add_argument("--spec", type=Path, default=DEFAULT_SPEC, help="Series JSON specification")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="Report directory")
    parser.add_argument(
        "--include",
        action="append",
        default=[],
        metavar="GLOB",
        help="Only scan IDs or filenames matching this glob; repeatable",
    )
    parser.add_argument(
        "--core-start-max-page",
        type=int,
        default=4,
        help="Latest acceptable Chapter 1 page in focused volumes",
    )
    parser.add_argument(
        "--index-max-pages", type=int, default=12, help="Maximum series-index pages"
    )
    parser.add_argument(
        "--heading-bottom-zone",
        type=float,
        default=100,
        help="Bottom content-zone depth in points for orphan-heading checks",
    )
    parser.add_argument(
        "--heading-min-size",
        type=float,
        default=11.0,
        help="Minimum visual heading font size",
    )
    parser.add_argument(
        "--min-following-lines",
        type=int,
        default=2,
        help="Meaningful lines required below a bottom-zone H2/H3",
    )
    parser.add_argument(
        "--min-following-words",
        type=int,
        default=8,
        help="Meaningful words required below a bottom-zone H2/H3",
    )
    parser.add_argument(
        "--content-top", type=float, default=45, help="Points excluded at the page top"
    )
    parser.add_argument(
        "--content-bottom",
        type=float,
        default=45,
        help="Points excluded at the page bottom",
    )
    parser.add_argument(
        "--table-bottom-proximity",
        type=float,
        default=125,
        help="How near the page bottom a first table fragment must end",
    )
    parser.add_argument(
        "--table-continuation-top",
        type=float,
        default=165,
        help="Maximum top coordinate for the next-page repeated header",
    )
    parser.add_argument(
        "--table-min-fragment-rows",
        type=int,
        default=4,
        help="Target minimum body rows on each table fragment",
    )
    parser.add_argument(
        "--skip-table-check",
        action="store_true",
        help="Skip the slower vector-table continuation scan",
    )
    parser.add_argument(
        "--require-all",
        action="store_true",
        help="Treat missing master or series-spec artifacts as errors",
    )
    parser.add_argument(
        "--fail-level",
        choices=("none", "warning", "error"),
        default="error",
        help="Lowest finding severity that produces exit code 1",
    )
    parser.add_argument("--quiet", action="store_true", help="Suppress per-document progress")
    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    root = args.root.resolve()
    master = args.master if args.master.is_absolute() else root / args.master
    dist = args.dist if args.dist.is_absolute() else root / args.dist
    spec = args.spec if args.spec.is_absolute() else root / args.spec
    output = args.output if args.output.is_absolute() else root / args.output
    output = output.resolve()
    output.mkdir(parents=True, exist_ok=True)

    documents, discovery_warnings = load_documents(
        root.resolve(), master.resolve(), dist.resolve(), spec.resolve(), args.include
    )
    if not documents:
        parser.error("no matching PDF artifacts were found")
    known_headings = collect_headings(root)

    findings: list[dict[str, Any]] = []
    if args.require_all:
        for warning in discovery_warnings:
            findings.append(
                {
                    "check": "artifact_discovery",
                    "severity": "error",
                    "document_id": "DISCOVERY",
                    "document": "Artifact discovery",
                    "path": str(root),
                    "page": None,
                    "message": warning,
                    "evidence": {},
                }
            )

    records: list[dict[str, Any]] = []
    for index, document in enumerate(documents, start=1):
        if not args.quiet:
            print(
                f"[{index}/{len(documents)}] Scanning {document.document_id}: "
                f"{document.path.name}",
                flush=True,
            )
        try:
            record, document_findings = scan_document(document, known_headings, args)
        except Exception as exc:  # keep the report useful after a single corrupt PDF
            record = {
                "id": document.document_id,
                "kind": document.kind,
                "title": document.title,
                "path": str(document.path),
                "pages": 0,
                "core_start_page": None,
                "code_labels_checked": 0,
                "code_continuations_checked": 0,
                "source_headings_checked": 0,
                "vector_tables_found": 0,
                "findings": {"error": 1, "warning": 0, "info": 0},
            }
            document_findings = [
                {
                    "check": "scan_error",
                    "severity": "error",
                    "document_id": document.document_id,
                    "document": document.title,
                    "path": str(document.path),
                    "page": None,
                    "message": f"Semantic scan failed: {exc}",
                    "evidence": {"exception_type": type(exc).__name__},
                }
            ]
        records.append(record)
        findings.extend(document_findings)
        if not args.quiet:
            counts = finding_counts(document_findings)
            print(
                f"    {record['pages']} pages; {counts['error']} errors, "
                f"{counts['warning']} warnings",
                flush=True,
            )

    counts = finding_counts(findings)
    summary = {
        "documents": len(records),
        "pages": sum(int(record["pages"]) for record in records),
        "findings": counts,
        "code_labels_checked": sum(int(record["code_labels_checked"]) for record in records),
        "code_continuations_checked": sum(
            int(record["code_continuations_checked"]) for record in records
        ),
        "source_headings_checked": sum(
            int(record["source_headings_checked"]) for record in records
        ),
        "vector_tables_found": sum(int(record["vector_tables_found"]) for record in records),
    }
    report = {
        "schema_version": 1,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "quality_status": quality_status(findings),
        "configuration": {
            "root": str(root),
            "master": str(master.resolve()),
            "dist": str(dist.resolve()),
            "spec": str(spec.resolve()),
            "core_start_max_page": args.core_start_max_page,
            "index_max_pages": args.index_max_pages,
            "heading_bottom_zone": args.heading_bottom_zone,
            "heading_min_size": args.heading_min_size,
            "min_following_lines": args.min_following_lines,
            "min_following_words": args.min_following_words,
            "table_bottom_proximity": args.table_bottom_proximity,
            "table_continuation_top": args.table_continuation_top,
            "table_min_fragment_rows": args.table_min_fragment_rows,
            "table_check_enabled": not args.skip_table_check,
            "fail_level": args.fail_level,
        },
        "discovery_warnings": discovery_warnings,
        "summary": summary,
        "documents": records,
        "findings": findings,
    }
    json_path = output / "qa-semantic-layout.json"
    markdown_path = output / "qa-semantic-layout.md"
    json_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    write_markdown_report(report, markdown_path)

    print(
        f"Semantic layout QA {report['quality_status']}: {summary['documents']} PDFs, "
        f"{summary['pages']} pages, {counts['error']} errors, {counts['warning']} warnings"
    )
    print(f"JSON: {json_path}")
    print(f"Markdown: {markdown_path}")
    return 1 if should_fail(findings, args.fail_level) else 0


if __name__ == "__main__":
    raise SystemExit(main())
