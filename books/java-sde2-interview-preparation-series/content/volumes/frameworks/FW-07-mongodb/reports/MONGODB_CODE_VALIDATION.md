# MongoDB Code Validation — Backend Wave 3

## Results

- Java 21 dependency-free companions: **1/1 compiled with `-Xlint:all -Werror` and executed**.
- Official MongoDB Java sync-driver command tests: **7 passed, 0 failed/errors/skipped**.
- Observed output: `MongoDbInterviewCompanion checks passed`.

```bash
bash content/volumes/frameworks/FW-07-mongodb/labs/validate_mongodb_labs.sh
```

Tests prove BSON numeric/date/null/array types, versioned update filter/operators, unique index options/order, two-field cursor boundary, aggregation stage order, explicit transaction concerns, and compound cursor index shape. They do not prove a live query plan, election, transaction, sharding, or storage behavior.

## Exact root source array

Set each entry `series_native: true`, in this order:

```text
content/volumes/frameworks/FW-07-mongodb/chapters/00-learning-path-and-document-first-principles.md
content/volumes/frameworks/FW-07-mongodb/chapters/01-bson-crud-validation-null-arrays-and-update-semantics.md
content/volumes/frameworks/FW-07-mongodb/chapters/02-document-modeling-embedding-references-growth-and-atomicity.md
content/volumes/frameworks/FW-07-mongodb/chapters/03-indexes-multikey-plans-and-pagination.md
content/volumes/frameworks/FW-07-mongodb/chapters/04-aggregation-pipelines-memory-and-result-grain.md
content/volumes/frameworks/FW-07-mongodb/chapters/05-replication-read-write-concerns-transactions-and-retries.md
content/volumes/frameworks/FW-07-mongodb/chapters/06-sharding-change-streams-migrations-operations-and-security.md
content/volumes/frameworks/FW-07-mongodb/chapters/07-java-driver-spring-data-boundaries-testing-and-diagnostics.md
content/volumes/frameworks/FW-07-mongodb/chapters/08-live-interviews-rapid-qa-practice-solutions-and-sources.md
```

```json
"code_companion": {
  "path": "content/volumes/frameworks/FW-07-mongodb/code/MongoDbInterviewCompanion.java",
  "title": "Java 21 MongoDB Interview Reasoning Companion",
  "description": "Executable models for document boundaries, optimistic filters, cursor ordering, transaction retry labels, and idempotent change projections."
}
```

Set `publication_status: "published"`, `volume_label: "Publication Edition"`, `min_pages: 22`, and `max_pages: 90`.

After root integration:

```bash
python3 scripts/validate_series.py --source-only
python3 scripts/build_series.py --volume MONGO
python3 scripts/validate_series.py
```
