#!/usr/bin/env python3
"""Create a reader-facing PDF library without moving stable release artifacts."""

from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SPEC_PATH = ROOT / "publishing" / "series.json"
DIST = ROOT / "dist"
DEFAULT_OUTPUT = ROOT / "output" / "reader-library"
INDEX_NAME = "Java-SDE2-Interview-Preparation-Series-Index.pdf"

def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT,
                        help="organized library directory (default: output/reader-library)")
    parser.add_argument("--check", action="store_true",
                        help="validate sources and grouping without copying files")
    return parser.parse_args()


def load_spec() -> dict:
    return json.loads(SPEC_PATH.read_text(encoding="utf-8"))


def validate(spec: dict) -> list[dict]:
    volumes = {volume["id"]: volume for volume in spec["volumes"]}
    order = spec["learning_order"]
    if set(order) != set(volumes):
        missing = sorted(set(volumes) - set(order))
        extra = sorted(set(order) - set(volumes))
        raise ValueError(f"learning_order mismatch: missing={missing}, extra={extra}")

    assignments: list[dict] = []
    seen_outputs: set[str] = set()
    path_labels = spec.get("path_labels", {})
    if set(path_labels) != set(volumes) or len(set(path_labels.values())) != len(volumes):
        raise ValueError("path_labels must define one unique public study code per book")
    segment_by_book: dict[str, tuple[dict, int]] = {}
    for segment_number, segment in enumerate(spec["segments"], start=1):
        for segment_position, volume_id in enumerate(segment["books"], start=1):
            if volume_id in segment_by_book:
                raise ValueError(f"volume {volume_id} is assigned to multiple segments")
            segment_by_book[volume_id] = (segment, segment_position)
    if set(segment_by_book) != set(volumes):
        raise ValueError("segments must assign every focused volume exactly once")

    for ordinal, volume_id in enumerate(order, start=1):
        volume = volumes[volume_id]
        path_label = str(path_labels[volume_id])
        source = DIST / volume["output_name"]
        if not source.is_file():
            raise FileNotFoundError(f"missing focused PDF: {source}")
        if volume["output_name"] in seen_outputs:
            raise ValueError(f"duplicate output name: {volume['output_name']}")
        seen_outputs.add(volume["output_name"])
        segment, segment_position = segment_by_book[volume_id]
        segment_number = spec["segments"].index(segment) + 1
        directory = f"{segment_number:02d}-{segment['id']}"
        group_title = f"{segment['title']} - {len(segment['books'])} Books"
        assignments.append({
            "ordinal": ordinal,
            "path_label": path_label,
            "volume": volume,
            "source": source,
            "directory": directory,
            "group_title": group_title,
            "segment": segment,
            "segment_position": segment_position,
            "reader_name": f"{segment['code']}-{segment_position:02d}-{volume['output_name']}",
        })

    expected = len(spec["volumes"])
    if len(assignments) != expected:
        raise ValueError(f"expected {expected} focused PDFs, found {len(assignments)}")
    for filename in (INDEX_NAME, spec["master_artifact"]["file"]):
        if not (DIST / filename).is_file():
            raise FileNotFoundError(f"missing reference PDF: {DIST / filename}")
    return assignments


def write_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def build_readme(spec: dict, assignments: list[dict]) -> str:
    lines = [
        "# Organized Java SDE-2 PDF Library",
        "",
        "Choose Java, DSA, or System Design and Backend, then read the books inside",
        "that segment in order. Segment codes are shared by the website, PDF cover, and this folder.",
        "",
        "This directory is generated from `publishing/series.json`. Canonical reviewed",
        "artifacts remain in `dist/`, so repository links and release filenames do not break.",
        "",
        "## Start here",
        "",
        f"- [Series index](00-start-here/{INDEX_NAME})",
        f"- [Complete {spec['master_artifact']['page_count']}-page master](00-start-here/{spec['master_artifact']['file']})",
        "",
    ]
    current_group = None
    for item in assignments:
        if item["directory"] != current_group:
            current_group = item["directory"]
            lines.extend([f"## {item['group_title']}", ""])
        volume = item["volume"]
        lines.append(
            f"- **{item['segment']['code']} {item['segment_position']:02d}** - [{volume['title']}]"
            f"({item['directory']}/{item['reader_name']})"
        )
    lines.extend([
        "",
        "## Regenerate",
        "",
        "```bash",
        "python3 scripts/organize_pdf_library.py",
        "```",
        "",
    ])
    return "\n".join(lines)


def copy_library(spec: dict, assignments: list[dict], output: Path) -> None:
    output = output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    directories = {"00-start-here"} | {item["directory"] for item in assignments}
    for directory in sorted(directories):
        (output / directory).mkdir(parents=True, exist_ok=True)

    reference_files = (INDEX_NAME, spec["master_artifact"]["file"])
    for filename in reference_files:
        source = DIST / filename
        if not source.is_file():
            raise FileNotFoundError(f"missing reference PDF: {source}")
        shutil.copy2(source, output / "00-start-here" / filename)

    for item in assignments:
        shutil.copy2(item["source"], output / item["directory"] / item["reader_name"])

    write_text(output / "README.md", build_readme(spec, assignments))


def main() -> None:
    args = arguments()
    spec = load_spec()
    assignments = validate(spec)
    if args.check:
        print(f"PASS {len(assignments)} focused PDFs assigned once in learning order")
        print("PASS 2 reference PDFs present")
        return
    copy_library(spec, assignments, args.output)
    print(f"organized {len(assignments) + 2} PDFs under {args.output.resolve()}")
    print("stable canonical artifacts remain unchanged in dist/")


if __name__ == "__main__":
    main()
