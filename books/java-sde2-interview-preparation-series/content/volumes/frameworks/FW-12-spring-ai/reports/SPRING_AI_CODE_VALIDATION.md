# Spring AI Code Validation

## Validated companion

`code/SpringAiInterviewCompanion.java` is dependency-free Java 21. It tests deterministic application controls without calling or imitating a language model.

## Command

```bash
javac --release 21 -Xlint:all -Werror -d <temporary-classes> \
  content/volumes/frameworks/FW-12-spring-ai/code/SpringAiInterviewCompanion.java
java -ea -cp <temporary-classes> SpringAiInterviewCompanion
```

## Result

| Check | Result |
|---|---|
| Compilation with all lint warnings as errors | PASS |
| Execution with assertions enabled | PASS |
| Context-window budget and overflow | PASS |
| Active-version, tenant-filtered cosine retrieval | PASS |
| Structured-output business validation | PASS |
| Authorized, approval-bound, idempotent refund tool | PASS |
| Scoped conversation ownership and duplicate message defense | PASS |
| Retrieval/citation evaluation checks | PASS |
| Remaining-deadline retry plan | PASS |

Observed output:

```text
PASS 7 Spring AI deterministic-boundary suites
```

No provider-backed result is claimed. Model quality, vector-store filter semantics, streaming, and live tool integration require separately controlled contract/evaluation tests.
