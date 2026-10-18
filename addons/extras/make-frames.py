"""Original Holy Lois tooltip frames in Legendary Tooltips' layout (64x16 per frame: corners 8x8 at the four ends,
top ornament x 8-55 rows 0-7, bottom ornament rows 8-15). Row 0 Mythic flame crown, row 1 Cursed thorn vine."""
from PIL import Image
im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))  # same canvas as the mod's own sheet
def px(x, y, c): im.putpixel((x, y), c + (255,) if len(c) == 3 else c)
def mirror(x, y, c, oy=0):  # symmetric around the 64px centre (x 31.5)
    px(x, y + oy, c); px(63 - x, y + oy, c)

# Mythic: dark red, red, orange, pale tip
D, R, O, T = (122, 18, 24), (255, 90, 90), (255, 160, 64), (255, 226, 170)
# top band (row 6) with dashes, five flame spikes rising to the centre
for x in range(14, 32): mirror(x, 6, D if x % 3 == 0 else R)
for x, top in ((31, 0), (27, 2), (22, 3), (17, 5)):
    for y in range(top, 6): mirror(x, y, T if y == top else O if y < top + 2 else R)
    mirror(x - 1, 5, D); mirror(x + 1, 5, D) if x + 1 < 32 else None
# bottom band (row 9) and a small drop
for x in range(18, 32): mirror(x, 9, D if x % 4 == 1 else R)
for y, c in ((10, O), (11, T)): mirror(31, y, c)
mirror(30, 10, D)
# corners: little diamonds with a hot centre (top and bottom)
for oy, cy in ((0, 3), (8, 4)):
    mirror(3, cy, T, oy); mirror(2, cy, R, oy); mirror(4, cy, R, oy); mirror(3, cy - 1, R, oy); mirror(3, cy + 1, R, oy)
    mirror(5, cy - 1 if oy == 0 else cy + 1, D, oy)

# Cursed: dark violet, violet, light lilac, sick green eye
V0, V, L, E = (52, 14, 80), (155, 77, 255), (217, 166, 255), (140, 255, 120)
oy = 16
for x in range(12, 32):  # wavy vine rows 5-6
    y = 5 if (x // 3) % 2 == 0 else 6
    mirror(x, y, V, oy)
    if x % 5 == 0: mirror(x, y - 1, L, oy); mirror(x, y - 2, V0, oy)   # thorn up
    if x % 5 == 2: mirror(x, y + 1, V0, oy)                           # thorn down
# the eye at the centre (x 28-35, rows 1-4)
for x in range(28, 32):
    mirror(x, 2 if x in (28, 29) else 1, V, oy); mirror(x, 4 if x in (28, 29) else 5, V, oy)
for x in (30, 31): mirror(x, 2, L, oy); mirror(x, 4, L, oy)
mirror(29, 3, V, oy); mirror(30, 3, L, oy); mirror(31, 3, E, oy)
# bottom: a hanging thorn chain
for x in range(20, 32): mirror(x, 9, V0 if x % 2 else V, oy)
for x in (24, 28, 31): mirror(x, 10, V, oy); mirror(x, 11, L, oy)
# corners: hooked thorns
for cy, d in ((2, 1), (13, -1)):
    mirror(2, cy, V, oy); mirror(3, cy, V, oy); mirror(4, cy, L, oy); mirror(2, cy + d, V, oy); mirror(2, cy + 2 * d, V0, oy)

im.save("tooltip_frames.png")
preview = Image.new("RGBA", (64, 32), (16, 10, 24, 255)); preview.alpha_composite(im.crop((0, 0, 64, 32)))
preview.resize((512, 256), Image.NEAREST).save("tooltip_frames-preview.png")
print("ok")
