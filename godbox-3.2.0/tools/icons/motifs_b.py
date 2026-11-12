"""Мотивы: существа, лица, боссы, маги."""
from lib import Layer
from motifs_a import motif, sprite, MOTIFS, star8


def face(L, ears=None, x0=2, x1=13, y0=4, y1=13, snout=None, eyes='k', eye_y=None, nose=None, cut=True):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if cut and x in (x0, x1) and y in (y0, y1):
                continue
            c = 'm'
            if y == y0 or x == x0:
                c = 'h'
            elif y == y1 or x == x1:
                c = 's'
            L.px(x, y, c)
    if ears == 'point':
        L.art(x0, y0 - 3, ["h..", "hm.", "hmm"])
        L.art(x1 - 2, y0 - 3, ["..m", ".ms", "mss"])
    elif ears == 'round':
        L.art(x0 - 1, y0 - 2, ["hhh", "hlm"])
        L.art(x1 - 1, y0 - 2, ["mmm", "mls"])
    elif ears == 'long':
        for dx in (x0 + 2, x1 - 3):
            for j in range(5):
                L.art(dx, y0 - 5 + j, ["hl" if dx < 8 else "ml"])
    elif ears == 'horn':
        L.art(x0 - 1, y0 - 2, ["w.", "ww", "wm"][::1])
        L.art(x1 - 0, y0 - 2, [".w", "ww", "mw"])
    elif ears == 'side':
        L.art(x0 - 2, y0 + 1, ["hh", "mm"])
        L.art(x1 + 1, y0 + 1, ["mm", "ms"])
    ey = (y0 + 3) if eye_y is None else eye_y
    ex1, ex2 = x0 + 2, x1 - 3
    if eyes == 'k':
        L.rect(ex1, ey, 2, 2, 'k'); L.rect(ex2, ey, 2, 2, 'k')
        L.px(ex1, ey, 'w'); L.px(ex2, ey, 'w')
    elif eyes:
        L.rect(ex1, ey, 2, 2, eyes); L.rect(ex2, ey, 2, 2, eyes)
    if snout:
        cx = (x0 + x1) // 2
        L.rect(cx - 1, y1 - 3, 4, 3, snout)
        L.px(cx, y1 - 2, 'k'); L.px(cx + 1, y1 - 2, 'k')
    if nose:
        L.px((x0 + x1) // 2, y1 - 3, nose); L.px((x0 + x1) // 2 + 1, y1 - 3, nose)


# --- люди и расы
@motif
def villager(L, **kw):
    face(L, y0=3, y1=13, eyes=None)
    L.rect(4, 6, 8, 1, 'D')
    L.rect(4, 7, 2, 1, 'e'); L.rect(10, 7, 2, 1, 'e')
    L.rect(7, 7, 2, 4, 'F'); L.px(8, 11, 'F')
    L.rect(3, 12, 10, 1, 'B')


@motif
def pillager(L, **kw):
    face(L, y0=3, y1=13, eyes=None)
    L.rect(3, 3, 10, 2, 'G')
    L.line(4, 5, 7, 7, 'k'); L.line(11, 5, 8, 7, 'k')
    L.rect(4, 7, 2, 1, 'k'); L.rect(10, 7, 2, 1, 'k')
    L.rect(7, 7, 2, 4, 'S'); L.rect(5, 12, 6, 1, 'k')


@motif
def witch(L, **kw):
    L.art(4, 0, ["...PP...", "..PPP...", "..PPP...", ".PPPpP..", ".PPPPP.."])
    L.rect(1, 5, 14, 2, 'P'); L.rect(1, 5, 14, 1, 'p')
    L.rect(4, 3, 7, 1, 'y')
    face(L, x0=3, x1=12, y0=7, y1=14, eyes=None)
    L.rect(5, 9, 2, 1, 'k'); L.rect(9, 9, 2, 1, 'k')
    L.rect(7, 10, 2, 3, 'F'); L.px(8, 12, 'N')


@motif
def zombie(L, **kw):
    face(L, y0=3, y1=13, eyes=None)
    L.rect(4, 6, 3, 2, 'k'); L.rect(9, 6, 3, 2, 'k')
    L.px(6, 8, 'N'); L.px(9, 8, 'N')
    L.rect(5, 11, 6, 1, 'k'); L.px(6, 12, 'k'); L.px(9, 12, 'k')
    L.px(3, 4, 'N'); L.px(12, 5, 'N'); L.px(11, 3, 'R'); L.px(4, 10, 'R')


@motif
def skeleton(L, **kw):
    face(L, y0=2, y1=11, eyes=None)
    L.rect(4, 5, 3, 3, 'k'); L.rect(9, 5, 3, 3, 'k')
    L.rect(7, 8, 2, 2, 'k')
    L.rect(4, 12, 8, 2, 'W'); L.rect(4, 13, 8, 1, 'g')
    for x in (5, 7, 9, 11):
        L.px(x, 12, 'g')


sprite('creeper', [
    "nnNnnnNnnNnn",
    "nNnnNnnnnnNn",
    "nnkkknnkkknn",
    "NnkkknnkkkNn",
    "nnkkknnkkknn",
    "nnnnnkknNnnn",
    "nNnnkkkknnNn",
    "nnnkkkkkknnn",
    "nnnkkNNkkNnn",
    "nNnkk..kknNn",
    "nnnnnNnnnnnn",
], y=2)


@motif
def spider(L, **kw):
    for sx in (0, 1):
        for i, (y0, y1) in enumerate(((3, 1), (5, 5), (8, 9), (10, 14))):
            xa, xb = (7, 1) if sx == 0 else (8, 14)
            L.line(xa, 8, xb, y1 + 0, 'G')
    L.disc(8, 9.5, 3.3, 'hms')
    L.disc(8, 5.5, 2.4, 'hms')
    L.px(7, 5, 'r'); L.px(9, 5, 'r'); L.px(8, 4, 'r')
    L.px(7, 10, 'R'); L.px(9, 10, 'R'); L.px(8, 9, 'R')


@motif
def slime(L, **kw):
    L.art(2, 3, [
        "..hhhhhhhh..",
        ".hhhhhhhhhm.",
        "hhhwhhhhhhmm",
        "hhwhhhhhhhmm",
        "hhhhkkhhkkmm",
        "hhhhkkhhkkms",
        "hhhhhhhhhhms",
        "hhhhhkkhhhms",
        "mmmmmmmmmmms",
        ".ssssssssss.",
    ])
    L.px(4, 13, 'm'); L.px(11, 13, 'm')


@motif
def demon(L, **kw):
    face(L, y0=4, y1=13, eyes=None)
    L.art(2, 0, ["y.", "yw", "yw", "ym"]); L.art(12, 0, [".y", "wy", "wy", "my"])
    L.rect(4, 7, 3, 1, 'y'); L.rect(9, 7, 3, 1, 'y'); L.px(5, 8, 'k'); L.px(10, 8, 'k')
    L.line(4, 6, 6, 7, 'k'); L.line(11, 6, 9, 7, 'k')
    L.rect(5, 11, 6, 2, 'k'); L.px(5, 11, 'w'); L.px(10, 11, 'w'); L.px(8, 11, 'w')


@motif
def coldone(L, **kw):
    face(L, y0=4, y1=13, eyes=None)
    L.art(3, 1, ["w...w...w", "ww.www.ww", "wwwwwwwww"])
    L.rect(4, 7, 3, 2, 'w'); L.rect(9, 7, 3, 2, 'w')
    L.rect(5, 11, 6, 1, 'S'); L.px(6, 12, 'w'); L.px(9, 12, 'w')
    L.px(3, 9, 'w'); L.px(12, 10, 'w')


@motif
def snowman(L, **kw):
    L.disc(8, 11, 4.4, 'hms')
    L.disc(8, 5.5, 3.3, 'hms')
    L.rect(5, 0, 6, 2, 'k'); L.rect(4, 2, 8, 1, 'k')
    L.rect(6, 5, 1, 1, 'k'); L.rect(9, 5, 1, 1, 'k')
    L.rect(7, 6, 4, 1, 'o')
    for y in (10, 12):
        L.px(8, y, 'k')
    L.px(3, 10, 'B'); L.px(2, 9, 'B'); L.px(13, 10, 'B'); L.px(14, 9, 'B')


@motif
def fairy(L, **kw):
    L.ellipse(4.5, 6, 3, 4, 'cwc'); L.ellipse(11.5, 6, 3, 4, 'cwc')
    L.rect(6, 3, 4, 4, 'f'); L.px(7, 4, 'k'); L.px(9, 4, 'k')
    L.rect(6, 2, 4, 1, 'y'); L.px(5, 3, 'y'); L.px(10, 3, 'y')
    L.art(5, 7, [".mmmm.", "mmmmmm", ".mmmm.", ".hmms."])
    L.px(7, 12, 'f'); L.px(9, 12, 'f')
    L.px(13, 2, 'y'); L.px(14, 2, 'y'); L.px(13, 1, 'y'); L.px(13, 3, 'y'); L.px(1, 12, 'w'); L.px(14, 13, 'w')


# --- звери
@motif
def sheep(L, **kw):
    L.ellipse(8, 7, 7, 6, 'wWg')
    L.rect(5, 6, 6, 7, 'G'); L.rect(5, 6, 6, 1, 'g')
    L.rect(6, 8, 2, 1, 'w'); L.rect(9, 8, 2, 1, 'w'); L.px(7, 8, 'k'); L.px(10, 8, 'k')
    L.rect(7, 11, 3, 1, 'l')


@motif
def cow(L, **kw):
    face(L, ears='horn', snout='l', y0=4)
    L.rect(2, 5, 4, 4, 'k'); L.rect(10, 9, 3, 4, 'k')
    L.rect(4, 7, 2, 2, 'w'); L.px(5, 7, 'k'); L.px(5, 8, 'k')
    L.rect(10, 7, 2, 2, 'k'); L.px(11, 7, 'w')


@motif
def pig(L, **kw):
    face(L, ears='round', y0=4)
    L.rect(5, 9, 6, 4, 'S'); L.rect(5, 9, 6, 1, 'M')
    L.rect(6, 10, 1, 2, 'k'); L.rect(9, 10, 1, 2, 'k')


@motif
def chicken(L, **kw):
    face(L, y0=4, eyes=None)
    L.art(6, 1, ["..r.", ".rrr", "rrrr"][::1])
    L.rect(4, 7, 2, 2, 'k'); L.rect(10, 7, 2, 2, 'k')
    L.rect(6, 9, 4, 2, 'o'); L.rect(7, 11, 2, 2, 'r'); L.rect(6, 9, 4, 1, 'y')


@motif
def rabbit(L, **kw):
    face(L, ears='long', y0=6, y1=14, snout='l')
    L.rect(7, 12, 2, 1, 'w')


@motif
def horse(L, **kw):
    for y in range(2, 15):
        for x in range(4, 12):
            if y < 4 and x in (4, 11):
                continue
            L.px(x, y, 'h' if x < 6 else 'm' if x < 10 else 's')
    L.art(3, 0, ["h.", "hm"]); L.art(11, 0, [".m", "ms"])
    L.rect(6, 1, 4, 2, 'D'); L.rect(2, 2, 2, 9, 'D'); L.rect(2, 2, 2, 1, 'B')
    L.rect(5, 6, 2, 2, 'k'); L.rect(9, 6, 2, 2, 'k')
    L.rect(5, 11, 6, 4, 'M'); L.px(6, 12, 'k'); L.px(9, 12, 'k')


@motif
def cat(L, **kw):
    face(L, ears='point', y0=5, y1=13, nose=None)
    L.rect(4, 8, 2, 2, 'e'); L.rect(10, 8, 2, 2, 'e'); L.px(5, 8, 'k'); L.px(10, 8, 'k')
    L.px(8, 10, 'l'); L.px(7, 10, 'l')
    L.line(0, 10, 3, 11, 'w'); L.line(0, 13, 3, 12, 'w'); L.line(15, 10, 12, 11, 'w'); L.line(15, 13, 12, 12, 'w')


@motif
def fox(L, **kw):
    face(L, ears='point', y0=5, y1=13)
    L.art(2, 9, ["w", "ww", "wwww", "wwwww"])
    L.art(13, 9, ["w"]); L.art(11, 10, ["ww", "www"]); L.art(9, 11, ["www"]); L.art(7, 12, ["wwww"])
    L.rect(4, 7, 2, 2, 'k'); L.rect(10, 7, 2, 2, 'k')
    L.rect(7, 12, 2, 2, 'k')


@motif
def wolf(L, **kw):
    face(L, ears='point', y0=4, y1=13)
    L.rect(5, 9, 6, 4, 'W'); L.rect(7, 9, 2, 2, 'k'); L.px(7, 12, 'r'); L.px(8, 12, 'r')
    L.rect(4, 6, 2, 2, 'y'); L.rect(10, 6, 2, 2, 'y'); L.px(5, 7, 'k'); L.px(10, 7, 'k')


@motif
def bear(L, **kw):
    face(L, ears='round', y0=4)
    L.rect(5, 9, 6, 4, 't'); L.rect(7, 9, 2, 2, 'k')
    L.px(8, 12, 'k')


@motif
def panda(L, **kw):
    L.art(2, 1, ["kkk", "kkk"]); L.art(11, 1, ["kkk", "kkk"])
    face(L, y0=3, y1=13, eyes=None)
    L.rect(4, 6, 3, 3, 'k'); L.rect(9, 6, 3, 3, 'k')
    L.px(5, 7, 'w'); L.px(10, 7, 'w')
    L.rect(7, 10, 2, 1, 'k'); L.px(7, 12, 'k'); L.px(8, 12, 'k')


@motif
def polarbear(L, **kw):
    face(L, ears='round', y0=4)
    L.rect(5, 9, 6, 4, 'W'); L.rect(7, 9, 2, 1, 'k')
    L.px(8, 12, 'k')
    L.px(3, 7, 'c'); L.px(12, 6, 'c')


@motif
def bee(L, **kw):
    L.ellipse(5, 5, 3, 3, 'wcc'); L.ellipse(10, 4, 3, 3, 'wcc')
    L.ellipse(8, 9, 5.5, 3.8, 'hms')
    L.rect(6, 6, 2, 7, 'k'); L.rect(10, 6, 2, 7, 'k')
    L.rect(2, 8, 2, 2, 'k'); L.px(3, 8, 'w')
    L.px(14, 10, 'k'); L.px(15, 10, 'k')


@motif
def frog(L, **kw):
    face(L, y0=5, y1=13, eyes=None)
    L.rect(2, 2, 4, 4, 'h'); L.rect(10, 2, 4, 4, 'm')
    L.rect(3, 3, 2, 2, 'k'); L.rect(11, 3, 2, 2, 'k'); L.px(3, 3, 'w'); L.px(11, 3, 'w')
    L.rect(4, 10, 8, 1, 'k'); L.rect(5, 11, 6, 2, 'r')
    L.px(7, 8, 'S'); L.px(9, 8, 'S')


@motif
def turtle(L, **kw):
    L.rect(6, 0, 4, 3, 'M'); L.px(7, 1, 'k'); L.px(9, 1, 'k')
    for x, y in ((1, 3), (12, 3), (1, 11), (12, 11)):
        L.rect(x, y, 3, 3, 'M')
    L.ellipse(8, 8, 6, 6, 'hms')
    for x, y in ((6, 5), (9, 5), (5, 8), (8, 8), (11, 8), (6, 11), (9, 11)):
        L.rect(x, y, 2, 2, 'S')
    L.rect(7, 14, 2, 2, 'M')


@motif
def monkey(L, **kw):
    face(L, ears='side', y0=3, y1=13, eyes=None)
    L.art(4, 6, ["ffffffff", "ffffffff", "ffffffff", "ffffffff", ".ffffff."])
    L.rect(5, 7, 2, 1, 'k'); L.rect(9, 7, 2, 1, 'k')
    L.px(8, 9, 'F'); L.rect(6, 11, 4, 1, 'D')
    L.px(1, 4, 'f'); L.px(14, 4, 'f')


@motif
def rhino(L, **kw):
    face(L, ears='point', y0=4, y1=13)
    L.art(6, 0, ["..w", ".ww", ".ww", "www"][::1])
    L.rect(7, 4, 3, 1, 'W')
    L.rect(5, 9, 6, 4, 'S'); L.px(6, 10, 'k'); L.px(9, 10, 'k')
    L.rect(4, 6, 2, 1, 'k'); L.rect(10, 6, 2, 1, 'k')


sprite('camel', [
    "......hhh.hhh.",
    ".....hhhhhhhh.",
    "...hhhhhhmmmmm",
    "..hhhhhhhhmmmm",
    "hhhhhhhhhhhmmm",
    "hhhhhhhhhhmmms",
    "hhhhhhhhhhmmss",
    ".hhhhhhhhhmmss",
    ".mm.mm..mm.mm.",
    ".mm.mm..mm.mm.",
    ".ss.ss..ss.ss.",
], y=2)

sprite('fishside', [
    "....hhh.......",
    "..hhhhhhhh..hh",
    ".hhhhhhhhhmhhm",
    "hhkkhhhhhhmmmm",
    "hwkwhhhhhmmmmm",
    "hhhhhhhhhmmmm.",
    ".wwhmmmmmmmmmm",
    "..mmmmmmmmm.mm",
    ".....mmms.....",
], y=3)

for _n, _r in (('fishside', None),):
    pass


@motif
def piranha(L, **kw):
    MOTIFS['fishside'](L)
    L.rect(1, 9, 5, 2, 'k')
    for x in (1, 3, 5):
        L.px(x, 9, 'w')
    for x in (2, 4):
        L.px(x, 10, 'w')


@motif
def axolotl(L, **kw):
    face(L, y0=4, y1=13, eyes=None)
    for dx, sx in ((0, -1), (13, 1)):
        for j, y in enumerate((4, 6, 8)):
            xx = 0 if dx == 0 else 14
            L.rect(xx, y, 2, 1, 'r')
            L.px(xx + (1 if dx == 0 else 0), y, 'l')
    L.rect(4, 7, 2, 2, 'k'); L.rect(10, 7, 2, 2, 'k'); L.px(4, 7, 'w'); L.px(10, 7, 'w')
    L.px(5, 11, 'R'); L.px(6, 12, 'R'); L.px(7, 12, 'R'); L.px(8, 12, 'R'); L.px(9, 12, 'R'); L.px(10, 11, 'R')


# --- гуманоиды: расы
def human(L, skin_ramp=None, hair='B', ears=None, extra=None):
    face(L, y0=3, y1=13, eyes=None)
    L.rect(2, 3, 12, 3, hair); L.rect(2, 6, 2, 3, hair); L.rect(12, 6, 2, 3, hair)
    L.rect(4, 7, 2, 2, 'k'); L.rect(10, 7, 2, 2, 'k'); L.px(4, 7, 'w'); L.px(10, 7, 'w')
    L.rect(6, 11, 4, 1, 'F')
    if ears == 'elf':
        L.art(0, 6, ["f.", "ff"]); L.art(14, 6, [".f", "ff"]); L.px(0, 5, 'f'); L.px(15, 5, 'f')
    if extra:
        extra(L)


@motif
def race_human(L, **kw):
    human(L)


@motif
def race_elf(L, **kw):
    human(L, hair='y', ears='elf')
    L.px(7, 2, 'e'); L.px(8, 2, 'e')


@motif
def race_orc(L, **kw):
    face(L, y0=3, y1=13, eyes=None)
    L.rect(2, 3, 12, 2, 'k')
    L.rect(4, 6, 2, 2, 'y'); L.rect(10, 6, 2, 2, 'y'); L.px(5, 7, 'k'); L.px(10, 7, 'k')
    L.rect(4, 11, 8, 2, 'k')
    L.rect(4, 9, 2, 3, 'w'); L.rect(10, 9, 2, 3, 'w')
    L.px(7, 9, 'S'); L.px(8, 9, 'S')


@motif
def race_dwarf(L, **kw):
    face(L, y0=3, y1=11, eyes=None)
    L.rect(2, 1, 12, 3, 'G'); L.rect(2, 1, 12, 1, 'g'); L.rect(7, 0, 2, 2, 'y')
    L.rect(4, 6, 2, 2, 'k'); L.rect(10, 6, 2, 2, 'k')
    L.rect(7, 8, 2, 2, 'F')
    L.art(2, 9, ["oooooooooooo", "ooooooooooooo"[:12], ".oooooooooo.", "..oooooooo..", "...oooooo...", "....oooo...."])
    L.rect(6, 10, 4, 1, 'D')


# --- голем, разоритель, титан, иссушитель, драконы
@motif
def golem(L, **kw):
    face(L, y0=2, y1=13, eyes=None)
    L.rect(3, 4, 10, 1, 'k'); L.rect(4, 5, 2, 2, 'R'); L.rect(10, 5, 2, 2, 'R')
    L.rect(7, 6, 2, 5, 'D'); L.px(7, 10, 'B')
    L.rect(4, 12, 8, 1, 'k')
    L.rect(2, 8, 1, 4, 'n'); L.rect(13, 3, 1, 5, 'n'); L.px(3, 10, 'N')


@motif
def ravager(L, **kw):
    face(L, y0=5, y1=14, eyes=None)
    L.art(0, 0, ["w...", "ww..", "www.", "wwwm"][::1]); L.art(12, 0, ["...w", "..ww", ".www", "mwww"])
    L.rect(4, 7, 2, 2, 'R'); L.rect(10, 7, 2, 2, 'R'); L.px(4, 6, 'k'); L.px(11, 6, 'k')
    L.rect(5, 10, 6, 3, 'S'); L.px(6, 11, 'k'); L.px(9, 11, 'k')
    L.rect(4, 12, 1, 3, 'w'); L.rect(11, 12, 1, 3, 'w')


@motif
def titan(L, **kw):
    L.rect(6, 0, 4, 4, 'M'); L.rect(6, 0, 4, 1, 'H')
    L.rect(7, 2, 1, 1, 'R'); L.rect(9, 2, 1, 1, 'R')
    L.rect(3, 4, 10, 4, 'h'); L.rect(3, 4, 10, 1, 'w')
    L.rect(1, 4, 2, 8, 'm'); L.rect(13, 4, 2, 8, 's')
    L.rect(5, 8, 6, 4, 'm'); L.rect(5, 12, 2, 3, 's'); L.rect(9, 12, 2, 3, 's')
    L.rect(7, 5, 2, 3, 'G')


@motif
def wither(L, **kw):
    L.art(5, 2, ["kkkkkk", "kwkkwk", "kkkkkk", "k.kk.k", ".kkkk."][::1])
    L.art(0, 5, ["kkkkk", "kwkwk", "kkkkk", ".k.k."])
    L.art(11, 5, ["kkkkk", "kwkwk", "kkkkk", ".k.k."])
    L.art(2, 10, ["G.G.G.G.G.G.G"[:12]])
    L.rect(7, 9, 2, 5, 'G'); L.rect(4, 11, 8, 1, 'G')


sprite('dragonhead', [
    "..w.......w...",
    "..ww.....ww...",
    "..whhhhhhhm...",
    ".hhhhhhhhhhmm.",
    ".hyhhhhhhhmmmm",
    ".hhhhhhhhhmmmm",
    "hhhhhhhhhmmmmm",
    "hhhhhkhhhmmmmk",
    "hhhhhhhmmmmmmm",
    ".wwhhhhhhmmsm.",
    "..wwwwwwwwsm..",
    "...w.w.w.w....",
], y=2)


@motif
def crab(L, **kw):
    L.art(0, 1, ["rr..", "rrr.", ".rr."]); L.art(12, 1, ["..rr", ".rrr", ".rr."])
    L.art(1, 4, ["R.", ".R"]); L.art(13, 4, [".R", "R."])
    L.ellipse(8, 9, 6, 4, 'hms')
    L.rect(5, 4, 1, 3, 'M'); L.rect(10, 4, 1, 3, 'M'); L.rect(5, 4, 2, 2, 'w'); L.rect(10, 4, 2, 2, 'w')
    L.px(6, 5, 'k'); L.px(11, 5, 'k')
    for x in (2, 4, 10, 12):
        L.px(x, 13, 'S'); L.px(x, 14, 'S')
    L.rect(6, 11, 4, 1, 'k')


@motif
def robot(L, **kw):
    L.rect(7, 0, 2, 2, 'g'); L.px(7, 0, 'r')
    L.rect(2, 2, 12, 9, 'm'); L.rect(2, 2, 12, 1, 'h'); L.rect(2, 2, 1, 9, 'h'); L.rect(2, 10, 12, 1, 's'); L.rect(13, 2, 1, 9, 's')
    L.rect(3, 4, 10, 3, 'k'); L.rect(4, 5, 3, 1, 'r'); L.rect(9, 5, 3, 1, 'r')
    L.rect(4, 8, 8, 2, 'G')
    for x in (5, 7, 9, 11):
        L.px(x, 8, 'k'); L.px(x, 9, 'k')
    L.rect(0, 5, 2, 4, 'G'); L.rect(14, 5, 2, 4, 'G')
    L.rect(4, 11, 8, 3, 'S'); L.rect(6, 12, 4, 1, 'y')


@motif
def kraken(L, **kw):
    L.ellipse(8, 5, 5.5, 4.5, 'hms')
    L.rect(5, 4, 3, 3, 'w'); L.rect(9, 4, 3, 3, 'w'); L.px(6, 5, 'k'); L.px(10, 5, 'k'); L.rect(6, 4, 1, 2, 'k'); L.rect(10, 4, 1, 2, 'k')
    for x, h in ((1, 7), (4, 8), (8, 8), (11, 8), (14, 7)):
        L.rect(x, 9, 2, h - 2, 'm')
    L.line(1, 14, 0, 15, 'm'); L.line(14, 14, 15, 15, 'm')
    L.px(2, 12, 'l'); L.px(5, 12, 'l'); L.px(9, 12, 'l'); L.px(12, 12, 'l')


# --- маги
def wizard(L, hat='P', hat2='p', robe='m', face_c='f', orb='y', staff='B', beard=None, mask=None, hood=False):
    if hood:
        L.art(4, 0, ["..hh..", ".hhhh.", "hhhhhh", "hhhhhh"])
        L.rect(5, 3, 6, 4, 'hms'[0]) if False else None
        L.rect(5, 4, 6, 3, face_c); L.px(6, 5, 'e'); L.px(9, 5, 'e')
    else:
        L.art(5, 0, ["..%%.", ".%%%.", "%%%%%"][::1] if False else [".." + hat2 + "...", "." + hat + hat + hat + "..", hat + hat + hat + hat + hat + hat])
        L.rect(2, 3, 11, 1, hat); L.rect(2, 3, 11, 1, hat2)
        L.rect(5, 4, 6, 3, face_c); L.px(6, 5, 'k'); L.px(9, 5, 'k')
        if beard:
            L.rect(6, 6, 4, 2, beard)
    L.rect(3, 7, 9, 7, robe)
    L.rect(3, 7, 9, 1, 'h' if robe == 'm' else robe); L.rect(11, 8, 1, 6, 's' if robe == 'm' else robe)
    L.rect(13, 3, 1, 12, staff)
    L.rect(12, 1, 3, 3, orb); L.px(12, 1, 'w')


@motif
def necro(L, **kw):
    wizard(L, robe='m', face_c='W', orb='e', hood=True)
    L.rect(6, 5, 1, 1, 'e'); L.rect(9, 5, 1, 1, 'e')
    L.rect(7, 6, 2, 1, 'k')


@motif
def evilmage(L, **kw):
    wizard(L, hat='k', hat2='G', robe='m', orb='r')
    L.rect(6, 6, 4, 1, 'R')


@motif
def whitemage(L, **kw):
    wizard(L, hat='w', hat2='W', robe='m', orb='y', beard='w')


@motif
def druid(L, **kw):
    wizard(L, hat='N', hat2='n', robe='m', orb='e', staff='B', beard='D')
    L.px(4, 9, 'n'); L.px(10, 11, 'n'); L.px(6, 12, 'n')


@motif
def battlemage(L, **kw):
    wizard(L, hat='b', hat2='c', robe='m', orb='c')
    L.px(8, 10, 'y'); L.px(7, 9, 'y'); L.px(8, 9, 'y')


@motif
def plaguedoctor(L, **kw):
    L.rect(3, 0, 9, 2, 'k'); L.rect(1, 2, 13, 1, 'k')
    L.rect(3, 3, 8, 6, 'm'); L.rect(3, 3, 8, 1, 'h')
    L.rect(4, 4, 3, 3, 'w'); L.rect(8, 4, 3, 3, 'w'); L.px(5, 5, 'k'); L.px(9, 5, 'k')
    L.art(6, 6, ["...tt", "..ttT", "..tTT", "...TT"][::1])
    L.art(7, 7, ["tt", "tT", "tT", "TT"])
    L.rect(3, 10, 10, 5, 'k'); L.rect(3, 10, 10, 1, 'G')
    L.px(7, 12, 'n'); L.px(8, 12, 'n')


@motif
def bandit(L, **kw):
    face(L, y0=3, y1=13, eyes=None)
    L.rect(2, 3, 12, 3, 'r'); L.rect(2, 3, 12, 1, 'R'); L.art(12, 5, ["rr", "r."])
    L.rect(2, 7, 12, 3, 'k'); L.rect(4, 8, 2, 1, 'w'); L.rect(10, 8, 2, 1, 'w')
    L.rect(6, 11, 4, 1, 'F'); L.rect(5, 12, 6, 1, 'D')


@motif
def fireelemental(L, **kw):
    MOTIFS['flame'](L)
    L.rect(5, 7, 2, 2, 'k'); L.rect(9, 7, 2, 2, 'k'); L.rect(6, 11, 4, 1, 'k')


@motif
def crystalgolem(L, **kw):
    L.art(2, 0, [
        "......hh......",
        ".....hwhm.....",
        "....hhhhmm....",
        "...hhhhhhmm...",
        "..hhkkhhkkmm..",
        "..hhkkhhkkmm..",
        "..hhhhhhhhms..",
        "...hhhhhhmms..",
        "...hhmmmmmms..",
        "...mhmmmmmss..",
        "....mmmmmss...",
        "....mmsmmss...",
        ".....mmmss....",
        "......mss.....",
    ])


@motif
def fireskull(L, **kw):
    L.shift(0, 0)
    MOTIFS['skull'](L, dy=2)
    L.art(3, 0, [".o...o.o...", "oyo.oyoyo.o"][:1])
    L.art(2, 0, ["..o..o..o...", ".oyo.oyo.oy.", ".oo...o..oo."][:2])


@motif
def jumpyskull(L, **kw):
    MOTIFS['skull'](L, dy=-2)
    L.art(4, 11, ["G.G.G.G", ".G.G.G.", "G.G.G.G"][:3])
    L.rect(3, 14, 10, 1, 'G')
