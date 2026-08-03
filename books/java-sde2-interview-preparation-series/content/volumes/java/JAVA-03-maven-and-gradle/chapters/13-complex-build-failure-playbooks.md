# Complex Build Failure Playbooks

SDE-2 diagnosis should separate symptom, evidence, containment, cause, correction, validation, and prevention. These scenarios combine tool mechanics with delivery judgment.

## Playbook method

For every incident:

```text
1. Scope and contain
2. Capture exact command, commit, wrapper, JDK, and environment
3. Find the first meaningful failure
4. Inspect effective model and resolved graph
5. Form one falsifiable hypothesis
6. Make the smallest safe correction
7. Validate locally, in clean CI, and at artifact/runtime boundary
8. Add a guardrail without hiding future failures
```

## Scenario 1: IDE green, wrapper red

**Evidence:** IDE compiler and test runner versions differ from the build; generated sources exist only in the IDE.

**Response:** reproduce with wrapper, inspect toolchain/release and generation phase/task, configure IDE delegation, and make generated inputs part of the build graph.

**Interview follow-up:** Why is committing generated source not automatically the best fix? It creates dual ownership and drift unless generation policy explicitly requires checked-in outputs.

## Scenario 2: Clean build passes, incremental build fails

**Likely cause:** task or plugin has undeclared inputs/outputs, stale generated files, or order dependence.

**Response:** preserve the failing workspace, compare task outcomes, disable caches selectively for diagnosis, correct the producer/consumer relationship, then test clean and incremental sequences.

## Scenario 3: Incremental build passes, clean build fails

**Likely cause:** hidden reliance on local repository installation, stale generated output, missing declared dependency, or an IDE-created file.

**Response:** reproduce in an empty checkout and isolated artifact cache, then declare or generate the missing input. Never make "run module A once" an onboarding step.

## Scenario 4: `NoSuchMethodError` only in production

**Evidence:** compile and runtime classpaths differ, container adds a library, fat JAR contains duplicates, or conflict mediation selected an incompatible version.

**Response:** capture the loaded class origin, inspect runtime graph and artifact contents, align or isolate versions, and add packaged runtime smoke coverage.

## Scenario 5: Maven parent change is ignored

**Causes:** child inherits a released remote parent rather than reactor parent, `relativePath` differs, effective POM comes from cache, or profile changes selection.

**Response:** inspect `help:effective-pom -Dverbose`, parent coordinate and `relativePath`, reactor selection, and local repository state. Do not install random snapshots until the parent relationship is understood.

## Scenario 6: Gradle task runs during every build

**Causes:** missing output, changing input, task action always marked out of date, non-cacheable implementation, or output deleted by another task.

**Response:** use verbose task outcomes, inspect declared properties, isolate nondeterministic values, and confirm no overlapping outputs.

## Scenario 7: Dependency lock update changes hundreds of entries

**Causes:** platform/BOM change, repository metadata drift, configuration coverage changed, or tool upgrade altered resolution.

**Response:** do not accept mechanical churn. Compare selection reasons, group intentional upgrades, review security/compatibility, and run consumer tests.

## Scenario 8: Private dependency resolves from public repository

**Risk:** dependency confusion or namespace takeover.

**Response:** block release, inspect repository order and requested coordinates, restrict internal group to the private repository, verify bytes, rotate credentials if malicious code executed, and audit other builds.

## Scenario 9: CI cache makes a removed dependency appear available

**Cause:** build uses a broad machine-level classpath, local publication, or stale generated artifact rather than a declared dependency. A proper Maven/Gradle dependency cache should not add an undeclared graph node by itself.

**Response:** run in an isolated home/cache, inspect effective classpaths, and remove hidden installation assumptions.

## Scenario 10: Integration-test infrastructure leaks

**Maven:** pipeline invoked `integration-test` rather than `verify`, bypassing final lifecycle stages.

**Gradle:** cleanup was not a finalizer or external finally step.

**Response:** clean up resources first, then repair lifecycle/task modeling and add timeout plus ownership tags.

## Scenario 11: Parallel build is slower and flaky

**Cause:** nested test/build parallelism oversubscribes CPU/memory, modules contend for ports or database locks, or task/plugin work is not thread-safe.

**Response:** measure utilization and critical path, reduce one layer of concurrency, isolate resources, and compare tail latency plus failure rate.

## Scenario 12: Published library compiles in Gradle but not Maven

**Cause:** publication metadata lost variant semantics, POM scopes are wrong, or a dependency required by the public API is hidden.

**Response:** inspect generated POM and module metadata, test a clean Maven consumer and Gradle consumer, correct API exposure, and republish under a new immutable version.

## Scenario 13: Wrapper upgrade breaks only CI

**Causes:** CI JDK is incompatible, distribution access is blocked, checksum changed, wrapper executable bit is missing, or removed behavior affects a plugin.

**Response:** compare wrapper files and CI launcher JDK, validate the distribution checksum, use an upgrade compatibility report, and separate tool upgrade from unrelated code.

## Scenario 14: Reproducible build hashes differ by operating system

**Causes:** line endings, file permissions/order, generated absolute paths, locale, or platform-specific native artifacts.

**Response:** compare archives structurally, normalize intended metadata, isolate platform-specific outputs, and document the reproducibility boundary rather than claiming universal bit identity.

## Scenario 15: Release used unreviewed plugin code

**Cause:** plugin version was dynamic/unpinned, plugin repository was broad, or Gradle build logic from an included build changed outside the reviewed diff.

**Response:** stop publication, preserve resolved plugin evidence, verify produced artifacts, pin and restrict plugins, and include build-logic ownership in review rules.

## Scenario 16: Secret leaked through build diagnostics

**Response order:** revoke or rotate; contain log/artifact access; identify exposure duration and consumers; clean retained logs/caches; replace with masked short-lived credentials; add logging and permission controls. Rewriting Git or deleting the log alone is insufficient.

## Scenario 17: Selective CI misses a downstream break

**Cause:** change impact ignored transitive consumers, shared build logic, parent/BOM/catalog changes, or generated contracts.

**Response:** expand the graph, run the full verification gate, correct affected-set rules, and add a regression case for the change type.

## Scenario 18: Migration produces different artifacts

**Cause:** lifecycle/task mappings are only superficially equivalent. Resource filtering, test discovery, manifest, dependency scope, generated source order, or publication metadata differs.

**Response:** create a parity matrix; compare tests, classpaths, archive contents, metadata, runtime smoke behavior, and hashes where applicable before switching CI.

## Incident communication template

```text
Impact:
Affected commits/artifacts:
Containment:
Known evidence:
Current hypothesis:
Next falsifying test:
Recovery decision:
Validation:
Preventive owner and due date:
```

## Practice

1. **Interview Core:** Lead scenario 4 aloud in five minutes.
2. **Debugging:** Create commands and evidence for scenario 6.
3. **SDE-2 Follow-up:** Compare containment for scenarios 8 and 15.
4. **SDE-2 Follow-up:** Design a parity gate for scenario 18.
5. **Leadership:** Write a blameless post-incident action for scenario 17 with a measurable owner.

## Readiness check

- [ ] I preserve evidence before deleting caches.
- [ ] I distinguish immediate containment from durable prevention.
- [ ] I validate source, graph, artifact, and runtime boundaries.
