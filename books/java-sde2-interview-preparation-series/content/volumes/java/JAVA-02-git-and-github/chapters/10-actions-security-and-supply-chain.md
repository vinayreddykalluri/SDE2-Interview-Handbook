# Actions Security, Secrets, Dependencies, and Supply-Chain Integrity

A repository workflow can read code, mint tokens, access caches, publish packages, deploy infrastructure, and modify pull requests. Treat it as privileged application code with untrusted inputs.

## Learning objectives

- apply least privilege to `GITHUB_TOKEN` and secrets;
- prevent expression-based script injection;
- explain the risk of `pull_request_target` and untrusted checkout;
- pin actions and separate validation from delivery;
- use dependency review, secret scanning, push protection, signing, OIDC, and attestations appropriately;
- respond correctly when a secret enters Git history.

## Threat model

Potentially untrusted input includes:

- pull request code from forks;
- branch names, commit messages, issue and PR titles or bodies;
- action and reusable-workflow implementations;
- dependencies, build plugins, wrapper binaries, container images;
- downloaded artifacts and caches;
- generated files and test fixtures;
- self-hosted runner state.

An attacker needs only one path from untrusted input to a privileged interpreter, token, cache, or deployment.

## Least-privilege token permissions

GitHub creates a job-scoped `GITHUB_TOKEN` for workflows. Declare permissions explicitly:

```yaml
permissions:
  contents: read
```

Grant a job more only when required:

```yaml
jobs:
  publish-report:
    permissions:
      contents: read
      pull-requests: write
```

Avoid repository-wide `write-all`. Separate read-only test jobs from the one job that comments, publishes, or deploys. Environments can add approval and secret boundaries for production.

## Script injection through GitHub expressions

Unsafe:

```yaml
- name: Validate title
  run: echo "${{ github.event.pull_request.title }}" | grep '^JAVA-'
```

The title is attacker-controlled text interpolated into a shell script. Quoting is easy to get wrong because the workflow expression is substituted before the shell interprets the script.

Safer: pass data through an environment variable and quote it as data:

```yaml
- name: Validate title
  env:
    PR_TITLE: ${{ github.event.pull_request.title }}
  run: |
    case "$PR_TITLE" in
      JAVA-*) exit 0 ;;
      *) echo "title must start with JAVA-"; exit 1 ;;
    esac
```

Even better, use a well-reviewed action or program that receives structured input without constructing shell code. Treat branch names, labels, email addresses, and filenames as untrusted too.

## The `pull_request_target` danger

`pull_request_target` runs in the context of the target repository's default branch and can support safe metadata operations such as labeling. It can have access to secrets or write privileges that an ordinary fork pull-request workflow does not.

Dangerous pattern:

```yaml
on: pull_request_target
steps:
  - uses: actions/checkout@v6
    with:
      ref: ${{ github.event.pull_request.head.sha }}
  - run: ./mvnw verify
```

This checks out and executes untrusted PR code in a privileged context. A modified wrapper or build plugin can steal credentials, change repository state, or poison shared cache.

Safe design choices:

- use `pull_request` with read-only permissions to build untrusted code;
- use `pull_request_target` only for metadata that does not check out or execute untrusted content;
- use carefully designed workflow separation for privileged follow-up, treating artifacts as untrusted until verified;
- prefer short-lived, narrowly scoped credentials and protected environments.

## Pin actions to immutable identities

Tags can move. Pin third-party actions to a verified full-length commit SHA:

```yaml
- uses: actions/checkout@<VERIFIED_FULL_COMMIT_SHA> # v6.x.y
```

Use dependency automation to propose verified updates and review the action diff/changelog. Organization or repository policy can require full-SHA pinning. Reusable workflows referenced by mutable branches create a similar trust concern; use organizational policy and reviewed versioning.

## Fork pull-request permissions

Fork PR workflows generally receive reduced permissions and should not receive ordinary repository secrets. Design tests to run without secrets:

- use local service containers with test-only values;
- mock external services at a clear contract boundary;
- split secret-requiring integration tests into a controlled trusted workflow;
- require approval before running first-time contributor workflows if policy uses that safeguard;
- never print context objects or environment values indiscriminately.

Secret redaction is a defense, not a guarantee. Encoded, transformed, or structured values may evade masking.

## Dependency review and Java supply chain

For pull requests that change Maven or Gradle dependencies, dependency review can surface added, removed, or changed dependencies and known vulnerability or license information. The dependency review action can become a required check where available.

Review also needs Java-specific context:

- plugin and annotation-processor code executes during build;
- repositories control where artifacts are resolved;
- transitive dependencies can change without direct source import;
- snapshots and dynamic versions reduce reproducibility;
- a compromised dependency can run in tests or production;
- generated dependency graphs can be submitted when platform discovery is incomplete.

Dependabot alerts identify vulnerable dependencies already present; dependency review tries to stop new risk during a PR. Neither replaces runtime security, architecture review, or a controlled upgrade plan.

## Secret scanning and push protection

Push protection can block supported secret patterns before they enter a repository. If a block is real:

1. do not bypass for convenience;
2. remove the secret from every affected commit in the proposed push;
3. rotate or revoke any credential that may already have escaped;
4. replace it with a secret-manager reference or protected configuration;
5. inspect logs, forks, caches, artifacts, releases, and deployments for exposure;
6. document the incident.

Delegated bypass can route exceptional requests to designated reviewers. An approval means the push is allowed; it does not make a real credential safe.

## Secret committed locally versus pushed

### Latest local commit, never shared

Rotate if there is any chance the credential escaped through logs, backups, or tooling. Remove it, amend the commit, and verify all commits to be pushed:

```bash
git commit --amend --all
git log -p origin/main..HEAD
```

### Pushed to any remote

Assume compromise. Rotation is first. Deleting the file in a later commit leaves earlier history accessible. Coordinated history rewriting may reduce exposure, but clones, forks, caches, build logs, package artifacts, and provider audit logs may retain copies. Follow the organization's incident process.

## OIDC for cloud deployment

OpenID Connect lets a workflow exchange a GitHub-issued identity token for a short-lived cloud credential, avoiding long-lived cloud secrets in GitHub. The cloud trust policy must restrict claims such as repository, branch/tag, workflow, and protected environment.

```yaml
permissions:
  contents: read
  id-token: write
```

`id-token: write` permits requesting an identity token; it does not itself grant cloud access. The cloud-side trust and role permissions determine access. A broad subject condition can turn short-lived credentials into broad compromise.

## Signing, tags, releases, and attestations

- Signed commits or tags provide verifiable signer evidence when keys and identities are managed correctly.
- Protected tag rules can restrict movement or deletion of release tags.
- Artifact attestations can provide cryptographic provenance linking a build artifact to a workflow identity and source revision.
- Immutable release features, where enabled, can prevent published release tags/assets from being changed and provide release integrity evidence.

These controls complement one another. A signed malicious commit remains malicious. A trustworthy pipeline must secure inputs, workflow code, dependencies, build environment, identity, and publication.

## Self-hosted runner boundary

Self-hosted runners can retain filesystem state, network access, credentials, and process effects between jobs unless carefully isolated. Running arbitrary fork code on a persistent runner can expose the organization. Use ephemeral isolated runners for untrusted code, restrict network and credentials, patch images, and separate runner groups by trust.

## Interview questions and model answers

**What is the most dangerous GitHub Actions anti-pattern?**

Executing untrusted pull-request code in a privileged workflow context, especially with secrets, write token permissions, shared cache, or a self-hosted runner. A common form is checking out a fork head under `pull_request_target` and running its build.

**Why pin an action by full commit SHA?**

It selects immutable Git content instead of a movable tag. I verify that SHA belongs to the authentic action repository, keep a readable version comment, and use reviewed automation to update it.

**If a secret is deleted in the next commit, is the incident over?**

No. It remains in prior history and may be in clones, logs, artifacts, caches, or forks. Revoke or rotate first, remove it from active code and relevant history through the incident plan, scan retained systems, and document impact.

## Exercises

1. **Foundation - permissions:** Reduce a workflow from `write-all` to per-job permissions.
2. **Interview Core - injection:** Repair three steps that interpolate PR titles, branch names, or filenames into shell code.
3. **Interview Core - trust split:** Design read-only fork validation and a separately approved deployment workflow.
4. **Interview Core - incident:** Write the first 30 minutes of a response to a cloud key pushed to a public branch.
5. **SDE-2 Follow-up:** Threat-model a self-hosted runner that builds public contributions and publishes Maven packages.

## Chapter summary

Workflow security starts with trust boundaries: untrusted code and metadata must not reach privileged interpreters, credentials, runners, caches, or deployment paths. Use least privilege, immutable action identities, fork-safe validation, dependency review, push protection, short-lived cloud identity, and coordinated incident response.

## Revision checklist

- [ ] I declare minimal token permissions.
- [ ] I do not interpolate untrusted expressions into shell code.
- [ ] I understand why privileged untrusted checkout is dangerous.
- [ ] I can respond to a secret leak with rotation first.
- [ ] I can explain OIDC, signing, and attestations without overstating them.
