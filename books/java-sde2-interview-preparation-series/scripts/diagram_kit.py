#!/usr/bin/env python3
"""Shared drawing toolkit for the instructional diagram generators.

Two defects motivated this module, and both were silent.

**Output paths.** Each generator hardcoded a path from before the volume
reorganization -- `content/volumes/06-arrays-and-array-patterns/assets` rather
than `content/volumes/dsa/DSA-06-arrays-and-array-patterns/assets`. The
generators did not fail. `mkdir(parents=True)` created the stale directory,
wrote the PNGs into it, and reported success while the assets the chapters
actually reference were never updated. `volume_assets()` resolves the
directory from the volume id and refuses to create a new one.

**Fonts.** Generators searched macOS font paths first and fell back to DejaVu,
so the same generator produced visibly different diagrams depending on the
host. Diagrams are committed artifacts, so this made regeneration a source of
spurious diffs. They now load only the fonts vendored under `assets/fonts`,
the same set the PDF build uses.

Everything else here is the house diagram style: palette, header band, footer
rule, and the primitives the volume generators compose.
"""

from __future__ import annotations

from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
FONT_DIR = ROOT / "assets" / "fonts"
VOLUMES = ROOT / "content" / "volumes"

# Canvas geometry. Chosen so a full-width diagram lands near 1:1.65 on the page
# at the series text width; changing it changes every diagram's scale.
W, H = 2400, 1450

# House palette. Matches the PDF theme colours in scripts/build_book.py.
NAVY, BLUE, TEAL, GOLD = "#0B2545", "#1F5A94", "#17758A", "#C58A22"
GREEN, RED, INK, MUTED = "#2D7D66", "#A4423E", "#17212B", "#52606D"
LINE, WHITE = "#AAB8C6", "#FFFFFF"
PALE_BLUE, PALE_TEAL, PALE_GOLD = "#EAF2F8", "#EAF6F8", "#FFF6E3"
PALE_GREEN, PALE_RED, PALE_GRAY = "#EAF5F0", "#FBEDEC", "#F3F6F8"

_FONT_FILES = {
    ("sans", False): "Lato-Regular.ttf",
    ("sans", True): "Lato-Bold.ttf",
    ("mono", False): "DejaVuSansMono.ttf",
    ("mono", True): "DejaVuSansMono-Bold.ttf",
}


def font(size: int, *, bold: bool = False, mono: bool = False) -> ImageFont.FreeTypeFont:
    """Load a vendored font. Missing files are a hard error, not a fallback."""
    filename = _FONT_FILES[("mono" if mono else "sans", bold)]
    path = FONT_DIR / filename
    if not path.is_file():
        raise RuntimeError(
            f"Missing bundled font {path}. Diagram generation reads only "
            "assets/fonts so that output does not depend on the host."
        )
    return ImageFont.truetype(str(path), size)


def volume_assets(volume_dir_name: str) -> Path:
    """Resolve a volume's assets directory, refusing to invent one.

    `volume_dir_name` is the directory under content/volumes/<segment>/, for
    example "DSA-17-dynamic-programming". The volume directory must already
    exist; only `assets/` is created. This is the guard that would have caught
    the stale-path bug, which wrote PNGs nothing read.
    """
    matches = [p for p in VOLUMES.glob(f"*/{volume_dir_name}") if p.is_dir()]
    if not matches:
        available = sorted(p.name for p in VOLUMES.glob("*/*") if p.is_dir())
        raise RuntimeError(
            f"No volume directory named {volume_dir_name!r} under {VOLUMES}. "
            f"Known volumes: {', '.join(available[:6])}..."
        )
    if len(matches) > 1:
        raise RuntimeError(f"Ambiguous volume name {volume_dir_name!r}: {matches}")
    assets = matches[0] / "assets"
    assets.mkdir(exist_ok=True)
    return assets


def markdown_path(volume_dir_name: str, filename: str) -> str:
    """The repo-relative path a chapter must use to reference a diagram."""
    assets = volume_assets(volume_dir_name)
    return str(assets.relative_to(ROOT) / filename)


def canvas(title: str, subtitle: str, footer: str) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    """A titled canvas with the series header band and footer rule."""
    image = Image.new("RGB", (W, H), WHITE)
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, W, 188), fill=NAVY)
    draw.rectangle((0, 0, 24, 188), fill=GOLD)
    draw.text((78, 44), title, font=font(67, bold=True), fill=WHITE)
    draw.text((82, 119), subtitle, font=font(34), fill="#CFE1F2")
    draw.line((70, H - 70, W - 70, H - 70), fill=LINE, width=3)
    draw.text((72, H - 55), footer, font=font(27), fill=MUTED)
    return image, draw


def box(draw: ImageDraw.ImageDraw, xy: tuple[int, int, int, int], *,
        fill: str = WHITE, outline: str = LINE, width: int = 3, radius: int = 14) -> None:
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def centered(draw: ImageDraw.ImageDraw, xy: tuple[int, int, int, int], text: str,
             *, f: ImageFont.FreeTypeFont, fill: str = INK) -> None:
    """Centre text inside a box, measured rather than estimated."""
    x0, y0, x1, y1 = xy
    left, top, right, bottom = draw.textbbox((0, 0), text, font=f)
    draw.text(
        (x0 + (x1 - x0 - (right - left)) / 2 - left,
         y0 + (y1 - y0 - (bottom - top)) / 2 - top),
        text, font=f, fill=fill)


def arrow(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int],
          *, colour: str = BLUE, width: int = 5, head: int = 22) -> None:
    """A straight arrow with a filled triangular head."""
    import math
    draw.line((start, end), fill=colour, width=width)
    angle = math.atan2(end[1] - start[1], end[0] - start[0])
    draw.polygon(
        [end,
         (end[0] - head * math.cos(angle - math.pi / 7),
          end[1] - head * math.sin(angle - math.pi / 7)),
         (end[0] - head * math.cos(angle + math.pi / 7),
          end[1] - head * math.sin(angle + math.pi / 7))],
        fill=colour)


def legend(draw: ImageDraw.ImageDraw, x: int, y: int,
           entries: Iterable[tuple[str, str]], *, swatch: int = 34) -> None:
    """A colour key. Entries are (colour, label) pairs."""
    f = font(29)
    for index, (colour, label) in enumerate(entries):
        top = y + index * (swatch + 18)
        draw.rounded_rectangle((x, top, x + swatch, top + swatch),
                               radius=7, fill=colour, outline=LINE, width=2)
        draw.text((x + swatch + 18, top + 2), label, font=f, fill=MUTED)


def save(image: Image.Image, volume_dir_name: str, filename: str) -> Path:
    path = volume_assets(volume_dir_name) / filename
    image.save(path, "PNG", optimize=True)
    return path
