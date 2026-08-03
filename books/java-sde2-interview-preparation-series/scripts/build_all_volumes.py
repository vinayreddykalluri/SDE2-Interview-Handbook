#!/usr/bin/env python3
"""Rebuild every focused volume, resumably and in parallel.

`build_series.py` builds one volume per invocation and takes roughly forty
seconds each, so a full series rebuild is a long single-threaded run. This
driver fans the volumes out across worker processes and records completion in
a state file, so an interrupted rebuild resumes instead of starting over.

Each volume is an independent pandoc + ReportLab pipeline writing to its own
output path, so parallelism is safe. The series index is NOT built here --
it reads dist/manifest.json after every volume has landed, and must run last:

    python scripts/build_all_volumes.py --jobs 4
    python scripts/build_series.py --index-only

Usage:
    --jobs N        worker processes (default: CPU count)
    --state PATH    resume file (default: build/volume-build-state.json)
    --fresh         ignore the state file and rebuild everything
    --only ID,ID    restrict to specific volume ids
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import time
from concurrent.futures import ProcessPoolExecutor, as_completed
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SERIES_JSON = ROOT / "publishing" / "series.json"
BUILD_SCRIPT = ROOT / "scripts" / "build_series.py"
DEFAULT_STATE = ROOT / "build" / "volume-build-state.json"


def volume_ids() -> list[str]:
    data = json.loads(SERIES_JSON.read_text(encoding="utf-8"))
    return [volume["id"] for volume in data["volumes"]]


def build_one(volume_id: str) -> tuple[str, bool, str, float]:
    started = time.monotonic()
    process = subprocess.run(
        [sys.executable, str(BUILD_SCRIPT), "--volume", volume_id, "--skip-index"],
        cwd=str(ROOT),
        text=True,
        capture_output=True,
    )
    elapsed = time.monotonic() - started
    output = (process.stdout or "").strip() or (process.stderr or "").strip()
    return volume_id, process.returncode == 0, output, elapsed


def load_state(path: Path) -> set[str]:
    if not path.is_file():
        return set()
    try:
        return set(json.loads(path.read_text(encoding="utf-8")).get("completed", []))
    except (json.JSONDecodeError, OSError):
        return set()


def save_state(path: Path, completed: set[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps({"completed": sorted(completed)}, indent=2) + "\n",
        encoding="utf-8",
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jobs", type=int, default=os.cpu_count() or 2)
    parser.add_argument("--state", type=Path, default=DEFAULT_STATE)
    parser.add_argument("--fresh", action="store_true")
    parser.add_argument("--only", default="")
    args = parser.parse_args()

    every = volume_ids()
    if args.only:
        wanted = {part.strip() for part in args.only.split(",") if part.strip()}
        unknown = wanted - set(every)
        if unknown:
            parser.error(f"unknown volume ids: {', '.join(sorted(unknown))}")
        every = [v for v in every if v in wanted]

    completed = set() if args.fresh else load_state(args.state)
    pending = [v for v in every if v not in completed]

    if not pending:
        print(f"All {len(every)} volumes already built. Use --fresh to rebuild.")
        return 0

    print(f"Building {len(pending)} of {len(every)} volumes with {args.jobs} workers.")
    failures: list[tuple[str, str]] = []

    with ProcessPoolExecutor(max_workers=args.jobs) as pool:
        futures = {pool.submit(build_one, v): v for v in pending}
        for future in as_completed(futures):
            volume_id, ok, output, elapsed = future.result()
            if ok:
                completed.add(volume_id)
                save_state(args.state, completed)
                print(f"  [{elapsed:5.1f}s] {output}")
            else:
                failures.append((volume_id, output))
                print(f"  [{elapsed:5.1f}s] FAILED {volume_id}\n{output}", file=sys.stderr)

    print(f"\nCompleted {len(completed)}/{len(every)} volumes.")
    if failures:
        print(f"{len(failures)} failed: {', '.join(v for v, _ in failures)}", file=sys.stderr)
        return 1

    print("Now run: python scripts/build_series.py --index-only")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
