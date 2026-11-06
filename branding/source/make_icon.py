"""Draws the BarelyStorage icon as 32x32 pixel art and writes it at several sizes.

Run from the repository root: python branding/source/make_icon.py
Writes branding/icon/icon_<size>.png, branding/icon/icon.svg and barelystorage_logo.png (for the jar).

The picture: a server rack with four drive bays on the studio's night tile, one drive lit green.
"""
import math
from pathlib import Path

from PIL import Image

N = 32
OUT = Path(__file__).resolve().parent.parent / "icon"

BG = (24, 22, 38)
BG_EDGE = (46, 42, 70)
BG_TOP = (58, 54, 88)
GOLD = (242, 179, 61)
GOLD_HI = (255, 226, 140)
GOLD_LO = (178, 116, 32)
FRAME = (62, 66, 92)
FRAME_HI = (104, 110, 146)
FRAME_LO = (34, 36, 54)
BAY = (14, 14, 26)
DRIVE = (44, 50, 76)
DRIVE_HI = (74, 82, 118)
LED_ON = (92, 232, 140)
LED_AMBER = (250, 168, 52)
LED_OFF = (60, 64, 92)
TEAL = (70, 214, 224)

px = [[None] * N for _ in range(N)]


def put(x, y, c):
    if 0 <= x < N and 0 <= y < N:
        px[y][x] = c


def rect(x0, y0, x1, y1, c):
    for yy in range(y0, y1 + 1):
        for xx in range(x0, x1 + 1):
            put(xx, yy, c)


R = 4
for y in range(N):
    for x in range(N):
        cx = min(max(x, R - 0.5), N - R - 0.5)
        cy = min(max(y, R - 0.5), N - R - 0.5)
        if math.hypot(x - cx, y - cy) <= R:
            px[y][x] = BG
for y in range(N):
    for x in range(N):
        if px[y][x] is None:
            continue
        edge = any(not (0 <= x + dx < N and 0 <= y + dy < N) or px[y + dy][x + dx] is None for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge:
            px[y][x] = BG_TOP if y < N // 2 else BG_EDGE

# Rack frame with a bevel.
rect(6, 4, 25, 27, FRAME)
rect(6, 4, 25, 4, FRAME_HI)
rect(6, 4, 6, 27, FRAME_HI)
rect(25, 5, 25, 27, FRAME_LO)
rect(7, 27, 25, 27, FRAME_LO)
rect(8, 6, 23, 25, BAY)

# Four drive bays, the top one lit.
for i in range(4):
    y0 = 7 + i * 5
    rect(9, y0, 22, y0 + 3, DRIVE)
    rect(9, y0, 22, y0, DRIVE_HI)
    rect(11, y0 + 2, 17, y0 + 2, BAY)
    put(20, y0 + 1, LED_ON if i == 0 else LED_AMBER if i == 1 else LED_OFF)
    put(20, y0 + 2, LED_ON if i == 0 else LED_OFF)

# Gold foot plate and a teal data glint on the top bay.
rect(5, 28, 26, 28, GOLD_LO)
rect(5, 28, 26, 28, GOLD)
rect(5, 28, 5, 28, GOLD_HI)
for x in range(11, 18):
    put(x, 9, TEAL if x % 2 else (40, 150, 160))

img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
for y in range(N):
    for x in range(N):
        if px[y][x] is not None:
            img.putpixel((x, y), px[y][x] + (255,))

OUT.mkdir(parents=True, exist_ok=True)
for size in (32, 64, 128, 256, 512, 1024):
    img.resize((size, size), Image.NEAREST).save(OUT / f"icon_{size}.png", optimize=True)
img.resize((512, 512), Image.NEAREST).save(OUT / "barelystorage_logo.png", optimize=True)

rects = []
for y in range(N):
    for x in range(N):
        c = px[y][x]
        if c is not None:
            rects.append(f'<rect x="{x}" y="{y}" width="1.02" height="1.02" fill="#{c[0]:02x}{c[1]:02x}{c[2]:02x}"/>')
(OUT / "icon.svg").write_text(
    f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {N} {N}" shape-rendering="crispEdges">' + "".join(rects) + "</svg>\n",
    encoding="utf-8",
)
print("wrote", OUT)
