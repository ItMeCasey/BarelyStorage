"""Original pixel textures for BarelyStorage, drawn with Pillow. Run through make_assets.py.

Everything is drawn at 32x32 (blocks and items) in the studio palette: night metal, gold accents, LED green, code blue.
"""
from PIL import Image, ImageDraw
import random

NIGHT = (15, 14, 26)
METAL_D = (30, 31, 44)
METAL = (44, 46, 62)
METAL_L = (66, 69, 92)
METAL_H = (98, 102, 132)
EDGE = (46, 42, 70)
GOLD = (242, 179, 61)
GOLD_L = (255, 226, 140)
GOLD_D = (178, 116, 32)
GREEN = (91, 227, 138)
GREEN_D = (22, 70, 40)
RED = (255, 94, 94)
RED_D = (80, 22, 26)
BLUE = (122, 168, 255)
BLUE_D = (28, 44, 84)
CYAN = (90, 240, 255)
AMBER = (255, 190, 70)
PURPLE = (150, 90, 220)


def img(w=32, h=32, color=(0, 0, 0, 0)):
    return Image.new("RGBA", (w, h), color)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def brushed(w, h, base, seed=1, spread=7):
    """Metal with fine horizontal brush lines and a soft top-to-bottom gradient."""
    rnd = random.Random(seed)
    im = img(w, h)
    px = im.load()
    for y in range(h):
        line = rnd.randint(-spread, spread)
        shade = int((0.5 - y / h) * 8)
        for x in range(w):
            n = rnd.randint(-2, 2)
            c = tuple(max(0, min(255, base[i] + line + shade + n)) for i in range(3))
            px[x, y] = c + (255,)
    return im


def bevel(im, light=METAL_H, dark=NIGHT, inset=0):
    d = ImageDraw.Draw(im)
    w, h = im.size
    d.line([(inset, inset), (w - 1 - inset, inset)], fill=light + (255,))
    d.line([(inset, inset), (inset, h - 1 - inset)], fill=light + (255,))
    d.line([(inset, h - 1 - inset), (w - 1 - inset, h - 1 - inset)], fill=dark + (255,))
    d.line([(w - 1 - inset, inset), (w - 1 - inset, h - 1 - inset)], fill=dark + (255,))


def led(im, x, y, color, on=True, w=2, h=1):
    d = ImageDraw.Draw(im)
    if on:
        d.rectangle([x - 1, y - 1, x + w, y + h], fill=mix(color, METAL_D, 0.65) + (255,))
        d.rectangle([x, y, x + w - 1, y + h - 1], fill=color + (255,))
    else:
        d.rectangle([x, y, x + w - 1, y + h - 1], fill=mix(color, NIGHT, 0.82) + (255,))


def screw(im, x, y):
    d = ImageDraw.Draw(im)
    d.point((x, y), fill=METAL_H + (255,))
    d.point((x + 1, y + 1), fill=NIGHT + (255,))


def rack_front(on):
    im = brushed(32, 32, METAL, 3)
    d = ImageDraw.Draw(im)
    # outer frame
    d.rectangle([0, 0, 31, 31], outline=METAL_D + (255,))
    bevel(im, inset=1)
    # top vent slits
    for i in range(6):
        d.line([(6 + i * 4, 3), (8 + i * 4, 3)], fill=NIGHT + (255,))
        d.line([(6 + i * 4, 4), (8 + i * 4, 4)], fill=METAL_D + (255,))
    # four drive trays
    for t in range(4):
        y = 7 + t * 5
        d.rectangle([3, y, 28, y + 3], fill=NIGHT + (255,))
        d.rectangle([4, y + 1, 21, y + 2], fill=METAL_D + (255,))
        d.line([(4, y + 1), (21, y + 1)], fill=METAL_L + (255,))
        d.rectangle([22, y + 1, 23, y + 2], fill=METAL_H + (255,))
        led(im, 25, y + 1, GREEN if on else GREEN, on)
        led(im, 27, y + 1, AMBER if (t % 2) else CYAN, on and t != 2)
    # gold nameplate
    d.rectangle([3, 28, 14, 29], fill=GOLD_D + (255,))
    d.line([(3, 28), (14, 28)], fill=GOLD + (255,))
    led(im, 24, 28, GREEN if on else RED, True, 3, 1)
    for p in ((2, 2), (29, 2), (2, 29), (29, 29)):
        screw(im, *p)
    return im


def rack_side():
    im = brushed(32, 32, METAL_D, 5)
    d = ImageDraw.Draw(im)
    bevel(im, METAL_L, NIGHT, 0)
    for r in range(4):
        for c in range(7):
            x = 5 + c * 3
            y = 6 + r * 6
            d.line([(x, y), (x, y + 3)], fill=NIGHT + (255,))
            d.line([(x + 1, y), (x + 1, y + 3)], fill=METAL + (255,))
    d.line([(2, 2), (29, 2)], fill=GOLD_D + (255,))
    return im


def rack_top():
    im = brushed(32, 32, METAL_D, 6)
    d = ImageDraw.Draw(im)
    bevel(im, METAL_L, NIGHT, 0)
    for y in range(5, 28, 4):
        for x in range(5, 28, 4):
            d.rectangle([x, y, x + 1, y + 1], fill=NIGHT + (255,))
            d.point((x, y), fill=METAL_L + (255,))
    for p in ((2, 2), (29, 2), (2, 29), (29, 29)):
        screw(im, *p)
    return im


def terminal_front(kind):
    im = brushed(32, 32, METAL, 8)
    d = ImageDraw.Draw(im)
    bevel(im, inset=0)
    d.rectangle([3, 3, 28, 24], fill=NIGHT + (255,))
    d.rectangle([4, 4, 27, 23], fill=(8, 22, 34, 255))
    if kind == "storage":
        for r in range(4):
            for c in range(7):
                x = 6 + c * 3
                y = 6 + r * 4
                d.rectangle([x, y, x + 1, y + 2], fill=mix(BLUE, NIGHT, 0.55 + 0.1 * ((r + c) % 3)) + (255,))
        d.line([(5, 21), (26, 21)], fill=CYAN + (255,))
        d.rectangle([5, 5, 11, 5], fill=CYAN + (255,))
    elif kind == "crafting":
        for r in range(3):
            for c in range(3):
                x = 6 + c * 4
                y = 7 + r * 4
                d.rectangle([x, y, x + 2, y + 2], outline=mix(CYAN, NIGHT, 0.4) + (255,))
        d.line([(19, 12), (22, 12)], fill=GOLD + (255,))
        d.line([(21, 10), (22, 12), (21, 14)], fill=GOLD + (255,))
        d.rectangle([23, 10, 26, 14], outline=GOLD + (255,))
    elif kind == "queue":
        for r in range(4):
            y = 6 + r * 4
            d.rectangle([6, y, 8, y + 2], fill=mix(BLUE, NIGHT, 0.3) + (255,))
            d.line([(10, y + 1), (14 + (r * 5) % 9, y + 1)], fill=mix(CYAN, NIGHT, 0.35) + (255,))
            d.rectangle([10, y + 2, 25, y + 2], fill=mix(METAL_L, NIGHT, 0.5) + (255,))
            d.rectangle([10, y + 2, 12 + (r * 7) % 12, y + 2], fill=GREEN + (255,))
    else:
        for r in range(3):
            for c in range(3):
                x = 6 + c * 5
                y = 6 + r * 5
                d.rectangle([x, y, x + 3, y + 3], outline=mix(GOLD, NIGHT, 0.4) + (255,))
        d.line([(22, 12), (25, 12)], fill=GOLD + (255,))
        d.line([(24, 10), (25, 12), (24, 14)], fill=GOLD + (255,))
        d.rectangle([22, 17, 26, 21], outline=GOLD + (255,))
    # glass glare
    for i in range(6):
        d.point((5 + i, 5 + i), fill=(255, 255, 255, 40))
    # keys / status
    led(im, 5, 27, GREEN, True, 3, 1)
    led(im, 10, 27, AMBER, kind == "pattern", 3, 1)
    d.rectangle([18, 26, 27, 28], fill=METAL_D + (255,))
    d.line([(18, 26), (27, 26)], fill=METAL_L + (255,))
    return im


def terminal_side():
    im = brushed(32, 32, METAL_D, 9)
    bevel(im, METAL_L, NIGHT, 0)
    d = ImageDraw.Draw(im)
    d.rectangle([6, 6, 25, 25], outline=METAL + (255,))
    d.line([(6, 15), (25, 15)], fill=GOLD_D + (255,))
    return im


def assembler_front(active):
    im = brushed(32, 32, METAL, 12)
    d = ImageDraw.Draw(im)
    bevel(im, inset=0)
    d.rectangle([4, 4, 27, 20], fill=NIGHT + (255,))
    d.rectangle([5, 5, 26, 19], fill=(14, 16, 26, 255))
    c = BLUE if active else mix(BLUE, NIGHT, 0.7)
    # robot arm
    d.line([(8, 17), (13, 10)], fill=c + (255,), width=2)
    d.line([(13, 10), (20, 12)], fill=c + (255,), width=2)
    d.rectangle([19, 12, 22, 15], outline=GOLD + (255,) if active else GOLD_D + (255,))
    d.rectangle([7, 17, 10, 19], fill=METAL_H + (255,))
    if active:
        for i in range(3):
            d.point((22 + i, 11 - i), fill=GOLD_L + (255,))
    # status strip
    d.rectangle([4, 23, 27, 27], fill=METAL_D + (255,))
    for i in range(6):
        led(im, 6 + i * 4, 25, GREEN if (active and i % 2 == 0) else AMBER, active or i == 0)
    return im


def assembler_side():
    im = brushed(32, 32, METAL_D, 13)
    bevel(im, METAL_L, NIGHT, 0)
    d = ImageDraw.Draw(im)
    for y in (6, 12, 18, 24):
        d.line([(4, y), (27, y)], fill=NIGHT + (255,))
        d.line([(4, y + 1), (27, y + 1)], fill=METAL + (255,))
    return im


def assembler_top():
    im = brushed(32, 32, METAL_D, 14)
    bevel(im, METAL_L, NIGHT, 0)
    d = ImageDraw.Draw(im)
    d.rectangle([8, 8, 23, 23], fill=NIGHT + (255,))
    d.rectangle([9, 9, 22, 22], outline=GOLD_D + (255,))
    d.rectangle([13, 13, 18, 18], fill=GOLD_D + (255,))
    d.rectangle([14, 14, 17, 17], fill=GOLD + (255,))
    return im


def cable_tex():
    """Plain dark grey with very slight noise."""
    rnd = random.Random(21)
    im = img(16, 16)
    px = im.load()
    for y in range(16):
        for x in range(16):
            n = rnd.randint(-2, 2)
            px[x, y] = (54 + n, 55 + n, 60 + n, 255)
    return im


def node_tex(accent):
    im = brushed(16, 16, METAL, 31, 5)
    bevel(im, METAL_H, NIGHT, 0)
    d = ImageDraw.Draw(im)
    d.rectangle([3, 3, 12, 12], outline=accent + (255,))
    d.rectangle([5, 5, 10, 10], fill=mix(accent, NIGHT, 0.5) + (255,))
    d.rectangle([6, 6, 9, 9], fill=accent + (255,))
    screw(im, 1, 1)
    screw(im, 13, 13)
    return im


# ---- Items ----

def cartridge(body, label):
    im = img()
    d = ImageDraw.Draw(im)
    d.rectangle([7, 2, 24, 29], fill=NIGHT + (255,))
    for y in range(3, 29):
        t = (y - 3) / 26
        d.line([(8, y), (23, y)], fill=mix(mix(body, (255, 255, 255), 0.25), mix(body, NIGHT, 0.45), t) + (255,))
    d.line([(8, 3), (23, 3)], fill=mix(body, (255, 255, 255), 0.6) + (255,))
    d.line([(8, 3), (8, 28)], fill=mix(body, (255, 255, 255), 0.35) + (255,))
    # label
    d.rectangle([10, 6, 21, 13], fill=(18, 18, 30, 255))
    d.rectangle([11, 7, 20, 12], fill=mix(label, NIGHT, 0.55) + (255,))
    for i in range(4):
        d.line([(12, 8 + i), (12 + 3 + (i % 2) * 3, 8 + i)], fill=label + (255,))
    # grip ridges
    for y in range(16, 22):
        d.line([(10, y), (21, y)], fill=mix(body, NIGHT, 0.55) + (255,) if y % 2 else mix(body, (255, 255, 255), 0.12) + (255,))
    led(im, 10, 24, label, True, 3, 1)
    # contact pins
    d.rectangle([10, 27, 21, 29], fill=GOLD_D + (255,))
    for x in range(11, 21, 2):
        d.line([(x, 27), (x, 29)], fill=GOLD_L + (255,))
    return im


def board(accent):
    im = img()
    d = ImageDraw.Draw(im)
    d.rectangle([3, 6, 28, 26], fill=(12, 64, 44, 255))
    d.rectangle([4, 7, 27, 25], outline=(30, 110, 76, 255))
    for y in (9, 13, 17, 21, 24):
        d.line([(5, y), (8 + (y % 3) * 5, y)], fill=(70, 190, 130, 255))
    d.rectangle([3, 26, 28, 28], fill=GOLD_D + (255,))
    for x in range(4, 28, 2):
        d.line([(x, 26), (x, 28)], fill=GOLD_L + (255,))
    return im, d


def basic_core():
    im, d = board(GOLD)
    d.rectangle([10, 10, 21, 21], fill=NIGHT + (255,))
    d.rectangle([11, 11, 20, 20], fill=mix(GOLD, NIGHT, 0.35) + (255,))
    d.rectangle([13, 13, 18, 18], fill=GOLD + (255,))
    d.rectangle([14, 14, 16, 16], fill=GOLD_L + (255,))
    for i in range(4):
        d.line([(12 + i * 2, 8), (12 + i * 2, 9)], fill=METAL_H + (255,))
        d.line([(12 + i * 2, 22), (12 + i * 2, 23)], fill=METAL_H + (255,))
    return im


def ram_blade():
    im, d = board(BLUE)
    for i in range(4):
        x = 6 + i * 6
        d.rectangle([x, 9, x + 4, 22], fill=NIGHT + (255,))
        d.rectangle([x + 1, 10, x + 3, 21], fill=mix(BLUE, NIGHT, 0.55) + (255,))
        d.rectangle([x + 1, 12, x + 3, 13], fill=METAL_H + (255,))
        d.rectangle([x + 1, 16, x + 3, 17], fill=METAL_H + (255,))
        d.point((x + 2, 19), fill=CYAN + (255,))
    return im


def quantum_core():
    im, d = board(CYAN)
    d.rectangle([9, 9, 22, 22], fill=NIGHT + (255,))
    d.ellipse([10, 10, 21, 21], fill=mix(PURPLE, NIGHT, 0.4) + (255,))
    d.ellipse([12, 12, 19, 19], fill=PURPLE + (255,))
    d.ellipse([14, 14, 17, 17], fill=CYAN + (255,))
    d.point((15, 15), fill=(255, 255, 255, 255))
    for a in ((10, 7), (21, 7), (10, 24), (21, 24)):
        led(im, a[0], a[1], CYAN, True, 1, 1)
    return im


def pattern(accent):
    im = img()
    d = ImageDraw.Draw(im)
    d.rectangle([4, 4, 27, 27], fill=NIGHT + (255,))
    for y in range(5, 27):
        d.line([(5, y), (26, y)], fill=mix(METAL_L, METAL_D, (y - 5) / 22) + (255,))
    d.rectangle([4, 4, 27, 27], outline=NIGHT + (255,))
    # circuit traces
    c = accent
    d.line([(8, 22), (8, 14), (14, 14), (14, 9)], fill=c + (255,))
    d.line([(18, 22), (18, 17), (23, 17)], fill=c + (255,))
    d.line([(12, 22), (12, 18), (16, 18)], fill=mix(c, NIGHT, 0.4) + (255,))
    for p in ((14, 9), (23, 17), (8, 22), (18, 22)):
        d.rectangle([p[0] - 1, p[1] - 1, p[0] + 1, p[1] + 1], outline=GOLD + (255,))
        d.point(p, fill=GOLD_L + (255,))
    d.rectangle([20, 7, 24, 11], fill=NIGHT + (255,))
    d.rectangle([21, 8, 23, 10], fill=mix(c, NIGHT, 0.3) + (255,))
    return im


def upgrade_card(accent, glyph):
    im = img()
    d = ImageDraw.Draw(im)
    d.rectangle([5, 3, 26, 28], fill=NIGHT + (255,))
    for y in range(4, 28):
        d.line([(6, y), (25, y)], fill=mix(METAL_L, METAL_D, (y - 4) / 24) + (255,))
    d.rectangle([7, 5, 24, 20], fill=mix(accent, NIGHT, 0.82) + (255,))
    c = accent + (255,)
    if glyph == "speed":
        d.polygon([(17, 6), (10, 14), (15, 14), (13, 19), (21, 11), (16, 11)], fill=c)
    elif glyph == "capacity":
        for r in range(3):
            for k in range(3):
                d.rectangle([10 + k * 5, 7 + r * 4, 12 + k * 5, 9 + r * 4], fill=c)
    elif glyph == "efficiency":
        d.rectangle([10, 8, 21, 17], outline=c)
        d.rectangle([12, 10, 17, 15], fill=c)
        d.rectangle([22, 11, 23, 14], fill=c)
    else:
        d.polygon([(9, 7), (22, 7), (17, 13), (17, 18), (14, 18), (14, 13)], fill=c)
    d.rectangle([7, 23, 24, 26], fill=GOLD_D + (255,))
    for x in range(8, 24, 2):
        d.line([(x, 23), (x, 26)], fill=GOLD_L + (255,))
    led(im, 8, 21, accent, True, 2, 1)
    return im


def write_all(tex_dir_block, tex_dir_item):
    import os
    os.makedirs(tex_dir_block, exist_ok=True)
    os.makedirs(tex_dir_item, exist_ok=True)
    blocks = {
        "server_rack_front_off": rack_front(False),
        "server_rack_front_on": rack_front(True),
        "server_rack_side": rack_side(),
        "server_rack_top": rack_top(),
        "storage_terminal_front": terminal_front("storage"),
        "pattern_terminal_front": terminal_front("pattern"),
        "crafting_terminal_front": terminal_front("crafting"),
        "queue_terminal_front": terminal_front("queue"),
        "terminal_side": terminal_side(),
        "assembler_front_idle": assembler_front(False),
        "assembler_front_active": assembler_front(True),
        "assembler_side": assembler_side(),
        "assembler_top": assembler_top(),
        "rack_bus_cable": cable_tex(),
        "storage_bus": node_tex(BLUE),
        "interface_node": node_tex(GOLD),
        "import_node": node_tex(GREEN),
        "export_node": node_tex(RED),
    }
    for name, im in blocks.items():
        im.save(os.path.join(tex_dir_block, name + ".png"))
    items = {
        "upgrade_speed": upgrade_card(GOLD, "speed"),
        "upgrade_capacity": upgrade_card(BLUE, "capacity"),
        "upgrade_efficiency": upgrade_card(GREEN, "efficiency"),
        "upgrade_filter": upgrade_card(PURPLE, "filter"),
        "cartridge_iron": cartridge((150, 154, 168), (200, 205, 220)),
        "cartridge_steel": cartridge((96, 128, 170), BLUE),
        "cartridge_gold": cartridge((220, 168, 56), GOLD_L),
        "cartridge_obsidian": cartridge((56, 40, 84), PURPLE),
        "cartridge_quantum": cartridge((30, 60, 90), CYAN),
        "basic_core": basic_core(),
        "ram_blade": ram_blade(),
        "quantum_core": quantum_core(),
        "blank_pattern": pattern((140, 144, 168)),
        "crafting_pattern": pattern(BLUE),
        "processing_pattern": pattern(AMBER),
    }
    for name, im in items.items():
        im.save(os.path.join(tex_dir_item, name + ".png"))
    return list(blocks), list(items)
