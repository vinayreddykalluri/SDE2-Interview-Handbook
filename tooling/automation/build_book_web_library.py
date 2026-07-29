#!/usr/bin/env python3
"""Build the complete web book library from canonical Markdown and Java sources."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
import subprocess
import sys
import tempfile
from collections import defaultdict
from pathlib import Path
from typing import Any

import yaml


ROOT = Path(__file__).resolve().parents[2]
BOOK_ROOT = ROOT / "books" / "java-sde2-interview-preparation-series"
SERIES_SPEC = BOOK_ROOT / "publishing" / "series.json"
ARTIFACT_MANIFEST = BOOK_ROOT / "dist" / "manifest.json"
REPOSITORY = "https://github.com/vinayreddykalluri/SDE2-Interview-Handbook"
BOOK_REPOSITORY_PATH = "books/java-sde2-interview-preparation-series"
PRODUCTION_ROOT = "https://vinayreddykalluri.github.io/SDE2-Interview-Handbook"
READER_CSS = ROOT / "tooling" / "book-web" / "book-reader.css"
PORTAL_ICON = ROOT / "apps" / "portal" / "assets" / "s2-mark.svg"
MERMAID_SCRIPT = ROOT / "docs" / "assets" / "javascripts" / "mermaid.mjs"
THEME_MANAGER_SCRIPT = ROOT / "docs" / "assets" / "javascripts" / "theme-manager.js"

MARKDOWN_LINK = re.compile(r"(?P<prefix>\]\()(?P<target><?[^)\s>]+>?)(?P<rest>[^)]*\))")
JAVA_FENCE = re.compile(r"^```java(?:\s|$)", re.MULTILINE | re.IGNORECASE)
HEADING = re.compile(r"^#\s+(.+?)\s*$", re.MULTILINE)


def read_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def web_slug(volume: dict[str, Any]) -> str:
    return f"{str(volume['path_label']).lower()}-{volume['slug']}"


def source_title(path: Path) -> str:
    text = path.read_text(encoding="utf-8")
    match = HEADING.search(text)
    if not match:
        return path.stem.replace("-", " ").title()
    title = match.group(1).strip()
    return re.sub(r"^(?:Chapter\s+)?\d+\s*(?::|\.|-)\s*", "", title, flags=re.IGNORECASE).strip()


def source_group(path: Path) -> str:
    parent = path.parent.name.lower()
    return {
        "chapters": "Chapters",
        "exercises": "Practice",
        "solutions": "Solutions",
    }.get(parent, "Core reading")


def source_url(path: Path) -> str:
    return f"{REPOSITORY}/blob/master/{BOOK_REPOSITORY_PATH}/{path.as_posix()}"


def pdf_url(filename: str) -> str:
    return f"{REPOSITORY}/raw/refs/heads/master/{BOOK_REPOSITORY_PATH}/dist/{filename}"


def safe_html(value: str) -> str:
    return (
        value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )


def context_bar(
    *,
    label: str,
    overview_href: str | None,
    code_href: str | None,
    pdf_href: str,
) -> str:
    links = []
    if overview_href:
        links.append(f'<a href="{overview_href}">Overview</a>')
    if code_href:
        links.append(f'<a href="{code_href}">Code</a>')
    links.append(f'<a href="{pdf_href}">Download PDF</a>')
    return (
        '<div class="reader-contextbar">'
        f'<span>{safe_html(label)}</span>'
        '<nav aria-label="Current book actions">'
        + "".join(links)
        + "</nav></div>"
    )


def study_status(*, segment_code: str, book_position: int, book_count: int, chapter_position: int | None = None, chapter_count: int | None = None) -> str:
    if chapter_position is None or chapter_count is None:
        progress = round(book_position / book_count * 100)
        chapter_text = "BOOK OVERVIEW"
    else:
        progress = round(chapter_position / chapter_count * 100)
        chapter_text = f"CHAPTER {chapter_position:02d} OF {chapter_count:02d}"
    return f'''<section class="reader-study-status" aria-label="Study progress">
  <div class="reader-study-status__labels">
    <strong>{safe_html(segment_code)}</strong>
    <span>BOOK {book_position:02d} OF {book_count:02d}</span>
    <span>{chapter_text}</span>
  </div>
  <div class="reader-progress" role="progressbar" aria-label="Current reading progress" aria-valuemin="0" aria-valuemax="100" aria-valuenow="{progress}"><i style="width:{progress}%"></i></div>
  <p>READ <b>·</b> TRACE <b>·</b> PRACTICE <b>·</b> EXPLAIN</p>
</section>'''


def reader_pagination(previous_href: str, previous_label: str, next_href: str, next_label: str) -> str:
    return f'''<nav class="reader-pagination" aria-label="Chapter navigation">
  <a href="{previous_href}"><span>PREVIOUS</span><strong>{safe_html(previous_label)}</strong></a>
  <a href="{next_href}"><span>NEXT</span><strong>{safe_html(next_label)}</strong></a>
</nav>'''


def resolve_local_target(source_path: Path, target: str) -> Path | None:
    clean = target.strip("<>").split("#", 1)[0]
    if not clean or clean.startswith(("#", "http://", "https://", "mailto:", "data:")):
        return None
    candidates = [BOOK_ROOT / clean, BOOK_ROOT / source_path.parent / clean]
    for candidate in candidates:
        resolved = candidate.resolve()
        if resolved.is_file() and (resolved == BOOK_ROOT.resolve() or BOOK_ROOT.resolve() in resolved.parents):
            return resolved
    return None


def rewrite_links(
    text: str,
    source_path: Path,
    source_outputs: dict[Path, str],
    volume_dir: Path,
) -> str:
    media_dir = volume_dir / "media"

    def replace(match: re.Match[str]) -> str:
        raw_target = match.group("target")
        target = raw_target.strip("<>")
        fragment = ""
        if "#" in target:
            target, fragment_value = target.split("#", 1)
            fragment = "#" + fragment_value
        resolved = resolve_local_target(source_path, target)
        if resolved is None:
            return match.group(0)

        relative = resolved.relative_to(BOOK_ROOT)
        if resolved.suffix.lower() == ".md" and relative in source_outputs:
            replacement = source_outputs[relative] + fragment
        elif resolved.suffix.lower() == ".pdf":
            replacement = f"{REPOSITORY}/raw/refs/heads/master/{BOOK_REPOSITORY_PATH}/{relative.as_posix()}" + fragment
        elif resolved.suffix.lower() != ".md":
            digest = hashlib.sha1(relative.as_posix().encode("utf-8")).hexdigest()[:8]
            destination = media_dir / f"{digest}-{resolved.name}"
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(resolved, destination)
            replacement = f"media/{destination.name}" + fragment
        else:
            return match.group(0)
        return match.group("prefix") + replacement + match.group("rest")

    return MARKDOWN_LINK.sub(replace, text)


def markdown_list(items: list[str]) -> str:
    return "\n".join(f"- {item}" for item in items) if items else "- None required"


def generate_code_page(
    volume: dict[str, Any],
    source_entries: list[dict[str, Any]],
    volume_dir: Path,
    book_pdf: str,
) -> tuple[str, int]:
    rows = []
    total_fences = 0
    for entry in source_entries:
        count = len(JAVA_FENCE.findall(entry["text"]))
        total_fences += count
        if count:
            rows.append(f"| [{entry['title']}]({entry['output']}) | {count} | {entry['group']} |")

    companion = volume.get("code_companion")
    companion_section = ""
    if companion:
        companion_path = BOOK_ROOT / companion["path"]
        companion_text = companion_path.read_text(encoding="utf-8")
        companion_source = source_url(Path(companion["path"]))
        companion_section = f"""
## Standalone companion

**{companion['title']}** — {companion['description']}

[Open the canonical `.java` source]({companion_source}){{ .md-button }}

```java linenums="1"
{companion_text.rstrip()}
```
"""

    table = "\n".join(rows) if rows else "| No fenced Java snippets | 0 | Use the linked implementation references |"
    page = f"""# Code and Implementation Index

{context_bar(label=f"{volume['segment_code']} · {volume['short_title']}", overview_href='../', code_href=None, pdf_href=book_pdf)}

{study_status(segment_code=volume['segment_code'], book_position=volume['segment_position'], book_count=volume['segment_count'])}

This page indexes the Java examples embedded throughout **{volume['title']}**. The chapter remains the source of truth for contracts, invariants, dry runs, and explanations; this index makes implementations easy to locate.

| Web chapter | Java blocks | Section |
|---|---:|---|
{table}

{companion_section}
## How to use the code

1. Read the contract and dry run before copying an implementation.
2. Recreate the method from memory in a separate file.
3. Test normal, boundary, empty, overflow, aliasing, and adversarial cases where relevant.
4. Compare your code with the canonical version and explain every difference.
5. State complete time, auxiliary-space, output-space, and mutation costs.

The book may contain intentionally incorrect snippets for debugging or output prediction. Their surrounding text identifies the intended learning purpose.
"""
    (volume_dir / "code.md").write_text(page, encoding="utf-8")
    return "Code and implementation index", total_fences + (1 if companion else 0)


def build_library(staging_docs: Path) -> tuple[list[dict[str, Any]], list[Any], int, int]:
    spec = read_json(SERIES_SPEC)
    manifest = read_json(ARTIFACT_MANIFEST)
    artifacts = {str(item["id"]): item for item in manifest["volumes"]}
    volumes = {str(item["id"]): item for item in spec["volumes"]}
    ordered = []
    for global_position, volume_id in enumerate(spec["learning_order"], start=1):
        segment = next(item for item in spec["segments"] if str(volume_id) in item["books"])
        segment_position = [str(item) for item in segment["books"]].index(str(volume_id)) + 1
        volume = dict(volumes[str(volume_id)])
        volume["path_label"] = str(spec["path_labels"][str(volume_id)])
        volume["book_position"] = global_position
        volume["segment_id"] = segment["id"]
        volume["segment_title"] = segment["title"]
        volume["segment_short_title"] = segment["short_title"]
        volume["segment_code"] = f"{segment['code']} {segment_position:02d}"
        volume["segment_position"] = segment_position
        volume["segment_count"] = len(segment["books"])
        ordered.append(volume)

    (staging_docs / "assets").mkdir(parents=True, exist_ok=True)
    shutil.copy2(READER_CSS, staging_docs / "assets" / "book-reader.css")
    shutil.copy2(PORTAL_ICON, staging_docs / "assets" / "s2-mark.svg")
    shutil.copy2(MERMAID_SCRIPT, staging_docs / "assets" / "mermaid.mjs")
    shutil.copy2(THEME_MANAGER_SCRIPT, staging_docs / "assets" / "theme-manager.js")

    nav_groups: dict[str, list[Any]] = defaultdict(list)
    built_books: list[dict[str, Any]] = []
    total_documents = 0
    total_code_examples = 0

    for order, volume in enumerate(ordered, start=1):
        volume_id = str(volume["id"])
        slug = web_slug(volume)
        volume_dir = staging_docs / slug
        volume_dir.mkdir(parents=True, exist_ok=True)
        markdown_sources = [
            Path(source["path"])
            for source in volume.get("sources", [])
            if Path(source["path"]).suffix.lower() == ".md"
        ]
        source_outputs = {
            path: f"{position:02d}-{path.stem}.md"
            for position, path in enumerate(markdown_sources, start=1)
        }

        source_entries: list[dict[str, Any]] = []
        for position, relative_source in enumerate(markdown_sources, start=1):
            absolute_source = BOOK_ROOT / relative_source
            title = source_title(absolute_source)
            output = source_outputs[relative_source]
            raw_text = absolute_source.read_text(encoding="utf-8")
            rewritten = rewrite_links(raw_text, relative_source, source_outputs, volume_dir)
            rewritten = HEADING.sub(f"# {title}", rewritten, count=1)
            previous_href = "../" if position == 1 else f"../{Path(source_outputs[markdown_sources[position - 2]]).stem}/"
            previous_label = "Book overview" if position == 1 else source_title(BOOK_ROOT / markdown_sources[position - 2])
            if position < len(markdown_sources):
                next_source = markdown_sources[position]
                next_title = source_title(BOOK_ROOT / next_source)
                next_href = f"../{Path(source_outputs[next_source]).stem}/"
                next_label = next_title
            else:
                next_href = "../code/"
                next_label = "Code and implementation index"
            page = f"""{context_bar(label=f"{volume['segment_code']} · {volume['short_title']}", overview_href='../', code_href='../code/', pdf_href=pdf_url(volume['output_name']))}

{study_status(segment_code=volume['segment_code'], book_position=volume['segment_position'], book_count=volume['segment_count'], chapter_position=position, chapter_count=len(markdown_sources))}

{rewritten.rstrip()}

---

<div class="book-reader-endnote"><strong>Source:</strong> <a href="{source_url(relative_source)}">canonical Markdown</a></div>

{reader_pagination(previous_href, previous_label, next_href, next_label)}
"""
            (volume_dir / output).write_text(page, encoding="utf-8")
            source_entries.append(
                {
                    "title": title,
                    "output": output,
                    "group": source_group(relative_source),
                    "text": raw_text,
                }
            )

        code_title, code_examples = generate_code_page(
            volume, source_entries, volume_dir, pdf_url(volume["output_name"])
        )
        total_code_examples += code_examples
        total_documents += len(source_entries)

        grouped_sources: dict[str, list[dict[str, Any]]] = defaultdict(list)
        for entry in source_entries:
            grouped_sources[entry["group"]].append(entry)
        reading_sections = []
        for group in ("Chapters", "Core reading", "Practice", "Solutions"):
            entries = grouped_sources.get(group, [])
            if not entries:
                continue
            reading_sections.append(f"## {group}\n\n" + "\n".join(
                f"{volume['segment_code']}.{index:02d} [{entry['title']}]({entry['output']})"
                for index, entry in enumerate(entries, start=1)
            ))

        artifact = artifacts[volume_id]
        quick_href = ""
        quick_links = []
        # The generated catalog is available before this builder runs.
        catalog_path = ROOT / "apps" / "portal" / "content" / "books.json"
        if catalog_path.is_file():
            catalog = read_json(catalog_path)
            catalog_book = next((book for book in catalog["books"] if book["id"] == volume_id), None)
            if catalog_book:
                quick_links = catalog_book.get("webReads", [])
        if quick_links:
            quick_href = "".join(
                f'<a href="../../{item["href"]}">{safe_html(item["label"])}</a>'
                for item in quick_links
            )

        source_word_count = sum(len(re.findall(r"\b[\w'-]+\b", entry["text"])) for entry in source_entries)
        index_page = f"""# {volume['title']}

{context_bar(label=f"{volume['segment_code']} · {volume['short_title']}", overview_href=None, code_href='code/', pdf_href=pdf_url(volume['output_name']))}

{study_status(segment_code=volume['segment_code'], book_position=volume['segment_position'], book_count=volume['segment_count'])}

<div class="book-identity">
  <span>{safe_html(volume['segment_title'])}</span>
  <span>{safe_html(volume['segment_code'])}</span>
  <span>BOOK {volume['segment_position']} OF {volume['segment_count']}</span>
  <span>{'ROADMAP EDITION' if volume.get('publication_status') == 'planned' else 'FULL EDITION'}</span>
  <span>{artifact['page_count']} PDF PAGES</span>
  <span>{len(source_entries)} WEB DOCUMENTS</span>
  <span>{source_word_count:,} WORDS</span>
  <span>{code_examples} CODE ENTRIES</span>
</div>

## {volume['subtitle']}

{volume['purpose']}

<div class="book-primary-actions">
  <a href="{Path(source_entries[0]['output']).stem}/" class="md-button md-button--primary">Start reading</a>
  <a href="code/" class="md-button">Browse code</a>
  <a href="{pdf_url(volume['output_name'])}" class="md-button">Download PDF</a>
  {quick_href}
</div>

## Prerequisites

{markdown_list(list(volume.get('prerequisites', [])))}

## Learning outcomes

{markdown_list(list(volume.get('outcomes', [])))}

{"\n\n".join(reading_sections)}

## Code

- [{code_title}](code.md)
- [Canonical source directory]({REPOSITORY}/tree/master/{BOOK_REPOSITORY_PATH}/{Path(volume['sources'][0]['path']).parent.as_posix()})
"""
        (volume_dir / "index.md").write_text(index_page, encoding="utf-8")

        book_nav: list[Any] = [{"Overview": f"{slug}/index.md"}]
        for chapter_position, entry in enumerate(source_entries, start=1):
            book_nav.append({f"{volume['segment_code']}.{chapter_position:02d} · {entry['title']}": f"{slug}/{entry['output']}"})
        book_nav.append({"Code": f"{slug}/code.md"})

        nav_groups[volume["segment_title"]].append({f"{volume['segment_code']} · {volume['short_title']}": book_nav})
        built_books.append(
            {
                "id": volume_id,
                "path_label": volume["path_label"],
                "book_position": volume["book_position"],
                "segment_id": volume["segment_id"],
                "segment_title": volume["segment_title"],
                "segment_code": volume["segment_code"],
                "segment_position": volume["segment_position"],
                "segment_count": volume["segment_count"],
                "publication_status": volume.get("publication_status", "published"),
                "slug": slug,
                "title": volume["title"],
                "page_count": int(artifact["page_count"]),
                "documents": len(source_entries),
                "code_examples": code_examples,
                "word_count": source_word_count,
                "pdf": pdf_url(volume["output_name"]),
            }
        )

        legacy_slug = f"{volume_id.lower()}-{volume['slug']}"
        if legacy_slug != slug:
            legacy_dir = staging_docs / legacy_slug
            legacy_dir.mkdir(parents=True, exist_ok=True)
            (legacy_dir / "index.md").write_text(
                f'''<meta http-equiv="refresh" content="0; url=../{slug}/">

# This book moved

[Continue to {volume['segment_code']}: {volume['title']}](../{slug}/index.md)
''',
                encoding="utf-8",
            )

    library_rows = "\n".join(
        f"| {book['segment_title']} | {book['segment_code']} | [{book['title']}]({book['slug']}/index.md) | {book['segment_position']} of {book['segment_count']} | {'Roadmap' if book['publication_status'] == 'planned' else 'Full'} | {book['documents']} | {book['code_examples']} | {book['page_count']} | [PDF]({book['pdf']}) |"
        for book in built_books
    )
    library_page = f"""# Java SDE-2 Learning Library

Choose **Java Engineering**, **Data Structures and Algorithms**, or **System Design and Backend**. Follow the books inside your selected segment in order. Every book is available on the web and as a matching PDF; these are two formats of the same curriculum. The library contains **{len(built_books)} focused books**, **{total_documents} web documents**, and **{total_code_examples} indexed code entries** generated from canonical sources.

!!! tip "Choose before you begin"
    New to Java? Start with JAVA 01. Preparing for coding rounds? Start with DSA 01. Preparing for backend and architecture rounds? Start with SD 01. A roadmap edition shows committed scope and ordering while its complete chapter set is developed.

<div class="library-route-grid">
  <a href="01-java-foundations-for-problem-solving/"><strong>JAVA 01</strong><span>Language, tooling, runtime, and Java engineering</span></a>
  <a href="02-time-and-space-complexity/"><strong>DSA 01</strong><span>Complexity, patterns, structures, and algorithms</span></a>
  <a href="18f-design-backend-testing-and-security/"><strong>SD 01</strong><span>Backend foundations, data, Spring, and distributed systems</span></a>
</div>

| Segment | Code | Continue on the web | Book | Edition | Chapters | Code | PDF pages | Offline |
|---|---:|---|---:|---|---:|---:|---:|---|
{library_rows}
"""
    (staging_docs / "index.md").write_text(library_page, encoding="utf-8")

    nav = [{"Choose a Segment": "index.md"}]
    for name in ("Java Engineering", "Data Structures and Algorithms", "System Design and Backend"):
        nav.append({name: nav_groups[name]})
    return built_books, nav, total_documents, total_code_examples


def write_config(path: Path, docs_dir: Path, nav: list[Any]) -> None:
    config: dict[str, Any] = {
        "site_name": "Java SDE2 Learning Library",
        "site_description": "Complete Java SDE-2 books rendered from canonical Markdown with code, exercises, solutions, and PDF downloads.",
        "site_author": "Vinay Reddy Kalluri and contributors",
        "site_url": f"{PRODUCTION_ROOT}/books/",
        "docs_dir": str(docs_dir),
        "use_directory_urls": True,
        "theme": {
            "name": "material",
            "custom_dir": str(ROOT / "tooling" / "mkdocs-overrides"),
            "language": "en",
            "logo": "assets/s2-mark.svg",
            "favicon": "assets/s2-mark.svg",
            "features": [
                "navigation.indexes",
                "navigation.sections",
                "navigation.top",
                "navigation.footer",
                "toc.follow",
                "search.suggest",
                "search.highlight",
                "content.code.copy",
                "content.code.annotate",
            ],
            "palette": {"scheme": "default", "primary": "slate", "accent": "amber"},
            "font": {"text": "Source Serif 4", "code": "IBM Plex Mono"},
        },
        "plugins": ["search"],
        "extra": {"surface": "books"},
        "extra_css": ["assets/book-reader.css"],
        "extra_javascript": ["assets/mermaid.mjs"],
        "markdown_extensions": [
            "admonition",
            "footnotes",
            "attr_list",
            "def_list",
            "pymdownx.details",
            "pymdownx.tasklist",
            "pymdownx.highlight",
            "pymdownx.superfences",
            "pymdownx.inlinehilite",
            "pymdownx.tabbed",
            {"toc": {"permalink": True, "toc_depth": 3}},
        ],
        "nav": nav,
        "copyright": "Copyright © 2026 Vinay Reddy Kalluri and contributors. Content licensed CC BY 4.0.",
    }
    path.write_text(yaml.safe_dump(config, sort_keys=False, allow_unicode=True), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--site-dir", type=Path, default=ROOT / "site" / "books")
    args = parser.parse_args()
    site_dir = args.site_dir.resolve()

    with tempfile.TemporaryDirectory(prefix="sde2-book-web-") as temporary:
        work = Path(temporary)
        docs_dir = work / "docs"
        docs_dir.mkdir()
        books, nav, documents, code_examples = build_library(docs_dir)
        config_path = work / "mkdocs.yml"
        write_config(config_path, docs_dir, nav)
        subprocess.run(
            [
                sys.executable,
                "-m",
                "mkdocs",
                "build",
                "--strict",
                "--clean",
                "--config-file",
                str(config_path),
                "--site-dir",
                str(site_dir),
            ],
            cwd=ROOT,
            check=True,
        )

    missing = []
    for book in books:
        base = site_dir / book["slug"]
        for expected in (base / "index.html", base / "code" / "index.html"):
            if not expected.is_file():
                missing.append(expected)
    if missing:
        for path in missing:
            print(f"Missing generated web-book page: {path}", file=sys.stderr)
        return 1

    manifest = {
        "schemaVersion": 1,
        "generatedFrom": f"{BOOK_REPOSITORY_PATH}/publishing/series.json",
        "bookCount": len(books),
        "documentCount": documents,
        "codeEntryCount": code_examples,
        "books": books,
    }
    (site_dir / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(
        f"Built complete web-book library: {len(books)} books, "
        f"{documents} documents, {code_examples} code entries"
    )
    print(f"  Library: {site_dir / 'index.html'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
