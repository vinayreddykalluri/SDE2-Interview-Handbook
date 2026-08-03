# Spring Data Code Validation — Wave 1

## Validation environment

- Source baseline: Java 21.
- Maven runtime observed: Java 23.0.2, compiling the project with release 21.
- Spring Boot: 4.1.0.
- Hibernate ORM observed: 7.4.1.Final.
- H2 observed: 2.4.240.

## Dependency-free companion

Command:

```bash
javac --release 21 -Xlint:all -Werror \
  content/volumes/frameworks/FW-06-spring-data/code/SpringDataInterviewCompanion.java
java -ea SpringDataInterviewCompanion
```

Result: **PASS**.

Expected and observed output:

```text
SpringDataInterviewCompanion checks passed
```

Validated contracts:

1. repository ordering intent;
2. timestamp-descending plus ID-descending comparator composition;
3. separate offset and cursor state;
4. bounded classified optimistic retries;
5. workload-evidence store selection and MongoDB transaction nuance;
6. deterministic observability keys.

## Real Spring Data JPA fixture

Command:

```bash
mvn -q -f content/volumes/frameworks/FW-06-spring-data/labs/maven-demo/pom.xml test
```

Result: **PASS — 7 tests, 0 failures, 0 errors, 0 skipped**.

The tests prove:

1. derived `Slice` ordering uses `(createdAt DESC, id DESC)` and one prepared statement;
2. keyset continuation uses timestamp and ID without an offset;
3. an exception leaving the transactional service rolls back managed mutation;
4. duplicate request key is observed as a translated integrity failure at flush;
5. a stale detached entity produces an optimistic-lock failure;
6. a repository `@Lock(PESSIMISTIC_WRITE)` call applies a JPA pessimistic lock mode inside a transaction;
7. existence and count remain different questions.

## One-command validation

```bash
bash content/volumes/frameworks/FW-06-spring-data/labs/validate_spring_data_labs.sh
```

Result: **PASS**.

## Warnings and boundaries

- Mockito/Byte Buddy printed a dynamic-agent warning because Maven ran on JDK 23. It did not affect compilation or the seven tests. The project source target remains Java 21.
- H2 validates framework/JPA contracts only. It cannot validate MySQL-specific plans, collations, lock ranges, deadlocks, or isolation.
- The central series validator currently skips the SD06 companion because all SD06 manifest sources are `series_native: false`. Root integration must correct the validator/manifest wiring before publication.
- No PDF build was run in this isolated wave; the root publishing pass owns rebuild and visual QA.
