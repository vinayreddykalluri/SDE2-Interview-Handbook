# Hibernate/JPA Code Validation — Backend Wave 2

## Results

- Java source baseline: release 21.
- Dependency-free companions: **1 discovered, compiled, and executed** with `-Xlint:all -Werror`.
- Real provider fixture: Hibernate ORM **7.4.1.Final**, H2 **2.4.240**, Jakarta Persistence 3.2 descriptor.
- Maven/JUnit: **7 tests passed, 0 failed, 0 errors, 0 skipped**.
- Output mismatches: 0.

Command:

```bash
bash content/volumes/frameworks/FW-02-hibernate-and-jpa/labs/validate_hibernate_jpa_labs.sh
```

Observed companion output:

```text
HibernateJpaInterviewCompanion checks passed
```

## Tested behavior

1. one Java managed instance per entity identity in one persistence context;
2. dirty checking emits an update and increments `@Version`;
3. orphan removal deletes a privately owned child;
4. lazy traversal uses three statements for two roots while fetch join uses one;
5. two contexts produce a stale optimistic-lock failure;
6. bulk JPQL leaves managed state stale until clear/refetch;
7. projection plus limit returns two rows without entity loading.

Hibernate logs a test-only warning that the built-in pool is not for production; this is intentional for a disposable Java SE fixture. The expected optimistic-lock rollback can also log that a batch was released with pending statements. Neither is a test failure.

## Required root manifest mapping

Replace the removed roadmap with these `series_native: true` sources, in order:

```text
content/volumes/frameworks/FW-02-hibernate-and-jpa/chapters/00-learning-path-sql-before-orm.md
content/volumes/frameworks/FW-02-hibernate-and-jpa/chapters/01-jpa-contract-hibernate-provider-and-bootstrap.md
content/volumes/frameworks/FW-02-hibernate-and-jpa/chapters/02-mapping-identity-values-and-schema-boundaries.md
content/volumes/frameworks/FW-02-hibernate-and-jpa/chapters/03-lifecycle-persistence-context-dirty-checking-and-flush.md
content/volumes/frameworks/FW-02-hibernate-and-jpa/chapters/04-associations-ownership-cascade-orphans-and-aggregate-boundaries.md
content/volumes/frameworks/FW-02-hibernate-and-jpa/chapters/05-proxies-fetch-plans-n-plus-one-and-graph-control.md
content/volumes/frameworks/FW-02-hibernate-and-jpa/chapters/06-jpql-criteria-native-projections-and-pagination.md
content/volumes/frameworks/FW-02-hibernate-and-jpa/chapters/07-batching-locking-caches-equality-and-bulk-work.md
content/volumes/frameworks/FW-02-hibernate-and-jpa/chapters/08-testing-sql-traces-and-production-diagnostics.md
content/volumes/frameworks/FW-02-hibernate-and-jpa/chapters/09-live-interviews-rapid-qa-practice-and-sources.md
```

Add:

```json
"code_companion": {
  "path": "content/volumes/frameworks/FW-02-hibernate-and-jpa/code/HibernateJpaInterviewCompanion.java",
  "title": "Java 21 Hibernate and JPA Interview Companion",
  "description": "Executable models for entity lifecycle, persistence-context identity, dirty checking, association ownership, optimistic versions, and bounded batch work."
}
```

Change `publication_status` to `published`, `volume_label` to `Publication Edition`, and use approximately `min_pages: 22`, `max_pages: 90` before building.

## Root validation hooks

```bash
python3 scripts/validate_series.py --source-only
python3 scripts/build_series.py --volume HIBERNATE
python3 scripts/validate_series.py
```

The central validator ignores `planned` volumes, so root must update both status and companion mapping. PDF/web rendering and representative-page inspection remain root-owned.

## Target-engine boundary

H2 proves portable ORM mechanics only. MySQL-specific lock scope, dialect SQL, plan selection, collation, DDL, and wire batching require the exact MySQL/Connector-J environment.
