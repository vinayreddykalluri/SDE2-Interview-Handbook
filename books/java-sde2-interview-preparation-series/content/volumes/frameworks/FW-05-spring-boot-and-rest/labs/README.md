# Spring Boot and REST capstone lab

Run from the series root:

```bash
bash content/volumes/frameworks/FW-05-spring-boot-and-rest/labs/validate_spring_rest_capstone.sh
```

The lab compiles the dependency-free Java 21 companion with all lint warnings treated as errors, enables assertions, and verifies its exact output. It exercises request fingerprints, scoped idempotency, replay and conflict decisions, lost-response recovery, keyset pagination, comparator safety, and deadline-aware retry admission. The models teach invariants; they do not pretend an in-memory map is a production idempotency store.
