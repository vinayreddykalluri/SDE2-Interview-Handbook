# Build and Validation Scripts

Scripts are deterministic orchestration entry points. Use the stable `Makefile` targets instead of invoking scripts directly unless you are developing the tooling.

| Script | Responsibility |
|---|---|
| `bootstrap_macos.sh` | Install and configure the supported local macOS toolchain |
| `check_local_environment.py` | Report required and optional local software |
| `build_site.py` | Assemble the portal and MkDocs handbook into `site/` |
| `build_pdf.py` | Render module and combined PDF books |
| `build_docx.py` | Render module and combined DOCX books |
| `build_all.py` | Run all local output builders |
| `validate_repository_layout.py` | Enforce the single-root repository contract |
| `validate_structure.py` | Validate curriculum hierarchy and MkDocs navigation |
| `validate_links.py` | Check internal documentation references |
| `validate_java_examples.py` | Compile and smoke-run Java examples |
| `validate_web.py` | Validate portal metadata, assets, and JavaScript |
| `configure_deployment_urls.py` | Normalize canonical URLs in a hosted static artifact |
| `validate_deployment.py` | Validate the Vercel build and dependency contract |

Generated files belong in ignored output directories. Scripts must not modify canonical documentation or source examples as a side effect of validation.
