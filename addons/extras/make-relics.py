"""Relic art (original pixel art). Staff of Quiet Roads: inventory icon (16x16) and the texture for its 3D in-hand model.
from PIL import Image
import sys, os
OUT = sys.argv[1]
WOOD_D, WOOD, WOOD_L = (74, 48, 28), (112, 76, 44), (150, 108, 64)
CLOTH_D, CLOTH, BRASS_D, BRASS, BRASS_L = (58, 64, 110), (92, 104, 168), (150, 104, 30), (214, 160, 54), (255, 226, 130)

ICON = [  # D outline, W wood, L light wood, c cloth, C dark cloth, b dark brass, B brass, Y highlight
 "................",
 "...........DDD..",
 "..........DLWWD.",
 ".........DWDDDWD",
 ".........DWD.DWD",
 "........DWD..DBD",
 ".......DLDb.bYBb",
 "......DWDYBb.DBD",
 ".....DWD.bb...D.",
 "....DcCD........",
 "...DCcD.........",
 "...DcD..........",
 "..DWD...........",
 ".DLD............",
 "DWD.............",
 "DD..............",
]
COLORS = {'D': (52, 32, 18), 'W': WOOD, 'L': WOOD_L, 'c': CLOTH, 'C': CLOTH_D, 'b': BRASS_D, 'B': BRASS, 'Y': BRASS_L}
icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y, row in enumerate(ICON):
    assert len(row) == 16, y
    for x, ch in enumerate(row):
        if ch != '.': icon.putpixel((x, y), COLORS[ch] + (255,))
icon.save(os.path.join(OUT, "staff_of_quiet_roads.png"))

# 3D texture: 16x16 strips the model's faces sample (u ranges): wood 0-3, cloth 4-7, brass 8-11, bell 12-15
tex = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
for y in range(16):
    for x in range(16):
        if x < 4: c = [WOOD_D, WOOD, WOOD_L, WOOD][x] if y % 5 else WOOD_D
        elif x < 8: c = CLOTH if (x + y) % 3 else CLOTH_D
        elif x < 12: c = BRASS_L if y % 4 == 0 else BRASS
        else: c = BRASS if y % 3 else BRASS_D
        tex.putpixel((x, y), c + (255,))
tex.save(os.path.join(OUT, "staff_of_quiet_roads_3d.png"))
big = Image.new("RGBA", (16, 16), (40, 40, 48, 255)); big.alpha_composite(icon)
big.resize((256, 256), Image.NEAREST).save(os.path.join(OUT, "..", "..", "..", "..", "..", "..", "build", "staff-icon-preview.png")) if os.path.isdir(os.path.join(OUT, "..", "..", "..", "..", "..", "..", "build")) else None
