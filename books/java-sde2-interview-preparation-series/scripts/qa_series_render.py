#!/usr/bin/env python3
"""Render every focused PDF, aggregate coarse checks, and make review sheets."""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps


ROOT = Path(__file__).resolve().parents[1]
SERIES = ROOT / "series"
DIST = SERIES / "dist"
SPEC = SERIES / "series.json"
INDEX_NAME = "Java-SDE2-Interview-Preparation-Series-Index.pdf"


def page_images(output: Path) -> list[Path]:
    return sorted(
        (output / "pages").glob("page-*.jpg"),
        key=lambda path: int(path.stem.rsplit("-", 1)[1]),
    )


def sample_pages(images: list[Path]) -> list[Path]:
    indexes = {0, 2, 3, 4, len(images) // 2, len(images) - 1}
    return [images[index] for index in sorted(indexes) if 0 <= index < len(images)]


def make_review_sheets(
    rendered: list[tuple[str, str, list[Path]]], output: Path
) -> list[Path]:
    font = ImageFont.load_default(size=13)
    title_font = ImageFont.load_default(size=15)
    thumb_width, thumb_height = 230, 298
    label_height = 42
    columns = 6
    documents_per_sheet = 5
    paths: list[Path] = []
    for sheet_number, start in enumerate(
        range(0, len(rendered), documents_per_sheet), start=1
    ):
        batch = rendered[start : start + documents_per_sheet]
        sheet = Image.new(
            "RGB",
            (columns * thumb_width, len(batch) * (thumb_height + label_height)),
            "#d8dee6",
        )
        draw = ImageDraw.Draw(sheet)
        for row, (volume_id, title, images) in enumerate(batch):
            samples = sample_pages(images)
            y = row * (thumb_height + label_height)
            for column, path in enumerate(samples):
                x = column * thumb_width
                with Image.open(path) as source:
                    thumbnail = ImageOps.contain(
                        ImageOps.exif_transpose(source).convert("RGB"),
                        (thumb_width - 6, thumb_height - 6),
                    )
                page = int(path.stem.rsplit("-", 1)[1])
                sheet.paste(
                    thumbnail,
                    (x + (thumb_width - thumbnail.width) // 2, y + 3),
                )
                draw.rectangle(
                    (x, y + thumb_height, x + thumb_width - 1, y + thumb_height + label_height - 1),
                    fill="#0b2545",
                )
                label = f"{volume_id} | p{page}"
                draw.text((x + 7, y + thumb_height + 4), label, font=title_font, fill="white")
                if column == 0:
                    draw.text(
                        (x + 7, y + thumb_height + 22),
                        title[:34],
                        font=font,
                        fill="#dce8f5",
                    )
        target = output / f"series-review-{sheet_number:02d}.jpg"
        sheet.save(target, "JPEG", quality=84, optimize=True)
        paths.append(target)
    return paths


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--dpi", type=int, default=54)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)

    spec = json.loads(SPEC.read_text(encoding="utf-8"))
    documents = [
        (volume["id"], volume["title"], volume["output_name"])
        for volume in spec["volumes"]
    ]
    documents.append(("INDEX", "Series Index", INDEX_NAME))

    reports: list[dict[str, object]] = []
    rendered: list[tuple[str, str, list[Path]]] = []
    for volume_id, title, filename in documents:
        destination = output / volume_id
        result = subprocess.run(
            [
                sys.executable,
                str(ROOT / "scripts" / "qa_render.py"),
                str(DIST / filename),
                "--output",
                str(destination),
                "--dpi",
                str(args.dpi),
                "--columns",
                "4",
                "--rows",
                "5",
            ],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )
        if result.returncode:
            raise RuntimeError(
                f"Render QA failed for {volume_id}:\n{result.stdout}{result.stderr}"
            )
        report = json.loads((destination / "qa-report.json").read_text(encoding="utf-8"))
        report["id"] = volume_id
        report["title"] = title
        reports.append(report)
        images = page_images(destination)
        rendered.append((volume_id, title, images))
        print(f"Rendered {volume_id}: {len(images)} pages")

    problems = {
        "blank": {item["id"]: item["blank_pages"] for item in reports if item["blank_pages"]},
        "edge": {item["id"]: item["edge_pages"] for item in reports if item["edge_pages"]},
        "dark": {item["id"]: item["dark_pages"] for item in reports if item["dark_pages"]},
    }
    if any(problems.values()):
        raise RuntimeError(f"Aggregate render QA found candidates: {problems}")

    sheets = make_review_sheets(rendered, output)
    summary = {
        "documents": len(reports),
        "pages": sum(int(item["pages"]) for item in reports),
        "dpi": args.dpi,
        "review_sheets": [str(path) for path in sheets],
        "problems": problems,
        "reports": [
            {
                "id": item["id"],
                "title": item["title"],
                "pages": item["pages"],
                "report": str(output / str(item["id"]) / "qa-report.json"),
            }
            for item in reports
        ],
    }
    (output / "qa-summary.json").write_text(
        json.dumps(summary, indent=2) + "\n", encoding="utf-8"
    )
    print(f"Render QA passed: {summary['documents']} PDFs, {summary['pages']} pages")
    print(f"Review sheets: {len(sheets)}")


if __name__ == "__main__":
    main()
