# Vercel Deployment

The repository is configured as a static Vercel project. Vercel builds the same portal and MkDocs output used locally, then serves the contents of `site/`. No GitHub Actions workflow is required.

## Build Contract

`vercel.json` defines:

- `installCommand`: install only the pinned website dependencies from `requirements-web.txt`;
- `buildCommand`: build the portal and handbook, then normalize canonical deployment URLs;
- `outputDirectory`: publish only `site/`.

The full `requirements.txt` remains the local authoring and printable-book toolchain. Keeping the hosted dependency set separate avoids installing PDF and DOCX tooling during a static website build.

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
  .venv/bin/python scripts/configure_deployment_urls.py --check
```

The check is read-only. A real Vercel build runs the URL normalizer without `--check` after `site/` is created.

## Preview Review Checklist

- `/` opens the learning portal.
- `/docs/` opens the searchable handbook.
- A coding-foundation module opens from the portal and from MkDocs navigation.
- Search, progress state, keyboard navigation, and the mobile menu work.
- Mermaid diagrams and code blocks are readable in light and dark modes.
- `/robots.txt`, `/sitemap.xml`, and canonical metadata use the intended production origin.
- No generated output or `.vercel/` project state is committed.

## Production Policy

The production branch is `master`. Connecting Vercel to GitHub creates Vercel deployments on pushes; it does not enable repository GitHub Actions. Production promotion should wait for explicit approval of the preview UX and content.
