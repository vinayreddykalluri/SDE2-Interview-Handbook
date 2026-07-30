# Java Repository Hygiene: Ignore Rules, Attributes, and Review Boundaries

A clean Java repository stores authoritative source and reproducible configuration while excluding machine-local and regenerable noise. The exact boundary depends on the build and release contract, not a generic ignore file copied from the internet.

## Learning objectives

- decide which Java project artifacts belong in Git;
- write and diagnose ignore rules;
- normalize line endings and classify binary paths with `.gitattributes`;
- manage executable wrappers, generated code, large files, and credentials;
- design commits around Java build, migration, and API boundaries.

## Source-of-truth test

Ask four questions for every path:

1. Is this authored input or generated output?
2. Can the exact output be reproduced from versioned inputs and a pinned toolchain?
3. Must consumers use the repository without that generator?
4. Does the path contain machine-specific, secret, licensed, or oversized data?

Typical decisions:

| Path | Usual policy | Reason |
|---|---|---|
| `src/main/java/**` | track | authoritative application source |
| `src/test/java/**` | track | behavior and regression contract |
| `pom.xml`, `settings.gradle*` | track | build definition |
| Maven/Gradle wrapper scripts and metadata | track | reproducible entry point; validate wrapper integrity |
| `target/`, `build/`, `.gradle/` | ignore | regenerable local output/cache |
| IDE workspace metadata | generally ignore | machine/user-specific; team-shared style files may be exceptions |
| database migrations | track | ordered production data/schema contract |
| generated Java | policy-dependent | track only when consumers or tooling require it |
| `.env`, tokens, private keys | never track | secrets require secure external storage |
| JAR/WAR artifacts | normally release/package storage | avoid repository bloat and source/binary ambiguity |

## Layered ignore rules

Repository rules belong in `.gitignore`. User-specific global exclusions can hide editor and operating-system noise without imposing it on every project. `.git/info/exclude` is local to one clone and uncommitted.

Example root `.gitignore`:

```gitignore
# Java build output
target/
build/
out/
.gradle/

# Editors and local environment
.idea/
.vscode/
*.iml
*.log
.env

# Keep wrapper artifacts even when a broad binary rule exists
!gradle/wrapper/gradle-wrapper.jar
```

Ignore patterns are path patterns, not regular expressions. Negation cannot always re-include a file if a parent directory is fully excluded without making traversal possible. Diagnose the exact matching rule:

```bash
git check-ignore -v -- path/to/file
git status --ignored --short
```

Ignored does not mean untracked if a path is already in the index.

## Attributes define content handling

A cross-platform Java repository can make line-ending policy explicit:

```gitattributes
* text=auto
*.java text eol=lf
*.xml text eol=lf
*.yml text eol=lf
*.yaml text eol=lf
*.properties text eol=lf
*.sh text eol=lf
*.bat text eol=crlf
*.cmd text eol=crlf
*.png binary
*.jpg binary
*.jar binary
*.pdf binary
```

Text normalization stores normalized line endings in Git and chooses a working-tree representation according to attributes. Marking binary content prevents inappropriate text conversion and normal text diffs.

After introducing or correcting attributes, renormalization can create a large intentional diff:

```bash
git add --renormalize .
git status --short
git diff --cached --stat
```

Do this in a dedicated, reviewed change. Do not combine repository-wide normalization with feature behavior.

## Executable scripts

On systems that support it, Git tracks an executable bit as part of file mode. Ensure wrappers and shell scripts are executable:

```bash
git update-index --chmod=+x mvnw
git update-index --chmod=+x gradlew
git diff --summary
```

Windows does not use POSIX executable bits in the same way, so CI on Linux is a useful verification boundary.

## Generated code policy

Choose one explicit model:

### Generate during build, do not track

Best when the generator and inputs are pinned, all consumers build through the wrapper, and code review should focus on the specification.

### Track generated output

Useful when consumers cannot run the generator, generation is expensive, or the output is part of a published SDK contract. Require a deterministic generation check so stale output fails CI.

### Publish generated artifact

Useful when consumers need binaries or source packages but the application repository should remain source-focused. Publish through release or package infrastructure.

Whichever model is selected, document ownership and avoid manual edits to generated output.

## Build dependency and lock changes

Review `pom.xml`, Gradle build files, version catalogs, and lockfiles as security and reproducibility changes. Ask:

- direct or transitive dependency?
- runtime, test, plugin, annotation processor, or build-only scope?
- version conflict or exclusion?
- checksum/signature and repository origin?
- new license or vulnerability?
- Java version compatibility?
- generated or lock output updated consistently?

Keep large dependency upgrades separate from feature code unless inseparable. This helps bisection, review, and rollback.

## Database migrations

Migrations are production code. Prefer append-only ordered migrations after release rather than editing an already-applied file. A PR should state:

- forward and compatibility phases;
- lock and duration risk;
- whether old and new application versions can overlap;
- data backfill and validation;
- rollback or forward-fix strategy.

Git can merge two new migration files cleanly while their version identifiers or SQL effects conflict. CI and integration environments must validate ordering.

## Large files and repository performance

Git history retains prior versions reachable from commits. A 200 MB artifact deleted in the next commit still contributes to repository history. Prevent it with ignore rules, size checks, and appropriate package or large-file storage.

If a large file is committed locally but not pushed, remove it from the private commit and recommit. If published, coordinate history-rewrite tooling only after assessing forks, clones, open PRs, releases, and retention. Repository cleanup is an incident, not a casual `git rm`.

## Secrets and configuration

Track a safe template such as `.env.example` with names and nonsecret sample values. Store real credentials in a secret manager or protected CI environment. Never depend on a later deletion to make a secret safe.

Spring configuration often layers defaults and environment overrides. Review:

- whether a default exposes a credential or unsafe endpoint;
- whether debug logging can print tokens;
- whether test credentials are actually valid beyond test scope;
- whether `application-prod.yml` contains environment-specific secrets;
- whether a new property is documented and validated at startup.

## Interview questions and model answers

**Should a Java repository commit generated sources?**

It depends on the consumer and reproducibility contract. If pinned tooling can deterministically generate them for every consumer, ignoring them reduces noise. If consumers cannot generate them or they are the reviewed SDK output, track them with a CI stale-generation check. The repository should document one model.

**What is the difference between `.gitignore` and `.gitattributes`?**

Ignore rules decide which untracked paths Git normally considers for addition. Attributes influence how matched paths are normalized, diffed, merged, exported, or otherwise handled. Neither removes existing history.

**Why commit wrapper files?**

They define a repository-owned build entry point and tool distribution version so contributors and CI use the same launcher. Teams should also validate wrapper provenance and checksums according to their supply-chain policy.

## Exercises

1. **Foundation - policy:** Classify 20 paths from a Maven or Gradle project as source, generated, local, secret, or release artifact.
2. **Foundation - debugging:** Use `git check-ignore -v` to explain why a nested file remains ignored after a negation rule.
3. **Interview Core - design:** Write `.gitattributes` for Java, shell, Windows command, images, PDFs, and JARs.
4. **Interview Core - migration:** Review two cleanly merged database migrations for ordering and compatibility risk.
5. **SDE-2 Follow-up:** Design a deterministic generated-client workflow with review, stale-output CI, and release ownership.

## Chapter summary

Repository hygiene is an ownership and reproducibility contract. Track authoritative inputs, version the build entry point, exclude local and regenerable noise, normalize text deliberately, classify binaries, and treat migrations, dependencies, and secrets as high-risk review boundaries.

## Revision checklist

- [ ] I can justify tracked and ignored paths from first principles.
- [ ] I can diagnose ignore matches.
- [ ] I can define cross-platform attributes safely.
- [ ] I can explain the generated-code policy.
- [ ] I treat secrets, dependencies, and migrations as production concerns.
