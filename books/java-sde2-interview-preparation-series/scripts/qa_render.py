#!/usr/bin/env python3
"""Render every PDF page, run coarse layout checks, and build contact sheets."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps
from pypdf import PdfReader


RUNNING_TEXT = {
    "java foundations to advanced engineering",
    "java 21 baseline",
    "appendices",
}


def substantive_characters(text: str) -> int:
    """Count text after removing the book's running header and footer lines."""
    body: list[str] = []
    for raw in text.splitlines():
        line = raw.strip()
        lowered = line.casefold()
        if not line or lowered in RUNNING_TEXT or line.isdigit():
            continue
        if re.fullmatch(r"Part [IVXLCDM]+ - .+", line):
            continue
        body.append(line)
    return len(re.sub(r"\s+", "", " ".join(body)))


def page_number(path: Path) -> int:
    match = re.search(r"(\d+)$", path.stem)
    if not match:
        raise ValueError(f"cannot identify page number: {path}")
    return int(match.group(1))


def content_bbox(image: Image.Image) -> tuple[int, int, int, int] | None:
    gray = image.convert("L")
    ink = gray.point(lambda value: 255 if value < 244 else 0)
    return ink.getbbox()


def analyze(images: list[Path], texts: list[str]) -> list[dict[str, object]]:
    records: list[dict[str, object]] = []
    for index, path in enumerate(images):
        with Image.open(path) as source:
            image = source.convert("RGB")
            gray = image.convert("L")
            histogram = gray.histogram()
            pixels = image.width * image.height
            dark = sum(histogram[:80]) / pixels
            nonwhite = sum(histogram[:245]) / pixels
            bbox = content_bbox(image)
            edge = False
            if bbox:
                left, top, right, bottom = bbox
                # The cover deliberately uses full-bleed navy and gold rules.
                # Treat edge contact as suspicious only on interior pages.
                edge = index != 0 and (
                    left <= 3
                    or top <= 3
                    or right >= image.width - 3
                    or bottom >= image.height - 3
                )
            raw_text = texts[index] if index < len(texts) else ""
            text_chars = len(re.sub(r"\s+", "", raw_text))
            body_chars = substantive_characters(raw_text)
            records.append(
                {
                    "page": index + 1,
                    "image": path.name,
                    "width": image.width,
                    "height": image.height,
                    "content_bbox": bbox,
                    "nonwhite_fraction": round(nonwhite, 6),
                    "dark_fraction": round(dark, 6),
                    "text_characters": text_chars,
                    "substantive_text_characters": body_chars,
                    "appears_blank": body_chars < 8,
                    "content_touches_edge": edge,
                    "unusually_dark": dark > 0.35,
                }
            )
    return records


def make_contact_sheets(
    images: list[Path],
    output: Path,
    columns: int,
    rows: int,
) -> list[Path]:
    if not images:
        return []
    per_sheet = columns * rows
    label_height = 24
    with Image.open(images[0]) as sample:
        page_width, page_height = sample.size
    font = ImageFont.load_default(size=16)
    sheets: list[Path] = []
    for start in range(0, len(images), per_sheet):
        batch = images[start : start + per_sheet]
        sheet = Image.new(
            "RGB",
            (columns * page_width, rows * (page_height + label_height)),
            "#d8dee6",
        )
        draw = ImageDraw.Draw(sheet)
        for offset, path in enumerate(batch):
            row, column = divmod(offset, columns)
            x = column * page_width
            y = row * (page_height + label_height)
            number = page_number(path)
            draw.rectangle((x, y, x + page_width - 1, y + label_height - 1), fill="#0b2545")
            draw.text((x + 8, y + 3), f"Page {number}", font=font, fill="white")
            with Image.open(path) as page:
                normalized = ImageOps.exif_transpose(page).convert("RGB")
                sheet.paste(normalized, (x, y + label_height))
        end = min(start + per_sheet, len(images))
        target = output / "contact-sheets" / f"pages-{start + 1:04d}-{end:04d}.jpg"
        target.parent.mkdir(parents=True, exist_ok=True)
        sheet.save(target, "JPEG", quality=78, optimize=True)
        sheets.append(target)
    return sheets


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("pdf", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--dpi", type=int, default=72)
    parser.add_argument("--columns", type=int, default=4)
    parser.add_argument("--rows", type=int, default=4)
    args = parser.parse_args()

    pdf = args.pdf.resolve()
    output = args.output.resolve()
    pages_dir = output / "pages"
    pages_dir.mkdir(parents=True, exist_ok=True)
    if not pdf.exists():
        raise SystemExit(f"missing PDF: {pdf}")

    subprocess.run(
        [
            "pdftoppm",
            "-jpeg",
            "-r",
            str(args.dpi),
            "-jpegopt",
            "quality=72,optimize=y",
            str(pdf),
            str(pages_dir / "page"),
        ],
        check=True,
    )
    images = sorted(pages_dir.glob("page-*.jpg"), key=page_number)
    reader = PdfReader(pdf)
    texts = [(page.extract_text() or "") for page in reader.pages]
    if len(images) != len(reader.pages):
        raise SystemExit(f"rendered {len(images)} images for {len(reader.pages)} PDF pages")

    records = analyze(images, texts)
    sheets = make_contact_sheets(images, output, args.columns, args.rows)
    report = {
        "pdf": str(pdf),
        "pages": len(images),
        "contact_sheets": [str(path) for path in sheets],
        "blank_pages": [record["page"] for record in records if record["appears_blank"]],
        "edge_pages": [record["page"] for record in records if record["content_touches_edge"]],
        "dark_pages": [record["page"] for record in records if record["unusually_dark"]],
        "records": records,
    }
    (output / "qa-report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(f"Rendered pages: {len(images)}")
    print(f"Contact sheets: {len(sheets)}")
    print(f"Blank candidates: {report['blank_pages']}")
    print(f"Edge candidates: {report['edge_pages']}")
    print(f"Dark candidates: {report['dark_pages']}")
    return 1 if report["blank_pages"] or report["edge_pages"] or report["dark_pages"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
