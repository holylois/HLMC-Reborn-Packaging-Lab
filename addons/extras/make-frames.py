"""Original Holy Lois tooltip frames in Legendary Tooltips' layout (64x16 per frame on a 128x128 sheet: corners 8x8 at the four
ends, top ornament x 8-55 rows 0-7, bottom ornament rows 8-15). Drawn as half patterns (left half, mirrored around the middle).
Row 0 Mythic: a red laurel wreath around a crowned jewel. Row 1 Cursed: a violet thorn vine with an eye."""
from PIL import Image

im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))  # same canvas as the mod's own sheet

def stamp(rows, palette, x0, y0, mirror=True):
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch == '.': continue
            c = palette[ch] + (255,)
            im.putpixel((x0 + dx, y0 + dy), c)
            if mirror: im.putpixel((63 - (x0 + dx), y0 + dy), c)

# Mythic: a laurel wreath cradling a crowned jewel, a fine line with pearls out to the corners, the Legendary layout but
# red and taller. d dark red, r red, o orange, y pale gold, w white jewel. Full rows: left half, column 31 is the centre.
MYTHIC = {'d': (110, 14, 20), 'r': (232, 64, 64), 'o': (255, 150, 70), 'y': (255, 222, 150), 'w': (255, 250, 235)}
MYTHIC_TOP = [
 '...............................y',
 '...................yo.........yo',
 '....................ro.o......rr',
 '..y..................rr.o.o..row',
 '...r....o....o....odr..rrr.o.rdo',
 '.....dddddddddddddddd.dr..rrr.rr',
 '.........................dr.....',
 '................................',
]
MYTHIC_BOTTOM = [
 '................................',
 '................................',
 '.....dddddddddddddddddddddddd.rr',
 '...r....o....o....o....o.......o',
 '..y............................y',
 '................................',
 '................................',
 '................................',
]
for oy, rows in ((0, MYTHIC_TOP), (8, MYTHIC_BOTTOM)):
    for dy, row in enumerate(rows):
        assert len(row) == 32
        for dx, ch in enumerate(row):
            if ch != '.':
                im.putpixel((dx, oy + dy), MYTHIC[ch] + (255,)); im.putpixel((62 - dx, oy + dy), MYTHIC[ch] + (255,))

# Cursed: v dark violet, p violet, l lilac, g sick green eye.
CURSED = {'v': (58, 16, 88), 'p': (160, 82, 255), 'l': (220, 170, 255), 'g': (140, 255, 120)}
stamp([
    "........................",
    "...................pp...",
    ".........l........p..pll",
    "...v....p.p...v..p..pgll"[:24],
    "..p.p..p...p.p.pp..p.lpl"[:24],
    ".p...pp..v..p...p.p...pp",
    "pp.v.....p......v.p..p..",
    "........v........v...v..",
], CURSED, 8, 16)
stamp([
    "........................",
    "pp.v.....p......v.p..p..",
    "........v..p.....v...vp.",
    "..........l.........pl..",
    "......................l.",
    "........................",
    "........................",
    "........................",
], CURSED, 8, 24)
for oy, rows in ((16, ["........", ".pp.....", ".p.l....", "..v.....", ".p......", "........", "........", "........"]),
                 (24, ["........", "........", "........", ".p......", "..v.....", ".p.l....", ".pp.....", "........"])):
    stamp(rows, CURSED, 0, oy)

im.save("tooltip_frames.png")
# Preview: the mod's gold frame (if available) above ours, on a dark tooltip background, 8x.
import os, sys
gold = sys.argv[1] if len(sys.argv) > 1 else None
sheet = Image.new("RGBA", (64, 48), (16, 8, 16, 255))
if gold and os.path.exists(gold): sheet.alpha_composite(Image.open(gold).convert("RGBA").crop((0, 0, 64, 16)), (0, 0))
sheet.alpha_composite(im.crop((0, 0, 64, 32)), (0, 16))
sheet.resize((512, 384), Image.NEAREST).save("tooltip_frames-preview.png")
print("ok")
