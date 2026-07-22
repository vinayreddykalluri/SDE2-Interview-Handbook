# Learning Portal

The portal is a lightweight, responsive shell around the canonical handbook. It provides track selection, module discovery, progress state, and links into the searchable MkDocs site.

## Ownership

- `index.html` and `assets/` own portal presentation and behavior.
- `content/coding-foundations.json` owns compact discovery metadata for the 19 foundation modules.
- Canonical lesson prose remains under `docs/` and is not duplicated here.
- `scripts/build_site.py` copies this shell to `site/` and mounts MkDocs at `site/docs/`.

Run `make validate-web` after portal changes and `make serve-web` to inspect the complete local experience at [http://127.0.0.1:8000/](http://127.0.0.1:8000/).

The GitHub Pages URL embedded in deployment metadata is a future deployment target. Public hosting remains disabled until the local product is approved.

