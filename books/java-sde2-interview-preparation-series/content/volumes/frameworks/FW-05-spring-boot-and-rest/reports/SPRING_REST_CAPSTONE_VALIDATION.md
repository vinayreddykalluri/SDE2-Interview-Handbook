# Spring Boot and REST Capstone Validation

## Command

```bash
bash content/volumes/frameworks/FW-05-spring-boot-and-rest/labs/validate_spring_rest_capstone.sh
```

## Validation contract

- Compile `SpringBoundaryModel.java` with `javac --release 21 -Xlint:all -Werror`.
- Execute with assertions enabled.
- Compare the complete process output with `SpringBoundaryModel assertions passed`.
- Fail on a compiler warning, assertion error, exception, or output mismatch.

## Result

Status is recorded from a clean validation run during this content wave: **PASS**.

The executable checks cover canonical request hashing, immutable input normalization, tenant-and-operation idempotency scope, replay versus conflict, lost-response recovery, keyset cursor ordering, overflow-safe comparison, and refusing a retry that cannot fit in the remaining deadline.
