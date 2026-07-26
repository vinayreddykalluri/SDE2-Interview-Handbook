#!/usr/bin/env python3
"""Create the deterministic DOCX reference file for the book build."""

from __future__ import annotations

from pathlib import Path
import shutil
import subprocess

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.style import WD_STYLE_TYPE
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "build" / "reference.docx"

BLUE = RGBColor(0x2E, 0x74, 0xB5)
DARK_BLUE = RGBColor(0x1F, 0x4D, 0x78)
NAVY = RGBColor(0x0B, 0x25, 0x45)
MUTED = RGBColor(0x52, 0x60, 0x6D)
LIGHT = "E8EEF5"
CODE_FILL = "F2F4F7"


def set_font(style, name: str, size: float, color: RGBColor | None = None, bold: bool | None = None, italic: bool | None = None) -> None:
    style.font.name = name
    style.font.size = Pt(size)
    rfonts = style._element.get_or_add_rPr().get_or_add_rFonts()
    for attribute in ("asciiTheme", "hAnsiTheme", "eastAsiaTheme", "cstheme"):
        rfonts.attrib.pop(qn(f"w:{attribute}"), None)
    for attribute in ("ascii", "hAnsi", "eastAsia", "cs"):
        rfonts.set(qn(f"w:{attribute}"), name)
    if color is not None:
        style.font.color.rgb = color
    if bold is not None:
        style.font.bold = bold
    if italic is not None:
        style.font.italic = italic


def set_spacing(style, before: float, after: float, line: float, keep_next: bool = False, page_break_before: bool = False) -> None:
    fmt = style.paragraph_format
    fmt.space_before = Pt(before)
    fmt.space_after = Pt(after)
    fmt.line_spacing = line
    fmt.keep_with_next = keep_next
    fmt.page_break_before = page_break_before


def shade_style(style, fill: str) -> None:
    ppr = style._element.get_or_add_pPr()
    shd = ppr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        ppr.append(shd)
    shd.set(qn("w:fill"), fill)


def paragraph_border(style, color: str = "9FB3C8", width: str = "8") -> None:
    ppr = style._element.get_or_add_pPr()
    borders = ppr.find(qn("w:pBdr"))
    if borders is None:
        borders = OxmlElement("w:pBdr")
        ppr.append(borders)
    bottom = OxmlElement("w:bottom")
    bottom.set(qn("w:val"), "single")
    bottom.set(qn("w:sz"), width)
    bottom.set(qn("w:space"), "5")
    bottom.set(qn("w:color"), color)
    borders.append(bottom)


def ensure_style(doc: Document, name: str, base: str = "Normal"):
    try:
        style = doc.styles[name]
    except KeyError:
        style = doc.styles.add_style(name, WD_STYLE_TYPE.PARAGRAPH)
    style.base_style = doc.styles[base]
    return style


def configure_styles(doc: Document) -> None:
    normal = doc.styles["Normal"]
    set_font(normal, "Calibri", 11, RGBColor(0x17, 0x21, 0x2B))
    set_spacing(normal, 0, 6, 1.25)
    normal.paragraph_format.widow_control = True

    title = doc.styles["Title"]
    set_font(title, "Arial", 30, NAVY, bold=True)
    set_spacing(title, 0, 8, 1.0, keep_next=True)
    title.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER

    subtitle = doc.styles["Subtitle"]
    set_font(subtitle, "Arial", 15, DARK_BLUE, italic=False)
    set_spacing(subtitle, 0, 8, 1.1, keep_next=True)
    subtitle.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER

    author = doc.styles["Author"]
    set_font(author, "Arial", 14, NAVY, bold=True)
    set_spacing(author, 0, 12, 1.0, keep_next=True)
    author.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER

    date = doc.styles["Date"]
    set_font(date, "Arial", 9, MUTED)
    set_spacing(date, 12, 2, 1.0, keep_next=True)
    date.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER

    for name, size, color, before, after in (
        ("Heading 1", 16, BLUE, 18, 10),
        ("Heading 2", 13, BLUE, 14, 7),
        ("Heading 3", 12, DARK_BLUE, 10, 5),
        ("Heading 4", 11, DARK_BLUE, 8, 4),
    ):
        style = doc.styles[name]
        set_font(style, "Calibri", size, color, bold=True)
        set_spacing(style, before, after, 1.1, keep_next=True, page_break_before=(name == "Heading 1"))
        style.paragraph_format.keep_together = True

    part = ensure_style(doc, "Book Part")
    set_font(part, "Calibri", 25, NAVY, bold=True)
    set_spacing(part, 120, 18, 1.0, keep_next=True, page_break_before=True)
    part.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
    ppr = part._element.get_or_add_pPr()
    outline = OxmlElement("w:outlineLvl")
    outline.set(qn("w:val"), "0")
    ppr.append(outline)

    kicker = ensure_style(doc, "Book Kicker")
    set_font(kicker, "Calibri", 10, RGBColor(0xC5, 0x8A, 0x22), bold=True)
    set_spacing(kicker, 0, 18, 1.0, keep_next=True)
    kicker.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER

    code = ensure_style(doc, "Source Code")
    set_font(code, "Consolas", 8.5, RGBColor(0x17, 0x21, 0x2B))
    set_spacing(code, 4, 7, 1.0)
    code.paragraph_format.left_indent = Inches(0.15)
    code.paragraph_format.right_indent = Inches(0.15)
    shade_style(code, CODE_FILL)
    paragraph_border(code, "D5DCE4", "4")

    block = ensure_style(doc, "Block Text")
    set_font(block, "Calibri", 10.5, DARK_BLUE)
    set_spacing(block, 6, 8, 1.2)
    block.paragraph_format.left_indent = Inches(0.25)
    block.paragraph_format.right_indent = Inches(0.15)
    shade_style(block, "F4F6F9")

    # Select by display name explicitly; python-docx warns when a style-id
    # fallback happens to share the same spelling.
    caption = next(style for style in doc.styles if style.name == "Caption")
    set_font(caption, "Calibri", 9, MUTED, italic=True)
    set_spacing(caption, 3, 9, 1.0, keep_next=False)
    caption.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER

    for name in ("List Bullet", "List Number"):
        # Pandoc's default reference uses direct numbering properties rather
        # than named list paragraph styles, so create these only as optional
        # convenience styles for downstream Word editing.
        style = ensure_style(doc, name)
        set_font(style, "Calibri", 11, RGBColor(0x17, 0x21, 0x2B))
        set_spacing(style, 0, 4, 1.25)
        style.paragraph_format.left_indent = Inches(0.375)
        style.paragraph_format.first_line_indent = Inches(-0.188)

    # Pandoc's DOCX writer uses these built-in reference styles for table-cell
    # paragraphs and table geometry.  Starting from Pandoc's own reference file
    # (see main) keeps their numbering and OOXML definitions intact.
    compact = doc.styles["Compact"]
    set_font(compact, "Calibri", 9.5, RGBColor(0x17, 0x21, 0x2B))
    set_spacing(compact, 0, 2, 1.05)

    table = doc.styles["Table"]
    set_font(table, "Calibri", 9.5, RGBColor(0x17, 0x21, 0x2B))

    table_text = ensure_style(doc, "Table Text")
    set_font(table_text, "Calibri", 9.5, RGBColor(0x17, 0x21, 0x2B))
    set_spacing(table_text, 0, 2, 1.1)

    interview = ensure_style(doc, "Interview Question")
    set_font(interview, "Calibri", 10.5, NAVY, bold=True)
    set_spacing(interview, 8, 4, 1.15, keep_next=True)
    shade_style(interview, LIGHT)

    small = ensure_style(doc, "Book Small")
    set_font(small, "Calibri", 9, MUTED)
    set_spacing(small, 0, 4, 1.1)


def configure_page(doc: Document) -> None:
    section = doc.sections[0]
    section.page_width = Inches(8.5)
    section.page_height = Inches(11)
    section.top_margin = Inches(1)
    section.right_margin = Inches(1)
    section.bottom_margin = Inches(1)
    section.left_margin = Inches(1)
    section.header_distance = Inches(0.492)
    section.footer_distance = Inches(0.492)
    section.different_first_page_header_footer = True


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    if shutil.which("pandoc") is None:
        raise RuntimeError("pandoc is required to create the DOCX reference file")

    # A hand-created python-docx file omits Pandoc-specific styles such as
    # Compact and Table.  Pandoc will still reference those missing style IDs,
    # which LibreOffice can misrender as empty multi-page tables.  Use Pandoc's
    # complete default reference as the base and customize it in place.
    subprocess.run(
        ["pandoc", "-o", str(OUT), "--print-default-data-file", "reference.docx"],
        check=True,
    )
    doc = Document(OUT)
    configure_page(doc)
    configure_styles(doc)
    doc.core_properties.title = "Java Foundations to Advanced Engineering"
    doc.core_properties.subject = "A Complete SDE-2 Interview Preparation Guide"
    doc.core_properties.author = "Vinay Reddy Kalluri"
    doc.core_properties.keywords = "Java, JVM, SDE-2, interview, concurrency, collections"

    doc.save(OUT)
    print(OUT)


if __name__ == "__main__":
    main()
