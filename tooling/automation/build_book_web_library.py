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

MARKDOWN_LINK = re.compile(r"(?P<prefix>\]\()(?P<target><?[^)\s>]+>?)(?P<rest>[^)]*\))")
JAVA_FENCE = re.compile(r"^```java(?:\s|$)", re.MULTILINE | re.IGNORECASE)
HEADING = re.compile(r"^#\s+(.+?)\s*$", re.MULTILINE)


def read_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def web_slug(volume: dict[str, Any]) -> str:
    return f"{str(volume['id']).lower()}-{volume['slug']}"


def source_title(path: Path) -> str:
    text = path.read_text(encoding="utf-8")
    match = HEADING.search(text)
    return match.group(1).strip() if match else path.stem.replace("-", " ").title()


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


def toolbar(*, portal_href: str, library_href: str, overview_href: str | None, pdf_href: str) -> str:
    links = [
        f'<a href="{portal_href}">Portal</a>',
        f'<a href="{library_href}">All web books</a>',
    ]
    if overview_href:
        links.append(f'<a href="{overview_href}">Book overview</a>')
    links.append(f'<a href="{pdf_href}">Download PDF</a>')
    return '<nav class="book-reader-toolbar" aria-label="Book reader shortcuts">' + "".join(links) + "</nav>"


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

{toolbar(portal_href='../../../', library_href='../../', overview_href='../', pdf_href=book_pdf)}

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
    ordered = [volumes[str(volume_id)] for volume_id in spec["learning_order"]]

    (staging_docs / "assets").mkdir(parents=True, exist_ok=True)
    shutil.copy2(READER_CSS, staging_docs / "assets" / "book-reader.css")
    shutil.copy2(PORTAL_ICON, staging_docs / "assets" / "s2-mark.svg")
    shutil.copy2(MERMAID_SCRIPT, staging_docs / "assets" / "mermaid.mjs")

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
            next_link = ""
            if position < len(markdown_sources):
                next_source = markdown_sources[position]
                next_title = source_title(BOOK_ROOT / next_source)
                next_link = f'[Continue: {next_title} →]({source_outputs[next_source]}){{ .md-button .md-button--primary }}'
            else:
                next_link = '[Open the code index →](code.md){ .md-button .md-button--primary }'
            page = f"""{toolbar(portal_href='../../../', library_href='../../', overview_href='../', pdf_href=pdf_url(volume['output_name']))}

{rewritten.rstrip()}

---

<div class="book-reader-endnote"><strong>Source:</strong> <a href="{source_url(relative_source)}">canonical Markdown</a></div>

{next_link}
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
                f"{index}. [{entry['title']}]({entry['output']})"
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

{toolbar(portal_href='../../', library_href='../', overview_href=None, pdf_href=pdf_url(volume['output_name']))}

<div class="book-identity">
  <span>LEARNING STEP {safe_html(str(volume['stage']))}</span>
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
        for entry in source_entries:
            book_nav.append({entry["title"]: f"{slug}/{entry['output']}"})
        book_nav.append({"Code": f"{slug}/code.md"})

        if order <= 8:
            track = "Start Here: Foundations"
        elif volume_id.startswith("18"):
            track = "Advanced Java and Backend"
        else:
            track = "DSA and Algorithms"
        nav_groups[track].append({f"{order:02d}. {volume['short_title']}": book_nav})
        built_books.append(
            {
                "id": volume_id,
                "slug": slug,
                "title": volume["title"],
                "page_count": int(artifact["page_count"]),
                "documents": len(source_entries),
                "code_examples": code_examples,
                "word_count": source_word_count,
                "pdf": pdf_url(volume["output_name"]),
            }
        )

    library_rows = "\n".join(
        f"| {index} | [{book['title']}]({book['slug']}/index.md) | {book['documents']} | {book['code_examples']} | {book['page_count']} | [PDF]({book['pdf']}) |"
        for index, book in enumerate(built_books, start=1)
    )
    library_page = f"""# Java SDE-2 Web Book Library

<nav class="book-reader-toolbar" aria-label="Library shortcuts"><a href="../">Portal</a><a href="../docs/books/">Book guide</a><a href="{REPOSITORY}">GitHub</a></nav>

This library renders the complete canonical book Markdown as searchable web chapters. It contains **{len(built_books)} focused books**, **{total_documents} source documents**, and **{total_code_examples} indexed code entries**. Nothing here is a separate prose copy: rebuilding the site reads the same Markdown and Java files used by the PDFs.

!!! tip "Recommended start"
    Begin with Java Foundations, then Time and Space Complexity, Number Systems, Bit Manipulation, Loop Mastery, Arrays, and Strings. Open advanced material only after its prerequisites are dependable.

| Step | Full web book | Documents | Code | PDF pages | Offline |
|---:|---|---:|---:|---:|---|
{library_rows}
"""
    (staging_docs / "index.md").write_text(library_page, encoding="utf-8")

    nav = [{"Library": "index.md"}]
    for name in ("Start Here: Foundations", "DSA and Algorithms", "Advanced Java and Backend"):
        nav.append({name: nav_groups[name]})
    return built_books, nav, total_documents, total_code_examples


def write_config(path: Path, docs_dir: Path, nav: list[Any]) -> None:
    config: dict[str, Any] = {
        "site_name": "Java SDE-2 Web Books",
        "site_description": "Complete Java SDE-2 books rendered from canonical Markdown with code, exercises, solutions, and PDF downloads.",
        "site_author": "Vinay Reddy Kalluri and contributors",
        "site_url": f"{PRODUCTION_ROOT}/books/",
        "repo_url": REPOSITORY,
        "repo_name": "vinayreddykalluri/SDE2-Interview-Handbook",
        "docs_dir": str(docs_dir),
        "use_directory_urls": True,
        "theme": {
            "name": "material",
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
            "palette": [
                {"media": "(prefers-color-scheme: light)", "scheme": "default", "primary": "slate", "accent": "amber"},
                {"media": "(prefers-color-scheme: dark)", "scheme": "slate", "primary": "black", "accent": "amber"},
            ],
            "font": {"text": "Source Serif 4", "code": "IBM Plex Mono"},
        },
        "plugins": ["search"],
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
