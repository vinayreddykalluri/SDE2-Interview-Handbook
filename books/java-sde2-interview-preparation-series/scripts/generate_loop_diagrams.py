#!/usr/bin/env python3
"""Generate precise loop and index diagrams for focused Volume 05."""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

import diagram_kit


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "content" / "volumes" / "dsa" / "DSA-05-loop-mastery-and-index-calculations" / "assets"

W, H = 2400, 1450
NAVY = "#0B2545"
BLUE = "#1F5A94"
TEAL = "#17758A"
GOLD = "#C58A22"
GREEN = "#2D7D66"
RED = "#A4423E"
INK = "#17212B"
MUTED = "#52606D"
LINE = "#AAB8C6"
PALE_BLUE = "#EAF2F8"
PALE_TEAL = "#EAF6F8"
PALE_GOLD = "#FFF6E3"
PALE_GREEN = "#EAF5F0"
PALE_RED = "#FBEDEC"
PALE_GRAY = "#F3F6F8"
WHITE = "#FFFFFF"


def font(size: int, *, bold: bool = False, mono: bool = False) -> ImageFont.FreeTypeFont:
    # Delegates to diagram_kit, which reads only the vendored assets/fonts.
    # Searching system fonts first made the same generator produce different
    # diagrams per host, and diagrams are committed artifacts.
    return diagram_kit.font(size, bold=bold, mono=mono)


F_TITLE = font(68, bold=True)
F_SUB = font(34)
F_SECTION = font(40, bold=True)
F_BODY = font(34)
F_BODY_BOLD = font(34, bold=True)
F_SMALL = font(27)
F_SMALL_BOLD = font(27, bold=True)
F_MONO = font(31, mono=True)
F_MONO_BOLD = font(31, bold=True, mono=True)
F_CELL = font(44, bold=True, mono=True)


def canvas(title: str, subtitle: str, figure: str) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", (W, H), WHITE)
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, W, 188), fill=NAVY)
    draw.rectangle((0, 0, 24, 188), fill=GOLD)
    draw.text((78, 45), title, font=F_TITLE, fill=WHITE)
    draw.text((82, 118), subtitle, font=F_SUB, fill="#CFE1F2")
    draw.line((70, H - 70, W - 70, H - 70), fill=LINE, width=3)
    draw.text((72, H - 55), "Java SDE-2 DSA Series | Volume 05: Loop Mastery", font=F_SMALL, fill=MUTED)
    width = draw.textlength(figure, font=F_SMALL_BOLD)
    draw.text((W - 72 - width, H - 55), figure, font=F_SMALL_BOLD, fill=NAVY)
    return image, draw


def center(draw: ImageDraw.ImageDraw, point: tuple[float, float], text: str, used_font: ImageFont.FreeTypeFont, fill: str = INK) -> None:
    draw.text(point, text, font=used_font, fill=fill, anchor="mm")


def box(draw: ImageDraw.ImageDraw, bounds: tuple[int, int, int, int], *, fill: str = PALE_GRAY, outline: str = BLUE, width: int = 4, radius: int = 22) -> None:
    draw.rounded_rectangle(bounds, radius=radius, fill=fill, outline=outline, width=width)


def arrow(draw: ImageDraw.ImageDraw, start: tuple[float, float], end: tuple[float, float], *, color: str = BLUE, width: int = 7, label: str | None = None) -> None:
    sx, sy = start
    ex, ey = end
    draw.line((sx, sy, ex, ey), fill=color, width=width)
    angle = math.atan2(ey - sy, ex - sx)
    head = 25
    left = (ex - head * math.cos(angle - math.pi / 6), ey - head * math.sin(angle - math.pi / 6))
    right = (ex - head * math.cos(angle + math.pi / 6), ey - head * math.sin(angle + math.pi / 6))
    draw.polygon(((ex, ey), left, right), fill=color)
    if label:
        center(draw, ((sx + ex) / 2, (sy + ey) / 2 - 20), label, F_SMALL_BOLD, color)


def array_row(draw: ImageDraw.ImageDraw, values: list[str], x: int, y: int, cell_w: int = 220, fills: list[str] | None = None, outlines: list[str] | None = None) -> None:
    fills = fills or [WHITE] * len(values)
    outlines = outlines or [LINE] * len(values)
    for index, value in enumerate(values):
        x1 = x + index * cell_w
        box(draw, (x1, y, x1 + cell_w, y + 150), fill=fills[index], outline=outlines[index], radius=12)
        center(draw, (x1 + cell_w / 2, y + 75), value, F_CELL)
        center(draw, (x1 + cell_w / 2, y - 32), str(index), F_SMALL_BOLD, MUTED)


def save(image: Image.Image, name: str) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    image.save(OUT / name, format="PNG", optimize=True, dpi=(240, 240))


def loop_lifecycle() -> Image.Image:
    image, draw = canvas("How a Java loop executes", "State changes follow a fixed control-flow cycle", "Figure 01")
    nodes = [
        ((100, 330, 520, 510), "1. initialize", "int i = 0", PALE_BLUE, BLUE),
        ((760, 300, 1260, 540), "2. test condition", "i < n ?", PALE_GOLD, GOLD),
        ((1510, 330, 1970, 510), "3. execute body", "use values[i]", PALE_GREEN, GREEN),
        ((1510, 770, 1970, 950), "4. update", "i++", PALE_TEAL, TEAL),
        ((760, 800, 1260, 1040), "5. repeat or exit", "true -> body\nfalse -> after loop", PALE_BLUE, BLUE),
    ]
    for bounds, title, body, fill, outline in nodes:
        box(draw, bounds, fill=fill, outline=outline, width=5)
        center(draw, ((bounds[0] + bounds[2]) / 2, bounds[1] + 55), title, F_SECTION, outline)
        for line_index, line in enumerate(body.split("\n")):
            center(draw, ((bounds[0] + bounds[2]) / 2, bounds[1] + 125 + 48 * line_index), line, F_MONO_BOLD)
    arrow(draw, (520, 420), (760, 420), label="once")
    arrow(draw, (1260, 420), (1510, 420), color=GREEN, label="true")
    arrow(draw, (1740, 510), (1740, 770), color=TEAL)
    arrow(draw, (1510, 860), (1260, 920), color=TEAL)
    arrow(draw, (1010, 800), (1010, 540), color=BLUE, label="next test")
    arrow(draw, (760, 420), (520, 920), color=RED, label="false")
    box(draw, (100, 820, 520, 1020), fill=PALE_RED, outline=RED, width=5)
    center(draw, (310, 880), "after loop", F_SECTION, RED)
    center(draw, (310, 950), "condition is false", F_BODY)
    box(draw, (290, 1150, 2110, 1285), fill=PALE_GRAY, outline=LINE, width=3)
    center(draw, (1200, 1195), "continue in a for loop jumps to UPDATE, then retests", F_BODY_BOLD, NAVY)
    center(draw, (1200, 1245), "continue in a while loop jumps directly to the CONDITION", F_BODY_BOLD, TEAL)
    return image


def half_open_range() -> Image.Image:
    image, draw = canvas("Half-open range and loop invariant", "At boundary i, [0, i) is processed and [i, n) remains", "Figure 02")
    values = ["8", "3", "5", "2", "9", "4", "7"]
    fills = [PALE_GREEN] * 3 + [PALE_GOLD] + [PALE_GRAY] * 3
    outlines = [GREEN] * 3 + [GOLD] + [LINE] * 3
    array_row(draw, values, 315, 430, 250, fills, outlines)
    draw.line((315, 650, 1065, 650), fill=GREEN, width=12)
    center(draw, (690, 700), "processed prefix [0, i)", F_BODY_BOLD, GREEN)
    draw.line((1065, 650, 2065, 650), fill=BLUE, width=12)
    center(draw, (1565, 700), "remaining suffix [i, n)", F_BODY_BOLD, BLUE)
    arrow(draw, (1190, 320), (1190, 410), color=GOLD, label="i = 3")
    box(draw, (300, 820, 2100, 1055), fill=PALE_BLUE, outline=BLUE, width=5)
    center(draw, (1200, 875), "Invariant before the next iteration", F_SECTION, NAVY)
    center(draw, (1200, 950), "sum equals exactly values[0] + values[1] + values[2]", F_MONO_BOLD, BLUE)
    center(draw, (1200, 1005), "values[i] has not been incorporated yet", F_BODY_BOLD, GOLD)
    box(draw, (470, 1140, 1930, 1285), fill=PALE_GREEN, outline=GREEN, width=4)
    center(draw, (1200, 1185), "progress measure: n - i", F_SECTION, GREEN)
    center(draw, (1200, 1245), "each update decreases it by one until i == n", F_BODY)
    return image


def two_pointers() -> Image.Image:
    image, draw = canvas("Opposing two pointers", "Sortedness justifies eliminating one boundary at a time", "Figure 03")
    values = ["1", "3", "4", "7", "10"]
    center(draw, (1200, 260), "Target = 8", F_SECTION, NAVY)
    array_row(draw, values, 325, 390, 350, [PALE_GOLD, WHITE, WHITE, WHITE, PALE_RED], [GOLD, LINE, LINE, LINE, RED])
    arrow(draw, (500, 680), (500, 555), color=GOLD, label="left")
    arrow(draw, (1900, 680), (1900, 555), color=RED, label="right")
    box(draw, (560, 740, 1840, 875), fill=PALE_RED, outline=RED)
    center(draw, (1200, 785), "1 + 10 = 11 > 8", F_MONO_BOLD, RED)
    center(draw, (1200, 835), "10 is too large with the smallest candidate -> right--", F_BODY_BOLD)
    array_row(draw, values, 325, 1010, 350, [PALE_GOLD, WHITE, WHITE, PALE_GREEN, PALE_RED], [GOLD, LINE, LINE, GREEN, RED])
    arrow(draw, (500, 1300), (500, 1175), color=GOLD, label="left")
    arrow(draw, (1550, 1300), (1550, 1175), color=GREEN, label="right")
    center(draw, (1200, 930), "Next state: candidate interval [0, 3]", F_BODY_BOLD, BLUE)
    center(draw, (1200, 1325), "1 + 7 = 8 -> found indexes (0, 3)", F_BODY_BOLD, GREEN)
    return image


def compaction() -> Image.Image:
    image, draw = canvas("Read/write compaction", "read discovers; write marks the next retained position", "Figure 04")
    original = ["2", "1", "2", "3", "2"]
    center(draw, (300, 295), "Input", F_SECTION, NAVY)
    array_row(draw, original, 420, 330, 310)
    center(draw, (300, 650), "Keep 1", F_SECTION, GREEN)
    array_row(draw, ["1", "?", "?", "?", "?"], 420, 590, 310, [PALE_GREEN] + [PALE_GRAY] * 4, [GREEN] + [LINE] * 4)
    arrow(draw, (885, 505), (575, 570), color=GREEN)
    center(draw, (1700, 540), "read 1 -> write 0", F_BODY_BOLD, GREEN)
    center(draw, (300, 950), "Keep 3", F_SECTION, GREEN)
    array_row(draw, ["1", "3", "?", "?", "?"], 420, 890, 310, [PALE_GREEN] * 2 + [PALE_GRAY] * 3, [GREEN] * 2 + [LINE] * 3)
    arrow(draw, (1350, 805), (885, 870), color=GREEN)
    center(draw, (1700, 845), "read 3 -> write 1", F_BODY_BOLD, GREEN)
    box(draw, (470, 1170, 1930, 1305), fill=PALE_BLUE, outline=BLUE)
    center(draw, (1200, 1215), "Invariant: [0, write) contains retained items", F_BODY_BOLD, NAVY)
    center(draw, (1200, 1265), "write <= read, so unread data is never overwritten", F_BODY_BOLD, BLUE)
    return image


def windows() -> Image.Image:
    image, draw = canvas("Sliding-window state", "One value enters; zero or more values leave", "Figure 05")
    center(draw, (600, 270), "Fixed width k = 3", F_SECTION, NAVY)
    array_row(draw, ["4", "-1", "2", "10", "-3"], 140, 390, 390, [PALE_RED, PALE_GREEN, PALE_GREEN, PALE_GOLD, WHITE], [RED, GREEN, GREEN, GOLD, LINE])
    draw.line((140, 610, 1310, 610), fill=GREEN, width=12)
    center(draw, (725, 655), "old [0, 3): sum 5", F_BODY_BOLD, GREEN)
    arrow(draw, (335, 755), (335, 570), color=RED, label="leave 4")
    arrow(draw, (1505, 755), (1505, 570), color=GOLD, label="enter 10")
    center(draw, (1780, 270), "Variable validity", F_SECTION, NAVY)
    array_row(draw, ["1", "2", "1", "2", "3"], 140, 880, 390, [PALE_RED, PALE_RED, PALE_RED, PALE_GREEN, PALE_GOLD], [RED, RED, RED, GREEN, GOLD])
    center(draw, (1200, 1090), "adding 3 creates three distinct values", F_BODY_BOLD, RED)
    arrow(draw, (335, 1190), (1115, 1190), color=RED, label="left advances until valid")
    box(draw, (520, 1240, 1880, 1340), fill=PALE_GREEN, outline=GREEN)
    center(draw, (1200, 1290), "final valid window [3, 5) = [2, 3]", F_BODY_BOLD, GREEN)
    return image


def aggregate_moves() -> Image.Image:
    image, draw = canvas("Nested syntax, linear aggregate work", "Count pointer movement across the whole method", "Figure 06")
    headers = ["right", "value", "left after shrink", "left moves", "pairs added"]
    rows = [
        ["0", "1", "0", "0", "0"],
        ["1", "2", "0", "0", "1"],
        ["2", "4", "0", "0", "2"],
        ["3", "7", "2", "2", "1"],
    ]
    x0, y0 = 190, 330
    widths = [340, 340, 520, 420, 420]
    x = x0
    for header, width in zip(headers, widths):
        draw.rectangle((x, y0, x + width, y0 + 100), fill=NAVY)
        center(draw, (x + width / 2, y0 + 50), header, F_BODY_BOLD, WHITE)
        x += width
    for row_index, row in enumerate(rows):
        y = y0 + 100 + row_index * 125
        x = x0
        fill = PALE_BLUE if row_index % 2 == 0 else WHITE
        for value, width in zip(row, widths):
            draw.rectangle((x, y, x + width, y + 125), fill=fill, outline=LINE, width=3)
            center(draw, (x + width / 2, y + 62), value, F_MONO_BOLD)
            x += width
    box(draw, (300, 980, 2100, 1170), fill=PALE_GREEN, outline=GREEN, width=5)
    center(draw, (1200, 1035), "right moves n times; left moves at most n times", F_SECTION, GREEN)
    center(draw, (1200, 1110), "total pointer moves <= 2n -> O(n), despite nested syntax", F_BODY_BOLD, NAVY)
    center(draw, (1200, 1260), "Never multiply loop bounds before checking whether the inner pointer resets.", F_BODY_BOLD, RED)
    return image


def grid_mapping() -> Image.Image:
    image, draw = canvas("Flatten and unflatten indexes", "A 3 x 4 row-major grid maps cells to one linear range", "Figure 07")
    start_x, start_y = 320, 310
    cell_w, cell_h = 430, 230
    flat = 0
    for row in range(3):
        for col in range(4):
            x1 = start_x + col * cell_w
            y1 = start_y + row * cell_h
            selected = row == 2 and col == 1
            box(draw, (x1, y1, x1 + cell_w - 18, y1 + cell_h - 18), fill=PALE_GOLD if selected else PALE_BLUE, outline=GOLD if selected else BLUE, width=6 if selected else 3, radius=12)
            center(draw, (x1 + 205, y1 + 70), f"({row}, {col})", F_MONO_BOLD, GOLD if selected else NAVY)
            center(draw, (x1 + 205, y1 + 145), f"flat {flat}", F_CELL, INK)
            flat += 1
    arrow(draw, (945, 1000), (945, 860), color=GOLD)
    center(draw, (1200, 1025), "selected cell (row 2, col 1)", F_BODY_BOLD, GOLD)
    box(draw, (280, 1080, 2120, 1290), fill=PALE_GREEN, outline=GREEN, width=5)
    center(draw, (1200, 1135), "flatten: row * cols + col = 2 * 4 + 1 = 9", F_MONO_BOLD, GREEN)
    center(draw, (1200, 1200), "unflatten: row = 9 / 4 = 2; col = 9 % 4 = 1", F_MONO_BOLD, NAVY)
    center(draw, (1200, 1260), "cast before multiplication when dimensions may overflow int", F_BODY_BOLD, RED)
    return image


def spiral() -> Image.Image:
    image, draw = canvas("Spiral traversal boundaries", "Emit one edge, then shrink the unvisited rectangle", "Figure 08")
    start_x, start_y = 300, 300
    cell_w, cell_h = 360, 210
    rows, cols = 4, 5
    order = [(0,0),(0,1),(0,2),(0,3),(0,4),(1,4),(2,4),(3,4),(3,3),(3,2),(3,1),(3,0),(2,0),(1,0)]
    order_map = {cell: idx + 1 for idx, cell in enumerate(order)}
    for row in range(rows):
        for col in range(cols):
            x1 = start_x + col * cell_w
            y1 = start_y + row * cell_h
            outer = row in (0, rows - 1) or col in (0, cols - 1)
            fill = PALE_GREEN if outer else PALE_GOLD
            outline = GREEN if outer else GOLD
            box(draw, (x1, y1, x1 + cell_w - 18, y1 + cell_h - 18), fill=fill, outline=outline, width=4, radius=10)
            label = f"visit {order_map[(row,col)]}" if (row,col) in order_map else "next ring"
            center(draw, (x1 + 170, y1 + 95), label, F_MONO_BOLD, outline)
    draw.rectangle((start_x - 20, start_y - 20, start_x + cols * cell_w - 2, start_y + rows * cell_h - 2), outline=GREEN, width=12)
    draw.rectangle((start_x + cell_w - 20, start_y + cell_h - 20, start_x + 4 * cell_w - 2, start_y + 3 * cell_h - 2), outline=GOLD, width=12)
    box(draw, (430, 1195, 1970, 1330), fill=PALE_BLUE, outline=BLUE)
    center(draw, (1200, 1240), "guards prevent revisiting a final row or column", F_BODY_BOLD, NAVY)
    center(draw, (1200, 1290), "invariant: outside current bounds is emitted exactly once", F_BODY_BOLD, BLUE)
    return image


def lower_bound() -> Image.Image:
    image, draw = canvas("Half-open lower-bound search", "Find the first index whose value is at least target", "Figure 09")
    values = ["1", "3", "5", "7", "9"]
    center(draw, (1200, 260), "target = 6 | interval starts [low, high) = [0, 5)", F_SECTION, NAVY)
    steps = [
        (380, 0, 5, 2, "values[2] = 5 < 6 -> low = 3"),
        (720, 3, 5, 4, "values[4] = 9 >= 6 -> high = 4"),
        (1060, 3, 4, 3, "values[3] = 7 >= 6 -> high = 3"),
    ]
    for y, low, high, mid, text in steps:
        fills = []
        outlines = []
        for index in range(5):
            if index == mid:
                fills.append(PALE_GOLD); outlines.append(GOLD)
            elif low <= index < high:
                fills.append(PALE_BLUE); outlines.append(BLUE)
            else:
                fills.append(PALE_GRAY); outlines.append(LINE)
        array_row(draw, values, 360, y, 250, fills, outlines)
        center(draw, (300, y + 75), f"[{low}, {high})", F_MONO_BOLD, BLUE)
        center(draw, (1940, y + 75), text, F_BODY_BOLD, GOLD if mid in range(low, high) else NAVY)
    center(draw, (1200, 1305), "low == high == 3 -> insertion point and first value >= 6", F_SECTION, GREEN)
    return image


def enhanced_for() -> Image.Image:
    image, draw = canvas("What enhanced-for means", "Java uses different iteration machinery for arrays and Iterable values", "Figure 10")
    box(draw, (150, 300, 1080, 1050), fill=PALE_BLUE, outline=BLUE, width=5)
    center(draw, (615, 370), "Enhanced-for over an array", F_SECTION, BLUE)
    center(draw, (615, 460), "for (int value : values)", F_MONO_BOLD)
    arrow(draw, (615, 520), (615, 620), color=BLUE)
    center(draw, (615, 670), "index = 0", F_MONO)
    center(draw, (615, 735), "index < values.length", F_MONO)
    center(draw, (615, 800), "value = values[index]", F_MONO)
    center(draw, (615, 865), "index++", F_MONO)
    center(draw, (615, 965), "value is a copy for primitive elements", F_BODY_BOLD, NAVY)
    box(draw, (1320, 300, 2250, 1050), fill=PALE_GREEN, outline=GREEN, width=5)
    center(draw, (1785, 370), "Enhanced-for over Iterable", F_SECTION, GREEN)
    center(draw, (1785, 460), "for (Item item : items)", F_MONO_BOLD)
    arrow(draw, (1785, 520), (1785, 620), color=GREEN)
    center(draw, (1785, 670), "Iterator<Item> it = items.iterator()", F_MONO)
    center(draw, (1785, 750), "while (it.hasNext())", F_MONO)
    center(draw, (1785, 830), "Item item = it.next()", F_MONO)
    center(draw, (1785, 965), "use iterator.remove() when supported", F_BODY_BOLD, NAVY)
    box(draw, (440, 1150, 1960, 1300), fill=PALE_RED, outline=RED, width=4)
    center(draw, (1200, 1195), "Do not structurally mutate a collection behind its iterator", F_BODY_BOLD, RED)
    center(draw, (1200, 1250), "fail-fast detection is best effort, not a concurrency guarantee", F_BODY, INK)
    return image


def main() -> None:
    diagrams = [
        (loop_lifecycle(), "01-loop-execution-lifecycle.png"),
        (half_open_range(), "02-half-open-range-invariant.png"),
        (two_pointers(), "03-opposing-two-pointers.png"),
        (compaction(), "04-read-write-compaction.png"),
        (windows(), "05-sliding-window-state.png"),
        (aggregate_moves(), "06-aggregate-pointer-movement.png"),
        (grid_mapping(), "07-flatten-unflatten-grid.png"),
        (spiral(), "08-spiral-boundaries.png"),
        (lower_bound(), "09-lower-bound-half-open-search.png"),
        (enhanced_for(), "10-enhanced-for-desugaring.png"),
    ]
    for image, filename in diagrams:
        save(image, filename)
        print(OUT / filename)


if __name__ == "__main__":
    main()
