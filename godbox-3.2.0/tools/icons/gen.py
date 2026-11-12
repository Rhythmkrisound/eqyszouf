#!/usr/bin/env python3
"""Генератор иконок сил и категорий GodBox.

    python3 tools/icons/gen.py            # перегенерировать атласы, обновить константы в PowerIcons.java
    python3 tools/icons/gen.py --check    # только проверить, что атласы/константы соответствуют enum'ам

Ячейка атласа = Power.ordinal() (строка за строкой, COLS ячеек в ряд). Порядок и число констант
парсятся из Power.java; хэш порядка зашит в PowerIcons.java и сверяется в рантайме.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image, ImageDraw

from lib import Layer, Scheme, render
import motifs_a
import motifs_b  # noqa: F401  (регистрируют мотивы)
import motifs_c  # noqa: F401
import motifs_d  # noqa: F401
from recipes import POWERS, CATEGORIES

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
JAVA = os.path.join(ROOT, 'src/main/java/com/vladik/godbox')
OUT = os.path.join(ROOT, 'src/main/resources/assets/godbox/textures/gui')
VERIFY = '/tmp/verify'
CELL = 16
COLS = 20


def java_hash(s):
    h = 0
    for ch in s:
        h = (31 * h + ord(ch)) & 0xFFFFFFFF
    return h - (1 << 32) if h >= (1 << 31) else h


def parse_powers():
    t = open(os.path.join(JAVA, 'Power.java'), encoding='utf-8').read()
    r = re.findall(r'^    ([A-Z][A-Z0-9_]*)\(\s*(?:Category\.)?(\w+),', t, re.M)
    return [(n, c) for n, c in r]


def parse_categories():
    t = open(os.path.join(JAVA, 'Category.java'), encoding='utf-8').read()
    return re.findall(r'^    ([A-Z][A-Z0-9_]*)\("', t, re.M)


def build(recipe):
    motif, p, s, badge, kw = recipe
    L = Layer()
    for name in kw.get('also', []):
        motifs_a.MOTIFS[name](L)
    if motif not in motifs_a.MOTIFS:
        raise SystemExit('неизвестный мотив: ' + motif)
    motifs_a.MOTIFS[motif](L, **{k: v for k, v in kw.items() if k != 'also'})
    layers = [L]
    if badge:
        B = Layer()
        B.art(10, 10, motifs_a.BADGES[badge])
        layers.append(B)
    return render(layers, Scheme(p, s))


def to_image(cells, cols):
    rows = (len(cells) + cols - 1) // cols
    atlas = Image.new('RGBA', (cols * CELL, rows * CELL), (0, 0, 0, 0))
    for i, img in enumerate(cells):
        cx, cy = (i % cols) * CELL, (i // cols) * CELL
        for y in range(16):
            for x in range(16):
                atlas.putpixel((cx + x, cy + y), img[y][x])
    return atlas


def sheets(names, cells, cats, labels):
    from PIL import ImageFont
    os.makedirs(VERIFY, exist_ok=True)
    for f in os.listdir(VERIFY):
        if f.startswith('sheet_'):
            os.remove(os.path.join(VERIFY, f))
    S, cw, ch, per = 4, 136, 92, 8
    groups = {}
    for n, c, img in zip(names, cats, cells):
        groups.setdefault(c, []).append((n, img))
    paths = []
    for c, items in groups.items():
        for part in range(0, len(items), 24):
            chunk = items[part:part + 24]
            rows = (len(chunk) + per - 1) // per
            im = Image.new('RGB', (per * cw, rows * ch), (58, 60, 72))
            d = ImageDraw.Draw(im)
            for i, (n, img) in enumerate(chunk):
                x0, y0 = (i % per) * cw, (i // per) * ch
                d.rectangle([x0 + 36, y0 + 4, x0 + 36 + 65, y0 + 4 + 65], fill=(38, 40, 50))
                tile = Image.new('RGBA', (16, 16))
                for y in range(16):
                    for x in range(16):
                        tile.putpixel((x, y), img[y][x])
                tile = tile.resize((16 * S, 16 * S), Image.NEAREST)
                im.paste(tile, (x0 + 37, y0 + 5), tile)
                d.text((x0 + 3, y0 + 74), n, fill=(230, 230, 240))
            p = os.path.join(VERIFY, 'sheet_%s_%d.png' % (c.lower(), part // 24))
            im.save(p)
            paths.append(p)
    return paths


def main():
    powers = parse_powers()
    names = [n for n, _ in powers]
    cat_names = parse_categories()
    missing = [n for n in names if n not in POWERS]
    extra = [n for n in POWERS if n not in names]
    cmissing = [n for n in cat_names if n not in CATEGORIES]
    cextra = [n for n in CATEGORIES if n not in cat_names]
    if missing or extra or cmissing or cextra:
        raise SystemExit('Рецепты не совпадают с enum: нет рецептов %s; лишние %s; категории нет %s, лишние %s'
                         % (missing, extra, cmissing, cextra))
    if len(set(names)) != len(names):
        raise SystemExit('повторяющиеся константы Power')

    cells = [build(POWERS[n]) for n in names]
    ccells = [build(CATEGORIES[n]) for n in cat_names]

    seen = {}
    for n, img in zip(names, cells):
        key = tuple(tuple(r) for r in img)
        if all(px[3] == 0 for r in img for px in r):
            raise SystemExit('пустая иконка: ' + n)
        if key in seen:
            raise SystemExit('одинаковые иконки: %s и %s' % (n, seen[key]))
        seen[key] = n
    cseen = set()
    for n, img in zip(cat_names, ccells):
        key = tuple(tuple(r) for r in img)
        if key in cseen:
            raise SystemExit('одинаковые иконки категорий: ' + n)
        cseen.add(key)

    h = java_hash(','.join(names))
    ch = java_hash(','.join(cat_names))
    jpath = os.path.join(JAVA, 'client/PowerIcons.java')
    src = open(jpath, encoding='utf-8').read()
    new = src
    for key, val in (('POWER_COUNT', len(names)), ('POWER_HASH', h), ('CAT_COUNT', len(cat_names)), ('CAT_HASH', ch), ('COLS', COLS)):
        new, k = re.subn(r'(static final int %s = )-?\d+;' % key, r'\g<1>%d;' % val, new)
        if k != 1:
            raise SystemExit('в PowerIcons.java нет константы ' + key)

    if '--check' in sys.argv:
        if new != src:
            raise SystemExit('PowerIcons.java не соответствует enum: перегенерируйте (python3 tools/icons/gen.py)')
        ex = Image.open(os.path.join(OUT, 'power_icons.png'))
        rows = (len(names) + COLS - 1) // COLS
        if ex.size != (COLS * CELL, rows * CELL):
            raise SystemExit('размер power_icons.png не соответствует числу сил')
        print('OK: %d сил, %d категорий' % (len(names), len(cat_names)))
        return

    os.makedirs(OUT, exist_ok=True)
    to_image(cells, COLS).save(os.path.join(OUT, 'power_icons.png'))
    to_image(ccells, len(ccells)).save(os.path.join(OUT, 'category_icons.png'))
    if new != src:
        open(jpath, 'w', encoding='utf-8').write(new)
    cats = [c for _, c in powers]
    paths = sheets(names, cells, [c if c != 'null' else 'NONE' for c in cats], None)
    csheet = to_image(ccells, len(ccells)).resize((len(ccells) * 64, 64), Image.NEAREST)
    bg = Image.new('RGB', csheet.size, (58, 60, 72))
    bg.paste(csheet, (0, 0), csheet)
    bg.save(os.path.join(VERIFY, 'sheet_categories.png'))
    print('иконок: %d сил + %d категорий' % (len(names), len(cat_names)))
    print('\n'.join(paths))


if __name__ == '__main__':
    main()
