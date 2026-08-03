#!/usr/bin/env python3
"""Generate the instructional diagrams for focused Volume 06."""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

import diagram_kit


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "content" / "volumes" / "dsa" / "DSA-06-arrays-and-array-patterns" / "assets"
W, H = 2400, 1450
NAVY, BLUE, TEAL, GOLD = "#0B2545", "#1F5A94", "#17758A", "#C58A22"
GREEN, RED, INK, MUTED = "#2D7D66", "#A4423E", "#17212B", "#52606D"
LINE, WHITE = "#AAB8C6", "#FFFFFF"
PALE_BLUE, PALE_TEAL, PALE_GOLD = "#EAF2F8", "#EAF6F8", "#FFF6E3"
PALE_GREEN, PALE_RED, PALE_GRAY = "#EAF5F0", "#FBEDEC", "#F3F6F8"


def font(size: int, *, bold: bool = False, mono: bool = False) -> ImageFont.FreeTypeFont:
    # Delegates to diagram_kit, which reads only the vendored assets/fonts.
    # Searching system fonts first made the same generator produce different
    # diagrams per host, and diagrams are committed artifacts.
    return diagram_kit.font(size, bold=bold, mono=mono)


TITLE = font(67, bold=True)
SUB = font(34)
SECTION = font(39, bold=True)
BODY = font(33)
BOLD = font(33, bold=True)
SMALL = font(27)
MONO = font(35, bold=True, mono=True)
CELL = font(43, bold=True, mono=True)


def canvas(title: str, subtitle: str, number: str) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", (W, H), WHITE)
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, W, 188), fill=NAVY)
    draw.rectangle((0, 0, 24, 188), fill=GOLD)
    draw.text((78, 44), title, font=TITLE, fill=WHITE)
    draw.text((82, 119), subtitle, font=SUB, fill="#CFE1F2")
    draw.line((70, H - 70, W - 70, H - 70), fill=LINE, width=3)
    draw.text((72, H - 55), "Java SDE-2 DSA Series | Volume 06: Arrays", font=SMALL, fill=MUTED)
    width = draw.textlength(number, font=BOLD)
    draw.text((W - 72 - width, H - 58), number, font=BOLD, fill=NAVY)
    return image, draw


def center(draw: ImageDraw.ImageDraw, xy: tuple[float, float], text: str,
           used_font: ImageFont.FreeTypeFont = BODY, color: str = INK) -> None:
    draw.text(xy, text, font=used_font, fill=color, anchor="mm")


def box(draw: ImageDraw.ImageDraw, bounds: tuple[int, int, int, int],
        fill: str = PALE_GRAY, outline: str = BLUE, width: int = 4) -> None:
    draw.rounded_rectangle(bounds, radius=20, fill=fill, outline=outline, width=width)


def arrow(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int],
          color: str = BLUE, label: str | None = None) -> None:
    draw.line((*start, *end), fill=color, width=7)
    angle = math.atan2(end[1] - start[1], end[0] - start[0])
    head = 25
    points = [end,
              (end[0] - head * math.cos(angle - math.pi / 6), end[1] - head * math.sin(angle - math.pi / 6)),
              (end[0] - head * math.cos(angle + math.pi / 6), end[1] - head * math.sin(angle + math.pi / 6))]
    draw.polygon(points, fill=color)
    if label:
        center(draw, ((start[0] + end[0]) / 2, (start[1] + end[1]) / 2 - 28), label, SMALL, color)


def row(draw: ImageDraw.ImageDraw, values: list[str], x: int, y: int,
        width: int = 260, fills: list[str] | None = None) -> None:
    fills = fills or [WHITE] * len(values)
    for index, value in enumerate(values):
        x1 = x + index * width
        box(draw, (x1, y, x1 + width, y + 150), fills[index], LINE)
        center(draw, (x1 + width / 2, y + 75), value, CELL)
        center(draw, (x1 + width / 2, y - 32), str(index), SMALL, MUTED)


def save(image: Image.Image, filename: str) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    image.save(OUT / filename, "PNG", optimize=True, dpi=(240, 240))


def storage() -> Image.Image:
    image, draw = canvas("Values, references, and one array object", "Assignment copies a reference; it does not copy array contents", "Figure 01")
    box(draw, (120, 320, 720, 520), PALE_BLUE, BLUE)
    center(draw, (420, 375), "int[] first", SECTION, NAVY)
    center(draw, (420, 455), "reference value A42", MONO, BLUE)
    box(draw, (120, 750, 720, 950), PALE_BLUE, BLUE)
    center(draw, (420, 805), "int[] second", SECTION, NAVY)
    center(draw, (420, 885), "reference value A42", MONO, BLUE)
    row(draw, ["7", "4", "9", "2"], 1050, 535, 280, [PALE_GREEN] * 4)
    center(draw, (1610, 430), "one int[] object at A42", SECTION, GREEN)
    arrow(draw, (720, 420), (1040, 585), BLUE)
    arrow(draw, (720, 850), (1040, 630), BLUE)
    box(draw, (590, 1080, 2020, 1250), PALE_GOLD, GOLD)
    center(draw, (1305, 1130), "second[1] = 8 changes the shared object", BOLD, GOLD)
    center(draw, (1305, 1195), "first[1] is now 8 too", MONO, INK)
    return image


def logical_size() -> Image.Image:
    image, draw = canvas("Physical capacity vs logical size", "Insertions and deletions shift only the meaningful prefix", "Figure 02")
    row(draw, ["4", "8", "3", "6", "-", "-", "-"], 290, 365, 260,
        [PALE_GREEN] * 4 + [PALE_GRAY] * 3)
    draw.line((290, 590, 1330, 590), fill=GREEN, width=12)
    center(draw, (810, 640), "logical values [0, size), size = 4", BOLD, GREEN)
    draw.line((290, 730, 2110, 730), fill=BLUE, width=8)
    center(draw, (1200, 785), "physical capacity = 7", BOLD, BLUE)
    box(draw, (390, 900, 2010, 1180), PALE_GOLD, GOLD)
    center(draw, (1200, 960), "Insert 5 at index 1", SECTION, GOLD)
    center(draw, (1200, 1035), "shift [1, size) right, write 5, then size++", MONO)
    center(draw, (1200, 1110), "Never confuse unused capacity with valid data", BOLD, RED)
    return image


def opposing() -> Image.Image:
    image, draw = canvas("Opposing pointers eliminate impossible pairs", "Sorted order turns one comparison into a safe boundary move", "Figure 03")
    row(draw, ["1", "3", "4", "7", "10"], 325, 390, 350,
        [PALE_GOLD, WHITE, WHITE, PALE_GREEN, PALE_RED])
    arrow(draw, (500, 710), (500, 560), GOLD, "left")
    arrow(draw, (1900, 710), (1900, 560), RED, "right")
    box(draw, (510, 800, 1890, 1000), PALE_RED, RED)
    center(draw, (1200, 860), "1 + 10 > target 8", MONO, RED)
    center(draw, (1200, 935), "10 is too large even with the smallest candidate -> right--", BOLD)
    box(draw, (620, 1090, 1780, 1260), PALE_GREEN, GREEN)
    center(draw, (1200, 1145), "next: 1 + 7 = 8", MONO, GREEN)
    center(draw, (1200, 1210), "found indexes (0, 3)", BOLD, GREEN)
    return image


def compaction() -> Image.Image:
    image, draw = canvas("Read/write compaction", "The read pointer discovers; the write pointer preserves", "Figure 04")
    center(draw, (250, 340), "input", SECTION, NAVY)
    row(draw, ["0", "5", "0", "3", "2"], 420, 280, 310)
    center(draw, (250, 735), "result", SECTION, NAVY)
    row(draw, ["5", "3", "2", "?", "?"], 420, 675, 310,
        [PALE_GREEN] * 3 + [PALE_GRAY] * 2)
    arrow(draw, (885, 465), (575, 655), GREEN, "keep 5")
    arrow(draw, (1505, 465), (885, 655), GREEN, "keep 3")
    box(draw, (430, 1050, 1970, 1245), PALE_BLUE, BLUE)
    center(draw, (1200, 1110), "Invariant", SECTION, NAVY)
    center(draw, (1200, 1180), "[0, write) contains retained processed values in order", BOLD, BLUE)
    return image


def partition() -> Image.Image:
    image, draw = canvas("Three-way partition regions", "Unknown values shrink until every slot belongs to a final region", "Figure 05")
    row(draw, ["0", "0", "?", "?", "?", "2", "2"], 290, 410, 260,
        [PALE_GREEN] * 2 + [PALE_GOLD] * 3 + [PALE_RED] * 2)
    draw.line((290, 660, 810, 660), fill=GREEN, width=12)
    draw.line((810, 660, 1590, 660), fill=GOLD, width=12)
    draw.line((1590, 660, 2110, 660), fill=RED, width=12)
    center(draw, (550, 715), "[0, low): 0", BOLD, GREEN)
    center(draw, (1200, 715), "[current, high]: unknown", BOLD, GOLD)
    center(draw, (1850, 715), "(high, n): 2", BOLD, RED)
    box(draw, (430, 900, 1970, 1210), PALE_BLUE, BLUE)
    center(draw, (1200, 965), "Inspect values[current]", SECTION, NAVY)
    center(draw, (1200, 1040), "0 -> swap low/current; advance both", BOLD, GREEN)
    center(draw, (1200, 1100), "1 -> advance current", BOLD, GOLD)
    center(draw, (1200, 1160), "2 -> swap current/high; do not advance current", BOLD, RED)
    return image


def window() -> Image.Image:
    image, draw = canvas("Fixed sliding-window state", "Neighboring windows reuse k - 1 values", "Figure 06")
    row(draw, ["2", "1", "5", "1", "3", "2"], 270, 350, 310,
        [PALE_RED, PALE_GREEN, PALE_GREEN, PALE_GOLD, WHITE, WHITE])
    draw.line((270, 590, 1200, 590), fill=GREEN, width=12)
    center(draw, (735, 650), "old window [0, 3), sum = 8", BOLD, GREEN)
    arrow(draw, (425, 805), (425, 530), RED, "remove 2")
    arrow(draw, (1355, 805), (1355, 530), GOLD, "add 1")
    box(draw, (460, 940, 1940, 1190), PALE_BLUE, BLUE)
    center(draw, (1200, 1005), "new window [1, 4)", SECTION, NAVY)
    center(draw, (1200, 1080), "sum = 8 - 2 + 1 = 7", MONO, BLUE)
    center(draw, (1200, 1140), "Each element enters once and leaves once", BOLD, GREEN)
    return image


def prefix_difference() -> Image.Image:
    image, draw = canvas("Prefix queries and difference updates", "Sentinels remove boundary special cases", "Figure 07")
    center(draw, (1200, 285), "Prefix sum of [3, -1, 4, 2]", SECTION, NAVY)
    row(draw, ["0", "3", "2", "6", "8"], 420, 355, 310, [PALE_BLUE] * 5)
    box(draw, (350, 660, 2050, 820), PALE_GREEN, GREEN)
    center(draw, (1200, 715), "sum [1, 4) = prefix[4] - prefix[1]", MONO, GREEN)
    center(draw, (1200, 775), "8 - 3 = 5", BOLD, GREEN)
    center(draw, (1200, 945), "Inclusive range add [1, 3] by +2", SECTION, NAVY)
    row(draw, ["0", "+2", "0", "0", "-2", "0"], 270, 1020, 310,
        [WHITE, PALE_GOLD, WHITE, WHITE, PALE_RED, WHITE])
    center(draw, (1200, 1240), "start at left; stop at right + 1; prefix once to materialize", BOLD, BLUE)
    return image


def cyclic() -> Image.Image:
    image, draw = canvas("Cyclic placement", "For values 1..n, value v belongs at index v - 1", "Figure 08")
    row(draw, ["3", "4", "-1", "1"], 500, 355, 350,
        [PALE_GOLD, WHITE, PALE_RED, PALE_GREEN])
    arrow(draw, (675, 730), (1375, 535), GOLD, "3 -> index 2")
    center(draw, (1200, 830), "swap until current value is home or blocked by a duplicate", BOLD, NAVY)
    row(draw, ["1", "-1", "3", "4"], 500, 940, 350,
        [PALE_GREEN, PALE_RED, PALE_GREEN, PALE_GREEN])
    box(draw, (700, 1190, 1700, 1300), PALE_RED, RED)
    center(draw, (1200, 1245), "index 1 expects value 2 -> answer 2", BOLD, RED)
    return image


def matrix_rotation() -> Image.Image:
    image, draw = canvas("Clockwise matrix rotation", "Transpose across the diagonal, then reverse every row", "Figure 09")
    stages = [(["1  2  3", "4  5  6", "7  8  9"], 170, "original"),
              (["1  4  7", "2  5  8", "3  6  9"], 930, "transpose"),
              (["7  4  1", "8  5  2", "9  6  3"], 1690, "reverse rows")]
    for lines, x, label in stages:
        box(draw, (x, 410, x + 540, 940), PALE_BLUE, BLUE)
        center(draw, (x + 270, 350), label, SECTION, NAVY)
        for offset, line in enumerate(lines):
            center(draw, (x + 270, 520 + offset * 150), line, MONO)
    arrow(draw, (710, 675), (910, 675), TEAL)
    arrow(draw, (1470, 675), (1670, 675), TEAL)
    box(draw, (470, 1080, 1930, 1260), PALE_GREEN, GREEN)
    center(draw, (1200, 1140), "mapping: (row, column) -> (column, n - 1 - row)", MONO, GREEN)
    center(draw, (1200, 1210), "requires a square matrix for in-place rotation", BOLD, RED)
    return image


def decision_map() -> Image.Image:
    image, draw = canvas("Array pattern decision map", "Start with constraints and prove each pointer or state transition", "Figure 10")
    box(draw, (850, 260, 1550, 430), PALE_GOLD, GOLD)
    center(draw, (1200, 345), "What structure is promised?", SECTION, NAVY)
    nodes = [
        ((100, 630, 590, 880), "sorted", "two pointers\nbinary search", BLUE),
        ((670, 630, 1160, 880), "contiguous", "window / Kadane", TEAL),
        ((1240, 630, 1730, 880), "many ranges", "prefix / difference", GREEN),
        ((1810, 630, 2300, 880), "bounded values", "cyclic / marking", RED),
    ]
    for bounds, signal, answer, color in nodes:
        box(draw, bounds, WHITE, color)
        center(draw, ((bounds[0] + bounds[2]) / 2, 700), signal, SECTION, color)
        for line_no, line in enumerate(answer.split("\n")):
            center(draw, ((bounds[0] + bounds[2]) / 2, 785 + line_no * 50), line, BOLD)
        arrow(draw, (1200, 430), ((bounds[0] + bounds[2]) // 2, 620), color)
    box(draw, (360, 1050, 2040, 1250), PALE_BLUE, BLUE)
    center(draw, (1200, 1110), "Before coding: contract -> baseline -> invariant", SECTION, NAVY)
    center(draw, (1200, 1185), "Then verify mutation, overflow, complexity, and edge cases", BOLD, BLUE)
    return image


def main() -> None:
    figures = [
        (storage(), "01-array-storage-and-references.png"),
        (logical_size(), "02-logical-size-and-shifts.png"),
        (opposing(), "03-opposing-two-pointer-elimination.png"),
        (compaction(), "04-read-write-compaction.png"),
        (partition(), "05-three-way-partition-regions.png"),
        (window(), "06-sliding-window-state.png"),
        (prefix_difference(), "07-prefix-and-difference-state.png"),
        (cyclic(), "08-cyclic-placement.png"),
        (matrix_rotation(), "09-matrix-rotation.png"),
        (decision_map(), "10-pattern-decision-map.png"),
    ]
    for image, filename in figures:
        save(image, filename)
        print(f"generated {OUT / filename}")


if __name__ == "__main__":
    main()
