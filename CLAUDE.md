# CLAUDE.md — SDE2-Interview-Handbook

Working context for AI agents and contributors. Audited 2026-08-02 against `master` @ `a6203e3`, then updated the same day after the reproducibility and Java-coverage work described in §5.

---

## 1. What this repo is

A local-first, open-source Java SDE-2 interview preparation system. Markdown is the single source of truth; everything else (MkDocs site, standalone book web library, 42 PDFs, DOCX) is **generated** from it via Python tooling.

- ~1,110 tracked files (excluding `.git`), 713 Markdown files, 176 Java files, 43 PDFs.
- 185 MB working tree / 67.6 MB packfile — **163 MB of that is `books/.../dist/*.pdf` committed to git**.
- Licensing is split: MIT for code, CC BY 4.0 for content.

## 2. Repository map

```
apps/portal/          Static portal (vanilla HTML/CSS/JS + 3 JSON content feeds). No build step, no package.json.
books/java-sde2-interview-preparation-series/
  content/master/     Canonical master-book Markdown (616-page reference)
  content/volumes/    48 volume directories across java/ dsa/ frameworks/ system-design/
  publishing/         series.json + author-notes.json — the publishing manifest (source of truth for catalog)
  dist/               43 committed PDFs + manifest.json  (~167 MB)
  scripts/            Book-local build/validate/diagram Python (16 scripts)
  examples/java/      Maven project, package com.interviewbook.examples
  reports/            Per-book audit/build/changelog/coverage/validation reports (historical, append-only)
docs/                 MkDocs site source: backend-interview/ (10 modules), coding-foundations/ (19 modules), community/, project/
examples/java/        Second Java tree, package io.github.vinayreddykalluri.interviewhandbook (javac-only, no pom)
tooling/automation/   Repo-level build + validate Python (17 scripts)
tooling/book-web/     Templates for the standalone book web library
tooling/requirements/ authoring.txt (mkdocs/weasyprint/pypandoc)
Makefile              Entry point for everything
```

## 3. Commands

Everything routes through `make`. Python venv at `.venv`, dependencies split across two requirement files.

```bash
make bootstrap        # macOS only — tooling/automation/bootstrap_macos.sh
make install          # venv + tooling/requirements/authoring.txt
make doctor           # environment preflight
make validate         # 6 validators; skips Java with a warning if javac is absent
make validate-all     # strict path CI uses: all validators + PDF integrity
make validate-pdfs    # PDFs vs dist/manifest.json only
make build-site       # MkDocs -> site/
make build-book-web   # standalone book library -> site/books
make build-books      # full series rebuild: 41 volumes + index + master, then validate
make build-pdf|docx|all
make sync-book-catalog / check-book-catalog   # reconcile series.json <-> portal catalog <-> dist/manifest.json
make verify           # validate + build-site
```

A full series rebuild takes roughly 25 minutes single-threaded. `build_all_volumes.py` parallelizes it and records progress in `build/volume-build-state.json`, so an interrupted run resumes; pass `--fresh` to force a full rebuild. The index reads `dist/manifest.json` and must run *after* every volume.

**The build requires pandoc ≥ 2.10.** Older pandoc emits a 5-field Table AST node and the renderer expects 6. `require_pandoc()` fails with an explanation rather than a tuple-unpacking error.

Book-local (run from `books/java-sde2-interview-preparation-series/`, needs its own `requirements.txt`):

```bash
python scripts/validate_book.py --source-only
python scripts/validate_series.py --source-only
```

**Verified 2026-08-02** (Python 3.10, no JDK present):

| Check | Result |
|---|---|
| `validate_repository_layout` | pass — 6 root sections, 10 backend modules, 42 curriculum pages |
| `validate_structure` | pass — 19 foundation modules, 57 chapters, 138 nav targets |
| `validate_links` | pass — 153 documents |
| `validate_web` | pass — 41 books / 4 segments / 41 focused PDFs reconciled to 43 total |
| `validate_deployment` | pass — pinned static Vercel build publishing `site/` |
| `build_all.py --check-only` | pass |
| `validate_java_examples` | **not run** — requires `javac` 17+ |

The validation layer is genuinely strong and self-consistent. No content/catalog drift found.

## 4. Invariants an agent must not break

0. **Fonts determine pagination, and pagination is asserted.** The build reads only `assets/fonts/` and never system fonts. Changing a font changes page counts in every volume, which invalidates `dist/manifest.json`, the README totals, and the `min_pages`/`max_pages` bands. Change a font → rebuild the whole series → run `validate_pdfs.py`. Never rebuild one volume after a font change.
1. **Markdown is canonical.** Never hand-edit anything in `dist/`, `site/`, or generated web output. Edit `content/` or `docs/`, then rebuild.
2. **Catalog triangle.** `publishing/series.json` ↔ `apps/portal/content/books.json` ↔ `books/.../dist/manifest.json` must agree on titles, segment assignment, counts, and ordering. Change one → run `make sync-book-catalog` → `make check-book-catalog`.
3. **Two independent Java trees.** `examples/java` (`io.github.vinayreddykalluri.*`, javac + `-Xlint:all -Werror`, `--release 21`) and `books/.../examples/java` (`com.interviewbook.*`, Maven). They are *not* mirrors. Know which one you are in.
4. **`master` is the only publishing branch** and is protected. CI runs on PR and push to master.
5. Root has exactly six intentional sections (`apps/ books/ docs/ examples/ tooling/` + root files) — `validate_repository_layout.py` enforces this. New top-level directories fail the build.

## 5. Audit findings

### Resolved on 2026-08-02

**R1 — The PDF build was silently pinned to pandoc ≥ 2.10, and CI would have crashed on it.** `build_book.py` reads pandoc's native JSON AST and unpacks a Table node as a 6-tuple — a shape introduced in pandoc-types 1.21 (pandoc 2.10). Ubuntu 22.04 ships pandoc 2.9.2.1, which emits a 5-field Table, so the build died with `ValueError: not enough values to unpack (expected 6, got 5)` on the first table in the source. `build-books.yml` installed pandoc with a bare `apt-get install -y pandoc`, so **re-enabling release automation as previously written would have failed immediately**. Fixed: `require_pandoc()` now checks the version up front and explains the failure, the workflow pins a pandoc `.deb` by version, and both the PDF and DOCX paths call the guard.

**R2 — PDF pagination depended on the host's fonts.** `register_fonts()` searched macOS paths first (Charter, Avenir Next, Menlo) and fell back to DejaVu on Linux. The same unchanged Markdown rendered 127 pages on macOS and 133 on Linux. Since page counts are recorded in `dist/manifest.json` and bounded by `min_pages`/`max_pages`, this was a correctness bug. Fixed: Caladea, Lato, and DejaVu Sans Mono are vendored under `assets/fonts/`, the build reads only those, and a missing font is a hard error rather than a silent substitution. Caladea renders JAVA-02 at 126 pages against Charter's 127, so the typography is materially unchanged.

**R3 — No validator ever opened a PDF.** Added `scripts/validate_pdfs.py`, checking page counts against the declared band, manifest page-count/bytes/SHA-256 agreement, blank and near-empty pages, pages with no extractable text (a font that fails to embed still renders but is invisible to search and screen readers), and stranded headings. Wired into `validate-books.yml` (`--quick`, against committed artifacts) and `build-books.yml` (full, against freshly rendered ones). Verified non-vacuous by fault injection.

**R4 — `build_book_web_library.py` required Python 3.12+.** It used a backslash inside an f-string expression, a `SyntaxError` before PEP 701. CI pins Python 3.11, so the web library could not have been built there. Fixed by hoisting the expressions.

**R5 — Release automation re-enabled.** `build-books.yml` and `deploy-pages.yml` moved into `.github/workflows/`. `deploy-pages.yml` now also triggers on book content and publishing changes; without those paths the site silently served the previous edition after a content change.

**R6 — `make validate` no longer hard-fails without a JDK** (was H3). It skips Java validation with a loud warning; `make validate-all` is the strict path.

**R8 — Vercel and CI environment assumptions fixed.** Three separate environment defects surfaced only once the workflows and the deployment actually ran: the Makefile hardcoded `.venv/bin/python` so every `make` target died instantly in CI; `$(CURDIR)/$(PYTHON)` broke once `PYTHON` resolved absolute; and Vercel's build image now ships a uv-managed Python that refuses `pip install` under PEP 668, so `installCommand` builds a venv. `build-books.yml` is also split into independent `book-series` and `handbook-pdf` jobs so the unverified LaTeX path cannot block the series.

**R9 — Hardcoded catalog counts derived instead.** `validate_web.py` asserted `len(books) != 40` and `totalPdfCount != 42`. Adding a volume failed validation in a place unrelated to the change, and the "fix" was to edit the validator - which is how a checked invariant becomes a number someone bumps to make the build pass. Both are now derived from `publishing/series.json`.

**R10 — SD-03 Generative AI System Design added.** A new system-design volume covering the generative-AI design round: token and latency budgeting, RAG architecture with authorization pushed into the query, agent bounds and idempotency, indirect prompt injection, and evaluating a probabilistic system. 78 pages, with an executable Java model.

**R7 — Java coverage gaps closed.** Added master chapters 55–58 (`java.time`, regular expressions, JPMS, Java 22–25) as new Part IX, wired into the JAVA-04/05/06 volumes at their correct reading positions. `LocalDate` previously appeared nowhere in 15,677 lines of master content.

### Critical

**C1 — 163 MB of PDFs committed to git.** `dist/` holds 42 binary PDFs, several 4–6 MB each, tracked as plain binaries (`.gitattributes` marks them binary but there is **no Git LFS**). Every regeneration writes a new full blob; the packfile is already 67.6 MB at only 32 commits. This compounds — clone time and repo size will degrade roughly linearly with each publish cycle. This is the single highest-cost structural decision in the repo.

C1 is now the *only* thing standing between this repo and a clean release process. With R1–R5 fixed, CI can build and validate the full series unattended, so `dist/` no longer needs to be tracked. That unwind is the next real piece of work and is deliberately left for a deliberate, coordinated commit — see §6.

~~**C2 — Release automation is disabled.**~~ Resolved; see R5.

### High

**H1 — Duplicated Java example infrastructure.** Two source trees, two package roots, two build models (Maven vs raw javac), two smoke-test entry points (`AllExamplesSmokeTest` vs `ExampleSmokeTest`). CI compiles the `com.interviewbook` tree; the `io.github...` tree is validated by a separate script that silently no-ops without a JDK. Divergence risk is high and there is no doc stating which tree a new example belongs in.

**H2 — Duplicated Python tooling.** `tooling/automation/` (17 scripts) and `books/.../scripts/` (16 scripts) both build and validate. Two `requirements.txt` files with overlapping-but-not-identical pins (`tooling/requirements/authoring.txt` pins exact versions; the book file uses `>=` ranges). Nothing enforces that they stay compatible.

~~**H3 — `validate_java_examples` fails open in practice.**~~ Resolved; see R6.

**H4 — Java version inconsistency.** README badge says Java 17+, `validate_java_examples` says "JDK 17 or newer", CI installs Java 21 and compiles with `--release 21`. Anyone on 17 will fail CI on any language feature past 17.

### Medium

**M1 — No Python tests.** Zero `test_*.py` across ~33 tooling scripts including `build_book_web_library.py` (27 KB) and `validate_web.py` (26 KB). The validators are the safety net and nothing validates the validators.

**M2 — `reports/` is unbounded.** 40+ historical per-book audit/build/changelog files under `books/.../reports/`. Useful provenance, but there is no retention policy and it is indistinguishable from live documentation to a newcomer.

**M3 — Framework labs are unexercised.** 8 `labs/maven-demo` Maven projects (MySQL, Hibernate, Spring, Spring Data, Mongo, Redis, Kafka) with JUnit tests. CI never builds them. They can rot silently.

**M4 — macOS-only bootstrap.** `bootstrap_macos.sh` is the only bootstrap path; Linux/WSL contributors have no documented on-ramp despite CI running on Ubuntu.

**M5 — Portal has no dependency or asset pipeline.** `apps/portal` is hand-written JS/CSS. Fine at current size, but there is no minification, no cache-busting, and no test — and it consumes JSON contracts that other tooling generates.

### Low

- `CITATION.cff` and `series.json` carry a release tag and dates that must be bumped manually on each publish; nothing validates their freshness.
- README states hard counts (40 books, 3,372 / 4,006 pages, 403 manifest entries → 396 unique docs) — the 403 vs 396 delta is intentional but undocumented at the point of the claim.
- `docs/community/repository-audit-2026-07.md` is a prior audit; this file supersedes its operational sections.

## 6. Next steps — prioritized

### Now (unblocks everything else)

0. ~~Re-enable release automation.~~ Done — see R5. Step 1 is now unblocked.

1. **Move PDFs out of git.** Two viable paths, pick one:
   - *Preferred:* stop tracking `dist/*.pdf` entirely; build them in CI and publish as GitHub Release assets on tag. README and `docs/books.md` link to the release, not to repo paths. Requires rewriting the many in-repo `books/.../dist/...pdf` links to release URLs.
   - *Cheaper:* adopt Git LFS for `dist/*.pdf`. Keeps link structure intact, still needs a history rewrite (`git filter-repo`) to actually reclaim the 67 MB.
   Either way: history rewrite is a one-time coordinated force-push on a protected branch — do it deliberately, and only after C2 is solved.
2. **Pin one Java version.** Choose 21 (matches CI and `--release 21`), then update the README badge, `validate_java_examples.py`, and the docs together. Note that chapter 58 now documents the Java 22–25 delta, so the *content* baseline and the *build* baseline are separate decisions — say so explicitly wherever the version is stated.

### Next

4. **Consolidate the two Java trees.** Decide the rule — most likely: `books/.../examples/java` for book-embedded snippets, `examples/java` for the standalone runnable library — write it into `docs/community/repository-structure.md`, and add both to CI compilation. If they should actually be one tree, merge now while it is 175 files.
5. **Add pytest coverage for the tooling.** Start with the three highest-risk scripts: `sync_book_catalog.py`, `validate_web.py`, `build_book_web_library.py`. Golden-file tests over a small fixture catalog will catch the drift class of bug that currently only surfaces at publish time.
6. **Build the framework labs in CI.** A nightly or `paths:`-filtered job running `mvn -q test` across the 8 `labs/maven-demo` projects. Cheap insurance on the most-likely-to-rot content.
7. **Consider renumbering the master chapters.** Chapters 55–58 sit in an appended Part IX because inserting them in reading order would have renumbered 30 files and repointed 309 in-text `Chapter NN` references plus 42 `series.json` paths. The focused volumes place them correctly, so this is cosmetic — but if you ever do renumber, do it as one scripted commit with a verification pass over every cross-reference.

### Later

8. Merge or clearly namespace the duplicated tooling layers; single `requirements.txt` with a shared constraints file.
9. Add `bootstrap_linux.sh` (or a devcontainer) so CI's environment is reproducible locally.
10. Archive `reports/` older than the current edition under `reports/archive/<edition>/` and state the retention rule in `reports/README.md`.
11. Automate release-metadata bumps (`CITATION.cff`, `series.json` tag/date) from the git tag rather than by hand.

## 7. Conventions

- Book IDs: `JAVA-01..09`, `DSA-01..17`, `FW-01..12`, `SD-01..03`. Filenames follow `Java-SDE2-<SEG>-<NN>-<Title-In-Kebab>.pdf`.
- Volume directories mirror the ID: `content/volumes/<segment>/<ID>-<slug>/`.
- Book numbering is prerequisite order, not difficulty order — do not renumber without updating `series.json`, the portal catalog, and every cross-reference.
- Reports are named `<TOPIC>_<KIND>_REPORT.md` / `_AUDIT.md` / `_CHANGELOG.md`, screaming snake case.
- Docs use MkDocs Material with Mermaid; diagrams live in `docs/assets/diagrams/` and are generated by `books/.../scripts/generate_*_diagrams.py`.

## 8. Gotchas

- `make` picks `.venv/bin/python` when a venv exists and falls back to the system interpreter otherwise. Targets that `cd` into a subdirectory must use `$(PYTHON_ABS)`, not `$(CURDIR)/$(PYTHON)` — the latter breaks the moment `PYTHON` resolves to an absolute path.
- `build-books.yml` is two independent jobs. `book-series` renders through pandoc and ReportLab; `handbook-pdf` renders through LaTeX. They share no tooling, so a missing TeX package must not be able to stop the series from building.
- `make clean` deletes `site/` and `output/` but never `dist/` — `dist/` is tracked, not build output.
- `mkdocs.yml` nav is validated by `validate_structure.py`; adding a doc without a nav entry fails the build.
- `vercel.json` pins a static build publishing `site/`; `validate_deployment.py` asserts this, so changing the output dir breaks validation.
- The sandbox/CI expects `javac` on PATH. Locally, `make validate` will hard-fail without a JDK — see next-step 5.
