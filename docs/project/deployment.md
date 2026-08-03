# Vercel Deployment

The repository is configured as a static Vercel project. Vercel builds the same portal, concise handbook, and complete web-book library used locally, then serves the contents of `site/`.

## Why the PDFs are not deployed

`.vercelignore` excludes `books/java-sde2-interview-preparation-series/dist/*` and keeps only `manifest.json`. The 42 PDFs are 163 MB, which is above the 100 MB Hobby upload limit and would be pointless to serve twice — every download link in the site points at GitHub instead.

This has one non-obvious consequence, and it broke the build until it was fixed. The generated book pages are compiled by `mkdocs build --strict`, which fails on any internal link it cannot resolve. Link rewriting used to resolve PDF targets by looking for the file on disk, so with `dist/` stripped a relative link such as `../00-start-here/Java-SDE2-Interview-Preparation-Series-Index.pdf` resolved to nothing, survived into the generated Markdown as a relative path, and aborted the deployment.

`build_book_web_library.py` now resolves PDF links by **name, from `dist/manifest.json`**, and always emits an absolute GitHub URL. The manifest is deliberately the one file kept out of the ignore rule. If you add a PDF, it must appear in the manifest or its links will not rewrite.

To reproduce a hosted build locally, delete the PDFs from a scratch copy and build:

```bash
cp -r . /tmp/vercel-sim && cd /tmp/vercel-sim
find books/java-sde2-interview-preparation-series/dist -name '*.pdf' -delete
python3 tooling/automation/build_site.py     # must succeed with dist/ empty
grep -roh 'href="[^"]*\.pdf"' site/ | grep -v '^href="https://'   # must print nothing
```

## Free-tier (Hobby) constraints that apply here

| Limit | Value | Status |
|---|---|---|
| Source upload | 100 MB | 23 MB after `.vercelignore` — fine |
| Source file count | 15,000 | ~1,000 — fine |
| Build time | 45 min | ~1 min — fine |
| Output files | no hard cap | 933 — fine |
| Fast Data Transfer | 100 GB/month | See the note below |
| Commercial use | not permitted on Hobby | Move to Pro if the books are ever monetized |
| Git organization repos | not supported on Hobby | Repo is under a personal account — fine |

The one number worth watching is transfer. Generated pages average about 219 KB because MkDocs Material inlines the full navigation tree for 407 documents into every page, so the library is roughly 147 MB of HTML. At 100 GB/month that supports a few hundred thousand page views, which is ample — but if it ever becomes a constraint, the fix is `navigation.prune` in `mkdocs.yml`, which emits only the visible portion of the nav per page. The `Cache-Control` headers in `vercel.json` already keep hashed theme assets out of repeat transfers.

## Build Contract

`vercel.json` defines:

- `installCommand`: install only the pinned website dependencies from `tooling/requirements/portal.txt`;
- `buildCommand`: build the portal, handbook, and all complete web books, then normalize canonical deployment URLs;
- `outputDirectory`: publish only `site/`.

The full `tooling/requirements/authoring.txt` manifest remains the local authoring and printable-book toolchain. Keeping the hosted dependency set separate avoids installing PDF and DOCX tooling during a static website build.

## Import from GitHub

1. In Vercel, choose **Add New Project** and import `vinayreddykalluri/SDE2-Interview-Handbook`.
2. Keep the root directory as `.`.
3. Use the **Other** framework preset if Vercel asks; committed settings in `vercel.json` provide the commands.
4. Do not override the install command, build command, or output directory in the dashboard.
5. Enable automatic exposure of Vercel system environment variables, or set `PUBLIC_SITE_URL` to the final HTTPS origin.
6. deploy a preview and complete the review checklist below before promoting it to production.

`PUBLIC_SITE_URL` takes precedence over `VERCEL_PROJECT_PRODUCTION_URL`. Supply an origin only, for example `https://handbook.example.com`, without a trailing slash.

## Local Contract Check

```bash
make validate-deployment
make build-site
PUBLIC_SITE_URL=https://handbook.example.com \
  .venv/bin/python tooling/automation/configure_deployment_urls.py --check
```

The check is read-only. A real Vercel build runs the URL normalizer without `--check` after `site/` is created.

## Preview Review Checklist

- `/` opens the learning portal.
- `/docs/` opens the searchable handbook.
- `/books/` opens the 40-book searchable library, and every book exposes its contents and code index.
- A coding-foundation module opens from the portal and from MkDocs navigation.
- A portal book card opens its complete web reader and current PDF without a broken route.
- Search, progress state, keyboard navigation, and the mobile menu work.
- Mermaid diagrams and code blocks are readable in light and dark modes.
- `/robots.txt`, `/sitemap.xml`, and canonical metadata use the intended production origin.
- No generated output or `.vercel/` project state is committed.

## Production Policy

The production branch is `master`. Connecting Vercel to GitHub creates Vercel deployments on pushes, independently of the repository's GitHub Actions. Production promotion should wait for explicit approval of the preview UX and content.

Vercel and GitHub Pages now both publish from `master`, so the same commit is served from two origins. `configure_deployment_urls.py` rewrites canonical URLs to whichever origin is building, which keeps the two from competing for search ranking. Set `PUBLIC_SITE_URL` on the Vercel project to the origin you want treated as canonical.
