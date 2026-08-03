# Bundled publication fonts

These fonts are vendored so that a PDF built on Linux, macOS, or CI paginates
identically. Before they existed, `register_fonts()` fell back to whatever the
host provided — macOS resolved Charter/Avenir Next/Menlo, Linux resolved
DejaVu — and the same unchanged Markdown produced different page counts
(JAVA-02 rendered 127 pages on macOS and 133 on Linux). Page counts are
recorded in `dist/manifest.json` and asserted against `min_pages`/`max_pages`
in `publishing/series.json`, so a font-dependent build is a correctness
problem, not a cosmetic one.

The build now loads only these files. It never reads system fonts, and it
fails loudly if a file is missing rather than silently substituting.

| Role | Family | License | Why this family |
|---|---|---|---|
| Body serif | Caladea | SIL Open Font License 1.1 | Sturdy transitional serif with Cambria metrics; holds up at 9–10pt for sustained reading, which is what a 130-page technical volume needs. |
| Headings and navigation | Lato | SIL Open Font License 1.1 | Humanist sans in the same register as the original Avenir Next selection — warm, geometric, legible in small caps and table headers. |
| Code | DejaVu Sans Mono | Bitstream Vera / DejaVu license (permissive) | Menlo descends from Bitstream Vera Sans Mono, so code blocks keep almost exactly the character width the series was designed around. |

All three licenses permit redistribution, including inside this repository and
inside the generated PDFs.

## Adding or replacing a font

1. Drop the `.ttf` into this directory.
2. Register it in `FONT_FILES` in `scripts/build_book.py`.
3. Rebuild the full series — **do not rebuild one volume**. Font metrics change
   pagination everywhere, and `dist/manifest.json`, the README page totals, and
   the series index all have to move together.
4. Run `python scripts/validate_pdfs.py` to confirm every volume still lands
   inside its declared page band.

Variable fonts are not supported: ReportLab's `TTFont` reads a single static
instance, so a variable file renders at its default axis position and silently
ignores weight. Use static cuts.
