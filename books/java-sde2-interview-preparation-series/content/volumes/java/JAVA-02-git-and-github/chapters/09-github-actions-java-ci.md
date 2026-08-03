# GitHub Actions CI for Java: Trustworthy Builds and Merge Gates

Continuous integration is executable review evidence. A green check is valuable only when the workflow builds the intended revision, uses trusted dependencies, has appropriate permissions, and runs tests that enforce the actual contract.

## Learning objectives

- read workflow triggers, jobs, steps, expressions, and permissions;
- build Java projects through Maven and Gradle wrappers;
- design JDK matrices and caching without confusing cache with artifacts;
- support pull requests, pushes, and merge queues;
- debug skipped, cancelled, flaky, and environment-specific failures;
- separate untrusted validation from privileged delivery.

## Workflow execution model

```text
event -> workflow -> jobs -> runner -> ordered steps
                      |
                      +--> outputs/artifacts/check result
```

- A **workflow** is a YAML file under `.github/workflows`.
- A **job** runs on one runner environment and contains ordered steps.
- Jobs run in parallel unless `needs` creates dependencies.
- Each job starts with its own filesystem and environment unless a service or self-hosted design says otherwise.
- An **action** is reusable code invoked by `uses`.
- A **workflow command** under `run` executes shell code on the runner.

The trigger determines the trust boundary. A pull request from a fork is untrusted input. A protected deployment on the default branch is privileged.

## Minimal Maven pull-request CI

```yaml
name: java-ci

on:
  pull_request:
  push:
    branches: [main]
  merge_group:

permissions:
  contents: read

jobs:
  verify-maven:
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - name: Check out source
        uses: actions/checkout@v6

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
          cache: maven

      - name: Verify wrapper build
        run: ./mvnw --batch-mode --no-transfer-progress verify
```

This teaching example uses release tags for readability. A hardened production workflow should pin third-party actions, including GitHub-authored actions, to verified full commit SHAs and retain a version comment for update tooling:

```yaml
- uses: actions/checkout@<VERIFIED_FULL_COMMIT_SHA> # v6.x.y
```

The SHA must be obtained from the action's authentic repository or trusted update automation. Do not invent or shorten it.

## Gradle wrapper CI

```yaml
jobs:
  verify-gradle:
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - uses: actions/checkout@v6
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
      - name: Validate Gradle wrapper
        uses: gradle/actions/wrapper-validation@<VERIFIED_FULL_COMMIT_SHA>
      - name: Configure Gradle
        uses: gradle/actions/setup-gradle@<VERIFIED_FULL_COMMIT_SHA>
      - name: Run checks
        run: ./gradlew --no-daemon check
```

The wrapper keeps the build version in the repository. Wrapper validation helps detect an unexpected wrapper JAR. The Maven and Gradle book covers build lifecycle and dependency resolution in depth.

## JDK compatibility matrix

If a library supports Java 17 and 21:

```yaml
strategy:
  fail-fast: false
  matrix:
    java: ["17", "21"]

steps:
  - uses: actions/checkout@v6
  - uses: actions/setup-java@v4
    with:
      distribution: temurin
      java-version: ${{ matrix.java }}
      cache: maven
  - run: ./mvnw --batch-mode --no-transfer-progress verify
```

`fail-fast: false` lets all variants report, which is useful for diagnosis. A matrix can multiply operating systems, architectures, distributions, and database versions quickly. Add a dimension only when it proves a supported contract.

## Cache versus artifact

| Mechanism | Purpose | Example | Trust concern |
|---|---|---|---|
| cache | reuse dependencies or intermediate state across runs | Maven local repository, Gradle caches | untrusted data or broad keys can poison later use |
| artifact | preserve explicit workflow outputs | test reports, built JAR, coverage report | retention, access, provenance, and untrusted content |

A cache is an optimization, never the only copy of a release artifact. Build correctness must survive a cold cache. Cache keys should be derived from relevant wrapper and dependency files, and privileged workflows should not blindly trust cache entries produced from untrusted code.

## Job dependencies and a stable gate

Required checks become easier to govern when one always-running gate summarizes applicable jobs:

```yaml
jobs:
  unit:
    runs-on: ubuntu-latest
    steps:
      - run: ./mvnw test

  integration:
    runs-on: ubuntu-latest
    steps:
      - run: ./mvnw verify -Pintegration

  required-java-gate:
    if: ${{ always() }}
    needs: [unit, integration]
    runs-on: ubuntu-latest
    steps:
      - name: Require successful dependencies
        env:
          UNIT_RESULT: ${{ needs.unit.result }}
          INTEGRATION_RESULT: ${{ needs.integration.result }}
        run: |
          test "$UNIT_RESULT" = "success"
          test "$INTEGRATION_RESULT" = "success"
```

In a real workflow, every job also needs checkout and Java setup where applicable. The gate's logic must treat cancelled and skipped results deliberately. Do not mark a critical skipped test as success merely to satisfy protection.

## Path filtering without stuck required checks

If an entire required workflow is skipped because no paths match, GitHub may leave an expected check unresolved depending on configuration. Safer patterns include:

- always run a lightweight required workflow and decide affected scope inside it;
- have the gate succeed only when all required-for-this-change jobs report acceptable states;
- use separate stable checks for independent components;
- test docs-only, build-only, Java-only, and mixed PRs before enforcing.

In a monorepo, change detection is itself production logic. Renames, shared build files, dependency graphs, and generated inputs can make naive path matching unsafe.

## Concurrency and cancellation

Cancel stale validation runs for the same PR branch:

```yaml
concurrency:
  group: ci-${{ github.event.pull_request.number || github.ref }}
  cancel-in-progress: true
```

Do not use cancellation blindly for deployments, migrations, or release publishing. Interrupting a stateful operation may leave partial external effects. Validation is normally restartable; delivery requires explicit concurrency and idempotency design.

## Service containers and integration tests

An integration job can start a database service, but define health checks and deterministic schema setup:

```yaml
services:
  mysql:
    image: mysql:8.4
    env:
      MYSQL_DATABASE: orders
      MYSQL_ROOT_PASSWORD: local-ci-only
    ports:
      - 3306:3306
    options: >-
      --health-cmd="mysqladmin ping -h 127.0.0.1 -proot"
      --health-interval=10s
      --health-timeout=5s
      --health-retries=10
```

Do not place production credentials in CI examples. Pin container images by an organizational policy where reproducibility and supply-chain integrity require it. The database book covers transaction and migration testing.

## Failure diagnosis ladder

1. Did the workflow trigger on this event and branch?
2. Was the job skipped by an `if`, path filter, environment approval, or dependency result?
3. Which exact revision and merge context was checked out?
4. Did Java and wrapper versions match local development?
5. Did a warm cache hide a missing dependency or generated input?
6. Is the failure deterministic on a clean local checkout?
7. Is it a test race, clock/time-zone assumption, port collision, order dependence, or resource leak?
8. Did a runner image or external service change?
9. Are logs and test reports retained without leaking secrets?

Rerunning until green is not a flaky-test strategy. Track the failure, quarantine only with ownership and a deadline, preserve evidence, and fix or remove the unreliable assertion.

## Matrix failure case

Suppose Java 17 passes and Java 21 fails with reflective-access behavior. Do not remove Java 21 from the matrix to restore green. Confirm the declared support contract, reproduce with the wrapper and exact JDK, inspect dependency compatibility, and either fix the code/dependency or change the support policy explicitly.

## Interview questions and model answers

**Why run CI through the wrapper?**

The wrapper pins the build-tool distribution and gives contributors and CI the same repository-owned entry point. CI should still validate wrapper provenance and use a pinned JDK and dependencies according to policy.

**Why does a merge queue require special workflow handling?**

GitHub creates a merge-group context that needs required checks. Workflows that only listen to pull requests may not run for that context, leaving the queue without required results. Add the documented `merge_group` event and test it.

**What makes a green check untrustworthy?**

It may test the wrong revision, skip affected work, use mutable untrusted actions, have excessive permissions, rely on poisoned cache, ignore failures, or exercise inadequate tests. Governance must evaluate both workflow integrity and test quality.

## Exercises

1. **Foundation - reading:** Annotate trigger, permissions, job, runner, steps, and cache in the Maven workflow.
2. **Interview Core - matrix:** Design the smallest matrix for a Java 17-compatible library developed on 21.
3. **Interview Core - debugging:** A required workflow is stuck on docs-only PRs. Repair the path/gate design.
4. **Interview Core - reliability:** Create a triage plan for an integration test that fails 2% of runs.
5. **SDE-2 Follow-up:** Design separate untrusted PR validation and privileged release workflows with explicit artifact handoff and trust verification.

## Chapter summary

Java CI should build through pinned repository contracts, run on every required merge context, use minimal permissions, distinguish caches from artifacts, and expose deterministic gates. The workflow itself is production security code.

## Revision checklist

- [ ] I can explain workflow execution and trust boundaries.
- [ ] I can build Maven and Gradle projects through wrappers.
- [ ] I can design a justified JDK matrix.
- [ ] I can prevent skipped required checks and merge-queue gaps.
- [ ] I diagnose flakiness instead of rerunning it away.
