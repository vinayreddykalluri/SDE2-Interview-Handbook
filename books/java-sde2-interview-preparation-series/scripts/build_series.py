#!/usr/bin/env python3
"""Build the focused Java SDE-2 interview-preparation PDF series."""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import re
import shutil
import subprocess
from pathlib import Path
from typing import Any, Iterable, Sequence

from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.colors import HexColor
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import inch
from reportlab.pdfgen import canvas as pdf_canvas
from reportlab.platypus import (
    BaseDocTemplate,
    CondPageBreak,
    Flowable,
    Frame,
    HRFlowable,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)
from reportlab.platypus.tableofcontents import TableOfContents
from reportlab.platypus.doctemplate import ActionFlowable

import build_book as master


ROOT = Path(__file__).resolve().parents[1]
PUBLISHING = ROOT / "publishing"
MANIFEST = PUBLISHING / "series.json"
BUILD = ROOT / "build" / "series"
DIST = ROOT / "dist"
TMP = ROOT / "tmp" / "pdfs"
COVER_BACKGROUND = PUBLISHING / "assets" / "modern-series-cover-background-v2.png"

AUTHOR = "Vinay Reddy Kalluri"
SERIES_TITLE = "Java SDE-2 Interview Preparation Series"
INDEX_NAME = "Java-SDE2-Interview-Preparation-Series-Index.pdf"
PAGE_W, PAGE_H = letter
MARGIN = 58
CONTENT_W = PAGE_W - 2 * MARGIN
COVER_W = 6.5 * inch

NAVY = master.NAVY
BLUE = master.BLUE
DARK_BLUE = master.DARK_BLUE
GOLD = master.GOLD
INK = master.INK
MUTED = master.MUTED
PALE = master.PALE
LIGHT = master.LIGHT
LINE = master.LINE


class CoverBand(Flowable):
    def __init__(self, text: str, font_name: str):
        super().__init__()
        self.text = text
        self.font_name = font_name
        self.width = COVER_W
        self.height = 30
        self.hAlign = "CENTER"

    def wrap(self, avail_width: float, avail_height: float) -> tuple[float, float]:
        return min(self.width, avail_width), self.height

    def draw(self) -> None:
        self.canv.setFillColor(NAVY)
        self.canv.rect(0, 0, self.width, self.height, stroke=0, fill=1)
        self.canv.setFillColor(colors.white)
        self.canv.setFont(self.font_name, 9.2)
        self.canv.drawCentredString(self.width / 2, 10.5, self.text)


class SeriesPdfRenderer(master.PdfRenderer):
    """Use the master renderer while giving series part dividers their own identity."""

    def __init__(self, styles: dict[str, ParagraphStyle], fonts: dict[str, str]):
        super().__init__(styles, fonts)
        self.pending_part = False

    def heading(self, level: int, text: str) -> list[Flowable]:
        if level == 1 and (
            text.startswith("Start Here") or text == "How to Use the Series"
        ):
            items = super().heading(level, text)
            if items and isinstance(items[0], (PageBreak, CondPageBreak)):
                return items[1:]
            return items
        if level != 1 or not (text.startswith("Part ") or text == "Appendices"):
            items = super().heading(level, text)
            if level == 1 and self.pending_part:
                self.pending_part = False
                if items and isinstance(items[0], (PageBreak, CondPageBreak)):
                    items = items[1:]
            return items

        self.heading_index += 1
        name = master.bookmark_name(text, self.heading_index)
        self.in_part = True
        self.pending_part = True
        para = Paragraph(html.escape(text), self.styles["series_part"])
        para._bookmark_name = name
        para._bookmark_text = text
        para._toc_level = 0
        para._outline_level = 0
        para._heading_level = 1
        return [
            CondPageBreak(master.FRAME_H - 12),
            para,
            HRFlowable(
                width="100%",
                thickness=1.2,
                color=GOLD,
                spaceBefore=2,
                spaceAfter=10,
            ),
        ]


class IndexPdfRenderer(SeriesPdfRenderer):
    """Keep index stage entries compact while retaining bookmarks and TOC links."""

    def heading(self, level: int, text: str) -> list[Flowable]:
        if level == 1 and (text.startswith("Stage ") or text.startswith("Learning Step ")):
            self.heading_index += 1
            self.pending_part = False
            name = master.bookmark_name(text, self.heading_index)
            para = Paragraph(html.escape(text), self.styles["stage_card"])
            para._bookmark_name = name
            para._bookmark_text = text
            para._toc_level = 1
            para._outline_level = 1
            para._heading_level = 1
            return [para]
        return super().heading(level, text)


class SeriesDocTemplate(BaseDocTemplate):
    def __init__(
        self,
        filename: str,
        styles: dict[str, ParagraphStyle],
        spec: dict[str, Any],
    ) -> None:
        super().__init__(
            filename,
            pagesize=letter,
            leftMargin=MARGIN,
            rightMargin=MARGIN,
            topMargin=62,
            bottomMargin=56,
            title=spec["title"],
            author=AUTHOR,
            subject=spec["subtitle"],
        )
        self.styles_map = styles
        self.spec = spec
        self.current_part = ""
        self.current_chapter = ""
        self.current_section = ""
        self.page_context: dict[int, str] = {}
        self._context_page = 0
        self._page_has_body = False
        frame = Frame(
            self.leftMargin,
            self.bottomMargin,
            self.width,
            self.height,
            id="body",
            leftPadding=0,
            rightPadding=0,
            topPadding=0,
            bottomPadding=0,
        )
        self.addPageTemplates(
            PageTemplate(
                id="series",
                frames=[frame],
                onPage=self.capture_page_start,
                onPageEnd=self.draw_page,
            )
        )

    def beforeDocument(self) -> None:
        self.current_part = ""
        self.current_chapter = ""
        self.current_section = ""
        self.page_context = {}
        self._context_page = 0
        self._page_has_body = False

    def capture_page_start(self, canvas, doc) -> None:
        self._context_page = doc.page
        self._page_has_body = False
        if doc.page == 1 and COVER_BACKGROUND.exists():
            canvas.saveState()
            canvas.drawImage(
                str(COVER_BACKGROUND),
                0,
                0,
                width=PAGE_W,
                height=PAGE_H,
                preserveAspectRatio=False,
                mask="auto",
            )
            canvas.restoreState()
        inherited = self.current_section or self.current_chapter or self.current_part
        if inherited:
            self.page_context.setdefault(doc.page, inherited)

    def draw_page(self, canvas, doc) -> None:
        canvas.saveState()
        canvas.setTitle(self.spec["title"])
        canvas.setAuthor(AUTHOR)
        canvas.setSubject(self.spec["subtitle"])
        if doc.page == 1:
            canvas.setFillColor(NAVY)
            canvas.rect(0, PAGE_H - 12, PAGE_W, 12, stroke=0, fill=1)
            canvas.setFillColor(GOLD)
            canvas.rect(0, 0, PAGE_W, 7, stroke=0, fill=1)
        else:
            canvas.setStrokeColor(LINE)
            canvas.setLineWidth(0.5)
            canvas.line(MARGIN, PAGE_H - 38, PAGE_W - MARGIN, PAGE_H - 38)
            canvas.setFont(self.styles_map["small"].fontName, 7.6)
            canvas.setFillColor(MUTED)
            canvas.drawString(MARGIN, PAGE_H - 31, SERIES_TITLE)
            header = self.page_context.get(doc.page) or self.current_section or self.current_chapter or self.current_part or self.spec["short_title"]
            canvas.drawRightString(PAGE_W - MARGIN, PAGE_H - 31, header[:70])
            canvas.line(MARGIN, 39, PAGE_W - MARGIN, 39)
            canvas.drawString(MARGIN, 27, self.spec["volume_label"])
            canvas.drawRightString(PAGE_W - MARGIN, 27, str(doc.page))
        canvas.restoreState()

    def afterFlowable(self, flowable: Flowable) -> None:
        if self._context_page != self.page:
            self._context_page = self.page
            self._page_has_body = False
        if not isinstance(flowable, Paragraph):
            if not isinstance(flowable, (ActionFlowable, PageBreak, CondPageBreak, Spacer, HRFlowable)):
                self._page_has_body = True
            return
        text = getattr(flowable, "_bookmark_text", flowable.getPlainText())
        heading_level = getattr(flowable, "_heading_level", None)
        if text.startswith("Part ") or text == "Appendices":
            self.current_part = text
            self.current_chapter = ""
            self.current_section = ""
            self.page_context[self.page] = text
        elif text.startswith("Chapter ") or text.startswith("Appendix "):
            self.current_chapter = text
            self.current_section = ""
            self.page_context[self.page] = text
        elif heading_level == 1:
            self.current_section = text
            if not self._page_has_body:
                self.page_context[self.page] = text
            else:
                self.page_context.setdefault(self.page, text)
        elif heading_level == 2:
            self.current_section = text
            if not self._page_has_body or getattr(flowable, "_force_page_context", False):
                self.page_context[self.page] = text
        if text.strip():
            self._page_has_body = True
        if not hasattr(flowable, "_bookmark_name"):
            return
        key = getattr(flowable, "_bookmark_name")
        toc_level = getattr(flowable, "_toc_level", 0)
        outline_level = getattr(flowable, "_outline_level", toc_level)
        self.canv.bookmarkPage(key)
        try:
            self.canv.addOutlineEntry(text, key, level=outline_level, closed=outline_level > 0)
        except ValueError:
            self.canv.addOutlineEntry(text, key, level=max(0, outline_level - 1), closed=True)
        if getattr(flowable, "_include_toc", True):
            self.notify("TOCEntry", (toc_level, text, self.page, key))


def invariant_canvas(*args: Any, **kwargs: Any):
    kwargs["invariant"] = 1
    return pdf_canvas.Canvas(*args, **kwargs)


def run(command: Sequence[str], capture: bool = False) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        list(map(str, command)),
        cwd=str(ROOT),
        check=True,
        text=True,
        capture_output=capture,
    )


def load_manifest() -> dict[str, Any]:
    data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    volumes = data.get("volumes", [])
    if not volumes:
        raise RuntimeError("series.json does not define any volumes")
    ids = [item["id"] for item in volumes]
    if len(ids) != len(set(ids)):
        raise RuntimeError("series.json contains duplicate physical volume IDs")
    outputs = [item["output_name"] for item in volumes]
    if len(outputs) != len(set(outputs)):
        raise RuntimeError("series.json contains duplicate output names")
    path_labels = data.get("path_labels", {})
    if set(path_labels) != set(ids):
        raise RuntimeError("path_labels must define exactly one public study code per physical volume")
    if len(set(path_labels.values())) != len(ids):
        raise RuntimeError("path_labels contains duplicate public study codes")
    ordered = learning_volumes(data)
    segments = data.get("segments", [])
    if not segments:
        raise RuntimeError("series.json must define at least one selectable segment")
    flattened: list[str] = []
    by_id = {item["id"]: item for item in volumes}
    for segment in segments:
        book_ids = segment.get("books", [])
        if not book_ids:
            raise RuntimeError(f"Segment {segment.get('id', '<unknown>')} has no books")
        flattened.extend(book_ids)
        for segment_position, item_id in enumerate(book_ids, start=1):
            if item_id not in by_id:
                raise RuntimeError(f"Unknown volume {item_id} in segment {segment['id']}")
            item = by_id[item_id]
            if "segment_id" in item:
                raise RuntimeError(f"Volume {item_id} appears in more than one segment")
            item["segment_id"] = segment["id"]
            item["segment_title"] = segment["title"]
            item["segment_short_title"] = segment["short_title"]
            item["segment_code"] = segment["code"]
            item["segment_position"] = segment_position
            item["segment_count"] = len(book_ids)
    if flattened != data.get("learning_order"):
        raise RuntimeError("learning_order must equal the three segment book lists in order")
    if set(flattened) != set(ids) or len(flattened) != len(ids):
        raise RuntimeError("segments must contain every physical volume exactly once")
    for position, item in enumerate(ordered, start=1):
        item["path_label"] = str(path_labels[item["id"]])
        item["book_position"] = position
        item["volume_label"] = (
            f"{item['segment_title']} - Book {item['segment_position']:02d} "
            f"of {item['segment_count']:02d} - Study Step {item['path_label']}"
        )
    return data


def learning_volumes(manifest: dict[str, Any]) -> list[dict[str, Any]]:
    """Return physical volumes in the recommended reader path."""
    volumes = manifest["volumes"]
    requested = manifest.get("learning_order")
    if not requested:
        return volumes
    by_id = {item["id"]: item for item in volumes}
    missing = [item_id for item_id in requested if item_id not in by_id]
    if missing:
        raise RuntimeError(f"Unknown IDs in learning_order: {missing}")
    if set(requested) != set(by_id):
        omitted = sorted(set(by_id) - set(requested))
        raise RuntimeError(f"learning_order omits physical volume IDs: {omitted}")
    return [by_id[item_id] for item_id in requested]


def segment_volumes(manifest: dict[str, Any], segment_id: str) -> list[dict[str, Any]]:
    by_id = {item["id"]: item for item in manifest["volumes"]}
    segment = next(item for item in manifest["segments"] if item["id"] == segment_id)
    return [by_id[item_id] for item_id in segment["books"]]


def safe_source_path(relative: str) -> Path:
    path = (ROOT / relative).resolve()
    try:
        path.relative_to(ROOT.resolve())
    except ValueError as exc:
        raise RuntimeError(f"Source escapes project root: {relative}") from exc
    if not path.exists():
        raise RuntimeError(f"Missing series source: {relative}")
    return path


def source_title(text: str, fallback: str) -> str:
    match = re.search(r"^#\s+(.+)$", text, flags=re.MULTILINE)
    if not match:
        return fallback
    title = match.group(1).strip()
    title = re.sub(r"^(?:Chapter\s+)?\d+\s*(?::|\.|-)\s*", "", title, flags=re.IGNORECASE)
    title = re.sub(r"^Appendix\s+[A-Z]\s*-\s*", "", title, flags=re.IGNORECASE)
    return title.strip()


def strip_first_h1(text: str) -> str:
    lines = text.splitlines()
    for index, line in enumerate(lines):
        if line.startswith("# "):
            del lines[index]
            if index < len(lines) and not lines[index].strip():
                del lines[index]
            break
    return "\n".join(lines).strip()


def fence_marker(line: str) -> str | None:
    match = re.match(r"^\s*(`{3,}|~{3,})", line)
    return match.group(1)[0] if match else None


def block_ranges(lines: list[str], marker: str) -> list[tuple[str, int, int]]:
    headings: list[tuple[str, int]] = []
    active_fence: str | None = None
    for index, line in enumerate(lines):
        current = fence_marker(line)
        if current and (active_fence is None or current == active_fence):
            active_fence = None if active_fence else current
        elif active_fence is None and line.startswith(marker + " ") and not line.startswith(marker + "#"):
            headings.append((line[len(marker) + 1 :].strip(), index))
    result: list[tuple[str, int, int]] = []
    for position, (title, start) in enumerate(headings):
        end = headings[position + 1][1] if position + 1 < len(headings) else len(lines)
        result.append((title, start, end))
    return result


def extract_sections(text: str, sections: dict[str, Any] | None) -> str:
    body = strip_first_h1(text)
    if not sections:
        return body
    lines = body.splitlines()
    selected: list[str] = []
    for h2_title, start, end in block_ranges(lines, "##"):
        if h2_title not in sections:
            continue
        h3_selection = sections[h2_title]
        if h3_selection is None:
            selected.extend(lines[start:end])
            selected.append("")
            continue
        block = lines[start:end]
        selected.append(block[0])
        h3_ranges = block_ranges(block[1:], "###")
        first_h3 = h3_ranges[0][1] + 1 if h3_ranges else len(block)
        selected.extend(block[1:first_h3])
        wanted = set(h3_selection)
        for h3_title, h3_start, h3_end in h3_ranges:
            if h3_title in wanted:
                selected.extend(block[h3_start + 1 : h3_end + 1])
        selected.append("")
    if not selected:
        raise RuntimeError(f"Section selection did not match source headings: {sorted(sections)}")
    return "\n".join(selected).strip()


def mark_master_references(text: str) -> str:
    output: list[str] = []
    active_fence: str | None = None
    for line in text.splitlines():
        current = fence_marker(line)
        if current and (active_fence is None or current == active_fence):
            active_fence = None if active_fence else current
        if active_fence is None:
            line = re.sub(r"\bChapters (?=\d)", "Master Chapters ", line)
            line = re.sub(r"\bChapter (?=\d)", "Master Chapter ", line)
        output.append(line)
    return "\n".join(output)


def orientation_markdown(spec: dict[str, Any], previous: dict[str, Any] | None, next_spec: dict[str, Any] | None) -> str:
    def compact(items: Sequence[str]) -> str:
        return "; ".join(item.rstrip(".").replace("|", "/") for item in items)

    prerequisites = compact(spec["prerequisites"])
    signals = compact(spec["recognition_signals"])
    outcomes = compact(spec["outcomes"])
    fallback = (
        f"{previous['segment_code']} {previous['segment_position']:02d} - {previous['short_title']}"
        if previous
        else f"Book 01 in the {spec['segment_title']} segment"
    )
    return master.ascii_safe(
        f"""# Start Here - Your Route Through This Volume

> **60-second entry rule:** If two or more recognition signals below expose a gap, begin with Chapter 1. If the prerequisites are weak, step back to `{fallback}`. If you can already explain every outcome without notes, jump to Part II and prove it through the practice ladder.

{spec['purpose']}

## Readiness map

| Decision | Evidence |
|---|---|
| **START HERE** | {signals}. |
| **PREPARE FIRST** | {prerequisites}. |
| **MOVE ON WHEN** | {outcomes}. |

## Choose the route that fits today

1. **Learning:** read in order, type the representative Java examples, and complete each dry run.
2. **Revision or rehearsal:** start at the first decision map, invariant, or failure mode you cannot explain; if none expose a gap, go directly to Part II and prove readiness without notes.

# Part I - Core Learning
"""
    )


def handoff_markdown(spec: dict[str, Any], previous: dict[str, Any] | None, next_spec: dict[str, Any] | None) -> str:
    practice = "\n".join(f"- {item}" for item in spec["practice_ladder"])
    previous_line = (
        f"[Previous book: {previous['segment_code']} {previous['segment_position']:02d} - {previous['title']}]({previous['output_name']})"
        if previous
        else f"This is the first volume in the {spec['segment_title']} segment."
    )
    next_line = (
        f"[Next book: {next_spec['segment_code']} {next_spec['segment_position']:02d} - {next_spec['title']}]({next_spec['output_name']})"
        if next_spec
        else f"[Return to the complete series index]({INDEX_NAME})"
    )
    return master.ascii_safe(
        f"""# Part II - Practice and Handoff

## Practice Ladder and Next Steps

## Practice ladder

{practice}

For every coding problem, state the input contract, select the numeric and collection types deliberately, write the invariant before the loop or recursion, test the smallest and largest valid inputs, and close with time and auxiliary-space complexity.

## Navigation

{previous_line}

[Series index]({INDEX_NAME})

{next_line}

## Completion check

- [ ] I can recognize the core patterns without being told the data structure or algorithm.
- [ ] I can implement the representative Java templates from memory.
- [ ] I can explain why each implementation is correct.
- [ ] I can test boundaries, invalid inputs, overflow, and adversarial shapes.
- [ ] I can answer at least one SDE-2 follow-up involving trade-offs or production constraints.
"""
    )


def assemble_volume(
    spec: dict[str, Any],
    previous: dict[str, Any] | None,
    next_spec: dict[str, Any] | None,
    build_dir: Path,
) -> Path:
    pieces = [orientation_markdown(spec, previous, next_spec)]
    for local_number, source_spec in enumerate(spec["sources"], start=1):
        path = safe_source_path(source_spec["path"])
        raw = master.ascii_safe(path.read_text(encoding="utf-8"))
        title = source_spec.get("title") or source_title(raw, path.stem)
        body = extract_sections(raw, source_spec.get("sections"))
        if not source_spec.get("series_native", False):
            body = mark_master_references(body)
        figure = source_spec.get("figure")
        figure_text = ""
        if figure:
            figure_path = safe_source_path(figure["path"])
            relative = figure_path.relative_to(ROOT).as_posix()
            figure_text = f"\n\n![{figure['caption']}]({relative})\n"
        pieces.append(f"# Chapter {local_number} - {title}{figure_text}\n\n{body}".strip())
    companion = spec.get("code_companion")
    if companion:
        path = safe_source_path(companion["path"])
        code = master.ascii_safe(path.read_text(encoding="utf-8")).strip()
        number = len(spec["sources"]) + 1
        title = companion.get("title", "Dependency-Free Java 21 Companion")
        description = companion.get(
            "description",
            "This standalone model compiles without framework dependencies and turns the volume's core contracts into executable assertions.",
        )
        pieces.append(
            f"# Chapter {number} - {title}\n\n{description}\n\n```java\n{code}\n```"
        )
    pieces.append(handoff_markdown(spec, previous, next_spec))
    content = "\n\n".join(pieces).strip() + "\n"
    if re.search(r"\b(?:TODO|TBD|FIXME|placeholder)\b", content, flags=re.IGNORECASE):
        raise RuntimeError(f"Draft marker found while assembling {spec['id']}")
    build_dir.mkdir(parents=True, exist_ok=True)
    output = build_dir / "volume.md"
    output.write_text(content, encoding="utf-8")
    return output


def cover_story(spec: dict[str, Any], styles: dict[str, ParagraphStyle], fonts: dict[str, str]) -> list[Flowable]:
    cover_title = ParagraphStyle(
        "SeriesCoverTitle",
        parent=styles["cover_title"],
        fontSize=28 if len(spec["title"]) > 44 else 31,
        leading=33 if len(spec["title"]) > 44 else 36,
        leftIndent=(CONTENT_W - COVER_W) / 2,
        rightIndent=(CONTENT_W - COVER_W) / 2,
    )
    scope = ParagraphStyle(
        "SeriesCoverScope",
        parent=styles["cover_scope"],
        fontSize=8.2,
        leading=10.5,
    )
    return [
        Spacer(1, 10),
        CoverBand(SERIES_TITLE.upper(), fonts["bold"]),
        Spacer(1, 38),
        Paragraph(spec["volume_label"].upper(), styles["cover_kicker"]),
        Paragraph(html.escape(spec["title"]), cover_title),
        Paragraph(html.escape(spec["subtitle"]), styles["cover_subtitle"]),
        HRFlowable(width="48%", thickness=2, color=GOLD, spaceBefore=4, spaceAfter=15),
        Paragraph("BY", styles["cover_author_label"]),
        Paragraph(AUTHOR, styles["cover_author"]),
        Paragraph(html.escape(spec["cover_deck"]), styles["cover_deck"]),
        Spacer(1, 22),
        HRFlowable(width=COVER_W, thickness=0.8, color=GOLD, spaceBefore=0, spaceAfter=7),
        Paragraph(html.escape(spec["topic_line"]), scope),
        HRFlowable(width=COVER_W, thickness=0.55, color=LINE, spaceBefore=7, spaceAfter=13),
        Paragraph("JAVA 21 | INTERVIEW CORE | SDE-2 FOLLOW-UPS | PRINTABLE EDITION", styles["cover_stat_line"]),
        Spacer(1, 24),
        Paragraph(f"Series edition - Release {spec['release_date']}", styles["cover_meta"]),
        Paragraph("Open educational interview-preparation edition", styles["cover_meta"]),
        PageBreak(),
    ]


def copyright_story(
    spec: dict[str, Any],
    styles: dict[str, ParagraphStyle],
    *,
    leading_spacer: float = 70,
) -> list[Flowable]:
    paragraphs = [
        "Copyright and Publishing Notes",
        f"Copyright 2026 {AUTHOR} and credited contributors. Open educational edition.",
        "Book prose, exercises, diagrams, and PDFs are licensed under Creative Commons Attribution 4.0 International (CC BY 4.0). Build scripts and source code are licensed under MIT. Individual credit is recorded in AUTHORS.md and Git history.",
        "Repository: https://github.com/vinayreddykalluri/SDE2-Interview-Handbook. Attribution does not imply endorsement. Java and related marks are owned by their respective holders.",
        "Examples target Java 21 unless a section states otherwise. Validate release-specific APIs, JVM behavior, security, performance, licensing, and operational requirements before production use.",
        f"Series position: {spec['volume_label']}. The local table of contents and PDF bookmarks navigate within this file; the roadmap and printed filenames navigate across the complete series.",
        "Source provenance: curated from the independent Java SDE-2 master guide plus focused original material. Original master chapter numbers are labeled explicitly when retained in cross-references.",
    ]
    title = Paragraph(paragraphs[0], styles["h1"])
    title._heading_level = 1
    title._bookmark_text = paragraphs[0]
    story: list[Flowable] = [
        Spacer(1, leading_spacer),
        title,
        HRFlowable(width="100%", thickness=1.2, color=GOLD, spaceAfter=18),
    ]
    story.extend(Paragraph(html.escape(text), styles["copyright"]) for text in paragraphs[1:])
    story.append(PageBreak())
    return story


def about_author_story(
    styles: dict[str, ParagraphStyle],
    *,
    page_break: bool = True,
) -> list[Flowable]:
    highlights = [
        "More than eight years building Java and Spring Boot services for high-throughput healthcare and enterprise platforms.",
        "Designed Kafka-based event systems that have processed more than one million events per hour, with explicit idempotency, retry, recovery, and observability controls.",
        "M.S. in Computer Science from the University of Missouri-Kansas City (3.9/4.0) and M.S. in Software Engineering from VIT University (9.3/10), where he was a Gold Medalist.",
        "Independent builder of Work Visa Insights, an immigration-data intelligence platform, and Play Together, a published Android application.",
    ]
    title = Paragraph("About the Author", styles["toc_title"])
    title._heading_level = 1
    title._bookmark_text = "About the Author"
    story: list[Flowable] = [
        title,
        Paragraph("Vinay Reddy Kalluri", styles["h2"]),
        Paragraph(
            "Vinay Reddy Kalluri is a senior Java backend engineer, the creator and founding author of the Java SDE-2 Interview Preparation Series, and its Editor-in-Chief and Chief Auditor. His work spans high-throughput microservices, event-driven architecture, resilient APIs, large-scale data processing, cloud delivery, and production performance across healthcare and enterprise systems.",
            styles["body"],
        ),
        Paragraph("Editorial leadership", styles["h2"]),
        Paragraph(
            "As Editor-in-Chief, Vinay owns the learning sequence, scope, voice, and publication decisions. As Chief Auditor, he owns the Java accuracy standard, validation evidence, attribution review, and release-readiness gate. Individual contributors retain visible credit for accepted original work through AUTHORS.md and the public Git history.",
            styles["body"],
        ),
        Paragraph(
            "What distinguishes his approach is the combination of hands-on system design and operational ownership. He treats timeouts, retries, idempotency, dead-letter recovery, data consistency, SQL behavior, caching, and observability as part of the design rather than afterthoughts. He has also mentored engineers and led modernization, migration, and reliability work across distributed services.",
            styles["body"],
        ),
        Paragraph("Selected engineering and academic highlights", styles["h2"]),
    ]
    for highlight in highlights:
        story.append(Paragraph(f"&#8226;&nbsp;&nbsp;{html.escape(highlight)}", styles["body"]))
    story.extend(
        [
            Paragraph("Why this series exists", styles["h2"]),
            Paragraph(
                "Vinay created this series to turn fragmented interview preparation into a deliberate progression: learn the fundamentals, run the examples, predict behavior, debug failures, explain trade-offs, and only then move into advanced engineering. The books reflect the same principle he applies to production systems: clarity before cleverness, explicit contracts, measurable behavior, and reliable foundations.",
                styles["body"],
            ),
            Paragraph(
                '<b>Connect:</b> <link href="https://www.linkedin.com/in/vinayreddykalluri/" color="#1F5A94">LinkedIn</link> &nbsp;|&nbsp; <link href="https://github.com/vinayreddykalluri" color="#1F5A94">GitHub</link>',
                styles["body"],
            ),
        ]
    )
    if page_break:
        story.append(PageBreak())
    return story


def stage_rows(manifest: dict[str, Any]) -> list[dict[str, str]]:
    """Return the three selectable learning segments for compact summaries."""
    return [dict(item) for item in manifest["segments"]]


def roadmap_story(
    spec: dict[str, Any],
    manifest: dict[str, Any],
    styles: dict[str, ParagraphStyle],
    fonts: dict[str, str],
) -> list[Flowable]:
    head_style = ParagraphStyle(
        "RoadmapHead",
        fontName=fonts["bold"],
        fontSize=8.5,
        leading=10,
        textColor=colors.white,
        alignment=TA_CENTER,
    )
    cell_style = ParagraphStyle(
        "RoadmapCell",
        fontName=fonts["sans"],
        fontSize=7.8,
        leading=9.2,
        textColor=INK,
        spaceAfter=0,
    )
    number_style = ParagraphStyle(
        "RoadmapNumber",
        parent=cell_style,
        fontName=fonts["bold"],
        textColor=NAVY,
        alignment=TA_CENTER,
    )
    rows: list[list[Paragraph]] = [
        [Paragraph("SEGMENT", head_style), Paragraph("BOOK", head_style), Paragraph("FOCUSED LEARNING PATH", head_style)]
    ]
    ordered_books = learning_volumes(manifest)
    for book in ordered_books:
        link = html.escape(book["output_name"], quote=True)
        segment = Paragraph(html.escape(book["segment_short_title"]), cell_style)
        number = Paragraph(f'<link href="{link}" color="#0B2545"><b>{book["segment_code"]} {book["segment_position"]:02d}</b></link>', number_style)
        title = Paragraph(
            f'<link href="{link}" color="#17212B">{html.escape(book["title"])}</link>',
            cell_style,
        )
        rows.append([segment, number, title])
    table = Table(rows, colWidths=[72, 62, CONTENT_W - 134], repeatRows=1, hAlign="LEFT")
    commands: list[tuple[Any, ...]] = [
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("GRID", (0, 0), (-1, -1), 0.35, LINE),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("TOPPADDING", (0, 0), (-1, -1), 4.5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4.5),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
    ]
    for row_index, book in enumerate(ordered_books, start=1):
        if str(book["id"]) == str(spec.get("id", "")):
            commands.append(("BACKGROUND", (0, row_index), (-1, row_index), PALE))
            commands.append(("LINEBEFORE", (0, row_index), (0, row_index), 3, GOLD))
    table.setStyle(TableStyle(commands))
    title = Paragraph("Series Roadmap", styles["toc_title"])
    title._heading_level = 1
    title._bookmark_text = "Series Roadmap"
    return [
        title,
        Paragraph(
            "Choose Java, DSA, or System Design and Backend first, then follow the books inside that segment in order. Stable study-step codes remain on filenames and links; the clearer segment book codes appear on the website and every PDF cover.",
            styles["small"],
        ),
        Spacer(1, 7),
        table,
        Spacer(1, 7),
        Paragraph(
            "Roadmap editions identify planned books whose chapter sets will be expanded one at a time. Existing deep-dive books remain available later in each segment, so no published material is displaced.",
            styles["small"],
        ),
        PageBreak(),
    ]


def toc_story(styles: dict[str, ParagraphStyle], fonts: dict[str, str]) -> list[Flowable]:
    toc_header = ParagraphStyle(
        "SeriesTOCRepeatHeader",
        fontName=fonts["bold"],
        fontSize=7.8,
        leading=9.5,
        textColor=colors.white,
        spaceAfter=0,
    )
    toc = master.GroupedTableOfContents(toc_header)
    toc.levelStyles = [
        ParagraphStyle("SeriesTOCPart", fontName=fonts["bold"], fontSize=9.5, leading=12.2, textColor=NAVY, leftIndent=0, firstLineIndent=0, spaceBefore=4),
        ParagraphStyle("SeriesTOCChapter", fontName=fonts["sans"], fontSize=8.4, leading=10.6, textColor=INK, leftIndent=18, firstLineIndent=0, rightIndent=4, spaceBefore=1),
    ]
    title = Paragraph("Contents", styles["toc_title"])
    title._heading_level = 1
    title._bookmark_text = "Contents and Navigation"
    return [
        title,
        Paragraph("This contents page covers only the current focused PDF. Section-level navigation is available through PDF bookmarks.", styles["small"]),
        Spacer(1, 8),
        toc,
    ]


def local_navigation_story(
    spec: dict[str, Any],
    manifest: dict[str, Any],
    styles: dict[str, ParagraphStyle],
    fonts: dict[str, str],
) -> list[Flowable]:
    """Show the reader exactly where this file sits without a full roadmap."""
    if spec["id"] == "00":
        return []
    volumes = segment_volumes(manifest, spec["segment_id"])
    index = next(i for i, item in enumerate(volumes) if item["id"] == spec["id"])
    previous = volumes[index - 1] if index else None
    next_spec = volumes[index + 1] if index + 1 < len(volumes) else None
    nav_style = ParagraphStyle(
        "LocalNavigation",
        parent=styles["small"],
        fontName=fonts["sans"],
        fontSize=8.2,
        leading=10.5,
        textColor=DARK_BLUE,
        alignment=TA_CENTER,
        spaceAfter=0,
    )
    current_style = ParagraphStyle(
        "LocalNavigationCurrent",
        parent=nav_style,
        fontName=fonts["bold"],
        textColor=NAVY,
    )

    def linked(item: dict[str, Any] | None, fallback: str) -> Paragraph:
        if item is None:
            return Paragraph(fallback, nav_style)
        label = f"{item['segment_code']} {item['segment_position']:02d} - {item['short_title']}"
        return Paragraph(
            f'<link href="{html.escape(item["output_name"], quote=True)}" color="#164E63">{html.escape(label)}</link>',
            nav_style,
        )

    rows = [[
        linked(previous, "START OF SEGMENT"),
        Paragraph(f"YOU ARE HERE<br/>{html.escape(spec['segment_code'])} {spec['segment_position']:02d} - {html.escape(spec['short_title'])}", current_style),
        linked(next_spec, "SEGMENT COMPLETE"),
    ]]
    table = Table(rows, colWidths=[CONTENT_W * 0.27, CONTENT_W * 0.46, CONTENT_W * 0.27])
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (0, 0), LIGHT),
                ("BACKGROUND", (1, 0), (1, 0), master.GUIDE_BG),
                ("BACKGROUND", (2, 0), (2, 0), LIGHT),
                ("BOX", (0, 0), (-1, -1), 0.45, LINE),
                ("INNERGRID", (0, 0), (-1, -1), 0.35, LINE),
                ("LINEABOVE", (1, 0), (1, 0), 2, GOLD),
                ("LEFTPADDING", (0, 0), (-1, -1), 6),
                ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                ("TOPPADDING", (0, 0), (-1, -1), 7),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
            ]
        )
    )
    heading = Paragraph(f"{spec['segment_short_title']} Segment Position", styles["h2"])
    heading._heading_level = 2
    heading._bookmark_text = "Contents and Navigation"
    return [heading, table, Spacer(1, 13)]


def split_front_orientation(blocks: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    """Split immediately before Part I so Start Here can precede the TOC."""
    for index, block in enumerate(blocks):
        if block.get("t") != "Header":
            continue
        level, _, inlines = block["c"]
        if level == 1 and master.plain_inlines(inlines).startswith("Part I"):
            return blocks[:index], blocks[index:]
    return blocks, []


def validate_pdf(path: Path, spec: dict[str, Any]) -> dict[str, Any]:
    reader = PdfReader(path)
    page_count = len(reader.pages)
    if page_count < spec.get("min_pages", 8):
        raise RuntimeError(f"{spec['id']} is unexpectedly short: {page_count} pages")
    if page_count > spec.get("max_pages", 180):
        raise RuntimeError(f"{spec['id']} is unexpectedly long: {page_count} pages")
    metadata = reader.metadata or {}
    if metadata.get("/Author") != AUTHOR:
        raise RuntimeError(f"{spec['id']} PDF author metadata is incorrect")
    if spec["title"] not in str(metadata.get("/Title", "")):
        raise RuntimeError(f"{spec['id']} PDF title metadata is incorrect")
    texts: list[str] = []
    for index, page in enumerate(reader.pages, start=1):
        text = page.extract_text() or ""
        texts.append(text)
        if index > 1:
            compact = re.sub(r"\s+", "", text)
            if len(compact) < 8:
                raise RuntimeError(f"{spec['id']} page {index} appears blank")
    full_text = "\n".join(texts)
    normalized_cover = re.sub(r"\s+", " ", texts[0]).strip()
    if spec["title"] not in normalized_cover or AUTHOR not in normalized_cover:
        raise RuntimeError(f"{spec['id']} cover text is incomplete")
    required_front_matter = ("About the Author", "Series Roadmap", "Contents")
    missing_front_matter = [item for item in required_front_matter if item not in full_text]
    if missing_front_matter:
        raise RuntimeError(
            f"{spec['id']} is missing front matter: {', '.join(missing_front_matter)}"
        )
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    return {"page_count": page_count, "sha256": digest, "bytes": path.stat().st_size}


def build_pdf(
    spec: dict[str, Any],
    manifest: dict[str, Any],
    markdown_path: Path,
    output_path: Path,
    fonts: dict[str, str],
) -> dict[str, Any]:
    pandoc = run(
        ["pandoc", markdown_path, "--from=gfm+pipe_tables+task_lists", "--to=json"],
        capture=True,
    )
    ast = json.loads(pandoc.stdout)
    build_dir = markdown_path.parent
    (build_dir / "book-ast.json").write_text(json.dumps(ast), encoding="utf-8")
    styles = master.make_pdf_styles(fonts)
    styles["series_part"] = ParagraphStyle(
        "SeriesPartBanner",
        parent=styles["h2"],
        fontName=fonts["bold"],
        fontSize=10.5,
        leading=13.5,
        textColor=NAVY,
        backColor=master.GUIDE_BG,
        borderColor=GOLD,
        borderWidth=0.6,
        borderRadius=4,
        borderPadding=(6, 9, 6, 9),
        spaceBefore=0,
        spaceAfter=5,
        keepWithNext=1,
    )
    styles["stage_card"] = ParagraphStyle(
        "IndexStageCard",
        parent=styles["h2"],
        fontName=fonts["bold"],
        fontSize=12.5,
        leading=15.8,
        textColor=NAVY,
        backColor=PALE,
        borderColor=BLUE,
        borderWidth=0.55,
        borderRadius=4,
        borderPadding=(6, 8, 6, 8),
        spaceBefore=11,
        spaceAfter=6,
        keepWithNext=1,
    )
    renderer = IndexPdfRenderer(styles, fonts) if spec["id"] == "00" else SeriesPdfRenderer(styles, fonts)
    orientation_blocks, learning_blocks = split_front_orientation(ast["blocks"])
    story: list[Flowable] = []
    story.extend(cover_story(spec, styles, fonts))
    story.extend(renderer.blocks(orientation_blocks))
    story.append(PageBreak())
    story.extend(local_navigation_story(spec, manifest, styles, fonts))
    story.extend(toc_story(styles, fonts))
    story.extend(renderer.blocks(learning_blocks))
    story.append(PageBreak())
    story.extend(roadmap_story(spec, manifest, styles, fonts))
    if spec["id"] == "00":
        # Editorial and licensing details deserve complete, independently
        # navigable pages in the public index.
        story.extend(about_author_story(styles))
        story.extend(copyright_story(spec, styles, leading_spacer=70))
    else:
        story.extend(about_author_story(styles))
        story.extend(copyright_story(spec, styles))
    output_path.parent.mkdir(parents=True, exist_ok=True)
    doc = SeriesDocTemplate(str(output_path), styles, spec)
    doc.multiBuild(story, canvasmaker=invariant_canvas)
    return validate_pdf(output_path, spec)


def index_markdown(manifest: dict[str, Any]) -> str:
    lines = [
        "# Choose Your Learning Segment",
        "",
        "This library has three independent starting points: Java Engineering, Data Structures and Algorithms, and System Design and Backend. Choose the segment that matches your immediate goal, then follow its numbered books in order. You can study a second segment in parallel after its prerequisites are comfortable.",
        "",
        "Keep the PDFs together in the same directory so relative links can work in viewers that permit local-file navigation. The printed filenames remain the fallback when a viewer blocks those links.",
        "",
        "## Choose your starting point in 60 seconds",
        "",
        "> **Start rule:** select one segment first. Inside it, begin with Book 01 unless you can pass that book's readiness checks without notes.",
        "",
        "| Your goal | Start here | Continue with |",
        "|---|---|---|",
        "| Learn or rebuild Java | JAVA 01 - Java Foundations | Continue through the Java segment in order |",
        "| Build interview problem-solving skill | DSA 01 - Time and Space Complexity | Continue through the DSA segment in order |",
        "| Prepare for Java backend and design rounds | SD 01 - Backend and Design Foundations | Continue through databases, Spring, messaging, and distributed systems |",
        "",
        "## The learning loop for every volume",
        "",
        "1. **Recognize:** use the entry signals and decision maps to identify the pattern.",
        "2. **Understand:** restate the contract, invariant, and failure modes in your own words.",
        "3. **Implement:** type the Java example and test boundaries before looking back.",
        "4. **Explain:** give the complexity, trade-offs, and one production follow-up aloud.",
        "5. **Prove readiness:** pass the completion check before opening the next volume.",
        "",
    ]
    for part_number, segment in enumerate(manifest["segments"], start=1):
        lines.extend(
            [
                f"# Part {part_number} - {segment['title']}",
                "",
                segment["description"],
                "",
            ]
        )
        for item in segment_volumes(manifest, segment["id"]):
            status = " - Roadmap Edition" if item.get("publication_status") == "planned" else ""
            lines.extend(
                [
                    f"## {segment['code']} {item['segment_position']:02d} - {item['title']}{status}",
                    "",
                    item["purpose"],
                    "",
                    f"Open [{item['output_name']}]({item['output_name']}).",
                    "",
                ]
            )
    lines.extend(
        [
            "# Part IV - Recommended Study Rhythm",
            "",
            "# Build, Practice, Explain, Revisit",
            "",
            "For each volume, complete one learning pass, one no-notes implementation pass, one spoken explanation pass, and one delayed revision pass. Keep an error log organized by contract, invariant, numeric safety, data-structure choice, complexity, and test coverage. A roadmap edition establishes ordering and scope; return as its chapter set expands rather than treating it as finished instruction.",
        ]
    )
    return master.ascii_safe("\n".join(lines).strip() + "\n")


def build_index(manifest: dict[str, Any], fonts: dict[str, str]) -> dict[str, Any]:
    spec = {
        "id": "00",
        "stage": "00",
        "title": "Java SDE-2 Interview Preparation Series Index",
        "short_title": "Series Index",
        "subtitle": "A Basics-to-Advanced Navigation Guide",
        "output_name": INDEX_NAME,
        "volume_label": "Series Index - 3 Segments / 40 Books",
        "release_date": manifest["release_date"],
        "cover_deck": "Choose Java, DSA, or System Design and Backend, then follow a clear prerequisite-aware path within the segment.",
        "topic_line": "JAVA ENGINEERING | DSA | SYSTEM DESIGN AND BACKEND",
        "min_pages": 8,
        "max_pages": 50,
    }
    build_dir = BUILD / "00-series-index"
    build_dir.mkdir(parents=True, exist_ok=True)
    markdown = build_dir / "index.md"
    markdown.write_text(index_markdown(manifest), encoding="utf-8")
    return build_pdf(spec, manifest, markdown, DIST / INDEX_NAME, fonts)


def select_volumes(manifest: dict[str, Any], requested: str | None) -> list[dict[str, Any]]:
    volumes = manifest["volumes"]
    if requested is None:
        return volumes
    matches = [item for item in volumes if item["id"].casefold() == requested.casefold()]
    if not matches:
        raise RuntimeError(f"Unknown physical volume ID: {requested}")
    return matches


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--volume", help="build one physical volume, such as 01 or 18C")
    parser.add_argument("--index-only", action="store_true", help="rebuild the index and refresh metadata for existing focused PDFs")
    parser.add_argument("--skip-index", action="store_true")
    args = parser.parse_args()
    if args.index_only and (args.volume or args.skip_index):
        parser.error("--index-only cannot be combined with --volume or --skip-index")

    manifest = load_manifest()
    BUILD.mkdir(parents=True, exist_ok=True)
    DIST.mkdir(parents=True, exist_ok=True)
    TMP.mkdir(parents=True, exist_ok=True)
    fonts = master.register_fonts()
    all_volumes = learning_volumes(manifest)
    selected = [] if args.index_only else select_volumes(manifest, args.volume)
    results: list[dict[str, Any]] = []

    for spec in selected:
        local_volumes = segment_volumes(manifest, spec["segment_id"])
        index = local_volumes.index(spec)
        previous = local_volumes[index - 1] if index > 0 else None
        next_spec = local_volumes[index + 1] if index + 1 < len(local_volumes) else None
        build_dir = BUILD / f"{spec['id']}-{spec['slug']}"
        markdown = assemble_volume(spec, previous, next_spec, build_dir)
        temporary = TMP / f"{spec['output_name']}.tmp.pdf"
        result = build_pdf(spec, manifest, markdown, temporary, fonts)
        final_path = DIST / spec["output_name"]
        shutil.copy2(temporary, final_path)
        result.update({"id": spec["id"], "stage": spec["stage"], "path_label": spec["path_label"], "book_position": spec["book_position"], "segment_id": spec["segment_id"], "segment_code": spec["segment_code"], "segment_position": spec["segment_position"], "publication_status": spec.get("publication_status", "published"), "title": spec["title"], "file": spec["output_name"]})
        results.append(result)
        print(f"{spec['id']}: {final_path} ({result['page_count']} pages)")

    if args.index_only:
        for spec in all_volumes:
            result = validate_pdf(DIST / spec["output_name"], spec)
            result.update({"id": spec["id"], "stage": spec["stage"], "path_label": spec["path_label"], "book_position": spec["book_position"], "segment_id": spec["segment_id"], "segment_code": spec["segment_code"], "segment_position": spec["segment_position"], "publication_status": spec.get("publication_status", "published"), "title": spec["title"], "file": spec["output_name"]})
            results.append(result)

    index_result: dict[str, Any] | None = None
    if not args.skip_index and args.volume is None:
        index_result = build_index(manifest, fonts)
        print(f"INDEX: {DIST / INDEX_NAME} ({index_result['page_count']} pages)")

    report_path = DIST / "manifest.json"
    existing: dict[str, Any] = {}
    if report_path.exists():
        existing = json.loads(report_path.read_text(encoding="utf-8"))
    by_id = {item["id"]: item for item in existing.get("volumes", [])}
    by_id.update({item["id"]: item for item in results})
    report = {
        "series": SERIES_TITLE,
        "author": AUTHOR,
        "release_date": manifest["release_date"],
        "public_segments": len(manifest["segments"]),
        "physical_volumes": len(all_volumes),
        "volumes": [by_id[item["id"]] for item in all_volumes if item["id"] in by_id],
    }
    if index_result:
        report["index"] = {"file": INDEX_NAME, **index_result}
    elif "index" in existing:
        report["index"] = existing["index"]
    report_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
