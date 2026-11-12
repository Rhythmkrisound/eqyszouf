"""Метеорит: рваная обугленная глыба с раскалёнными жилами (летящий метеор, GodBox Disasters.Meteor).

Две формы: meteor и meteor_b; у каждой тёмная порода rock и светящиеся трещины glow (bright). Центр модели в (0,0,0),
размер в игре задаётся масштабом на стороне Java. Scale 1.5: большая глыба около 2.5 блока.
"""
from lib import *

c = Creature("meteor", id_base=4300, scale=1.5, res=2, hide_carrier=False, death_fx=False, hitbox=(2.0, 2.0))
c2 = Creature("meteor_b", id_base=4400, scale=1.5, res=2, hide_carrier=False, death_fx=False, hitbox=(2.0, 2.0))

CRUST, CRUST2 = (36, 33, 36), (58, 52, 52)
rock = Mat(CRUST, noise=0.1, mottle=CRUST2, mottle_amt=0.7, mottle_size=3, shade=0.18, bevel=0.22,
           patterns=[spots((12, 10, 12), 0.07, (1, 3)), spots((120, 52, 20), 0.012, (1, 2), 0.7),
                     stripes((150, 60, 18), 1, 9, "d", 0.25, 0.9)])
scorch = rock.with_(base=(24, 22, 24), mottle=(44, 38, 38), patterns=[spots((8, 6, 8), 0.1, (1, 3)),
                                                                     stripes((190, 80, 20), 1, 7, "d", 0.3, 0.9)])
ash = rock.with_(base=(74, 68, 66), mottle=(52, 46, 46), patterns=[spots((20, 18, 20), 0.08, (1, 3))])
lava = Mat((196, 58, 14), noise=0.05, mottle=(236, 104, 22), mottle_amt=0.6, mottle_size=2, shade=0, bevel=0)
hot = Mat((255, 206, 70), noise=0.04, mottle=(255, 236, 140), mottle_amt=0.6, mottle_size=2, shade=0, bevel=0)

THIN = 0.4  # на сколько пикселей жила выступает над породой


class Rock:
    """Глыба породы: жилы кладутся в её собственной системе координат с тем же поворотом, поэтому лежат вровень с гранью."""

    def __init__(self, part, frm, to, mat, rot):
        self.frm, self.to, self.rot = frm, to, rot
        self.org = tuple((a + b) / 2 for a, b in zip(frm, to))
        part.box(frm, to, mat, rot=rot)

    def _seg(self, glow, face, a0, a1, b0, b1, mat, t=THIN, inset=0.0):
        ax = "xyz".index(face[0])
        u, v = [i for i in range(3) if i != ax]
        lo, hi = [0.0] * 3, [0.0] * 3
        lo[u], hi[u] = min(a0, a1), max(a0, a1)
        lo[v], hi[v] = min(b0, b1), max(b0, b1)
        if face[1] == "+":
            lo[ax], hi[ax] = self.to[ax] - inset, self.to[ax] + t
        else:
            lo[ax], hi[ax] = self.frm[ax] - t, self.frm[ax] + inset
        glow.box(tuple(lo), tuple(hi), mat, rot=self.rot, origin=self.org)

    def vein(self, glow, face, pts, w=1.2, mat=None):
        """Ломаная трещина по грани: pts — точки (a, b) в осях грани, звенья идут ступеньками вдоль осей."""
        mat = mat or lava
        h = w / 2
        for (a0, b0), (a1, b1) in zip(pts, pts[1:]):
            self._seg(glow, face, a0 - h, a1 + h, b0 - h, b0 + h, mat)
            self._seg(glow, face, a1 - h, a1 + h, b0 - h, b1 + h, mat)

    def spot(self, glow, face, a, b, size=2.0):
        """Небольшая раскалённая вставка, чуть утопленная в породу."""
        h = size / 2
        self._seg(glow, face, a - h, a + h, b - h, b + h, hot, t=0.3, inset=0.3)


# ---------------------------------------------------------------- форма A: тяжёлая глыба
ra = c.part("rock")
ga = c.part("glow", bright=True)
core = Rock(ra, (-8, -7, -8), (8, 7, 8), rock, ("y", 22.5))                    # ядро
left = Rock(ra, (-11, -3, -5), (3, 8, 7), scorch, ("x", 22.5))                 # бугор слева-сверху
low = Rock(ra, (-4, -10, -9), (10, 2, 6), rock, ("z", 22.5))                   # нижний выступ
chip = Rock(ra, (2, 1, -11), (12, 10, 4), ash, ("y", 45))                      # скол спереди
back = Rock(ra, (-9, -9, 2), (1, 1, 12), scorch, ("x", 45))                    # задний горб
Rock(ra, (-12, -8, -10), (-5, -2, -3), rock, ("z", 45))                        # зубец
Rock(ra, (6, -8, 2), (13, -1, 9), ash, ("y", 22.5))                            # мелкий обломок
cap = Rock(ra, (-3, 6, -4), (5, 12, 5), rock, ("x", -22.5))                    # шапка

cap.vein(ga, "y+", [(-2, -3), (-1, 0), (2, 1), (3, 4)])
cap.vein(ga, "y+", [(-1, 0), (-2, 3)], w=1.0)
cap.spot(ga, "y+", 2, 1)
cap.vein(ga, "z-", [(-2, 7), (0, 9), (3, 10)], w=1.0)
left.vein(ga, "x-", [(-2, -3), (1, -1), (2, 3), (5, 4)])
left.vein(ga, "x-", [(1, -1), (-1, 3)], w=1.0)
left.spot(ga, "x-", 2, 3)
left.vein(ga, "z+", [(-10, 1), (-7, 4), (-4, 5)], w=1.0)
chip.vein(ga, "z-", [(3, 2), (6, 4), (6, 7), (10, 8)])
chip.vein(ga, "z-", [(6, 4), (9, 3)], w=1.0)
chip.spot(ga, "z-", 8, 8)
chip.vein(ga, "x+", [(-9, 4), (-6, 5), (-4, 8)], w=1.0)
low.vein(ga, "y-", [(0, -7), (3, -4), (3, 0), (8, 3)])
low.vein(ga, "y-", [(3, -4), (7, -6)], w=1.0)
low.spot(ga, "y-", 3, 0)
core.vein(ga, "z+", [(-6, -3), (-3, 0), (-3, 3), (1, 5)])
core.spot(ga, "z+", -3, 1)
back.vein(ga, "z+", [(-7, -6), (-4, -4), (-3, -1)], w=1.0)
back.spot(ga, "z+", -4, -3)
core.vein(ga, "x+", [(-4, -5), (-1, -2), (2, -1), (4, 3)], w=1.0)
core.spot(ga, "x+", 2, -1, 1.6)
core.vein(ga, "z-", [(-5, -4), (-2, -1), (1, -1), (3, 3)], w=1.0)
core.vein(ga, "z-", [(-2, -1), (-4, 3)], w=1.0)
core.spot(ga, "z-", 1, -1, 1.6)
core.vein(ga, "x-", [(-3, 4), (0, 2), (3, 1)], w=1.0)

# ---------------------------------------------------------------- форма B: вытянутый обломок
rb = c2.part("rock")
gb = c2.part("glow", bright=True)
core = Rock(rb, (-6, -6, -11), (6, 6, 11), rock, ("y", 22.5))
left = Rock(rb, (-9, -2, -4), (5, 8, 8), scorch, ("z", 22.5))
low = Rock(rb, (-2, -9, -12), (8, 1, 3), ash, ("x", 22.5))
tail = Rock(rb, (-8, -8, 3), (3, 1, 13), rock, ("y", 45))
chip = Rock(rb, (1, 2, -13), (9, 9, -3), scorch, ("z", 45))
Rock(rb, (-10, -9, -9), (-4, -3, -2), ash, ("x", 45))
Rock(rb, (3, -9, 5), (10, -2, 11), rock, ("z", 22.5))
cap = Rock(rb, (-3, 5, 2), (5, 11, 10), rock, ("y", -22.5))

cap.vein(gb, "y+", [(-1, 3), (0, 6), (3, 7), (4, 9)])
cap.vein(gb, "y+", [(0, 6), (-2, 8)], w=1.0)
cap.spot(gb, "y+", 2, 7)
left.vein(gb, "y+", [(-7, -2), (-4, 0), (-2, 3), (2, 4)])
left.spot(gb, "y+", -3, 1)
left.vein(gb, "x-", [(0, -2), (3, 0), (4, 3)], w=1.0)
chip.vein(gb, "z-", [(2, 3), (4, 5), (7, 6)])
chip.vein(gb, "z-", [(4, 5), (5, 8)], w=1.0)
chip.spot(gb, "z-", 6, 6)
core.vein(gb, "x+", [(-4, -8), (-1, -4), (3, -3), (4, 2), (1, 6)])
core.vein(gb, "x+", [(-1, -4), (-4, 0)], w=1.0)
core.spot(gb, "x+", 3, -3)
core.vein(gb, "x-", [(-3, -7), (0, -3), (3, -1)], w=1.0)
low.vein(gb, "y-", [(0, -10), (3, -7), (4, -3), (7, 0)])
low.spot(gb, "y-", 4, -3, 1.6)
tail.vein(gb, "z+", [(-6, -6), (-3, -3), (-1, -1)], w=1.0)
tail.spot(gb, "z+", -3, -4)
