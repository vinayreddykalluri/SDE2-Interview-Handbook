# Repository Organization Audit — July 2026

## Outcome

The handbook now has one discoverable home for each kind of work: curriculum under `docs/`, runnable website examples under `examples/`, the publication workspace under `books/`, static portal code under `web/`, and repository-wide automation under `scripts/`. The book workspace no longer mixes dozens of audits, master chapters, focused volumes, build metadata, Java projects, and release PDFs at one level.

The website now reads a generated catalog derived from the same publishing manifest and PDF integrity manifest that build the books. This removes the former gap in which the portal described only the backend track and showed stale source counts while 30 published PDFs were difficult to discover.

## Audited inventory

At the audit point the repository contained 336 Markdown files, 106 Java files, and 30 versioned PDFs. The published set contains 28 focused books, one series index, and one 616-page master book, totaling 2,452 pages. The searchable handbook contains 19 ordered coding-foundation modules plus the 10-module backend interview track.

## Findings and resolutions

| Finding | Risk | Resolution |
|---|---|---|
| Book root mixed roughly 40 reports and policy copies with canonical source | Contributors could not tell what to edit | Moved content, reports, publishing configuration, assets, examples, and artifacts into named sections |
| Master and focused sources used unrelated `book/` and `series/volumes/` names | Source ownership was opaque | Consolidated under `content/master/` and `content/volumes/` |
| Build output and public artifacts were nested below `series/` while the master PDF lived at root | Downloads and automation used inconsistent paths | Consolidated reviewed artifacts and `manifest.json` under `dist/` |
| PDF files were treated as textual diffs by Git | Pull requests produced thousands of meaningless object-offset lines | Added `.gitattributes` binary rules for PDF, DOCX, and PNG artifacts |
| Book package duplicated root governance, contribution, license, citation, security, and support files | Policy changes could drift | Kept GitHub-standard policy files at repository root and linked to them from the book workspace |
| Several historical audit claims overstated later DSA volume depth | Contributors had no reliable backlog | Corrected the comprehensive audit and documented volumes 07–17 as published baselines needing expansion |
| Portal exposed no structured book library | Readers could not navigate the publication from the web experience | Added a searchable, ordered catalog with page counts, source links, and release downloads |
| Portal book data could be hand-maintained and drift | Website and PDFs could disagree | Added `scripts/sync_book_catalog.py` and validation of generated `web/content/books.json` |
| Validation encoded legacy book paths | Reorganization could silently break CI | Updated book scripts, CI paths, and repository-layout contracts |

## Book workspace migration map

| Previous path | Canonical path |
|---|---|
| `book/` | `content/master/` |
| `series/volumes/` | `content/volumes/` |
| `code-examples/` | `examples/java/` |
| `diagrams/` | `assets/diagrams/` |
| `docs/images/` | `assets/covers/` |
| `series/series.json` | `publishing/series.json` |
| `series/assets/` | `publishing/assets/` |
| `series/dist/` and root master artifacts | `dist/` |
| root audit/build/coverage files | `reports/<category>/` |
| `CONTENT_SPEC.md` | `docs/editorial-standard.md` |
| `SERIES_ROADMAP.md` | `docs/roadmap.md` |

The old paths were moved with Git-aware renames. Removed duplicate policy files remain recoverable from Git history.

## Canonical synchronization contracts

### Coding-foundation module

Keep the module directory under `docs/coding-foundations/`, its `mkdocs.yml` entry, `web/content/coding-foundations.json`, and its semantic Java package under `examples/java/` aligned.

### Published book

Edit the canonical Markdown under `books/java-sde2-interview-preparation-series/content/`. Update `publishing/series.json` only for ordering, metadata, or source mapping. Rebuild the affected PDF and manifest, then run `make sync-book-catalog`. `web/content/books.json` is generated and must not be edited directly.

### Policy and authorship

Repository-wide `AUTHORS.md`, `CONTRIBUTING.md`, `GOVERNANCE.md`, licenses, citation, conduct, security, and support files are canonical. The book README links to them. Vinay Reddy Kalluri remains creator, founding author, Editor-in-Chief, and Chief Auditor; accepted original contributors retain individual credit.

## Open educational backlog

Java Foundations, Complexity, Number Systems, Bit Manipulation, Loop Mastery, and Arrays provide the strongest early path. Focused volumes 07–17 are valid published baselines but need deeper worked examples, diagrams, Java companions, exercise sets, and separated solutions. GitHub issues tagged `book`, `editorial`, and `help wanted` are the public coordination layer for those improvements.

## Validation gate

Run these commands before proposing a structural or publication change:

```bash
make validate
make build-site
cd books/java-sde2-interview-preparation-series
python3 scripts/validate_book.py --source-only
python3 scripts/validate_series.py --source-only
```

For affected book content, also rebuild the relevant PDF and inspect the rendered pages. For portal changes, inspect desktop and mobile layouts locally.

## Executed verification

The reorganization was tested from a clean clone rather than the author's diverged local checkout.

| Check | Result |
|---|---|
| Repository validation | Pass: 10 backend modules, 42 backend curriculum pages, 19 foundation modules, and 57 foundation chapters |
| Root Java validation | Pass: 81 examples plus the smoke-test source |
| Portal contract | Pass: 19 modules, 69 foundation examples, and 30 published PDFs |
| Unified static site | Pass: strict MkDocs build plus portal assembly under `site/` |
| Master-book validation | Pass: 54 chapters, seven appendices, Java compilation, DOCX, and 616-page PDF |
| Focused-series validation | Pass: 134 mapped Markdown sources, 19 focused Java classes, 29 focused/index PDFs, and 1,836 pages |
| Number Systems code | Pass: 24 standalone snippets and 820 boundary assertions |
| Semantic PDF QA | Pass: all 30 PDFs and 2,452 pages; zero errors and zero warnings |
| Visual PDF sampling | Pass: index, Java, Complexity, and Binary Search covers plus contents, code, and algorithm pages |
| Portal visual sampling | Pass at 1,440-pixel desktop and 390-pixel mobile widths; 28 cards, functional filtering, no horizontal overflow, and no console warnings/errors |
