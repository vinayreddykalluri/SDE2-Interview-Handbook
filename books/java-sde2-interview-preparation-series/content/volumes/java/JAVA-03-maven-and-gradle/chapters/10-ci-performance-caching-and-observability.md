# CI, Performance, Caching, and Build Observability

A reliable build is fast enough to run frequently and transparent enough to debug. Optimize measured critical paths without weakening the build contract.

## CI stages

A practical Java pull-request pipeline may separate:

```text
checkout + wrapper validation
          |
          v
compile + unit tests + static checks
          |
          v
integration tests + package smoke test
          |
          v
dependency/security evidence + artifact upload
```

Fail fast on cheap deterministic checks, but retain useful reports from later independent jobs. Cache only data whose trust and invalidation model is understood.

## Maven performance model

Maven reactor builds can use parallelism, but plugin thread safety and resource contention matter:

```bash
./mvnw -T 1C verify
```

This requests a thread count relative to CPU cores. Measure it. Tests may already fork JVMs, and combined build/test parallelism can exhaust memory or database capacity.

The local repository avoids repeated downloads, but sharing an unscoped mutable local repository across concurrent CI jobs can create corruption or cross-job contamination. CI caches should be keyed to operating system, wrapper/tool version, and relevant dependency metadata; they should not replace artifact publication.

Maven Daemon is a separate tool with different operational assumptions. Label its use rather than implying every `mvn` process is persistent.

## Gradle work avoidance

Gradle exposes several independent mechanisms:

1. **Up-to-date checks** skip a task when its declared inputs and local outputs match.
2. **Incremental task action** processes only changed input portions when supported.
3. **Build cache** restores task outputs from a compatible previous execution.
4. **Configuration cache** reuses the configured task graph when configuration inputs match.
5. **Daemon** reuses a JVM process and warmed state.
6. **Parallel execution** schedules independent project work concurrently.

```bash
./gradlew build --console=verbose
./gradlew build --build-cache
./gradlew build --configuration-cache
./gradlew build --parallel
```

Enable features deliberately, observe warnings, and fix incompatible custom tasks rather than switching to warn mode forever.

## Cache correctness

A cache key is a statement: these declared inputs completely determine these outputs. Missing input example:

```text
task declares: source files
task reads:    source files + environment variable REGION
cache key:     excludes REGION
result:        output for us-east may be reused in eu-west
```

Secrets should rarely influence cacheable output. If they do, avoid storing secret-derived outputs in a shared cache and understand whether secret values enter keys, metadata, or logs.

Remote caches require write-trust policy. A compromised writer can poison outputs consumed by many developers. Common patterns let trusted CI push while developer machines read only.

## Build timing method

Do not optimize from one warm laptop run.

1. define clean and incremental scenarios;
2. collect several runs on representative machines;
3. separate dependency download, configuration, compilation, tests, packaging, and upload;
4. find the critical path, not just the longest individual task;
5. inspect cache hit reasons and misses;
6. change one variable;
7. compare median and tail latency plus failure rate.

Build scans or profiles can help, but external telemetry must follow source, path, environment, and dependency-data policy.

## Selective execution

Skipping modules or tests can accelerate feedback when selection is graph-correct. It is unsafe when change impact is based only on file paths. Shared parents, version catalogs, convention plugins, schemas, and dependency constraints can affect the entire build.

Maintain layers:

- quick local loop;
- required PR graph;
- post-merge broader graph;
- scheduled clean and compatibility builds;
- release build from an immutable commit.

## Diagnosing a slow build

Ask:

- Is time spent downloading, configuring, compiling, testing, packaging, or waiting?
- Which work is on the critical path?
- Are tasks actually cacheable or merely expected to be?
- Which input changes cause misses?
- Is parallelism causing memory pressure and garbage collection?
- Are integration resources serialized?
- Did a build-logic change invalidate all projects?
- Is CI restoring a cache after the job already needs the data?

## Interview drill

**Question:** What is the difference between Gradle's build cache and configuration cache?

**Strong answer:** The build cache stores outputs of cacheable task executions keyed by declared inputs and implementation details. The configuration cache stores the configured task graph and relevant configuration inputs so Gradle can skip configuration. They optimize different phases and can be used independently. Neither excuses undeclared inputs.

## Practice

1. **Foundation:** Classify dependency cache, up-to-date output, and published artifact.
2. **Predict:** Can more parallel forks make a build slower?
3. **Debugging:** A cache hit returns region-specific stale output. Identify the contract defect.
4. **Interview Core:** Compare Gradle cache layers and Maven local-repository reuse.
5. **SDE-2 Follow-up:** Design trusted remote-cache read/write policy.

## Readiness check

- [ ] I optimize measured critical paths.
- [ ] I distinguish work avoidance mechanisms.
- [ ] I treat cache correctness and writer trust as production concerns.
