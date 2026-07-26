# Applications

Deployable reader experiences belong here. Each application owns its presentation assets and compact delivery metadata; canonical educational content remains under `docs/` or `books/`.

| Application | Purpose | Build command |
|---|---|---|
| [`portal/`](portal/) | Responsive landing page, learning-path discovery, progress controls, and the synchronized PDF catalog | `make build-site` |

The generated deployment artifact is written to the ignored root `site/` directory.
