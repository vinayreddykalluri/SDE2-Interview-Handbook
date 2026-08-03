#!/usr/bin/env python3
"""Run deterministic release checks, optionally followed by local artifact builds.

Release CI uses ``--check-only`` so catalog drift is caught without creating a
site or requiring the large PDF payload. A full local build retains byte-level
PDF validation before it writes any derived output.
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
AUTOMATION = ROOT / "tooling" / "automation"


def run(script: str, *arguments: str) -> None:
    subprocess.run(
        [sys.executable, str(AUTOMATION / script), *arguments],
        cwd=str(ROOT),
        check=True,
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--check-only",
        action="store_true",
        help="Validate source and synchronized release metadata without building artifacts.",
    )
    args = parser.parse_args()

    # CI already runs the book-source and Java suites. Its release-specific gap
    # is the four-segment web/catalog/artifact metadata contract, so keep this
    # mode focused and independent of generated PDFs and site output.
    if args.check_only:
        run("validate_web.py", "--metadata-only")
        print("Release source/catalog checks passed; artifact builders were not run")
        return

    run("validate_repository_layout.py")
    run("validate_structure.py")
    run("validate_links.py")
    run("validate_java_examples.py")
    run("validate_web.py")
    run("build_site.py")
    run("build_pdf.py")
    run("build_docx.py")

if __name__ == "__main__":
    main()
