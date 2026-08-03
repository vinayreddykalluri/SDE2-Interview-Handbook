# Security, Reproducibility, and the Build Supply Chain

Builds execute plugins, resolve external metadata, download code, access credentials, and produce deployable bytes. Build configuration is production security code.

## Threat model

```text
source change ----+
plugin change ----+--> build runner --> artifact --> repository --> deployment
dependency bytes -+         ^                |
wrapper/toolchain +         |                +--> SBOM/provenance/signature
credentials ----------------+
```

Threats include dependency confusion, compromised repositories or publisher accounts, malicious plugins, tampered wrappers, poisoned caches, leaked credentials, mutable versions, and build scripts that execute untrusted input.

## Repository policy

Prefer approved HTTPS repositories and organization mirrors. Keep public and private namespaces distinct. Restrict which repository may serve which groups when the tool supports it.

Gradle can centralize repositories and fail project-local additions:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                maven("https://packages.example/internal")
            }
            filter {
                includeGroupByRegex("com[.]example([.].*)?")
            }
        }
        mavenCentral()
    }
}
```

Maven mirrors and repository-manager policy can route approved resolution. Mirror configuration needs careful `mirrorOf` reasoning; an overbroad or unreachable mirror can break plugin and dependency resolution for every project.

## Dependency verification

Gradle verification metadata can check dependency and plugin artifacts and metadata using expected checksums or signatures:

```bash
./gradlew --write-verification-metadata sha256 help
./gradlew build
```

Bootstrapping records what was resolved at that moment. Verify initial values through an independent trusted source before treating them as a security baseline.

Maven relies on repository transport, signatures/checksums, repository-manager controls, plugins, and organizational policy. Pin plugin and dependency versions, restrict repositories, and generate verifiable component inventories.

## Wrapper security

For Gradle, pin the distribution checksum in wrapper properties and validate the wrapper JAR. For Maven, review wrapper distribution URL/type and wrapper code. A pull request changing wrapper files deserves focused review because it changes code that runs before the project build.

Do not embed wrapper credentials in a committed distribution URL.

## Secrets

- inject short-lived credentials through the CI secret mechanism;
- use least-privilege publication tokens;
- separate read and write credentials;
- do not place secrets in build scripts, POMs, checked-in properties, or command history;
- avoid debug logs that print headers, environment, or effective settings;
- rotate first if a secret is exposed; deleting history is not revocation.

## SBOM, signature, provenance, and reproducibility

These provide different evidence:

| Evidence | Question answered |
|---|---|
| SBOM | which components are reported in the artifact/build? |
| checksum | are these bytes identical to expected bytes? |
| signature | did the holder of a signing identity sign these bytes/metadata? |
| provenance/attestation | which declared process and inputs produced the artifact? |
| reproducible rebuild | can an independent environment recreate identical bytes? |
| vulnerability scan | are known advisories associated with reported components? |

None alone proves absence of malicious behavior. Combine evidence and protect the systems that create it.

## Reproducibility checklist

- wrapper and plugin versions pinned;
- JDK/toolchain and release target controlled;
- no dynamic release dependencies;
- resolved graph recorded or constrained;
- timestamps, file ordering, permissions, and generated paths controlled;
- locale/encoding/timezone behavior tested;
- clean isolated rebuild compared by hash;
- publication uses the verified artifact, not a new rebuild.

## Incident: compromised dependency version

1. contain by blocking the coordinate/version and pausing affected releases;
2. identify all resolved graphs, artifacts, caches, and deployments containing it;
3. preserve logs, lock files, SBOMs, hashes, and repository audit records;
4. select or rebuild with a verified safe version;
5. invalidate poisoned caches under controlled procedures;
6. rotate secrets accessible to executed malicious code;
7. redeploy verified immutable artifacts;
8. add repository, verification, detection, and upgrade controls.

## Interview drill

**Question:** If a dependency checksum matches, is the dependency safe?

**Strong answer:** The checksum proves byte identity against the expected digest. Safety depends on how the expected digest was established, whether the publisher and repository were trusted, whether the component is vulnerable or malicious, and how it executes in context. I combine verification, provenance, SBOM/scanning, repository policy, and runtime controls.

## Practice

1. **Foundation:** Separate integrity, authenticity, provenance, and vulnerability evidence.
2. **Predict:** What happens if verification metadata is generated after repository compromise?
3. **Debugging:** A CI token appears in `--debug` output. State the first response.
4. **Interview Core:** Explain dependency-confusion defenses for an internal group.
5. **SDE-2 Follow-up:** Design read/write trust for repositories and build caches.

## Readiness check

- [ ] I treat plugins and wrappers as executable dependencies.
- [ ] I rotate exposed credentials before history cleanup.
- [ ] I do not overclaim what a checksum, signature, SBOM, or scan proves.
