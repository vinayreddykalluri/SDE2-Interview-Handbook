# DSA 08-17 Build Report

## Build result

Existing command used for each affected volume:

```bash
python3 scripts/build_series.py --volume <08..17> --skip-index
```

| Book | Previous pages | Final pages | Change | Web documents | Indexed words | Indexed code entries |
|---|---:|---:|---:|---:|---:|---:|
| DSA 08 Hashing | 44 | 60 | +16 | 8 | 14,270 | 25 |
| DSA 09 Recursion | 24 | 37 | +13 | 5 | 6,324 | 9 |
| DSA 10 Linked Lists | 25 | 36 | +11 | 5 | 5,792 | 12 |
| DSA 11 Ordering Structures | 24 | 36 | +12 | 5 | 5,703 | 7 |
| DSA 12 Binary Search | 23 | 36 | +13 | 5 | 5,594 | 9 |
| DSA 13 Trees | 26 | 38 | +12 | 5 | 6,158 | 10 |
| DSA 14 Heaps | 25 | 36 | +11 | 5 | 5,809 | 11 |
| DSA 15 Graphs | 33 | 45 | +12 | 5 | 6,834 | 10 |
| DSA 16 Greedy | 28 | 39 | +11 | 5 | 6,219 | 8 |
| DSA 17 Dynamic Programming | 32 | 43 | +11 | 5 | 6,640 | 8 |
| **Total** | **284** | **406** | **+122** | **53** | **69,343** | **109** |

## Web result

Commands:

```bash
python3 tooling/automation/sync_book_catalog.py
python3 tooling/automation/build_site.py
```

The generated web library contains 40 books, 213 canonical documents, and 945 indexed code entries. DSA 08-17 overview pages expose foundation, pattern, interview, practice, solution, code, and PDF paths from the same manifest order.

## PDF QA

`qa_semantic_layout.py` scanned 10 PDFs and 406 pages: 0 errors and 10 review warnings. Each warning was the expected repeated header on the two-page series-roadmap table. Poppler renders of every cover, Chapter 1 start, interview-round start, and both sides of every warned table continuation were visually inspected. No clipped text, orphan chapter heading, code overflow, broken table, missing footer, or malformed continuation was found.

## Library totals

- Focused books: 40 PDFs and 2,220 pages
- Series index: 17 pages
- Master book: 616 pages
- Complete library: 42 PDFs and 2,853 pages

The series index and master book were not rebuilt because their canonical content did not change; their published page counts remain valid. The ten focused PDFs and artifact manifest were rebuilt.
