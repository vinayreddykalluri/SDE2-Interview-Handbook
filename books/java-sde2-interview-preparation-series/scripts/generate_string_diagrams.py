#!/usr/bin/env python3
"""Generate instructional diagrams for focused Volume 07."""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "content" / "volumes" / "07-strings-and-string-patterns" / "assets"
W, H = 2400, 1450
NAVY, BLUE, TEAL, GOLD = "#0B2545", "#1F5A94", "#17758A", "#C58A22"
GREEN, RED, INK, MUTED = "#2D7D66", "#A4423E", "#17212B", "#52606D"
LINE, WHITE = "#AAB8C6", "#FFFFFF"
PALE_BLUE, PALE_TEAL, PALE_GOLD = "#EAF2F8", "#EAF6F8", "#FFF6E3"
PALE_GREEN, PALE_RED, PALE_GRAY = "#EAF5F0", "#FBEDEC", "#F3F6F8"


def font(size: int, *, bold: bool = False, mono: bool = False) -> ImageFont.FreeTypeFont:
    names = (
        ["/System/Library/Fonts/Supplemental/Courier New Bold.ttf",
         "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"]
        if mono and bold else
        ["/System/Library/Fonts/Supplemental/Courier New.ttf",
         "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"]
        if mono else
        ["/System/Library/Fonts/Supplemental/Arial Bold.ttf",
         "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"]
        if bold else
        ["/System/Library/Fonts/Supplemental/Arial.ttf",
         "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"]
    )
    for name in names:
        if Path(name).exists():
            return ImageFont.truetype(name, size)
    return ImageFont.load_default(size=size)


TITLE = font(65, bold=True)
SUB = font(33)
SECTION = font(39, bold=True)
BODY = font(32)
BOLD = font(32, bold=True)
SMALL = font(26)
MONO = font(34, bold=True, mono=True)
CELL = font(41, bold=True, mono=True)


def canvas(title: str, subtitle: str, number: str) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", (W, H), WHITE)
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, W, 188), fill=NAVY)
    draw.rectangle((0, 0, 24, 188), fill=GOLD)
    draw.text((78, 44), title, font=TITLE, fill=WHITE)
    draw.text((82, 119), subtitle, font=SUB, fill="#CFE1F2")
    draw.line((70, H - 70, W - 70, H - 70), fill=LINE, width=3)
    draw.text((72, H - 55), "Java SDE-2 DSA Series | Volume 07: Strings", font=SMALL, fill=MUTED)
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
        center(draw, ((start[0] + end[0]) / 2, (start[1] + end[1]) / 2 - 30), label, SMALL, color)


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


def values_references() -> Image.Image:
    image, draw = canvas("String values, references, and immutability",
                         "Reassignment changes a variable; String methods create values", "Figure 01")
    box(draw, (110, 315, 690, 510), PALE_BLUE, BLUE)
    center(draw, (400, 375), "String first", SECTION, NAVY)
    center(draw, (400, 455), "reference A17", MONO, BLUE)
    box(draw, (110, 760, 690, 955), PALE_BLUE, BLUE)
    center(draw, (400, 820), "String alias", SECTION, NAVY)
    center(draw, (400, 900), "reference A17", MONO, BLUE)
    box(draw, (980, 360, 1600, 585), PALE_GREEN, GREEN)
    center(draw, (1290, 420), "String object A17", SECTION, GREEN)
    center(draw, (1290, 510), 'value "java"', MONO)
    arrow(draw, (690, 410), (960, 450), BLUE)
    arrow(draw, (690, 850), (960, 510), BLUE)
    box(draw, (1700, 765, 2260, 990), PALE_GOLD, GOLD)
    center(draw, (1980, 825), "String object B08", SECTION, GOLD)
    center(draw, (1980, 915), 'value "JAVA"', MONO)
    arrow(draw, (690, 410), (1680, 815), GOLD, "first = first.toUpperCase()")
    box(draw, (580, 1100, 1820, 1265), PALE_RED, RED)
    center(draw, (1200, 1150), "A17 was not edited", BOLD, RED)
    center(draw, (1200, 1215), "alias still observes the value java", MONO)
    return image


def unicode_units() -> Image.Image:
    image, draw = canvas("One text value, several counting units",
                         "Choose char, code point, byte, or grapheme before indexing", "Figure 02")
    center(draw, (1200, 285), 'text = "A + U+1F642 + e + combining acute"', SECTION, NAVY)
    labels = [
        ("UTF-16 char units", "A | high | low | e | mark", "length() = 5", BLUE, PALE_BLUE),
        ("Unicode code points", "A | smiling face | e | acute", "count = 4", TEAL, PALE_TEAL),
        ("User-perceived text", "A | smiling face | e-with-accent", "often 3 graphemes", GREEN, PALE_GREEN),
        ("UTF-8 bytes", "1 | 4 | 1 | 2 bytes", "encoded length = 8", GOLD, PALE_GOLD),
    ]
    for index, (name, items, count, color, fill) in enumerate(labels):
        y = 385 + index * 225
        box(draw, (180, y, 2220, y + 165), fill, color)
        center(draw, (500, y + 52), name, BOLD, color)
        center(draw, (1300, y + 55), items, MONO)
        center(draw, (1300, y + 120), count, BODY, color)
    return image


def empty_blank_null() -> Image.Image:
    image, draw = canvas("Null, empty, and blank are different contracts",
                         "Guard absence before invoking String methods", "Figure 03")
    box(draw, (850, 265, 1550, 425), PALE_GOLD, GOLD)
    center(draw, (1200, 345), "What does the reference contain?", SECTION, NAVY)
    nodes = [
        ((120, 655, 690, 930), "null", "no String object", "check reference first", RED, PALE_RED),
        ((915, 655, 1485, 930), '""', "length is zero", "isEmpty() is true", BLUE, PALE_BLUE),
        ((1710, 655, 2280, 930), '"  \\t"', "only whitespace", "isBlank() is true", GREEN, PALE_GREEN),
    ]
    for bounds, title, line1, line2, color, fill in nodes:
        box(draw, bounds, fill, color)
        center(draw, ((bounds[0] + bounds[2]) / 2, 720), title, MONO, color)
        center(draw, ((bounds[0] + bounds[2]) / 2, 805), line1, BODY)
        center(draw, ((bounds[0] + bounds[2]) / 2, 870), line2, BOLD, color)
        arrow(draw, (1200, 425), ((bounds[0] + bounds[2]) // 2, 640), color)
    box(draw, (420, 1080, 1980, 1250), PALE_GRAY, LINE)
    center(draw, (1200, 1135), "Never call text.equals(...) when text may be null", BOLD, RED)
    center(draw, (1200, 1205), 'Use Objects.equals(a, b) or a defined non-null contract', BODY, NAVY)
    return image


def builder_pipeline() -> Image.Image:
    image, draw = canvas("StringBuilder owns a mutable construction buffer",
                         "Append into one builder, publish one immutable String", "Figure 04")
    stages = [
        ((100, 390, 610, 650), "start", '[ ]', PALE_GRAY, LINE),
        ((720, 390, 1230, 650), "append Java", '[J a v a]', PALE_BLUE, BLUE),
        ((1340, 390, 1850, 650), "append 21", '[J a v a 2 1]', PALE_TEAL, TEAL),
        ((1890, 840, 2300, 1100), "toString", '"Java21"', PALE_GREEN, GREEN),
    ]
    for bounds, title, data, fill, color in stages:
        box(draw, bounds, fill, color)
        center(draw, ((bounds[0] + bounds[2]) / 2, bounds[1] + 62), title, BOLD, color)
        center(draw, ((bounds[0] + bounds[2]) / 2, bounds[1] + 165), data, MONO)
    arrow(draw, (610, 520), (700, 520), BLUE)
    arrow(draw, (1230, 520), (1320, 520), TEAL)
    arrow(draw, (1850, 570), (2050, 820), GREEN)
    box(draw, (240, 865, 1650, 1160), PALE_GOLD, GOLD)
    center(draw, (945, 930), "Capacity is not length", SECTION, NAVY)
    center(draw, (945, 1015), "length = characters currently stored", BOLD)
    center(draw, (945, 1085), "capacity = available buffer before growth", BOLD)
    center(draw, (1200, 1260), "Do not retain and mutate a builder after ownership was transferred", BODY, RED)
    return image


def palindrome() -> Image.Image:
    image, draw = canvas("Opposing pointers for a palindrome contract",
                         "Normalize only what the problem statement permits", "Figure 05")
    row(draw, ["r", "a", "c", "e", "c", "a", "r"], 290, 365, 260,
        [PALE_GOLD, WHITE, WHITE, PALE_GREEN, WHITE, WHITE, PALE_RED])
    arrow(draw, (420, 750), (420, 540), GOLD, "left")
    arrow(draw, (1980, 750), (1980, 540), RED, "right")
    box(draw, (510, 815, 1890, 1030), PALE_GREEN, GREEN)
    center(draw, (1200, 875), "Compare, then move inward", SECTION, GREEN)
    center(draw, (1200, 955), "Invariant: every position outside [left, right] matches", BOLD)
    box(draw, (500, 1110, 1900, 1265), PALE_RED, RED)
    center(draw, (1200, 1160), "Contract questions", BOLD, RED)
    center(draw, (1200, 1220), "case? punctuation? Unicode code points? normalization?", MONO)
    return image


def frequency_state() -> Image.Image:
    image, draw = canvas("Frequency state turns order into counts",
                         "The alphabet contract determines array, map, and normalization choices", "Figure 06")
    center(draw, (1200, 285), 'word = "cacao"', SECTION, NAVY)
    row(draw, ["c", "a", "c", "a", "o"], 550, 355, 260,
        [PALE_BLUE, PALE_GOLD, PALE_BLUE, PALE_GOLD, PALE_GREEN])
    box(draw, (340, 700, 2060, 980), PALE_GRAY, LINE)
    entries = [("a", "2", GOLD), ("c", "2", BLUE), ("o", "1", GREEN)]
    for index, (key, value, color) in enumerate(entries):
        x = 620 + index * 580
        box(draw, (x, 770, x + 340, 900), WHITE, color)
        center(draw, (x + 85, 835), key, CELL, color)
        center(draw, (x + 250, 835), value, CELL)
    box(draw, (380, 1080, 2020, 1250), PALE_TEAL, TEAL)
    center(draw, (1200, 1130), "Anagram invariant", SECTION, TEAL)
    center(draw, (1200, 1200), "same chosen units + same multiplicities + same normalization", BOLD)
    return image


def sliding_window() -> Image.Image:
    image, draw = canvas("Variable window for the longest unique substring",
                         "Jump left past the previous occurrence; never move left backward", "Figure 07")
    row(draw, ["a", "b", "b", "a", "c"], 550, 330, 260,
        [PALE_RED, PALE_RED, PALE_GOLD, PALE_GREEN, PALE_GREEN])
    arrow(draw, (1200, 725), (1200, 520), GOLD, "repeat b at right = 2")
    box(draw, (330, 790, 2070, 1010), PALE_BLUE, BLUE)
    center(draw, (1200, 850), "last[b] = 1, so left = max(left, 1 + 1) = 2", MONO, BLUE)
    center(draw, (1200, 940), "Window [left, right] is duplicate-free after repair", BOLD, GREEN)
    box(draw, (460, 1090, 1940, 1260), PALE_GOLD, GOLD)
    center(draw, (1200, 1145), "Aggregate proof", SECTION, NAVY)
    center(draw, (1200, 1210), "right advances n times; left advances at most n times -> O(n)", BOLD)
    return image


def minimum_cover() -> Image.Image:
    image, draw = canvas("Minimum covering window: expand, satisfy, shrink",
                         "A satisfied-types count makes the invariant test constant-time", "Figure 08")
    stages = [
        ((90, 330, 700, 590), "1. EXPAND", "add right unit\nuntil valid", BLUE, PALE_BLUE),
        ((895, 330, 1505, 590), "2. RECORD", "current window\nis a candidate", GREEN, PALE_GREEN),
        ((1700, 330, 2310, 590), "3. SHRINK", "remove left unit\nwhile still valid", GOLD, PALE_GOLD),
    ]
    for bounds, title, lines, color, fill in stages:
        box(draw, bounds, fill, color)
        center(draw, ((bounds[0] + bounds[2]) / 2, 395), title, SECTION, color)
        for offset, line in enumerate(lines.split("\n")):
            center(draw, ((bounds[0] + bounds[2]) / 2, 480 + offset * 55), line, BOLD)
    arrow(draw, (700, 460), (875, 460), BLUE)
    arrow(draw, (1505, 460), (1680, 460), GREEN)
    arrow(draw, (2020, 610), (380, 740), GOLD, "invalid -> expand again")
    box(draw, (260, 790, 2140, 1030), PALE_TEAL, TEAL)
    center(draw, (1200, 850), "Validity", SECTION, NAVY)
    center(draw, (1200, 925), "formed == required distinct units", MONO, TEAL)
    center(draw, (1200, 990), "surplus copies do not increase formed", BOLD)
    box(draw, (480, 1110, 1920, 1265), PALE_RED, RED)
    center(draw, (1200, 1160), "Failure boundary", BOLD, RED)
    center(draw, (1200, 1220), "do not mix UTF-16 indexes with code-point positions", MONO)
    return image


def kmp_fallback() -> Image.Image:
    image, draw = canvas("KMP reuses a proven prefix after mismatch",
                         "The prefix table stores the longest proper prefix that is also a suffix", "Figure 09")
    center(draw, (1200, 285), 'pattern = "ababaca"', SECTION, NAVY)
    row(draw, ["a", "b", "a", "b", "a", "c", "a"], 290, 350, 260,
        [PALE_BLUE, PALE_GOLD, PALE_BLUE, PALE_GOLD, PALE_BLUE, PALE_RED, PALE_GREEN])
    center(draw, (180, 700), "lps", SECTION, NAVY)
    row(draw, ["0", "0", "1", "2", "3", "0", "1"], 290, 640, 260,
        [PALE_GRAY] * 7)
    box(draw, (350, 930, 2050, 1170), PALE_GOLD, GOLD)
    center(draw, (1200, 990), "Mismatch after matching ababa", SECTION, RED)
    center(draw, (1200, 1065), "matched = lps[matched - 1]", MONO, BLUE)
    center(draw, (1200, 1125), "Keep the overlap aba; do not restart from zero", BOLD, GREEN)
    center(draw, (1200, 1255), "Each text position advances once; fallbacks do not rewind the text", BODY, NAVY)
    return image


def decision_map() -> Image.Image:
    image, draw = canvas("String pattern decision map",
                         "Choose the text unit and contract before choosing the algorithm", "Figure 10")
    box(draw, (790, 245, 1610, 420), PALE_GOLD, GOLD)
    center(draw, (1200, 330), "What must the result describe?", SECTION, NAVY)
    nodes = [
        ((80, 600, 530, 875), "ends / mirror", "two pointers", BLUE),
        ((670, 600, 1120, 875), "contiguous range", "sliding window", TEAL),
        ((1280, 600, 1730, 875), "multiplicity", "frequency state", GREEN),
        ((1870, 600, 2320, 875), "pattern search", "naive / KMP", RED),
    ]
    for bounds, signal, answer, color in nodes:
        box(draw, bounds, WHITE, color)
        center(draw, ((bounds[0] + bounds[2]) / 2, 675), signal, BOLD, color)
        center(draw, ((bounds[0] + bounds[2]) / 2, 795), answer, SECTION, NAVY)
        arrow(draw, (1200, 420), ((bounds[0] + bounds[2]) // 2, 580), color)
    box(draw, (330, 1040, 2070, 1260), PALE_BLUE, BLUE)
    center(draw, (1200, 1100), "Before coding", SECTION, NAVY)
    center(draw, (1200, 1170), "null + units + case + normalization + output indexes + mutation", BOLD, BLUE)
    center(draw, (1200, 1225), "Then prove progress, complexity, and edge behavior", BODY, GREEN)
    return image


def main() -> None:
    figures = [
        (values_references(), "01-string-values-references-immutability.png"),
        (unicode_units(), "02-unicode-units-and-boundaries.png"),
        (empty_blank_null(), "03-null-empty-blank-contract.png"),
        (builder_pipeline(), "04-string-builder-pipeline.png"),
        (palindrome(), "05-palindrome-two-pointers.png"),
        (frequency_state(), "06-frequency-and-anagram-state.png"),
        (sliding_window(), "07-longest-unique-window.png"),
        (minimum_cover(), "08-minimum-cover-window.png"),
        (kmp_fallback(), "09-kmp-prefix-fallback.png"),
        (decision_map(), "10-string-pattern-decision-map.png"),
    ]
    for image, filename in figures:
        save(image, filename)
        print(f"generated {OUT / filename}")


if __name__ == "__main__":
    main()
