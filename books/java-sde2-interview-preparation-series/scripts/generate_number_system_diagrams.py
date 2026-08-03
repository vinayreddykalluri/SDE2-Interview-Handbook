#!/usr/bin/env python3
"""Generate high-resolution diagrams for Volume 01: Number Systems.

The diagrams use the navy, blue, gold, green, and neutral palette from the
main Java SDE-2 book. All labels are ASCII so the rendered output remains
portable across the PDF and DOCX publishing paths.
"""

from __future__ import annotations

import argparse
import math
from pathlib import Path
from typing import Callable, Iterable

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUT = (
    ROOT
    / "content"
    / "volumes"
    / "dsa"
    / "DSA-02-03-number-systems-and-math-foundations"
    / "assets"
)

W, H = 2400, 1500
NAVY = "#0B2545"
BLUE = "#1F5A94"
CYAN = "#2F80A8"
GOLD = "#C58A22"
GREEN = "#2D7D66"
RED = "#A4423E"
INK = "#17212B"
MUTED = "#52606D"
LINE = "#AAB8C6"
PALE_BLUE = "#EAF2F8"
PALE_CYAN = "#EAF6F8"
PALE_GOLD = "#FFF6E3"
PALE_GREEN = "#EAF5F0"
PALE_RED = "#FBEDEC"
PALE_GRAY = "#F3F6F8"
WHITE = "#FFFFFF"


def font(size: int, *, bold: bool = False, mono: bool = False) -> ImageFont.FreeTypeFont:
    if mono:
        candidates = [
            "/System/Library/Fonts/Supplemental/Courier New Bold.ttf"
            if bold
            else "/System/Library/Fonts/Supplemental/Courier New.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"
            if bold
            else "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
        ]
    else:
        candidates = [
            "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
            if bold
            else "/System/Library/Fonts/Supplemental/Arial.ttf",
            "/System/Library/Fonts/Supplemental/Helvetica.ttc",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
            if bold
            else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        ]
    for candidate in candidates:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size=size)
    return ImageFont.load_default(size=size)


F_TITLE = font(72, bold=True)
F_SUBTITLE = font(36)
F_SECTION = font(44, bold=True)
F_BOX_TITLE = font(38, bold=True)
F_BODY = font(36)
F_BODY_BOLD = font(36, bold=True)
F_SMALL = font(29)
F_SMALL_BOLD = font(29, bold=True)
F_DIGIT = font(74, bold=True, mono=True)
F_MONO = font(34, mono=True)
F_MONO_BOLD = font(34, bold=True, mono=True)


def new_canvas(title: str, subtitle: str, figure: str) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", (W, H), WHITE)
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, W, 192), fill=NAVY)
    draw.rectangle((0, 0, 24, 192), fill=GOLD)
    draw.text((78, 35), title, font=F_TITLE, fill=WHITE)
    draw.text((82, 119), subtitle, font=F_SUBTITLE, fill="#CFE1F2")
    draw.line((70, H - 70, W - 70, H - 70), fill=LINE, width=3)
    draw.text(
        (72, H - 55),
        "Java SDE-2 DSA Series | Volume 01: Number Systems and Math Foundations",
        font=F_SMALL,
        fill=MUTED,
    )
    label_width = draw.textlength(figure, font=F_SMALL_BOLD)
    draw.text((W - 72 - label_width, H - 55), figure, font=F_SMALL_BOLD, fill=NAVY)
    return image, draw


def center_text(
    draw: ImageDraw.ImageDraw,
    center: tuple[float, float],
    text: str,
    used_font: ImageFont.FreeTypeFont,
    fill: str = INK,
) -> None:
    draw.text(center, text, font=used_font, fill=fill, anchor="mm")


def wrapped_lines(
    draw: ImageDraw.ImageDraw,
    text: str,
    max_width: int,
    used_font: ImageFont.FreeTypeFont,
) -> list[str]:
    result: list[str] = []
    for paragraph in text.split("\n"):
        if not paragraph:
            result.append("")
            continue
        words = paragraph.split()
        current = ""
        for word in words:
            probe = word if not current else f"{current} {word}"
            if draw.textlength(probe, font=used_font) <= max_width:
                current = probe
            else:
                if current:
                    result.append(current)
                current = word
        if current:
            result.append(current)
    return result


def draw_wrapped(
    draw: ImageDraw.ImageDraw,
    position: tuple[int, int],
    text: str,
    max_width: int,
    used_font: ImageFont.FreeTypeFont = F_BODY,
    fill: str = INK,
    line_gap: int = 9,
) -> int:
    x, y = position
    line_height = used_font.getbbox("Ag")[3] - used_font.getbbox("Ag")[1] + line_gap
    for line in wrapped_lines(draw, text, max_width, used_font):
        draw.text((x, y), line, font=used_font, fill=fill)
        y += line_height
    return y


def rounded_box(
    draw: ImageDraw.ImageDraw,
    bounds: tuple[int, int, int, int],
    *,
    fill: str = PALE_GRAY,
    outline: str = BLUE,
    width: int = 4,
    radius: int = 24,
) -> None:
    draw.rounded_rectangle(bounds, radius=radius, fill=fill, outline=outline, width=width)


def titled_box(
    draw: ImageDraw.ImageDraw,
    bounds: tuple[int, int, int, int],
    title: str,
    body: str,
    *,
    fill: str = PALE_BLUE,
    outline: str = BLUE,
) -> None:
    x1, y1, x2, y2 = bounds
    rounded_box(draw, bounds, fill=fill, outline=outline)
    draw.rounded_rectangle((x1, y1, x2, y1 + 68), radius=24, fill=outline)
    draw.rectangle((x1, y1 + 34, x2, y1 + 68), fill=outline)
    center_text(draw, ((x1 + x2) / 2, y1 + 34), title, F_BOX_TITLE, WHITE)
    draw_wrapped(draw, (x1 + 24, y1 + 91), body, x2 - x1 - 48, F_BODY)


def arrow(
    draw: ImageDraw.ImageDraw,
    start: tuple[float, float],
    end: tuple[float, float],
    *,
    color: str = BLUE,
    width: int = 7,
    head: int = 25,
    label: str | None = None,
) -> None:
    sx, sy = start
    ex, ey = end
    draw.line((sx, sy, ex, ey), fill=color, width=width)
    angle = math.atan2(ey - sy, ex - sx)
    left = (
        ex - head * math.cos(angle - math.pi / 6),
        ey - head * math.sin(angle - math.pi / 6),
    )
    right = (
        ex - head * math.cos(angle + math.pi / 6),
        ey - head * math.sin(angle + math.pi / 6),
    )
    draw.polygon(((ex, ey), left, right), fill=color)
    if label:
        mx = (sx + ex) / 2
        my = (sy + ey) / 2
        bbox = draw.textbbox((0, 0), label, font=F_SMALL_BOLD)
        tw = bbox[2] - bbox[0]
        th = bbox[3] - bbox[1]
        rounded_box(
            draw,
            (int(mx - tw / 2 - 13), int(my - th / 2 - 12), int(mx + tw / 2 + 13), int(my + th / 2 + 12)),
            fill=WHITE,
            outline=color,
            width=2,
            radius=12,
        )
        center_text(draw, (mx, my - 1), label, F_SMALL_BOLD, color)


def save(image: Image.Image, out_dir: Path, filename: str) -> Path:
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / filename
    image.save(path, format="PNG", optimize=True, dpi=(240, 240))
    return path


def decimal_place_value() -> Image.Image:
    image, draw = new_canvas(
        "Decimal place value",
        "A digit contributes digit x power of 10",
        "Figure 01",
    )
    digits = ["5", "3", "2", "0", "7"]
    place_names = ["Ten-thousands", "Thousands", "Hundreds", "Tens", "Ones"]
    powers = ["10^4", "10^3", "10^2", "10^1", "10^0"]
    contributions = ["50,000", "3,000", "200", "0", "7"]
    colors = [NAVY, BLUE, CYAN, GREEN, GOLD]
    fills = [PALE_BLUE, PALE_BLUE, PALE_CYAN, PALE_GREEN, PALE_GOLD]

    start_x = 105
    cell_w = 420
    gap = 35
    for index, digit in enumerate(digits):
        x1 = start_x + index * (cell_w + gap)
        x2 = x1 + cell_w
        center_text(draw, ((x1 + x2) / 2, 265), place_names[index], F_SMALL_BOLD, colors[index])
        rounded_box(draw, (x1, 315, x2, 600), fill=fills[index], outline=colors[index], width=6)
        center_text(draw, ((x1 + x2) / 2, 420), digit, F_DIGIT, colors[index])
        center_text(draw, ((x1 + x2) / 2, 515), powers[index], F_MONO_BOLD, INK)
        center_text(draw, ((x1 + x2) / 2, 560), f"weight {10 ** (4 - index):,}", F_SMALL, MUTED)
        rounded_box(draw, (x1, 660, x2, 805), fill=WHITE, outline=LINE, width=3)
        center_text(draw, ((x1 + x2) / 2, 710), f"{digit} x {powers[index]}", F_MONO, INK)
        center_text(draw, ((x1 + x2) / 2, 765), f"= {contributions[index]}", F_BODY_BOLD, colors[index])

    rounded_box(draw, (155, 905, W - 155, 1115), fill=PALE_GOLD, outline=GOLD, width=5)
    center_text(draw, (W / 2, 968), "Expanded form", F_SECTION, NAVY)
    center_text(
        draw,
        (W / 2, 1050),
        "50,000 + 3,000 + 200 + 0 + 7 = 53,207",
        F_MONO_BOLD,
        INK,
    )
    center_text(
        draw,
        (W / 2, 1240),
        "Same digit, different position -> different value",
        F_BODY_BOLD,
        NAVY,
    )
    return image


def binary_place_value() -> Image.Image:
    image, draw = new_canvas(
        "Binary place value",
        "Each position is a power of 2; each bit is 0 or 1",
        "Figure 02",
    )
    bits = [1, 0, 1, 1, 0, 1]
    exponents = [5, 4, 3, 2, 1, 0]
    cell_w = 335
    gap = 35
    start_x = 110

    for index, (bit, exponent) in enumerate(zip(bits, exponents)):
        x1 = start_x + index * (cell_w + gap)
        x2 = x1 + cell_w
        center_text(draw, ((x1 + x2) / 2, 275), f"bit {exponent}", F_SMALL_BOLD, NAVY)
        fill = PALE_GOLD if bit else PALE_GRAY
        outline = GOLD if bit else LINE
        rounded_box(draw, (x1, 325, x2, 600), fill=fill, outline=outline, width=6)
        center_text(draw, ((x1 + x2) / 2, 420), str(bit), F_DIGIT, GOLD if bit else MUTED)
        center_text(draw, ((x1 + x2) / 2, 515), f"2^{exponent}", F_MONO_BOLD, INK)
        center_text(draw, ((x1 + x2) / 2, 560), f"weight {2 ** exponent}", F_SMALL, MUTED)
        rounded_box(draw, (x1, 665, x2, 805), fill=WHITE, outline=outline, width=3)
        center_text(draw, ((x1 + x2) / 2, 715), f"{bit} x {2 ** exponent}", F_MONO, INK)
        center_text(draw, ((x1 + x2) / 2, 765), f"= {bit * (2 ** exponent)}", F_BODY_BOLD, GOLD if bit else MUTED)

    rounded_box(draw, (130, 910, W - 130, 1130), fill=PALE_BLUE, outline=BLUE, width=5)
    center_text(draw, (W / 2, 972), "Binary to decimal", F_SECTION, NAVY)
    center_text(draw, (W / 2, 1052), "32 + 0 + 8 + 4 + 0 + 1 = 45", F_MONO_BOLD, INK)
    center_text(draw, (W / 2, 1098), "101101 base 2 = 45 base 10", F_BODY_BOLD, BLUE)

    rounded_box(draw, (360, 1205, W - 360, 1320), fill=PALE_GOLD, outline=GOLD, width=4)
    center_text(draw, (W / 2, 1262), "Move left: weight doubles | Move right: weight halves", F_BODY_BOLD, NAVY)
    return image


def repeated_division() -> Image.Image:
    image, draw = new_canvas(
        "Decimal to binary by repeated division",
        "Divide by 2, record remainders, then read from bottom to top",
        "Figure 03",
    )
    rows = [
        (45, 22, 1),
        (22, 11, 0),
        (11, 5, 1),
        (5, 2, 1),
        (2, 1, 0),
        (1, 0, 1),
    ]
    draw.text((105, 250), "Division steps", font=F_SECTION, fill=NAVY)
    x1, x2, x3, x4 = 110, 610, 1050, 1420
    y = 330
    row_h = 125
    for step, (value, quotient, remainder) in enumerate(rows, start=1):
        fill = PALE_BLUE if step % 2 else PALE_GRAY
        rounded_box(draw, (x1, y, x4, y + 92), fill=fill, outline=LINE, width=3, radius=16)
        center_text(draw, (x1 + 70, y + 46), str(step), F_SMALL_BOLD, MUTED)
        center_text(draw, ((x1 + x2) / 2, y + 46), f"{value} / 2 = {quotient}", F_MONO_BOLD, INK)
        center_text(draw, ((x2 + x3) / 2, y + 46), "remainder", F_SMALL, MUTED)
        rounded_box(draw, (x3, y + 10, x4 - 18, y + 82), fill=PALE_GOLD, outline=GOLD, width=4, radius=14)
        center_text(draw, ((x3 + x4 - 18) / 2, y + 46), str(remainder), F_MONO_BOLD, GOLD)
        y += row_h

    panel_x1, panel_x2 = 1580, 2265
    rounded_box(draw, (panel_x1, 300, panel_x2, 1110), fill=PALE_GOLD, outline=GOLD, width=5)
    center_text(draw, ((panel_x1 + panel_x2) / 2, 365), "Remainder stack", F_SECTION, NAVY)
    stack_values = [1, 0, 1, 1, 0, 1]
    stack_y = 445
    for index, remainder in enumerate(stack_values):
        rounded_box(
            draw,
            (1745, stack_y, 2100, stack_y + 82),
            fill=WHITE,
            outline=GOLD if index == 0 else LINE,
            width=4,
            radius=12,
        )
        center_text(draw, (1922, stack_y + 41), str(remainder), F_MONO_BOLD, GOLD if index == 0 else INK)
        stack_y += 94
    arrow(draw, (2170, 1000), (2170, 455), color=GOLD, label="read upward")
    center_text(draw, ((panel_x1 + panel_x2) / 2, 1060), "101101", F_DIGIT, NAVY)

    rounded_box(draw, (260, 1165, W - 260, 1315), fill=PALE_GREEN, outline=GREEN, width=5)
    center_text(draw, (W / 2, 1215), "Result", F_BOX_TITLE, GREEN)
    center_text(draw, (W / 2, 1270), "45 base 10 = 101101 base 2", F_MONO_BOLD, INK)
    return image


def positional_accumulation() -> Image.Image:
    image, draw = new_canvas(
        "Base to decimal by positional accumulation",
        "Read left to right: value = value x base + digit",
        "Figure 04",
    )
    rounded_box(draw, (185, 235, W - 185, 355), fill=PALE_GOLD, outline=GOLD, width=5)
    center_text(draw, (W / 2, 295), "value = value x base + digit", F_MONO_BOLD, NAVY)

    headers = ["Step", "Digit", "Before", "Calculation", "After"]
    widths = [240, 260, 320, 760, 320]
    table_x = 250
    table_y = 425
    row_h = 112
    x_positions = [table_x]
    for width in widths:
        x_positions.append(x_positions[-1] + width)

    for column, header in enumerate(headers):
        draw.rectangle(
            (x_positions[column], table_y, x_positions[column + 1], table_y + row_h),
            fill=NAVY,
            outline=WHITE,
            width=2,
        )
        center_text(
            draw,
            ((x_positions[column] + x_positions[column + 1]) / 2, table_y + row_h / 2),
            header,
            F_BOX_TITLE,
            WHITE,
        )

    digits = [1, 0, 1, 1, 0, 1]
    before = [0, 1, 2, 5, 11, 22]
    after = [1, 2, 5, 11, 22, 45]
    for row in range(6):
        y1 = table_y + (row + 1) * row_h
        y2 = y1 + row_h
        fill = PALE_BLUE if row % 2 == 0 else WHITE
        values = [
            str(row + 1),
            str(digits[row]),
            str(before[row]),
            f"{before[row]} x 2 + {digits[row]}",
            str(after[row]),
        ]
        for column, value in enumerate(values):
            draw.rectangle(
                (x_positions[column], y1, x_positions[column + 1], y2),
                fill=fill,
                outline=LINE,
                width=2,
            )
            used_font = F_MONO_BOLD if column in (1, 3, 4) else F_BODY
            fill_color = GOLD if column == 1 and digits[row] else INK
            center_text(
                draw,
                ((x_positions[column] + x_positions[column + 1]) / 2, (y1 + y2) / 2),
                value,
                used_font,
                fill_color,
            )

    rounded_box(draw, (210, 1225, 1115, 1335), fill=PALE_GREEN, outline=GREEN, width=4)
    center_text(draw, (662, 1280), "101101 base 2 = 45 base 10", F_BODY_BOLD, GREEN)
    rounded_box(draw, (1285, 1225, 2190, 1335), fill=PALE_RED, outline=RED, width=4)
    center_text(draw, (1737, 1260), "Before each step", F_SMALL_BOLD, RED)
    center_text(draw, (1737, 1305), "validate digit and check overflow", F_SMALL, INK)
    return image


def java_primitive_ranges() -> Image.Image:
    image, draw = new_canvas(
        "Java primitive integer ranges",
        "Signed width determines the minimum and maximum representable values",
        "Figure 05",
    )
    rounded_box(draw, (210, 235, W - 210, 345), fill=PALE_GOLD, outline=GOLD, width=5)
    center_text(
        draw,
        (W / 2, 290),
        "signed w-bit range: -2^(w-1) through 2^(w-1) - 1",
        F_MONO_BOLD,
        NAVY,
    )

    headers = ["Type", "Bits", "Minimum", "Maximum", "Common interview use"]
    widths = [260, 230, 580, 580, 560]
    x_positions = [90]
    for width in widths:
        x_positions.append(x_positions[-1] + width)
    table_y = 405
    row_h = 124

    for column, header in enumerate(headers):
        draw.rectangle(
            (x_positions[column], table_y, x_positions[column + 1], table_y + row_h),
            fill=NAVY,
            outline=WHITE,
            width=2,
        )
        center_text(draw, ((x_positions[column] + x_positions[column + 1]) / 2, table_y + row_h / 2), header, F_SMALL_BOLD, WHITE)

    rows = [
        ("byte", "8", "-128", "127", "bytes and compact storage"),
        ("short", "16", "-32,768", "32,767", "compact storage; rare arithmetic"),
        ("int", "32", "-2,147,483,648", "2,147,483,647", "indexes, counts, most inputs"),
        ("long", "64", "-9,223,372,036,854,775,808", "9,223,372,036,854,775,807", "large sums, products, time"),
        ("char", "16", "0", "65,535", "UTF-16 code unit; unsigned"),
    ]
    row_colors = [BLUE, CYAN, GREEN, GOLD, RED]
    for row_index, row in enumerate(rows):
        y1 = table_y + (row_index + 1) * row_h
        y2 = y1 + row_h
        fill = PALE_GRAY if row_index % 2 else WHITE
        for column, value in enumerate(row):
            draw.rectangle((x_positions[column], y1, x_positions[column + 1], y2), fill=fill, outline=LINE, width=2)
            used_font = F_SMALL_BOLD if column == 0 else F_SMALL
            color = row_colors[row_index] if column == 0 else INK
            center_text(draw, ((x_positions[column] + x_positions[column + 1]) / 2, (y1 + y2) / 2), value, used_font, color)

    titled_box(
        draw,
        (100, 1120, 760, 1325),
        "Promotion",
        "byte, short, and char arithmetic normally evaluates as int.",
        fill=PALE_BLUE,
        outline=BLUE,
    )
    titled_box(
        draw,
        (870, 1120, 1530, 1325),
        "Safe widening",
        "Cast an operand first: long product = (long) a * b;",
        fill=PALE_GREEN,
        outline=GREEN,
    )
    titled_box(
        draw,
        (1640, 1120, 2300, 1325),
        "Not a substitute",
        "float and double are approximate, not wider exact integers.",
        fill=PALE_GOLD,
        outline=GOLD,
    )
    return image


def overflow_wraparound() -> Image.Image:
    image, draw = new_canvas(
        "Signed integer overflow and wraparound",
        "Ordinary Java integer arithmetic keeps the low fixed-width bits",
        "Figure 06",
    )
    center_text(draw, (W / 2, 270), "32-bit int boundary", F_SECTION, NAVY)

    rounded_box(draw, (120, 340, 1010, 590), fill=PALE_GOLD, outline=GOLD, width=6)
    center_text(draw, (565, 405), "Integer.MAX_VALUE", F_BOX_TITLE, NAVY)
    center_text(draw, (565, 480), "2,147,483,647", F_MONO_BOLD, INK)
    center_text(draw, (565, 545), "all value bits are 1", F_SMALL, MUTED)

    rounded_box(draw, (1390, 340, 2280, 590), fill=PALE_RED, outline=RED, width=6)
    center_text(draw, (1835, 405), "Integer.MIN_VALUE", F_BOX_TITLE, NAVY)
    center_text(draw, (1835, 480), "-2,147,483,648", F_MONO_BOLD, INK)
    center_text(draw, (1835, 545), "sign bit is 1; value bits are 0", F_SMALL, MUTED)

    arrow(draw, (1015, 420), (1380, 420), color=RED, label="+ 1 wraps")
    arrow(draw, (1380, 540), (1015, 540), color=GOLD, label="- 1 wraps")

    rounded_box(draw, (125, 685, 1115, 1005), fill=PALE_RED, outline=RED, width=5)
    center_text(draw, (620, 745), "Unsafe: narrow evaluation happens first", F_BOX_TITLE, RED)
    center_text(draw, (620, 825), "long wrong = intA * intB;", F_MONO_BOLD, INK)
    center_text(draw, (620, 890), "int product wraps, then widens", F_BODY, MUTED)
    center_text(draw, (620, 950), "(a, b) -> a - b", F_MONO, INK)

    rounded_box(draw, (1285, 685, 2275, 1005), fill=PALE_GREEN, outline=GREEN, width=5)
    center_text(draw, (1780, 745), "Safe: widen or compare before the risk", F_BOX_TITLE, GREEN)
    center_text(draw, (1780, 825), "long safe = (long) intA * intB;", F_MONO_BOLD, INK)
    center_text(draw, (1780, 890), "int mid = left + (right - left) / 2;", F_MONO, INK)
    center_text(draw, (1780, 950), "Integer.compare(a, b)", F_MONO, INK)

    rounded_box(draw, (300, 1110, W - 300, 1305), fill=PALE_BLUE, outline=BLUE, width=5)
    center_text(draw, (W / 2, 1165), "When overflow is invalid", F_BOX_TITLE, NAVY)
    center_text(
        draw,
        (W / 2, 1230),
        "Use Math.addExact, Math.multiplyExact, Math.toIntExact, or BigInteger",
        F_BODY_BOLD,
        INK,
    )
    center_text(draw, (W / 2, 1275), "Check the contract; do not silently choose a policy.", F_SMALL, MUTED)
    return image


def euclidean_gcd() -> Image.Image:
    image, draw = new_canvas(
        "Euclidean GCD process",
        "Replace (a, b) with (b, a % b) until the remainder is zero",
        "Figure 07",
    )
    pairs = [(252, 105), (105, 42), (42, 21), (21, 0)]
    x_values = [105, 690, 1275, 1860]
    for index, (left, right) in enumerate(pairs):
        outline = GOLD if right == 0 else BLUE
        fill = PALE_GOLD if right == 0 else PALE_BLUE
        rounded_box(draw, (x_values[index], 310, x_values[index] + 430, 545), fill=fill, outline=outline, width=6)
        center_text(draw, (x_values[index] + 215, 375), f"Step {index}", F_SMALL_BOLD, outline)
        center_text(draw, (x_values[index] + 215, 460), f"({left}, {right})", F_MONO_BOLD, INK)
        if index < len(pairs) - 1:
            arrow(
                draw,
                (x_values[index] + 435, 425),
                (x_values[index + 1] - 15, 425),
                color=GREEN,
                label="(b, a % b)",
            )

    headers = ["Division", "Quotient", "Remainder"]
    x_positions = [300, 1250, 1630, 2100]
    table_y = 675
    row_h = 112
    for column, header in enumerate(headers):
        draw.rectangle((x_positions[column], table_y, x_positions[column + 1], table_y + row_h), fill=NAVY, outline=WHITE, width=2)
        center_text(draw, ((x_positions[column] + x_positions[column + 1]) / 2, table_y + row_h / 2), header, F_BOX_TITLE, WHITE)
    table_rows = [
        ("252 = 105 x 2 + 42", "2", "42"),
        ("105 = 42 x 2 + 21", "2", "21"),
        ("42 = 21 x 2 + 0", "2", "0"),
    ]
    for row_index, row in enumerate(table_rows):
        y1 = table_y + (row_index + 1) * row_h
        y2 = y1 + row_h
        fill = WHITE if row_index % 2 == 0 else PALE_GRAY
        for column, value in enumerate(row):
            draw.rectangle((x_positions[column], y1, x_positions[column + 1], y2), fill=fill, outline=LINE, width=2)
            center_text(draw, ((x_positions[column] + x_positions[column + 1]) / 2, (y1 + y2) / 2), value, F_MONO if column == 0 else F_MONO_BOLD, INK)

    rounded_box(draw, (210, 1150, 1010, 1315), fill=PALE_GREEN, outline=GREEN, width=5)
    center_text(draw, (610, 1200), "Invariant", F_BOX_TITLE, GREEN)
    center_text(draw, (610, 1260), "common divisors do not change", F_BODY, INK)
    rounded_box(draw, (1140, 1150, 2190, 1315), fill=PALE_GOLD, outline=GOLD, width=5)
    center_text(draw, (1665, 1200), "Stop when b = 0", F_BOX_TITLE, GOLD)
    center_text(draw, (1665, 1260), "gcd(252, 105) = 21", F_MONO_BOLD, NAVY)
    return image


def modular_clock() -> Image.Image:
    image, draw = new_canvas(
        "Modulo as a clock",
        "Normalized modulo maps every integer to one position in [0, m)",
        "Figure 08",
    )
    cx, cy, radius = 770, 800, 430
    draw.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), fill=PALE_BLUE, outline=NAVY, width=9)
    draw.ellipse((cx - 55, cy - 55, cx + 55, cy + 55), fill=NAVY)
    center_text(draw, (cx, cy), "mod 12", F_SMALL_BOLD, WHITE)

    for value in range(12):
        angle = math.radians(value * 30 - 90)
        x = cx + radius * 0.82 * math.cos(angle)
        y = cy + radius * 0.82 * math.sin(angle)
        point_fill = GOLD if value in (5, 9) else WHITE
        point_outline = GOLD if value in (5, 9) else BLUE
        draw.ellipse((x - 48, y - 48, x + 48, y + 48), fill=point_fill, outline=point_outline, width=4)
        center_text(draw, (x, y), str(value), F_SMALL_BOLD, NAVY if value in (5, 9) else INK)

    def clock_point(value: int, radial: float = 0.66) -> tuple[float, float]:
        angle = math.radians(value * 30 - 90)
        return cx + radius * radial * math.cos(angle), cy + radius * radial * math.sin(angle)

    arrow(draw, clock_point(0), clock_point(9), color=GOLD, width=8, label="-3")
    arrow(draw, clock_point(0, 0.48), clock_point(5, 0.48), color=GREEN, width=8, label="+17")

    titled_box(
        draw,
        (1350, 300, 2255, 595),
        "Java remainder",
        "-3 % 12 = -3\nThe sign follows the dividend.",
        fill=PALE_RED,
        outline=RED,
    )
    titled_box(
        draw,
        (1350, 665, 2255, 960),
        "Normalized modulo",
        "Math.floorMod(-3, 12) = 9\nThe result is in [0, 12).",
        fill=PALE_GREEN,
        outline=GREEN,
    )
    rounded_box(draw, (1350, 1040, 2255, 1265), fill=PALE_GOLD, outline=GOLD, width=5)
    center_text(draw, (1802, 1095), "Same positions", F_BOX_TITLE, GOLD)
    center_text(draw, (1802, 1160), "17 mod 12 = 5", F_MONO_BOLD, INK)
    center_text(draw, (1802, 1215), "(9 + 8) mod 12 = 5", F_MONO_BOLD, INK)
    return image


def powers_of_two_scale() -> Image.Image:
    image, draw = new_canvas(
        "Powers of two for DSA reasoning",
        "Repeated doubling builds powers; repeated halving explains logarithms",
        "Figure 09",
    )
    center_text(draw, (W / 2, 265), "Doubling ladder", F_SECTION, NAVY)
    powers = [(exponent, 2**exponent) for exponent in range(9)]
    start_x = 95
    card_w = 225
    gap = 30
    for index, (exponent, value) in enumerate(powers):
        x1 = start_x + index * (card_w + gap)
        x2 = x1 + card_w
        fill = PALE_GOLD if exponent in (0, 5, 8) else PALE_BLUE
        outline = GOLD if exponent in (0, 5, 8) else BLUE
        rounded_box(draw, (x1, 340, x2, 535), fill=fill, outline=outline, width=4)
        center_text(draw, ((x1 + x2) / 2, 390), f"2^{exponent}", F_MONO_BOLD, outline)
        center_text(draw, ((x1 + x2) / 2, 475), str(value), F_BODY_BOLD, INK)
        if index < len(powers) - 1:
            arrow(draw, (x2 + 2, 438), (x2 + gap - 2, 438), color=GREEN, width=5, head=16)
    center_text(draw, (W / 2, 590), "Each move right multiplies by 2", F_BODY_BOLD, GREEN)

    center_text(draw, (W / 2, 690), "Interview anchors", F_SECTION, NAVY)
    anchors = [
        ("2^10", "1,024", "about 1 thousand"),
        ("2^20", "1,048,576", "about 1 million"),
        ("2^30", "1,073,741,824", "about 1 billion"),
        ("2^31", "2,147,483,648", "int positive boundary + 1"),
        ("2^32", "4,294,967,296", "all 32-bit patterns"),
        ("2^63", "9,223,372,036,854,775,808", "long magnitude boundary"),
    ]
    card_width = 690
    card_height = 190
    anchor_positions = [(100, 755), (855, 755), (1610, 755), (100, 985), (855, 985), (1610, 985)]
    outlines = [BLUE, CYAN, GREEN, GOLD, RED, NAVY]
    fills = [PALE_BLUE, PALE_CYAN, PALE_GREEN, PALE_GOLD, PALE_RED, PALE_GRAY]
    for index, ((power, exact, note), (x, y)) in enumerate(zip(anchors, anchor_positions)):
        rounded_box(draw, (x, y, x + card_width, y + card_height), fill=fills[index], outline=outlines[index], width=5)
        center_text(draw, (x + 130, y + 62), power, F_MONO_BOLD, outlines[index])
        center_text(draw, (x + 420, y + 62), exact, F_SMALL_BOLD, INK)
        center_text(draw, (x + card_width / 2, y + 135), note, F_SMALL, MUTED)

    rounded_box(draw, (330, 1230, W - 330, 1340), fill=PALE_GOLD, outline=GOLD, width=4)
    center_text(draw, (W / 2, 1285), "For n = 2^k, exactly k halvings reduce n to 1", F_BODY_BOLD, NAVY)
    return image


def base_conversion_map() -> Image.Image:
    image, draw = new_canvas(
        "Base conversion map",
        "Two reusable directions connect text representations and integer values",
        "Figure 10",
    )
    center_x, center_y = 1200, 650
    rounded_box(draw, (895, 500, 1505, 800), fill=PALE_GOLD, outline=GOLD, width=7)
    center_text(draw, (center_x, 585), "Integer value", F_SECTION, NAVY)
    center_text(draw, (center_x, 665), "fixed width or BigInteger", F_BODY, INK)
    center_text(draw, (center_x, 730), "example: 45", F_MONO_BOLD, GOLD)

    bases = [
        ("Binary", "base 2", "101101", (120, 280), BLUE, PALE_BLUE),
        ("Octal", "base 8", "55", (120, 850), CYAN, PALE_CYAN),
        ("Hexadecimal", "base 16", "2D", (1710, 280), GREEN, PALE_GREEN),
        ("Generic", "base 2 through 36", "digits 0-9, a-z", (1710, 850), RED, PALE_RED),
    ]
    boxes: list[tuple[int, int, int, int]] = []
    for title, base, example, (x, y), outline, fill in bases:
        bounds = (x, y, x + 570, y + 290)
        boxes.append(bounds)
        rounded_box(draw, bounds, fill=fill, outline=outline, width=5)
        center_text(draw, (x + 285, y + 62), title, F_SECTION, outline)
        center_text(draw, (x + 285, y + 130), base, F_BODY, INK)
        center_text(draw, (x + 285, y + 215), example, F_MONO_BOLD, NAVY)

    arrow(draw, (690, 430), (890, 575), color=BLUE, label="parse")
    arrow(draw, (895, 715), (690, 980), color=CYAN, label="format")
    arrow(draw, (1710, 430), (1510, 575), color=GREEN, label="parse")
    arrow(draw, (1505, 715), (1710, 980), color=RED, label="format")

    rounded_box(draw, (785, 250, 1615, 395), fill=PALE_BLUE, outline=BLUE, width=4)
    center_text(draw, (1200, 300), "Text -> value", F_BOX_TITLE, BLUE)
    center_text(draw, (1200, 350), "value = value x base + digit", F_MONO, INK)

    rounded_box(draw, (785, 880, 1615, 1025), fill=PALE_GREEN, outline=GREEN, width=4)
    center_text(draw, (1200, 930), "Value -> text", F_BOX_TITLE, GREEN)
    center_text(draw, (1200, 980), "repeated division; reverse remainders", F_MONO, INK)

    rounded_box(draw, (190, 1210, W - 190, 1340), fill=PALE_GRAY, outline=NAVY, width=4)
    center_text(draw, (W / 2, 1250), "Java toolkit", F_BOX_TITLE, NAVY)
    center_text(
        draw,
        (W / 2, 1300),
        "parseInt(text, base) | toString(value, base) | Character.digit | Character.forDigit | BigInteger",
        F_SMALL_BOLD,
        INK,
    )
    return image


def topic_dependency_map() -> Image.Image:
    image, draw = new_canvas(
        "Number Systems learning route",
        "Learn each prerequisite before the interview pattern that consumes it",
        "Figure 11",
    )
    columns = [
        ("1. Representation", "numbers and digits\nplace value\nJava numeric types", BLUE, PALE_BLUE),
        ("2. Safe operations", "digit loops\nbases and parsing\noverflow and precision", CYAN, PALE_CYAN),
        ("3. Number tools", "divisibility and factors\nprimes, GCD, and LCM\nmodulo, powers, roots", GREEN, PALE_GREEN),
        ("4. Interview use", "large numeric strings\npattern recognition\nSDE-2 follow-ups", GOLD, PALE_GOLD),
    ]
    x_positions = [90, 675, 1260, 1845]
    for index, ((title, body, outline, fill), x) in enumerate(zip(columns, x_positions)):
        titled_box(draw, (x, 360, x + 465, 945), title, body, fill=fill, outline=outline)
        if index < len(columns) - 1:
            arrow(draw, (x + 475, 650), (x + 565, 650), color=GREEN, width=7, label="then")
    rounded_box(draw, (230, 1080, W - 230, 1285), fill=PALE_GRAY, outline=NAVY, width=5)
    center_text(draw, (W / 2, 1135), "Scope boundary", F_BOX_TITLE, NAVY)
    center_text(
        draw,
        (W / 2, 1210),
        "This volume stops at prerequisites; full bit tricks, binary-search patterns, arrays, strings, and hashing live in their own books.",
        F_SMALL_BOLD,
        INK,
    )
    return image


def digit_extraction() -> Image.Image:
    image, draw = new_canvas(
        "Digit extraction loop",
        "Modulo reads the last digit; integer division removes it",
        "Figure 12",
    )
    rows = [(5382, 2, 538), (538, 8, 53), (53, 3, 5), (5, 5, 0)]
    headers = ["remaining", "remaining % 10", "remaining / 10"]
    x_bounds = [(150, 760), (760, 1490), (1490, 2250)]
    for header, (x1, x2) in zip(headers, x_bounds):
        rounded_box(draw, (x1, 285, x2, 390), fill=NAVY, outline=NAVY, width=3)
        center_text(draw, ((x1 + x2) / 2, 337), header, F_MONO_BOLD, WHITE)
    y = 430
    for step, (remaining, digit, next_value) in enumerate(rows, start=1):
        fill = PALE_BLUE if step % 2 else PALE_GRAY
        values = [str(remaining), str(digit), str(next_value)]
        for value, (x1, x2) in zip(values, x_bounds):
            rounded_box(draw, (x1, y, x2, y + 150), fill=fill, outline=LINE, width=3, radius=12)
            center_text(draw, ((x1 + x2) / 2, y + 75), value, F_MONO_BOLD, INK)
        y += 175
    rounded_box(draw, (250, 1190, W - 250, 1330), fill=PALE_GOLD, outline=GOLD, width=5)
    center_text(draw, (W / 2, 1240), "Observed digits: 2, 8, 3, 5", F_BOX_TITLE, GOLD)
    center_text(draw, (W / 2, 1290), "The loop reads from right to left and stops when remaining becomes zero.", F_BODY_BOLD, NAVY)
    return image


def twos_complement_intuition() -> Image.Image:
    image, draw = new_canvas(
        "Two's complement intuition",
        "Negate a fixed-width bit pattern by inverting every bit and adding one",
        "Figure 13",
    )
    steps = [
        ("+5", "00000101", "positive 8-bit pattern", BLUE, PALE_BLUE),
        ("invert", "11111010", "flip each 0 and 1", CYAN, PALE_CYAN),
        ("add 1", "11111011", "fixed-width result represents -5", GREEN, PALE_GREEN),
    ]
    y = 300
    for index, (label, bits, note, outline, fill) in enumerate(steps):
        rounded_box(draw, (260, y, 2140, y + 220), fill=fill, outline=outline, width=5)
        center_text(draw, (520, y + 110), label, F_BOX_TITLE, outline)
        center_text(draw, (1220, y + 82), bits, F_DIGIT, NAVY)
        center_text(draw, (1220, y + 165), note, F_SMALL_BOLD, MUTED)
        if index < len(steps) - 1:
            arrow(draw, (1200, y + 225), (1200, y + 290), color=GOLD, width=7)
        y += 315
    rounded_box(draw, (180, 1230, W - 180, 1350), fill=PALE_RED, outline=RED, width=5)
    center_text(draw, (W / 2, 1290), "Asymmetry: 8-bit range is -128 through 127, so abs(MIN_VALUE) cannot fit.", F_BODY_BOLD, RED)
    return image


def factor_pairs() -> Image.Image:
    image, draw = new_canvas(
        "Factor pairs stop at the square root",
        "Each divisor below sqrt(n) contributes a partner above sqrt(n)",
        "Figure 14",
    )
    pairs = [(1, 36), (2, 18), (3, 12), (4, 9), (6, 6)]
    center_text(draw, (W / 2, 275), "Positive factor pairs of 36", F_SECTION, NAVY)
    y = 355
    for left, right in pairs:
        rounded_box(draw, (360, y, 910, y + 130), fill=PALE_BLUE, outline=BLUE, width=4)
        rounded_box(draw, (1490, y, 2040, y + 130), fill=PALE_GREEN, outline=GREEN, width=4)
        center_text(draw, (635, y + 65), str(left), F_DIGIT, BLUE)
        center_text(draw, (1765, y + 65), str(right), F_DIGIT, GREEN)
        arrow(draw, (930, y + 65), (1470, y + 65), color=GOLD, label=f"{left} x {right} = 36")
        y += 165
    rounded_box(draw, (180, 1210, W - 180, 1340), fill=PALE_GOLD, outline=GOLD, width=5)
    center_text(draw, (W / 2, 1275), "At 6 x 6, the pair meets: count the square root only once.", F_BODY_BOLD, NAVY)
    return image


def sieve_process() -> Image.Image:
    image, draw = new_canvas(
        "Sieve of Eratosthenes",
        "Keep primes; mark each prime's multiples starting at p x p",
        "Figure 15",
    )
    primes = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29}
    values = list(range(2, 31))
    start_x, start_y = 150, 330
    cell_w, cell_h, gap = 185, 140, 28
    for index, value in enumerate(values):
        row, column = divmod(index, 10)
        x = start_x + column * (cell_w + gap)
        y = start_y + row * (cell_h + 55)
        is_prime = value in primes
        rounded_box(
            draw,
            (x, y, x + cell_w, y + cell_h),
            fill=PALE_GREEN if is_prime else PALE_GRAY,
            outline=GREEN if is_prime else LINE,
            width=4,
            radius=14,
        )
        center_text(draw, (x + cell_w / 2, y + 55), str(value), F_BOX_TITLE, GREEN if is_prime else MUTED)
        center_text(draw, (x + cell_w / 2, y + 105), "prime" if is_prime else "marked", F_SMALL, INK)
    notes = [
        "p = 2: mark 4, 6, 8, ...",
        "p = 3: start at 9; smaller multiples were already marked",
        "Stop after p > 30 / p; every composite then has a marked factor.",
    ]
    y = 990
    for note in notes:
        rounded_box(draw, (250, y, W - 250, y + 105), fill=PALE_BLUE, outline=BLUE, width=3)
        center_text(draw, (W / 2, y + 52), note, F_BODY_BOLD, NAVY)
        y += 125
    return image


def fast_exponentiation_process() -> Image.Image:
    image, draw = new_canvas(
        "Fast exponentiation by squaring",
        "Use the exponent's binary digits to select squared factors",
        "Figure 16",
    )
    center_text(draw, (W / 2, 275), "Compute 3^13; binary 13 is 1101", F_SECTION, NAVY)
    headers = ["remaining exponent", "result", "factor", "action"]
    x = [110, 610, 1010, 1450, 2290]
    for index, header in enumerate(headers):
        rounded_box(draw, (x[index], 340, x[index + 1], 445), fill=NAVY, outline=NAVY, width=3)
        center_text(draw, ((x[index] + x[index + 1]) / 2, 392), header, F_SMALL_BOLD, WHITE)
    rows = [
        (13, 1, 3, "odd: result = 1 x 3"),
        (6, 3, 9, "even: skip multiply"),
        (3, 3, 81, "odd: result = 3 x 81"),
        (1, 243, 6561, "odd: result = 243 x 6561"),
        (0, 1_594_323, 43_046_721, "done"),
    ]
    y = 470
    for row_index, row in enumerate(rows):
        fill = PALE_BLUE if row_index % 2 == 0 else PALE_GRAY
        for index, value in enumerate(row):
            rounded_box(draw, (x[index], y, x[index + 1], y + 130), fill=fill, outline=LINE, width=3, radius=10)
            center_text(draw, ((x[index] + x[index + 1]) / 2, y + 65), str(value), F_MONO if index < 3 else F_SMALL_BOLD, INK)
        y += 145
    rounded_box(draw, (230, 1240, W - 230, 1350), fill=PALE_GOLD, outline=GOLD, width=5)
    center_text(draw, (W / 2, 1295), "Thirteen multiplications become O(log exponent) squaring steps.", F_BODY_BOLD, NAVY)
    return image


def large_string_traversal() -> Image.Image:
    image, draw = new_canvas(
        "Streaming a huge numeric string",
        "Keep a bounded state; never parse the entire value into long",
        "Figure 17",
    )
    digits = "98765432109876543210"
    shown = list(digits[:8]) + ["..."] + list(digits[-4:])
    start_x = 80
    cell_w = 150
    gap = 18
    for index, digit in enumerate(shown):
        x1 = start_x + index * (cell_w + gap)
        rounded_box(draw, (x1, 315, x1 + cell_w, 475), fill=PALE_BLUE, outline=BLUE, width=4)
        center_text(draw, (x1 + cell_w / 2, 395), digit, F_MONO_BOLD, NAVY)
    arrow(draw, (250, 565), (W - 250, 565), color=GREEN, width=8, label="left to right")
    rounded_box(draw, (180, 670, W - 180, 920), fill=PALE_GOLD, outline=GOLD, width=5)
    center_text(draw, (W / 2, 735), "Remainder invariant", F_SECTION, NAVY)
    center_text(draw, (W / 2, 825), "remainder = (remainder x 10 + digit) mod modulus", F_MONO_BOLD, INK)
    center_text(draw, (W / 2, 875), "State always remains in [0, modulus).", F_BODY_BOLD, GOLD)
    titled_box(
        draw,
        (230, 1030, 1080, 1290),
        "Time",
        "O(number of digits)\nEach character is visited once.",
        fill=PALE_GREEN,
        outline=GREEN,
    )
    titled_box(
        draw,
        (1320, 1030, 2170, 1290),
        "Auxiliary state",
        "O(1) for modulo\nO(n) only when building an output string.",
        fill=PALE_CYAN,
        outline=CYAN,
    )
    return image


DIAGRAMS: dict[str, tuple[str, Callable[[], Image.Image]]] = {
    "decimal-place-value": ("01-decimal-place-value.png", decimal_place_value),
    "binary-place-value": ("02-binary-place-value.png", binary_place_value),
    "repeated-division": ("03-decimal-to-binary-repeated-division.png", repeated_division),
    "positional-accumulation": ("04-base-to-decimal-positional-accumulation.png", positional_accumulation),
    "java-primitive-ranges": ("05-java-primitive-ranges.png", java_primitive_ranges),
    "overflow-wraparound": ("06-integer-overflow-wraparound.png", overflow_wraparound),
    "euclidean-gcd": ("07-euclidean-gcd-process.png", euclidean_gcd),
    "modular-clock": ("08-modular-clock.png", modular_clock),
    "powers-of-two": ("09-powers-of-two-scale.png", powers_of_two_scale),
    "base-conversion-map": ("10-base-conversion-map.png", base_conversion_map),
    "topic-dependency-map": ("11-topic-dependency-map.png", topic_dependency_map),
    "digit-extraction": ("12-digit-extraction-loop.png", digit_extraction),
    "twos-complement": ("13-twos-complement-intuition.png", twos_complement_intuition),
    "factor-pairs": ("14-factor-pairs.png", factor_pairs),
    "sieve-process": ("15-sieve-process.png", sieve_process),
    "fast-exponentiation": ("16-fast-exponentiation.png", fast_exponentiation_process),
    "large-string-traversal": ("17-large-numeric-string-traversal.png", large_string_traversal),
}


def make_contact_sheet(paths: Iterable[Path], out_dir: Path) -> Path:
    selected = list(paths)
    thumb_w, thumb_h = 720, 450
    margin = 44
    label_h = 58
    columns = 2
    rows = math.ceil(len(selected) / columns)
    sheet_w = columns * thumb_w + (columns + 1) * margin
    sheet_h = rows * (thumb_h + label_h) + (rows + 1) * margin
    sheet = Image.new("RGB", (sheet_w, sheet_h), "#DCE3EA")
    draw = ImageDraw.Draw(sheet)

    for index, path in enumerate(selected):
        row, column = divmod(index, columns)
        x = margin + column * (thumb_w + margin)
        y = margin + row * (thumb_h + label_h + margin)
        with Image.open(path) as source:
            preview = source.convert("RGB")
            preview.thumbnail((thumb_w, thumb_h), Image.Resampling.LANCZOS)
            px = x + (thumb_w - preview.width) // 2
            py = y + (thumb_h - preview.height) // 2
            sheet.paste(preview, (px, py))
        draw.rectangle((x, y, x + thumb_w, y + thumb_h), outline=NAVY, width=3)
        center_text(draw, (x + thumb_w / 2, y + thumb_h + label_h / 2), path.name, font(24, bold=True), NAVY)

    path = out_dir / "number-systems-contact-sheet.png"
    sheet.save(path, format="PNG", optimize=True, dpi=(160, 160))
    return path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=DEFAULT_OUT,
        help=f"PNG output directory (default: {DEFAULT_OUT})",
    )
    parser.add_argument(
        "--only",
        action="append",
        choices=sorted(DIAGRAMS),
        help="Generate only this named diagram; repeat for multiple diagrams.",
    )
    parser.add_argument(
        "--contact-sheet",
        action="store_true",
        help="Also create a contact sheet for visual QA.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    names = args.only or list(DIAGRAMS)
    generated: list[Path] = []
    for name in names:
        filename, renderer = DIAGRAMS[name]
        generated.append(save(renderer(), args.output_dir, filename))
    if args.contact_sheet:
        generated.append(make_contact_sheet(generated, args.output_dir))
    for path in generated:
        with Image.open(path) as image:
            print(f"{path}: {image.width}x{image.height}")


if __name__ == "__main__":
    main()
