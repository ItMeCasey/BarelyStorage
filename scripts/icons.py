"""Anti-aliased white 12x12 icons for the GUI buttons, drawn at 16x and shrunk. The game tints them per theme and state.

Written to assets/barelystorage/textures/gui/icon/<name>.png by make_assets.py.
"""
import math
import os
from PIL import Image, ImageDraw

K = 16
N = 12
W = (255, 255, 255, 255)
DIM = (255, 255, 255, 90)


def canvas():
    im = Image.new("RGBA", (N * K, N * K), (0, 0, 0, 0))
    return im, ImageDraw.Draw(im)


def s(v):
    return v * K


def pts(points):
    return [(s(x), s(y)) for x, y in points]


def line(d, points, w=1.2, fill=W):
    p = pts(points)
    d.line(p, fill=fill, width=int(w * K), joint="curve")
    r = w * K / 2
    for x, y in (p[0], p[-1]):
        d.ellipse([x - r, y - r, x + r, y + r], fill=fill)


def rrect(d, x0, y0, x1, y1, r=1.0, fill=W, outline=None, w=1.0):
    d.rounded_rectangle([s(x0), s(y0), s(x1), s(y1)], radius=s(r), fill=fill, outline=outline, width=int(w * K))


def circle(d, cx, cy, r, fill=W, outline=None, w=1.0):
    d.ellipse([s(cx - r), s(cy - r), s(cx + r), s(cy + r)], fill=fill, outline=outline, width=int(w * K))


def poly(d, points, fill=W):
    d.polygon(pts(points), fill=fill)


def save(im, folder, name):
    im.resize((N, N), Image.LANCZOS).save(os.path.join(folder, name + ".png"))


def height(n):
    im, d = canvas()
    for i, (y, x1) in enumerate(((2.2, 10.5), (5.2, 8.5), (8.2, 6.5))):
        rrect(d, 1.5, y, x1, y + 1.7, 0.85, W if i < n else DIM)
    return im


def moon():
    im, d = canvas()
    circle(d, 6, 6, 4.6)
    circle(d, 8.4, 4.4, 4.0, fill=(0, 0, 0, 0))
    return im


def sun():
    im, d = canvas()
    circle(d, 6, 6, 2.6)
    for a in range(8):
        r0, r1 = 3.9, 5.4
        x, y = math.cos(a * math.pi / 4), math.sin(a * math.pi / 4)
        line(d, [(6 + x * r0, 6 + y * r0), (6 + x * r1, 6 + y * r1)], 1.0)
    return im


def bars():
    im, d = canvas()
    for i, h in enumerate((3.5, 6.5, 9.5)):
        rrect(d, 1.6 + i * 3.4, 10.5 - h, 4.0 + i * 3.4, 10.5, 0.6)
    return im


def cube():
    im, d = canvas()
    top = [(6, 1.2), (10.4, 3.6), (6, 6.0), (1.6, 3.6)]
    left = [(1.6, 4.6), (5.5, 6.9), (5.5, 11), (1.6, 8.6)]
    right = [(10.4, 4.6), (6.5, 6.9), (6.5, 11), (10.4, 8.6)]
    poly(d, top)
    poly(d, left, DIM)
    poly(d, right, (255, 255, 255, 170))
    return im


def letters():
    from PIL import ImageFont
    im, d = canvas()
    font_path = os.path.join(os.path.dirname(__file__), "..", "common", "src", "main", "resources", "assets", "barelystorage", "font", "liberation_sans.ttf")
    big = ImageFont.truetype(font_path, int(6.3 * K))
    d.text((s(0.7), s(-0.4)), "A", font=big, fill=W)
    d.text((s(6.0), s(4.8)), "Z", font=big, fill=(255, 255, 255, 200))
    line(d, [(1.2, 9.8), (4.6, 9.8)], 0.8, DIM)
    return im


def arrow(up):
    im, d = canvas()
    if up:
        line(d, [(6, 10.4), (6, 2.2)], 1.5)
        line(d, [(2.4, 5.6), (6, 2.0), (9.6, 5.6)], 1.5)
    else:
        line(d, [(6, 1.6), (6, 9.8)], 1.5)
        line(d, [(2.4, 6.4), (6, 10.0), (9.6, 6.4)], 1.5)
    return im


def layers():
    im, d = canvas()
    rrect(d, 1.2, 1.2, 7.4, 7.4, 1.2, fill=None, outline=W, w=1.2)
    rrect(d, 4.6, 4.6, 10.8, 10.8, 1.2, fill=(255, 255, 255, 200))
    return im


def tray():
    im, d = canvas()
    rrect(d, 1.2, 1.2, 10.8, 10.8, 1.6, fill=None, outline=W, w=1.2)
    circle(d, 6, 6, 1.9)
    return im


def gear():
    im, d = canvas()
    for a in range(8):
        ang = a * math.pi / 4
        cx, cy = 6 + math.cos(ang) * 4.6, 6 + math.sin(ang) * 4.6
        circle(d, cx, cy, 1.25)
    circle(d, 6, 6, 4.0)
    circle(d, 6, 6, 1.6, fill=(0, 0, 0, 0))
    return im


def grid():
    im, d = canvas()
    for r in range(3):
        for c in range(3):
            rrect(d, 1.4 + c * 3.5, 1.4 + r * 3.5, 3.6 + c * 3.5, 3.6 + r * 3.5, 0.5, W if (r + c) % 2 == 0 else (255, 255, 255, 170))
    return im


def tag():
    im, d = canvas()
    poly(d, [(1.2, 2.4), (6.6, 2.4), (10.8, 6.6), (6.6, 10.8), (1.2, 5.6)])
    poly(d, [(1.2, 2.4), (6.6, 2.4), (10.8, 6.6), (6.6, 10.8), (1.2, 5.6)])
    circle(d, 3.6, 4.2, 0.9, fill=(0, 0, 0, 0))
    return im


def check():
    im, d = canvas()
    line(d, [(2.0, 6.4), (4.8, 9.2), (10.2, 2.8)], 1.7)
    return im


def trash():
    im, d = canvas()
    rrect(d, 1.6, 2.4, 10.4, 3.5, 0.5)
    rrect(d, 4.2, 1.0, 7.8, 2.6, 0.6)
    rrect(d, 2.6, 4.2, 9.4, 11.0, 1.0, fill=None, outline=W, w=1.2)
    line(d, [(5, 5.6), (5, 9.4)], 0.9)
    line(d, [(7, 5.6), (7, 9.4)], 0.9)
    return im


def stepper(plus, dots):
    im, d = canvas()
    cy = 4.2
    line(d, [(3.0, cy), (9.0, cy)], 1.5)
    if plus:
        line(d, [(6.0, cy - 3.0), (6.0, cy + 3.0)], 1.5)
    for i in range(dots):
        circle(d, 6 + (i - (dots - 1) / 2) * 2.6, 10.0, 0.95)
    return im


def play():
    im, d = canvas()
    poly(d, [(3.2, 1.6), (10.4, 6), (3.2, 10.4)])
    return im


def cross():
    im, d = canvas()
    line(d, [(2.4, 2.4), (9.6, 9.6)], 1.6)
    line(d, [(9.6, 2.4), (2.4, 9.6)], 1.6)
    return im


def chevrons(up, count):
    im, d = canvas()
    for i in range(count):
        y = 2.6 + i * 3.2 if up else 1.8 + i * 3.2
        if up:
            line(d, [(2.4, y + 3.2), (6, y), (9.6, y + 3.2)], 1.5)
        else:
            line(d, [(2.4, y), (6, y + 3.2), (9.6, y)], 1.5)
    return im


def equals():
    im, d = canvas()
    line(d, [(2.4, 4.4), (9.6, 4.4)], 1.5)
    line(d, [(2.4, 7.6), (9.6, 7.6)], 1.5)
    return im


def write_all(folder):
    os.makedirs(folder, exist_ok=True)
    icons = {
        "height_0": height(1), "height_1": height(2), "height_2": height(3),
        "theme_0": moon(), "theme_1": sun(),
        "sort_0": bars(), "sort_1": cube(), "sort_2": letters(),
        "order_0": arrow(True), "order_1": arrow(False),
        "view_0": layers(), "view_1": tray(), "view_2": gear(),
        "tags": tag(), "mode_0": grid(), "mode_1": gear(), "encode": check(), "clear": trash(),
        "minus_1": stepper(False, 1), "minus_10": stepper(False, 2), "minus_100": stepper(False, 3),
        "plus_1": stepper(True, 1), "plus_10": stepper(True, 2), "plus_100": stepper(True, 3),
        "start": play(), "cancel": cross(),
        "priority_0": chevrons(True, 2), "priority_1": equals(), "priority_2": chevrons(False, 2),
    }
    for name, im in icons.items():
        save(im, folder, name)
    return list(icons)
