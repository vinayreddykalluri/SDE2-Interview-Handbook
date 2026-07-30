# Tags, Releases, Versioning, and Production Hotfixes

A release should connect source identity, build evidence, artifact identity, deployment state, and rollback instructions. A Git tag alone does not perform a build or deploy, and a GitHub release is not the same object as a tag.

## Learning objectives

- compare lightweight and annotated tags;
- define a reproducible release flow for a Java artifact;
- protect release identity and verify artifacts;
- execute a production hotfix without losing fixes across branches;
- handle rollback, revert, and forward-fix choices;
- reason about version and schema compatibility.

## Tag types

A lightweight tag is a reference directly to an object. An annotated tag normally creates a tag object containing tagger, date, message, and optional signature, then points to a commit.

```bash
git tag -a v2.8.0 -m "Release 2.8.0"
git show v2.8.0
git push origin v2.8.0
```

For release identity, annotated and optionally signed tags provide stronger metadata. Protect release tag patterns so they cannot be moved or deleted casually.

Do not reuse a published version tag for different content. Consumers, caches, dependency resolvers, provenance records, and audits assume a version identity is stable.

## GitHub release relationship

A GitHub release is platform metadata associated with a tag and can include notes and assets. It is not stored in the commit graph. Deleting or editing a release can have different effects from moving a Git tag, subject to protection and immutable-release settings.

Where immutable releases are supported and enabled, published release tags and assets can be locked against modification and accompanied by integrity evidence. Prepare all assets in a draft and publish only when complete.

## Reproducible Java release pipeline

```text
reviewed commit on protected branch
        |
        v
verified CI + dependency/security gates
        |
        v
version/tag authorization
        |
        v
clean wrapper build in isolated runner
        |
        +--> tests and quality evidence
        +--> JAR/source/Javadoc/SBOM
        +--> provenance/attestation/signatures
        |
        v
package registry / GitHub release
        |
        v
deployment promotion with environment approval
```

Build once and promote the same verified artifact when possible. Rebuilding per environment can produce different bytes through timestamps, dependencies, tooling, or configuration. Keep environment-specific secrets and configuration outside the compiled artifact unless the product contract requires otherwise.

## Versioning questions

Semantic Versioning can communicate API compatibility for libraries, but a team must define what its public API includes:

- Java public types and methods;
- serialized JSON or event schemas;
- database compatibility;
- configuration properties;
- CLI arguments;
- behavior relied on by consumers.

For services, deployment versions may use semantic, calendar, or build-based identifiers. Whatever scheme is chosen must map reliably to source and artifact provenance.

## Normal release checklist

1. Default branch is protected and current.
2. Required tests and security checks pass for the release commit.
3. Version and changelog are correct.
4. Database and API compatibility are reviewed.
5. Artifact is built by a trusted pinned workflow.
6. Artifact digest, provenance, and SBOM are retained as policy requires.
7. Tag and release point to the intended commit.
8. Deployment order, metrics, and rollback are documented.
9. Post-deploy verification confirms user behavior, not only process health.

## Hotfix flow across release and main

Assume production runs `v2.8.0` while `main` contains unfinished 2.9 work. Create the hotfix from the production tag or maintained release branch:

```bash
git fetch --tags origin
git switch -c hotfix/2.8.1 v2.8.0
```

Implement the smallest fix and regression test. Open a PR into `release/2.8` or the documented release base, run the release-compatible toolchain, then publish `v2.8.1`.

The fix must also reach active development. Depending on divergence:

- merge the release branch back to `main`;
- cherry-pick the focused fix with provenance;
- implement an equivalent forward fix if main architecture changed.

Track this explicitly. A production fix that never reaches main is a regression waiting for the next release.

## Hotfix decision table

| Situation | Likely action | Guardrail |
|---|---|---|
| recent bad deploy, safe prior artifact | roll back deployment | confirm schema/config backward compatibility |
| isolated bad commit on current branch | revert through emergency PR | run current required checks |
| data corruption still active | stop writes/feature first | preserve evidence and coordinate data recovery |
| main contains risky unrelated work | branch from production tag/release line | forward-port fix afterward |
| migration not backward compatible | forward fix or staged recovery | application-only rollback may fail |
| credential exposed | revoke/rotate immediately | code rollback alone is insufficient |

## Release branch drift

Long-lived release branches accumulate duplicate fixes and merge complexity. Define:

- supported lines and end-of-life dates;
- which changes qualify for backport;
- whether backports are cherry-picked with `-x` or recreated;
- who owns release tags;
- how main receives every accepted fix;
- how CI tests old JDK and dependency constraints.

## Revert versus rollback

- **Git revert** changes source history through a new inverse commit.
- **Deployment rollback** promotes a previously built artifact.
- **Feature rollback** disables behavior using a flag.
- **Data rollback** restores or repairs persisted state.

They solve different layers. A service can roll back code but remain broken because the schema migrated or messages were already emitted.

## Interview questions and model answers

**How do you create an emergency fix when main is ahead of production?**

Branch from the exact production tag or supported release branch, make the smallest fix and regression test, use the protected emergency review path, build with the compatible toolchain, publish a new immutable version, verify production, then explicitly forward-port the fix to main.

**Why should a release artifact be built once?**

Promoting identical verified bytes preserves provenance and reduces environment-dependent rebuild differences. Configuration and secrets should normally be injected at deployment, and the artifact digest should connect source, CI, registry, and runtime.

**What can make application rollback unsafe?**

An incompatible database migration, irreversible external side effect, changed event schema, new configuration contract, or data written in a format the old version cannot read.

## Exercises

1. **Foundation - tags:** Create and inspect lightweight and annotated tags in a lab repository.
2. **Interview Core - traceability:** Define evidence connecting commit, tag, JAR digest, workflow, and deployment.
3. **Interview Core - hotfix:** Draw a release branch and forward-port path for a 2.8.1 security fix.
4. **Interview Core - rollback:** Classify source revert, artifact rollback, feature disablement, and data repair for five incidents.
5. **SDE-2 Follow-up:** Design a release policy for a Java library supporting two major lines and signed, immutable artifacts.

## Chapter summary

Release engineering binds source to immutable artifact and deployment evidence. Protect tags, build through trusted workflows, test compatibility, branch hotfixes from production reality, and make forward-port ownership explicit.

## Revision checklist

- [ ] I can distinguish tags from GitHub releases.
- [ ] I can explain annotated tag benefits.
- [ ] I can map source identity to artifact and deployment.
- [ ] I can execute and forward-port a hotfix.
- [ ] I distinguish revert, rollback, flag disablement, and data repair.
