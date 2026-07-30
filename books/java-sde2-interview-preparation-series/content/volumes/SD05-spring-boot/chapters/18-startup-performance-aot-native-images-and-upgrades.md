# Startup Performance, AOT, Native Images, and Upgrades

Optimize from measured constraints. A service that starts in four seconds and deploys safely does not need native-image complexity because another team published a benchmark.

## Measure the phase

Startup time can be dominated by:

- classpath scanning and configuration parsing;
- bean construction and post-processing;
- database migrations;
- remote calls in initialization;
- cache warmup/data loading;
- embedded server startup;
- JIT and first-request warmup.

Use structured startup steps, Java Flight Recorder, profiles, condition reports, and bean counts. Separate process start, context refresh, ready event, and first useful response.

## Safe reductions

1. Remove unused starters and transitive capabilities.
2. Eliminate remote calls from constructors and post-processors.
3. Bound or defer optional warmup.
4. Narrow component/entity/repository scans.
5. Simplify excessive context customizers.
6. Make migrations a deliberate deployment step.

Global lazy initialization can improve startup but moves errors and latency to the first request. Use it only with warmup and failure evidence.

## AOT processing

Ahead-of-time processing analyzes application structure during the build and generates code/hints that reduce some runtime discovery. It changes build behavior and may reveal dynamic patterns that need explicit hints.

AOT is useful for JVM deployment too; native compilation is a separate choice. Test AOT output in CI, not only ordinary JVM mode.

## Native image trade-offs

Potential benefits:

- fast process startup;
- lower steady memory for some workloads;
- good fit for scale-to-zero or short-lived processes.

Costs:

- longer/more complex builds;
- closed-world constraints for reflection, resources, serialization, and dynamic proxies;
- different profiling and peak-throughput behavior;
- dependency compatibility work.

Benchmark the actual service, traffic, container limits, and deployment model.

## Upgrade discipline

For a maintenance upgrade:

1. read Boot and dependency release notes;
2. remove expired overrides;
3. compare dependency trees and configuration changelog;
4. compile with warnings treated seriously;
5. run behavior, migration, packaging, startup, and smoke tests;
6. canary and observe before full rollout.

For major upgrades, move through the recommended latest maintenance line first. Boot 4 introduced modularized starters and newer platform baselines; code that depended on internal auto-configuration classes or deprecated starters needs explicit migration.

## Configuration drift

Unknown, renamed, or removed properties can silently stop affecting behavior. Keep configuration metadata checks, compare effective values, and inspect deprecation logs. A successful context does not prove every old property still works.

## Common mistakes

- Enabling lazy initialization to hide cycles or invalid beans.
- Timing only `main` to ready and ignoring first-request warmup.
- Selecting native image without workload evidence.
- Treating internal auto-configuration classes as stable API.
- Upgrading Boot while pinning an incompatible Framework version.
- Ignoring removed configuration properties because tests still start.
- Combining framework upgrade, database migration, and major refactor in one rollout.

## Interview angle

**Interviewer:** How would you reduce a 90-second startup?

**Strong answer:** I decompose startup steps, separate context work from migrations/remote warmup, identify the longest bounded operations, remove unused capabilities and constructor I/O, and decide what is mandatory before readiness. I validate improvements in the packaged deployment. AOT/native or lazy initialization comes only after simpler causes are measured.

## Quick check

1. Which timestamps define startup?
2. What risk does global lazy initialization add?
3. Is AOT identical to native compilation?
4. When can native images help?
5. Why compare configuration across upgrades?

## Practice

- **Foundation:** Record process, context, ready, and first-response times.
- **Interview Core:** Remove one unused starter and compare startup evidence.
- **Interview Core:** Write a major-upgrade verification matrix.
- **SDE-2 Follow-up:** Decide JVM versus native for a scale-to-zero API using measured assumptions.
