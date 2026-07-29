# DSA 08-17 Build Report

## Build result

Existing command used for each affected volume:

```bash
python3 scripts/build_series.py --volume <08..17> --skip-index
```

| Book | Previous pages | Final pages | Change | Web documents | Indexed words | Indexed code entries |
|---|---:|---:|---:|---:|---:|---:|
| DSA 08 Hashing | 44 | 64 | +20 | 9 | 15,146 | 26 |
| DSA 09 Recursion | 24 | 43 | +19 | 6 | 7,312 | 10 |
| DSA 10 Linked Lists | 25 | 42 | +17 | 6 | 6,605 | 13 |
| DSA 11 Ordering Structures | 24 | 41 | +17 | 6 | 6,467 | 8 |
| DSA 12 Binary Search | 23 | 41 | +18 | 6 | 6,518 | 10 |
| DSA 13 Trees | 26 | 42 | +16 | 6 | 7,052 | 11 |
| DSA 14 Heaps | 25 | 41 | +16 | 6 | 6,785 | 12 |
| DSA 15 Graphs | 33 | 50 | +17 | 6 | 7,841 | 11 |
| DSA 16 Greedy | 28 | 43 | +15 | 6 | 6,962 | 9 |
| DSA 17 Dynamic Programming | 32 | 48 | +16 | 6 | 7,462 | 9 |
| **Total** | **284** | **455** | **+171** | **63** | **78,150** | **119** |

## Web result

Commands:

```bash
python3 tooling/automation/sync_book_catalog.py
python3 tooling/automation/build_site.py
```

The generated web library contains 40 books, 223 canonical documents, and 955 indexed code entries. DSA 08-17 overview pages expose foundation, core patterns, essential clinics, interview rounds, practice, solutions, code, and PDF paths from the same manifest order.

## PDF QA

`qa_semantic_layout.py` scanned 10 PDFs and 455 pages: 0 errors and 10 review warnings. Each warning was the expected repeated header on the two-page series-roadmap table. All 42 pages in the initial clinic render were inspected. Three single-answer tail pages were found, tightened, rebuilt, and re-inspected; the final clinics occupy 39 balanced pages. The final PDFs contain no clipped text, orphan chapter heading, code overflow, broken table, missing footer, malformed continuation, or unintended sparse tail page.

## Library totals

- Focused books: 40 PDFs and 2,269 pages
- Series index: 17 pages
- Master book: 616 pages
- Complete library: 42 PDFs and 2,902 pages

The series index and master book were not rebuilt because their canonical content did not change; their published page counts remain valid. The ten focused PDFs and artifact manifest were rebuilt.
