"""Мотивы: бомбы, стихии, символы. Роли цветов: h/m/s — основная рампа, H/M/S — вторая, остальное — фиксированные."""
import math
from lib import Layer

MOTIFS = {}
BADGES = {}


def motif(f):
    MOTIFS[f.__name__] = f
    return f


def sprite(name, rows, x=None, y=None):
    """Регистрирует мотив из ASCII-рисунка; без x/y центрируется."""
    w = max(len(r) for r in rows)
    rows = [r.ljust(w, '.') for r in rows]

    def f(L, flip=False, dx=0, dy=0, **kw):
        ox = (16 - w) // 2 if x is None else x
        oy = (16 - len(rows)) // 2 if y is None else y
        L.art(ox + dx, oy + dy, rows, flip)
    f.__name__ = name
    MOTIFS[name] = f
    return f


def star8(L, cx, cy, rc, rd, c, cc=None):
    for k in range(1, rc + 1):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            L.px(cx + dx * k, cy + dy * k, c)
    for k in range(1, rd + 1):
        for dx, dy in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
            L.px(cx + dx * k, cy + dy * k, c)
    L.px(cx, cy, cc or c)


# ---------------------------------------------------------------- бомбы
@motif
def bomb(L, **kw):
    L.disc(6.5, 9.5, 5.2, 'hms')
    L.rect(7, 3, 3, 2, 'g')
    L.px(10, 2, 'B'); L.px(11, 1, 'B')
    L.px(12, 1, 'y'); L.px(12, 0, 'o'); L.px(13, 1, 'o'); L.px(12, 2, 'o'); L.px(11, 0, 'y')
    L.px(4, 7, 'w'); L.px(4, 8, 'w'); L.px(5, 6, 'w')


sprite('grenade', [
    "......GG......",
    "....gGGGggg...",
    "....gGGG..gg..",
    "...hhmmmm.Y.Y.",
    "..hhhhmmmmY.Y.",
    "..hhmsmmsmms..",
    ".hhhmmmmmmmss.",
    ".hmsmmsmmsmss.",
    ".hmmmmmmmmmss.",
    ".mmsmmsmmsmss.",
    "..mmmmmmmmss..",
    "..mmsmmsmsss..",
    "...smmmmsss...",
    ".....ssss.....",
])

sprite('dynamite', [
    "..........y...",
    ".........oy.y.",
    "........B.o...",
    ".......B......",
    "..hmmhhmmhhmm.",
    "..hmmhhmmhhmm.",
    "..hmmhhmmhhmm.",
    ".BBBBBBBBBBBB.",
    ".BttBttBttBtB.",
    "..hmshhmshhms.",
    "..hmshhmshhms.",
    "..hmshhmshhms.",
    "..hmshhmshhms.",
    "..ssssssssss..",
])


@motif
def mine(L, **kw):
    L.ellipse(7.5, 10.5, 6.2, 3.6, 'hms')
    L.ellipse(7.5, 9.2, 4.4, 2.2, ('H', 'M', 'S'))
    L.rect(6, 5, 4, 3, 'M')
    L.rect(7, 4, 2, 1, 'r'); L.px(7, 5, 'r'); L.px(8, 5, 'r'); L.px(7, 4, 'w')
    for x in (2, 13):
        L.px(x, 7, 'g')
    L.px(4, 6, 'g'); L.px(11, 6, 'g')


sprite('flame', [
    "......m.......",
    ".....mm.......",
    ".....mmm..m...",
    "....mmmm.mm...",
    "...mmmmmmmmm..",
    "...mmmhmmmmms.",
    "..mmmmhhmmmms.",
    "..mmmhhhhmmms.",
    "..mmhhhhhhmmss",
    "..mmhhhwwhhmss",
    "...mhhhwwhhmss",
    "...mmhhhhhhmss",
    "....smhhhhmss.",
    "......ssss....",
])

sprite('drop', [
    "....h.....",
    "....hh....",
    "...hhmm...",
    "...hmmm...",
    "..hhmmmm..",
    "..hmmmmm..",
    ".hhmmmmmm.",
    ".hwmmmmms.",
    ".hwmmmmms.",
    ".mmmmmmss.",
    "..mmmmss..",
    "...ssss...",
])

sprite('bolt', [
    "......hhhh..",
    ".....hhhh...",
    "....hhhm....",
    "...hhhm.....",
    "..hhhhhhhh..",
    "..hmmmmmmm..",
    "......hmm...",
    ".....hmm....",
    "....hmm.....",
    "...hmm......",
    "...mm.......",
    "..m.........",
])

sprite('skull', [
    "...hhhhhh...",
    "..hhhhhhhh..",
    ".hhhhhhhhhm.",
    ".hhhhhhhhhm.",
    ".hkkkhhkkkm.",
    ".hkkkhhkkkm.",
    ".hhhhkkhhhm.",
    "..hhhhhhhm..",
    "...hmhmhm...",
    "...hhhhhm...",
    "...hmhmhm...",
])


@motif
def cloud(L, content=None, dy=0, **kw):
    rows = [
        ".....hhh......",
        "...hhhhhhh....",
        "..hhhhhhhhhh..",
        ".hhhhhhhhhhhm.",
        "hhhhhhhhhhhmmm",
        "mmmmmmmmmmmmss",
        ".ssssssssssss.",
    ]
    L.art(1, 1 + dy, rows)
    y = 9 + dy
    if content == 'rain':
        for x, yy in ((3, 9), (7, 10), (11, 9), (5, 12), (9, 13), (13, 12)):
            L.px(x, yy, 'b'); L.px(x, yy + 1, 'c')
    elif content == 'blood':
        for x, yy in ((3, 9), (7, 10), (11, 9), (5, 12), (9, 13), (13, 12)):
            L.px(x, yy, 'r'); L.px(x, yy + 1, 'R')
    elif content == 'snow':
        for x, yy in ((3, 9), (7, 10), (11, 9), (5, 12), (9, 13), (12, 12), (2, 13)):
            L.px(x, yy, 'w')
    elif content == 'gold':
        for x, yy in ((2, 9), (7, 10), (11, 9), (4, 12), (9, 13)):
            L.rect(x, yy, 2, 2, 'y'); L.px(x + 1, yy + 1, 'Y')
    elif content == 'loot':
        for (x, yy), c in zip(((2, 9), (6, 11), (10, 9), (4, 13), (12, 12)), 'rbnpy'):
            L.rect(x, yy, 2, 2, c)
    elif content == 'ash':
        for x, yy in ((3, 9), (7, 10), (11, 9), (5, 12), (9, 13)):
            L.px(x, yy, 'G')
    elif content == 'bolt':
        L.art(5, 9, ["..yy", ".yy.", "yyyy", "..yy", ".y.."])
    elif content == 'sun':
        for x in (2, 5, 8, 11, 14):
            L.line(8, 9, x, 15, 'y')
        L.line(7, 9, 7, 15, 'w'); L.line(9, 9, 9, 15, 'w')
        L.rect(6, 8, 4, 2, 'y')


@motif
def snowflake(L, **kw):
    star8(L, 7, 7, 6, 3, 'm', 'w')
    L.px(6, 7, 'w'); L.px(8, 7, 'w'); L.px(7, 6, 'w'); L.px(7, 8, 'w')
    for x, y in ((5, 1), (9, 1), (5, 13), (9, 13), (1, 5), (1, 9), (13, 5), (13, 9)):
        L.px(x, y, 'h')


@motif
def sun(L, **kw):
    L.disc(7.5, 7.5, 3.8, 'hms')
    for x, y, w, h in ((7, 0, 2, 2), (7, 14, 2, 2), (0, 7, 2, 2), (14, 7, 2, 2)):
        L.rect(x, y, w, h, 'm')
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12), (3, 3), (11, 3), (3, 11), (11, 11)):
        L.px(x, y, 'm')


@motif
def moon(L, stars=True, **kw):
    for y in range(16):
        for x in range(16):
            d1 = (x + .5 - 7.5) ** 2 + (y + .5 - 8) ** 2
            d2 = (x + .5 - 10.5) ** 2 + (y + .5 - 6.5) ** 2
            if d1 <= 6.2 ** 2 and d2 > 5.4 ** 2:
                L.px(x, y, 'h' if x + y < 14 else 'm' if x + y < 19 else 's')
    if stars:
        L.px(12, 3, 'y'); L.px(11, 3, 'y'); L.px(12, 2, 'y'); L.px(13, 3, 'y'); L.px(12, 4, 'y')
        L.px(13, 10, 'w')


@motif
def meteor(L, **kw):
    L.line(7, 8, 14, 1, 'r'); L.line(8, 8, 14, 2, 'o'); L.line(7, 7, 13, 1, 'o'); L.line(8, 7, 13, 2, 'y')
    L.line(6, 8, 11, 3, 'o')
    L.disc(5.5, 10.5, 4.7, 'ooR')
    L.disc(5.2, 10.2, 3.3, 'hms')
    L.px(4, 9, 'w'); L.px(6, 11, 'G')


@motif
def meteors(L, **kw):
    for x, y, r in ((4.5, 11.5, 2.7), (10.5, 8, 2.3), (4, 4, 1.4)):
        L.line(int(x + 1), int(y - 1), int(x + 5), int(y - 5), 'r')
        L.line(int(x + 2), int(y - 1), int(x + 5), int(y - 4), 'o')
        L.disc(x, y, r + 1.0, 'ooR')
        L.disc(x, y, r, 'hms')


sprite('tornado', [
    "hhhhhhhhhhhh",
    ".mmmmmmmmmmm",
    "..hhhhhhhhs.",
    "...mmmmmmss.",
    "...hhhhhhs..",
    "....mmmmss..",
    "....hhhhs...",
    ".....mmss...",
    ".....hhs....",
    "......ms....",
    "......s.....",
])

sprite('crack', [
    "..........GG..",
    ".........GkG..",
    "........GkkG..",
    ".......GkG....",
    "......GkG.....",
    ".....GkkkG....",
    "....GkGGkG....",
    "....GkG.kG....",
    ".....GkGkG....",
    "......GkkG....",
    ".......GkG....",
    "........GG....",
])

sprite('wave', [
    "........hhh...",
    ".......hwwhhh.",
    "......hwhhmmhh",
    ".....hwhhmmmmm",
    "....hhhhhmmmmm",
    "...hhhhmmmmmmm",
    "..hhhmmmmmmmmm",
    ".hhmmmmmmmmmmm",
    "hmmmmmmmmmmmss",
    "mmmmmsmmmmmsss",
    "sssssssssssss.",
])

sprite('mountain', [
    ".......hh.....",
    "......hhmm....",
    ".....hhhmm....",
    "....hhhhmms...",
    "...hhhmmmmms..",
    "..hhhhmmmmmss.",
    ".hhhhmmmmmmmss",
    "hhhhmmmmmmmmss",
])

sprite('peak', [
    ".......hh.....",
    "......hwwm....",
    ".....hwwwmm...",
    "....hhwwmmms..",
    "...hhhhmmmmms.",
    "..hhhhmmmmmmss",
    ".hhhhmmmmmmmss",
    "hhhhmmmmmmmmss",
    "mmmmmmmmmmmmss",
])

sprite('volcano', [
    "..g.g...o.y...",
    "...g..o.y.....",
    ".....oyoyo....",
    "....hhoyomm...",
    "...hhhhooomms.",
    "...hhhmmoomms.",
    "..hhhhmmoomsss",
    "..hhhmmmmoomss",
    ".hhhhmmmmmommss",
    "hhhhmmmmmmmmmss",
])

sprite('geyser', [
    ".....h.h......",
    "....h.hh.h....",
    ".....hhhh.....",
    "....hhhhhh....",
    ".....hwmm.....",
    ".....hwmm.....",
    "....hhwmms....",
    "..GGGGhmGGG...",
    ".GgGGGGGGGGG..",
    "GgggGGGGGGGGG.",
    "GGGGGGGGGGGGGG",
], y=3)

sprite('ufo', [
    "....cccc....",
    "...cwcccc...",
    "..hhhhhhhh..",
    ".hhhhhhhhhmm",
    "hmyhhyhhyhms",
    ".ssssssssss.",
    "..y..y..y...",
    ".y...y...y..",
], y=1)

sprite('beam', [
    "..hh....hh..",
    ".hhhmmmmhhh.",
    ".hhmmmmmmhh.",
], y=11)

sprite('ghost', [
    "...hhhhhh...",
    "..hhhhhhhh..",
    ".hhhhhhhhhm.",
    ".hhkkhhkkhm.",
    ".hhkkhhkkhm.",
    ".hhhhhhhhhm.",
    ".hhhhkkhhhm.",
    ".hhhhhhhhhm.",
    ".hmhhhhhhmm.",
    ".mm.hhhh.ms.",
    ".m...mm...s.",
])

sprite('hand', [
    "...f..f.......",
    "..hf.hf..f....",
    "..hf.hf.hf....",
    "..hf.hf.hf.f..",
    ".hhfhhfhhfhf..",
    ".hhhhhhhhhhfm.",
    ".hhhhhhhhhhmm.",
    ".hhhhhhhhhmms.",
    "..hhhhhhhhms..",
    "...hhhhhhmms..",
    "....hhhhmms...",
    "....mmmmmss...",
])

sprite('finger', [
    ".....hh.......",
    "....hhhm......",
    "....hhhm......",
    "....hhhm......",
    "....hhhm.hh...",
    "....hhhhhhhhh.",
    "..hhhhhhhhhhm.",
    ".hhhhhhhhhhhm.",
    ".hhhhhhhhhhmm.",
    "..hhhhhhhhmm..",
    "...hhhhhhmms..",
    "....mmmmmms...",
])

sprite('magnet', [
    "..RR....bb....",
    ".RRR....bbb...",
    ".RrR....bbb...",
    ".RrR....bbb...",
    ".rrr....bbb...",
    ".mmm....mmm...",
    ".mmmm..mmmm...",
    "..mmmmmmmm....",
    "...mmmmmm.....",
    "....mmmm......",
], y=2)

sprite('eraser', [
    "...........hh.",
    "..........hhm.",
    ".........hhm..",
    "........hhm...",
    ".......hhm....",
    "......hhm.....",
    "...llbbm......",
    "..llllbb......",
    ".llllbbb......",
    ".lllbbb.......",
    "..lbb.........",
])

sprite('zzz', [
    "wwwww....",
    "...w.....",
    "..w......",
    ".wwwww...",
    "......wwww",
    ".......w..",
    "......w...",
    ".....wwww.",
], y=2)

sprite('swirl', [
    "...hhhhhh...",
    ".hhmmmmmmhh.",
    "hhm......mhh",
    "hm..hhhh..mh",
    "hm.hm..mh.mh",
    "hm.hm.hm..mh",
    "hm.hm.hm..mm",
    "hm..hhmm..mm",
    "hhm......mms",
    ".hhmmmmmmmm.",
    "...mmmmmm...",
])

sprite('virus', [
    "...r..rr..r...",
    "....r.rr.r....",
    ".r..mmmmmm.r..",
    "..rhhhhhhmmr..",
    "..mhhhhhhmmm..",
    "rrmhhwhhhmmmrr",
    "rrmhhhhhhmmmrr",
    "..mmhhhhmmsm..",
    "..rmmmmmmmsr..",
    ".r..mmmmss..r.",
    "....r.rr.r....",
    "...r..rr..r...",
])

@motif
def blob(L, eyes=False, drip=False, spots=None, **kw):
    MOTIFS['blob0'](L, dy=-1 if drip else 0)
    if eyes:
        L.rect(4, 5, 2, 2, 'w'); L.px(5, 6, 'k'); L.rect(9, 6, 3, 3, 'w'); L.px(10, 7, 'k'); L.px(7, 10, 'R'); L.px(8, 11, 'R'); L.px(3, 9, 'R')
    if drip:
        for x in (4, 8, 12):
            L.px(x, 12, 'm'); L.px(x, 13, 'm'); L.px(x, 14, 's')
    for x, y in spots or ():
        L.px(x, y, 'c')


sprite('blob0', [
    "....hhhh......",
    "..hhhhhhmm....",
    ".hhhwhhhmmm...",
    ".hhwhhhhmmmmm.",
    "hhhhhhhmmmmmm.",
    "hhhhhhmmmmmmms",
    "hhhmmmmmmmmmms",
    ".hmmmmmmmmmmss",
    ".mmmmsmmmmsss.",
    "..msssssssss..",
])

sprite('hole', [
    "...hhhhhh...",
    "..hmmmmmmh..",
    ".hmkkkkkkmh.",
    "hmkkGGGGkkmh",
    "hmkGkkkkGkmh",
    "hmkGkwkkGkmh",
    "hmkGkkkkGkms",
    "hmkkGGGGkkms",
    ".hmkkkkkkms.",
    "..hmmmmmms..",
    "...ssssss...",
])

sprite('sphere', [
    "....hhhhhh....",
    "..hhhmmmmmhh..",
    ".hhmm....mmhh.",
    ".hm........mm.",
    "hm....kk....ms",
    "hm...kkkk...ms",
    "hm...kkkk...ms",
    "hm....kk....ms",
    ".hm........ms.",
    ".hsmm....mmss.",
    "..sssmmmmsss..",
    "....ssssss....",
])


@motif
def mushroom(L, **kw):
    L.art(2, 2, [
        "...hhhhhhhh...",
        "..hhwwhhhhmm..",
        ".hhwwhhhwwhhm.",
        ".hhhhhhhwwhhm.",
        "hhhhwwhhhhhmmm",
        "mmmmmmmmmmmmss",
    ])
    L.art(5, 8, [
        "tttttt",
        "tTtttT",
        "tTtttT",
        "tTtttT",
        "TTTTTT",
    ])


@motif
def mushcloud(L, **kw):
    L.art(1, 1, [
        "....hhhhhh....",
        "..hhhhhhhhhh..",
        ".hhhhhhhhhhhm.",
        "hhhhhhhhhhhmmm",
        "hhhhhhhhhhmmmm",
        ".mmmmmmmmmmss.",
        "..ssmmmmmsss..",
    ])
    L.art(5, 8, [
        "..hh..",
        ".hhhm.",
        "..hm..",
        "..hm..",
        ".hhmm.",
    ])
    L.rect(3, 12, 10, 2, 'o'); L.rect(5, 12, 6, 1, 'y')


@motif
def ring(L, r=5.5, c='m', **kw):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + .5 - 8, y + .5 - 8)
            if r - 1.2 <= d <= r:
                L.px(x, y, c)


@motif
def burst(L, c1='r', c2='y', c3='b', **kw):
    star8(L, 7, 7, 6, 4, c1, 'w')
    star8(L, 7, 7, 3, 2, c2, 'w')
    for x, y in ((1, 1), (13, 2), (2, 13), (13, 13), (0, 7), (14, 7)):
        L.px(x, y, c3)


sprite('rocket', [
    "........hh...",
    ".......hhhm..",
    "......hhhhmm.",
    ".....hhwhhms.",
    "....hhhhhmms.",
    "...hhhhhmms..",
    "..yBhhhmms...",
    ".yyoBhmms....",
    ".yoy.Bs......",
    "..y.....y....",
], x=2, y=2)
