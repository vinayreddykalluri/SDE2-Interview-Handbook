# Spring Boot executable labs

These labs validate the book's highest-risk runtime claims without changing the publication pipeline.

## Dependency-free companion

`../code/SpringBootInterviewCompanion.java` compiles with Java 21, `-Xlint:all`, and `-Werror`. It models configuration precedence and origin, conditional auto-configuration/back-off, availability transitions, caller deadline allocation, and durable-idempotency state semantics.

## Real Spring Boot fixture

`maven-demo` pins Spring Boot 4.1.0 and runs six tests covering:

1. command-line configuration precedence in a real `SpringApplication`;
2. application liveness/readiness after successful startup;
3. default conditional auto-configuration;
4. property-based disablement;
5. user-bean back-off;
6. fail-fast configuration-property validation and condition-report evidence.

Run everything:

```bash
bash validate_spring_boot_labs.sh
```

H2, MySQL, web-server load, security, and native-image behavior intentionally remain outside this focused fixture. Their chapters specify the evidence required at those boundaries.
