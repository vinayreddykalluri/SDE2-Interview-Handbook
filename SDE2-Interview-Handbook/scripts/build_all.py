#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def main() -> None:
    subprocess.run(["python", "scripts/validate_structure.py"], cwd=str(ROOT), check=True)
    subprocess.run(["python", "scripts/validate_links.py"], cwd=str(ROOT), check=True)
    subprocess.run(["mkdocs", "build"], cwd=str(ROOT), check=True)
    subprocess.run(["python", "scripts/build_pdf.py"], cwd=str(ROOT), check=True)
    subprocess.run(["python", "scripts/build_docx.py"], cwd=str(ROOT), check=True)

if __name__ == "__main__":
    main()
