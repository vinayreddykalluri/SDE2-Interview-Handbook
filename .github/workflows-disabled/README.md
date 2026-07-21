# GitHub Actions disabled

GitHub Actions are intentionally disabled while the handbook is validated locally.
GitHub only discovers workflow files under `.github/workflows/`, so the YAML files in
this directory cannot run on pushes or through manual dispatch.

Use the [local development guide](../../LOCAL_DEVELOPMENT.md) to bootstrap, validate,
build, and serve the complete handbook before restoring automation.

When local approval is complete, restore automation with:

```bash
mkdir -p .github/workflows
git mv .github/workflows-disabled/build-books.yml .github/workflows/build-books.yml
git mv .github/workflows-disabled/deploy-pages.yml .github/workflows/deploy-pages.yml
git commit -m "Enable handbook GitHub Actions"
git push origin master
```
