# Stage 1 - Number Systems and Math Foundations

Stage 1 is delivered in two linked PDFs because the complete learning path is easier to navigate as foundations plus an interview workbook:

- Part A: Chapters 1-13, concepts and foundations
- Part B: Chapters 14-16 plus focused 14A/15A modules, 52 implementations, Java traps, expanded practice, delayed solutions, cheat sheet, and readiness assessment

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
python3 scripts/build_series.py --volume 01 --skip-index
python3 scripts/build_series.py --volume 01B --skip-index
mkdir -p series/tmp/pdfs/number-systems-targeted/01
mkdir -p series/tmp/pdfs/number-systems-targeted/01B
pdftoppm -png -r 120 series/dist/Java-SDE2-DSA-01-Number-Systems-and-Math-Foundations.pdf series/tmp/pdfs/number-systems-targeted/01/page
pdftoppm -png -r 120 series/dist/Java-SDE2-DSA-01B-Number-Systems-Interview-Workbook.pdf series/tmp/pdfs/number-systems-targeted/01B/page
```

These commands rebuild and visually inspect only the two Number Systems PDFs. The series index and unrelated volumes remain untouched because Stage 1 titles, filenames, and sibling navigation are stable.
