#!/usr/bin/env python3
"""Instructional diagrams for the algorithm volumes (DSA-08 through DSA-17).

Those ten volumes had no diagrams at all, while DSA-01 through DSA-07 have
66 between them - and the ten include Graphs and Trees, the most visual
subjects in the curriculum.

Each figure illustrates one mechanism the prose describes in words, chosen
where a picture genuinely carries the idea better than a paragraph: the
Union-Find forest before and after compression, the Morris thread, the
binary-lifting jump table, the LRU map-and-list pairing, and the interval
scheduling exchange argument.

Run:  python scripts/generate_algorithm_diagrams.py
"""

from __future__ import annotations

import diagram_kit as kit
from diagram_kit import (
    BLUE, GOLD, GREEN, INK, LINE, MUTED, NAVY, PALE_BLUE, PALE_GOLD,
    PALE_GRAY, PALE_GREEN, PALE_RED, PALE_TEAL, RED, TEAL, W, H,
)

TITLE = kit.font(60, bold=True)
SECTION = kit.font(38, bold=True)
BODY = kit.font(31)
BOLD = kit.font(31, bold=True)
SMALL = kit.font(26)
MONO = kit.font(30, mono=True)
CELL = kit.font(38, bold=True, mono=True)


def node(draw, cx, cy, label, *, radius=46, fill=PALE_BLUE, outline=BLUE):
    draw.ellipse((cx - radius, cy - radius, cx + radius, cy + radius),
                 fill=fill, outline=outline, width=4)
    kit.centered(draw, (cx - radius, cy - radius, cx + radius, cy + radius),
                 label, f=CELL, fill=INK)


# ---------------------------------------------------------------- DSA-15 ----

def union_find_compression() -> None:
    """Path compression: the same set before and after a find."""
    image, draw = kit.canvas(
        "Path compression flattens the forest",
        "find(5) repoints every node on the path directly at the root",
        "Java SDE-2 DSA Series | Volume 15: Graphs | Figure 01")

    draw.text((150, 250), "before find(5)", font=SECTION, fill=MUTED)
    chain = [(370, 380, "0"), (370, 520, "1"), (370, 660, "3"), (370, 800, "5")]
    for i, (x, y, label) in enumerate(chain):
        node(draw, x, y, label, fill=PALE_GOLD if i == 0 else PALE_BLUE,
             outline=GOLD if i == 0 else BLUE)
        if i:
            kit.arrow(draw, (x, y - 46), (chain[i - 1][0], chain[i - 1][1] + 46), colour=BLUE)
    draw.text((250, 880), "find(5) walks 3 edges", font=SMALL, fill=MUTED)

    draw.line((820, 300, 820, 950), fill=LINE, width=3)

    draw.text((1000, 250), "after find(5)", font=SECTION, fill=MUTED)
    node(draw, 1500, 400, "0", fill=PALE_GOLD, outline=GOLD)
    for cx, label in ((1230, "1"), (1500, "3"), (1770, "5")):
        node(draw, cx, 700, label, fill=PALE_GREEN, outline=GREEN)
        kit.arrow(draw, (cx, 654), (1500, 446), colour=GREEN)
    draw.text((1150, 880), "every later find(5), find(3), find(1) is now one hop",
              font=SMALL, fill=MUTED)

    kit.box(draw, (150, 1010, 2250, 1130), fill=PALE_GRAY)
    kit.centered(draw, (150, 1010, 2250, 1130),
                 "Union by size alone: O(log n).  Compression alone: O(log n).  "
                 "Both together: amortized O(alpha(n)).",
                 f=BODY, fill=INK)
    kit.save(image, "DSA-15-graphs", "01-union-find-path-compression.png")


def state_space_graph() -> None:
    """An implicit graph: states are configurations, edges are moves."""
    image, draw = kit.canvas(
        "An implicit graph has no edge list",
        "Vertices are configurations; edges are whatever the rules permit",
        "Java SDE-2 DSA Series | Volume 15: Graphs | Figure 02")

    draw.text((150, 250), "explicit graph", font=SECTION, fill=MUTED)
    kit.box(draw, (150, 320, 900, 620), fill=PALE_GRAY)
    draw.text((190, 360), "adj[0] = [1, 2]", font=MONO, fill=INK)
    draw.text((190, 420), "adj[1] = [3]", font=MONO, fill=INK)
    draw.text((190, 480), "adj[2] = [3]", font=MONO, fill=INK)
    draw.text((190, 545), "stored, then read", font=SMALL, fill=MUTED)

    draw.text((1150, 250), "implicit graph", font=SECTION, fill=MUTED)
    kit.box(draw, (1150, 320, 2250, 620), fill=PALE_TEAL, outline=TEAL)
    draw.text((1190, 360), 'neighbours("0000") =', font=MONO, fill=INK)
    draw.text((1190, 420), '  turn one dial, either way', font=MONO, fill=INK)
    draw.text((1190, 480), '  -> 8 codes', font=MONO, fill=INK)
    draw.text((1190, 545), "computed on demand, never stored", font=SMALL, fill=MUTED)

    centre = (1200, 850)
    node(draw, centre[0], centre[1], "0000", radius=78, fill=PALE_GOLD, outline=GOLD)
    labels = ["1000", "0100", "0010", "0001", "9000", "0900", "0090", "0009"]
    import math
    for i, label in enumerate(labels):
        angle = 2 * math.pi * i / len(labels)
        x = centre[0] + int(430 * math.cos(angle))
        y = centre[1] + int(210 * math.sin(angle))
        node(draw, x, y, label, radius=66, fill=PALE_BLUE)
        kit.arrow(draw,
                  (centre[0] + int(78 * math.cos(angle)), centre[1] + int(78 * math.sin(angle))),
                  (x - int(66 * math.cos(angle)), y - int(66 * math.sin(angle))),
                  colour=BLUE, width=4, head=16)

    kit.box(draw, (150, 1150, 2250, 1270), fill=PALE_RED, outline=RED)
    kit.centered(draw, (150, 1150, 2250, 1270),
                 "State must exclude the path and the step count, and include everything a move depends on.",
                 f=BODY, fill=INK)
    kit.save(image, "DSA-15-graphs", "02-implicit-state-space-graph.png")


# ---------------------------------------------------------------- DSA-13 ----

def morris_thread() -> None:
    """The Morris thread: a temporary right pointer back to the successor."""
    image, draw = kit.canvas(
        "The Morris thread reuses a null right pointer",
        "3.right temporarily points at 4, so the walk can return without a stack",
        "Java SDE-2 DSA Series | Volume 13: Trees | Figure 01")

    node(draw, 1200, 360, "4", fill=PALE_GOLD, outline=GOLD)
    node(draw, 850, 580, "2")
    node(draw, 1600, 580, "6")
    node(draw, 640, 800, "1")
    node(draw, 1060, 800, "3", fill=PALE_GREEN, outline=GREEN)
    for a, b in (((1200, 360), (850, 580)), ((1200, 360), (1600, 580)),
                 ((850, 580), (640, 800)), ((850, 580), (1060, 800))):
        draw.line((a[0], a[1] + 46, b[0], b[1] - 46), fill=LINE, width=5)

    # the thread
    draw.line((1106, 800, 1420, 800), fill=GREEN, width=6)
    draw.line((1420, 800, 1420, 380), fill=GREEN, width=6)
    kit.arrow(draw, (1420, 400), (1250, 372), colour=GREEN, width=6)
    draw.text((1180, 830), "thread: 3.right -> 4", font=BOLD, fill=GREEN)

    kit.box(draw, (150, 990, 1150, 1240), fill=PALE_GREEN, outline=GREEN)
    draw.text((190, 1020), "Why it terminates", font=SECTION, fill=INK)
    draw.text((190, 1085), "the predecessor search stops at", font=BODY, fill=INK)
    draw.text((190, 1135), "`current`, not only at null - or it", font=BODY, fill=INK)
    draw.text((190, 1185), "follows its own thread forever", font=BODY, fill=INK)

    kit.box(draw, (1250, 990, 2250, 1240), fill=PALE_RED, outline=RED)
    draw.text((1290, 1020), "What it costs", font=SECTION, fill=INK)
    draw.text((1290, 1085), "the tree is mutated mid-walk, so", font=BODY, fill=INK)
    draw.text((1290, 1135), "it is unusable with concurrent", font=BODY, fill=INK)
    draw.text((1290, 1185), "readers or immutable nodes", font=BODY, fill=INK)
    kit.save(image, "DSA-13-trees-bsts-and-tries", "01-morris-traversal-thread.png")


def binary_lifting() -> None:
    """The doubling table that turns O(n) LCA into O(log n)."""
    image, draw = kit.canvas(
        "Binary lifting: jump halfway, then halfway again",
        "up[k][v] = up[k-1][ up[k-1][v] ]",
        "Java SDE-2 DSA Series | Volume 13: Trees | Figure 02")

    xs = [280 + i * 250 for i in range(8)]
    for i, x in enumerate(xs):
        node(draw, x, 420, str(i), radius=52,
             fill=PALE_GOLD if i == 0 else PALE_BLUE,
             outline=GOLD if i == 0 else BLUE)
        if i:
            kit.arrow(draw, (x - 52, 420), (xs[i - 1] + 52, 420), colour=LINE, width=4, head=16)
    draw.text((280, 500), "parent chain: up[0]", font=SMALL, fill=MUTED)

    # 2^1 and 2^2 jumps from node 7
    def hop(y, span, colour, label):
        draw.line((xs[7], 420 + 60, xs[7], y), fill=colour, width=5)
        draw.line((xs[7], y, xs[7 - span], y), fill=colour, width=5)
        kit.arrow(draw, (xs[7 - span], y), (xs[7 - span], 480), colour=colour, width=5)
        draw.text((xs[7 - span] + 30, y - 46), label, font=BOLD, fill=colour)

    hop(700, 2, TEAL, "up[1][7] = 5")
    hop(870, 4, GREEN, "up[2][7] = 3")

    kit.box(draw, (150, 1000, 2250, 1250), fill=PALE_GRAY)
    draw.text((200, 1030), "Lifting node 7 up 6 levels: 6 = binary 110 = jumps of 4 then 2",
              font=BODY, fill=INK)
    draw.text((200, 1090), "Preprocess O(n log n) once, then every LCA query is O(log n).",
              font=BODY, fill=INK)
    draw.text((200, 1150), "Descend from the LARGEST k, and return early if the nodes coincide -",
              font=BODY, fill=INK)
    draw.text((200, 1200), "without that early return, one node being an ancestor of the other returns its parent.",
              font=BODY, fill=INK)
    kit.save(image, "DSA-13-trees-bsts-and-tries", "02-binary-lifting-jumps.png")


# ---------------------------------------------------------------- DSA-08 ----

def lru_structure() -> None:
    """The hash map and doubly-linked list that make LRU O(1)."""
    image, draw = kit.canvas(
        "LRU needs both structures, not either one",
        "The map gives O(1) lookup; the doubly-linked list gives O(1) reordering",
        "Java SDE-2 DSA Series | Volume 08: Hashing | Figure 01")

    draw.text((170, 250), "HashMap: key -> node", font=SECTION, fill=MUTED)
    for i, key in enumerate(["\"a\"", "\"b\"", "\"c\""]):
        y = 330 + i * 110
        kit.box(draw, (170, y, 520, y + 88), fill=PALE_TEAL, outline=TEAL)
        kit.centered(draw, (170, y, 520, y + 88), key, f=CELL, fill=INK)

    draw.text((760, 250), "Doubly-linked list, most recent first", font=SECTION, fill=MUTED)
    boxes = [("head", PALE_GRAY, LINE), ("c", PALE_GREEN, GREEN),
             ("b", PALE_BLUE, BLUE), ("a", PALE_RED, RED), ("tail", PALE_GRAY, LINE)]
    xs = []
    for i, (label, fill, outline) in enumerate(boxes):
        x = 760 + i * 300
        xs.append(x)
        kit.box(draw, (x, 330, x + 230, 470), fill=fill, outline=outline)
        kit.centered(draw, (x, 330, x + 230, 470), label, f=CELL, fill=INK)
        if i:
            kit.arrow(draw, (xs[i - 1] + 230, 380), (x, 380), colour=BLUE, width=4, head=15)
            kit.arrow(draw, (x, 425), (xs[i - 1] + 230, 425), colour=MUTED, width=4, head=15)

    # Elbow routing: down out of the map, along a clear lane, then up into the
    # target box. Straight lines here crossed the list boxes and the labels.
    lane = {0: 660, 1: 620, 2: 580}
    for i, key in enumerate(["\"a\"", "\"b\"", "\"c\""]):
        target = {"\"a\"": xs[3], "\"b\"": xs[2], "\"c\"": xs[1]}[key]
        y0 = 374 + i * 110
        y = lane[i]
        draw.line((520, y0, 600, y0), fill=TEAL, width=3)
        draw.line((600, y0, 600, y), fill=TEAL, width=3)
        draw.line((600, y, target + 115, y), fill=TEAL, width=3)
        kit.arrow(draw, (target + 115, y), (target + 115, 472), colour=TEAL, width=3, head=14)

    draw.text((xs[1], 500), "MRU", font=SMALL, fill=GREEN)
    draw.text((xs[3] - 40, 500), "LRU - evict from here", font=SMALL, fill=RED)

    kit.box(draw, (170, 720, 1180, 1040), fill=PALE_GREEN, outline=GREEN)
    draw.text((210, 750), "Why doubly linked", font=SECTION, fill=INK)
    draw.text((210, 820), "the map finds the node, but", font=BODY, fill=INK)
    draw.text((210, 875), "unlinking it needs its predecessor.", font=BODY, fill=INK)
    draw.text((210, 930), "Singly linked makes that O(n) and", font=BODY, fill=INK)
    draw.text((210, 985), "the whole design collapses.", font=BODY, fill=INK)

    kit.box(draw, (1250, 720, 2250, 1040), fill=PALE_GOLD, outline=GOLD)
    draw.text((1290, 750), "Why the node stores its key", font=SECTION, fill=INK)
    draw.text((1290, 820), "eviction starts from the list end,", font=BODY, fill=INK)
    draw.text((1290, 875), "then must remove the map entry.", font=BODY, fill=INK)
    draw.text((1290, 930), "Without the key that is an O(n)", font=BODY, fill=INK)
    draw.text((1290, 985), "reverse lookup.", font=BODY, fill=INK)

    kit.box(draw, (170, 1100, 2250, 1220), fill=PALE_RED, outline=RED)
    kit.centered(draw, (170, 1100, 2250, 1220),
                 "get() reorders the list, so it mutates - this cache is not safe even for concurrent reads.",
                 f=BODY, fill=INK)
    kit.save(image, "DSA-08-hashing-maps-sets-and-prefix-state", "01-lru-map-and-list.png")


# ---------------------------------------------------------------- DSA-16 ----

def exchange_argument() -> None:
    """The interval scheduling exchange step, drawn."""
    image, draw = kit.canvas(
        "The exchange argument, in one picture",
        "Greedy's interval finishes no later, so swapping it in never breaks validity",
        "Java SDE-2 DSA Series | Volume 16: Greedy | Figure 01")

    def bar(x0, x1, y, label, fill, outline):
        kit.box(draw, (x0, y, x1, y + 92), fill=fill, outline=outline, radius=10)
        kit.centered(draw, (x0, y, x1, y + 92), label, f=BOLD, fill=INK)

    draw.text((170, 260), "OPT and greedy agree up to here", font=SECTION, fill=MUTED)
    bar(200, 620, 330, "agreed", PALE_GRAY, LINE)
    draw.line((660, 300, 660, 1000), fill=LINE, width=4)

    draw.text((700, 260), "first disagreement", font=SECTION, fill=RED)
    bar(700, 1180, 330, "OPT picks o", PALE_RED, RED)
    bar(700, 980, 450, "greedy picks g", PALE_GREEN, GREEN)
    draw.text((700, 570), "finish(g) <= finish(o) - greedy takes the earliest finish",
              font=BODY, fill=INK)

    draw.text((1260, 260), "everything OPT does later", font=SECTION, fill=MUTED)
    bar(1260, 1700, 330, "next", PALE_BLUE, BLUE)
    bar(1780, 2250, 330, "next", PALE_BLUE, BLUE)
    draw.text((1260, 450), "starts at or after finish(o), so also after finish(g)",
              font=BODY, fill=INK)

    kit.arrow(draw, (840, 460), (840, 350), colour=GREEN, width=6)
    draw.text((870, 400), "swap g in: still valid, same count", font=BOLD, fill=GREEN)

    kit.box(draw, (170, 700, 2250, 960), fill=PALE_GREEN, outline=GREEN)
    draw.text((210, 730), "The load-bearing sentence", font=SECTION, fill=INK)
    draw.text((210, 800), "Every later interval starts after finish(o), which is at or after finish(g).",
              font=BODY, fill=INK)
    draw.text((210, 860), "That single fact is what the sort key buys - and it is why sorting by",
              font=BODY, fill=INK)
    draw.text((210, 915), "start time or by duration has no such sentence available.",
              font=BODY, fill=INK)

    kit.box(draw, (170, 1030, 2250, 1150), fill=PALE_GOLD, outline=GOLD)
    kit.centered(draw, (170, 1030, 2250, 1150),
                 "Measured over 400 random inputs: earliest-finish 0 wrong, earliest-start 36 wrong, shortest-first 170 wrong.",
                 f=BODY, fill=INK)
    kit.save(image, "DSA-16-greedy-algorithms", "01-exchange-argument.png")


# ---------------------------------------------------------------- DSA-09 ----

def recursion_depth() -> None:
    """Which recursions are safe, and the quicksort depth fix."""
    image, draw = kit.canvas(
        "Recursion depth is a hard resource bound",
        "Java has no tail-call optimization, so depth that scales with n will overflow",
        "Java SDE-2 DSA Series | Volume 09: Recursion | Figure 01")

    draw.text((170, 250), "balanced tree: depth O(log n)", font=SECTION, fill=GREEN)
    kit.box(draw, (170, 320, 1080, 700), fill=PALE_GREEN, outline=GREEN)
    for level, count in enumerate((1, 2, 4)):
        for i in range(count):
            span = 900 / (count + 1)
            node(draw, int(200 + span * (i + 1)), 390 + level * 130, "", radius=26,
                 fill="#FFFFFF", outline=GREEN)
    draw.text((210, 630), "10^9 nodes -> depth 30. Safe.", font=BODY, fill=INK)

    draw.text((1250, 250), "degenerate tree: depth O(n)", font=SECTION, fill=RED)
    kit.box(draw, (1250, 320, 2250, 700), fill=PALE_RED, outline=RED)
    for i in range(5):
        node(draw, 1330 + i * 90, 390 + i * 55, "", radius=26, fill="#FFFFFF", outline=RED)
    draw.text((1290, 630), "10^6 nodes -> depth 10^6. StackOverflowError.", font=SMALL, fill=INK)

    kit.box(draw, (170, 760, 2250, 1150), fill=PALE_GRAY)
    draw.text((210, 790), "Quicksort: recurse into the smaller half, loop on the larger",
              font=SECTION, fill=INK)
    draw.text((210, 870), "recurse into both      sorted input, n = 200   ->   depth 199",
              font=MONO, fill=RED)
    draw.text((210, 940), "recurse into smaller   sorted input, n = 200   ->   depth   1",
              font=MONO, fill=GREEN)
    draw.text((210, 1030), "Two lines. Each call now covers at most half the range, so depth is O(log n)",
              font=BODY, fill=INK)
    draw.text((210, 1085), "even on adversarial input - without converting anything to iteration.",
              font=BODY, fill=INK)
    kit.save(image, "DSA-09-recursion-and-backtracking", "01-recursion-depth-and-quicksort.png")


def main() -> None:
    for build in (union_find_compression, state_space_graph, morris_thread,
                  binary_lifting, lru_structure, exchange_argument, recursion_depth):
        build()
        print(f"  {build.__name__}")
    print("done")


if __name__ == "__main__":
    main()
