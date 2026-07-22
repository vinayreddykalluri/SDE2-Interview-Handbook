#!/usr/bin/env python3
from __future__ import annotations

import argparse
import os
import shutil
import subprocess
from pathlib import Path
from typing import Iterable, List

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
OUTPUT = ROOT / "output"
TEMPLATES = ROOT / "templates"
SITE = ROOT / "site"

MODULE_DIRS = sorted(DOCS.glob("coding-foundations/[0-9][0-9]-*"), key=lambda p: p.name)


def resolve_pdf_engine() -> str:
    requested = os.environ.get("PDF_ENGINE")
    if requested:
        if shutil.which(requested) is None:
            raise RuntimeError(
                f"PDF_ENGINE={requested!r} is not available on PATH. "
                "Install it or unset PDF_ENGINE to use automatic detection."
            )
        return requested

    for candidate in ("xelatex", "tectonic"):
        if shutil.which(candidate):
            return candidate

    raise RuntimeError(
        "A PDF engine is required. Install tectonic (recommended for local builds) "
        "or xelatex, then retry."
    )


def run(cmd: List[str], cwd: Path = ROOT) -> None:
    subprocess.run(cmd, cwd=str(cwd), check=True)


def combine_markdown_files(paths: Iterable[Path], title: str) -> Path:
    tmp = OUTPUT / "combined"
    tmp.mkdir(parents=True, exist_ok=True)
    out = tmp / f"{title}.md"
    lines: List[str] = [f"# {title}\n"]
    for p in paths:
        text = p.read_text(encoding="utf-8")
        lines.append("\n\\newpage\n")
        lines.append(text)
    out.write_text("\n".join(lines), encoding="utf-8")
    return out


def gather_module_chapters(module_dir: Path) -> List[Path]:
    return sorted([p for p in module_dir.glob("*.md") if p.name != "index.md"], key=lambda p: p.name)


def build_module_pdf(module_dir: Path, keep: bool = False) -> Path:
    module_title = "Module " + module_dir.name.replace("-", " ").title()
    chapters = [module_dir / "index.md", *gather_module_chapters(module_dir)]
    exists = [p for p in chapters if p.exists()]
    if not exists:
        raise RuntimeError(f"No chapters found in {module_dir}")

    combined = combine_markdown_files(exists, module_title)

    output_pdf = OUTPUT / "pdf" / f"{module_dir.name.replace('module-', 'Module-').replace('-', ' ').title().replace(' ', '-')}.pdf"
    output_pdf.parent.mkdir(parents=True, exist_ok=True)

    if shutil.which("pandoc") is None:
        raise RuntimeError("pandoc is required to build PDF. Install pandoc and retry.")

    cmd = [
        "pandoc",
        str(combined),
        "--from=gfm+tex_math_dollars",
        "--standalone",
        "--toc",
        "--number-sections",
        "--highlight-style=tango",
        f"--pdf-engine={resolve_pdf_engine()}",
        "--resource-path=docs",
        "-V",
        "papersize=letter",
        "-V",
        "geometry:margin=0.75in",
        "-V",
        "fontsize=10pt",
        "-V",
        "colorlinks=true",
        "-o",
        str(output_pdf),
    ]
    run(cmd)

    if not keep:
        combined.unlink(missing_ok=True)
    return output_pdf


def build_combined_pdf() -> Path:
    vol_chapters: List[Path] = []
    for v in MODULE_DIRS:
        idx = v / "index.md"
        if idx.exists():
            vol_chapters.append(idx)
            vol_chapters.extend([p for p in gather_module_chapters(v) if p.exists()])
    if not vol_chapters:
        raise RuntimeError("No markdown found to generate combined PDF")

    combined = combine_markdown_files(vol_chapters, "SDE2 Interview Handbook")
    output_pdf = OUTPUT / "combined" / "SDE2-Interview-Handbook.pdf"
    output_pdf.parent.mkdir(parents=True, exist_ok=True)

    if shutil.which("pandoc") is None:
        raise RuntimeError("pandoc is required to build PDF. Install pandoc and retry.")

    cmd = [
        "pandoc",
        str(combined),
        "--from=gfm+tex_math_dollars",
        "--standalone",
        "--toc",
        "--number-sections",
        "--highlight-style=tango",
        f"--pdf-engine={resolve_pdf_engine()}",
        "--resource-path=docs",
        "-V",
        "papersize=letter",
        "-V",
        "geometry:margin=0.75in",
        "-V",
        "fontsize=10pt",
        "-V",
        "colorlinks=true",
        "-o",
        str(output_pdf),
    ]
    run(cmd)
    combined.unlink(missing_ok=True)
    return output_pdf


def render_all() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    OUTPUT.joinpath("pdf").mkdir(parents=True, exist_ok=True)
    OUTPUT.joinpath("combined").mkdir(parents=True, exist_ok=True)

    for module in MODULE_DIRS:
        if not any(module.glob("*.md")):
            continue
        build_module_pdf(module)

    build_combined_pdf()
    print("PDF generation completed")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--module", choices=[v.name for v in MODULE_DIRS], help="Specific module")
    parser.add_argument("--keep", action="store_true", help="Keep temporary combined source")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if args.module:
        build_module_pdf((DOCS / "coding-foundations" / args.module))
    else:
        render_all()


if __name__ == "__main__":
    main()
