#!/usr/bin/env python3
from __future__ import annotations

import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "examples" / "java" / "src"
MAIN_ROOT = JAVA_ROOT / "main" / "java"
TEST_ROOT = JAVA_ROOT / "test" / "java"
SMOKE_CLASS = "io.github.vinayreddykalluri.interviewhandbook.tests.ExampleSmokeTest"


def require_tool(name: str) -> str:
    executable = shutil.which(name)
    if executable is None:
        raise RuntimeError(f"{name} is required. Install JDK 17 or newer and retry.")
    return executable


def java_sources(root: Path) -> list[Path]:
    return sorted(root.rglob("*.java")) if root.exists() else []


def main() -> None:
    javac = require_tool("javac")
    java = require_tool("java")
    main_sources = java_sources(MAIN_ROOT)
    test_sources = java_sources(TEST_ROOT)

    if not main_sources:
        raise RuntimeError(f"No Java examples found under {MAIN_ROOT}")
    if not test_sources:
        raise RuntimeError(f"No Java smoke checks found under {TEST_ROOT}")

    with tempfile.TemporaryDirectory(prefix="sde2-handbook-java-") as temp_dir:
        classes = Path(temp_dir) / "classes"
        classes.mkdir()
        compile_command = [
            javac,
            "--release",
            "17",
            "-encoding",
            "UTF-8",
            "-Xlint:all",
            "-d",
            str(classes),
            *map(str, main_sources),
            *map(str, test_sources),
        ]
        subprocess.run(compile_command, cwd=ROOT, check=True)
        subprocess.run(
            [java, "-ea", "-cp", str(classes), SMOKE_CLASS],
            cwd=ROOT,
            check=True,
        )

    print(f"Validated {len(main_sources)} Java examples and {len(test_sources)} smoke-test source(s)")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, subprocess.CalledProcessError) as exc:
        print(f"Java example validation failed: {exc}", file=sys.stderr)
        raise SystemExit(1) from exc
