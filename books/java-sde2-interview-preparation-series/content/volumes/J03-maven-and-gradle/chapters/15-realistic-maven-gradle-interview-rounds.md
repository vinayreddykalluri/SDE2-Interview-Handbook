# Realistic Maven and Gradle Interview Rounds

Use these as spoken simulations. Pause after the interviewer line, answer aloud, then compare the strong answer and follow-up.

## Round 1: Build fundamentals

**Interviewer:** What problem does a Java build tool solve beyond compilation?

**Candidate:** It defines the repeatable graph around compilation: source layout, generated inputs, dependency resolution, separate classpaths, tests, packaging, metadata, publication, and environment/toolchain rules. It lets developers and CI request an outcome and verify the same contract.

**Follow-up:** What is the graph's most important property?

**Candidate:** Work must have explicit dependencies, inputs, and outputs. Otherwise ordering, incrementality, caching, and selective execution can be wrong.

## Round 2: Maven phase versus goal

**Interviewer:** Is `compile` a Maven command or plugin goal?

**Candidate:** In `mvn compile`, `compile` is a lifecycle phase. Maven traverses preceding phases and executes goals bound to them. `compiler:compile` is a specific plugin goal. A goal can also be invoked directly.

**Follow-up:** Why prefer `verify` in CI?

**Candidate:** It reaches packaging and configured integration/quality verification without mutating the local repository like `install` or publishing like `deploy`.

## Round 3: Gradle lifecycle

**Interviewer:** Why does code in `build.gradle.kts` run when I request an unrelated task?

**Candidate:** Build scripts participate in configuration. Gradle initializes projects, evaluates configuration needed to construct the task graph, then executes selected tasks. Eager configuration or side effects can therefore run without the task action.

**Follow-up:** How do you improve it?

**Candidate:** Use lazy task registration, providers, convention plugins, declared inputs, and configuration-cache-compatible APIs. I measure configuration time before rewriting.

## Round 4: Scope mapping

**Interviewer:** Is Maven `compile` identical to Gradle `implementation`?

**Candidate:** No. Both can represent a normal application dependency, but Maven compile dependencies propagate to consumers. Gradle `implementation` is hidden from a Java library consumer's compile classpath; `api` expresses public exposure. I choose from classpath and publication needs.

## Round 5: Version conflict

**Interviewer:** Two libraries request different Jackson versions. Which wins?

**Candidate:** I inspect the actual graph. Maven normally uses nearest definition unless management or direct declarations control it. Gradle selection can involve conflict rules, constraints, platforms, variants, and locks. I use dependency tree or dependency insight and verify the runtime artifact.

**Follow-up:** Why not force the newest?

**Candidate:** Newest does not prove binary or behavioral compatibility for both callers.

## Round 6: BOM versus lock

**Interviewer:** A BOM makes the build reproducible, correct?

**Candidate:** It aligns or manages a version family but does not necessarily record the complete resolved graph, authenticate bytes, pin plugins/toolchains, or control timestamps. Locks and verification solve different parts, and reproducibility must be tested.

## Round 7: Tests

**Interviewer:** Why is `mvn integration-test` risky as the CI command?

**Candidate:** Failsafe's model uses post-integration-test for teardown and verify for final result checking. Stopping at integration-test can bypass cleanup or verification. I invoke `mvn verify`.

**Follow-up:** How do you model the equivalent in Gradle?

**Candidate:** An explicit integration suite/task, ordered after unit tests, wired into `check`, with cleanup as a finalizer or pipeline finally action.

## Round 8: JDK versions

**Interviewer:** The build says Java 21. What do you verify?

**Candidate:** Wrapper/build-tool launcher JDK, selected compiler/test toolchains, `--release` or compatibility target, and deployed runtime. They can differ.

## Round 9: Multi-module ordering

**Interviewer:** Should I list Maven modules in dependency order?

**Candidate:** Real dependencies should establish order. The reactor sorts collected projects using instantiated relationships. Declaration order is not an architecture mechanism. Gradle similarly uses task/project dependencies.

## Round 10: Gradle caches

**Interviewer:** Explain `UP-TO-DATE` versus `FROM-CACHE`.

**Candidate:** Up-to-date means current workspace outputs already match declared inputs. From-cache means compatible outputs were restored from a build cache. Configuration cache is separate and reuses the configured task graph.

**Follow-up:** What makes cache reuse unsafe?

**Candidate:** Missing inputs, overlapping outputs, nondeterminism, secret-derived data, or an untrusted cache writer.

## Round 11: Runtime linkage failure

**Interviewer:** CI passes, production throws `NoSuchMethodError`. Lead the diagnosis.

**Candidate:** I capture the exact binary signature and loaded class origin, compare compile and runtime graphs, inspect the packaged artifact and container libraries, identify duplicate or mediated versions, and correct alignment or isolation. Then I add a packaged runtime smoke test.

## Round 12: Publishing

**Interviewer:** Why test both Maven and Gradle consumers of a library?

**Candidate:** Gradle Module Metadata can express variants that a generated Maven POM cannot represent exactly. Incorrect API exposure or scopes may work for one consumer and fail for the other. Clean consumer tests validate published metadata.

## Round 13: Security

**Interviewer:** A checksum matches. Are we safe?

**Candidate:** We have byte identity relative to an expected digest. We still need confidence in how the digest was established, publisher/repository trust, vulnerability and malicious-code assessment, provenance, and execution controls.

## Round 14: Secret leak

**Interviewer:** A token is printed in Maven debug logs. What first?

**Candidate:** Revoke or rotate the token, then contain access and assess exposure. Log deletion is not revocation. I replace it with short-lived least-privilege credentials and fix redaction/debug policy.

## Round 15: Tool selection

**Interviewer:** Give me a one-minute Maven-versus-Gradle recommendation.

**Candidate:** I start with build shape, plugin maturity, team expertise, customization, module scale, performance evidence, publication semantics, and security/reproducibility requirements. Maven often lowers novelty for conventional services. Gradle can justify richer task, variant, and cache modeling at scale. I would prototype the critical path and record migration and exit costs.

## Round 16: Build migration

**Interviewer:** The converted build is green. Can we switch?

**Candidate:** Green is necessary but not sufficient. I compare resolved classpaths, test inventory, generated source order, resources, archive contents, manifests, publication metadata, runtime smoke behavior, and performance. I run parallel builds, retain one publication authority, and keep rollback until an observation window passes.

## Round 17: Slow build

**Interviewer:** Developers want more parallelism. What do you do?

**Candidate:** Measure clean and incremental critical paths, resource utilization, nested test forks, cache misses, and p95 latency. More parallelism can increase contention and flakiness. I change one layer and compare speed plus reliability.

## Round 18: SDE-2 ownership

**Interviewer:** What distinguishes senior build ownership from knowing commands?

**Candidate:** I make the build contract explicit, keep feedback fast and trustworthy, connect source to immutable artifacts, secure dependencies and credentials, diagnose from evidence, design safe recovery, and leave measurable guardrails owned by the team.

## Self-scoring rubric

Score each answer from 0 to 3:

- 0: incorrect or command-only;
- 1: correct definition without mechanics;
- 2: mechanics plus diagnostic evidence;
- 3: mechanics, trade-offs, recovery, and preventive control.

A strong SDE-2 rehearsal scores at least 42 of 54 without reading notes.

## Practice

1. Record rounds 5, 10, 11, 13, and 16 aloud.
2. Limit each first answer to 90 seconds.
3. Add one clarifying question before scenario answers.
4. Name the exact command that would obtain evidence.
5. End each incident answer with validation and prevention.

## Readiness check

- [ ] I can answer without saying "Maven is XML" or "Gradle is faster."
- [ ] I include classpath, artifact, runtime, and security evidence.
- [ ] I can lead an unfamiliar build incident with a controlled method.
