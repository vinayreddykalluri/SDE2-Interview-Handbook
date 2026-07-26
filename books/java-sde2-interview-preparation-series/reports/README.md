# Publication Reports

This directory keeps evidence and historical decisions separate from reader-facing source. Reports describe a release; they are not canonical lesson content.

| Section | Use it for |
|---|---|
| [`audits/`](audits/) | Content-quality findings and series-wide review conclusions |
| [`coverage/`](coverage/) | Required-topic matrices and final coverage status |
| [`validation/`](validation/) | Java compilation, execution, and output evidence |
| [`build/`](build/) | PDF artifact inventory, page counts, and inspection results |
| [`changes/`](changes/) | Concise records of substantive educational changes |
| [`planning/`](planning/) | Historical restructuring plans and release checklists |

When improving a book, edit its canonical source under `../content/`, validate it, rebuild the affected PDF under `../dist/`, and update only the reports whose claims changed. Do not add reports to the book-workspace root.
