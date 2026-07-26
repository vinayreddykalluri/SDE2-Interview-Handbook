# 51. Testing, Build Tools, Static Analysis, and Dependency Management

## Learning objectives

By the end of this chapter, you should be able to:

- choose unit, integration, contract, end-to-end, property, and performance tests from risk;
- write deterministic behavioral tests with clear fixtures, boundaries, and oracles;
- use fakes, stubs, mocks, clocks, and seeded randomness deliberately;
- configure Java toolchains and release targets for repeatable builds;
- integrate compiler checks, static analysis, formatting, coverage, and mutation evidence without metric gaming; and
- govern direct and transitive dependencies for compatibility, security, licensing, and reproducibility.

## Why this matters at SDE-2

SDE-2 engineers own confidence, not only code. A test suite must catch meaningful regressions quickly enough to run, a build must produce the same intended artifact in CI and release, and dependencies must be understood as executable supply-chain input.

Many teams have thousands of tests and still fear deployment because tests assert implementation details, integration behavior is mocked away, and flaky timing tests are ignored. Other teams create slow end-to-end suites for behavior that a pure unit test would prove better. The goal is a layered evidence system whose cost matches risk.

## First-principles model

A test is an experiment:

```text
given controlled state and inputs
when one behavior occurs
then observable results and side effects match an oracle
```

A useful suite varies the scope of that experiment:

```text
small pure tests        -> fast logic feedback
component/integration   -> database, serialization, framework wiring
contract tests          -> producer/consumer compatibility
end-to-end tests        -> a few critical deployed journeys
load/security tests     -> nonfunctional limits and abuse behavior
production telemetry    -> actual environment validation
```

The build is a function from reviewed source plus pinned inputs to an artifact and evidence. Hidden machine state, changing dependency metadata, undeclared tools, timezone, locale, or network downloads make that function unstable.

> **Specification boundary:** The Java compiler and `--release` option define language/API targeting behavior for supported releases, but Maven, Gradle, JUnit, analysis plugins, coverage tools, and dependency resolution are separate tools. Their defaults and configuration models are version-sensitive.

## Core terminology

- **Test oracle:** Rule that decides whether observed behavior is correct.
- **Fixture:** Controlled state and data used by a test.
- **Unit test:** Test of a small behavior with in-process collaborators controlled.
- **Integration test:** Test of interaction with a real boundary or multiple configured components.
- **Contract test:** Test that a provider and consumer agree on a boundary schema/behavior.
- **Test double:** Replacement collaborator, including fake, stub, spy, or mock.
- **Fake:** Working simplified implementation, such as an in-memory repository.
- **Stub:** Supplies predetermined responses.
- **Mock:** Verifies configured interactions.
- **Flaky test:** Test whose result varies without a relevant product change.
- **Hermetic build/test:** Runs from declared inputs without relying on uncontrolled external state.
- **Toolchain:** Selected JDK used to compile, test, or run build tasks.
- **Transitive dependency:** Dependency brought in by another dependency.
- **Lock/checksum:** Mechanism constraining resolved versions or artifact integrity.
- **SBOM:** Software bill of materials describing shipped components.

## Detailed mechanics

### Test behavior, not implementation shape

Assert public outcomes, emitted records, committed state, and meaningful collaborator contracts. Avoid asserting private method calls, exact incidental SQL order, or every getter invocation. Such tests prevent refactoring without protecting users.

Use Arrange-Act-Assert or Given-When-Then to make the causal boundary visible. One test can assert several aspects of one outcome, but avoid multiple unrelated acts. Name the scenario and expected behavior: `expired_token_is_rejected` communicates more than `testValidate2`.

Cover happy paths, boundaries, invalid input, duplicate/retry behavior, and failure recovery. Use equivalence partitions rather than enumerating random examples. Parameterized tests express a behavior table. Property-based tests generate broad input and shrink failures, but properties need meaningful invariants; "does not throw" is often weak.

### Determinism

Inject `Clock` rather than calling the current time throughout domain code. Supply a seeded or fake random source when exact identity matters. Use temporary directories supplied by the test framework. Set explicit charset, locale, timezone, and ordering when they affect output.

Do not use arbitrary sleeps to coordinate concurrent tests. Use latches, barriers, futures with bounded waits, controllable schedulers, or event probes. A test that passes 1,000 times does not prove a data race absent. Use concurrency stress/model-checking tools where appropriate and retain a simple invariant test.

Isolate mutable state. Static caches, singleton clients, thread locals, environment variables, and database rows can leak between tests. Parallel test execution magnifies hidden sharing. Every test should either own state or use namespaced fixtures and cleanup that survives failure.

### Test doubles and boundaries

Use a fake when stateful behavior matters, a stub for a simple response, and a mock when the interaction itself is the contract. Mocking a class you own can reveal that its API is awkward; mocking a vendor SDK throughout the domain spreads vendor details.

Do not mock the database to prove SQL, transaction, constraint, type, or isolation behavior. Run integration tests against the same database engine and compatible version. An in-memory substitute may implement different SQL and concurrency semantics. Containerized test dependencies improve realism but add image, startup, platform, and supply-chain concerns.

Consumer-driven contract tests can catch incompatible request/response changes before deployment, but they do not prove availability or semantic correctness. Maintain a small end-to-end path through authentication, network, persistence, and messaging for the most critical journeys.

### Exception and async tests

Assert the exception type, safe message or error code, causal information, and state after failure. A test that only expects "some exception" accepts too much. For futures and reactive/asynchronous work, await with a bounded deadline and assert both result and cancellation/failure propagation.

Test retry logic with a fake sleeper/scheduler rather than wall-clock delay. Assert maximum attempts, backoff sequence, idempotency identity, and non-retryable failure. Test that resources close after exceptions using a small instrumented fake or integration metric.

### Coverage, mutation, and static evidence

Line or branch coverage shows executed structure, not assertion quality. A high percentage can coexist with no meaningful oracle. Use coverage to find untested risk, not as a universal quality score.

Mutation testing changes operations and asks whether tests fail. Surviving meaningful mutations expose weak assertions, but equivalent mutations and runtime cost require triage. Apply it to critical pure logic rather than every generated accessor.

Static analysis complements tests by exploring paths without running inputs. Useful categories include nullness, resource leaks, ignored return values, concurrency annotations, insecure APIs, bug patterns, API compatibility, style, and architecture dependency rules. Enable compiler lint checks and treat new serious findings as failures. Suppress narrowly with a reason and scope; a global exclusion turns evidence off.

> **Tooling note:** SpotBugs, Error Prone, NullAway, Checkstyle, PMD, JaCoCo, mutation tools, and architecture-test libraries have distinct models and false positives. Pin versions and understand whether a rule is sound, heuristic, source-level, or bytecode-level before making it a gate.

### Build reproducibility and Java targeting

Use the Maven or Gradle wrapper so developers and CI invoke a reviewed tool version. Select JDK toolchains explicitly. Compiling on JDK 21 with source compatibility 17 alone can accidentally call Java 21 APIs. Use the compiler's `--release 17` mechanism, through the build tool, when the artifact must run on Java 17.

Pin plugin versions. Avoid dynamic dependency versions and mutable artifact repositories. Verify checksums/signatures according to organizational policy. Reproducible archives also require stable timestamps, file order, metadata, and generated content. Byte-for-byte reproducibility is stronger than "the same source compiled successfully."

Separate fast checks from slower integration, contract, and performance stages while keeping one release provenance chain. Generated sources, annotation processors, and code generators are build dependencies with executable power. Pin and review them like runtime libraries.

### Dependency resolution and governance

Declare direct dependencies explicitly rather than relying on a transitive one by accident. Use scopes/configurations correctly so test tools do not ship in runtime artifacts and compile-only APIs are present where needed. Inspect the resolved graph, not only the build file.

Maven and Gradle resolve version conflicts differently and behavior changes with plugins/configuration. A BOM or platform aligns a tested family but does not prove compatibility with every application dependency. Locking improves repeatability; it also means update automation must deliberately refresh and validate locks.

For each shipped component consider:

- origin and artifact integrity;
- supported version and release cadence;
- known vulnerabilities and exploitability in the application;
- license and distribution obligations;
- transitive footprint and duplicate functionality;
- Java baseline, native code, reflection, and framework compatibility; and
- maintenance or replacement plan.

A vulnerability scanner match is a triage input. Confirm the actual artifact, affected version, reachable feature, deployment exposure, compensating control, and vendor fix. Do not ignore a critical issue because no public exploit is known, and do not upgrade blindly without compatibility tests.

### CI pipeline design

A practical pipeline runs formatting/checks, compilation, unit tests, static analysis, packaging, integration/contract tests, dependency and secret scans, artifact signing/attestation, and selected deployment tests. Order cheap high-signal failures early. Run from a clean checkout with least-privilege credentials.

Cache immutable artifacts using keys that include relevant locks and tool versions. A cache must accelerate resolution, not alter it. Publish one promoted artifact rather than rebuilding independently for each environment.

## Worked Java example

This invitation policy injects time, making expiry tests deterministic:

```java
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

record Invitation(Instant expiresAt) {
    boolean isValidAt(Instant instant) {
        return instant.isBefore(expiresAt);
    }
}

final class InvitationService {
    private final Clock clock;
    private final Duration lifetime;

    InvitationService(Clock clock, Duration lifetime) {
        this.clock = java.util.Objects.requireNonNull(clock);
        if (lifetime == null || lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("lifetime must be positive");
        }
        this.lifetime = lifetime;
    }

    Invitation create() {
        return new Invitation(clock.instant().plus(lifetime));
    }
}

final class InvitationServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void invitation_is_valid_before_expiry_but_not_at_expiry() {
        Invitation invitation = new InvitationService(CLOCK, Duration.ofMinutes(15))
                .create();

        assertTrue(invitation.isValidAt(NOW.plusSeconds(899)));
        assertFalse(invitation.isValidAt(NOW.plusSeconds(900)));
    }

    @Test
    void non_positive_lifetime_is_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new InvitationService(CLOCK, Duration.ZERO));
    }
}
```

The snippet uses JUnit Jupiter APIs and needs a pinned JUnit dependency in the test configuration. It tests boundary semantics at exactly the expiry instant without sleeping.

## Execution or memory walkthrough

The fixed clock always returns noon. `create` adds 15 minutes, producing an immutable invitation expiring at 12:15. At 12:14:59 the test instant is strictly before expiry, so validity is true. At exactly 12:15 the strict comparison is false. The test documents a half-open validity interval rather than leaving `<=` versus `<` implicit.

The second test never constructs invalid service state. It checks the public constructor boundary and exact exception type. No mock verifies that `clock.instant()` was called once; call count is an implementation detail unless the contract requires one time snapshot.

Memory and threads are local to each test. The static fixed clock and instant are immutable. If tests changed JVM default timezone instead, parallel tests could interfere. Explicit UTC data avoids that global-state dependency.

## Complexity and performance

Test feedback time is roughly:

```text
sum(test execution + fixture setup + dependency startup)
divided by safe parallelism, plus scheduling and build overhead
```

Unit tests should normally be milliseconds or less, while real database startup and migration dominate integration stages. Reuse can reduce cost but risks state leakage. Prefer suite-level infrastructure with per-test transaction/schema/namespace isolation when behavior remains realistic.

Build complexity grows with modules, annotation processing, generated code, dependency graph, and cache misses. Parallelism helps independent tasks until CPU, memory, disk, ports, database connections, or test isolation becomes limiting.

| Evidence | Strength | Blind spot |
|---|---|---|
| unit tests | precise logic and edge cases | real wiring and infrastructure |
| integration tests | driver/framework/database behavior | full deployment/network path |
| contract tests | boundary compatibility | provider correctness/availability |
| end-to-end | critical real journey | slow diagnosis and combinatorial coverage |
| static analysis | broad path/pattern scan | runtime/environment behavior |
| coverage | executed code map | oracle quality |
| mutation | assertion sensitivity | cost/equivalent changes |

## Edge cases and common mistakes

- Asserting private methods or exact call sequences instead of behavior.
- Mocking the database and claiming transaction or SQL correctness.
- Using sleeps, current time, default timezone, unordered collections, or unseeded randomness.
- Sharing ports, rows, files, static caches, or environment state across parallel tests.
- Catching an exception in a test but never failing when no exception occurs.
- Treating coverage percentage as correctness.
- Quarantining flaky tests indefinitely without ownership and deadline.
- Running every test as an end-to-end test and producing slow, ambiguous failures.
- Compiling with a new JDK's APIs while claiming an older runtime target.
- Omitting plugin and annotation-processor versions.
- Depending accidentally on a transitive library.
- Trusting a BOM, lock file, or vulnerability scanner as proof of safety.
- Rebuilding different artifacts for staging and production.
- Placing production credentials or personal data in fixtures, logs, or test reports.

## Production engineering notes

Give flaky tests the urgency of production defects. Capture seed, schedule, environment, and artifacts; assign an owner; fix root cause or temporarily quarantine with a deadline and visible risk. Blind retries hide nondeterminism and inflate CI cost.

Keep tests close to risk. Database migrations need forward/backward and realistic-volume tests. Serialization needs golden compatibility fixtures. Idempotent message handling needs duplicate and crash-window tests. Security controls need negative and abuse cases. Performance-sensitive changes need controlled benchmarks, not fixed nanosecond assertions on shared CI.

Use dependency update automation to open small reviewed changes with release notes, resolved graph diff, tests, and rollback plan. Generate an SBOM from the shipped artifact, not only declared dependencies. Scan container base images and build plugins as well as Java libraries.

Protect the build system: least-privilege tokens, isolated untrusted pull-request jobs, approved repositories, checksum verification, no secret echo, and signed/promoted artifacts. A compromised build dependency executes with CI authority.

## Interview questions and model answers

**What should you unit test versus integration test?**

Unit-test domain decisions and boundaries with controlled collaborators. Integration-test behavior owned by the database, driver, serializer, framework configuration, or network protocol. Use a few end-to-end tests for critical deployed journeys.

**When is a mock appropriate?**

When interaction with a collaborator is itself the contract or a difficult boundary must return a controlled result. Prefer state/output assertions and fakes when possible; avoid mocking implementation details.

**How do you make time-dependent tests reliable?**

Inject `Clock` or a domain time source, use fixed instants and explicit zones, and test just before, at, and after boundaries without sleeping.

**What does `--release 17` do when compiling on JDK 21?**

It selects the supported Java 17 language/API target model so code cannot accidentally link against newer standard APIs, while generating the corresponding class-file target. It does not validate third-party runtime compatibility.

**How do you manage dependency vulnerabilities?**

Inventory the shipped graph, verify the affected artifact/version and reachable feature, assess exposure and controls, upgrade or mitigate promptly, run compatibility tests, and document exceptions with expiration.

**Why are reproducible builds important?**

They make artifacts traceable to reviewed inputs, support verification and incident response, and prevent hidden machine state or mutable dependencies from changing what ships.

## Exercises

1. Refactor a test using `Thread.sleep(1_000)` into one using a fake scheduler or synchronization primitive.
2. Design tests for a money transfer: pure invariant tests, real database transaction tests, duplicate-command tests, and one end-to-end path.
3. Write a property for a sorting function that is stronger than "output has the same size."
4. Configure a Java 21 build toolchain that produces a Java 17-compatible library and explain third-party dependency checks still needed.
5. Inspect a hypothetical resolved graph with two logging API versions and propose an alignment and verification plan.
6. Create a CI stage order that gives fast feedback while protecting release provenance and secrets.

## Chapter summary

Testing is a layered evidence system. Deterministic unit tests protect domain behavior, integration tests validate real boundaries, contract tests protect schemas, and a few end-to-end tests validate critical journeys. Build wrappers, toolchains, release targeting, pinned plugins, locks, checksums, and promoted artifacts make delivery reproducible. Static analysis and coverage guide risk but do not replace oracles. Dependency governance must inspect the shipped transitive graph, security, licensing, origin, and compatibility.

## Revision checklist

- [ ] I select test scope from the behavior and failure risk.
- [ ] I assert public outcomes rather than private implementation steps.
- [ ] I control clock, randomness, locale, timezone, files, ports, and concurrency.
- [ ] I know when to use a fake, stub, mock, real database, or contract test.
- [ ] I interpret coverage, mutation, and static findings as evidence, not proof.
- [ ] I use wrappers, pinned plugins, explicit toolchains, and `--release` targeting.
- [ ] I inspect and lock the resolved dependency graph and generate an artifact SBOM.
- [ ] I treat flaky tests and build-system security as production reliability concerns.
