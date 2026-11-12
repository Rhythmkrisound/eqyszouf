"""Мотивы: символы, чудеса, биомы-сцены, значки."""
from lib import Layer, text
from motifs_a import motif, sprite, MOTIFS, BADGES, star8

sprite('crown', [
    ".h...h...h.",
    ".hh.hhh.hh.",
    ".hhhhhhhhhm",
    ".hhhhhhhhmm",
    ".hrhhbhhrhm",
    ".hhhhhhhhmm",
    ".mmmmmmmmms",
    ".ssssssssss",
], y=4)

sprite('sword', [
    "..........wwg",
    ".........wwwg",
    "........wwwg.",
    ".......wwwg..",
    "......wwwg...",
    ".Y...wwwg....",
    "..Y.wwwg.....",
    "...YYwg......",
    "....YYB......",
    "...BBBY......",
    "..BB..Y......",
    ".BB..........",
])

sprite('shield', [
    "hhhhhhhhhhhh",
    "hmmmmmmmmmmm",
    "hmmmwwwmmmms",
    "hmmmwwwmmmms",
    "hmwwwwwwwmms",
    "hmmmwwwmmmms",
    ".hmmwwwmmms.",
    ".hmmmwmmmms.",
    "..hmmmmmms..",
    "...hmmmms...",
    "....mmms....",
    ".....ms.....",
], y=2)

sprite('heart', [
    ".hhh..hhh.",
    "hhhhhhhhhm",
    "hwhhhhhhmm",
    "hhhhhhhhmm",
    ".hhhhhhmm.",
    "..hhhhmm..",
    "...hhmm...",
    "....mm....",
], y=4)

sprite('crackedheart', [
    ".hhh..hhh.",
    "hhhhhhhhhm",
    "hwhhkhhhmm",
    "hhhkhhhhmm",
    ".hhhkhhmm.",
    "..hhhkmm..",
    "...hkhm...",
    "....km....",
], y=4)


@motif
def hearts(L, **kw):
    MOTIFS['heart'](L, dx=-3, dy=-3)
    L.art(7, 7, [".hh.hh.", "hhhhhhm", "hhhhhhm", ".hhhhm.", "..hhm..", "...m..."])


sprite('star', [
    ".....hh.....",
    ".....hh.....",
    "....hhmm....",
    "hhhhhhmmmmmm",
    ".hhhhmmmmms.",
    "..hhhmmmms..",
    "..hhmmmmms..",
    ".hhhm..mmms.",
    ".hhm....mms.",
    ".hm......ms.",
])


@motif
def sparkle(L, **kw):
    L.art(2, 2, [
        ".....hh.....",
        ".....hh.....",
        "....hwwm....",
        "....hwwm....",
        "...hhwwmm...",
        "hhhhwwwwmmmm",
        "hhhhwwwwmmmm",
        "...hhwwmm...",
        "....hwwm....",
        "....hwwm....",
        ".....mm.....",
        ".....mm.....",
    ])
    L.px(13, 2, 'w'); L.px(12, 3, 'w'); L.px(14, 3, 'w'); L.px(13, 4, 'w')
    L.px(2, 13, 'w'); L.px(1, 14, 'w'); L.px(3, 14, 'w'); L.px(2, 15, 'w')


@motif
def cross(L, **kw):
    L.rect(5, 1, 6, 14, 'm'); L.rect(1, 5, 14, 6, 'm')
    L.rect(5, 1, 6, 1, 'h'); L.rect(1, 5, 14, 1, 'h')
    L.rect(10, 5, 1, 10, 's'); L.rect(5, 14, 6, 1, 's'); L.rect(1, 10, 14, 1, 's')
    L.rect(6, 6, 4, 4, 'h')


@motif
def angry(L, **kw):
    L.disc(8, 8, 6.8, 'hms')
    L.rect(3, 5, 4, 1, 'k'); L.rect(9, 5, 4, 1, 'k'); L.line(3, 4, 6, 6, 'k'); L.line(12, 4, 9, 6, 'k')
    L.rect(4, 7, 2, 2, 'w'); L.rect(10, 7, 2, 2, 'w'); L.px(5, 8, 'k'); L.px(10, 8, 'k')
    L.rect(5, 11, 6, 2, 'k'); L.px(6, 11, 'w'); L.px(9, 11, 'w')


@motif
def fist(L, **kw):
    L.art(1, 3, [
        ".hhhhhhhhh.",
        "hhhhhhhhhhm",
        "hhhhhhhhhhm",
        "hmmmmmmmmmm",
        "hhhhhhhhhhm",
        "hhhhhhhhhhm",
        ".hhhhhhhhms",
        ".hhhhhhhmms",
        "..mmmmmmmss",
    ])
    L.rect(2, 12, 10, 3, 'D')
    L.rect(2, 12, 10, 1, 'B')


@motif
def chevrons(L, n=2, **kw):
    for k in range(n):
        L.art(2 + k * 6, 3, ["hh....", ".hh...", "..hh..", "...hh.", "....hh", "...mm.", "..mm..", ".mm...", "mm...."])


@motif
def armor(L, **kw):
    L.art(1, 1, [
        "hhh..hhhh..hhh",
        "hhhhhhhhhhhhhm",
        "hhhhhhhhhhhhmm",
        ".hhhhhhhhhhmm.",
        ".hhhhmmmmhhmm.",
        ".hhhmhhhhmhmm.",
        ".hhhmhhhhmhms.",
        ".hhhhmmmmhhms.",
        ".hhhhhhhhhhms.",
        ".hhhhhhhhhmms.",
        ".mmmmmmmmmmss.",
    ])


@motif
def person(L, big=False, **kw):
    if big:
        L.rect(5, 0, 6, 5, 'f'); L.rect(5, 0, 6, 1, 'D')
        L.rect(3, 5, 10, 6, 'm'); L.rect(1, 5, 2, 7, 'f'); L.rect(13, 5, 2, 7, 'F')
        L.rect(4, 11, 3, 5, 'S'); L.rect(9, 11, 3, 5, 'S')
    else:
        L.rect(6, 4, 4, 4, 'f'); L.rect(6, 4, 4, 1, 'D')
        L.rect(5, 8, 6, 4, 'm'); L.rect(4, 8, 1, 4, 'f'); L.rect(11, 8, 1, 4, 'F')
        L.rect(5, 12, 2, 3, 'S'); L.rect(9, 12, 2, 3, 'S')


@motif
def ankh(L, **kw):
    L.art(4, 0, [".hhhh.", "hh..hh", "h....m", "hh..mm", ".hhmm."])
    L.rect(7, 5, 2, 10, 'm'); L.rect(7, 5, 1, 10, 'h'); L.rect(3, 8, 10, 2, 'm'); L.rect(3, 8, 10, 1, 'h')


@motif
def pause(L, **kw):
    L.rect(3, 3, 4, 10, 'm'); L.rect(9, 3, 4, 10, 'm')
    L.rect(3, 3, 4, 1, 'h'); L.rect(9, 3, 4, 1, 'h'); L.rect(3, 3, 1, 10, 'h'); L.rect(9, 3, 1, 10, 'h')
    L.rect(6, 3, 1, 10, 's'); L.rect(12, 3, 1, 10, 's')


@motif
def timeicon(L, label='x1', tri=1, **kw):
    for k in range(tri):
        ox = 1 + k * 5 if tri > 1 else 5
        L.art(ox, 0, ["h...", "hh..", "hhh.", "hhhh", "mmm.", "mm..", "m..."])
    text(L, 3 if len(label) <= 2 else 2, 10, label)


@motif
def cluster(L, **kw):
    for x, y in ((4.5, 10), (11, 10), (7.8, 5)):
        L.disc(x, y, 3.2, 'hms')
        L.px(x + 1, y - 3, 'y')
        L.px(x - 1, y - 1, 'w')


@motif
def quake(L, glow=False, **kw):
    for j in range(12):
        for i in range(12):
            L.px(2 + i, 3 + j, 'h' if j == 0 or i == 0 else 's' if j == 11 or i == 11 else 'm')
    pts = [(8, 3), (7, 5), (9, 7), (6, 9), (8, 11), (7, 14)]
    for a, b in zip(pts, pts[1:]):
        L.line(a[0], a[1], b[0], b[1], 'o' if glow else 'k')
        L.line(a[0] + 1, a[1], b[0] + 1, b[1], 'y' if glow else 'k')
    for x, y in ((0, 6), (0, 9), (15, 6), (15, 9)):
        L.px(x, y, 'w')


@motif
def lantern(L, **kw):
    L.rect(7, 0, 2, 2, 'G'); L.rect(4, 2, 8, 2, 'G'); L.rect(4, 12, 8, 2, 'G')
    L.rect(5, 4, 6, 8, 'y'); L.rect(7, 6, 2, 4, 'w')


# --- чудеса
sprite('library', [
    "...hhhhhhhh...",
    "..hhhhhhhhhm..",
    ".hhhhhhhhhhmm.",
    "wwwwwwwwwwwwww",
    ".wBBwwBBwwBBw.",
    ".wBrwwByywwBb.",
    ".wBBwwBBwwBBw.",
    ".wBrwwByywwBb.",
    ".wBBwwBBwwBBw.",
    "WWWWWWWWWWWWWW",
], y=3)

sprite('lighthouse', [
    "...yyyy...",
    "..oyyyyo..",
    "..kkkkkk..",
    "...rrrr...",
    "...wwww...",
    "...rrrr...",
    "...wwww...",
    "..rrrrrr..",
    "..wwwwww..",
    ".GGGGGGGG.",
], x=3, y=2)

sprite('colossus', [
    "....yyyyy.....",
    "...yyYyyYy....",
    "...yyyyyyy....",
    "....yyyyy.....",
    "..yyyyyyyyy...",
    ".yy.yyyyy.yy..",
    ".y..yyyyy..y..",
    "....yyyyy.....",
    "...yy...yy....",
    "...yy...yy....",
    "..yyy...yyy...",
    "TTTTTTTTTTTTTT",
], y=2)


@motif
def gardens(L, **kw):
    L.rect(1, 12, 14, 3, 'T'); L.rect(2, 9, 12, 3, 't'); L.rect(4, 6, 8, 3, 'T')
    for x, y, c in ((2, 10, 'n'), (5, 10, 'r'), (9, 10, 'n'), (12, 10, 'y'), (5, 7, 'n'), (10, 7, 'r')):
        L.rect(x, y - 1, 2, 2, c)
    L.art(5, 0, [".nn.nn.", "nnnnnnn", "nNnnNnn", ".nnnnn.", "..nNn.."][:5])
    L.rect(7, 5, 1, 1, 'B')
    L.px(2, 12, 'n'); L.px(13, 12, 'n'); L.px(6, 13, 'N'); L.px(10, 13, 'N')


sprite('cart', [
    "......tttt......",
    ".....tttttt.....",
    "..BBBBBBBBBBBB..",
    "..BtttttttttB...",
    "..BtTtTtTtTtB...",
    "..BBBBBBBBBBBB..",
], x=0, y=5)


@motif
def caravan(L, **kw):
    MOTIFS['cart'](L)
    L.disc(4.5, 13, 2.3, 'DBD'); L.disc(11.5, 13, 2.3, 'DBD')
    L.px(4, 13, 'y'); L.px(11, 13, 'y')


@motif
def handshake(L, **kw):
    L.rect(0, 5, 4, 6, 'b'); L.rect(12, 5, 4, 6, 'r')
    L.art(3, 4, ["fffffffff", "fffffffff", "FfffFffff"[:9], "fffffffff", ".FFfffFF.", "...FFF..."])
    L.rect(5, 6, 1, 3, 'F'); L.rect(8, 6, 1, 3, 'F')


# --- сцены биомов
def scene(L, sky, sky2, g1, g2, deco, hz=9):
    for y in range(1, 15):
        for x in range(1, 15):
            if x in (1, 14) and y in (1, 14):
                continue
            if y < hz:
                L.px(x, y, sky if y < 5 else sky2)
            else:
                L.px(x, y, g1 if y == hz else g2 if (x * 3 + y) % 7 else g1)
    deco(L)


def d_grass(L):
    L.rect(3, 7, 1, 3, 'N'); L.rect(2, 6, 3, 2, 'r'); L.px(3, 6, 'y')
    L.rect(11, 6, 1, 4, 'N'); L.rect(10, 5, 3, 2, 'y'); L.px(11, 5, 'w')
    L.disc(7.5, 5, 1.8, 'www'); L.px(6, 5, 'W')


def d_jungle(L):
    L.rect(7, 2, 3, 8, 'B'); L.rect(4, 1, 9, 3, 'N'); L.rect(3, 2, 11, 2, 'n'); L.rect(4, 4, 2, 4, 'n'); L.rect(11, 4, 2, 5, 'n')
    L.px(12, 6, 'r'); L.px(12, 7, 'y')


def d_savanna(L):
    L.rect(7, 5, 2, 5, 'D'); L.rect(2, 3, 12, 2, 'N'); L.rect(4, 2, 8, 1, 'n'); L.rect(2, 3, 12, 1, 'n')
    L.disc(13, 3, 1.2, 'yyy')


def d_desert(L):
    L.rect(7, 4, 3, 6, 'n'); L.rect(4, 5, 2, 3, 'n'); L.rect(5, 7, 3, 1, 'n'); L.rect(11, 3, 2, 3, 'n'); L.rect(9, 5, 3, 1, 'n')
    L.px(8, 4, 'N'); L.px(8, 6, 'N'); L.disc(3, 3, 1.6, 'yyy')


def d_swamp(L):
    L.rect(3, 5, 1, 5, 'D'); L.rect(2, 4, 3, 1, 'D'); L.rect(4, 6, 2, 1, 'D')
    L.rect(7, 11, 5, 2, 'e'); L.rect(8, 11, 2, 1, 'n'); L.px(10, 12, 'w')
    L.rect(11, 3, 2, 2, 'G'); L.rect(10, 4, 4, 1, 'G')


def d_perma(L):
    for x, h in ((3, 4), (6, 6), (10, 5)):
        L.rect(x, 9 - h, 2, h, 'c'); L.px(x, 9 - h - 1, 'w'); L.px(x, 9 - h, 'w')
    L.px(12, 3, 'w'); L.px(2, 4, 'w'); L.px(8, 2, 'w')


def d_mush(L):
    L.rect(7, 5, 2, 4, 't'); L.rect(4, 2, 8, 3, 'r'); L.rect(5, 1, 6, 1, 'r'); L.px(6, 3, 'w'); L.px(10, 2, 'w'); L.px(8, 3, 'w')
    L.rect(2, 7, 1, 2, 't'); L.rect(1, 6, 3, 1, 'p'); L.rect(12, 7, 1, 2, 't'); L.rect(11, 6, 3, 1, 'p')


def d_candy(L):
    L.rect(4, 4, 1, 6, 'w'); L.disc(4.5, 3.5, 2.2, 'rwr'); L.px(4, 3, 'w')
    for i in range(5):
        L.px(11 + (1 if i < 1 else 0), 4 + i, 'r' if i % 2 else 'w')
    L.rect(11, 4, 1, 6, 'r'); L.px(11, 5, 'w'); L.px(11, 7, 'w'); L.px(12, 3, 'r'); L.px(13, 4, 'r')
    L.px(8, 7, 'y'); L.px(7, 8, 'p')


def d_ench(L):
    L.rect(7, 4, 2, 6, 'B'); L.disc(8, 4, 3.2, 'pcp'); L.px(7, 3, 'w')
    for x, y in ((3, 3), (12, 5), (4, 7), (11, 2), (2, 6)):
        L.px(x, y, 'y')


def d_crystal(L):
    for x, h, c in ((3, 5, 'c'), (6, 7, 'p'), (9, 6, 'c'), (11, 4, 'p')):
        L.rect(x, 9 - h, 2, h, c); L.px(x, 9 - h, 'w'); L.px(x, 8 - h, c)


def d_infernal(L):
    L.art(4, 2, ["..y..", ".oyo.", "ooyoo", "oyyyo", "ooyoo"])
    L.px(11, 5, 'o'); L.px(2, 6, 'o'); L.px(12, 4, 'y')


def d_corrupt(L):
    L.rect(7, 2, 2, 7, 'k'); L.rect(5, 3, 1, 3, 'k'); L.rect(10, 4, 1, 3, 'k'); L.rect(3, 6, 2, 1, 'k')
    L.rect(6, 4, 4, 2, 'P'); L.px(7, 4, 'r'); L.px(9, 4, 'r')
    L.px(12, 2, 'p'); L.px(3, 3, 'p')


SCENES = {
    'grassland': ('c', 'w', 'n', 'N', d_grass, 9),
    'jungle': ('N', 'n', 'N', 'D', d_jungle, 10),
    'savanna': ('y', 'o', 'Y', 'T', d_savanna, 10),
    'desert': ('c', 'y', 'y', 't', d_desert, 10),
    'swamp': ('N', 'A', 'D', 'N', d_swamp, 9),
    'permafrost': ('b', 'c', 'w', 'W', d_perma, 9),
    'mushroom': ('P', 'p', 'g', 'G', d_mush, 9),
    'candy': ('l', 'w', 'p', 'l', d_candy, 9),
    'enchanted': ('P', 'b', 'N', 'P', d_ench, 10),
    'crystal': ('G', 'k', 'G', 'k', d_crystal, 10),
    'infernal': ('R', 'r', 'o', 'R', d_infernal, 10),
    'corrupted': ('k', 'P', 'p', 'P', d_corrupt, 10),
}


@motif
def biome(L, kind='grassland', **kw):
    s = SCENES[kind]
    scene(L, s[0], s[1], s[2], s[3], s[4], s[5])


# --- мини-масштабы суши для генерации
@motif
def landmass(L, kind='continent', **kw):
    for y in range(1, 15):
        for x in range(1, 15):
            if x in (1, 14) and y in (1, 14):
                continue
            L.px(x, y, 'b' if (x + y * 2) % 7 else 'c')
    def blob(cx, cy, rx, ry, r1='n', r2='N'):
        L.ellipse(cx, cy, rx, ry, (r1, r1, r2), light=True)
    if kind == 'continent':
        blob(8, 8, 5.5, 4.5)
        L.rect(6, 6, 2, 2, 'B'); L.px(10, 9, 'B')
    elif kind == 'islands':
        blob(5, 5, 2.5, 2.2); blob(11, 6, 2.2, 2); blob(7, 11, 3, 2.2)
    elif kind == 'archi':
        for x, y in ((4, 4), (8, 3), (12, 5), (5, 9), (10, 10), (7, 13), (13, 12)):
            L.rect(x, y, 2, 2, 'n'); L.px(x, y, 'y')
    elif kind == 'pangaea':
        blob(8, 8, 7, 6.2, 'n', 'N')
        L.rect(5, 5, 2, 2, 'B'); L.rect(9, 9, 3, 2, 'B'); L.rect(8, 5, 2, 1, 'w')
    elif kind == 'mountains':
        for y in range(1, 15):
            for x in range(1, 15):
                if x in (1, 14) and y in (1, 14):
                    continue
                L.px(x, y, "c")
        for x, h in ((4, 7), (8, 10), (12, 6)):
            for j in range(h):
                half = int(j * 0.6)
                for xx in range(x - half, x + half + 1):
                    L.px(xx, 12 - h + j, "w" if j < 3 else "g" if xx <= x else "G")
        L.rect(1, 12, 14, 3, "n")
        L.rect(2, 14, 12, 1, "N")
    elif kind == 'random':
        blob(5, 6, 3, 3); L.rect(9, 8, 4, 4, 'y'); L.px(10, 9, 'k'); L.px(12, 11, 'k'); L.px(11, 10, 'k')


# --- значки 5x5
BADGES.update({
    'plus': ["..e..", "..e..", "eeeee", "..e..", "..e.."],
    'minus': [".....", ".....", "rrrrr", ".....", "....."],
    'up': ["..e..", ".eee.", "eeeee", "..e..", "..e.."],
    'down': ["..r..", "..r..", "rrrrr", ".rrr.", "..r.."],
    'x': ["r...r", ".r.r.", "..r..", ".r.r.", "r...r"],
    'check': ["....e", "...ee", "e.ee.", "eee..", ".e..."],
    'star': ["..y..", "..y..", "yyyyy", ".yyy.", ".y.y."],
    'heart': [".r.r.", "rrrrr", "rrrrr", ".rrr.", "..r.."],
    'drop': ["..b..", ".bb..", ".bbb.", "bbbbb", ".bbb."],
    'flame': ["..o..", ".oo..", ".ooo.", "oyyoo", ".oyo."],
    'skull': [".www.", "wkwkw", "wwwww", ".www.", ".w.w."],
    'clock': [".www.", "wwkww", "wwkkw", "wwwww", ".www."],
    'question': [".yyy.", "y...y", "..yy.", ".....", "..y.."],
    'bang': ["..r..", "..r..", "..r..", ".....", "..r.."],
    'bolt': ["..yy.", ".yy..", "yyyyy", "..yy.", ".yy.."],
    'snow': ["w.w.w", ".www.", "wwwww", ".www.", "w.w.w"],
    'gear': ["g.g.g", ".ggg.", "ggkgg", ".ggg.", "g.g.g"],
    'sparkle': ["..y..", "..y..", "yywyy", "..y..", "..y.."],
    'crown': ["y.y.y", "yyyyy", "yryby", "yyyyy", "....."],
    'law': ["ttttt", "tDDDt", "ttttt", "tDDDt", "TTTTT"],
    'shield': ["bbbbb", "bwwwb", "bwwwb", ".bwb.", "..b.."],
    'coin': [".yyy.", "yYyYy", "yYyYy", "yyyyy", ".yyy."],
    'leaf': ["...nn", "..nnn", ".nnn.", "nnn..", "n...."],
    'arrowr': ["..y..", "...y.", "yyyyy", "...y.", "..y.."],
    'eye': [".....", ".www.", "wkkkw", ".www.", "....."],
    'bomb': ["...yo", "..kk.", ".kkkk", ".kkkk", "..kk."],
    'virus': ["g.g.g", ".ggg.", "ggrgg", ".ggg.", "g.g.g"],
    'wave': [".....", ".bb..", "b..bb", ".....", "....."],
    'swordb': ["....w", "...w.", "Y.w..", ".Yw..", "B.Y.."],
    'home': ["..r..", ".rrr.", "rrrrr", ".ttt.", ".tkt."],
    'bubble': ["..c..", ".c.c.", "c...c", ".c.c.", "..c.."],
    'cycle': [".yyy.", "y...y", "y..yy", "y...y", ".yy.."],
})
