#!/usr/bin/env python3
"""Generate print-safe diagrams for the Java SDE-2 interview book."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "diagrams"

W, H = 1800, 1080
NAVY = "#0B2545"
BLUE = "#1F5A94"
CYAN = "#2F80A8"
PALE = "#EAF2F8"
PALE2 = "#F4F7FA"
GOLD = "#C58A22"
GREEN = "#2D7D66"
RED = "#A4423E"
INK = "#17212B"
MUTED = "#52606D"
WHITE = "#FFFFFF"
LINE = "#9FB3C8"


def font(size: int, bold: bool = False, mono: bool = False) -> ImageFont.FreeTypeFont:
    candidates = []
    if mono:
        candidates += [
            "/System/Library/Fonts/Supplemental/Courier New Bold.ttf" if bold else "/System/Library/Fonts/Supplemental/Courier New.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
        ]
    else:
        candidates += [
            "/System/Library/Fonts/Supplemental/Arial Bold.ttf" if bold else "/System/Library/Fonts/Supplemental/Arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        ]
    for candidate in candidates:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size=size)
    return ImageFont.load_default(size=size)


F_TITLE = font(48, bold=True)
F_SUB = font(25)
F_BOX = font(27, bold=True)
F_BODY = font(22)
F_SMALL = font(18)
F_MONO = font(21, mono=True)


@dataclass(frozen=True)
class Box:
    x: int
    y: int
    w: int
    h: int
    title: str
    lines: tuple[str, ...] = ()
    fill: str = PALE
    stroke: str = BLUE


def canvas(title: str, subtitle: str) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", (W, H), WHITE)
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, W, 132), fill=NAVY)
    draw.text((70, 34), title, font=F_TITLE, fill=WHITE)
    draw.text((72, 92), subtitle, font=F_SUB, fill="#CFE1F2")
    draw.line((70, H - 52, W - 70, H - 52), fill=LINE, width=2)
    draw.text((70, H - 42), "Java Foundations to Advanced Engineering", font=F_SMALL, fill=MUTED)
    return image, draw


def wrapped(draw: ImageDraw.ImageDraw, text: str, max_width: int, used_font: ImageFont.FreeTypeFont) -> list[str]:
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        probe = word if not current else f"{current} {word}"
        if draw.textlength(probe, font=used_font) <= max_width:
            current = probe
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def box(draw: ImageDraw.ImageDraw, b: Box, radius: int = 18) -> None:
    draw.rounded_rectangle((b.x, b.y, b.x + b.w, b.y + b.h), radius=radius, fill=b.fill, outline=b.stroke, width=4)
    draw.rectangle((b.x, b.y, b.x + b.w, b.y + 54), fill=b.stroke)
    draw.text((b.x + 18, b.y + 12), b.title, font=F_BOX, fill=WHITE)
    y = b.y + 72
    for raw in b.lines:
        for line in wrapped(draw, raw, b.w - 36, F_BODY):
            draw.text((b.x + 18, y), line, font=F_BODY, fill=INK)
            y += 30
        y += 5


def arrow(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int], color: str = BLUE, width: int = 5, label: str | None = None) -> None:
    draw.line((*start, *end), fill=color, width=width)
    import math

    angle = math.atan2(end[1] - start[1], end[0] - start[0])
    length = 18
    for delta in (2.55, -2.55):
        tip = (end[0] + length * math.cos(angle + delta), end[1] + length * math.sin(angle + delta))
        draw.line((*end, *tip), fill=color, width=width)
    if label:
        mx, my = (start[0] + end[0]) // 2, (start[1] + end[1]) // 2
        bbox = draw.textbbox((0, 0), label, font=F_SMALL)
        pad = 6
        draw.rounded_rectangle((mx - (bbox[2] - bbox[0]) // 2 - pad, my - 26, mx + (bbox[2] - bbox[0]) // 2 + pad, my + 2), radius=5, fill=WHITE)
        draw.text((mx - (bbox[2] - bbox[0]) // 2, my - 24), label, font=F_SMALL, fill=color)


def save(image: Image.Image, name: str) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    image.save(OUT / name, format="PNG", optimize=True, dpi=(180, 180))


def compilation_pipeline() -> None:
    image, draw = canvas("Java compilation and execution", "From source text to optimized native instructions")
    xs = [70, 355, 640, 925, 1210, 1495]
    titles = ["Source", "javac", "Class file", "Load + link", "Execution", "CPU"]
    lines = [
        (".java", "Unicode source"),
        ("parse + type-check", "lower to bytecode"),
        ("constant pool", "methods + bytecode"),
        ("load + verify + link", "initialize on active use"),
        ("interpret", "profile + JIT"),
        ("native code", "OS + hardware"),
    ]
    for i, x in enumerate(xs):
        box(draw, Box(x, 335, 235, 270, titles[i], lines[i], PALE if i % 2 == 0 else PALE2, BLUE if i < 4 else GREEN))
        if i < len(xs) - 1:
            arrow(draw, (x + 235, 470), (xs[i + 1] - 12, 470))
    draw.text((90, 700), "Key interview boundary", font=F_BOX, fill=NAVY)
    draw.rounded_rectangle((90, 755, 1710, 910), radius=18, fill="#FFF8E7", outline=GOLD, width=4)
    draw.text((125, 785), "The JVM specification defines an abstract machine and class-file format.", font=F_BODY, fill=INK)
    draw.text((125, 830), "Interpretation, tiered compilation, profiling, and exact machine-code generation are implementation strategies.", font=F_BODY, fill=INK)
    save(image, "01-compilation-pipeline.png")


def jvm_architecture() -> None:
    image, draw = canvas("JVM architecture", "Abstract runtime areas plus a typical HotSpot execution environment")
    box(draw, Box(80, 205, 500, 210, "Class loader subsystem", ("load - verify - prepare - resolve", "initialize classes and interfaces"), "#EAF2F8", BLUE))
    box(draw, Box(650, 205, 500, 210, "Execution engine", ("interpreter and profiling", "C1/C2 JIT, code cache, deoptimization"), "#EDF8F4", GREEN))
    box(draw, Box(1220, 205, 500, 210, "Native boundary", ("JNI and native libraries", "OS threads, files, sockets, clocks"), "#FFF5E5", GOLD))
    draw.text((80, 475), "Runtime data areas", font=F_BOX, fill=NAVY)
    runtime = [
        Box(80, 525, 390, 250, "Heap (shared)", ("objects and arrays", "collector-managed"), PALE, BLUE),
        Box(500, 525, 390, 250, "Method area (shared)", ("class metadata model", "HotSpot: metaspace is native"), PALE2, CYAN),
        Box(920, 525, 390, 250, "Java stacks", ("one per thread", "frames, locals, operand stacks"), "#EDF8F4", GREEN),
        Box(1340, 525, 380, 250, "Per-thread state", ("pc register", "native stack and thread-local data"), "#FFF5E5", GOLD),
    ]
    for b in runtime:
        box(draw, b)
    arrow(draw, (330, 415), (330, 515), label="types")
    arrow(draw, (900, 415), (900, 515), label="executes")
    arrow(draw, (1470, 415), (1470, 515), label="calls")
    draw.rounded_rectangle((110, 850, 1690, 970), radius=16, fill="#F7F8FA", outline=LINE, width=3)
    draw.text((145, 880), "HotSpot also runs GC, compiler, service, signal, and monitoring threads; not every process memory region is a JVMS runtime data area.", font=F_BODY, fill=INK)
    save(image, "02-jvm-architecture.png")


def class_loading() -> None:
    image, draw = canvas("Class lifecycle", "Loading, linking, and initialization are distinct events")
    stages = [
        ("Load", "Find bytes; create Class object", BLUE),
        ("Verify", "Check class-file and bytecode safety", CYAN),
        ("Prepare", "Allocate static storage; defaults", GREEN),
        ("Initialize", "Initialize; invoke <clinit> if present", RED),
    ]
    x = 105
    for i, (title, detail, color) in enumerate(stages):
        box(draw, Box(x, 275, 330, 230, title, tuple(wrapped(draw, detail, 290, F_BODY)), PALE2, color))
        if i < len(stages) - 1:
            arrow(draw, (x + 330, 390), (x + 400, 390), color=color)
        x += 420
    box(draw, Box(690, 570, 420, 175, "Resolve (may be lazy)", ("turn a symbolic reference into a runtime link",), "#FFF5E5", GOLD))
    draw.line((900, 505, 900, 560), fill=GOLD, width=4)
    draw.text((935, 520), "before or after initialization", font=F_SMALL, fill=GOLD)
    draw.rounded_rectangle((95, 800, 1705, 970), radius=20, fill="#F4F7FA", outline=LINE, width=3)
    draw.text((130, 830), "Parent delegation controls lookup; class identity is (binary name, defining loader).", font=F_BOX, fill=NAVY)
    draw.text((130, 880), "Initialization uses a unique per-class initialization lock and follows specified dependency rules.", font=F_BODY, fill=INK)
    draw.text((130, 925), "NoClassDefFoundError can report a missing definition or a class whose earlier initialization failed.", font=F_BODY, fill=INK)
    save(image, "03-class-lifecycle.png")


def runtime_areas() -> None:
    image, draw = canvas("Runtime data areas", "Shared state, per-thread state, and native implementation memory")
    draw.text((90, 195), "Shared by JVM threads", font=F_BOX, fill=NAVY)
    box(draw, Box(90, 245, 770, 260, "Heap", ("objects, arrays, GC regions", "logical generations depend on collector"), PALE, BLUE))
    box(draw, Box(940, 245, 770, 260, "Method area model", ("runtime constant pools and type data", "HotSpot stores class metadata in native metaspace"), PALE2, CYAN))
    draw.text((90, 555), "Specified per-thread areas and optional implementation state", font=F_BOX, fill=NAVY)
    box(draw, Box(70, 615, 390, 285, "Java stack", ("frame per active invocation", "locals + operand stack + frame data"), "#EDF8F4", GREEN))
    box(draw, Box(495, 615, 390, 285, "pc register", ("current JVM instruction location", "undefined during a native method"), "#FFF5E5", GOLD))
    box(draw, Box(920, 615, 390, 285, "Native method stack", ("per thread if the JVM provides one", "native calling state is implementation-specific"), "#FCEFED", RED))
    box(draw, Box(1345, 615, 390, 285, "Optional HotSpot state", ("TLAB may be enabled or absent", "other thread-local runtime structures"), PALE2, CYAN))
    save(image, "04-runtime-data-areas.png")


def object_layout() -> None:
    image, draw = canvas("Object creation and layout", "Language semantics on the left; representative 64-bit HotSpot layout on the right")
    box(draw, Box(80, 220, 700, 575, "Creation semantics", ("1. Allocate storage for the new object", "2. Initialize fields to default values", "3. Invoke the superclass-constructor chain", "4. Run instance initializers", "5. Execute the constructor body"), PALE2, BLUE))
    draw.rounded_rectangle((80, 825, 700, 950), radius=16, fill="#FFF8E7", outline=GOLD, width=4)
    draw.text((110, 850), "Caller responsibility", font=F_BOX, fill=GOLD)
    draw.text((110, 900), "Publish the constructed reference safely when threads share it.", font=F_SMALL, fill=INK)
    draw.text((900, 210), "Representative object memory", font=F_BOX, fill=NAVY)
    regions = [
        ("Mark word", "lock, hash, age, GC state", 130, BLUE),
        ("Class pointer", "metadata type reference", 100, CYAN),
        ("Instance fields", "layout may reorder by implementation rules", 230, GREEN),
        ("Alignment padding", "round size to object alignment", 90, GOLD),
    ]
    y = 285
    for title, detail, height, color in regions:
        draw.rounded_rectangle((900, y, 1680, y + height), radius=12, fill=PALE2, outline=color, width=4)
        draw.text((930, y + 18), title, font=F_BOX, fill=color)
        draw.text((930, y + 61), detail, font=F_BODY, fill=INK)
        y += height + 12
    draw.text((905, 920), "Exact widths depend on VM, flags, architecture, and class shape.", font=F_SMALL, fill=RED)
    save(image, "05-object-layout.png")


def garbage_collection() -> None:
    image, draw = canvas("Reachability and generational collection", "Collectors reclaim unreachable objects; algorithms and heap organization vary")
    roots = [Box(75, 230, 300, 160, "GC roots", ("threads, statics, JNI",), "#FFF5E5", GOLD)]
    objs = [
        Box(500, 205, 270, 150, "A", ("reachable",), "#EDF8F4", GREEN),
        Box(880, 205, 270, 150, "B", ("reachable",), "#EDF8F4", GREEN),
        Box(1260, 205, 270, 150, "C", ("reachable",), "#EDF8F4", GREEN),
        Box(690, 455, 270, 150, "D", ("unreachable cycle",), "#FCEFED", RED),
        Box(1070, 455, 270, 150, "E", ("unreachable cycle",), "#FCEFED", RED),
    ]
    for b in roots + objs:
        box(draw, b)
    arrow(draw, (375, 310), (490, 280), color=GOLD)
    arrow(draw, (770, 280), (870, 280), color=GREEN)
    arrow(draw, (1150, 280), (1250, 280), color=GREEN)
    arrow(draw, (960, 530), (1060, 530), color=RED)
    arrow(draw, (1070, 570), (970, 570), color=RED)
    draw.rounded_rectangle((90, 700, 1710, 930), radius=20, fill=PALE2, outline=LINE, width=3)
    draw.text((125, 735), "Generational heuristic (not universal)", font=F_BOX, fill=NAVY)
    draw.text((125, 785), "Most objects die young, so many collectors separate young and old work; collector/version behavior differs.", font=F_BODY, fill=INK)
    draw.text((125, 835), "G1, ZGC, Shenandoah, Parallel, and Serial differ in generations, barriers, concurrency, compaction, pauses, and throughput.", font=F_BODY, fill=INK)
    draw.text((125, 885), "The illustrated roots are representative rather than an exhaustive implementation list.", font=F_SMALL, fill=MUTED)
    save(image, "06-gc-reachability.png")


def happens_before() -> None:
    image, draw = canvas("Happens-before reasoning", "Visibility follows synchronization edges, not elapsed wall-clock time")
    left = [
        Box(110, 235, 520, 155, "Thread A: ordinary writes", ("payload = 42",), PALE, BLUE),
        Box(110, 500, 520, 155, "Thread A: release action", ("ready = true (volatile write)",), "#EDF8F4", GREEN),
    ]
    right = [
        Box(1170, 500, 520, 155, "Thread B: acquire action", ("subsequent read of the same volatile ready",), "#EDF8F4", GREEN),
        Box(1170, 765, 520, 155, "Thread B: ordinary reads", ("observe payload == 42",), PALE, BLUE),
    ]
    for b in left + right:
        box(draw, b)
    arrow(draw, (370, 390), (370, 490), label="program order")
    arrow(draw, (630, 575), (1160, 575), color=GREEN, label="synchronizes-with")
    arrow(draw, (1430, 655), (1430, 755), label="program order")
    draw.rounded_rectangle((760, 235, 1040, 390), radius=18, fill="#FFF8E7", outline=GOLD, width=4)
    draw.text((795, 270), "Transitivity", font=F_BOX, fill=GOLD)
    draw.text((795, 320), "A writes happen-before", font=F_BODY, fill=INK)
    draw.text((795, 350), "B reads after acquire", font=F_BODY, fill=INK)
    save(image, "07-happens-before.png")


def collections_hierarchy() -> None:
    image, draw = canvas("Collections framework map", "Java 21 contracts: select semantics first, then workload and implementation")
    box(draw, Box(720, 165, 360, 125, "Iterable", ("iteration contract",), PALE2, NAVY))
    box(draw, Box(720, 350, 360, 125, "Collection", ("group of elements",), PALE, BLUE))
    arrow(draw, (900, 290), (900, 340))
    children = [
        Box(60, 615, 380, 220, "SequencedCollection", ("defined encounter order + both ends", "List and Deque in Java 21"), PALE2, BLUE),
        Box(485, 615, 380, 220, "Set / SequencedSet", ("uniqueness; ordered subtype", "HashSet, LinkedHashSet, TreeSet"), "#EDF8F4", GREEN),
        Box(910, 615, 380, 220, "Queue / Deque", ("processing order; Deque extends Queue", "ArrayDeque, PriorityQueue"), "#FFF5E5", GOLD),
        Box(1335, 615, 400, 220, "Map / SequencedMap", ("separate key-to-value hierarchy", "HashMap, LinkedHashMap, TreeMap"), "#FCEFED", RED),
    ]
    for b in children:
        box(draw, b)
    for x in (250, 675, 1100):
        arrow(draw, (900, 475), (x, 605))
    draw.line((1080, 410, 1535, 410, 1535, 605), fill=RED, width=5)
    arrow(draw, (1535, 585), (1535, 605), color=RED)
    draw.text((1200, 370), "Map is not a Collection", font=F_SMALL, fill=RED)
    draw.rounded_rectangle((110, 900, 1690, 980), radius=14, fill="#F7F8FA", outline=LINE, width=3)
    draw.text((145, 925), "Java 21 retrofits first/last/reversed encounter-order APIs through sequenced interfaces.", font=F_BODY, fill=INK)
    save(image, "08-collections-map.png")


def hashmap_put() -> None:
    image, draw = canvas("HashMap put path", "Representative OpenJDK-style mechanics; not a full API contract")
    box(draw, Box(65, 250, 260, 180, "Hash + index", ("mix key hashCode", "select table bin"), PALE2, BLUE))
    box(draw, Box(405, 250, 260, 180, "Empty bin?", ("branch on first node",), PALE2, CYAN))
    box(draw, Box(745, 180, 300, 180, "Install node", ("new mapping",), "#EDF8F4", GREEN))
    box(draw, Box(745, 455, 300, 200, "Search bin", ("first node, list, or tree", "compare hash + key equality"), "#FFF5E5", GOLD))
    box(draw, Box(1125, 385, 280, 180, "Existing key?", ("replace its value", "size is unchanged"), PALE2, NAVY))
    box(draw, Box(1125, 635, 280, 190, "No match", ("append or tree insert", "treeify when eligible"), "#FCEFED", RED))
    box(draw, Box(1480, 520, 260, 220, "New mapping", ("increment size", "resize past threshold"), PALE, BLUE))
    arrow(draw, (325, 340), (395, 340))
    arrow(draw, (665, 315), (735, 270), color=GREEN, label="yes")
    arrow(draw, (535, 430), (735, 540), color=GOLD, label="no")
    arrow(draw, (1045, 540), (1115, 475), color=NAVY)
    arrow(draw, (1045, 580), (1115, 720), color=RED)
    arrow(draw, (1045, 270), (1470, 585), color=GREEN)
    arrow(draw, (1405, 720), (1470, 650), color=RED)
    draw.rounded_rectangle((85, 875, 1715, 985), radius=18, fill="#F7F8FA", outline=LINE, width=3)
    draw.text((120, 900), "Equal keys need equal hash codes; equality-relevant key state must remain stable while stored.", font=F_BODY, fill=INK)
    draw.text((120, 940), "Expected O(1) depends on distribution; collision structure and thresholds are version-sensitive.", font=F_SMALL, fill=MUTED)
    save(image, "09-hashmap-put.png")


def executor_model() -> None:
    image, draw = canvas("ThreadPoolExecutor admission", "Workers, queue capacity, rejection, and Future semantics")
    box(draw, Box(60, 250, 260, 180, "execute / submit", ("submit creates a Future", "execute does not"), PALE, BLUE))
    box(draw, Box(390, 250, 260, 180, "Below core?", ("start a core worker",), PALE2, CYAN))
    box(draw, Box(720, 250, 260, 180, "Queue accepts?", ("enqueue bounded work",), "#FFF5E5", GOLD))
    box(draw, Box(1050, 250, 280, 180, "Below maximum?", ("start an extra worker",), "#EDF8F4", GREEN))
    box(draw, Box(1410, 250, 300, 180, "Reject", ("run rejection policy", "caller-runs can slow admission"), "#FCEFED", RED))
    box(draw, Box(640, 580, 520, 210, "Worker executes task", ("complete normally or exceptionally", "interrupt/cancel is best-effort signalling"), "#EDF8F4", GREEN))
    box(draw, Box(1300, 580, 390, 210, "Future (submit only)", ("observe value or failure", "cancel may request interruption"), PALE2, NAVY))
    arrow(draw, (320, 340), (380, 340))
    arrow(draw, (650, 315), (710, 315), label="no")
    arrow(draw, (980, 315), (1040, 315), label="no")
    arrow(draw, (1330, 315), (1400, 315), label="no")
    arrow(draw, (520, 430), (720, 570), color=GREEN, label="yes")
    arrow(draw, (850, 430), (850, 570), color=GOLD, label="yes: queued")
    arrow(draw, (1190, 430), (1080, 570), color=GREEN, label="yes")
    arrow(draw, (1160, 685), (1290, 685), color=NAVY, label="submit")
    draw.text((90, 865), "A bounded queue enables an admission decision; the executor is not automatic backpressure.", font=F_BOX, fill=NAVY)
    draw.text((90, 915), "Sizing and rejection policy must match workload, latency targets, and downstream capacity.", font=F_BODY, fill=INK)
    save(image, "10-executor-model.png")


def virtual_threads() -> None:
    image, draw = canvas("Virtual thread scheduling", "Java 21/OpenJDK model: many virtual threads mount on fewer carrier threads")
    for row in range(3):
        for col in range(6):
            x = 90 + col * 220
            y = 215 + row * 145
            box(draw, Box(x, y, 175, 100, f"VT {row * 6 + col + 1}", ("task",), PALE2, BLUE), radius=12)
    draw.text((1430, 210), "Scheduler", font=F_BOX, fill=NAVY)
    box(draw, Box(1400, 260, 310, 310, "Scheduler model", ("mount runnable VTs", "unmount around supported blocking", "Java 21 pinning can retain carrier"), "#FFF5E5", GOLD))
    carriers = [Box(250, 730, 320, 140, "Carrier 1", ("platform thread",), "#EDF8F4", GREEN), Box(740, 730, 320, 140, "Carrier 2", ("platform thread",), "#EDF8F4", GREEN), Box(1230, 730, 320, 140, "Carrier N", ("platform thread",), "#EDF8F4", GREEN)]
    for b in carriers:
        box(draw, b)
    for x in (410, 900, 1390):
        arrow(draw, (1555, 570), (x, 720), color=GOLD)
    draw.rounded_rectangle((120, 905, 1680, 985), radius=14, fill="#F7F8FA", outline=LINE, width=3)
    draw.text((150, 930), "JDK 24+: JEP 491 removes nearly all synchronized-monitor pinning; native/class-init cases can remain.", font=F_BODY, fill=INK)
    save(image, "11-virtual-threads.png")


def interview_loop() -> None:
    image, draw = canvas("Coding interview control loop", "A repeatable process makes reasoning visible and reduces avoidable errors")
    steps = [
        ("Clarify", "inputs, outputs, constraints", BLUE),
        ("Model", "examples, invariant, pattern", CYAN),
        ("Choose", "data structure and algorithm", GREEN),
        ("Implement", "small coherent steps", GOLD),
        ("Verify", "dry run, edges, tests", RED),
        ("Analyze", "time, space, trade-offs", NAVY),
    ]
    positions = [(160, 250), (710, 190), (1250, 250), (1250, 650), (710, 740), (160, 650)]
    boxes = []
    for (title, detail, color), (x, y) in zip(steps, positions):
        b = Box(x, y, 380, 170, title, tuple(wrapped(draw, detail, 340, F_BODY)), PALE2, color)
        boxes.append(b)
        box(draw, b)
    # Route arrows between box edges so the reading loop stays visible without
    # crossing labels or body text.
    routes = [
        ((540, 300), (700, 275)),
        ((1090, 275), (1240, 315)),
        ((1440, 420), (1440, 640)),
        ((1240, 735), (1100, 805)),
        ((700, 805), (550, 735)),
        ((350, 640), (350, 430)),
    ]
    for i, (start, end) in enumerate(routes):
        arrow(draw, start, end, color=steps[i][2])
    draw.rounded_rectangle((690, 460, 1110, 625), radius=20, fill="#FFF8E7", outline=GOLD, width=4)
    draw.text((740, 495), "Communicate", font=F_BOX, fill=GOLD)
    draw.text((735, 545), "state decisions and update", font=F_BODY, fill=INK)
    draw.text((760, 578), "when evidence changes", font=F_BODY, fill=INK)
    save(image, "12-interview-loop.png")


def main() -> None:
    for generate in (
        compilation_pipeline,
        jvm_architecture,
        class_loading,
        runtime_areas,
        object_layout,
        garbage_collection,
        happens_before,
        collections_hierarchy,
        hashmap_put,
        executor_model,
        virtual_threads,
        interview_loop,
    ):
        generate()
    print(f"Generated 12 diagrams in {OUT}")


if __name__ == "__main__":
    main()
