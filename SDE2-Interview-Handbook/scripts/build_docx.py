#!/usr/bin/env python3
from __future__ import annotations

import argparse
import subprocess
import shutil
from pathlib import Path
from typing import List

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
OUTPUT = ROOT / "output"
VOLUME_DIRS = sorted((DOCS / d for d in DOCS.glob("volume-*")), key=lambda p: p.name)


def run(cmd: List[str]) -> None:
    subprocess.run(cmd, check=True)


def collect_markdown(volume_dir: Path) -> List[Path]:
    return sorted([p for p in volume_dir.glob("*.md")], key=lambda p: p.name)


def build_volume_docx(volume_dir: Path) -> Path:
    files = collect_markdown(volume_dir)
    if not files:
        raise RuntimeError(f"No markdown in {volume_dir}")
    out = OUTPUT / "docx" / f"{volume_dir.name.replace('volume-','Volume-').upper()}.docx"
    out.parent.mkdir(parents=True, exist_ok=True)

    cmd = [
        "pandoc",
        *map(str, files),
        "--from=gfm",
        "-s",
    ]

    if (ROOT / "templates" / "reference.docx").exists():
        cmd += ["--reference-doc", str(ROOT / "templates" / "reference.docx")]
    cmd += ["-o", str(out)]
    run(cmd)
    return out


def build_combined_docx() -> Path:
    all_files = []
    for v in VOLUME_DIRS:
        files = collect_markdown(v)
        all_files.extend(files)
    if not all_files:
        raise RuntimeError("No markdown files for combined docx")
    out = OUTPUT / "combined" / "SDE2-Interview-Handbook.docx"
    out.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        "pandoc",
        *map(str, all_files),
        "--from=gfm",
        "-s",
        "-o",
        str(out),
    ]
    if (ROOT / "templates" / "reference.docx").exists():
        cmd.insert(len(cmd)-2, "--reference-doc")
        cmd.insert(len(cmd)-2, str(ROOT / "templates" / "reference.docx"))
    run(cmd)
    return out


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--volume", choices=[v.name for v in VOLUME_DIRS])
    return parser.parse_args()


def main() -> None:
    if shutil.which("pandoc") is None:
        raise RuntimeError("pandoc is required to build DOCX. Install pandoc and retry.")
    args = parse_args()
    if args.volume:
        build_volume_docx(ROOT / "docs" / args.volume)
    else:
        for v in VOLUME_DIRS:
            if v.name == "volume-01-java-fundamentals":
                build_volume_docx(v)
        build_combined_docx()


if __name__ == "__main__":
    main()
