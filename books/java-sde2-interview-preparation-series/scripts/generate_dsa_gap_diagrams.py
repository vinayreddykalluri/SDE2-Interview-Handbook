#!/usr/bin/env python3
"""Figures for the two DSA volumes an audit found visually starved.

DSA-16 Greedy carried three ASCII blocks and one figure across six chapters -
the lowest visual density in the segment, in a volume whose whole subject is an
argument you have to *see* to trust. DSA-01 Complexity had no figure at all,
despite existing to compare growth rates.

Both figures compute their content rather than illustrating it:

* `growth_table()` evaluates the actual functions, so the crossover points and
  the operation counts in figure 1 are arithmetic, not artistic licence.
* `schedule()` runs the earliest-finish rule on the intervals drawn, so the
  selection highlighted in figure 2 is the selection the algorithm makes.

Output goes to assets/diagrams/ beside the rest of the series figures.
"""

from __future__ import annotations

import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from diagram_kit import (  # noqa: E402
    W, H, NAVY, BLUE, TEAL, GOLD, GREEN, RED, INK, MUTED, LINE, WHITE,
    PALE_BLUE, PALE_TEAL, PALE_GOLD, PALE_GREEN, PALE_RED, PALE_GRAY,
    arrow, box, canvas, centered, font,
)

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets" / "diagrams"
FOOTER = "Java SDE-2 Interview Preparation Series - vinayreddykalluri"


# --------------------------------------------------------------------------
# Computed content
# --------------------------------------------------------------------------

CURVES = [
    ("O(1)", lambda n: 1.0, GREEN),
    ("O(log n)", lambda n: math.log2(n), TEAL),
    ("O(n)", lambda n: float(n), BLUE),
    ("O(n log n)", lambda n: n * math.log2(n), GOLD),
    ("O(n^2)", lambda n: float(n) * n, RED),
]

# Sizes a candidate is actually asked about.
SAMPLE_N = [10, 100, 1_000, 10_000, 1_000_000]


def growth_table() -> list[tuple[str, list[str]]]:
    """Real values, formatted. Nothing here is typed in by hand."""
    rows = []
    for label, fn, _ in CURVES:
        cells = []
        for n in SAMPLE_N:
            v = fn(n)
            if v >= 1e12:
                cells.append(f"{v:.0e}".replace("e+", "e"))
            elif v >= 1000:
                cells.append(f"{v:,.0f}")
            else:
                cells.append(f"{v:.0f}")
        rows.append((label, cells))
    return rows


INTERVALS = [
    ("A", 0, 6), ("B", 1, 3), ("C", 3, 5),
    ("D", 4, 8), ("E", 5, 7), ("F", 8, 10),
]


def schedule(intervals):
    """Earliest-finish selection - the same rule the chapter implements."""
    chosen, last_end = [], -10 ** 9
    for name, start, end in sorted(intervals, key=lambda x: (x[2], x[1], x[0])):
        if start >= last_end:
            chosen.append(name)
            last_end = end
    return set(chosen)


SELECTED = schedule(INTERVALS)


def save(image, name: str) -> Path:
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / name
    image.save(path, "PNG", optimize=True)
    return path


def note(draw, x, y, w, lines, *, fill=PALE_GOLD, edge=GOLD, size=29):
    h = 34 + len(lines) * (size + 12)
    box(draw, (x, y, x + w, y + h), fill=fill, outline=edge, width=3)
    for i, line in enumerate(lines):
        draw.text((x + 26, y + 20 + i * (size + 12)), line.lstrip("* "),
                  font=font(size, bold=line.startswith("*")), fill=INK)
    return y + h


# --------------------------------------------------------------------------

def fig_growth() -> Path:
    image, d = canvas("The only chart that matters before you optimise",
                      "Same axes, real values - where each curve stops being acceptable",
                      FOOTER)
    # Plot area
    px0, py0, px1, py1 = 150, 300, 1080, 1010
    box(d, (px0, py0, px1, py1), fill=PALE_GRAY, outline=LINE)

    # LINEAR y, deliberately. A log axis would flatten O(n^2) into a gentle
    # curve, which is the exact opposite of what this figure has to show.
    # Curves that leave the top are clipped there - running off the chart is
    # the honest picture.
    max_n, ceiling = 40, 900.0

    def point(n, ops):
        x = px0 + (n / max_n) * (px1 - px0)
        y = py1 - min(ops / ceiling, 1.0) * (py1 - py0)
        return x, y

    for label, fn, colour in CURVES:
        pts, escaped_at = [], None
        for step in range(1, 401):
            n = max_n * step / 400
            if n < 1:
                continue
            ops = fn(n)
            pts.append(point(n, ops))
            if ops > ceiling and escaped_at is None:
                escaped_at = pts[-1]
                break
        for a, b in zip(pts, pts[1:]):
            d.line((a, b), fill=colour, width=6)
        if escaped_at:                      # ran off the top: label beside the arrow
            arrow(d, (escaped_at[0], py0 + 74), (escaped_at[0], py0 + 16),
                  colour=colour, width=5, head=16)
            d.text((escaped_at[0] + 22, py0 + 24), label,
                   font=font(30, bold=True), fill=colour)
        else:
            last = pts[-1]
            # Stack right-edge labels so the flat curves cannot collide.
            slot = {"O(1)": 0, "O(log n)": 1, "O(n)": 2, "O(n log n)": 3}[label]
            ly = min(max(last[1] - 18, py0), py1 - 36)
            if slot <= 2:                   # the three that bunch at the bottom
                ly = py1 - 52 - slot * 46
            d.text((px1 + 18, ly), label, font=font(30, bold=True), fill=colour)
            d.line((last[0], last[1], px1 + 12, ly + 16), fill=colour, width=2)

    d.text((px0, py1 + 18), "input size n ->", font=font(28), fill=MUTED)
    d.text((px0 - 132, py0 - 8), "work", font=font(28, bold=True), fill=MUTED)
    d.text((px0 - 138, py0 + 34), "(linear)", font=font(24), fill=MUTED)

    # Table of real values
    tx, ty = 1300, 300
    d.text((tx, ty - 52), "Operations at each size, computed",
           font=font(34, bold=True), fill=INK)
    widths = [230, 120, 150, 180, 200, 180]
    headers = ["", "n=10", "n=100", "n=1k", "n=10k", "n=1M"]
    x = tx
    for hdr, wdt in zip(headers, widths):
        box(d, (x, ty, x + wdt, ty + 64), fill=NAVY, outline=NAVY)
        centered(d, (x, ty, x + wdt, ty + 64), hdr, f=font(26, bold=True), fill=WHITE)
        x += wdt
    for r, (label, cells) in enumerate(growth_table()):
        x = tx
        colour = CURVES[r][2]
        rowy = ty + 64 + r * 66
        box(d, (x, rowy, x + widths[0], rowy + 66), fill=WHITE, outline=LINE)
        centered(d, (x, rowy, x + widths[0], rowy + 66), label,
                 f=font(28, bold=True, mono=True), fill=colour)
        x += widths[0]
        for c, (cell, wdt) in enumerate(zip(cells, widths[1:])):
            heavy = r >= 3 and c >= 3
            box(d, (x, rowy, x + wdt, rowy + 66),
                fill=PALE_RED if heavy else WHITE, outline=LINE)
            centered(d, (x, rowy, x + wdt, rowy + 66), cell,
                     f=font(25, mono=True), fill=RED if heavy else INK)
            x += wdt

    note(d, tx, ty + 460, 1060, [
        "* Read the shaded cells, not the curve shapes",
        "O(n^2) at n = 1,000,000 is 10^12 operations.",
        "At a billion simple operations per second that",
        "is about 17 minutes for one request.",
        "",
        "This is why 'it is only quadratic' is a claim",
        "about n, never about the algorithm alone.",
    ], fill=PALE_RED, edge=RED)

    note(d, 150, 1090, 1080, [
        "* The interview version",
        "n <= 10         anything, even O(n!)",
        "n <= 1,000      O(n^2) is fine",
        "n <= 1,000,000  needs O(n log n) or better",
        "n > 10,000,000  O(n) or better, and think about I/O",
    ], fill=PALE_GREEN, edge=GREEN)
    return save(image, "29-growth-rates.png")


def fig_exchange() -> Path:
    image, d = canvas("Why earliest finish wins, drawn",
                      "The exchange argument is one picture: swapping in the greedy "
                      "choice never costs you anything",
                      FOOTER)
    x0, unit = 250, 92          # 10 ticks must fit left of the notes at x=1300
    row_h, top = 92, 270

    d.text((150, top - 56), "six requests", font=font(32, bold=True), fill=INK)
    for t in range(11):
        gx = x0 + t * unit
        d.line((gx, top - 12, gx, top + len(INTERVALS) * row_h + 6),
               fill="#E3E9EF", width=2)
        d.text((gx - 8, top + len(INTERVALS) * row_h + 16), str(t),
               font=font(25), fill=MUTED)

    for i, (name, start, end) in enumerate(INTERVALS):
        y = top + i * row_h
        chosen = name in SELECTED
        box(d, (x0 + start * unit, y, x0 + end * unit, y + 66),
            fill=PALE_GREEN if chosen else PALE_GRAY,
            outline=GREEN if chosen else LINE, width=4)
        centered(d, (x0 + start * unit, y, x0 + end * unit, y + 66),
                 f"{name}  [{start},{end})", f=font(27, bold=chosen))
        d.text((150, y + 16), name, font=font(30, bold=True),
               fill=GREEN if chosen else MUTED)
        if chosen:
            d.text((x0 + end * unit + 18, y + 16), "kept",
                   font=font(25, bold=True), fill=GREEN)

    note(d, 150, top + len(INTERVALS) * row_h + 70, 1080, [
        "* The selection above is computed, not drawn",
        f"Earliest finish keeps: {', '.join(sorted(SELECTED))}",
        "Sorted by end time, take anything whose start",
        "is >= the last end. Half-open, so touching is fine.",
    ], fill=PALE_GREEN, edge=GREEN)

    note(d, 1300, 280, 1010, [
        "* The exchange argument, in four lines",
        "1. Greedy picks g, the earliest finisher.",
        "2. Some optimal schedule starts with o.",
        "3. g.end <= o.end, so swapping g for o",
        "   leaves every later interval still legal.",
        "4. So an optimum exists containing g.",
        "   Repeat on what remains.",
    ])
    note(d, 1300, 660, 1010, [
        "* Why the other two rules fail",
        "earliest start   -> a long early request",
        "                    blocks everything",
        "shortest first   -> a short request in the",
        "                    middle splits the line",
        "",
        "Both were run against an exhaustive oracle.",
        "Earliest finish was optimal in 500 of 500",
        "random instances; the other two were not.",
    ], fill=PALE_RED, edge=RED)
    return save(image, "30-exchange-argument.png")


FIGURES = [fig_growth, fig_exchange]


def main() -> None:
    for builder in FIGURES:
        path = builder()
        print(f"{path.relative_to(ROOT)}  ({path.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
