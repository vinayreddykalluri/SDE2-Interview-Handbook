# Spring Data executable labs

This volume has two validation layers.

1. `../code/SpringDataInterviewCompanion.java` is dependency-free Java 21. It validates deterministic comparator composition, separate offset/cursor models, classified optimistic retry, repository intent, and evidence-based store selection.
2. `maven-demo` uses Spring Boot 4.1.0, Spring Data JPA, Hibernate, H2, and JUnit. It proves derived-query ordering, `Slice` query count, keyset continuation, service rollback, flush-time uniqueness, optimistic conflict, pessimistic JPA lock mode, and existence semantics.

Run both from this directory:

```bash
bash validate_spring_data_labs.sh
```

H2 deliberately proves framework/JPA behavior only. MySQL execution plans, collation, gap/next-key locks, deadlocks, online migrations, and isolation behavior require the target MySQL version and belong in the MySQL/Hibernate integration suite.
