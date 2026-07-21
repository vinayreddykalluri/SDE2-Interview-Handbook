#!/usr/bin/env python3
"""Build the web portal and MkDocs handbook into one GitHub Pages artifact."""

from __future__ import annotations

import json
import shutil
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
WEB_SOURCE = ROOT / "web"
SITE_OUTPUT = ROOT / "site"
DOCS_OUTPUT = SITE_OUTPUT / "docs"
VOLUMES_FILE = WEB_SOURCE / "content" / "volumes.json"


def main() -> int:
    if SITE_OUTPUT.exists():
        shutil.rmtree(SITE_OUTPUT)

    subprocess.run(
        [
            sys.executable,
            "-m",
            "mkdocs",
            "build",
            "--strict",
            "--clean",
            "--site-dir",
            str(DOCS_OUTPUT),
        ],
        cwd=ROOT,
        check=True,
    )

    shutil.copytree(WEB_SOURCE, SITE_OUTPUT, dirs_exist_ok=True)

    volumes = json.loads(VOLUMES_FILE.read_text(encoding="utf-8"))
    required_outputs = [
        SITE_OUTPUT / "index.html",
        SITE_OUTPUT / "assets" / "styles.css",
        SITE_OUTPUT / "assets" / "app.js",
        SITE_OUTPUT / "content" / "volumes.json",
        DOCS_OUTPUT / "index.html",
    ]
    required_outputs.extend(
        DOCS_OUTPUT / volume["slug"] / "index.html" for volume in volumes
    )

    missing = [path.relative_to(ROOT) for path in required_outputs if not path.is_file()]
    if missing:
        print("Site build is incomplete:", file=sys.stderr)
        for path in missing:
            print(f"  - {path}", file=sys.stderr)
        return 1

    print("Built unified GitHub Pages artifact:")
    print(f"  Portal:   {SITE_OUTPUT / 'index.html'}")
    print(f"  Handbook: {DOCS_OUTPUT / 'index.html'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
