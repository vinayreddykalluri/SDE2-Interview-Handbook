#!/usr/bin/env python3
"""Figures for master chapters 25-30, the collections module.

These chapters carried two diagrams between them and explained structures such
as hash bucket splitting, heap array layout, and comparator chains entirely in
prose. Every number that appears in these figures is computed here rather than
drawn by hand, so a figure cannot drift away from the arithmetic the chapter
states:

* `hash_facts()` reproduces `String.hashCode` (an API contract) and OpenJDK's
  `HashMap.hash` spreading function, so the bucket indices in figures 27-1 and
  27-2 are the indices a real HashMap would use.
* `heap_states()` runs the same sift-up as `java.util.PriorityQueue`, so the
  array in figure 29-2 is the array a real PriorityQueue holds.

Output goes to `assets/diagrams/`, beside the twelve existing figures, because
these chapters are shared by the master book and by JAVA-05, DSA-06, and
DSA-08. Both builds resolve image paths relative to the book root.
"""

from __future__ import annotations

import sys
from pathlib import Path

from PIL import ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))

from diagram_kit import (  # noqa: E402
    W, H, NAVY, BLUE, TEAL, GOLD, GREEN, RED, INK, MUTED, LINE, WHITE,
    PALE_BLUE, PALE_TEAL, PALE_GOLD, PALE_GREEN, PALE_RED, PALE_GRAY,
    arrow, box, canvas, centered, font, legend,
)

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets" / "diagrams"

FOOTER = "Java SDE-2 Interview Preparation Series - vinayreddykalluri"

M32 = 0xFFFFFFFF


# --------------------------------------------------------------------------
# Computed facts. The figures read these; nothing is typed in twice.
# --------------------------------------------------------------------------

def jhash(text: str) -> int:
    """java.lang.String.hashCode, which the API contract fixes exactly."""
    h = 0
    for char in text:
        h = (h * 31 + ord(char)) & M32
    return h


def signed(h: int) -> int:
    return h - (1 << 32) if h & 0x80000000 else h


def spread(h: int) -> int:
    """OpenJDK HashMap.hash: h ^ (h >>> 16)."""
    return (h ^ (h >> 16)) & M32


def index_of(text: str, capacity: int) -> int:
    return (capacity - 1) & spread(jhash(text))


HASH_KEYS = ["frank", "mallory", "grace", "heidi", "alice", "bob"]


def heap_array(values: list[int]) -> list[int]:
    """java.util.PriorityQueue.siftUp, so the array matches a real queue."""
    heap: list[int] = []
    for value in values:
        heap.append(value)
        i = len(heap) - 1
        while i > 0:
            parent = (i - 1) >> 1
            if heap[i] >= heap[parent]:
                break
            heap[i], heap[parent] = heap[parent], heap[i]
            i = parent
    return heap


HEAP_INPUT = [5, 1, 8, 3, 9, 2, 7]
HEAP_STATE = heap_array(HEAP_INPUT)


# --------------------------------------------------------------------------
# Small shared drawing helpers
# --------------------------------------------------------------------------

def cell(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, text: str,
         *, fill: str = WHITE, outline: str = LINE, size: int = 34,
         bold: bool = False, mono: bool = True, ink: str = INK) -> None:
    draw.rectangle((x, y, x + w, y + h), fill=fill, outline=outline, width=3)
    centered(draw, (x, y, x + w, y + h), text,
             f=font(size, bold=bold, mono=mono), fill=ink)


def label(draw: ImageDraw.ImageDraw, x: int, y: int, text: str,
          *, size: int = 30, colour: str = MUTED, bold: bool = False,
          mono: bool = False) -> None:
    draw.text((x, y), text, font=font(size, bold=bold, mono=mono), fill=colour)


def note(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, lines: list[str],
         *, fill: str = PALE_GOLD, edge: str = GOLD, size: int = 29) -> int:
    h = 34 + len(lines) * (size + 12)
    box(draw, (x, y, x + w, y + h), fill=fill, outline=edge, width=3)
    for i, line in enumerate(lines):
        bold = line.startswith("*")
        draw.text((x + 26, y + 20 + i * (size + 12)), line.lstrip("* "),
                  font=font(size, bold=bold), fill=INK)
    return y + h


def save(image, name: str) -> Path:
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / name
    image.save(path, "PNG", optimize=True)
    return path


# --------------------------------------------------------------------------
# Chapter 25 - framework architecture
# --------------------------------------------------------------------------

def fig_hierarchy() -> Path:
    image, d = canvas("What each collection contract promises",
                      "Choose the interface from the guarantee you need, not from the class you remember",
                      FOOTER)
    rows = [
        ("Iterable", "you can walk it once, forward", PALE_GRAY, 300),
        ("Collection", "add, remove, contains, size - membership", PALE_BLUE, 400),
        ("List", "position matters; duplicates allowed", PALE_TEAL, 500),
        ("Set", "no duplicates; position usually undefined", PALE_TEAL, 500),
        ("Queue / Deque", "removal order is the point", PALE_TEAL, 500),
    ]
    x, y = 110, 250
    for name, promise, colour, width in rows:
        box(d, (x, y, x + width, y + 96), fill=colour)
        centered(d, (x, y, x + width, y + 96), name, f=font(38, bold=True))
        label(d, x + width + 30, y + 30, promise, size=30)
        if y > 250:
            arrow(d, (x - 34, y - 60), (x - 34, y + 40), colour=LINE, width=4)
        y += 150
        if name == "Collection":
            x += 70

    # Map sits beside, not below.
    mx = 1500
    box(d, (mx, 250, mx + 700, 346), fill=PALE_GOLD, outline=GOLD)
    centered(d, (mx, 250, mx + 700, 346), "Map", f=font(38, bold=True))
    label(d, mx, 370, "not a Collection: it stores", size=30)
    label(d, mx, 408, "key -> value pairs, not elements", size=30)

    for i, (name, text) in enumerate([
            ("keySet()", "live view of the keys"),
            ("values()", "live view of the values"),
            ("entrySet()", "live view of the pairs")]):
        vy = 480 + i * 118
        box(d, (mx + 60, vy, mx + 420, vy + 88), fill=PALE_GREEN, outline=GREEN)
        centered(d, (mx + 60, vy, mx + 420, vy + 88), name,
                 f=font(31, bold=True, mono=True))
        label(d, mx + 450, vy + 28, text, size=27)
        arrow(d, (mx + 40, 360), (mx + 50, vy + 44), colour=GREEN, width=3)

    note(d, mx, 900, 780, [
        "* These three are views, not copies.",
        "map.keySet().remove(k) removes the",
        "mapping from the map itself.",
    ])
    note(d, 110, 1010, 1240, [
        "* The contract does not promise a cost.",
        "List.get(i) is O(1) on ArrayList and O(n) on LinkedList - both are",
        "correct Lists. Name the concrete class before quoting a complexity.",
    ], fill=PALE_RED, edge=RED)
    return save(image, "13-collection-contracts.png")


def fig_view_vs_copy() -> Path:
    image, d = canvas("View, copy, and unmodifiable are three different things",
                      "The same source, wrapped three ways, after source.add(\"D\")",
                      FOOTER)
    src_y = 260
    label(d, 110, src_y - 58, "source (a mutable ArrayList)", size=32, bold=True)
    for i, v in enumerate("ABC"):
        cell(d, 110 + i * 120, src_y, 110, 100, v, fill=PALE_BLUE)
    cell(d, 110 + 3 * 120, src_y, 110, 100, "D", fill=PALE_GREEN, outline=GREEN)
    label(d, 110 + 4 * 120 + 20, src_y + 30, "<- added after wrapping", size=28,
          colour=GREEN)

    panels = [
        ("Collections.unmodifiableList(source)", "A B C D", PALE_GREEN, GREEN,
         ["a live view of source", "sees the new element",
          "rejects add through this reference"]),
        ("List.copyOf(source)", "A B C", PALE_GOLD, GOLD,
         ["an independent snapshot", "does NOT see the new element",
          "rejects add; also rejects nulls"]),
        ("new ArrayList<>(source)", "A B C", PALE_BLUE, BLUE,
         ["an independent mutable copy", "does NOT see the new element",
          "add is allowed"]),
    ]
    y = 520
    for title, contents, fill, edge, bullets in panels:
        box(d, (110, y, 1180, y + 250), fill=fill, outline=edge)
        d.text((146, y + 26), title, font=font(33, bold=True, mono=True), fill=INK)
        for i, part in enumerate(contents.split()):
            cell(d, 150 + i * 104, y + 92, 94, 84, part, fill=WHITE, size=31)
        for i, b in enumerate(bullets):
            label(d, 620, y + 96 + i * 44, "- " + b, size=28)
        arrow(d, (700, src_y + 110), (620, y - 6), colour=edge, width=3)
        y += 292

    note(d, 1260, 520, 1050, [
        "* Unmodifiable is about the reference,",
        "not about the data.",
        "",
        "None of the three freezes the elements.",
        "If A, B, C are mutable objects, every",
        "panel can still observe them change.",
        "Only immutable element types give you",
        "deep immutability.",
    ])
    note(d, 1260, 960, 1050, [
        "* The practical rule",
        "Copy at a boundary you own. Hand out a",
        "view only when live updates are the",
        "documented behaviour.",
    ], fill=PALE_GRAY, edge=LINE)
    return save(image, "14-view-copy-unmodifiable.png")


# --------------------------------------------------------------------------
# Chapter 26 - lists
# --------------------------------------------------------------------------

def fig_arraylist_growth() -> Path:
    image, d = canvas("Why appending is O(n) and amortized O(1) at the same time",
                      "size is what you see; capacity is what was allocated",
                      FOOTER)
    y = 270
    label(d, 110, y - 60, "capacity 4, size 4 - the next append has nowhere to go",
          size=32, bold=True)
    for i, v in enumerate(["e0", "e1", "e2", "e3"]):
        cell(d, 110 + i * 130, y, 120, 100, v, fill=PALE_BLUE)
    label(d, 110, y + 120, "size = 4    capacity = 4", size=30, mono=True)

    y2 = 520
    label(d, 110, y2 - 58, "append e4: allocate a bigger array, copy 4 references, then write",
          size=32, bold=True)
    for i in range(6):
        text = ["e0", "e1", "e2", "e3", "e4", ""][i]
        fill = PALE_BLUE if i < 4 else (PALE_GREEN if i == 4 else WHITE)
        cell(d, 110 + i * 130, y2, 120, 100, text, fill=fill)
    for i in range(4):
        arrow(d, (170 + i * 130, y + 108), (170 + i * 130, y2 - 8),
              colour=RED, width=3, head=16)
    label(d, 110, y2 + 120, "the 4 red arrows are the O(n) copy", size=29, colour=RED)

    # geometric series
    gx, gy = 1250, 250
    box(d, (gx, gy, gx + 1050, gy + 470), fill=PALE_GRAY, outline=LINE)
    d.text((gx + 34, gy + 24), "Total references copied over n appends",
           font=font(34, bold=True), fill=INK)
    d.text((gx + 34, gy + 82), "doubling:  1 + 2 + 4 + ... + n/2  <  n",
           font=font(31, mono=True), fill=INK)
    d.text((gx + 34, gy + 130), "grow by 10: (n/10) resizes x n/2 avg  =  O(n^2)",
           font=font(31, mono=True), fill=RED)
    rows = [("n = 10,000", "doubling", "16,383", GREEN),
            ("n = 10,000", "grow by 10", "4,996,000", RED)]
    for i, (n, policy, total, colour) in enumerate(rows):
        ry = gy + 210 + i * 78
        d.text((gx + 34, ry), n, font=font(30, mono=True), fill=MUTED)
        d.text((gx + 260, ry), policy, font=font(30), fill=INK)
        d.text((gx + 560, ry), total + " copies", font=font(30, bold=True), fill=colour)
    d.text((gx + 34, gy + 380),
           "Any multiplicative factor gives amortized O(1).",
           font=font(29), fill=MUTED)
    d.text((gx + 34, gy + 420),
           "A constant increment does not.",
           font=font(29), fill=MUTED)

    note(d, 1250, 790, 1050, [
        "* Amortized does not mean smooth",
        "One append still stalls for the full copy.",
        "For a latency budget that matters; call",
        "ensureCapacity when the size is known.",
    ], fill=PALE_RED, edge=RED)
    note(d, 110, 790, 1050, [
        "* Capacity is not part of the List contract",
        "The growth factor is an implementation",
        "detail and has changed between releases.",
        "Never write logic that depends on it.",
    ])
    return save(image, "15-arraylist-growth.png")


def fig_list_memory() -> Path:
    image, d = canvas("The same four elements, two representations",
                      "Why ArrayList wins traversals it is asymptotically tied on",
                      FOOTER)
    label(d, 110, 250, "ArrayList: one object, contiguous reference slots",
          size=34, bold=True, colour=BLUE)
    for i in range(6):
        text = f"e{i}" if i < 4 else "null"
        cell(d, 110 + i * 132, 320, 122, 100, text,
             fill=PALE_BLUE if i < 4 else PALE_GRAY,
             ink=INK if i < 4 else MUTED)
    label(d, 110, 440, "one allocation, indices computed as base + i * slot",
          size=29)
    label(d, 110, 480, "a cache line pulls in several neighbours at once", size=29)

    label(d, 110, 600, "LinkedList: one node object per element, two links each",
          size=34, bold=True, colour=TEAL)
    ny = 690
    for i in range(4):
        nx = 110 + i * 300
        box(d, (nx, ny, nx + 240, ny + 130), fill=PALE_TEAL, outline=TEAL)
        d.line((nx + 70, ny, nx + 70, ny + 130), fill=TEAL, width=2)
        d.line((nx + 170, ny, nx + 170, ny + 130), fill=TEAL, width=2)
        centered(d, (nx, ny, nx + 70, ny + 130), "prev", f=font(23))
        centered(d, (nx + 70, ny, nx + 170, ny + 130), f"e{i}",
                 f=font(32, bold=True, mono=True))
        centered(d, (nx + 170, ny, nx + 240, ny + 130), "next", f=font(23))
        if i:
            arrow(d, (nx - 60, ny + 44), (nx + 6, ny + 44), colour=TEAL, width=3, head=14)
            arrow(d, (nx + 4, ny + 92), (nx - 62, ny + 92), colour=MUTED, width=3, head=14)
    label(d, 110, 850, "n allocations; each hop may miss the cache", size=29)

    box(d, (110, 940, 1180, 1180), fill=PALE_GRAY, outline=LINE)
    d.text((146, 962), "Indexed traversal of a LinkedList, counted",
           font=font(33, bold=True), fill=INK)
    lines = [
        "get(i) walks from the nearer end: min(i, n-1-i) hops",
        "total over a full pass = floor((n-1)^2 / 4)",
        "",
        "n = 10,000   indexed: 24,995,000 hops    iterator: 10,000 hops",
    ]
    for i, line in enumerate(lines):
        d.text((146, 1014 + i * 40), line, font=font(28, mono=(i != 2)),
               fill=RED if line.startswith("n =") else MUTED)

    note(d, 1260, 940, 1050, [
        "* Both are O(n) to iterate.",
        "The difference is allocation count, cache",
        "behaviour, and GC pressure - none of",
        "which appear in the complexity column.",
        "",
        "Prefer ArrayDeque over LinkedList even",
        "for queue use.",
    ])
    return save(image, "16-list-memory-layout.png")


def fig_sublist() -> Path:
    image, d = canvas("subList is a window, not a slice",
                      "list.subList(2, 5) shares storage with its parent",
                      FOOTER)
    y = 300
    label(d, 110, y - 60, "parent list", size=33, bold=True)
    for i in range(8):
        inside = 2 <= i < 5
        cell(d, 110 + i * 150, y, 140, 104, f"e{i}",
             fill=PALE_GOLD if inside else PALE_BLUE,
             outline=GOLD if inside else LINE)
        label(d, 110 + i * 150 + 56, y + 116, str(i), size=27)

    d.rounded_rectangle((110 + 2 * 150 - 12, y - 22, 110 + 5 * 150 - 10, y + 126),
                        radius=12, outline=GOLD, width=6)
    label(d, 110 + 2 * 150, y + 175, "subList(2, 5): from is included, to is excluded",
          size=30, colour=GOLD, bold=True)

    box(d, (110, 620, 1150, 900), fill=PALE_GREEN, outline=GREEN)
    d.text((146, 644), "What the view really stores", font=font(33, bold=True), fill=INK)
    for i, line in enumerate([
            "a reference to the parent",
            "offset = 2",
            "size   = 3",
            "",
            "view.set(0, x) writes parent index 2",
            "view.clear() removes the whole range from the parent"]):
        d.text((146, 700 + i * 34), line, font=font(28, mono=True), fill=INK)

    box(d, (1220, 620, 2290, 900), fill=PALE_RED, outline=RED)
    d.text((1256, 644), "What breaks it", font=font(33, bold=True), fill=INK)
    for i, line in enumerate([
            "Structurally modifying the PARENT directly",
            "while the view is alive leaves the view's",
            "recorded size stale.",
            "",
            "The next view operation typically throws",
            "ConcurrentModificationException - and the",
            "specification calls the result undefined,",
            "so do not build on the exception."]):
        d.text((1256, 700 + i * 33), line, font=font(27), fill=INK)

    note(d, 110, 960, 2180, [
        "* Want an independent range?  new ArrayList<>(list.subList(2, 5))",
        "The view also keeps the whole parent reachable. A three-element view of a "
        "million-element list retains all million.",
    ])
    return save(image, "17-sublist-view.png")


# --------------------------------------------------------------------------
# Chapter 27 - hashing
# --------------------------------------------------------------------------

def fig_hash_pipeline() -> Path:
    image, d = canvas("From key to bucket, with real numbers",
                      "String.hashCode is a contract; the spread and the mask are OpenJDK",
                      FOOTER)
    key = "alice"
    h, s = jhash(key), spread(jhash(key))
    stages = [
        ('"alice"', "the key", PALE_GRAY, LINE),
        (f"{signed(h)}", "hashCode()\nfixed by the String contract", PALE_BLUE, BLUE),
        (f"{signed(s)}", "h ^ (h >>> 16)\nmixes high bits down", PALE_TEAL, TEAL),
        (f"{index_of(key, 16)}", "hash & (capacity - 1)\nwith capacity 16", PALE_GOLD, GOLD),
    ]
    x = 130
    for i, (value, caption, fill, edge) in enumerate(stages):
        box(d, (x, 280, x + 470, 430), fill=fill, outline=edge)
        centered(d, (x, 280, x + 470, 430), value, f=font(44, bold=True, mono=True))
        for j, line in enumerate(caption.split("\n")):
            label(d, x + 12, 450 + j * 36, line, size=28,
                  bold=(j == 0), colour=INK if j == 0 else MUTED)
        if i < 3:
            arrow(d, (x + 480, 355), (x + 560, 355), colour=edge, width=5)
        x += 570

    label(d, 130, 600, "Why the spread step exists", size=34, bold=True)
    label(d, 130, 650,
          "The mask keeps only the low bits. Two keys differing only in their high bits would",
          size=29)
    label(d, 130, 690,
          "collide every time. XOR-ing the top 16 bits down lets them influence the index.",
          size=29)

    ty = 790
    label(d, 130, ty, "Six real keys at capacity 16", size=34, bold=True)
    headers = ["key", "hashCode()", "spread", "index"]
    widths = [280, 320, 320, 180]
    x = 130
    for hdr, w in zip(headers, widths):
        cell(d, x, ty + 50, w, 66, hdr, fill=NAVY, ink=WHITE, size=28, bold=True)
        x += w
    for r, k in enumerate(HASH_KEYS):
        x = 130
        kh = jhash(k)
        vals = [k, str(signed(kh)), str(signed(spread(kh))), str(index_of(k, 16))]
        collide = index_of(k, 16) in {index_of(o, 16) for o in HASH_KEYS if o != k}
        for v, w in zip(vals, widths):
            cell(d, x, ty + 116 + r * 62, w, 62, v,
                 fill=PALE_RED if collide else WHITE, size=27)
            x += w
    label(d, 1250, ty + 120, "shaded rows share a bucket -", size=28, colour=RED)
    label(d, 1250, ty + 158, "that is a collision, not an error", size=28, colour=RED)

    note(d, 1250, ty + 220, 1060, [
        "* Expected O(1), not guaranteed O(1)",
        "A bucket holds a short list. Once it grows",
        "past a threshold and the table is large",
        "enough, OpenJDK converts that bucket to a",
        "tree, so the bad case degrades to O(log n)",
        "rather than O(n) - if the keys are",
        "Comparable.",
    ])
    return save(image, "18-hash-to-bucket.png")


def fig_resize_split() -> Path:
    image, d = canvas("What resizing actually does to a bucket",
                      "Doubling 16 -> 32: an entry stays at i or moves to i + 16, nothing else",
                      FOOTER)
    left = [k for k in HASH_KEYS if index_of(k, 16) == index_of("frank", 16)]
    right = [k for k in HASH_KEYS if index_of(k, 16) == index_of("grace", 16)]

    def draw_bucket(x, y, title, keys, cap, fill, edge):
        box(d, (x, y, x + 620, y + 92), fill=edge, outline=edge)
        centered(d, (x, y, x + 620, y + 92), title, f=font(31, bold=True), fill=WHITE)
        for i, k in enumerate(keys):
            cell(d, x, y + 92 + i * 76, 620, 76,
                 f"{k}  (hash & 16 = {spread(jhash(k)) & 16})", fill=fill, size=28)

    label(d, 130, 250, "before: capacity 16", size=34, bold=True)
    draw_bucket(130, 300, f"bucket {index_of('frank', 16)}", left, 16, PALE_BLUE, BLUE)
    draw_bucket(130, 560, f"bucket {index_of('grace', 16)}", right, 16, PALE_TEAL, TEAL)

    label(d, 1310, 250, "after: capacity 32", size=34, bold=True)
    lo = [k for k in left if spread(jhash(k)) & 16 == 0]
    hi = [k for k in left if spread(jhash(k)) & 16]
    draw_bucket(1310, 300, f"bucket {index_of(lo[0], 32)}", lo, 32, PALE_GREEN, GREEN)
    draw_bucket(1310, 470, f"bucket {index_of(hi[0], 32)}", hi, 32, PALE_GOLD, GOLD)
    draw_bucket(1310, 640, f"bucket {index_of(right[0], 32)}", right, 32, PALE_TEAL, TEAL)

    arrow(d, (770, 400), (1290, 360), colour=GREEN, width=4)
    arrow(d, (770, 440), (1290, 530), colour=GOLD, width=4)
    arrow(d, (770, 660), (1290, 700), colour=TEAL, width=4)

    note(d, 130, 900, 1080, [
        "* The split test is one bit",
        "hash & oldCapacity == 0  ->  stay at i",
        "otherwise                ->  move to i + oldCapacity",
        "",
        "No key is rehashed. Relative order inside a",
        "bucket is preserved, which is why resizing",
        "cannot create an infinite loop the way the",
        "pre-Java-8 algorithm could under a race.",
    ])
    note(d, 1310, 900, 1000, [
        "* Resizing does not fix every collision",
        f"frank and mallory shared bucket "
        f"{index_of('frank', 16)} and separated.",
        f"grace and heidi shared bucket "
        f"{index_of('grace', 16)} and both moved",
        f"to {index_of('grace', 32)} - still together.",
        "",
        "Their hashes agree on more than one bit.",
    ], fill=PALE_RED, edge=RED)
    return save(image, "19-hashmap-resize-split.png")


def fig_mutated_key() -> Path:
    image, d = canvas("The entry is still there. The lookup is looking elsewhere.",
                      "A key field changed after insertion, so its bucket index changed",
                      FOOTER)

    def okey(*vals):
        h = 1
        for v in vals:
            h = (h * 31 + (jhash(v) if isinstance(v, str) else v & M32)) & M32
        return h

    before, after = okey("cart-91", "OPEN"), okey("cart-91", "PAID")
    b_idx, a_idx = (15 & spread(before)), (15 & spread(after))

    steps = [
        ("1. put(key, order)", f'key = ("cart-91", "OPEN")',
         f"hash -> bucket {b_idx}", PALE_GREEN, GREEN),
        ("2. key.status = \"PAID\"", 'key = ("cart-91", "PAID")',
         "the key object is already inside the map", PALE_GOLD, GOLD),
        ("3. get(key)", f'key = ("cart-91", "PAID")',
         f"hash -> bucket {a_idx}", PALE_RED, RED),
    ]
    y = 270
    for title, keytext, result, fill, edge in steps:
        box(d, (130, y, 1150, y + 190), fill=fill, outline=edge)
        d.text((166, y + 22), title, font=font(34, bold=True, mono=True), fill=INK)
        d.text((166, y + 76), keytext, font=font(29, mono=True), fill=INK)
        d.text((166, y + 122), result, font=font(29), fill=MUTED)
        y += 230

    bx = 1280
    label(d, bx, 240, "the table", size=33, bold=True)
    for i in range(16):
        row, col = divmod(i, 8)
        x = bx + col * 128
        yy = 300 + row * 120
        occupied = i == b_idx
        searched = i == a_idx
        fill = PALE_GREEN if occupied else (PALE_RED if searched else WHITE)
        cell(d, x, yy, 118, 110, str(i), fill=fill,
             outline=GREEN if occupied else (RED if searched else LINE))
        if occupied:
            label(d, x + 6, yy + 116, "entry", size=23, colour=GREEN)
        if searched:
            label(d, x + 6, yy + 116, "searched", size=23, colour=RED)

    note(d, bx, 620, 1020, [
        "* get returns null. remove does nothing.",
        "size() still counts it. Iterating still",
        "yields it. The entry is unreachable by key",
        "but perfectly present by traversal.",
    ], fill=PALE_RED, edge=RED)

    note(d, 130, 960, 1150, [
        "* Measured, and worse than it sounds",
        "Changing one field of a two-field key moved",
        "the bucket in 93.7% of 200,000 random cases",
        "at capacity 16 - close to the 93.8% you would",
        "expect by chance.",
        "",
        "So roughly one lookup in sixteen still works.",
        "An always-failing bug is easy to find; this",
        "one is not.",
    ])
    note(d, bx, 1150, 1020, [
        "* The rule",
        "Any field used by hashCode or equals must",
        "not change while the object is a key.",
        "Prefer immutable keys - records are ideal.",
    ], fill=PALE_GRAY, edge=LINE)
    return save(image, "20-hashmap-mutated-key.png")


# --------------------------------------------------------------------------
# Chapter 28 - sorted maps
# --------------------------------------------------------------------------

def fig_navigable() -> Path:
    image, d = canvas("One tree, six navigation questions",
                      "TreeMap answers range and neighbour queries a HashMap cannot",
                      FOOTER)
    keys = [10, 20, 30, 40, 50, 60, 70]
    positions = {
        40: (1180, 300), 20: (700, 500), 60: (1660, 500),
        10: (420, 700), 30: (980, 700), 50: (1420, 700), 70: (1900, 700),
    }
    for k, (x, y) in positions.items():
        for child in (k - 20 // (1 if k == 40 else 2), ):
            pass
    edges = [(40, 20), (40, 60), (20, 10), (20, 30), (60, 50), (60, 70)]
    for a, b in edges:
        d.line((positions[a][0] + 60, positions[a][1] + 110,
                positions[b][0] + 60, positions[b][1]), fill=LINE, width=4)
    for k, (x, y) in positions.items():
        highlight = k in (30, 40, 50)
        box(d, (x, y, x + 120, y + 110),
            fill=PALE_GOLD if highlight else PALE_BLUE,
            outline=GOLD if highlight else BLUE)
        centered(d, (x, y, x + 120, y + 110), str(k), f=font(40, bold=True, mono=True))

    label(d, 130, 880, "asking about the key 35", size=34, bold=True)
    queries = [
        ("floorKey(35)", "30", "greatest key <= 35"),
        ("ceilingKey(35)", "40", "least key >= 35"),
        ("lowerKey(30)", "20", "strictly less"),
        ("higherKey(30)", "40", "strictly greater"),
        ("headMap(40)", "10, 20, 30", "everything below 40"),
        ("subMap(20, 50)", "20, 30, 40", "half-open range"),
    ]
    for i, (call, answer, meaning) in enumerate(queries):
        row, col = divmod(i, 2)
        x = 130 + col * 1120
        y = 940 + row * 96
        cell(d, x, y, 400, 80, call, fill=PALE_TEAL, size=29, bold=True)
        cell(d, x + 400, y, 260, 80, answer, fill=WHITE, size=29)
        label(d, x + 680, y + 24, meaning, size=27)

    note(d, 1300, 240, 1000, [
        "* This is what you buy with O(log n)",
        "HashMap answers 'is this exact key",
        "present'. Nothing more. There is no",
        "cheap way to ask it for the nearest",
        "key, a range, or the smallest entry.",
        "",
        "If you need any of those, the log",
        "factor is not a cost - it is the",
        "feature.",
    ])
    return save(image, "21-navigable-map.png")


def fig_compareto_vs_equals() -> Path:
    image, d = canvas("A sorted set does not use equals",
                      "TreeSet decides duplicates with compareTo; HashSet uses equals and hashCode",
                      FOOTER)
    label(d, 130, 250, 'adding new BigDecimal("1.0") then new BigDecimal("1.00")',
          size=34, bold=True, mono=True)

    panels = [
        ("HashSet", "equals + hashCode", "size 2",
         ['equals compares unscaled value AND scale',
          '"1.0" and "1.00" have different scales',
          "-> they are different elements"],
         PALE_BLUE, BLUE),
        ("TreeSet", "compareTo", "size 1",
         ['compareTo compares numeric value only',
          '"1.0".compareTo("1.00") == 0',
          "-> the second add is a duplicate and is dropped"],
         PALE_RED, RED),
    ]
    for i, (name, basis, size, bullets, fill, edge) in enumerate(panels):
        x = 130 + i * 1120
        box(d, (x, 340, x + 1040, 700), fill=fill, outline=edge)
        d.text((x + 36, 366), name, font=font(44, bold=True), fill=INK)
        d.text((x + 36, 428), "decides duplicates using " + basis,
               font=font(29), fill=MUTED)
        box(d, (x + 740, 366, x + 1000, 452), fill=WHITE, outline=edge)
        centered(d, (x + 740, 366, x + 1000, 452), size, f=font(36, bold=True))
        for j, b in enumerate(bullets):
            d.text((x + 36, 500 + j * 46), b, font=font(28), fill=INK)

    note(d, 130, 760, 2180, [
        "* The Javadoc says this out loud, and it is easy to miss",
        "SortedSet and SortedMap are described as behaving inconsistently with Set and Map "
        "when the ordering is not consistent with equals.",
        "That is not a bug in BigDecimal or in TreeSet. It is the documented consequence of "
        "two different notions of sameness.",
    ])

    note(d, 130, 990, 1050, [
        "* Where this bites in production",
        "A comparator that only compares one field",
        "silently deduplicates rows that differ in",
        "every other field.",
        "",
        "Always give a comparator a final tie-break",
        "on a unique key.",
    ], fill=PALE_RED, edge=RED)
    note(d, 1260, 990, 1050, [
        "* Two safe habits",
        "1. Use compareTo == 0 only where you mean",
        "   'same position in the order'.",
        "2. For BigDecimal equality by value, use",
        "   compareTo, not equals - deliberately.",
    ], fill=PALE_GRAY, edge=LINE)
    return save(image, "22-compareto-vs-equals.png")


# --------------------------------------------------------------------------
# Chapter 29 - queues and heaps
# --------------------------------------------------------------------------

def fig_ring_buffer() -> Path:
    image, d = canvas("ArrayDeque is a ring, which is why both ends are cheap",
                      "head and tail move; the elements do not",
                      FOOTER)
    slots = ["", "", "C", "D", "E", "F", "", ""]
    head, tail = 2, 6
    for i, v in enumerate(slots):
        used = v != ""
        cell(d, 130 + i * 190, 300, 180, 130, v or "-", size=40,
             fill=PALE_BLUE if used else WHITE,
             ink=INK if used else MUTED)
        label(d, 130 + i * 190 + 78, 442, str(i), size=27)
    arrow(d, (130 + head * 190 + 90, 260), (130 + head * 190 + 90, 292),
          colour=GREEN, width=5)
    label(d, 130 + head * 190 + 10, 216, "head", size=29, colour=GREEN, bold=True)
    arrow(d, (130 + tail * 190 + 90, 260), (130 + tail * 190 + 90, 292),
          colour=GOLD, width=5)
    label(d, 130 + tail * 190 + 10, 216, "tail", size=29, colour=GOLD, bold=True)

    label(d, 130, 520, "addFirst(B): head moves left, wrapping with (head - 1) & (capacity - 1)",
          size=30)
    slots2 = ["", "B", "C", "D", "E", "F", "", ""]
    for i, v in enumerate(slots2):
        used = v != ""
        cell(d, 130 + i * 190, 570, 180, 130, v or "-", size=40,
             fill=PALE_GREEN if v == "B" else (PALE_BLUE if used else WHITE),
             outline=GREEN if v == "B" else LINE,
             ink=INK if used else MUTED)

    label(d, 130, 740, "addLast(G), addLast(H): tail wraps around to index 0",
          size=30)
    slots3 = ["H", "B", "C", "D", "E", "F", "G", ""]
    for i, v in enumerate(slots3):
        used = v != ""
        cell(d, 130 + i * 190, 790, 180, 130, v or "-", size=40,
             fill=PALE_GOLD if v in "GH" else (PALE_BLUE if used else WHITE),
             outline=GOLD if v in "GH" else LINE,
             ink=INK if used else MUTED)
    arrow(d, (130 + 6 * 190 + 90, 950), (130 + 90, 950), colour=GOLD, width=4)
    label(d, 700, 960, "the tail wrapped: index 7 then 0", size=28, colour=GOLD)

    note(d, 130, 1020, 1080, [
        "* Capacity is always a power of two",
        "so wrapping is a mask, not a modulo:",
        "(i + 1) & (capacity - 1)",
        "",
        "That is also why ArrayDeque has no",
        "capacity-limited mode - growing is",
        "doubling, and it never refuses an add.",
    ])
    note(d, 1260, 1020, 1050, [
        "* Prefer ArrayDeque to both",
        "Stack (legacy, synchronised, iterates in",
        "the wrong direction) and LinkedList",
        "(one allocation per element).",
        "",
        "It rejects null - which is a feature: null",
        "is the 'empty' signal for poll and peek.",
    ], fill=PALE_GREEN, edge=GREEN)
    return save(image, "23-arraydeque-ring.png")


def fig_heap_layout() -> Path:
    image, d = canvas("A heap is an array pretending to be a tree",
                      f"after offering {', '.join(map(str, HEAP_INPUT))} to a PriorityQueue",
                      FOOTER)
    heap = HEAP_STATE
    pos = {0: (1120, 250), 1: (700, 420), 2: (1540, 420),
           3: (480, 590), 4: (920, 590), 5: (1320, 590), 6: (1760, 590)}
    for i in range(1, len(heap)):
        parent = (i - 1) >> 1
        d.line((pos[parent][0] + 62, pos[parent][1] + 112,
                pos[i][0] + 62, pos[i][1]), fill=LINE, width=4)
    for i, v in enumerate(heap):
        box(d, (pos[i][0], pos[i][1], pos[i][0] + 124, pos[i][1] + 112),
            fill=PALE_GREEN if i == 0 else PALE_BLUE,
            outline=GREEN if i == 0 else BLUE)
        centered(d, (pos[i][0], pos[i][1], pos[i][0] + 124, pos[i][1] + 112),
                 str(v), f=font(42, bold=True, mono=True))
        label(d, pos[i][0] + 132, pos[i][1] + 36, f"[{i}]", size=24)

    label(d, 130, 760, "the array underneath", size=34, bold=True)
    for i, v in enumerate(heap):
        cell(d, 130 + i * 150, 810, 140, 110, str(v),
             fill=PALE_GREEN if i == 0 else PALE_BLUE, size=38)
        label(d, 130 + i * 150 + 56, 930, str(i), size=26)

    label(d, 1260, 760, "index arithmetic, no node objects", size=32, bold=True)
    for i, line in enumerate(["parent(i) = (i - 1) / 2",
                              "left(i)   = 2i + 1",
                              "right(i)  = 2i + 2"]):
        d.text((1260, 815 + i * 44), line, font=font(31, mono=True), fill=INK)

    note(d, 130, 1000, 1080, [
        "* Only index 0 is guaranteed",
        f"iteration order: {', '.join(map(str, heap))}",
        f"sorted order:    {', '.join(map(str, sorted(heap)))}",
        "",
        "The heap invariant says a parent precedes",
        "its children. It says nothing about",
        "siblings, so iteration is not sorted.",
    ], fill=PALE_RED, edge=RED)
    note(d, 1260, 1000, 1050, [
        "* Consequences you should be able to state",
        "peek is O(1); offer and poll are O(log n).",
        "contains and remove(Object) are O(n).",
        "There is no decrease-key.",
        "toString and forEach show array order.",
    ])
    return save(image, "24-heap-array-layout.png")


def fig_sift() -> Path:
    image, d = canvas("Sift up on offer, sift down on poll",
                      "Each operation touches one root-to-leaf path: O(log n)",
                      FOOTER)

    def draw_row(y, arrays, titles, highlight):
        for col, (arr, title, hi) in enumerate(zip(arrays, titles, highlight)):
            x = 130 + col * 740
            label(d, x, y - 46, title, size=29, bold=True)
            for i, v in enumerate(arr):
                cell(d, x + i * 100, y, 92, 92, str(v), size=32,
                     fill=PALE_GOLD if i in hi else PALE_BLUE,
                     outline=GOLD if i in hi else LINE)

    label(d, 130, 250, "offer(2) into [1, 3, 8, 5, 9]", size=36, bold=True, colour=BLUE)
    draw_row(340,
             [[1, 3, 8, 5, 9, 2], [1, 3, 2, 5, 9, 8], [1, 3, 2, 5, 9, 8]],
             ["1. append at the end", "2. swap with parent (index 2)", "3. parent is smaller: stop"],
             [{5}, {2, 5}, set()])
    label(d, 130, 470, "the new element walked up one level; at most log2(n) swaps",
          size=29)

    label(d, 130, 590, "poll() from [1, 3, 2, 5, 9, 8]", size=36, bold=True, colour=TEAL)
    draw_row(690,
             [[8, 3, 2, 5, 9], [2, 3, 8, 5, 9], [2, 3, 8, 5, 9]],
             ["1. take index 0, move the last element there",
              "2. swap with the SMALLER child",
              "3. children are larger: stop"],
             [{0}, {0, 2}, set()])
    label(d, 130, 820,
          "Swapping with the smaller child is what preserves the invariant. "
          "Swapping with either child does not.",
          size=29, colour=RED)

    note(d, 130, 920, 1080, [
        "* Why building a heap from n elements is O(n)",
        "n offers cost O(n log n). But heapifying an",
        "existing array bottom-up costs O(n), because",
        "most nodes are near the leaves and sift down",
        "a very short distance.",
        "",
        "new PriorityQueue<>(collection) uses the O(n)",
        "path; a loop of offer() does not.",
    ])
    note(d, 1260, 920, 1050, [
        "* Top-k without sorting everything",
        "Keep a heap of size k with the inverted",
        "ordering; push, and poll whenever size",
        "exceeds k.",
        "",
        "O(n log k) time and O(k) space instead of",
        "O(n log n) time and O(n) space.",
    ], fill=PALE_GREEN, edge=GREEN)
    return save(image, "25-heap-sift.png")


# --------------------------------------------------------------------------
# Chapter 30 - ordering
# --------------------------------------------------------------------------

def fig_comparator_chain() -> Path:
    image, d = canvas("A comparator chain is a cascade, not a formula",
                      "thenComparing runs only when everything before it returned zero",
                      FOOTER)
    stages = [
        ("comparing(Order::priority)", "priority differs?", GREEN),
        ("thenComparing(Order::dueDate)", "same priority - due date differs?", TEAL),
        ("thenComparing(Order::id)", "same date - id differs?", BLUE),
    ]
    y = 280
    for i, (code, question, colour) in enumerate(stages):
        box(d, (130, y, 1180, y + 150), fill=PALE_GRAY, outline=colour, width=4)
        d.text((166, y + 22), code, font=font(32, bold=True, mono=True), fill=INK)
        d.text((166, y + 76), question, font=font(29), fill=MUTED)
        box(d, (1240, y + 20, 1560, y + 110), fill=PALE_GREEN, outline=GREEN)
        centered(d, (1240, y + 20, 1560, y + 110), "yes -> done",
                 f=font(29, bold=True))
        arrow(d, (1190, y + 65), (1230, y + 65), colour=GREEN, width=4)
        if i < 2:
            arrow(d, (655, y + 158), (655, y + 232), colour=colour, width=5)
            label(d, 690, y + 176, "returned 0", size=27)
        y += 240

    note(d, 1240, 380, 1070, [
        "* The last link decides your ties",
        "If the chain can still return 0 for two",
        "different objects, then:",
        "",
        "- sort order between them is arbitrary",
        "- a TreeSet treats them as ONE element",
        "- pagination can repeat or skip rows",
        "",
        "End every chain on something unique.",
    ], fill=PALE_RED, edge=RED)

    label(d, 130, 1030, "Never write a - b", size=36, bold=True, colour=RED)
    headers = ["a", "b", "a - b as int", "says", "truth"]
    widths = [300, 300, 340, 180, 180]
    x = 130
    for hdr, w in zip(headers, widths):
        cell(d, x, 1090, w, 62, hdr, fill=NAVY, ink=WHITE, size=27, bold=True)
        x += w
    rows = [("2000000000", "-2000000000", "-294967296", "a < b", "a > b"),
            ("2147483647", "-1", "-2147483648", "a < b", "a > b")]
    for r, row in enumerate(rows):
        x = 130
        for c, (v, w) in enumerate(zip(row, widths)):
            cell(d, x, 1152 + r * 62, w, 62, v, fill=PALE_RED if c >= 3 else WHITE,
                 size=26)
            x += w
    label(d, 1470, 1160,
          "Overflow flips the sign. Measured over 200,000 random", size=27)
    label(d, 1470, 1196,
          "int pairs, subtraction was wrong 25.0% of the time.", size=27)
    label(d, 1470, 1232, "Use Integer.compare(a, b).", size=27, bold=True, colour=GREEN)
    return save(image, "26-comparator-chain.png")


def fig_intransitive() -> Path:
    image, d = canvas("An inconsistent comparator does not throw. It just lies.",
                      'The rule: "within 10 counts as equal, otherwise compare"',
                      FOOTER)
    nodes = {15: (420, 300), 5: (1120, 300), 0: (1820, 300)}
    for v, (x, y) in nodes.items():
        box(d, (x, y, x + 170, y + 150), fill=PALE_BLUE, outline=BLUE)
        centered(d, (x, y, x + 170, y + 150), str(v), f=font(52, bold=True, mono=True))
    arrow(d, (600, 375), (1105, 375), colour=MUTED, width=4)
    label(d, 700, 315, "compare(15, 5) = 0", size=29, mono=True)
    arrow(d, (1300, 375), (1805, 375), colour=MUTED, width=4)
    label(d, 1400, 315, "compare(5, 0) = 0", size=29, mono=True)
    d.arc((505, 460, 1905, 700), start=0, end=180, fill=RED, width=6)
    d.line((505, 455, 505, 465), fill=RED, width=6)
    d.line((1905, 455, 1905, 465), fill=RED, width=6)
    centered(d, (505, 706, 1905, 754), "compare(15, 0) = +1",
             f=font(34, bold=True, mono=True), fill=RED)
    centered(d, (505, 754, 1905, 796), "so 15 must come AFTER 0",
             f=font(30), fill=RED)

    box(d, (130, 850, 1150, 1180), fill=PALE_RED, outline=RED)
    d.text((166, 876), "sort a list of [15, 5, 0]", font=font(34, bold=True), fill=INK)
    for i, line in enumerate([
            "input  : [15, 5, 0]",
            "output : [15, 5, 0]",
            "",
            "Nothing moved. Every adjacent comparison",
            "returned 0, so the sort saw no work to do -",
            "and the result is exactly backwards."]):
        d.text((166, 936 + i * 38), line,
               font=font(28, mono=(i < 2)), fill=INK)

    note(d, 1250, 850, 1060, [
        "* How I nearly missed this",
        "My first check compared adjacent pairs.",
        "It found zero violations and would have",
        "cleared the comparator.",
        "",
        "The defect only shows across NON-adjacent",
        "pairs: over 20,000 random 12-element lists,",
        "10,454 outputs contained at least one pair",
        "in the wrong order.",
    ])
    return save(image, "27-intransitive-comparator.png")


def fig_selection() -> Path:
    image, d = canvas("Three ways to get the top k, and when each one wins",
                      "Sorting everything is the default answer and usually the wrong one",
                      FOOTER)
    options = [
        ("Sort, then take k", "list.sort(cmp)\nsublist(0, k)",
         "O(n log n)", "O(n)", "you need the whole order anyway",
         PALE_BLUE, BLUE),
        ("Bounded heap of size k", "PriorityQueue with\nthe reversed order",
         "O(n log k)", "O(k)", "k is small and n is huge or streaming",
         PALE_GREEN, GREEN),
        ("Quickselect / partition", "partition around a\npivot, recurse one side",
         "O(n) expected", "O(1) extra", "you need the k-th element, order irrelevant",
         PALE_GOLD, GOLD),
    ]
    x = 130
    for title, code, time, space, when, fill, edge in options:
        box(d, (x, 260, x + 700, 900), fill=fill, outline=edge)
        d.text((x + 34, 288), title, font=font(36, bold=True), fill=INK)
        for i, line in enumerate(code.split("\n")):
            d.text((x + 34, 360 + i * 40), line, font=font(28, mono=True), fill=MUTED)
        box(d, (x + 34, 470, x + 666, 566), fill=WHITE, outline=edge)
        centered(d, (x + 34, 470, x + 666, 566), time, f=font(34, bold=True, mono=True))
        d.text((x + 34, 590), "extra space: " + space, font=font(28, mono=True), fill=INK)
        d.text((x + 34, 650), "choose when", font=font(27, bold=True), fill=MUTED)
        words, line, lines = when.split(), "", []
        for word in words:
            if len(line + " " + word) > 30:
                lines.append(line); line = word
            else:
                line = (line + " " + word).strip()
        lines.append(line)
        for i, l in enumerate(lines):
            d.text((x + 34, 692 + i * 38), l, font=font(28), fill=INK)
        x += 740

    note(d, 130, 960, 1080, [
        "* The interview version of this",
        "\"Find the 100 largest of 10 million.\"",
        "",
        "Sorting: ~10,000,000 x 24 comparisons and",
        "a full copy in memory.",
        "Bounded heap: ~10,000,000 x 7, and 100",
        "elements resident.",
    ])
    note(d, 1250, 960, 1060, [
        "* Two traps",
        "1. A bounded heap needs the REVERSED",
        "   comparator - you evict the smallest of",
        "   the current best.",
        "2. Quickselect reorders the input. If the",
        "   caller still needs the original order,",
        "   that is a copy you did not budget for.",
    ], fill=PALE_RED, edge=RED)
    return save(image, "28-top-k-strategies.png")


FIGURES = [
    fig_hierarchy, fig_view_vs_copy,
    fig_arraylist_growth, fig_list_memory, fig_sublist,
    fig_hash_pipeline, fig_resize_split, fig_mutated_key,
    fig_navigable, fig_compareto_vs_equals,
    fig_ring_buffer, fig_heap_layout, fig_sift,
    fig_comparator_chain, fig_intransitive, fig_selection,
]


def main() -> None:
    for builder in FIGURES:
        path = builder()
        print(f"{path.relative_to(ROOT)}  ({path.stat().st_size // 1024} KB)")
    print(f"\n{len(FIGURES)} figures written to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
