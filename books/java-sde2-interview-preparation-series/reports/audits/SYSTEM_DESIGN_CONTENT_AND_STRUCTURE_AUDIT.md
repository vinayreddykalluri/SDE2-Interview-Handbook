# System Design Content and Structure Audit

## Scope and result

This wave audited and strengthened the two books that form the System Design learning path. It did not modify the website, PDF tooling, generated site, build output, or distribution files.

The new order is intentional:

1. **SD-01 — Design, Backend, Testing, and Security** teaches correctness inside a Java service boundary: request flow, trust boundaries, idempotency, local transactions, outbox, concurrency, tests, security, and operations.
2. **SD-02 — Distributed Systems and System Design** expands those same invariants across processes: capacity, replication, consistency, streaming, retries, overload, partition movement, multi-region failure, SLOs, and interview defense.

This avoids teaching distributed coordination before the reader can explain a correct local transaction and duplicate-safe boundary.

## Audit findings and actions

| Area | Previous condition | Risk | Action completed |
|---|---|---|---|
| SD-01 native material | relied on four broad master chapters; no native companion, exercises, or solutions | useful concepts were not tied into one low-level request lifecycle | added two native chapters, executable boundary model, ten drills, and reasoned solutions |
| request/data flow | layers were discussed, but commit ambiguity and identifier roles needed one continuous trace | candidates could name patterns without explaining state after timeout | traced edge → authn/authz → idempotency → transaction/outbox → response and recovery |
| security/testing | broad guidance needed concrete boundary and failure choices | shallow “unit vs integration” and OWASP-name answers | added risk-based test portfolio, SSRF/BOLA/tenant cases, shutdown, safe telemetry, and edge matrix |
| SD-02 theory-to-mechanics | strong coverage of capacity, Kafka, resilience, SLOs, and worked designs | key low-level decisions were dispersed across chapters | added one end-to-end write/read/async flow chapter with explicit state and identifier ownership |
| duplicate delivery | delivery semantics explained conceptually | “exactly once” could still be overstated across an external effect | added executable consumer effect ledger and transactional-boundary explanation |
| cache correctness | cache trade-offs existed | TTL could be confused with read-after-write freshness | added version-aware cache decisions and stale-on-error boundary |
| partition movement | partitioning covered | changing membership needed an executable intuition and migration protocol | added rendezvous selection checks and an online resharding sequence |
| overload | retry/rate-limit coverage existed | queue depth alone could hide expired work | added age-aware admission checks, retry-budget reasoning, and overload Q&A |
| interview practice | question bank existed | more adversarial follow-up chains and reasoned drills were needed | added eight multi-turn live chains and ten distributed-design drills with solutions |
| navigation | books lived under legacy `18F`/`18J` folders | series ownership and reading order were unclear | moved to `content/volumes/system-design/SD-01...` and `SD-02...`; added shelf README |

## Content inventory after enhancement

### SD-01

- four retained master sources for SOLID/LLD, backend/JDBC, testing/build quality, and security/reliability;
- two new native chapters;
- ten focused exercises with ten reasoned solutions;
- seven realistic live-interview Q&A chains;
- one dependency-free Java 21 companion covering idempotency, transaction/outbox state, tenant authorization, optimistic concurrency, and constant-time comparison;
- approximately 4,073 words across native chapters, exercises, and solutions.

### SD-02

- six native chapters, including the new request/data-flow and design-defense chapter;
- ten new design drills with ten reasoned solutions;
- eight new multi-turn live-interview Q&A chains, in addition to the existing worked question bank;
- one dependency-free Java 21 companion covering capacity, token-bucket admission, deadline-bounded retries, contiguous offsets, saga state, quorum/SLO calculations, idempotent effects, cache freshness, rendezvous movement, and backlog-age admission;
- approximately 16,294 words across chapters, exercises, and solutions.

## Failure coverage added

Both books now explicitly reason through duplicate commands, same-key/different-payload conflict, unknown commit result, outbox relay duplication, consumer effect atomicity, tenant crossing, lost updates, stale cache versions, cache collapse, hot keys, out-of-order consumer completion, poison records, retry storms, stale backlogs, resharding gaps, regional split brain, shutdown races, and sensitive telemetry.

The consistent interview response pattern is:

```text
constraint -> invariant -> authoritative state -> state transition
           -> failure/unknown outcome -> recovery -> evidence -> trade-off
```

## Structural changes

```text
content/volumes/system-design/
├── README.md
├── SD-01-design-backend-testing-and-security/
│   ├── chapters/
│   ├── code/
│   ├── exercises/
│   └── solutions/
└── SD-02-distributed-systems-and-system-design/
    ├── chapters/
    ├── code/
    ├── exercises/
    └── solutions/
```

The tracked SD-02 source was moved with `git mv`. SD-01 was newly authored in this wave and had no tracked predecessor, so its directory was moved directly into the new shelf. Manifest paths now use the ordered locations.

## Validation

Commands:

```bash
jq empty books/java-sde2-interview-preparation-series/publishing/series.json

javac --release 21 -Xlint:all -Werror -d "$classes" \
  books/java-sde2-interview-preparation-series/content/volumes/system-design/SD-01-design-backend-testing-and-security/code/BackendBoundaryPatterns.java \
  books/java-sde2-interview-preparation-series/content/volumes/system-design/SD-02-distributed-systems-and-system-design/code/DistributedSystemsPatterns.java

java -ea -cp "$classes" BackendBoundaryPatterns
java -ea -cp "$classes" DistributedSystemsPatterns
```

Results:

- manifest JSON: valid;
- SD-01 compile: pass with Java 21, all lint warnings treated as errors;
- SD-01 execution: `PASS 24 backend-boundary checks`;
- SD-02 compile: pass with Java 21, all lint warnings treated as errors;
- SD-02 execution: `PASS distributed-systems executable checks` (47 explicit Java assertions);
- manifest-declared sources/companions: all resolved after the move;
- old System Design source prefixes outside excluded generated/web paths: none;
- intentionally invalid Java snippets in companions: none.

## Remaining work and boundaries

- PDF and visual inspection belong to the later coordinated publishing rebuild; this wave intentionally did not touch PDF generation or output.
- The website should consume the corrected manifest paths in the coordinated web update rather than receiving a one-off route patch here.
- Framework-specific examples belong in the Frameworks series; database/ORM internals belong in the persistence books. The System Design books retain only the behavior needed to defend service and distributed boundaries.
- A future mock-interview wave could add scored 45-minute design rubrics, but this is lower priority than completing the same invariant/failure/exercise standard across unfinished books.
