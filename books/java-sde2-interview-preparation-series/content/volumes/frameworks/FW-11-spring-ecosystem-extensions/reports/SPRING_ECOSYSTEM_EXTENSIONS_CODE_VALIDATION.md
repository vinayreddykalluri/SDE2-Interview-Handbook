# Spring Ecosystem Extensions Code Validation

## Validated companion

`code/SpringEcosystemExtensionsCompanion.java` is dependency-free Java 21. It models framework-independent invariants rather than pretending to implement Spring.

## Command

```bash
javac --release 21 -Xlint:all -Werror -d <temporary-classes> \
  content/volumes/frameworks/FW-11-spring-ecosystem-extensions/code/SpringEcosystemExtensionsCompanion.java
java -ea -cp <temporary-classes> SpringEcosystemExtensionsCompanion
```

## Result

| Check | Result |
|---|---|
| Compilation with all lint warnings as errors | PASS |
| Execution with assertions enabled | PASS |
| First-matching Security chain and shadowing case | PASS |
| Authority plus tenant/resource authorization | PASS |
| Remaining-deadline retry planning | PASS |
| Reactive demand and cancellation accounting | PASS |
| Batch restart versus idempotent external effect | PASS |
| Correlation-state expiry | PASS |

Observed output:

```text
PASS 6 Spring ecosystem extension invariant suites
```

Remaining validation belongs to a future provider-backed lab and the root source/PDF build; no framework dependency was introduced in this focused pass.
