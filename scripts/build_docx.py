#!/usr/bin/env python3
from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path
from typing import List

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
OUTPUT = ROOT / "output"
REFERENCE_DOC = ROOT / "templates" / "reference.docx"
MODULE_DIRS = sorted(DOCS.glob("coding-foundations/[0-9][0-9]-*"), key=lambda p: p.name)


def reference_doc_args() -> List[str]:
    if not REFERENCE_DOC.exists():
        return []

    try:
        with zipfile.ZipFile(REFERENCE_DOC) as archive:
            names = set(archive.namelist())
    except (OSError, zipfile.BadZipFile):
        names = set()

    required = {"[Content_Types].xml", "word/document.xml", "word/styles.xml"}
    if required.issubset(names):
        return ["--reference-doc", str(REFERENCE_DOC)]

    print(
        f"Warning: {REFERENCE_DOC} is not a valid DOCX template; "
        "using Pandoc's default reference document.",
        file=sys.stderr,
    )
    return []


def module_output_stem(module_dir: Path) -> str:
    number, *topic = module_dir.name.split("-")
    return f"Module-{number}-{'-'.join(word.title() for word in topic)}"


def run(cmd: List[str]) -> None:
    subprocess.run(cmd, check=True)


def collect_markdown(module_dir: Path) -> List[Path]:
    return sorted([p for p in module_dir.glob("*.md")], key=lambda p: p.name)


def build_module_docx(module_dir: Path) -> Path:
    files = collect_markdown(module_dir)
    if not files:
        raise RuntimeError(f"No markdown in {module_dir}")
    out = OUTPUT / "docx" / f"{module_output_stem(module_dir)}.docx"
    out.parent.mkdir(parents=True, exist_ok=True)

    cmd = [
        "pandoc",
        *map(str, files),
        "--from=gfm",
        "--standalone",
        "--toc",
        "--number-sections",
        f"--resource-path={DOCS}",
    ]

    cmd += reference_doc_args()
    cmd += ["-o", str(out)]
    run(cmd)
    return out


def build_combined_docx() -> Path:
    all_files = []
    for v in MODULE_DIRS:
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
        "--standalone",
        "--toc",
        "--number-sections",
        f"--resource-path={DOCS}",
    ]
    cmd += reference_doc_args()
    cmd += ["-o", str(out)]
    run(cmd)
    return out


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--module", choices=[v.name for v in MODULE_DIRS])
    return parser.parse_args()


def main() -> None:
    if shutil.which("pandoc") is None:
        raise RuntimeError("pandoc is required to build DOCX. Install pandoc and retry.")
    args = parse_args()
    if args.module:
        build_module_docx(DOCS / "coding-foundations" / args.module)
    else:
        for v in MODULE_DIRS:
            build_module_docx(v)
        build_combined_docx()


if __name__ == "__main__":
    main()
