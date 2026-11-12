#!/usr/bin/env python3
"""Процедурные спрайты частиц GodBox (оригинальный пиксель-арт, без размытия).
Запуск: python3 tools/particles/gen.py  (нужен Pillow)
Пишет в src/main/resources/assets/godbox/textures/particle/ и контактный лист в /tmp/verify/particles.png"""
import math
import os
import random
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "src/main/resources/assets/godbox/textures/particle")
SHEET = "/tmp/verify/particles.png"
os.makedirs(OUT, exist_ok=True)
os.makedirs(os.path.dirname(SHEET), exist_ok=True)

# Палитры (от яркого к тёмному)
FIRE = [(255, 250, 215), (255, 226, 120), (255, 178, 52), (240, 110, 24), (190, 56, 20), (110, 30, 18), (58, 24, 20)]
SMOKE = [(150, 148, 146), (112, 110, 110), (82, 80, 82), (56, 55, 58), (34, 34, 38)]
RING = [(250, 244, 226), (222, 212, 188), (180, 168, 148), (130, 120, 106)]
DEBRIS = [(214, 206, 196), (170, 160, 150), (128, 118, 110), (90, 82, 78)]


def hash2(x, y, seed):
    n = (x * 374761393 + y * 668265263 + seed * 2246822519) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def vnoise(x, y, seed, cell):
    """Гладкий value-noise; порог потом даёт жёсткие пиксели."""
    gx, gy = x / cell, y / cell
    x0, y0 = int(math.floor(gx)), int(math.floor(gy))
    fx, fy = gx - x0, gy - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = hash2(x0, y0, seed), hash2(x0 + 1, y0, seed)
    c, d = hash2(x0, y0 + 1, seed), hash2(x0 + 1, y0 + 1, seed)
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def fbm(x, y, seed):
    return 0.6 * vnoise(x, y, seed, 8) + 0.3 * vnoise(x, y, seed + 7, 4) + 0.1 * vnoise(x, y, seed + 13, 2)


def pick(pal, t):
    t = max(0.0, min(0.999, t))
    return pal[int(t * len(pal))]


def fireball(frame, n=8, size=32):
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    k = frame / (n - 1)
    c = (size - 1) / 2
    grow = 0.42 + 0.5 * math.sin(min(1.0, k * 1.3) * math.pi / 2)
    for y in range(size):
        for x in range(size):
            dx, dy = (x - c) / c, (y - c) / c
            d = math.hypot(dx, dy)
            nz = fbm(x + frame * 3, y - frame * 2, 11)
            edge = grow * (0.72 + 0.5 * nz)
            if d > edge:
                continue
            # позже ядро гаснет: сдвигаем палитру к тёмному
            heat = (d / edge) * 0.55 + (1 - nz) * 0.18 + k * 0.45
            if k > 0.6 and hash2(x, y, frame) < (k - 0.6) * 0.9:
                continue  # прогары в догорающем шаре
            im.putpixel((x, y), pick(FIRE, heat) + (255,))
    return im


def shockwave(frame, n=4, size=32):
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c) / c
            nz = vnoise(x, y, 31 + frame, 3)
            inner = 0.72 + 0.05 * (nz - 0.5)
            outer = 0.97 - 0.05 * nz
            if inner <= d <= outer:
                t = (d - inner) / max(0.01, outer - inner)
                col = pick(RING, 0.15 + t * 0.7 + (nz - 0.5) * 0.4)
                if hash2(x, y, frame + 5) < 0.12:
                    continue  # рваный край
                im.putpixel((x, y), col + (255,))
            elif 0.62 < d < inner and hash2(x, y, frame + 9) < 0.18 * (d - 0.62) / 0.18:
                im.putpixel((x, y), RING[3] + (255,))  # пыль за фронтом
    return im


def smoke(i, size=32):
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            d = math.hypot((x - c) / c, (y - c) / c)
            nz = fbm(x, y, 41 + i * 5)
            if d > 0.55 + 0.4 * nz:
                continue
            # светлее сверху-слева, тёмный низ, чуть дизеринга
            shade = 0.15 + 0.5 * d + 0.25 * ((y - c) / c + 1) / 2 + (hash2(x, y, i) - 0.5) * 0.18
            im.putpixel((x, y), pick(SMOKE, shade) + (255,))
    return im


def ember(i, size=8):
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    cols = [FIRE[0], FIRE[2], FIRE[4], FIRE[5]]
    core, mid = cols[i], FIRE[min(6, 2 + i)]
    pts = {(3, 3), (4, 3), (3, 4), (4, 4)}
    halo = {(2, 3), (5, 3), (2, 4), (5, 4), (3, 2), (4, 2), (3, 5), (4, 5)}
    for p in pts:
        im.putpixel(p, core + (255,))
    if i < 3:
        for p in halo:
            im.putpixel(p, mid + (255,))
    return im


def debris(i, size=8):
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    rnd = random.Random(70 + i)
    r = 2 + (i % 2)
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - 3.5, y - 3.5)
            if d <= r + rnd.random() * 1.2 - 0.3:
                im.putpixel((x, y), pick(DEBRIS, (x + y) / 14 + rnd.random() * 0.3) + (255,))
    return im


def main():
    sprites = []
    for i in range(8):
        sprites.append(("fireball_%d" % i, fireball(i)))
    for i in range(4):
        sprites.append(("shockwave_%d" % i, shockwave(i)))
    for i in range(4):
        sprites.append(("smoke_%d" % i, smoke(i)))
    for i in range(4):
        sprites.append(("ember_%d" % i, ember(i)))
    for i in range(4):
        sprites.append(("debris_%d" % i, debris(i)))
    for name, im in sprites:
        im.save(os.path.join(OUT, name + ".png"))

    # контактный лист: тёмный и светлый фон, увеличение
    cell, pad = 96, 8
    cols = 8
    rows = math.ceil(len(sprites) / cols)
    W, H = cols * (cell + pad) + pad, rows * (cell + pad) + pad
    sheet = Image.new("RGBA", (W, H * 2), (0, 0, 0, 255))
    for half, bg in enumerate([(24, 26, 32, 255), (200, 214, 232, 255)]):
        Image.Image.paste(sheet, Image.new("RGBA", (W, H), bg), (0, half * H))
        for idx, (name, im) in enumerate(sprites):
            s = cell if im.width >= 32 else cell
            big = im.resize((s, s), Image.NEAREST)
            px = pad + (idx % cols) * (cell + pad)
            py = half * H + pad + (idx // cols) * (cell + pad)
            sheet.alpha_composite(big, (px, py))
    sheet.save(SHEET)


main()
