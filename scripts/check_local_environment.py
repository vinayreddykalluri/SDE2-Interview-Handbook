#!/usr/bin/env python3
from __future__ import annotations

import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path
from typing import Sequence

ROOT = Path(__file__).resolve().parents[1]
VENV_PYTHON = ROOT / ".venv" / "bin" / "python"
REFERENCE_DOC = ROOT / "templates" / "reference.docx"

COMMANDS: Sequence[tuple[str, Sequence[str]]] = (
    ("brew", ("--version",)),
    ("git", ("--version",)),
    ("make", ("--version",)),
    ("python3", ("--version",)),
    ("java", ("-version",)),
    ("javac", ("-version",)),
    ("node", ("--version",)),
    ("npm", ("--version",)),
    ("pandoc", ("--version",)),
    ("gh", ("--version",)),
)


def first_version_line(executable: str, args: Sequence[str]) -> str:
    result = subprocess.run(
        [executable, *args],
        cwd=ROOT,
        capture_output=True,
        text=True,
        timeout=15,
        check=False,
    )
    output = "\n".join(part for part in (result.stdout, result.stderr) if part).strip()
    return output.splitlines()[0] if output else "version unavailable"


def main() -> int:
    failures: list[str] = []
    print("SDE2 Interview Handbook local environment")
    print(f"Repository: {ROOT}")

    for command_name, version_args in COMMANDS:
        executable = shutil.which(command_name)
        if executable is None:
            failures.append(f"missing command: {command_name}")
            print(f"[MISSING] {command_name}")
            continue
        print(f"[READY]   {command_name}: {first_version_line(executable, version_args)}")

    requested_engine = os.environ.get("PDF_ENGINE")
    if requested_engine:
        engine = requested_engine if shutil.which(requested_engine) else None
    else:
        engine = next((name for name in ("xelatex", "tectonic") if shutil.which(name)), None)
    if engine:
        print(f"[READY]   PDF engine: {engine}")
    else:
        failures.append("missing PDF engine: install tectonic or xelatex")
        print("[MISSING] PDF engine: tectonic or xelatex")

    if not VENV_PYTHON.exists():
        failures.append("missing .venv; run make bootstrap")
        print("[MISSING] project virtual environment")
    else:
        dependency_check = subprocess.run(
            [str(VENV_PYTHON), "-c", "import mkdocs, yaml"],
            cwd=ROOT,
            capture_output=True,
            text=True,
            check=False,
        )
        if dependency_check.returncode == 0:
            print("[READY]   Python documentation dependencies")
        else:
            failures.append("Python dependencies are incomplete; run make install")
            print("[MISSING] Python documentation dependencies")

    if REFERENCE_DOC.exists() and zipfile.is_zipfile(REFERENCE_DOC):
        print("[READY]   DOCX reference template")
    elif REFERENCE_DOC.exists():
        print("[WARNING] DOCX reference template is invalid; Pandoc default will be used")
    else:
        print("[READY]   Pandoc default DOCX template will be used")

    if failures:
        print("\nEnvironment is not ready:", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        return 1

    print("\nEnvironment is ready for validation, document builds, and local web serving.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
