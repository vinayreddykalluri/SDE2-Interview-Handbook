# Spring Framework executable labs

This volume uses two validation layers:

1. `SpringFrameworkInterviewCompanion.java` is dependency-free Java 21. It makes bean-graph order, candidate selection, proxy crossing, and rollback rules executable.
2. `maven-demo` uses Spring Framework 7.0.8, AspectJ Weaver, H2, and JUnit. It proves actual context wiring, scopes, lifecycle, events, AOP proxy behavior, and JDBC transaction outcomes.

Run both from this directory:

```bash
bash validate_spring_framework_labs.sh
```

H2 is intentionally limited to framework mechanics. Use the MySQL volume and a real MySQL integration environment for InnoDB-specific isolation, plans, locks, and failure classification.
