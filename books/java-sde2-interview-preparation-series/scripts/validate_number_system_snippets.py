#!/usr/bin/env python3
"""Extract and compile every standalone Java class printed in Volume 1."""

from __future__ import annotations

import re
import subprocess
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CHAPTERS = (
    ROOT
    / "content"
    / "volumes"
    / "dsa"
    / "DSA-02-03-number-systems-and-math-foundations"
    / "chapters"
)
EXPECTED_STANDALONE_BLOCKS = 24


def java_blocks(path: Path) -> list[tuple[str, str]]:
    lines = path.read_text(encoding="ascii").splitlines()
    blocks: list[tuple[str, str]] = []
    index = 0
    while index < len(lines):
        opening = re.match(r"^\s*(`{3,}|~{3,})java\s*$", lines[index])
        if not opening:
            index += 1
            continue
        marker = opening.group(1)[0]
        index += 1
        code: list[str] = []
        while index < len(lines) and not re.match(
            rf"^\s*{re.escape(marker)}{{3,}}\s*$", lines[index]
        ):
            code.append(lines[index])
            index += 1
        if index >= len(lines):
            raise RuntimeError(f"Unclosed Java fence in {path}")
        source = "\n".join(code).strip() + "\n"
        public_type = re.search(
            r"(?m)^public\s+(?:(?:final|abstract|sealed)\s+)*"
            r"(?:class|record|interface|enum)\s+(\w+)",
            source,
        )
        solution = re.search(r"(?m)^(?:final\s+)?class\s+(Solution)\b", source)
        type_name = public_type.group(1) if public_type else solution.group(1) if solution else None
        if type_name:
            blocks.append((type_name, source))
        index += 1
    return blocks


def main() -> None:
    standalone: list[tuple[Path, str, str]] = []
    chapters = sorted(CHAPTERS.glob("*.md"))
    for chapter in chapters:
        standalone.extend((chapter, name, source) for name, source in java_blocks(chapter))
    if len(standalone) != EXPECTED_STANDALONE_BLOCKS:
        raise RuntimeError(
            f"Expected {EXPECTED_STANDALONE_BLOCKS} standalone Java blocks; "
            f"found {len(standalone)}"
        )

    with tempfile.TemporaryDirectory(prefix="number-systems-snippets-") as temporary:
        base = Path(temporary)
        for number, (chapter, type_name, source) in enumerate(standalone, start=1):
            unit = base / f"unit-{number:02d}"
            classes = unit / "classes"
            classes.mkdir(parents=True)
            source_path = unit / f"{type_name}.java"
            source_path.write_text(source, encoding="ascii")
            result = subprocess.run(
                [
                    "javac",
                    "--release",
                    "21",
                    "-Xlint:all",
                    "-d",
                    str(classes),
                    str(source_path),
                ],
                cwd=ROOT,
                text=True,
                capture_output=True,
                check=False,
            )
            if result.returncode:
                raise RuntimeError(
                    f"Standalone Java block {type_name} in {chapter.name} did not compile:\n"
                    f"{result.stdout}{result.stderr}"
                )
    print(
        f"Compiled {len(standalone)} standalone Java blocks "
        f"from {len(chapters)} Number Systems learning modules."
    )


if __name__ == "__main__":
    main()
