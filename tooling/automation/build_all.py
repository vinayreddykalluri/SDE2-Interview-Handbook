#!/usr/bin/env python3
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
AUTOMATION = ROOT / "tooling" / "automation"

def main() -> None:
    python = sys.executable
    subprocess.run([python, str(AUTOMATION / "validate_repository_layout.py")], cwd=str(ROOT), check=True)
    subprocess.run([python, str(AUTOMATION / "validate_structure.py")], cwd=str(ROOT), check=True)
    subprocess.run([python, str(AUTOMATION / "validate_links.py")], cwd=str(ROOT), check=True)
    subprocess.run([python, str(AUTOMATION / "validate_java_examples.py")], cwd=str(ROOT), check=True)
    subprocess.run([python, str(AUTOMATION / "validate_web.py")], cwd=str(ROOT), check=True)
    subprocess.run([python, str(AUTOMATION / "build_site.py")], cwd=str(ROOT), check=True)
    subprocess.run([python, str(AUTOMATION / "build_pdf.py")], cwd=str(ROOT), check=True)
    subprocess.run([python, str(AUTOMATION / "build_docx.py")], cwd=str(ROOT), check=True)

if __name__ == "__main__":
    main()
