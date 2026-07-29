#!/usr/bin/env python3
"""Validate focused-series sources, Java companions, navigation, and PDFs."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import tempfile
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

from PIL import Image
from pypdf import PdfReader


ROOT = Path(__file__).resolve().parents[1]
VOLUMES = ROOT / "content" / "volumes"
DIST = ROOT / "dist"
SERIES_SPEC = ROOT / "publishing" / "series.json"
ARTIFACT_MANIFEST = DIST / "manifest.json"
INDEX_NAME = "Java-SDE2-Interview-Preparation-Series-Index.pdf"
AUTHOR = "Vinay Reddy Kalluri"
DRAFT_MARKER = re.compile(r"\b(?:TODO|TBD|FIXME|placeholder|lorem ipsum)\b", re.IGNORECASE)
def fail(message: str) -> None:
    raise RuntimeError(message)


def normalized(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read_json(path: Path) -> dict[str, Any]:
    if not path.exists():
        fail(f"Missing JSON file: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def check_markdown(path: Path, require_ascii: bool = False) -> None:
    raw = path.read_bytes()
    if require_ascii:
        try:
            raw.decode("ascii")
        except UnicodeDecodeError as exc:
            fail(f"Series-native source is not ASCII: {path}: {exc}")
    text = raw.decode("utf-8")
    if DRAFT_MARKER.search(text):
        fail(f"Draft marker found: {path}")
    active_fence: str | None = None
    for line in text.splitlines():
        match = re.match(r"^\s*(`{3,}|~{3,})", line)
        if not match:
            continue
        marker = match.group(1)[0]
        if active_fence is None:
            active_fence = marker
        elif marker == active_fence:
            active_fence = None
    if active_fence is not None:
        fail(f"Unbalanced fenced code block: {path}")

    paragraphs = [
        normalized(part).casefold()
        for part in re.split(r"\n\s*\n", text)
        if len(normalized(part)) >= 120
        and not part.lstrip().startswith(("```", "~~~"))
    ]
    seen: set[str] = set()
    for paragraph in paragraphs:
        if paragraph in seen:
            fail(f"Duplicate long paragraph found in {path}")
        seen.add(paragraph)


def decorate_segments(spec: dict[str, Any]) -> None:
    by_id = {str(volume["id"]): volume for volume in spec["volumes"]}
    flattened: list[str] = []
    for segment in spec.get("segments", []):
        book_ids = [str(item) for item in segment.get("books", [])]
        if not book_ids:
            fail(f"Segment {segment.get('id', '<unknown>')} has no books")
        flattened.extend(book_ids)
        for position, volume_id in enumerate(book_ids, start=1):
            if volume_id not in by_id:
                fail(f"Segment {segment['id']} contains unknown volume {volume_id}")
            volume = by_id[volume_id]
            if "segment_id" in volume:
                fail(f"Volume {volume_id} appears in multiple segments")
            volume.update(
                {
                    "segment_id": segment["id"],
                    "segment_title": segment["title"],
                    "segment_code": segment["code"],
                    "segment_position": position,
                    "segment_count": len(book_ids),
                }
            )
    if flattened != [str(item) for item in spec.get("learning_order", [])]:
        fail("learning_order must equal the segment book lists in order")
    if len(flattened) != len(by_id) or set(flattened) != set(by_id):
        fail("Segments must contain every physical volume exactly once")
    for global_position, volume_id in enumerate(flattened, start=1):
        volume = by_id[volume_id]
        volume["book_position"] = global_position
        volume["path_label"] = str(spec["path_labels"][volume_id])
        volume["volume_label"] = (
            f"{volume['segment_title']} - Book {volume['segment_position']:02d} "
            f"of {volume['segment_count']:02d} - Study Step {volume['path_label']}"
        )


def check_numbering_contract(spec: dict[str, Any]) -> None:
    ids = [str(volume["id"]) for volume in spec["volumes"]]
    learning_order = [str(volume_id) for volume_id in spec.get("learning_order", [])]
    path_labels = {str(key): str(value) for key, value in spec.get("path_labels", {}).items()}
    if len(learning_order) != len(ids) or set(learning_order) != set(ids):
        fail("learning_order must contain every physical book exactly once")
    if set(path_labels) != set(ids):
        fail("path_labels must contain every physical book exactly once")
    actual_labels = [path_labels[volume_id] for volume_id in learning_order]
    if len(set(actual_labels)) != len(actual_labels):
        fail("Public study codes must be unique")
    segments = spec.get("segments", [])
    if len(segments) != 3:
        fail("Series manifest must define exactly three selectable segments")
    codes = [segment.get("code") for segment in segments]
    if len(set(codes)) != len(codes):
        fail("Segment codes must be unique")
    print(
        "Segments: "
        + ", ".join(f"{segment['title']} ({len(segment['books'])} books)" for segment in segments)
    )


def check_sources(spec: dict[str, Any]) -> None:
    paths: set[Path] = set()
    for volume in spec["volumes"]:
        for source in volume["sources"]:
            path = (ROOT / source["path"]).resolve()
            if not path.exists():
                fail(f"Missing source for volume {volume['id']}: {path}")
            paths.add(path)
            check_markdown(
                path,
                require_ascii=source["path"].startswith(
                    "content/volumes/01-number-systems-and-math-foundations/"
                ),
            )
        companion = volume.get("code_companion")
        if companion:
            path = (ROOT / companion["path"]).resolve()
            if not path.exists():
                fail(f"Missing Java companion for volume {volume['id']}: {path}")
            try:
                code = path.read_text(encoding="ascii")
            except UnicodeDecodeError as exc:
                fail(f"Java companion is not ASCII: {path}: {exc}")
            if DRAFT_MARKER.search(code):
                fail(f"Draft marker found in Java companion: {path}")

    chapter_dir = VOLUMES / "01-number-systems-and-math-foundations" / "chapters"
    chapters = sorted(chapter_dir.glob("*.md"))
    expected_chapters = {
        "01-why-number-systems-matter.md",
        "02-decimal-number-system.md",
        "03-binary-number-system.md",
        "04-octal-and-hexadecimal.md",
        "05-base-conversion-patterns.md",
        "06-java-integer-types-and-limits.md",
        "07-overflow-and-underflow.md",
        "08-very-large-numbers.md",
        "09-divisibility-rules.md",
        "10-factors-primes-gcd-lcm.md",
        "11-modular-arithmetic.md",
        "12-powers-roots-logarithms.md",
        "13-bit-level-prerequisites.md",
        "14-essential-interview-patterns.md",
        "14a-fifty-two-implementation-reference.md",
        "15-java-number-traps.md",
        "15a-expanded-practice-bank.md",
        "16-interview-questions-and-revision.md",
    }
    actual_chapters = {path.name for path in chapters}
    if actual_chapters != expected_chapters:
        missing = sorted(expected_chapters - actual_chapters)
        unexpected = sorted(actual_chapters - expected_chapters)
        fail(
            "Number Systems chapter inventory is incomplete: "
            f"missing={missing}, unexpected={unexpected}"
        )

    catalog = (chapter_dir / "14-essential-interview-patterns.md").read_text(encoding="ascii")
    pattern_numbers = {int(item) for item in re.findall(r"^### Pattern (\d+):", catalog, re.MULTILINE)}
    if pattern_numbers != set(range(1, 31)):
        fail(f"Mandatory problem catalog is incomplete: {sorted(pattern_numbers)}")

    revision = (chapter_dir / "16-interview-questions-and-revision.md").read_text(encoding="ascii")
    required_counts = {
        "conceptual interview questions": 30,
        "code-output questions": 20,
        "debugging questions": 20,
        "short coding exercises": 20,
        "medium coding problems": 10,
        "interviewer follow-up chains": 5,
    }
    for label, expected in required_counts.items():
        match = re.search(rf"^## .*?{re.escape(label)}", revision, flags=re.MULTILINE | re.IGNORECASE)
        if not match:
            fail(f"Revision bank is missing its {label} section")
        start = match.end()
        next_h2 = revision.find("\n## ", start)
        section = revision[start:] if next_h2 < 0 else revision[start:next_h2]
        count = len(re.findall(r"^### ", section, flags=re.MULTILINE))
        if count != expected:
            fail(f"Expected {expected} {label}; found {count}")

    assets = VOLUMES / "01-number-systems-and-math-foundations" / "assets"
    diagrams = sorted(
        path for path in assets.glob("*.png") if "contact-sheet" not in path.name
    )
    if len(diagrams) < 8:
        fail(f"Expected at least 8 Number Systems diagrams; found {len(diagrams)}")
    for path in diagrams:
        with Image.open(path) as image:
            if image.width < 1400 or image.height < 800:
                fail(f"Diagram is below the print-resolution target: {path} ({image.size})")

    print(f"Sources: {len(paths)} unique mapped Markdown files; 18 Number Systems chapters")
    print(f"Diagrams: {len(diagrams)} high-resolution Number Systems PNG files")


def annotation_uris(reader: PdfReader) -> set[str]:
    uris: set[str] = set()
    for page in reader.pages:
        for reference in page.get("/Annots", []):
            annotation = reference.get_object()
            action = annotation.get("/A")
            if action and action.get("/S") == "/URI":
                value = action.get("/URI")
                if value:
                    uris.add(str(value))
    return uris


def outline_count(reader: PdfReader) -> int:
    def walk(items: list[Any]) -> int:
        total = 0
        for item in items:
            if isinstance(item, list):
                total += walk(item)
            else:
                total += 1
        return total

    try:
        return walk(reader.outline)
    except Exception as exc:  # pragma: no cover - defensive parser boundary
        fail(f"Could not read PDF outline: {exc}")
    return 0


def source_display_title(source: dict[str, Any]) -> str:
    if source.get("title"):
        return source["title"]
    text = (ROOT / source["path"]).read_text(encoding="utf-8")
    match = re.search(r"^#\s+(.+)$", text, flags=re.MULTILINE)
    title = match.group(1).strip() if match else Path(source["path"]).stem
    title = re.sub(r"^(?:Chapter\s+)?\d+\s*(?::|\.|-)\s*", "", title, flags=re.IGNORECASE)
    title = re.sub(r"^Appendix\s+[A-Z]\s*-\s*", "", title, flags=re.IGNORECASE)
    return title.strip()


def check_pdf(path: Path, volume: dict[str, Any], known_outputs: set[str]) -> dict[str, Any]:
    reader = PdfReader(path)
    count = len(reader.pages)
    if not volume.get("min_pages", 8) <= count <= volume.get("max_pages", 180):
        fail(f"{volume['id']} page count {count} is outside its declared bounds")

    metadata = reader.metadata or {}
    if metadata.get("/Author") != AUTHOR:
        fail(f"{volume['id']} has incorrect author metadata")
    if volume["title"] not in str(metadata.get("/Title", "")):
        fail(f"{volume['id']} has incorrect title metadata")

    pages: list[str] = []
    for page_number, page in enumerate(reader.pages, start=1):
        width = float(page.mediabox.width)
        height = float(page.mediabox.height)
        if abs(width - 612) > 0.5 or abs(height - 792) > 0.5:
            fail(f"{volume['id']} page {page_number} is not US Letter: {width}x{height}")
        text = page.extract_text() or ""
        pages.append(text)
        if page_number > 1 and len(re.sub(r"\s+", "", text)) < 8:
            fail(f"{volume['id']} page {page_number} appears blank")

    full_text = normalized("\n".join(pages))
    cover_text = normalized(pages[0])
    if normalized(volume["volume_label"].upper()) not in cover_text:
        fail(f"{volume['id']} cover is missing its canonical study code: {volume['volume_label']}")
    if re.search(r"\bVOLUME\s+\d+\s+OF\s+18\b", cover_text):
        fail(f"{volume['id']} cover still exposes a conflicting legacy volume number")
    if "BY" not in cover_text or AUTHOR not in cover_text:
        fail(f"{volume['id']} cover is missing the simple author byline")
    for legacy_credit in ("FOUNDING AUTHOR", "EDITOR-IN-CHIEF", "CHIEF AUDITOR"):
        if legacy_credit in cover_text:
            fail(f"{volume['id']} cover still exposes the legacy credit: {legacy_credit}")
    for required in (
        volume["title"],
        AUTHOR,
        "About the Author",
        "Series Roadmap",
        "Contents",
        "Practice Ladder and Next Steps",
        "Completion check",
    ):
        if required not in full_text:
            fail(f"{volume['id']} is missing required PDF text: {required}")

    for number, source in enumerate(volume["sources"], start=1):
        expected = normalized(f"Chapter {number} - {source_display_title(source)}")
        if expected not in full_text:
            fail(f"{volume['id']} is missing local chapter heading: {expected}")
    companion = volume.get("code_companion")
    if companion:
        expected = normalized(
            f"Chapter {len(volume['sources']) + 1} - "
            f"{companion.get('title', 'Dependency-Free Java 21 Companion')}"
        )
        if expected not in full_text:
            fail(f"{volume['id']} is missing Java companion chapter: {expected}")

    outlines = outline_count(reader)
    expected_local_chapters = len(volume["sources"]) + int(bool(companion))
    if outlines < expected_local_chapters + 2:
        fail(f"{volume['id']} has too few bookmarks: {outlines}")

    uris = annotation_uris(reader)
    sibling_outputs = {name for name in known_outputs if name != INDEX_NAME}
    if not sibling_outputs.issubset(uris):
        missing = sorted(sibling_outputs - uris)
        fail(f"{volume['id']} roadmap is missing sibling links: {missing}")
    for uri in uris:
        if (
            uri.casefold().endswith(".pdf")
            and not urlparse(uri).scheme
            and uri not in known_outputs
        ):
            fail(f"{volume['id']} links to an unknown sibling PDF: {uri}")

    return {"page_count": count, "sha256": sha256(path), "bytes": path.stat().st_size}


def check_index(path: Path, spec: dict[str, Any], known_outputs: set[str]) -> dict[str, Any]:
    reader = PdfReader(path)
    count = len(reader.pages)
    if not 8 <= count <= 50:
        fail(f"Series index has unexpected page count: {count}")
    metadata = reader.metadata or {}
    if metadata.get("/Author") != AUTHOR:
        fail("Series index has incorrect author metadata")
    page_texts = [page.extract_text() or "" for page in reader.pages]
    text = normalized("\n".join(page_texts))
    cover_text = normalized(page_texts[0])
    if "BY" not in cover_text or AUTHOR not in cover_text:
        fail("Series index cover is missing the simple author byline")
    for legacy_credit in ("FOUNDING AUTHOR", "EDITOR-IN-CHIEF", "CHIEF AUDITOR"):
        if legacy_credit in cover_text:
            fail(f"Series index cover still exposes the legacy credit: {legacy_credit}")
    for required_publication_text in (
        "CC BY 4.0",
        "AUTHORS.md",
    ):
        if required_publication_text not in text:
            fail(
                "Series index is missing public authorship/licensing text: "
                f"{required_publication_text}"
            )
    for segment in spec["segments"]:
        if segment["title"] not in text:
            fail(f"Series index is missing segment {segment['title']}")
        for position in range(1, len(segment["books"]) + 1):
            if f"{segment['code']} {position:02d}" not in text:
                fail(f"Series index is missing {segment['code']} {position:02d}")
    for required in (
        "Java SDE-2 Interview Preparation Series Index",
        "About the Author",
        "Series Roadmap",
        "Contents",
        "CC BY 4.0",
        "AUTHORS.md",
    ):
        if required not in text:
            fail(f"Series index is missing required text: {required}")
    uris = annotation_uris(reader)
    expected = {volume["output_name"] for volume in spec["volumes"]}
    if not expected.issubset(uris):
        fail(f"Series index is missing volume links: {sorted(expected - uris)}")
    unknown_local_pdfs = {
        uri
        for uri in uris
        if uri.casefold().endswith(".pdf")
        and not urlparse(uri).scheme
        and uri not in known_outputs
    }
    if unknown_local_pdfs:
        fail(f"Series index contains unknown PDF links: {sorted(unknown_local_pdfs)}")
    return {"page_count": count, "sha256": sha256(path), "bytes": path.stat().st_size}


def check_artifacts(spec: dict[str, Any]) -> None:
    report = read_json(ARTIFACT_MANIFEST)
    if report.get("physical_volumes") != len(spec["volumes"]):
        fail("Artifact manifest has an incorrect physical-volume count")
    if len(report.get("volumes", [])) != len(spec["volumes"]):
        fail("Artifact manifest does not contain every physical volume")
    expected_order = [str(volume_id) for volume_id in spec["learning_order"]]
    if [str(item["id"]) for item in report["volumes"]] != expected_order:
        fail("Artifact manifest books are not in canonical learning order")

    known_outputs = {volume["output_name"] for volume in spec["volumes"]} | {INDEX_NAME}
    recorded = {item["id"]: item for item in report["volumes"]}
    total_pages = 0
    by_id = {str(volume["id"]): volume for volume in spec["volumes"]}
    for position, volume_id in enumerate(expected_order, start=1):
        volume = by_id[volume_id]
        path = DIST / volume["output_name"]
        if not path.exists():
            fail(f"Missing focused PDF: {path}")
        actual = check_pdf(path, volume, known_outputs)
        expected = recorded.get(volume["id"])
        if not expected:
            fail(f"Artifact manifest is missing volume {volume['id']}")
        expected_label = str(spec["path_labels"][volume_id])
        if expected.get("path_label") != expected_label or expected.get("book_position") != position:
            fail(f"Artifact manifest has incorrect study position for {volume_id}")
        for key in ("segment_id", "segment_code", "segment_position"):
            if expected.get(key) != volume.get(key):
                fail(f"Artifact manifest has incorrect {key} for {volume_id}")
        for key in ("page_count", "sha256", "bytes"):
            if expected.get(key) != actual[key]:
                fail(f"Artifact manifest mismatch for {volume['id']} field {key}")
        total_pages += actual["page_count"]
        print(f"PDF {volume['id']}: {actual['page_count']} pages; {actual['sha256'][:12]}...")

    index_path = DIST / INDEX_NAME
    if not index_path.exists():
        fail(f"Missing series index PDF: {index_path}")
    actual_index = check_index(index_path, spec, known_outputs)
    recorded_index = report.get("index")
    if not recorded_index:
        fail("Artifact manifest is missing the index PDF")
    for key in ("page_count", "sha256", "bytes"):
        if recorded_index.get(key) != actual_index[key]:
            fail(f"Artifact manifest mismatch for index field {key}")
    total_pages += actual_index["page_count"]
    print(f"Index: {actual_index['page_count']} pages; {actual_index['sha256'][:12]}...")
    print(f"Artifacts: {len(spec['volumes']) + 1} PDFs; {total_pages} total pages")


def run_java_validation() -> None:
    script = ROOT / "scripts" / "validate_number_system_examples.sh"
    if not script.exists():
        fail(f"Missing Java compilation validator: {script}")
    result = subprocess.run(
        ["bash", str(script)],
        cwd=ROOT,
        text=True,
        capture_output=True,
        check=False,
    )
    if result.returncode:
        print(result.stdout)
        print(result.stderr)
        fail("Number Systems Java validation failed")
    print(normalized(result.stdout) or "Java examples: validation passed")


def run_series_native_java_validation(spec: dict[str, Any]) -> None:
    """Compile and execute complete public Java classes in focused native chapters."""
    classes: dict[str, tuple[Path, str]] = {}
    volume_classes: dict[str, list[str]] = {}
    native_volumes: set[str] = set()
    fence = re.compile(r"```java\s*\n(.*?)\n```", re.DOTALL)
    declaration = re.compile(r"\bpublic\s+final\s+class\s+([A-Za-z_$][\w$]*)\b")

    def register(volume_id: str, path: Path, code: str) -> None:
        match = declaration.search(code)
        if not match or "public static void main" not in code:
            return
        class_name = match.group(1)
        if class_name in classes:
            fail(
                f"Duplicate series-native public class {class_name}: "
                f"{classes[class_name][0]} and {path}"
            )
        classes[class_name] = (path, code)
        volume_classes.setdefault(volume_id, []).append(class_name)

    for volume in spec["volumes"]:
        volume_id = str(volume["id"])
        if volume.get("publication_status") == "planned":
            continue
        native_sources = [
            source
            for source in volume["sources"]
            if source.get("series_native")
            and "01-number-systems-and-math-foundations" not in source["path"]
        ]
        if not native_sources:
            continue
        native_volumes.add(volume_id)
        companion = volume.get("code_companion")
        if companion:
            path = (ROOT / companion["path"]).resolve()
            try:
                code = path.read_text(encoding="ascii")
            except UnicodeDecodeError as exc:
                fail(f"Series-native Java companion is not ASCII: {path}: {exc}")
            register(volume_id, path, code)
            continue
        for source in native_sources:
            path = (ROOT / source["path"]).resolve()
            text = path.read_text(encoding="utf-8")
            for block in fence.findall(text):
                register(volume_id, path, block)

    if not classes:
        fail("No focused series-native public Java classes were found")
    for volume_id in sorted(native_volumes):
        companions = volume_classes.get(volume_id, [])
        if len(companions) != 1:
            fail(
                f"Focused volume {volume_id} must expose exactly one complete "
                f"dependency-free Java companion; found {companions}"
            )

    with tempfile.TemporaryDirectory(prefix="java-series-native-") as directory:
        temp = Path(directory)
        sources: list[Path] = []
        for class_name, (_, code) in sorted(classes.items()):
            output = temp / f"{class_name}.java"
            output.write_text(code, encoding="utf-8")
            sources.append(output)
        compile_result = subprocess.run(
            [
                "javac",
                "--release",
                "21",
                "-Xlint:all",
                "-Werror",
                "-d",
                str(temp / "classes"),
                *map(str, sources),
            ],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )
        if compile_result.returncode:
            print(compile_result.stdout)
            print(compile_result.stderr)
            fail("Focused series-native Java compilation failed")
        for class_name in sorted(classes):
            run_result = subprocess.run(
                ["java", "-ea", "-cp", str(temp / "classes"), class_name],
                cwd=ROOT,
                text=True,
                capture_output=True,
                check=False,
                timeout=30,
            )
            if run_result.returncode:
                print(run_result.stdout)
                print(run_result.stderr)
                fail(f"Focused series-native Java smoke test failed: {class_name}")
    print(f"Focused Java: compiled and ran {len(classes)} series-native classes")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-only", action="store_true")
    args = parser.parse_args()

    spec = read_json(SERIES_SPEC)
    decorate_segments(spec)
    check_numbering_contract(spec)
    if len(spec.get("volumes", [])) != 40:
        fail("Series manifest must define the complete 40-book segmented catalog")
    check_sources(spec)
    run_java_validation()
    run_series_native_java_validation(spec)
    if not args.source_only:
        check_artifacts(spec)
    print("Focused series validation passed.")


if __name__ == "__main__":
    main()
