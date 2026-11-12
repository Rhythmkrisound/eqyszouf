"""Мотивы: природа, земля, постройки, государство, предметы."""
from lib import Layer
from motifs_a import motif, sprite, MOTIFS, star8


# --- растения
sprite('tree', [
    "...hhhhh...",
    "..hhhhhhhm.",
    ".hhhhhhhhmm",
    ".hhhhhhhmms",
    "..hhhhmmms.",
    "...mmmmms..",
    "....BBD....",
    "....BBD....",
    "....BBD....",
    "...BBBDD...",
], y=3)

sprite('spruce', [
    ".....h.....",
    "....hhm....",
    "...hhhmm...",
    "....hhmm...",
    "...hhhhmm..",
    "..hhhhmmms.",
    "....hhmm...",
    "..hhhhhmms.",
    ".hhhhhmmmms",
    "....BBD....",
    "....BBD....",
], y=2)

sprite('bigtree', [
    "..hhhhhhhhhh..",
    ".hhhhhhhhhhmm.",
    "hhhhhhhhhhhmmm",
    "hhhhhhhhhhmmms",
    ".hhhhhhhmmmms.",
    "..mmmmmmmmms..",
    "....BBBDD.....",
    "....BBBDD.....",
    "....BBBDD.....",
    "....BBBDD.....",
    "..BBBBBDDDD...",
], y=2)

sprite('megaspruce', [
    "......h......",
    ".....hhm.....",
    "....hhhmm....",
    ".....hhmm....",
    "....hhhhmm...",
    "...hhhhmmms..",
    "....hhhhmm...",
    "...hhhhhmms..",
    "..hhhhhmmmms.",
    "...hhhhhmms..",
    ".hhhhhhhmmmms",
    "......BBD....",
    "......BBD....",
], y=1)

sprite('megajungle', [
    "..hhhhhhhhhh..",
    ".hhhhwhhhhhmm.",
    "hhhhhhhhhhhmmm",
    "hhhhhhhhhhmmms",
    ".hhhhhmhhmmms.",
    "...mmmmmmmms..",
    ".....BBBDD....",
    ".....BBBDD....",
    ".....BBBDD....",
    "....BBBBDDD...",
    "...BBBBBDDDD..",
], y=2)

sprite('sapling', [
    "....hh..hh..",
    "...hhhmhhhm.",
    "...hhhhhmmm.",
    "....hhhmmm..",
    ".....hhmm...",
    "......Bm....",
    "......BD....",
    ".....BBDD...",
], y=4)


@motif
def forest(L, **kw):
    MOTIFS['spruce'](L, dx=-4, dy=1)
    MOTIFS['tree'](L, dx=3, dy=1)
    MOTIFS['spruce'](L, dx=-1, dy=3)


@motif
def bush(L, **kw):
    L.ellipse(8, 10, 6.5, 4.5, 'hms')
    for x, y in ((4, 8), (8, 7), (11, 10), (6, 11), (9, 12), (3, 11)):
        L.rect(x, y, 2, 2, 'r'); L.px(x, y, 'l')


@motif
def flowers(L, **kw):
    for x, h, c in ((3, 7, 'r'), (8, 9, 'y'), (12, 6, 'p')):
        L.rect(x, 15 - h, 1, h - 1, 'N')
        L.rect(x - 1, 14 - h, 3, 3, c); L.px(x, 15 - h, 'w' if c != 'y' else 'o')
        L.px(x, 13 - h, c); L.px(x, 16 - h, c)
    L.rect(0, 14, 16, 1, 'n')


@motif
def grasstuft(L, **kw):
    L.art(1, 5, ["..h...h...h..", ".hh..hh..hh..", ".hm.hhm..hm.h", "hhm.hmm.hhm.h", "hmm.hmm.hmmhm", "hmmhhmmhhmmhm", "mmmmmmmmmmmmm"][::1])
    L.rect(0, 12, 16, 3, 'B'); L.rect(0, 12, 16, 1, 'n')
    L.px(2, 13, 'D'); L.px(7, 14, 'D'); L.px(12, 13, 'D')
    L.px(4, 4, 'y'); L.px(11, 3, 'r')


@motif
def fertilizer(L, **kw):
    L.art(3, 4, [".tttttt.", "tttttttt", "ttwwwwtt", "ttwkkwtt", "ttwwwwtt", "tTTTTTTt", ".TTTTTT."])
    L.rect(4, 2, 6, 2, 'T')
    star8(L, 12, 3, 2, 0, 'n', 'w')
    L.px(2, 11, 'n'); L.px(13, 11, 'n'); L.px(8, 13, 'n')


@motif
def seedlife(L, **kw):
    L.ellipse(8, 10, 4.5, 5, 'hms')
    L.art(7, 3, ["..n", ".nn", "nn."])
    L.rect(7, 5, 1, 2, 'N')
    star8(L, 3, 3, 2, 0, 'y', 'w'); star8(L, 13, 6, 1, 0, 'y', 'w')
    L.px(6, 9, 'w'); L.px(6, 10, 'w')


# --- земля
def block(L, top='n', topS='N', side='B', sideS='D', x=2, y=2, w=12, h=12, topH=3):
    for j in range(h):
        for i in range(w):
            if j < topH:
                c = top if (i + j) % 5 else topS
                if j == topH - 1 and i % 3 == 1:
                    c = side
            else:
                c = side if (i * 7 + j * 3) % 5 else sideS
            L.px(x + i, y + j, c)


@motif
def dirt(L, up=None, **kw):
    block(L, y=3, h=11)


@motif
def waterblock(L, **kw):
    for j in range(11):
        for i in range(12):
            L.px(2 + i, 3 + j, 'c' if (i + j * 2) % 6 < 1 else 'h' if j < 3 else 'm' if j < 8 else 's')


@motif
def sponge(L, **kw):
    for j in range(12):
        for i in range(12):
            L.px(2 + i, 2 + j, 'y' if (i * 5 + j * 3) % 7 else 'Y')
    for x, y in ((4, 4), (8, 5), (6, 8), (10, 10), (4, 11), (9, 8)):
        L.rect(x, y, 2, 2, 'D' if False else 'Y'); L.px(x, y, 'k')


sprite('sandpile', [
    ".......hh.....",
    "......hhhm....",
    ".....hhhhmm...",
    "....hhhhhmms..",
    "...hhhhhmmmms.",
    "..hhhhhmmmmmms",
    ".hhhhhmmmmmmmm",
    "hhhhhmmmmmmmss",
], y=6)

sprite('road', [
    ".......tt.....",
    "......tttT....",
    "......ttTT....",
    ".....ttttT....",
    ".....ttTTT....",
    "....ttttTT....",
    "....ttTTTT....",
    "...tttttTT....",
    "...ttTTTTT....",
    "..ttttttTT....",
    "..ttTTTTTT....",
    ".tttttttTT....",
], y=2)


@motif
def roadwide(L, **kw):
    for y in range(2, 15):
        w = 1 + (y - 2) // 2
        for x in range(8 - w, 8 + w + 1):
            c = 'h' if (x + y) % 4 else 's'
            L.px(x, y, c)
        if y % 3 == 0:
            L.px(8, y, 'y')


@motif
def wall(L, **kw):
    rows = 4
    for j in range(rows):
        for i in range(14):
            seam = (i + (j % 2) * 3) % 6 == 0
            c = 's' if seam else 'h' if (j == 0) else 'm'
            L.px(1 + i, 4 + j * 3, c); L.px(1 + i, 5 + j * 3, c); L.px(1 + i, 6 + j * 3, 's' if j < rows - 1 else 's')
    for i in range(0, 14, 4):
        L.rect(1 + i, 1, 3, 3, 'h'); L.rect(1 + i, 1, 3, 1, 'h')


@motif
def hammer(L, **kw):
    L.rect(2, 1, 8, 4, 'g'); L.rect(2, 1, 8, 1, 'w'); L.rect(2, 4, 8, 1, 'G')
    L.thick(8, 5, 3, 14, 'B', 'D')


# --- постройки
@motif
def house(L, wall='t', roof='m', door='B', **kw):
    L.art(1, 1, ["......hh......", "....hhhhhh....", "..hhhhhhhhhh..", ".hhhhhhhhhhhs.", "hhhhhhhhhhhhss"][::1])
    L.rect(2, 6, 12, 8, wall); L.rect(13, 6, 1, 8, 'T')
    L.rect(6, 9, 3, 5, door); L.rect(3, 8, 2, 2, 'c'); L.rect(10, 8, 2, 2, 'c')


@motif
def village(L, wall='t', roof='r', **kw):
    for ox, oy, w in ((0, 5, 7), (8, 6, 7), (4, 9, 7)):
        L.rect(ox, oy + 2, w, 3 if oy < 9 else 4, wall)
        for k in range(3):
            L.rect(ox + 1 - k + (0 if k else 0), oy + k, w - 2 + 2 * k, 1, roof)
        L.rect(ox + 1, oy, w - 2, 1, roof)
        L.rect(ox + w // 2, oy + 3, 2, 2, 'B')


sprite('pyramid', [
    ".......hh.....",
    "......hhmm....",
    ".....hhhmm....",
    "....hhhhmms...",
    "...hhhhhmmms..",
    "..hhhhhhmmmms.",
    ".hhhhhhhhmmmms",
    "hhhhhhhhhmmmss",
    "mmmmmmmmmmmmss",
], y=5)

sprite('temple', [
    ".....hhhh.....",
    "....hhhhhm....",
    "...hhhhhhmm...",
    "..hhhhhhhhmm..",
    "..hhhhhhhhms..",
    ".hhhhhhhhhhms.",
    ".hh.hhhhhh.ms.",
    ".hh.hhhhhh.ms.",
    ".hh.kkkkkk.ms.",
    ".hh.kkkkkk.ms.",
], y=3)

sprite('hut', [
    "......PP......",
    ".....PPPp.....",
    "....PPPPpp....",
    "..PPPPPPPPpp..",
    "....BBBBBB....",
    "...BBBBBBBD...",
    "...BByyBBBD...",
    "...BBkkBBBD...",
    "...BBkkBBBD...",
    "..B.B..B..D.B.",
], y=4)

sprite('igloo', [
    "....wwwwww....",
    "..wwwwwwwwww..",
    ".wwWwwwwWwwWw.",
    ".wwwwwwwwwwwwm",
    "wWwwwWwwwwWwwm",
    "wwwwwwkkkwwwwm",
    "wwWwwkkkkkwwWm",
    "mmmmmkkkkkmmmm",
], y=6)


@motif
def portal(L, **kw):
    L.rect(2, 1, 12, 14, 'S'); L.rect(2, 1, 12, 1, 'M')
    L.rect(5, 4, 6, 9, 'p'); L.rect(6, 5, 4, 7, 'P'); L.rect(7, 7, 2, 3, 'k')
    L.rect(2, 1, 4, 3, 'k'); L.rect(10, 10, 4, 5, 'k'); L.px(5, 4, 'k')
    L.px(12, 7, 'p'); L.px(3, 12, 'p'); L.px(4, 9, 'R')


sprite('mansion', [
    "......kkkk......",
    ".....kGGGGk.....",
    "..kkkGGGGGGkkk..",
    ".kGGGGGGGGGGGGk.",
    "kGGGGGGGGGGGGGGk",
    ".BBBBBBBBBBBBBB.",
    ".BcBcBBBBBBcBcB.",
    ".BBBBBBkkBBBBBB.",
    ".BcBcBBkkBBcBcB.",
    ".BBBBBBkkBBBBBB.",
], x=0, y=3)


@motif
def geode(L, **kw):
    L.ellipse(8, 9, 6.5, 5.5, 'hms', light=False)
    L.ellipse(8, 9, 4.7, 3.8, 'HMS')
    L.art(5, 6, ["..p.p..", ".ppwppp", "pppppppp"[:7], ".pppPpp"])
    L.rect(7, 6, 1, 2, 'w')


sprite('tower', [
    "w.w.w.w.",
    "wwwwwwww",
    ".hhhhhh.",
    ".hmhhmh.",
    ".hhkkhh.",
    ".hhkkhh.",
    ".hhhhhh.",
    ".hmhhmh.",
    ".hhhhhs.",
    ".hhkkhs.",
    "hhhhhhss",
], x=4, y=3)


@motif
def anchor(L, **kw):
    L.ring(r=1.6) if hasattr(L, 'ring') else None
    L.rect(7, 1, 2, 2, 'g'); L.rect(7, 3, 2, 10, 'h'); L.rect(5, 4, 6, 2, 'h')
    L.art(2, 9, ["h.....", "hh....", ".hhhhh"]); L.art(8, 9, ["....hh"[::-1], "....hh", "hhhhh."])
    L.rect(2, 10, 12, 1, 'm'); L.rect(3, 12, 10, 2, 'm'); L.rect(7, 13, 2, 2, 's')
    L.px(2, 9, 'h'); L.px(13, 9, 'h')


@motif
def ship(L, **kw):
    L.rect(7, 1, 1, 9, 'D')
    L.art(8, 1, ["wwww", "wwwwww", "wwwwwww", "wwwwwwww", "wwwwwww", "wwwwww", "wwww"][::1]) if False else None
    L.art(8, 2, ["ww", "www", "wwww", "wwwww", "wwwww", "WWWW"])
    L.art(1, 3, ["..w", ".ww", "www", "WWW"][::1])
    L.art(0, 10, ["BBBBBBBBBBBBBBBB", ".BBBBBBBBBBBBBB.", "..DDDDDDDDDDDD.."])
    L.px(3, 11, 'y'); L.px(7, 11, 'y'); L.px(11, 11, 'y')
    L.rect(0, 13, 16, 2, 'b'); L.px(2, 13, 'w'); L.px(9, 14, 'w')


@motif
def catapult(L, **kw):
    L.thick(2, 3, 11, 11, 'B', 'D')
    L.rect(1, 12, 14, 2, 'D'); L.disc(3, 13, 2, 'BDD'); L.disc(12, 13, 2, 'BDD')
    L.disc(3, 3, 2.2, 'gGG')


# --- государство
@motif
def banner(L, **kw):
    L.rect(2, 0, 2, 16, 'B'); L.rect(2, 0, 1, 16, 'D')
    for j in range(10):
        for i in range(10):
            if j + 2 > 9 and i > 4 - (j - 7) and False:
                continue
            c = 'h' if j == 0 else 's' if i == 9 else 'm'
            if j > 6 and abs(i - 5) < (j - 6) * 1.4 and False:
                continue
            L.px(4 + i, 1 + j, c)
    L.art(4, 11, ["mmmmmmmmmm", ".mmmmmmmm.", "..mmmmmm..", "...mmmm...", "....mm...."]) if True else None
    L.rect(7, 4, 4, 4, 'w'); L.rect(8, 5, 2, 2, 'M')


@motif
def helmet(L, **kw):
    L.art(2, 2, ["...hhhhhh...", "..hhhhhhhhm.", ".hhhhhhhhhmm", ".hhhhhhhhhms", ".hhhhhhhhhms", ".hhkkkkkkhms", ".hhhhkkhhhms", ".hhkkkkkkhms", ".hmkkhhkkhms", ".hm.kkkk.mss"][:10])
    L.rect(7, 0, 2, 3, 'r')


@motif
def swords(L, **kw):
    L.thick(2, 1, 11, 10, 'W', 'g'); L.thick(12, 1, 3, 10, 'W', 'g')
    L.rect(1, 9, 5, 1, 'Y') if False else None
    L.line(2, 11, 6, 7, 'Y'); L.line(13, 11, 9, 7, 'Y')
    L.thick(3, 12, 1, 14, 'B', 'D'); L.thick(13, 12, 14, 14, 'D', 'B')


@motif
def dove(L, **kw):
    L.art(0, 3, [
        "..........hh..",
        "..hh.....hhhh.",
        "..hhh...hhhhy.",
        "...hhhhhhhhhh.",
        "....hhhhhhhh..",
        ".....hhhhhhm..",
        "...hhmmmmmm...",
        "..hmm.mmmm....",
        ".hm....ms.....",
    ])
    L.px(10, 4, 'k')
    L.art(11, 11, ["nn", ".nn", "..n"][:3])


@motif
def scroll(L, **kw):
    L.rect(3, 2, 10, 12, 't'); L.rect(3, 2, 10, 1, 'w'); L.rect(12, 2, 1, 12, 'T')
    L.rect(2, 1, 12, 2, 'T'); L.rect(2, 13, 12, 2, 'T')
    for y in (5, 7, 9, 11):
        L.rect(5, y, 6, 1, 'D')


@motif
def chart(L, **kw):
    L.rect(2, 1, 12, 14, 'W'); L.rect(2, 1, 12, 1, 'w'); L.rect(13, 1, 1, 14, 'g')
    L.rect(4, 10, 2, 3, 'b'); L.rect(7, 7, 2, 6, 'n'); L.rect(10, 4, 2, 9, 'r')
    L.rect(3, 13, 10, 1, 'G')


@motif
def book(L, **kw):
    L.rect(2, 1, 12, 14, 'm'); L.rect(2, 1, 12, 1, 'h'); L.rect(2, 1, 1, 14, 'h'); L.rect(2, 14, 12, 1, 's'); L.rect(13, 1, 1, 14, 's')
    L.rect(4, 2, 1, 12, 'w') if False else None
    L.rect(5, 3, 7, 5, 'y'); L.rect(6, 4, 5, 3, 'Y') if False else None
    L.rect(6, 5, 5, 1, 'M')
    L.rect(4, 12, 9, 1, 'S')


@motif
def trophy(L, **kw):
    L.art(2, 1, ["yyyyyyyyyyyy", "yhhhyyyyyyyy"[:12], ".yhhyyyyyyY.", "..yhyyyyyYY.", "...yyyyyYY..", "....yyyYY..."][:6])
    L.rect(0, 2, 3, 4, 'Y') if False else None
    L.rect(7, 7, 2, 3, 'Y'); L.rect(5, 10, 6, 2, 'Y'); L.rect(4, 12, 8, 3, 'B'); L.rect(4, 12, 8, 1, 'y')


@motif
def hourglass(L, **kw):
    L.rect(3, 0, 10, 2, 'B'); L.rect(3, 14, 10, 2, 'B')
    L.art(4, 2, ["cccccccc", ".cccccc.", "..cccc..", "...cc...", "...cc...", "..ccss..", ".cssyss.", "ssyyyyss"][:8]) if False else None
    L.art(4, 2, ["wwwwwwww", ".wwwwww.", "..wyyw..", "...yy...", "...yy...", "..wyyw..", ".wwyyww.", "wwyyyyww"])


@motif
def bulb(L, **kw):
    L.ellipse(8, 6, 4.8, 4.8, 'yyY'); L.px(6, 4, 'w'); L.px(7, 3, 'w')
    L.rect(6, 10, 4, 2, 'Y'); L.rect(6, 12, 4, 2, 'g'); L.rect(7, 14, 2, 1, 'G')
    L.px(1, 3, 'y'); L.px(14, 3, 'y'); L.px(0, 7, 'y'); L.px(15, 7, 'y')


@motif
def dice(L, **kw):
    L.rect(2, 2, 12, 12, 'w'); L.rect(2, 2, 12, 1, 'W'); L.rect(2, 13, 12, 1, 'g'); L.rect(13, 2, 1, 12, 'g')
    for x, y in ((4, 4), (10, 4), (7, 7), (4, 10), (10, 10)):
        L.rect(x, y, 2, 2, 'k')


@motif
def coin(L, **kw):
    L.ellipse(8, 8, 6, 6, 'hms'); L.ellipse(8, 8, 3.8, 3.8, 'MMM' if False else 'mhm')
    L.rect(7, 5, 2, 6, 'Y') if False else None
    L.rect(7, 6, 2, 4, 'S')


@motif
def coins(L, **kw):
    L.ellipse(5, 11, 4, 3, 'hms'); L.ellipse(11, 11, 4, 3, 'hms'); L.ellipse(8, 6.5, 4, 3.2, 'hms')
    L.rect(7, 5, 2, 3, 'S')


@motif
def chest(L, **kw):
    L.rect(1, 4, 14, 10, 'B'); L.rect(1, 4, 14, 3, 'h' if False else 'B')
    L.rect(1, 4, 14, 1, 't'); L.rect(1, 13, 14, 1, 'D'); L.rect(14, 4, 1, 10, 'D')
    L.rect(1, 7, 14, 1, 'D'); L.rect(7, 6, 2, 4, 'y'); L.px(7, 7, 'k'); L.px(8, 7, 'k') if False else None
    L.rect(1, 4, 1, 10, 'D') if False else None


@motif
def gift(L, **kw):
    L.rect(2, 6, 12, 9, 'm'); L.rect(2, 6, 12, 1, 'h'); L.rect(13, 6, 1, 9, 's')
    L.rect(1, 4, 14, 3, 'h'); L.rect(1, 6, 14, 1, 's')
    L.rect(7, 4, 2, 11, 'y')
    L.art(4, 1, ["..y.y.", ".yy.yy", "yy...yy"][:3] if False else [".yy..yy.", "y..yy..y", ".yy..yy."][:3])
    L.rect(6, 0, 4, 1, 'Y') if False else None


@motif
def gear(L, **kw):
    for a in range(8):
        import math
        ang = a * math.pi / 4
        for r in (5.5, 6.5):
            L.px(7.5 + r * math.cos(ang) - .0, 7.5 + r * math.sin(ang), 'm')
    L.disc(7.5, 7.5, 5.3, 'hms')
    L.disc(7.5, 7.5, 2, 'sss')
    for x, y in ((6, 0), (7, 0), (8, 0), (6, 15), (7, 15), (8, 15), (0, 6), (0, 7), (0, 8), (15, 6), (15, 7), (15, 8)):
        L.px(x, y, 'm')


@motif
def wand(L, **kw):
    L.thick(3, 13, 11, 5, 'B', 'D')
    L.rect(11, 1, 3, 1, 'y'); star8(L, 12, 3, 2, 1, 'y', 'w')


@motif
def pickaxe(L, **kw):
    L.thick(3, 14, 10, 6, 'B', 'D')
    L.art(2, 1, ["gggggggggg", "gwwwwwwwwgG"[:10], "ggg....GGg"[:10], "gg......GG"])


@motif
def broom(L, **kw):
    L.thick(13, 1, 7, 9, 'B', 'D')
    L.art(2, 8, [".yyyyyy.", "yyYyyYyy", "yyyyyyyy", "yYyyYyyY", "yy.yy.yy", "y..y..y."][:6])


@motif
def trash(L, **kw):
    L.rect(2, 2, 12, 2, 'g'); L.rect(6, 1, 4, 1, 'g')
    L.art(3, 4, ["mmmmmmmmmm", "mhmmhmmhmm", "mhmmhmmhms", "mhmmhmmhms", "mhmmhmmhms", "mhmmhmmhms", "mhmmhmmhms", "sssssssssss"][:8])


@motif
def prohibit(L, **kw):
    for y in range(16):
        for x in range(16):
            import math
            d = math.hypot(x + .5 - 8, y + .5 - 8)
            if 6.0 <= d <= 7.9:
                L.px(x, y, 'r')
    L.thick(3, 12, 11, 3, 'r', 'r') if False else L.line(4, 11, 11, 4, 'r'); L.line(5, 11, 12, 4, 'r'); L.line(3, 11, 11, 3, 'R') if False else None


@motif
def bed(L, **kw):
    L.rect(1, 6, 14, 4, 'w'); L.rect(1, 6, 14, 1, 'W'); L.rect(1, 6, 5, 4, 'r'); L.rect(1, 6, 5, 1, 'R') if False else None
    L.rect(7, 6, 8, 4, 'm'); L.rect(7, 6, 8, 1, 'h')
    L.rect(1, 10, 14, 2, 'B'); L.rect(1, 12, 2, 2, 'D'); L.rect(13, 12, 2, 2, 'D')
    L.rect(2, 4, 4, 2, 'w'); L.rect(2, 4, 4, 1, 'W')


@motif
def magnifier(L, **kw):
    for y in range(16):
        for x in range(16):
            import math
            d = math.hypot(x + .5 - 6.5, y + .5 - 6.5)
            if 3.3 <= d <= 5.0:
                L.px(x, y, 'g')
            elif d < 3.3:
                L.px(x, y, 'c' if x + y > 9 else 'w')
    L.thick(10, 10, 13, 13, 'B', 'D')


@motif
def camera(L, **kw):
    L.rect(1, 4, 14, 9, 'h'); L.rect(1, 4, 14, 1, 'w'); L.rect(1, 12, 14, 1, 'G')
    L.rect(4, 2, 4, 2, 'G'); L.disc(8, 8.5, 3.4, 'cwb' if False else 'kGk'); L.disc(8, 8.5, 2, 'hhh' if False else 'cbc')
    L.px(12, 6, 'r')


@motif
def undo(L, **kw):
    L.art(2, 2, ["...mmmmmm...", ".mmmmmmmmmm.", "mmm......mmm", "mm........mm", "mm........mm", "mmmm.....mmm"][:6]) if False else None
    for pt in ((5, 3), (6, 2), (7, 2), (8, 2), (9, 2), (10, 3), (11, 4), (12, 6), (12, 8), (11, 10), (10, 11), (9, 12)):
        L.rect(pt[0], pt[1], 2, 2, 'm')
    L.art(0, 2, ["..m...", ".mm...", "mmmmm.", ".mm...", "..m..."][:5]) if False else None
    L.art(1, 1, ["...mm.", "..mmm.", ".mmmmm", "mmmmmm", ".mmmmm", "..mmm.", "...mm."][:7]) if False else None
    L.art(2, 3, ["m.", "mm", "mmm", "mmmm", "mmm", "mm", "m."]) if False else None
    L.art(1, 1, ["..mm", ".mmm", "mmmm", ".mmm", "..mm"][:5])


@motif
def compass(L, **kw):
    L.ellipse(8, 8, 7, 7, 'gGg' if False else 'hms')
    L.ellipse(8, 8, 5.2, 5.2, 'www' if False else 'WWW')
    L.art(6, 3, ["..r.", ".rrr", ".rrr", "rrrr"][:4]) if False else None
    L.px(8, 3, 'r'); L.px(8, 4, 'r'); L.px(7, 5, 'r'); L.px(8, 5, 'r'); L.px(8, 6, 'r'); L.px(7, 6, 'r')
    L.px(7, 9, 'G'); L.px(7, 10, 'G'); L.px(8, 10, 'G'); L.px(7, 11, 'G'); L.px(8, 9, 'G')
    L.px(7, 7, 'k'); L.px(8, 7, 'k'); L.px(7, 8, 'k'); L.px(8, 8, 'k')


@motif
def globe(L, **kw):
    L.disc(8, 8, 6.8, 'hms')
    L.art(4, 3, ["nnn", "nnnn", ".nn", "..n"][:4]); L.art(9, 6, ["nn", "nnn", "nn"]); L.art(5, 10, ["nn", "nnn"])
    L.px(5, 4, 'N'); L.px(10, 7, 'N')


@motif
def mapicon(L, **kw):
    L.rect(1, 2, 14, 12, 't'); L.rect(1, 2, 14, 1, 'w'); L.rect(1, 13, 14, 1, 'T')
    L.rect(5, 2, 1, 12, 'T'); L.rect(10, 2, 1, 12, 'T')
    L.art(2, 4, ["nn", "nnn"]); L.art(6, 7, ["bbb", "bbbb"]); L.art(11, 4, ["nn", "nnn", "nn"]); L.rect(7, 10, 2, 2, 'n')
    L.px(12, 10, 'r'); L.px(11, 11, 'r'); L.px(13, 11, 'r') if False else None
    L.px(12, 11, 'r')


@motif
def bell(L, **kw):
    L.rect(7, 0, 2, 2, 'g')
    L.art(3, 2, ["...yyyy...", "..yyyyyy..", "..yhyyyy..", "..yhyyyyY.", "..yhyyyyY.", ".yyhyyyyYY", ".yyyyyyyYY", "yyyyyyyyYY"][:8]) if False else None
    L.art(2, 2, [".....hh.....", "....hhhh....", "...hhhhhm...", "...hhhhhm...", "..hhhhhhmm..", "..hhhhhhms..", ".hhhhhhhmms.", "hhhhhhhhmmss", "mmmmmmmmmmss"])
    L.rect(7, 11, 2, 2, 'k'); L.rect(7, 12, 2, 2, 'g')


@motif
def candle(L, **kw):
    L.rect(6, 7, 4, 8, 'w'); L.rect(6, 7, 1, 8, 'W') if False else None; L.rect(9, 7, 1, 8, 'g')
    L.rect(7, 5, 1, 2, 'k'); L.art(6, 1, [".y.", "yoy", "yoy", ".o."][:4])


@motif
def prayer(L, **kw):
    L.art(3, 1, [
        "...ff.ff...",
        "...ff.ff...",
        "..hff.ffm..",
        "..hhfhffm..",
        "..hhhhhhm..",
        "..hhhhhhm..",
        "..hhhhhhm..",
        "..hhhhhhm..",
        "...hhhhm...",
        "...mmmmm...",
    ][:10]) if False else None
    L.art(3, 1, [
        "....hh.mm....",
        "....hh.mm....",
        "...hhh.mmm...",
        "...hhhhmmm...",
        "..hhhhhmmmm..",
        "..hhhhhmmms..",
        "..hhhhhmmms..",
        "...hhhhmmss..",
        "...hhhhmms...",
        "....hhhmm....",
        "....smmms....",
    ])


@motif
def question(L, **kw):
    L.art(4, 1, ["mmmmmm", "hhhhhhm"[:6], "....hhm", "...hhm.", "..hhm..", "..hm...", "..mm...", "", "..mm..", "..mm.."][:10]) if False else None
    L.art(4, 1, [".mmmm.", "mmmmmm", "mm..mm", "....mm", "...mm.", "..mm..", "..mm..", "......", "..mm..", "..mm.."])


@motif
def leaf(L, **kw):
    L.art(2, 2, ["........hhh", "......hhhhm", "....hhhhhmm", "...hhhhhmmm", "..hhhhhmmms", "..hhhhmmms.", ".hhhmmmms..", ".hmmmmss...", "hBmmss....."][:9])
    L.line(1, 13, 7, 7, 'D')


@motif
def seasons(L, **kw):
    L.rect(1, 1, 7, 7, 'n'); L.rect(8, 1, 7, 7, 'y'); L.rect(1, 8, 7, 7, 'o'); L.rect(8, 8, 7, 7, 'c')
    L.rect(1, 1, 7, 1, 'N') if False else None
    L.art(3, 3, [".w.", "www", ".w."]) if False else None
    L.px(4, 3, 'w'); L.px(3, 4, 'w'); L.px(4, 4, 'w'); L.px(5, 4, 'w'); L.px(4, 5, 'w')
    L.px(11, 3, 'o'); L.px(11, 4, 'o'); L.px(12, 4, 'o'); L.px(10, 4, 'o'); L.px(11, 5, 'o')
    L.px(4, 11, 'r'); L.px(3, 12, 'r'); L.px(5, 12, 'r'); L.px(4, 12, 'r'); L.px(4, 13, 'r')
    L.px(11, 11, 'w'); L.px(10, 12, 'w'); L.px(12, 12, 'w'); L.px(11, 13, 'w'); L.px(11, 12, 'w')


@motif
def ore(L, c1='W', c2='w', **kw):
    for j in range(12):
        for i in range(12):
            if (i in (0, 11) and j in (0, 11)):
                continue
            L.px(2 + i, 2 + j, 'h' if (i + j) % 7 else 'm' if j < 7 else 's')
    L.rect(2, 13, 12, 1, 's'); L.rect(13, 3, 1, 11, 's')
    for x, y in ((4, 4), (9, 5), (6, 8), (10, 10), (4, 11)):
        L.rect(x, y, 2, 2, c1); L.px(x, y, c2)


@motif
def ingot(L, **kw):
    L.art(2, 4, [".hhhhhhhhhh.", "hhhhhhhhhhhm", "hhmmmmmmmmms", ".mmmmmmmmmss"][:4])
    L.art(1, 9, [".hhhhhhhhhhh", "hhhhhhhhhhmm", "hmmmmmmmmmss", ".sssssssss.."][:4])


@motif
def log(L, **kw):
    L.rect(1, 3, 14, 10, 'B'); L.rect(1, 3, 14, 2, 't'); L.rect(1, 11, 14, 2, 'D')
    for x in (3, 7, 11):
        L.px(x, 7, 'D'); L.px(x + 1, 8, 'D')
    L.ellipse(13, 8, 2, 4, 'ttT')


@motif
def bread(L, **kw):
    L.ellipse(8, 9, 7, 4.5, 'hms')
    for x in (4, 7, 10):
        L.line(x, 6, x + 2, 11, 's')


@motif
def stonepile(L, **kw):
    L.ellipse(5, 11, 4.5, 3.5, 'hms'); L.ellipse(11, 11, 4, 3.5, 'hms'); L.ellipse(8, 7, 4.5, 3.8, 'hms')


@motif
def egg(L, **kw):
    L.ellipse(8, 9, 5, 6, 'hms')
    for x, y in ((6, 6), (9, 8), (7, 11), (10, 11)):
        L.px(x, y, 'N')
    L.px(6, 4, 'w')


@motif
def phantom(L, **kw):
    L.art(0, 3, [
        "bb........bb",
        "bbbb....bbbb",
        "bbbbbbbbbbbb",
        ".bbbbbbbbbb.",
        "..bbbbbbbb..",
        "...bkbbkb...",
        "....bbbb...."][:7]) if False else None
    L.art(0, 4, [
        "mm..........mm",
        "hmmm......mmms",
        "hhmmmmmmmmmmms",
        ".hhmmmmmmmmss.",
        "..hhhhhhhhss..",
        "...hGGhhGGs...",
        "....hhhhhs....",
    ])
    L.px(4, 9 - 0, 'g') if False else None
    L.px(5, 9, 'g'); L.px(10, 9, 'g')


@motif
def sprout(L, **kw):
    L.rect(7, 7, 2, 7, 'N'); L.art(3, 3, ["nnn.", "nnnn", ".nnn"]); L.art(9, 4, [".nnn", "nnnn", "nnn."])
    L.rect(2, 14, 12, 2, 'B')


@motif
def crystal(L, **kw):
    L.art(2, 1, [
        "......hh......",
        ".....hwhm.....",
        "....hhwhmm....",
        "...hhhwhhmm...",
        "...hhwhhhmm...",
        "...hhhhhhms...",
        "...hhhhhmms...",
        "...hhhhhmms...",
        "...hmhhmmms...",
        "....mmmmms....",
    ][:10])
    L.art(0, 8, ["..hh", ".hwhm", ".hhmm", "hhhms", "hmmms"][:5])
    L.art(11, 7, ["hh.", "hwhm", "hhmm", "hhms"][:4])
