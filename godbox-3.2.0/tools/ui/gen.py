#!/usr/bin/env python3
"""Генератор GUI-атласа GodBox: рамки, кнопки, значки и гербы королевств.
Запуск: python3 tools/ui/gen.py  (нужен Pillow). Пишет textures/gui/ui.png и контрольные листы в /tmp/verify.
Раскладка (координаты) продублирована в client/Art.java — менять только вместе."""
import os, random
from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src/main/resources/assets/godbox/textures/gui/ui.png")
VERIFY = "/tmp/verify"
ATLAS = 256


def c(h, a=255):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)


# палитра: тёмный камень, бронза, золото
ST0, ST1, ST2, ST3, ST4 = c(0x0c0c12), c(0x16161e), c(0x20202a), c(0x2c2c38), c(0x3a3a48)
BR0, BR1, BR2, BR3 = c(0x3a2410), c(0x6b4420), c(0x9a6a30), c(0xc89448)
GD0, GD1, GD2 = c(0xb8860b), c(0xffd700), c(0xffe680)
CLEAR = (0, 0, 0, 0)

img = Image.new("RGBA", (ATLAS, ATLAS), CLEAR)
px = img.load()


def put(x, y, col):
    px[x, y] = col


def rect(x, y, w, h, col):
    for j in range(y, y + h):
        for i in range(x, x + w):
            put(i, j, col)


def frame(x, y, w, h, outer, hi, lo, fill):
    """Рамка со срезанными углами: контур, свет сверху-слева, тень снизу-справа."""
    rect(x, y, w, h, fill)
    for i in range(w):
        put(x + i, y, outer)
        put(x + i, y + h - 1, outer)
    for j in range(h):
        put(x, y + j, outer)
        put(x + w - 1, y + j, outer)
    for i in range(1, w - 1):
        put(x + i, y + 1, hi)
        put(x + i, y + h - 2, lo)
    for j in range(1, h - 1):
        put(x + 1, y + j, hi)
        put(x + w - 2, y + j, lo)
    for (cx, cy) in ((x, y), (x + w - 1, y), (x, y + h - 1), (x + w - 1, y + h - 1)):
        put(cx, cy, CLEAR)


# ---------- панель 24x24, рамка 6 ----------
def panel(x, y):
    S = 24
    frame(x, y, S, S, ST0, BR2, BR0, ST2)
    for i in range(2, S - 2):
        put(x + i, y + 2, BR1)
        put(x + i, y + S - 3, BR0)
        put(x + 2, y + i, BR1)
        put(x + S - 3, y + i, BR0)
    rect(x + 3, y + 3, S - 6, S - 6, ST1)
    for i in range(3, S - 3):
        put(x + i, y + 3, ST0)
        put(x + 3, y + i, ST0)
        put(x + i, y + S - 4, ST3)
        put(x + S - 4, y + i, ST3)
    for (cx, cy, dx, dy) in ((x + 1, y + 1, 1, 1), (x + S - 2, y + 1, -1, 1),
                             (x + 1, y + S - 2, 1, -1), (x + S - 2, y + S - 2, -1, -1)):
        put(cx, cy, GD1)
        put(cx + dx, cy, GD0)
        put(cx, cy + dy, GD0)
    rect(x + 6, y + 6, S - 12, S - 12, ST1)


panel(0, 0)


# ---------- вставка (слот) 16x16, рамка 3 ----------
def inset(x, y, fill, hi=ST3):
    frame(x, y, 16, 16, ST0, hi, ST0, fill)
    for i in range(1, 15):
        put(x + i, y + 1, ST0)
        put(x + 1, y + i, ST0)
        put(x + i, y + 14, hi)
        put(x + 14, y + i, hi)
    for (cx, cy) in ((x, y), (x + 15, y), (x, y + 15), (x + 15, y + 15)):
        put(cx, cy, CLEAR)
    rect(x + 3, y + 3, 10, 10, fill)


inset(24, 0, ST0)


# ---------- подсказка 16x16, рамка 4 ----------
def tooltip(x, y):
    bg = c(0x100c18)
    frame(x, y, 16, 16, c(0x08060c), GD0, c(0x6a4c08), bg)
    rect(x + 2, y + 2, 12, 12, bg)
    for i in range(2, 14):
        put(x + i, y + 2, c(0x2a2040))
        put(x + 2, y + i, c(0x2a2040))
    put(x + 1, y + 1, GD1)


tooltip(40, 0)


# ---------- прокрутка: дорожка и ползунок 6x16, края по 2 ----------
def track(x, y):
    rect(x, y, 6, 16, ST1)
    for j in range(16):
        put(x, y + j, ST0)
        put(x + 5, y + j, ST3)
    for i in range(6):
        put(x + i, y, ST0)
        put(x + i, y + 15, ST3)
    put(x, y, CLEAR)
    put(x + 5, y, CLEAR)
    put(x, y + 15, CLEAR)
    put(x + 5, y + 15, CLEAR)


track(56, 0)


def thumb(x, y):
    rect(x, y, 6, 16, BR1)
    for j in range(16):
        put(x, y + j, BR0)
        put(x + 5, y + j, BR0)
        put(x + 1, y + j, BR3)
        put(x + 4, y + j, BR2)
    for i in range(6):
        put(x + i, y, BR0)
        put(x + i, y + 15, BR0)
    for (a, b, col) in ((2, 7, GD1), (3, 7, GD1), (2, 8, GD0), (3, 8, GD0)):
        put(x + a, y + b, col)
    for (cx, cy) in ((x, y), (x + 5, y), (x, y + 15), (x + 5, y + 15)):
        put(cx, cy, CLEAR)


thumb(62, 0)


# ---------- лента заголовка 32x16, края по 8 по горизонтали ----------
def ribbon(x, y):
    W, H = 32, 16
    for i in range(3, W - 3):
        for j in range(1, H - 1):
            put(x + i, y + j, BR1)
        put(x + i, y + 1, ST0)
        put(x + i, y + H - 2, ST0)
        put(x + i, y + 2, BR3)
        put(x + i, y + 3, BR2)
        put(x + i, y + H - 3, BR0)
        put(x + i, y + 7, GD1 if i % 2 else GD0)
        put(x + i, y + 8, BR0)
    # хвосты ленты с вырезом
    for side in (0, 1):
        for k in range(3):
            ex = x + k if side == 0 else x + W - 1 - k
            for j in range(2, H - 2):
                cut = abs(j - 7.5) <= (3 - k)
                if cut:
                    continue
                col = ST0 if k == 0 else (BR2 if j < 8 else BR0) if k == 1 else BR1
                put(ex, y + j, col)


ribbon(72, 0)


# ---------- кнопки 16x16, рамка 3 ----------
def button(x, y, state):
    hi, hi2, lo, fill = {
        "normal": (ST3, ST4, ST2, c(0x30303e)),
        "hover": (ST4, c(0x5a5a70), ST3, c(0x40405a)),
        "active": (BR2, GD1, BR0, c(0x4a3418)),
        "disabled": (ST1, ST2, ST0, ST1),
    }[state]
    frame(x, y, 16, 16, ST0, hi2, lo, fill)
    rect(x + 2, y + 2, 12, 12, fill)
    if state == "active":
        for i in range(2, 14):
            put(x + i, y + 13, c(0x2a1c0c))
            put(x + 13, y + i, c(0x2a1c0c))
    if state == "hover":
        for i in range(2, 14):
            put(x + i, y + 2, c(0x6a6a84))


for k, s in enumerate(("normal", "hover", "active", "disabled")):
    button(16 * k, 24, s)


# ---------- вкладки 16x16 ----------
def tab(x, y, selected):
    fill = c(0x4a3418) if selected else ST2
    rect(x, y, 16, 16, fill)
    for i in range(16):
        put(x + i, y, ST0)
    for j in range(16):
        put(x, y + j, ST0)
        put(x + 15, y + j, ST0)
    for i in range(1, 15):
        put(x + i, y + 1, GD1 if selected else ST4)
    for j in range(1, 15):
        put(x + 1, y + j, BR2 if selected else ST3)
    put(x, y, CLEAR)
    put(x + 15, y, CLEAR)
    if selected:
        for i in range(1, 15):
            put(x + i, y + 15, fill)
        for i in range(2, 14):
            put(x + i, y + 2, GD0)
    else:
        for i in range(16):
            put(x + i, y + 15, ST0)


tab(64, 24, True)
tab(80, 24, False)


# ---------- подсветка выбранной силы (рамка, центр прозрачный) ----------
def slotsel(x, y):
    for i in range(16):
        for (a, b) in ((i, 0), (i, 15), (0, i), (15, i)):
            put(x + a, y + b, GD1)
    for i in range(1, 15):
        for (a, b) in ((i, 1), (i, 14), (1, i), (14, i)):
            put(x + a, y + b, c(0xffd700, 110))
    for (a, b) in ((0, 0), (15, 0), (0, 15), (15, 15)):
        put(x + a, y + b, GD2)


slotsel(96, 24)


def slothover(x, y):
    rect(x, y, 16, 16, (255, 255, 255, 40))
    for i in range(16):
        for (a, b) in ((i, 0), (i, 15), (0, i), (15, i)):
            put(x + a, y + b, (255, 255, 255, 90))


slothover(112, 24)


# ---------- тонкая рамка 12x12, края по 3 (для плотных панелей) ----------
def thin(x, y):
    rect(x, y, 12, 12, ST1)
    for i in range(12):
        put(x + i, y, ST0)
        put(x + i, y + 11, ST0)
        put(x, y + i, ST0)
        put(x + 11, y + i, ST0)
    for i in range(1, 11):
        put(x + i, y + 1, BR2)
        put(x + 1, y + i, BR2)
        put(x + i, y + 10, BR0)
        put(x + 10, y + i, BR0)
    for i in range(2, 10):
        put(x + i, y + 2, ST0)
        put(x + 2, y + i, ST0)
    put(x + 1, y + 1, GD1)
    put(x + 10, y + 1, GD0)
    put(x + 1, y + 10, GD0)
    put(x + 10, y + 10, GD1)
    for (cx, cy) in ((x, y), (x + 11, y), (x, y + 11), (x + 11, y + 11)):
        put(cx, cy, CLEAR)


thin(128, 24)

# ---------- глифы 8x8 ----------
GLYPH_Y = 48
GLYPHS = {
    "close": ["........", ".##..##.", "..#..#..", "...##...", "...##...", "..#..#..", ".##..##.", "........"],
    "search": ["..####..", ".#....#.", ".#....#.", ".#....#.", "..####..", "......#.", ".......#", "........"],
    "star": ["...##...", "...##...", "########", ".######.", "..####..", "..#..#..", ".##..##.", "........"],
    "star_off": ["...##...", "...#.#..", "##.##.##", "#......#", ".#....#.", "..#..#..", ".##..##.", "........"],
    "left": ["........", "....#...", "...##...", "..###...", "...##...", "....#...", "........", "........"],
    "right": ["........", "...#....", "...##...", "...###..", "...##...", "...#....", "........", "........"],
    "up": ["........", "........", "...##...", "..####..", ".######.", "........", "........", "........"],
    "down": ["........", "........", ".######.", "..####..", "...##...", "........", "........", "........"],
    "pin": ["..####..", "..#..#..", "..#..#..", ".######.", "...##...", "...##...", "...#....", "...#...."],
    "check": ["........", ".......#", "......##", "#....##.", "##..##..", ".####...", "..##....", "........"],
    "plus": ["........", "...##...", "...##...", ".######.", ".######.", "...##...", "...##...", "........"],
}
GLYPH_ORDER = ["close", "search", "star", "star_off", "left", "right", "up", "down", "pin", "check", "plus"]
GLYPH_COL = {"close": c(0xff6a5a), "star": GD1, "star_off": c(0xa08a40), "check": c(0x6aff80)}
for n, name in enumerate(GLYPH_ORDER):
    gx = n * 8
    rows = GLYPHS[name]
    col = GLYPH_COL.get(name, c(0xe8e8f0))
    for j in range(8):
        for i in range(8):
            if rows[j][i] == "#":
                put(gx + i, GLYPH_Y + j, col)
    for j in range(7):
        for i in range(7):
            if rows[j][i] == "#" and rows[j + 1][i + 1] != "#":
                put(gx + i + 1, GLYPH_Y + j + 1, (0, 0, 0, 170))

# ---------- гербы ----------
EM32_Y, EM16_Y = 64, 128   # 32x32: 8 в ряд, ряды y=64 и y=96; 16x16: 12 в ряд на y=128
KINGS = [  # (id, rgb, заряд) — порядок как у Kingdoms.K
    ("red", 0xB02E26, "sword"), ("blue", 0x3C44AA, "wave"), ("green", 0x5E7C16, "tree"),
    ("yellow", 0xFED83D, "sun"), ("orange", 0xF9801D, "flame"), ("purple", 0x8932B8, "crescent"),
    ("cyan", 0x169C9C, "tower"), ("pink", 0xF38BAA, "star"), ("white", 0xF9FFFE, "eagle"),
    ("black", 0x2B2B33, "anvil"), ("light_blue", 0x3AB3DA, "anchor"), ("brown", 0x835432, "stag"),
]


def lum(h):
    return 0.299 * ((h >> 16) & 255) + 0.587 * ((h >> 8) & 255) + 0.114 * (h & 255)


def shade(h, f):
    r, g, b = (h >> 16) & 255, (h >> 8) & 255, h & 255
    return (min(255, int(r * f)), min(255, int(g * f)), min(255, int(b * f)), 255)


SHIELD = [(1, 1), (14.99, 1), (14.99, 8), (12, 12.5), (8, 15), (4, 12.5), (1, 8)]  # сетка 16


def charge(d, name, s, col, out):
    def P(pts):
        d.polygon([(x * s, y * s) for x, y in pts], fill=col)

    def R(x0, y0, x1, y1, fill=None):
        d.rectangle([x0 * s, y0 * s, (x1 + 1) * s - 1, (y1 + 1) * s - 1], fill=fill or col)

    if name == "sword":
        R(7, 3, 8, 9)
        P([(7, 3), (9, 3), (8, 1.5)])
        R(5, 9, 10, 9)
        R(7, 10, 8, 12)
        R(6, 13, 9, 13)
    elif name == "wave":
        for yy in (4, 7, 10):
            for xx in range(3, 13):
                off = 1 if (xx // 2) % 2 else 0
                R(xx, yy + off, xx, yy + off + 1)
    elif name == "tree":
        P([(8, 2), (12, 7), (4, 7)])
        P([(8, 5), (13, 10), (3, 10)])
        R(7, 10, 8, 13)
    elif name == "sun":
        R(5, 5, 10, 10)
        R(6, 4, 9, 11)
        R(4, 6, 11, 9)
        R(7, 2, 8, 3)
        R(7, 12, 8, 13)
        R(2, 7, 3, 8)
        R(12, 7, 13, 8)
        for (a, b) in ((3, 3), (11, 3), (3, 11), (11, 11)):
            R(a, b, a + 1, b + 1)
        R(6, 6, 9, 9, out)
        R(7, 5, 8, 10, out)
        R(5, 7, 10, 8, out)
    elif name == "flame":
        P([(8, 2), (11, 7), (11.5, 10.5), (8, 13.5), (4.5, 10.5), (5, 7), (7, 8)])
        P([(8, 8.5), (9.5, 10.5), (8, 12.5), (6.5, 10.5)])
        d.polygon([(8 * s, 8.5 * s), (9.5 * s, 10.5 * s), (8 * s, 12.5 * s), (6.5 * s, 10.5 * s)], fill=out)
    elif name == "crescent":
        P([(5, 2.5), (10.5, 3), (8, 5.5), (7, 8), (8, 10.5), (10.5, 13), (5, 13.5), (3, 10), (3, 6)])
    elif name == "tower":
        R(5, 5, 10, 13)
        for xx in (4, 7, 10):
            R(xx, 3, xx + 1, 5)
        R(7, 10, 8, 13, out)
        R(7, 7, 8, 8, out)
    elif name == "star":
        R(7, 2, 8, 3)
        R(6, 4, 9, 5)
        R(2, 6, 13, 6)
        R(3, 7, 12, 7)
        R(4, 8, 11, 8)
        R(5, 9, 10, 9)
        R(4, 10, 6, 10)
        R(9, 10, 11, 10)
        R(4, 11, 5, 11)
        R(10, 11, 11, 11)
    elif name == "eagle":
        P([(8, 4), (9, 6), (14, 3.5), (12.5, 8), (9, 9.5), (8.5, 13), (7.5, 13), (7, 9.5), (3.5, 8), (2, 3.5), (7, 6)])
        R(7, 2, 8, 5)
    elif name == "anvil":
        R(3, 4, 12, 6)
        R(5, 7, 10, 8)
        R(6, 9, 9, 10)
        R(4, 11, 11, 13)
        R(12, 5, 13, 5)
    elif name == "anchor":
        R(7, 3, 8, 12)
        R(5, 5, 10, 6)
        R(6, 2, 9, 3)
        P([(2, 8), (4, 8), (6, 11), (8, 12), (10, 11), (12, 8), (14, 8), (12, 13), (8, 14), (4, 13)])
        d.polygon([(6 * s, 9 * s), (8 * s, 11 * s), (10 * s, 9 * s), (10 * s, 11 * s), (8 * s, 13 * s), (6 * s, 11 * s)], fill=None)
    elif name == "stag":
        R(7, 8, 8, 13)
        R(6, 7, 9, 10)
        R(5, 4, 5, 8)
        R(10, 4, 10, 8)
        R(3, 3, 4, 3)
        R(11, 3, 12, 3)
        R(3, 6, 5, 6)
        R(10, 6, 12, 6)


def emblem(rgb, name, size):
    s = size // 16
    im = Image.new("RGBA", (size, size), CLEAR)
    d = ImageDraw.Draw(im)
    base = c(rgb)
    d.polygon([(x * s, y * s) for x, y in SHIELD], fill=c(0x0c0c12))
    m = s
    cx = cy = 8 * s

    def shrink(p):
        sx, sy = p[0] * s, p[1] * s
        dx, dy = cx - sx, cy - sy
        n = max(1e-6, (dx * dx + dy * dy) ** 0.5)
        return (sx + dx / n * m * 1.05, sy + dy / n * m * 1.05)

    d.polygon([shrink(p) for p in SHIELD], fill=base)
    pp = im.load()
    for y in range(size):
        for x in range(size):
            if pp[x, y] == base and (x >= size * 0.72 or y >= size * 0.8):
                pp[x, y] = shade(rgb, 0.8)
    # золотой кант по верхней кромке
    gold = GD1 if lum(rgb) < 200 else GD0
    for x in range(size):
        for y in range(size):
            if pp[x, y][3] and pp[x, y] != c(0x0c0c12):
                pass
    dark_charge = lum(rgb) > 150
    col = c(0x1a1a24) if dark_charge else c(0xf4f0e0)
    out = c(0xf4f0e0) if dark_charge else c(0x14141c)
    layer = Image.new("RGBA", (size, size), CLEAR)
    charge(ImageDraw.Draw(layer), name, s, col, out)
    if s == 2 and not dark_charge:
        sh = Image.new("RGBA", (size, size), CLEAR)
        lp, sp = layer.load(), sh.load()
        for y in range(size - 1):
            for x in range(size - 1):
                if lp[x, y][3] and not lp[x + 1, y + 1][3] and im.getpixel((x + 1, y + 1))[3]:
                    sp[x + 1, y + 1] = (0, 0, 0, 120)
        im = Image.alpha_composite(im, sh)
    # заряд только внутри щита
    lp = layer.load()
    ip = im.load()
    for y in range(size):
        for x in range(size):
            if lp[x, y][3] and ip[x, y][3] == 0:
                lp[x, y] = CLEAR
    im = Image.alpha_composite(im, layer)
    # золотая искра в верхних углах
    ip = im.load()
    ip[1 * s, 1 * s] = gold
    return im


for n, (kid, rgb, ch) in enumerate(KINGS):
    img.paste(emblem(rgb, ch, 32), ((n % 8) * 32, EM32_Y + (n // 8) * 32))
    img.paste(emblem(rgb, ch, 16), (n * 16, EM16_Y))

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT)
print("saved", OUT)


# ---------- контрольные листы ----------
def nine(dst, sx, sy, sw, sh, b, x, y, w, h):
    """Девятичастная сборка той же логикой, что в Art.java."""
    def blit(sx0, sy0, sw0, sh0, dx, dy, dw, dh):
        if dw <= 0 or dh <= 0 or sw0 <= 0 or sh0 <= 0:
            return
        part = img.crop((sx0, sy0, sx0 + sw0, sy0 + sh0)).resize((dw, dh), Image.NEAREST)
        dst.alpha_composite(part, (dx, dy))
    cw, ch = sw - 2 * b, sh - 2 * b
    blit(sx, sy, b, b, x, y, b, b)
    blit(sx + sw - b, sy, b, b, x + w - b, y, b, b)
    blit(sx, sy + sh - b, b, b, x, y + h - b, b, b)
    blit(sx + sw - b, sy + sh - b, b, b, x + w - b, y + h - b, b, b)
    blit(sx + b, sy, cw, b, x + b, y, w - 2 * b, b)
    blit(sx + b, sy + sh - b, cw, b, x + b, y + h - b, w - 2 * b, b)
    blit(sx, sy + b, b, ch, x, y + b, b, h - 2 * b)
    blit(sx + sw - b, sy + b, b, ch, x + w - b, y + b, b, h - 2 * b)
    blit(sx + b, sy + b, cw, ch, x + b, y + b, w - 2 * b, h - 2 * b)


def sheet(bg):
    W, H = 150, 190
    canvas = bg.copy()
    for (w, h, x, y) in ((24, 24, 4, 4), (40, 30, 34, 4), (64, 44, 80, 4)):
        nine(canvas, 0, 0, 24, 24, 6, x, y, w, h)
    for (w, h, x, y) in ((30, 20, 4, 54), (50, 24, 40, 54)):
        nine(canvas, 24, 0, 16, 16, 3, x, y, w, h)
    nine(canvas, 40, 0, 16, 16, 4, 100, 54, 40, 24)
    for k in range(4):
        nine(canvas, 16 * k, 24, 16, 16, 3, 4 + k * 36, 84, 32, 14)
    nine(canvas, 64, 24, 16, 16, 3, 4, 102, 30, 16)
    nine(canvas, 80, 24, 16, 16, 3, 38, 102, 30, 16)
    nine(canvas, 96, 24, 16, 16, 3, 72, 102, 20, 20)
    nine(canvas, 72, 0, 32, 16, 8, 4, 124, 80, 16)
    nine(canvas, 72, 0, 32, 16, 8, 88, 124, 56, 16)
    nine(canvas, 128, 24, 12, 12, 3, 92, 144, 50, 20)
    nine(canvas, 56, 0, 6, 16, 2, 4, 144, 6, 40)
    nine(canvas, 62, 0, 6, 16, 2, 4, 150, 6, 14)
    for n in range(len(GLYPH_ORDER)):
        canvas.alpha_composite(img.crop((n * 8, 48, n * 8 + 8, 56)), (20 + n * 11, 148))
    d = ImageDraw.Draw(canvas)
    d.rectangle([20, 166, 140, 168], fill=(255, 255, 255, 255))
    d.rectangle([20, 172, 100, 174], fill=c(0xffd700))
    return canvas.resize((W * 4, H * 4), Image.NEAREST)


def gamebg():
    bg = Image.new("RGBA", (150, 190), c(0x4a7a30))
    p = bg.load()
    random.seed(3)
    for y in range(190):
        for x in range(150):
            f = random.randint(-14, 14)
            r, g, b, _ = p[x, y]
            p[x, y] = (max(0, r + f), max(0, g + f), max(0, b + f), 255)
    return bg


os.makedirs(VERIFY, exist_ok=True)
out = Image.new("RGBA", (1200, 760), c(0x808080))
out.paste(sheet(Image.new("RGBA", (150, 190), c(0x808080))), (0, 0))
out.paste(sheet(gamebg()), (600, 0))
out.save(os.path.join(VERIFY, "ui_art.png"))

em = Image.new("RGBA", (12 * 160, 340), c(0x707070))
for n in range(12):
    e32 = img.crop(((n % 8) * 32, EM32_Y + (n // 8) * 32, (n % 8) * 32 + 32, EM32_Y + (n // 8) * 32 + 32))
    e16 = img.crop((n * 16, EM16_Y, n * 16 + 16, EM16_Y + 16))
    em.alpha_composite(e32.resize((128, 128), Image.NEAREST), (n * 160 + 16, 4))
    em.alpha_composite(e16.resize((64, 64), Image.NEAREST), (n * 160 + 16, 140))
    em.alpha_composite(e32, (n * 160 + 100, 220))
    em.alpha_composite(e16, (n * 160 + 100, 260))
    em.alpha_composite(e16.resize((12, 12), Image.NEAREST), (n * 160 + 130, 260))
    em.alpha_composite(e16.resize((8, 8), Image.NEAREST), (n * 160 + 150, 260))
em.save(os.path.join(VERIFY, "emblems.png"))
