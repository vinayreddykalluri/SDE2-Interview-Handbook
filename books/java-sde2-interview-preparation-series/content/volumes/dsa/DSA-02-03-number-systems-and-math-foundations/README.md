# DSA 02-03 — Number Systems and Math Foundations

The shared workspace publishes two consecutive DSA books because the material is easier to navigate as foundations followed by an interview workbook:

- `DSA-02`: Chapters 1-13, concepts and foundations
- `DSA-03`: Chapters 14-16 plus focused 14A/15A modules, 52 implementations, Java traps, expanded practice, delayed solutions, cheat sheet, and readiness assessment

## Source layout

```text
chapters/   18 canonical Markdown learning modules
assets/     seventeen educational figures and one QA contact sheet
code/       companion algorithms and an 820-assertion boundary test
exercises/  practice navigation
solutions/  solution and implementation map
```

## Build and validate

```bash
python3 scripts/generate_number_system_diagrams.py --contact-sheet
bash scripts/validate_number_system_examples.sh
# The --volume arguments below are stable internal manifest IDs.
python3 scripts/build_series.py --volume 01 --skip-index
python3 scripts/build_series.py --volume 01B --skip-index
mkdir -p tmp/pdfs/number-systems-targeted/DSA-02
mkdir -p tmp/pdfs/number-systems-targeted/DSA-03
pdftoppm -png -r 120 dist/02-dsa/Java-SDE2-DSA-02-Number-Systems-and-Math-Foundations.pdf tmp/pdfs/number-systems-targeted/DSA-02/page
pdftoppm -png -r 120 dist/02-dsa/Java-SDE2-DSA-03-Number-Systems-Interview-Workbook.pdf tmp/pdfs/number-systems-targeted/DSA-03/page
```

These commands rebuild and visually inspect only `DSA-02` and `DSA-03`. The series index and unrelated books remain untouched; public titles, filenames, and sibling navigation remain stable.
