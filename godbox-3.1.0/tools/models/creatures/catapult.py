"""Осадная катапульта (онагр): появляется у королевств с механикой (невидимая стойка-носитель, тег gb_siege).

Scale 1.5: about 3 blocks long. +Z is the firing direction. The throwing arm is its own part and swings
forward on Models.trigger (strike), then slowly returns to the cocked position.
"""
from lib import *

c = Creature("catapult", id_base=4200, scale=1.5, res=2, hide_carrier=False, death_fx=False, hitbox=(2.0, 1.8))

wood = Mat((128, 84, 46), mottle=(88, 54, 30), mottle_amt=0.55, mottle_size=3, bevel=0.2,
           patterns=[stripes((70, 42, 24), 1, 4, "h", 0.3, 0.8)])
plank = wood.with_(base=(146, 100, 58), add=[plates((6, 3), 0.25, 0.08)])
dark = wood.with_(base=(98, 62, 34), mottle=(66, 40, 22))
iron = Mat((62, 66, 76), mottle=(38, 40, 48), mottle_amt=0.5, mottle_size=3, bevel=0.28,
           patterns=[speckle((140, 146, 160), 0.04)])
rim = Mat((44, 44, 50), mottle=(24, 24, 28), mottle_size=2, bevel=0.25, patterns=[rivets((170, 174, 184), 3, 1)])
rope = Mat((178, 150, 96), mottle=(120, 96, 56), mottle_amt=0.6, mottle_size=2, bevel=0.1,
           patterns=[stripes((92, 70, 38), 1, 2, "d", 0.6)])

# ---------------------------------------------------------------- frame, windlass, wheels
fr = c.part("frame", pivot=(0, 0, 0))
for s in (1, -1):
    fr.box((s * 4.5, 5.5, -14), (s * 7, 9, 14), plank)                           # side beams
    for z in (-9, 9):                                                              # octagonal wheels
        for ang in (None, ("x", 45)):
            fr.box((s * 7.4, 0, z - 4.5), (s * 9, 9, z + 4.5), wood.with_(faces={"left": plank, "right": plank}), rot=ang)
        fr.box((s * 7.2, 3, z - 1.4), (s * 9.4, 6, z + 1.4), rim)                 # hub
        fr.box((s * 7.5, 0, z - 1.6), (s * 8.9, 0.8, z + 1.6), rim)               # tyre on the ground
    # A-frame uprights with braces
    fr.box((s * 4.2, 9, -1.5), (s * 6.4, 23, 1.5), plank)
    fr.box((s * 4.4, 9.5, 3.5), (s * 6.2, 20.5, 5.5), dark, rot=("x", -22.5))
    fr.box((s * 4.4, 9.5, -5.5), (s * 6.2, 20.5, -3.5), dark, rot=("x", 22.5))
    fr.box((s * 3.6, 18, -2.2), (s * 7, 20, 2.2), iron)                            # bearing plates
    fr.box((s * 2.6, 17, -3), (s * 5.4, 21, 3), rope)                              # twisted rope skeins
for z in (-9, 9):
    fr.box((-9.4, 3.2, z - 0.8), (9.4, 5.8, z + 0.8), iron)                        # axles
for z in (-12, 0, 12):
    fr.box((-7, 6, z - 1.4), (7, 8.6, z + 1.4), dark)                              # cross beams
fr.box((-4.5, 8.6, -12), (4.5, 9.4, 11), plank)                                    # deck
fr.box((-5.8, 22, -1.2), (5.8, 23.6, 1.2), dark)                                   # top tie
# windlass at the rear: drum, crank, ratchet
fr.box((-5, 10, -13), (5, 14, -9), rope, faces={"left": dark, "right": dark})
fr.box((5, 11, -12), (7.5, 13, -10), iron)
fr.box((7, 8.5, -12), (8.4, 15.5, -10), iron)
fr.box((-6.4, 9.4, -13.4), (-4.8, 14.6, -8.6), dark)
# front padded stop beam on posts
for s in (1, -1):
    fr.box((s * 3.4, 8.6, 8.5), (s * 5.4, 14, 10.5), dark)
fr.box((-4.5, 14, 8.5), (4.5, 16.5, 10.5), dark)
fr.box((-4.5, 15.6, 8), (4.5, 16.8, 11), rope)

# ---------------------------------------------------------------- throwing arm (cocked back, swings forward)
arm = c.part("arm", pivot=(0, 19, 0), rot=(-34, 0, 0), anim=[strike("x", 72, 24)])
arm.box((-1.2, -7, -1.2), (1.2, 18, 1.2), plank)                                   # shaft
arm.box((-2.6, -9, -2.6), (2.6, -5, 2.6), iron)                                    # weighted tail
arm.box((-1.7, -2, -1.7), (1.7, 1, 1.7), iron)                                     # hub on the axle
arm.box((-1.6, 8, -1.6), (1.6, 9.2, 1.6), rim)                                     # band
arm.box((-3.2, 17.4, -1.4), (3.2, 18.8, 5), plank)                                 # bucket floor
arm.box((-3.4, 17.4, -1.8), (-2.2, 20.2, 5), dark)                                 # bucket sides
arm.box((2.2, 17.4, -1.8), (3.4, 20.2, 5), dark)
arm.box((-3.2, 17.4, -2.2), (3.2, 21, -1), dark)                                   # bucket back
